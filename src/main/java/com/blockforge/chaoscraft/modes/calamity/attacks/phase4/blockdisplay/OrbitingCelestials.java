package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Block Display -- GROUP 6: ORBITING CELESTIAL BODIES (Structures #51-60)
 * Void Emperor (Boss 4) arena: three orbiting celestial objects at extreme altitude,
 * their trailing phenomena, a conjunction marker, and a debris cloud.
 *
 * Solar system: gold_block sun, amethyst crescent moon, black_concrete void planet.
 * Palette: magenta/cyan/crimson/purple per Calamity color spec.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 */
public final class OrbitingCelestials {

    private OrbitingCelestials() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SolarCore(plugin));
        registry.register(new SolarCoronaRing(plugin));
        registry.register(new SolarOrbitalTrail(plugin));
        registry.register(new AmethystMoon(plugin));
        registry.register(new MoonOrbitalTrail(plugin));
        registry.register(new VoidPlanet(plugin));
        registry.register(new VoidPlanetInnerRing(plugin));
        registry.register(new VoidPlanetOuterRing(plugin));
        registry.register(new CelestialConjunctionMarker(plugin));
        registry.register(new VoidPlanetDebrisCloud(plugin));
    }

    // ================================================================
    // #51 -- SOLAR CORE: Dense gold_block sphere, orbits at radius 38,
    //        Y+28. Self-rotates 1.2 deg/tick, scale breathes sinusoidally.
    // ================================================================
    public static class SolarCore extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sphereBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> sunspotBlocks = new ArrayList<>();
        private BlockDisplayHandle coreBlock;

        public SolarCore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_core", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double orbitalAngle = 0.0;
            double ox = Math.cos(orbitalAngle) * 38.0;
            double oz = Math.sin(orbitalAngle) * 38.0;
            Location sunCenter = center.clone().add(ox, 28, oz);

            // Core block: gold_block, scale 3.8
            coreBlock = displayBuilder.spawnBlock(sunCenter, Material.GOLD_BLOCK);
            coreBlock.scale(3.8f, 3.8f, 3.8f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
            spawnedEntities.add(coreBlock.entity());

            // Upper and lower caps
            BlockDisplayHandle upperCap = displayBuilder.spawnBlock(sunCenter.clone().add(0, 1.9, 0), Material.GOLD_BLOCK);
            upperCap.scale(2.8f, 2.0f, 2.8f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
            sphereBlocks.add(upperCap);
            spawnedEntities.add(upperCap.entity());

            BlockDisplayHandle lowerCap = displayBuilder.spawnBlock(sunCenter.clone().add(0, -1.9, 0), Material.GOLD_BLOCK);
            lowerCap.scale(2.8f, 2.0f, 2.8f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
            sphereBlocks.add(lowerCap);
            spawnedEntities.add(lowerCap.entity());

            // Equatorial band blocks (N, S, E, W)
            double[][] eqOffsets = {{0, 0, 2.0}, {0, 0, -2.0}, {2.0, 0, 0}, {-2.0, 0, 0}};
            for (double[] off : eqOffsets) {
                BlockDisplayHandle eq = displayBuilder.spawnBlock(sunCenter.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                eq.scale(2.2f, 2.2f, 2.2f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
                sphereBlocks.add(eq);
                spawnedEntities.add(eq.entity());
            }

            // Diagonal fills
            double[][] diagOffsets = {
                {1.4, 1.4, 1.4}, {-1.4, 1.4, 1.4}, {1.4, -1.4, 1.4}, {-1.4, -1.4, 1.4}
            };
            for (double[] off : diagOffsets) {
                BlockDisplayHandle diag = displayBuilder.spawnBlock(sunCenter.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                diag.scale(1.8f, 1.8f, 1.8f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
                sphereBlocks.add(diag);
                spawnedEntities.add(diag.entity());
            }

            // Intermediate diagonal fills
            double[][] intOffsets = {
                {1.4, 0, 1.8}, {-1.4, 0, 1.8}, {1.4, 0, -1.8}, {-1.4, 0, -1.8}
            };
            for (double[] off : intOffsets) {
                BlockDisplayHandle fill = displayBuilder.spawnBlock(sunCenter.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                fill.scale(1.6f, 1.6f, 1.6f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
                sphereBlocks.add(fill);
                spawnedEntities.add(fill.entity());
            }

            // 8 gilded_blackstone sunspot fill blocks
            double[][] sunspotOffsets = {
                {1.6, 0.8, 0.8}, {-1.6, 0.8, 0.8}, {0.8, -0.8, 1.6}, {-0.8, -0.8, 1.6},
                {1.6, -0.8, -0.8}, {-1.6, -0.8, -0.8}, {0.8, 0.8, -1.6}, {-0.8, 0.8, -1.6}
            };
            for (double[] off : sunspotOffsets) {
                BlockDisplayHandle spot = displayBuilder.spawnBlock(sunCenter.clone().add(off[0], off[1], off[2]), Material.GILDED_BLACKSTONE);
                spot.scale(1.4f, 1.4f, 1.4f).brightness(15, 15).glow(200, 150, 0).interpolation(3, 0);
                sunspotBlocks.add(spot);
                spawnedEntities.add(spot.entity());
            }

            DisplayBuilder.playSound(sunCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.15f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Orbital motion: 0.45 deg/tick around Y-axis
            double orbitalAngle = Math.toRadians(ticksAlive * 0.45);
            double ox = Math.cos(orbitalAngle) * 38.0;
            double oz = Math.sin(orbitalAngle) * 38.0;
            Location sunPos = center.clone().add(ox, 28, oz);

            // Self-rotation: 1.2 deg/tick
            float selfRot = (float) Math.toRadians(ticksAlive * 1.2);

            // Core breathing: scale = 3.8 + 0.25 * sin(tick * 0.08)
            float coreScale = 3.8f + 0.25f * (float) Math.sin(ticksAlive * 0.08);
            if (coreBlock != null) {
                coreBlock.entity().teleport(sunPos);
                coreBlock.scale(coreScale, coreScale, coreScale);
                coreBlock.rotate(selfRot, 0, 1, 0);
                coreBlock.interpolation(3, 0);
            }

            // Rotate sphere and sunspot blocks around orbital position
            for (BlockDisplayHandle h : sphereBlocks) {
                h.rotate(selfRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Sunspots counter-rotate at -0.8 deg/tick
            float sunspotRot = (float) Math.toRadians(ticksAlive * -0.8);
            for (BlockDisplayHandle h : sunspotBlocks) {
                h.rotate(sunspotRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Particles from sun center
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, sunPos, 6, 3.5, 3.5, 3.5, 0);
                w.spawnParticle(Particle.FLAME, sunPos, 2, 4.0, 4.0, 4.0, 0.03);
                w.spawnParticle(Particle.END_ROD, sunPos, 1, 5.0, 5.0, 5.0, 0);
            }

            // Ambient sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(sunPos, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.15f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SolarCore(plugin); }
    }

    // ================================================================
    // #52 -- SOLAR CORONA RING: 8 yellow_stained_glass arc segments +
    //        3 magma_block flare ejections around the sun equator.
    // ================================================================
    public static class SolarCoronaRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arcSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> flareBlocks = new ArrayList<>();

        public SolarCoronaRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_corona_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location sunCenter = center.clone().add(38, 28, 0);

            // 8 arc segments at ring radius 4.2
            double[] arcAngles = {0, 40, 80, 130, 170, 220, 260, 310};
            for (double angleDeg : arcAngles) {
                double rad = Math.toRadians(angleDeg);
                double x = Math.cos(rad) * 4.2;
                double z = Math.sin(rad) * 4.2;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(
                        sunCenter.clone().add(x, 0, z), Material.YELLOW_STAINED_GLASS);
                float tangentRot = (float) Math.toRadians(angleDeg + 90);
                seg.scale(0.6f, 0.6f, 4.8f).rotate(tangentRot, 0, 1, 0)
                   .glow(255, 200, 0).interpolation(3, 0);
                arcSegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            // 3 flare ejection points at gap positions
            double[] gapAngles = {120, 240, 340};
            for (double angleDeg : gapAngles) {
                double rad = Math.toRadians(angleDeg);
                double[] yOffsets = {0.5, 1.8, 3.2};
                for (double yOff : yOffsets) {
                    double x = Math.cos(rad) * 5.8;
                    double z = Math.sin(rad) * 5.8;
                    BlockDisplayHandle flare = displayBuilder.spawnBlock(
                            sunCenter.clone().add(x, yOff, z), Material.MAGMA_BLOCK);
                    flare.scale(0.8f, 0.8f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                    flareBlocks.add(flare);
                    spawnedEntities.add(flare.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring rotates at 0.6 deg/tick (half main sphere speed)
            float ringRot = (float) Math.toRadians(ticksAlive * 0.6);
            for (BlockDisplayHandle seg : arcSegments) {
                seg.rotate(ringRot, 0, 1, 0);
                seg.interpolation(3, 0);
            }

            // Flare blocks Y oscillation
            for (int i = 0; i < flareBlocks.size(); i++) {
                float yOsc = 0.4f * (float) Math.sin(ticksAlive * 0.12 + i * 0.5);
                BlockDisplay bd = flareBlocks.get(i).entity();
                Location loc = bd.getLocation();
                bd.teleport(loc.clone().add(0, yOsc - 0.4f * (float) Math.sin((ticksAlive - 1) * 0.12 + i * 0.5), 0));
            }

            // Flame particles from flare blocks
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle flare : flareBlocks) {
                    Location flareLoc = flare.entity().getLocation();
                    w.spawnParticle(Particle.FLAME, flareLoc, 3, 0.2, 0.2, 0.2, 0.06);
                }
            }

            // Ambient sound every 80 ticks
            if (ticksAlive % 80 == 0 && !flareBlocks.isEmpty()) {
                DisplayBuilder.playSound(flareBlocks.get(0).entity().getLocation(),
                        Sound.ENTITY_BLAZE_AMBIENT, 0.08f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SolarCoronaRing(plugin); }
    }

    // ================================================================
    // #53 -- SOLAR ORBITAL TRAIL: Particle emitter 90 deg behind sun,
    //        golden TOTEM ribbon + FLAME + END_ROD trailing particles.
    // ================================================================
    public static class SolarOrbitalTrail extends BlockDisplayAttack {

        public SolarOrbitalTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_orbital_trail", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only emitter, no block structures spawned
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Trail sits 90 deg behind the sun on the same orbital ring
            double orbitalAngle = Math.toRadians(ticksAlive * 0.45) - Math.PI / 2.0;
            double tx = Math.cos(orbitalAngle) * 38.0;
            double tz = Math.sin(orbitalAngle) * 38.0;
            Location trailPos = center.clone().add(tx, 28, tz);

            // Dense golden ribbon
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, trailPos, 12, 1.5, 0.8, 1.5, 0);
            w.spawnParticle(Particle.FLAME, trailPos, 3, 1.0, 1.0, 1.0, 0);
            w.spawnParticle(Particle.END_ROD, trailPos, 2, 2.0, 2.0, 2.0, 0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SolarOrbitalTrail(plugin); }
    }

    // ================================================================
    // #54 -- AMETHYST MOON: Crescent silhouette of 18 blocks, orbits
    //        counter-clockwise at radius 52, Y+35, 0.28 deg/tick.
    //        Amethyst_block outer arc, budding_amethyst inner concave,
    //        calcite crescent tips, amethyst_cluster accents.
    // ================================================================
    public static class AmethystMoon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerArc = new ArrayList<>();
        private final List<BlockDisplayHandle> innerConcave = new ArrayList<>();
        private final List<BlockDisplayHandle> crescentTips = new ArrayList<>();
        private final List<BlockDisplayHandle> clusterAccents = new ArrayList<>();

        public AmethystMoon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("amethyst_moon", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location moonCenter = center.clone().add(0, 35, 52);

            // Outer arc: 7 amethyst_block at 25-deg steps (with gap 55-130)
            double[] outerAngles = {0, 25, 50, 75, 130, 155, 180};
            for (double angleDeg : outerAngles) {
                double rad = Math.toRadians(angleDeg);
                double x = Math.cos(rad) * 4.5;
                double z = Math.sin(rad) * 4.5;
                BlockDisplayHandle arc = displayBuilder.spawnBlock(
                        moonCenter.clone().add(x, 0, z), Material.AMETHYST_BLOCK);
                float faceRot = (float) Math.toRadians(angleDeg);
                arc.scale(2.4f, 2.4f, 2.4f).rotate(faceRot, 0, 1, 0)
                   .brightness(8, 8).glow(180, 0, 255).interpolation(3, 0);
                outerArc.add(arc);
                spawnedEntities.add(arc.entity());
            }

            // Inner concave face: 5 budding_amethyst blocks
            double[] innerAngles = {25, 50, 75, 100, 130};
            for (double angleDeg : innerAngles) {
                double rad = Math.toRadians(angleDeg);
                double x = Math.cos(rad) * 2.7;
                double z = Math.sin(rad) * 2.7;
                BlockDisplayHandle inner = displayBuilder.spawnBlock(
                        moonCenter.clone().add(x, 0, z), Material.BUDDING_AMETHYST);
                inner.scale(1.8f, 3.2f, 1.8f).glow(180, 0, 255).interpolation(3, 0);
                innerConcave.add(inner);
                spawnedEntities.add(inner.entity());
            }

            // Crescent tips: 2 calcite blocks
            BlockDisplayHandle tipA = displayBuilder.spawnBlock(
                    moonCenter.clone().add(4.5, 1.5, 0), Material.CALCITE);
            tipA.scale(1.2f, 2.8f, 1.2f).glow(200, 200, 255).interpolation(3, 0);
            crescentTips.add(tipA);
            spawnedEntities.add(tipA.entity());

            double rad180 = Math.toRadians(180);
            BlockDisplayHandle tipB = displayBuilder.spawnBlock(
                    moonCenter.clone().add(Math.cos(rad180) * 4.5, 1.5, Math.sin(rad180) * 4.5), Material.CALCITE);
            tipB.scale(1.2f, 2.8f, 1.2f).glow(200, 200, 255).interpolation(3, 0);
            crescentTips.add(tipB);
            spawnedEntities.add(tipB.entity());

            // Amethyst cluster accents: 4 blocks on inner surface
            double[] accentAngles = {37, 62, 95, 120};
            for (double angleDeg : accentAngles) {
                double aRad = Math.toRadians(angleDeg);
                double x = Math.cos(aRad) * 2.4;
                double z = Math.sin(aRad) * 2.4;
                BlockDisplayHandle accent = displayBuilder.spawnBlock(
                        moonCenter.clone().add(x, 0, z), Material.AMETHYST_CLUSTER);
                accent.scale(0.9f, 0.9f, 0.9f).glow(180, 0, 255).interpolation(3, 0);
                clusterAccents.add(accent);
                spawnedEntities.add(accent.entity());
            }

            DisplayBuilder.playSound(moonCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.2f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Counter-clockwise orbit: -0.28 deg/tick
            double orbitalAngle = Math.toRadians(ticksAlive * -0.28);
            double ox = Math.cos(orbitalAngle) * 52.0;
            double oz = Math.sin(orbitalAngle) * 52.0;
            Location moonPos = center.clone().add(ox, 35, oz);

            // Scale breathe: 2.4 + 0.25 * sin(tick * 0.07)
            float breathScale = 2.4f + 0.25f * (float) Math.sin(ticksAlive * 0.07);
            for (BlockDisplayHandle h : outerArc) {
                h.scale(breathScale, breathScale, breathScale);
                h.interpolation(3, 0);
            }

            // Crescent tip oscillation
            for (int i = 0; i < crescentTips.size(); i++) {
                float tipYOsc = 1.5f + 0.6f * (float) Math.sin(ticksAlive * 0.11 + 1.57);
                crescentTips.get(i).scale(1.2f, 2.8f + tipYOsc * 0.1f, 1.2f);
                crescentTips.get(i).interpolation(3, 0);
            }

            // Self-rotation on Z-axis at 0.15 deg/tick
            float zRot = (float) Math.toRadians(ticksAlive * 0.15);
            for (BlockDisplayHandle h : outerArc) {
                h.rotate(zRot, 0, 0, 1);
            }

            // Particles from crescent center
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.WITCH, moonPos, 4, 3.0, 3.0, 3.0, 0);
                w.spawnParticle(Particle.CRIT, moonPos, 2, 4.0, 4.0, 4.0, 0);
            }

            // Dragon breath from calcite tips
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle tip : crescentTips) {
                    Location tipLoc = tip.entity().getLocation();
                    w.spawnParticle(Particle.DRAGON_BREATH, tipLoc, 1, 0.2, 0.2, 0.2, 0.04);
                }
            }

            // Ambient sound every 140 ticks
            if (ticksAlive % 140 == 0) {
                DisplayBuilder.playSound(moonPos, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.2f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystMoon(plugin); }
    }

    // ================================================================
    // #55 -- MOON ORBITAL TRAIL: Particle emitter 120 deg behind moon.
    //        Magenta WITCH ribbon + CRIT sparkle trail.
    // ================================================================
    public static class MoonOrbitalTrail extends BlockDisplayAttack {

        public MoonOrbitalTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_orbital_trail", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only emitter
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Trail 120 deg behind moon on same orbital circle
            double moonAngle = Math.toRadians(ticksAlive * -0.28);
            double trailAngle = moonAngle - Math.toRadians(120);
            double tx = Math.cos(trailAngle) * 52.0;
            double tz = Math.sin(trailAngle) * 52.0;
            Location trailPos = center.clone().add(tx, 35, tz);

            w.spawnParticle(Particle.WITCH, trailPos, 8, 1.8, 0.6, 1.8, 0);
            w.spawnParticle(Particle.CRIT, trailPos, 2, 2.5, 2.5, 2.5, 0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonOrbitalTrail(plugin); }
    }

    // ================================================================
    // #56 -- VOID PLANET: Compact dark sphere, 14 blocks.
    //        Innermost orbit radius 24, Y+22. Fastest at 0.72 deg/tick.
    //        Tidally locked. Phase 3 swell to scale 6.5.
    // ================================================================
    public static class VoidPlanet extends BlockDisplayAttack {

        private BlockDisplayHandle centralMass;
        private final List<BlockDisplayHandle> planetBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> craterBlocks = new ArrayList<>();

        public VoidPlanet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_planet", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location planetCenter = center.clone().add(24, 22, 0);

            // Central mass: black_concrete, max darkness
            centralMass = displayBuilder.spawnBlock(planetCenter, Material.BLACK_CONCRETE);
            centralMass.scale(4.2f, 4.2f, 4.2f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
            spawnedEntities.add(centralMass.entity());

            // Upper and lower nodes: obsidian
            BlockDisplayHandle upper = displayBuilder.spawnBlock(planetCenter.clone().add(0, 2.2, 0), Material.OBSIDIAN);
            upper.scale(2.8f, 2.0f, 2.8f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
            planetBlocks.add(upper);
            spawnedEntities.add(upper.entity());

            BlockDisplayHandle lower = displayBuilder.spawnBlock(planetCenter.clone().add(0, -2.2, 0), Material.OBSIDIAN);
            lower.scale(2.8f, 2.0f, 2.8f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
            planetBlocks.add(lower);
            spawnedEntities.add(lower.entity());

            // Four equatorial masses
            double[][] eqPos = {{2.1, 0, 0}, {-2.1, 0, 0}, {0, 0, 2.1}, {0, 0, -2.1}};
            for (double[] off : eqPos) {
                BlockDisplayHandle eq = displayBuilder.spawnBlock(planetCenter.clone().add(off[0], off[1], off[2]), Material.OBSIDIAN);
                eq.scale(2.0f, 2.0f, 2.0f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
                planetBlocks.add(eq);
                spawnedEntities.add(eq.entity());
            }

            // Four diagonal masses
            double[][] diagPos = {{1.5, 1.5, 0}, {-1.5, 1.5, 0}, {0, 1.5, 1.5}, {0, -1.5, 1.5}};
            for (double[] off : diagPos) {
                BlockDisplayHandle diag = displayBuilder.spawnBlock(planetCenter.clone().add(off[0], off[1], off[2]), Material.BLACK_CONCRETE);
                diag.scale(1.6f, 1.6f, 1.6f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
                planetBlocks.add(diag);
                spawnedEntities.add(diag.entity());
            }

            // Three crying_obsidian craters
            double[][] craterPos = {{2.0, 1.5, 0.8}, {-1.8, 0.5, 1.6}, {0.5, -2.0, -1.4}};
            for (double[] off : craterPos) {
                BlockDisplayHandle crater = displayBuilder.spawnBlock(planetCenter.clone().add(off[0], off[1], off[2]), Material.CRYING_OBSIDIAN);
                crater.scale(0.9f, 0.9f, 0.9f).brightness(2, 2).glow(128, 0, 255).interpolation(3, 0);
                craterBlocks.add(crater);
                spawnedEntities.add(crater.entity());
            }

            DisplayBuilder.playSound(planetCenter, Sound.ENTITY_ENDERMAN_AMBIENT, 0.25f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fastest orbit: 0.72 deg/tick
            double orbitalAngle = Math.toRadians(ticksAlive * 0.72);
            double ox = Math.cos(orbitalAngle) * 24.0;
            double oz = Math.sin(orbitalAngle) * 24.0;
            Location planetPos = center.clone().add(ox, 22, oz);

            // Tidally locked: face inward toward center
            float faceInward = (float) (orbitalAngle + Math.PI);
            if (centralMass != null) {
                centralMass.entity().teleport(planetPos);
                centralMass.rotate(faceInward, 0, 1, 0);
                centralMass.interpolation(3, 0);
            }

            // Inward-pulling PORTAL particles
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.PORTAL, planetPos, 6, 3.0, 3.0, 3.0, -0.02);
                w.spawnParticle(Particle.SMOKE, planetPos, 3, 4.0, 4.0, 4.0, -0.01);
            }

            // Ambient sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(planetPos, Sound.ENTITY_ENDERMAN_AMBIENT, 0.25f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPlanet(plugin); }
    }

    // ================================================================
    // #57 -- VOID PLANET INNER RING: 24 obsidian BlockDisplay entities
    //        in a ring tilted 35 deg, radius 7.5, rotates 1.8 deg/tick.
    // ================================================================
    public static class VoidPlanetInnerRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringShards = new ArrayList<>();

        public VoidPlanetInnerRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_planet_inner_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location planetCenter = center.clone().add(24, 22, 0);
            double ringRadius = 7.5;
            double tiltRad = Math.toRadians(35);

            // 24 shards at 15-deg intervals on a tilted ring
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15.0);
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                double y = Math.sin(angle) * Math.sin(tiltRad) * ringRadius * 0.3;

                BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        planetCenter.clone().add(x, y, z), Material.OBSIDIAN);
                float tangentRot = (float) Math.toRadians(i * 15.0 + 90);
                shard.scale(0.55f, 0.55f, 0.55f).rotate(tangentRot, 0, 1, 0)
                     .brightness(4, 4).glow(30, 0, 60).interpolation(3, 0);
                ringShards.add(shard);
                spawnedEntities.add(shard.entity());
            }

            DisplayBuilder.playSound(planetCenter, Sound.BLOCK_STONE_BREAK, 0.05f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring rotates at 1.8 deg/tick
            float ringRot = (float) Math.toRadians(ticksAlive * 1.8);
            for (int i = 0; i < ringShards.size(); i++) {
                // Scale ripple: 0.55 + 0.10 * sin(tick * 0.15 + index * 0.26)
                float scaleOsc = 0.55f + 0.10f * (float) Math.sin(ticksAlive * 0.15 + i * 0.26);
                ringShards.get(i).scale(scaleOsc, scaleOsc, scaleOsc);
                ringShards.get(i).rotate(ringRot, 0, 1, 0);
                ringShards.get(i).interpolation(3, 0);
            }

            // PORTAL particles from ring plane
            if (ticksAlive % 3 == 0) {
                double orbitalAngle = Math.toRadians(ticksAlive * 0.72);
                double ox = Math.cos(orbitalAngle) * 24.0;
                double oz = Math.sin(orbitalAngle) * 24.0;
                Location planetPos = center.clone().add(ox, 22, oz);
                w.spawnParticle(Particle.PORTAL, planetPos, 4, 7.5, 0.5, 7.5, 0);
                w.spawnParticle(Particle.SMOKE, planetPos, 2, 7.5, 0.5, 7.5, 0);
            }

            // Sound every 60 ticks
            if (ticksAlive % 60 == 0 && !ringShards.isEmpty()) {
                DisplayBuilder.playSound(ringShards.get(0).entity().getLocation(),
                        Sound.BLOCK_STONE_BREAK, 0.05f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPlanetInnerRing(plugin); }
    }

    // ================================================================
    // #58 -- VOID PLANET OUTER RING: 16 larger obsidian shards,
    //        radius 11, counter-rotates at -1.1 deg/tick.
    // ================================================================
    public static class VoidPlanetOuterRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerShards = new ArrayList<>();

        public VoidPlanetOuterRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_planet_outer_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location planetCenter = center.clone().add(24, 22, 0);
            double ringRadius = 11.0;
            double tiltX = Math.toRadians(35);
            double tiltZ = Math.toRadians(20);

            // 16 shards at 22.5-deg intervals
            for (int i = 0; i < 16; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                double y = Math.sin(angle) * Math.sin(tiltX) * ringRadius * 0.25
                         + Math.cos(angle) * Math.sin(tiltZ) * ringRadius * 0.15;

                BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        planetCenter.clone().add(x, y, z), Material.OBSIDIAN);
                shard.scale(0.95f, 0.95f, 0.95f).brightness(2, 2).glow(20, 0, 40).interpolation(3, 0);
                outerShards.add(shard);
                spawnedEntities.add(shard.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Counter-rotation at -1.1 deg/tick
            float outerRot = (float) Math.toRadians(ticksAlive * -1.1);
            for (int i = 0; i < outerShards.size(); i++) {
                float scaleOsc = 0.95f + 0.10f * (float) Math.sin(ticksAlive * 0.098 + i * 0.39);
                outerShards.get(i).scale(scaleOsc, scaleOsc, scaleOsc);
                outerShards.get(i).rotate(outerRot, 0, 1, 0);
                outerShards.get(i).interpolation(3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPlanetOuterRing(plugin); }
    }

    // ================================================================
    // #59 -- CELESTIAL CONJUNCTION MARKER: Single large NETHER_STAR
    //        ItemDisplay at arena center Y+32. Rotates 2.5 deg/tick.
    //        Scale oscillates. Descends in Phase 3.
    // ================================================================
    public static class CelestialConjunctionMarker extends BlockDisplayAttack {

        private ItemDisplayHandle starDisplay;

        public CelestialConjunctionMarker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_conjunction_marker", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location markerPos = center.clone().add(0, 32, 0);
            starDisplay = displayBuilder.spawnItem(markerPos, new ItemStack(Material.NETHER_STAR));
            starDisplay.scale(2.2f, 2.2f, 2.2f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(starDisplay.entity());

            DisplayBuilder.playSound(markerPos, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (starDisplay == null) return;

            // Fast Y-axis rotation: 2.5 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * 2.5);
            starDisplay.rotate(rot, 0, 1, 0);

            // Scale oscillation: 2.2 + 0.45 * sin(tick * 0.10)
            float scaleOsc = 2.2f + 0.45f * (float) Math.sin(ticksAlive * 0.10);
            starDisplay.scale(scaleOsc, scaleOsc, scaleOsc);
            starDisplay.interpolation(3, 0);

            Location markerPos = center.clone().add(0, 32, 0);

            // Particles
            if (ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, markerPos, 10, 1.5, 1.5, 1.5, 0);
                w.spawnParticle(Particle.CRIT, markerPos, 6, 2.0, 2.0, 2.0, 0);
                w.spawnParticle(Particle.END_ROD, markerPos, 4, 3.0, 3.0, 3.0, 0.08);
            }

            // Ambient sound every 80 ticks
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(markerPos, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialConjunctionMarker(plugin); }
    }

    // ================================================================
    // #60 -- VOID PLANET DEBRIS CLOUD: 30 small BlockDisplay debris
    //        trailing 180 deg behind void planet. Gravel, coarse_dirt,
    //        gray_concrete_powder. Turbulent floating quality.
    // ================================================================
    public static class VoidPlanetDebrisCloud extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> debrisBlocks = new ArrayList<>();
        private final double[] debrisAngles;
        private final float[] debrisPhases;

        public VoidPlanetDebrisCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_planet_debris_cloud", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
            debrisAngles = new double[30];
            debrisPhases = new float[30];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location cloudCenter = center.clone().add(-24, 22, 0); // 180 deg behind planet start

            Material[] mats = {
                Material.GRAVEL, Material.COARSE_DIRT, Material.GRAY_CONCRETE_POWDER
            };
            float[][] scaleRanges = {
                {0.3f, 0.7f}, {0.25f, 0.55f}, {0.2f, 0.45f}
            };
            int[] counts = {12, 10, 8};

            int idx = 0;
            for (int matIdx = 0; matIdx < 3; matIdx++) {
                for (int i = 0; i < counts[matIdx]; i++) {
                    // Spread across 60-deg trailing arc
                    double arcOffset = Math.toRadians(-30 + (60.0 * idx / 30.0));
                    debrisAngles[idx] = arcOffset;
                    debrisPhases[idx] = (float) (Math.random() * Math.PI * 2);

                    double x = Math.cos(arcOffset) * (23 + Math.random() * 2);
                    double z = Math.sin(arcOffset) * (23 + Math.random() * 2);
                    double yJitter = (Math.random() - 0.5) * 2.0;

                    float scaleLow = scaleRanges[matIdx][0];
                    float scaleHigh = scaleRanges[matIdx][1];
                    float scale = scaleLow + (float) Math.random() * (scaleHigh - scaleLow);

                    BlockDisplayHandle debris = displayBuilder.spawnBlock(
                            cloudCenter.clone().add(x, yJitter, z), mats[matIdx]);
                    debris.scale(scale, scale, scale).glow(60, 60, 60).interpolation(3, 0);
                    debrisBlocks.add(debris);
                    spawnedEntities.add(debris.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(cloudCenter, Sound.BLOCK_GRAVEL_BREAK, 0.04f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Debris follows void planet orbit 180 deg behind
            double planetAngle = Math.toRadians(ticksAlive * 0.72);
            double debrisBaseAngle = planetAngle + Math.PI; // 180 deg behind

            // Y turbulence on gray_concrete_powder blocks (last 8)
            for (int i = 22; i < debrisBlocks.size() && i < 30; i++) {
                float yOsc = 0.3f * (float) Math.sin(ticksAlive * 0.09 + debrisPhases[i]);
                BlockDisplay bd = debrisBlocks.get(i).entity();
                Location loc = bd.getLocation();
                float prevYOsc = 0.3f * (float) Math.sin((ticksAlive - 1) * 0.09 + debrisPhases[i]);
                bd.teleport(loc.clone().add(0, yOsc - prevYOsc, 0));
            }

            // SMOKE + PORTAL from debris zone
            if (ticksAlive % 3 == 0) {
                double dox = Math.cos(debrisBaseAngle) * 24.0;
                double doz = Math.sin(debrisBaseAngle) * 24.0;
                Location debrisCenter = center.clone().add(dox, 22, doz);
                w.spawnParticle(Particle.SMOKE, debrisCenter, 8, 3.0, 1.0, 3.0, 0);
                w.spawnParticle(Particle.PORTAL, debrisCenter, 3, 3.0, 1.0, 3.0, 0);
            }

            // Sound every 45 ticks
            if (ticksAlive % 45 == 0 && !debrisBlocks.isEmpty()) {
                float pitchJitter = 1.2f + (float) (Math.random() * 0.6);
                DisplayBuilder.playSound(debrisBlocks.get(0).entity().getLocation(),
                        Sound.BLOCK_GRAVEL_BREAK, 0.04f, pitchJitter);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPlanetDebrisCloud(plugin); }
    }
}
