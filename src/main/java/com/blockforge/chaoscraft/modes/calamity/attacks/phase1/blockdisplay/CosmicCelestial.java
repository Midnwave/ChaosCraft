package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

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
 * Phase 1 Block Display — GROUP 9: COSMIC AND CELESTIAL
 * 10 structures evoking the violence of stars, the silence of interstellar voids,
 * and the beautiful horror of celestial bodies consuming each other.
 * Adapted from boss1-voidmaw.md attacks #81-90 with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, etc. removed — damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple 128,0,255 / cyan 0,200,255 / crimson 200,0,50)
 * - All values configurable via AttackConfig
 */
public final class CosmicCelestial {

    private CosmicCelestial() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidNova(plugin));
        registry.register(new BlackEclipse(plugin));
        registry.register(new DeadPlanet(plugin));
        registry.register(new PulsarBeam(plugin));
        registry.register(new StellarGraveyardOrrery(plugin));
        registry.register(new Singularity(plugin));
        registry.register(new DyingStar(plugin));
        registry.register(new WanderingBlackHole(plugin));
        registry.register(new DeepField(plugin));
        registry.register(new CosmicWeb(plugin));
    }

    // ================================================================
    // 81. VOID NOVA — Expanding shockwave rings from collapsed stellar remnant
    // ================================================================
    public static class VoidNova extends BlockDisplayAttack {

        private BlockDisplayHandle coreBlock;
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> ejectedMatter = new ArrayList<>();
        private final List<BlockDisplayHandle> shimmerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> plasmaJets = new ArrayList<>();
        private int cycleStartTick = 0;

        public VoidNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_nova", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(24.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Netherite core — collapsed stellar remnant at center
            coreBlock = displayBuilder.spawnBlock(center, Material.NETHERITE_BLOCK);
            coreBlock.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(coreBlock.entity());

            // Inner shockwave ring: 12 obsidian at 2-block radius
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer shockwave ring: 16 black concrete at 4-block radius
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                Material mat = (i % 2 == 0) ? Material.BLACK_CONCRETE : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ejected stellar matter: 6 amethyst scattered on sphere at 3-block radius
            for (int i = 0; i < 6; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 6);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 3;
                double y = Math.cos(phi) * 3;
                double z = Math.sin(phi) * Math.sin(theta) * 3;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(3, 0);
                ejectedMatter.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shimmer ring: 4 dark prismarine at 5-block radius
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(3, 0);
                shimmerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Plasma jets: 3 purple stained glass at 120-degree spacing
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 5, 0.5, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 0, 50).interpolation(3, 0);
                plasmaJets.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Repeating nova cycle every 100 ticks
            int cyclePos = ticksAlive % 100;

            // Expansion phase: 0-80 ticks, scale 0.3 → 3.0
            float expansionProgress;
            if (cyclePos <= 80) {
                expansionProgress = cyclePos / 80.0f;
            } else {
                // Snap-back phase: 80-100, rapidly shrink
                expansionProgress = 1.0f - ((cyclePos - 80) / 20.0f);
            }

            float ringScale = 0.3f + expansionProgress * 2.7f;
            float ringRadius;

            // Inner ring expansion
            ringRadius = 2.0f + expansionProgress * 6.0f;
            for (int i = 0; i < innerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / innerRing.size();
                Location target = c.clone().add(Math.cos(angle) * ringRadius, 0, Math.sin(angle) * ringRadius);
                innerRing.get(i).entity().teleport(target);
                innerRing.get(i).scale(ringScale * 0.5f, ringScale * 0.5f, ringScale * 0.5f);
                innerRing.get(i).interpolation(3, 0);
            }

            // Outer ring expansion (slightly larger)
            ringRadius = 4.0f + expansionProgress * 8.0f;
            for (int i = 0; i < outerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / outerRing.size();
                Location target = c.clone().add(Math.cos(angle) * ringRadius, 0, Math.sin(angle) * ringRadius);
                outerRing.get(i).entity().teleport(target);
                outerRing.get(i).scale(ringScale * 0.4f, ringScale * 0.4f, ringScale * 0.4f);
                outerRing.get(i).interpolation(3, 0);
            }

            // Ejected matter: expand on sphere with wobble
            float matterRadius = 3.0f + expansionProgress * 5.0f;
            for (int i = 0; i < ejectedMatter.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 6);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.02;
                double x = Math.sin(phi) * Math.cos(theta) * matterRadius;
                double y = Math.cos(phi) * matterRadius;
                double z = Math.sin(phi) * Math.sin(theta) * matterRadius;
                ejectedMatter.get(i).entity().teleport(c.clone().add(x, y, z));
                ejectedMatter.get(i).scale(ringScale * 0.3f, ringScale * 0.3f, ringScale * 0.3f);
                ejectedMatter.get(i).interpolation(3, 0);
            }

            // Shimmer ring: outermost, lagging behind
            float shimmerProgress = Math.max(0, (cyclePos - 5) / 80.0f);
            if (cyclePos > 80) shimmerProgress = 1.0f - ((cyclePos - 80) / 20.0f);
            float shimmerRadius = 5.0f + shimmerProgress * 10.0f;
            for (int i = 0; i < shimmerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / shimmerRing.size();
                Location target = c.clone().add(Math.cos(angle) * shimmerRadius, 0, Math.sin(angle) * shimmerRadius);
                shimmerRing.get(i).entity().teleport(target);
                shimmerRing.get(i).interpolation(3, 0);
            }

            // Plasma jets: lag 10 ticks behind, trail outward faster
            float jetProgress = Math.max(0, Math.min(1, (cyclePos - 10) / 70.0f));
            if (cyclePos > 80) jetProgress = 1.0f - ((cyclePos - 80) / 20.0f);
            float jetRadius = 5.0f + jetProgress * 14.0f;
            for (int i = 0; i < plasmaJets.size(); i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location target = c.clone().add(Math.cos(angle) * jetRadius, 0.5, Math.sin(angle) * jetRadius);
                plasmaJets.get(i).entity().teleport(target);
                float jetScale = 0.3f + jetProgress * 1.5f;
                plasmaJets.get(i).scale(jetScale, jetScale, jetScale);
                plasmaJets.get(i).interpolation(3, 0);
            }

            // Netherite center stays fixed at scale 1.0
            coreBlock.entity().teleport(c);

            // Constant: reverse portal from core
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c, 8, 0.3, 0.3, 0.3, 0.02);
            }

            // At expansion peak: explosion + nova-white flood
            if (cyclePos == 78) {
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 1, 0, 0, 0, 0);
                DisplayBuilder.dustParticles(c, 40, 8.0, 200, 180, 255, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }

            // On snap-reset
            if (cyclePos == 95) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.4f);
            }

            // Purple/cyan ring particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(c, ringRadius * 0.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.5f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidNova(plugin); }
    }

    // ================================================================
    // 82. BLACK ECLIPSE — Dark disc with corona ring, heartbeat pulse
    // ================================================================
    public static class BlackEclipse extends BlockDisplayAttack {

        private BlockDisplayHandle moonDisc;
        private BlockDisplayHandle eclipseCore;
        private final List<BlockDisplayHandle> coronaRing = new ArrayList<>();
        private final List<BlockDisplayHandle> prominences = new ArrayList<>();
        private final List<BlockDisplayHandle> coronaBleed = new ArrayList<>();

        public BlackEclipse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_eclipse", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Obsidian moon disc (large flat disc, 5 blocks wide)
            moonDisc = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.OBSIDIAN);
            moonDisc.scale(5.0f, 0.5f, 5.0f).glow(20, 0, 40).interpolation(3, 0);
            spawnedEntities.add(moonDisc.entity());

            // Netherite center — the light-null point
            eclipseCore = displayBuilder.spawnBlock(center.clone().add(0, 5.3, 0), Material.NETHERITE_BLOCK);
            eclipseCore.scale(1.0f, 0.6f, 1.0f).glow(10, 0, 20).interpolation(5, 0);
            spawnedEntities.add(eclipseCore.entity());

            // Corona ring: 16 magenta stained glass at 6-block radius
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 6, 5, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(0.6f, 0.4f, 0.6f).glow(200, 0, 50).interpolation(3, 0);
                coronaRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Plasma prominences: 4 amethyst at cardinal positions in corona
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                Location loc = center.clone().add(Math.cos(angle) * 6, 5.5, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.8f, 0.5f).glow(200, 100, 255).interpolation(3, 0);
                prominences.add(h);
                spawnedEntities.add(h.entity());
            }

            // Intercardinal dark prismarine
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 4) + (Math.PI / 2) * i;
                Location loc = center.clone().add(Math.cos(angle) * 6, 4.8, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Corona glow-bleed: 8 crying obsidian scattered behind disc
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double side = (i % 2 == 0) ? 0.5 : -0.5;
                Location loc = center.clone().add(Math.cos(angle) * 4, 5 + side, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                coronaBleed.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Moon disc: slow Z-axis rotation (0.2 deg/tick = 0.00349 rad/tick)
            float discRot = ticksAlive * 0.00349f;
            moonDisc.rotate(discRot, 0, 0, 1);
            moonDisc.interpolation(3, 0);

            // Corona ring: counter-rotate at 0.4 deg/tick
            float coronaRot = -ticksAlive * 0.00698f;
            for (int i = 0; i < coronaRing.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / coronaRing.size() + coronaRot;
                Location target = c.clone().add(Math.cos(baseAngle) * 6, 5, Math.sin(baseAngle) * 6);
                coronaRing.get(i).entity().teleport(target);
            }

            // Prominences: oscillate radially +/-0.5 blocks over 60 ticks
            for (int i = 0; i < prominences.size(); i++) {
                double baseAngle = (Math.PI / 2) * i + coronaRot;
                float radialOsc = (float) Math.sin((ticksAlive + i * 15) * 0.105f) * 0.5f;
                double radius = 6.0 + radialOsc;
                Location target = c.clone().add(Math.cos(baseAngle) * radius, 5.5, Math.sin(baseAngle) * radius);
                prominences.get(i).entity().teleport(target);
            }

            // Eclipse core: scale 1.0 → 0.0 over 200 ticks, snap to 1.0
            int coreCycle = ticksAlive % 200;
            float coreScale = 1.0f - (coreCycle / 200.0f);
            eclipseCore.scale(coreScale, coreScale * 0.6f, coreScale);
            eclipseCore.interpolation(5, 0);

            // Sound: when core reaches blackout
            if (coreCycle == 195) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.3f);
            }

            // Heartbeat every 20 ticks
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }

            // Corona particles: orange-red erupting from glass ring
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 5, 0), 6.0, Particle.DUST, 10,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.3f));
            }

            // Reverse portal from disc center
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 5.3, 0), 3, 0.2, 0.2, 0.2, 0.01);
            }

            // Ash trail drifting down from disc
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ASH, c.clone().add(0, 4, 0), 10, 3.0, 0.5, 3.0, 0);
            }

            // Purple glow-bleed pulse
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle bleed : coronaBleed) {
                    float pulse = 0.4f + (float) Math.sin(ticksAlive * 0.15) * 0.15f;
                    bleed.scale(pulse, pulse, pulse);
                    bleed.interpolation(4, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackEclipse(plugin); }
    }

    // ================================================================
    // 83. DEAD PLANET — Rotating sphere with dual ring system
    // ================================================================
    public static class DeadPlanet extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> planetBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> poleBlocks = new ArrayList<>();
        private BlockDisplayHandle gravityAnchor;

        public DeadPlanet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dead_planet", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location planetCenter = center.clone().add(0, 5, 0);

            // Planet sphere: 20 black concrete using golden angle distribution
            List<BlockDisplayHandle> sphere = displayBuilder.spawnSphere(planetCenter, Material.BLACK_CONCRETE, 4.0, 20);
            for (BlockDisplayHandle h : sphere) {
                h.scale(0.8f, 0.8f, 0.8f).glow(30, 25, 40).interpolation(3, 0);
                planetBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crater scars: 8 obsidian on surface
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 4.2;
                double y = Math.cos(phi) * 4.2;
                double z = Math.sin(phi) * Math.sin(theta) * 4.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(planetCenter.clone().add(x, y, z), Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(40, 0, 60).interpolation(3, 0);
                planetBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer netherite equatorial ring: 3 segments
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = planetCenter.clone().add(Math.cos(angle) * 5.5, 0, Math.sin(angle) * 5.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(1.5f, 0.3f, 0.6f).glow(20, 20, 30).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner prismarine ring: 3 segments, tilted plane
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = planetCenter.clone().add(Math.cos(angle) * 4.0, Math.sin(angle) * 1.2, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(1.0f, 0.2f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Poles: 4 crying obsidian
            Location northPole = planetCenter.clone().add(0, 4.3, 0);
            Location southPole = planetCenter.clone().add(0, -4.3, 0);
            for (int i = 0; i < 2; i++) {
                Location poleLoc = (i == 0) ? northPole : southPole;
                BlockDisplayHandle h = displayBuilder.spawnBlock(poleLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                poleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Gravity anchor at north pole
            gravityAnchor = displayBuilder.spawnBlock(northPole.clone().add(0, 0.5, 0), Material.BLACKSTONE);
            gravityAnchor.scale(0.4f, 0.4f, 0.4f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(gravityAnchor.entity());

            // Amethyst ring accents
            for (int i = 0; i < 2; i++) {
                double angle = Math.PI * i;
                Location loc = planetCenter.clone().add(Math.cos(angle) * 4.0, 0.6, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.3f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.4f, 0.25f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location pc = c.clone().add(0, 5, 0);

            // Planet Y-axis rotation: 0.4 deg/tick
            float planetRot = ticksAlive * 0.00698f;

            // Rotate planet sphere blocks by adjusting positions
            for (int i = 0; i < Math.min(planetBlocks.size(), 20); i++) {
                // Golden angle sphere points rotated by planetRot
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));
                double y = 1 - (2.0 * i / 19);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i + planetRot;
                double bx = Math.cos(theta) * radiusAtY * 4;
                double bz = Math.sin(theta) * radiusAtY * 4;
                planetBlocks.get(i).entity().teleport(pc.clone().add(bx, y * 4, bz));
            }

            // Outer ring: Y-axis at 0.8 deg/tick (faster than planet)
            float outerRingRot = ticksAlive * 0.01396f;
            for (int i = 0; i < outerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 3 + outerRingRot;
                Location target = pc.clone().add(Math.cos(angle) * 5.5, 0, Math.sin(angle) * 5.5);
                outerRing.get(i).entity().teleport(target);
                outerRing.get(i).rotate(outerRingRot, 0, 1, 0);
                outerRing.get(i).interpolation(3, 0);
            }

            // Inner ring: tilted axis (X=0.3, Y=1.0) at 1.2 deg/tick
            float innerRingRot = ticksAlive * 0.02094f;
            for (int i = 0; i < innerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 3 + innerRingRot;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                double tiltY = Math.sin(angle) * 1.2; // tilted plane offset
                Location target = pc.clone().add(x, tiltY, z);
                innerRing.get(i).entity().teleport(target);
                innerRing.get(i).rotate(innerRingRot * 0.3f, 0.3f, 1, 0);
                innerRing.get(i).interpolation(3, 0);
            }

            // Slow drift: translate 0.005 blocks/tick on X, snap back every 400 ticks
            float drift = (ticksAlive % 400) * 0.005f;
            // Applied to planet center as subtle drift (not needed for entities since they orbit)

            // Particles: dead-world grey dust
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(pc, 5, 4.0, 60, 55, 70, 1.0f);
            }

            // Falling obsidian tears from poles
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle pole : poleBlocks) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            pole.entity().getLocation(), 2, 0.2, 0.2, 0.2, 0);
                }
            }

            // Ash orbiting ring plane
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.particleRing(pc, 5.5, Particle.ASH, 6, null);
            }

            // Ambient sound loop
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BASALT_STEP, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeadPlanet(plugin); }
    }

    // ================================================================
    // 84. PULSAR BEAM — Fast-spinning neutron star with polar beam columns
    // ================================================================
    public static class PulsarBeam extends BlockDisplayAttack {

        private BlockDisplayHandle neutronCore;
        private final List<BlockDisplayHandle> equatorialDisc = new ArrayList<>();
        private final List<BlockDisplayHandle> northBeam = new ArrayList<>();
        private final List<BlockDisplayHandle> southBeam = new ArrayList<>();
        private final List<BlockDisplayHandle> nebulaRemnants = new ArrayList<>();
        private final List<BlockDisplayHandle> collimatorRings = new ArrayList<>();

        public PulsarBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pulsar_beam", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location coreCenter = center.clone().add(0, 4, 0);

            // Neutron star core: netherite
            neutronCore = displayBuilder.spawnBlock(coreCenter, Material.NETHERITE_BLOCK);
            neutronCore.scale(1.2f, 1.2f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(neutronCore.entity());

            // Equatorial disc: 8 obsidian at 2-block radius
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = coreCenter.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.6f, 0.3f, 0.6f).glow(60, 0, 120).interpolation(2, 0);
                equatorialDisc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Accreted matter: 6 crying obsidian around equatorial disc
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6 + 0.3;
                Location loc = coreCenter.clone().add(Math.cos(angle) * 2.5, 0.2, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.3f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // North polar beam: 2 amethyst + 2 black concrete + 2 dark prismarine collimator
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle beam = displayBuilder.spawnBlock(
                        coreCenter.clone().add(0, 2 + i * 1.5, 0), Material.AMETHYST_BLOCK);
                beam.scale(0.5f, 1.2f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                northBeam.add(beam);
                spawnedEntities.add(beam.entity());
            }
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        coreCenter.clone().add(0, 5 + i, 0), Material.BLACK_CONCRETE);
                cap.scale(0.4f, 0.6f, 0.4f).glow(40, 40, 60).interpolation(2, 0);
                northBeam.add(cap);
                spawnedEntities.add(cap.entity());
            }
            // North collimator
            BlockDisplayHandle nColl = displayBuilder.spawnBlock(coreCenter.clone().add(0, 1.8, 0), Material.DARK_PRISMARINE);
            nColl.scale(0.8f, 0.2f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            collimatorRings.add(nColl);
            spawnedEntities.add(nColl.entity());

            // South polar beam: mirror of north
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle beam = displayBuilder.spawnBlock(
                        coreCenter.clone().add(0, -2 - i * 1.5, 0), Material.AMETHYST_BLOCK);
                beam.scale(0.5f, 1.2f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                southBeam.add(beam);
                spawnedEntities.add(beam.entity());
            }
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        coreCenter.clone().add(0, -5 - i, 0), Material.BLACK_CONCRETE);
                cap.scale(0.4f, 0.6f, 0.4f).glow(40, 40, 60).interpolation(2, 0);
                southBeam.add(cap);
                spawnedEntities.add(cap.entity());
            }
            // South collimator
            BlockDisplayHandle sColl = displayBuilder.spawnBlock(coreCenter.clone().add(0, -1.8, 0), Material.DARK_PRISMARINE);
            sColl.scale(0.8f, 0.2f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            collimatorRings.add(sColl);
            spawnedEntities.add(sColl.entity());

            // Nebula remnants: 3 purple stained glass orbiting outer equator
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = coreCenter.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                nebulaRemnants.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location cc = c.clone().add(0, 4, 0);

            // Fast Y-axis rotation: 4 deg/tick = 0.0698 rad/tick
            float fastRot = ticksAlive * 0.0698f;

            // Rotate equatorial disc
            for (int i = 0; i < equatorialDisc.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + fastRot;
                Location target = cc.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                equatorialDisc.get(i).entity().teleport(target);
            }

            // Nebula remnants: orbit at 1 deg/tick
            float nebulaRot = ticksAlive * 0.01745f;
            for (int i = 0; i < nebulaRemnants.size(); i++) {
                double angle = (Math.PI * 2 * i) / 3 + nebulaRot;
                Location target = cc.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                nebulaRemnants.get(i).entity().teleport(target);
            }

            // Pulse every 30 ticks — beam blocks flash to scale 2.0
            int pulsePhase = ticksAlive % 30;
            boolean isPulsing = pulsePhase < 5;
            float beamScale = isPulsing ? (0.5f + (1.5f * (1.0f - pulsePhase / 5.0f))) : 0.5f;

            for (BlockDisplayHandle beam : northBeam) {
                beam.scale(beamScale, isPulsing ? 2.0f : 1.2f, beamScale);
                beam.interpolation(2, 0);
            }
            for (BlockDisplayHandle beam : southBeam) {
                beam.scale(beamScale, isPulsing ? 2.0f : 1.2f, beamScale);
                beam.interpolation(2, 0);
            }

            // End rod blasts from beam termini during pulse
            if (isPulsing) {
                Location northTip = cc.clone().add(0, 7, 0);
                Location southTip = cc.clone().add(0, -7, 0);
                c.getWorld().spawnParticle(Particle.END_ROD, northTip, 20, 0.3, 1.5, 0.3, 0.05);
                c.getWorld().spawnParticle(Particle.END_ROD, southTip, 20, 0.3, 1.5, 0.3, 0.05);
            }

            // Pulse sound
            if (pulsePhase == 0) {
                DisplayBuilder.playSound(cc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f);
            }

            // Continuous: pulsar-blue dust along beam columns
            if (ticksAlive % 2 == 0) {
                for (float y = -5; y <= 5; y += 1.5f) {
                    DisplayBuilder.dustParticles(cc.clone().add(0, y, 0), 3, 0.3, 0, 200, 255, 1.0f);
                }
            }

            // Reverse portal from core
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, cc, 4, 0.3, 0.3, 0.3, 0.02);
            }

            // Ambient beacon sound loop
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(cc, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PulsarBeam(plugin); }
    }

    // ================================================================
    // 85. STELLAR GRAVEYARD ORRERY — Triple-star system with orbiting dead worlds
    // ================================================================
    public static class StellarGraveyardOrrery extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tripleStars = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> planetaryDiscs = new ArrayList<>();
        private final List<BlockDisplayHandle> moonBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> mausoleumStars = new ArrayList<>();
        private final List<BlockDisplayHandle> accretionDisc = new ArrayList<>();
        private final List<BlockDisplayHandle> cometTails = new ArrayList<>();

        private final double[] orbitRadii = {3.0, 4.5, 6.0};
        private final double[] orbitSpeeds = {0.01745, 0.01222, 0.00873}; // 1, 0.7, 0.5 deg/tick in rad

        public StellarGraveyardOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stellar_graveyard_orrery", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location orrCenter = center.clone().add(0, 5, 0);

            // Triple-star remnant: 3 netherite in tight triangle
            double[][] starOffsets = {{0.4, 0, 0}, {-0.2, 0, 0.35}, {-0.2, 0, -0.35}};
            for (double[] off : starOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        orrCenter.clone().add(off[0], off[1], off[2]), Material.NETHERITE_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                tripleStars.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 planetary dead-world discs, 2 obsidian each, at different orbit radii
            for (int p = 0; p < 3; p++) {
                List<BlockDisplayHandle> disc = new ArrayList<>();
                double angle = (Math.PI * 2 * p) / 3;
                for (int b = 0; b < 2; b++) {
                    Location loc = orrCenter.clone().add(
                            Math.cos(angle) * orbitRadii[p] + b * 0.3, 0, Math.sin(angle) * orbitRadii[p]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.5f, 0.3f, 0.5f).glow(40, 40, 60).interpolation(2, 0);
                    disc.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Crying obsidian at leading edge
                Location leadLoc = orrCenter.clone().add(
                        Math.cos(angle) * orbitRadii[p] + 0.4, 0, Math.sin(angle) * orbitRadii[p]);
                BlockDisplayHandle lead = displayBuilder.spawnBlock(leadLoc, Material.CRYING_OBSIDIAN);
                lead.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                disc.add(lead);
                spawnedEntities.add(lead.entity());
                planetaryDiscs.add(disc);
            }

            // Moon for outermost planet: 3 dark prismarine
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        orrCenter.clone().add(orbitRadii[2] + 1, 0, 0), Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                moonBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Accretion disc: 6 black concrete in flat halo
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = orrCenter.clone().add(Math.cos(angle) * 1.5, -0.1, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.6f, 0.15f, 0.6f).glow(20, 10, 30).interpolation(2, 0);
                accretionDisc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Mausoleum stars: 3 amethyst between orbits
            double[] mausRadii = {3.75, 5.25, 2.0};
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3 + Math.PI / 6;
                Location loc = orrCenter.clone().add(Math.cos(angle) * mausRadii[i], 1.5, Math.sin(angle) * mausRadii[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(2, 0);
                mausoleumStars.add(h);
                spawnedEntities.add(h.entity());
            }

            // Comet tails: 4 blackstone (2 comets, 2 blocks each)
            for (int comet = 0; comet < 2; comet++) {
                for (int t = 0; t < 2; t++) {
                    double angle = Math.PI * comet + t * 0.3;
                    Location loc = orrCenter.clone().add(Math.cos(angle) * 7, 0.5, Math.sin(angle) * 7);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.3f, 0.2f, 0.8f).glow(50, 50, 60).interpolation(2, 0);
                    cometTails.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.05f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location oc = c.clone().add(0, 5, 0);

            // Triple-star center rotation: 2 deg/tick
            float starRot = ticksAlive * 0.0349f;
            double[][] starOffsets = {{0.4, 0, 0}, {-0.2, 0, 0.35}, {-0.2, 0, -0.35}};
            for (int i = 0; i < tripleStars.size(); i++) {
                double angle = starRot + (Math.PI * 2 * i) / 3;
                double radius = 0.4;
                Location target = oc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                tripleStars.get(i).entity().teleport(target);
            }

            // Planetary disc orbits at different speeds
            for (int p = 0; p < 3; p++) {
                double baseAngle = ticksAlive * orbitSpeeds[p] + (Math.PI * 2 * p) / 3;
                List<BlockDisplayHandle> disc = planetaryDiscs.get(p);
                for (int b = 0; b < disc.size(); b++) {
                    double offset = b * 0.15;
                    Location target = oc.clone().add(
                            Math.cos(baseAngle) * (orbitRadii[p] + offset), 0,
                            Math.sin(baseAngle) * (orbitRadii[p] + offset));
                    disc.get(b).entity().teleport(target);
                }

                // Moon orbits outermost planet at 2 deg/tick
                if (p == 2) {
                    double planetX = Math.cos(baseAngle) * orbitRadii[2];
                    double planetZ = Math.sin(baseAngle) * orbitRadii[2];
                    float moonRot = ticksAlive * 0.0349f;
                    for (int m = 0; m < moonBlocks.size(); m++) {
                        double moonAngle = moonRot + (Math.PI * 2 * m) / 3;
                        Location mTarget = oc.clone().add(
                                planetX + Math.cos(moonAngle) * 1.0, 0.3,
                                planetZ + Math.sin(moonAngle) * 1.0);
                        moonBlocks.get(m).entity().teleport(mTarget);
                    }
                }
            }

            // Mausoleum stars: orbit in higher inclined plane at 0.3 deg/tick
            float mausRot = ticksAlive * 0.00524f;
            double[] mausRadii = {3.75, 5.25, 2.0};
            for (int i = 0; i < mausoleumStars.size(); i++) {
                double angle = mausRot + (Math.PI * 2 * i) / 3;
                double inclinedY = Math.sin(angle * 0.5) * 2.0; // 60-degree inclined plane
                Location target = oc.clone().add(
                        Math.cos(angle) * mausRadii[i], 1.5 + inclinedY,
                        Math.sin(angle) * mausRadii[i]);
                mausoleumStars.get(i).entity().teleport(target);
            }

            // Accretion disc slow rotation
            float accRot = ticksAlive * 0.01f;
            for (int i = 0; i < accretionDisc.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + accRot;
                Location target = oc.clone().add(Math.cos(angle) * 1.5, -0.1, Math.sin(angle) * 1.5);
                accretionDisc.get(i).entity().teleport(target);
            }

            // Particles: stellar-graveyard cold dust
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(oc, 12, 6.0, 40, 40, 80, 0.8f);
            }

            // Falling obsidian tears from mausoleum stars
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle star : mausoleumStars) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            star.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Ash in accretion disc plane
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.particleRing(oc, 1.5, Particle.ASH, 4, null);
            }

            // Funeral toll every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(oc, Sound.BLOCK_BELL_USE, 0.3f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StellarGraveyardOrrery(plugin); }
    }

    // ================================================================
    // 86. SINGULARITY — Concentric rings spiraling inward, gravitational pull
    // ================================================================
    public static class Singularity extends BlockDisplayAttack {

        private BlockDisplayHandle singularityCore;
        private BlockDisplayHandle photonSphere;
        private final List<List<BlockDisplayHandle>> concentricRings = new ArrayList<>();
        private final List<BlockDisplayHandle> lastStableOrbit = new ArrayList<>();
        private final double[] ringRadii = {1.0, 2.0, 3.0, 4.0, 5.0};
        private final double[] ringSpeeds = {0.0698, 0.0349, 0.02094, 0.01396, 0.00873};

        public Singularity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("singularity", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location sc = center.clone().add(0, 3, 0);

            // The singularity: netherite at absolute center, perfectly still
            singularityCore = displayBuilder.spawnBlock(sc, Material.NETHERITE_BLOCK);
            singularityCore.scale(0.6f, 0.6f, 0.6f).glow(10, 0, 20).interpolation(1, 0);
            spawnedEntities.add(singularityCore.entity());

            // Photon sphere: dark prismarine between center and innermost ring
            photonSphere = displayBuilder.spawnBlock(sc.clone().add(0.5, 0, 0), Material.DARK_PRISMARINE);
            photonSphere.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(photonSphere.entity());

            // 5 concentric rings of 4 blocks each
            for (int ring = 0; ring < 5; ring++) {
                List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
                for (int b = 0; b < 4; b++) {
                    double angle = (Math.PI * 2 * b) / 4;
                    Location loc = sc.clone().add(Math.cos(angle) * ringRadii[ring], 0, Math.sin(angle) * ringRadii[ring]);
                    Material mat;
                    if (ring == 4) {
                        mat = Material.OBSIDIAN; // outermost: event horizon markers
                    } else if (ring == 2 && (b == 0 || b == 2)) {
                        mat = Material.CRYING_OBSIDIAN; // 3rd ring cardinals
                    } else {
                        mat = Material.BLACK_CONCRETE;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float blockScale = 0.4f + ring * 0.1f;
                    h.scale(blockScale, 0.3f, blockScale).glow(128, 0, 255).interpolation(2, 0);
                    ringBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
                concentricRings.add(ringBlocks);
            }

            // Last stable orbit: 2 amethyst at 1.5-block radius
            for (int i = 0; i < 2; i++) {
                double angle = Math.PI * i;
                Location loc = sc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
                lastStableOrbit.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location sc = c.clone().add(0, 3, 0);

            // Singularity stays perfectly still
            singularityCore.entity().teleport(sc);

            // Photon sphere orbit
            float photonRot = ticksAlive * 0.0873f; // 5 deg/tick
            photonSphere.entity().teleport(sc.clone().add(
                    Math.cos(photonRot) * 0.7, 0, Math.sin(photonRot) * 0.7));

            // Each ring rotates at speed inversely proportional to radius
            // Also slowly spiral inward (shrink radius by 0.001/tick), snap back every 200 ticks
            float spiralFactor = (ticksAlive % 200) * 0.001f;

            for (int ring = 0; ring < 5; ring++) {
                double currentRadius = ringRadii[ring] - spiralFactor * ringRadii[ring] * 0.5;
                float rot = (float) (ticksAlive * ringSpeeds[ring]);
                List<BlockDisplayHandle> ringBlocks = concentricRings.get(ring);
                for (int b = 0; b < ringBlocks.size(); b++) {
                    double angle = (Math.PI * 2 * b) / 4 + rot;
                    Location target = sc.clone().add(
                            Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius);
                    ringBlocks.get(b).entity().teleport(target);
                }
            }

            // Last stable orbit: fastest at 6 deg/tick
            float lsoRot = ticksAlive * 0.1047f;
            for (int i = 0; i < lastStableOrbit.size(); i++) {
                double angle = lsoRot + Math.PI * i;
                lastStableOrbit.get(i).entity().teleport(sc.clone().add(
                        Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5));
            }

            // Particles: ALL pointing inward — reverse portal from every ring block
            if (ticksAlive % 2 == 0) {
                for (List<BlockDisplayHandle> ring : concentricRings) {
                    for (BlockDisplayHandle h : ring) {
                        Location bLoc = h.entity().getLocation();
                        // Direction vector toward center
                        double dx = sc.getX() - bLoc.getX();
                        double dz = sc.getZ() - bLoc.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.1) {
                            c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, bLoc, 2,
                                    dx / dist * 0.3, 0.05, dz / dist * 0.3, 0.02);
                        }
                    }
                }
            }

            // Falling obsidian tears from all blocks
            if (ticksAlive % 4 == 0) {
                for (List<BlockDisplayHandle> ring : concentricRings) {
                    for (BlockDisplayHandle h : ring) {
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                                h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // Sonic boom pulse every 60 ticks — breaking silence
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(sc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.2f);
            }

            // Sculk spread continuous
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(sc, Sound.BLOCK_SCULK_SPREAD, 0.2f, 0.1f);
            }

            // Purple dust vortex spiraling inward
            if (ticksAlive % 3 == 0) {
                float dustAngle = ticksAlive * 0.15f;
                for (int i = 0; i < 8; i++) {
                    double a = dustAngle + (Math.PI * 2 * i) / 8;
                    double r = 5.0 - (ticksAlive % 60) * 0.08;
                    if (r < 0.5) r = 0.5;
                    Location dustLoc = sc.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    DisplayBuilder.purpleDust(dustLoc, 2, 0.2);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Singularity(plugin); }
    }

    // ================================================================
    // 87. DYING STAR — Pulsing red giant with expanding shell and polar jets
    // ================================================================
    public static class DyingStar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stellarCore = new ArrayList<>();
        private final List<BlockDisplayHandle> outerShell = new ArrayList<>();
        private final List<BlockDisplayHandle> magentaSurface = new ArrayList<>();
        private final List<BlockDisplayHandle> sunspots = new ArrayList<>();
        private final List<BlockDisplayHandle> polarJets = new ArrayList<>();
        private BlockDisplayHandle nucleus;

        public DyingStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dying_star", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(14.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(560);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location sc = center.clone().add(0, 5, 0);

            // Nucleus: dark prismarine at absolute center
            nucleus = displayBuilder.spawnBlock(sc, Material.DARK_PRISMARINE);
            nucleus.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(nucleus.entity());

            // Dense netherite stellar core: 12 blocks in sphere
            List<BlockDisplayHandle> coreBlocks = displayBuilder.spawnSphere(sc, Material.NETHERITE_BLOCK, 2.0, 12);
            for (BlockDisplayHandle h : coreBlocks) {
                h.scale(0.7f, 0.7f, 0.7f).glow(200, 0, 50).interpolation(3, 0);
                stellarCore.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer crying obsidian shells: 16 at radii 3 and 5
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 3;
                double y = Math.cos(phi) * 3;
                double z = Math.sin(phi) * Math.sin(theta) * 3;
                BlockDisplayHandle h = displayBuilder.spawnBlock(sc.clone().add(x, y, z), Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                outerShell.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + 0.5;
                double x = Math.sin(phi) * Math.cos(theta) * 5;
                double y = Math.cos(phi) * 5;
                double z = Math.sin(phi) * Math.sin(theta) * 5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(sc.clone().add(x, y, z), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                outerShell.add(h);
                spawnedEntities.add(h.entity());
            }

            // Magenta stained glass outer visible surface: 8 at 6-block radius
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + 1.0;
                double x = Math.sin(phi) * Math.cos(theta) * 6;
                double y = Math.cos(phi) * 6;
                double z = Math.sin(phi) * Math.sin(theta) * 6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(sc.clone().add(x, y, z), Material.MAGENTA_STAINED_GLASS);
                h.scale(0.7f, 0.7f, 0.7f).glow(200, 0, 50).interpolation(3, 0);
                magentaSurface.add(h);
                spawnedEntities.add(h.entity());
            }

            // Purple stained glass planetary nebula inner ring: 4
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = sc.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Dark obsidian sunspots: 6 on stellar surface
            for (int i = 0; i < 6; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 6);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + 2.0;
                double x = Math.sin(phi) * Math.cos(theta) * 2.5;
                double y = Math.cos(phi) * 2.5;
                double z = Math.sin(phi) * Math.sin(theta) * 2.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(sc.clone().add(x, y, z), Material.OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(30, 0, 40).interpolation(3, 0);
                sunspots.add(h);
                spawnedEntities.add(h.entity());
            }

            // Polar jets: 2 amethyst at poles
            BlockDisplayHandle northJet = displayBuilder.spawnBlock(sc.clone().add(0, 3, 0), Material.AMETHYST_BLOCK);
            northJet.scale(0.4f, 1.0f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
            polarJets.add(northJet);
            spawnedEntities.add(northJet.entity());

            BlockDisplayHandle southJet = displayBuilder.spawnBlock(sc.clone().add(0, -3, 0), Material.AMETHYST_BLOCK);
            southJet.scale(0.4f, 1.0f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
            polarJets.add(southJet);
            spawnedEntities.add(southJet.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location sc = c.clone().add(0, 5, 0);

            // Stellar pulse: scale 1.0 → 1.6 → 1.0 over 80 ticks
            int pulseCycle = ticksAlive % 80;
            float pulseScale = 1.0f + (float) Math.sin(pulseCycle / 80.0 * Math.PI * 2) * 0.3f;

            // Core blocks scale with pulse
            for (int i = 0; i < stellarCore.size(); i++) {
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));
                double y = 1 - (2.0 * i / Math.max(1, stellarCore.size() - 1));
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i + ticksAlive * 0.01;
                double bx = Math.cos(theta) * radiusAtY * 2.0 * pulseScale;
                double bz = Math.sin(theta) * radiusAtY * 2.0 * pulseScale;
                stellarCore.get(i).entity().teleport(sc.clone().add(bx, y * 2.0 * pulseScale, bz));
                stellarCore.get(i).scale(0.7f * pulseScale, 0.7f * pulseScale, 0.7f * pulseScale);
                stellarCore.get(i).interpolation(3, 0);
            }

            // Outer shell: expanding slightly lagging behind (15 tick lag)
            int laggedCycle = (ticksAlive + 65) % 80; // 15 tick lag
            float outerPulse = 1.0f + (float) Math.sin(laggedCycle / 80.0 * Math.PI * 2) * 0.3f;

            for (int i = 0; i < outerShell.size(); i++) {
                double baseRadius = (i < 8) ? 3.0 : 5.0;
                int idx = i % 8;
                double phi = Math.acos(1 - 2.0 * (idx + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * idx + ((i < 8) ? 0 : 0.5);
                double x = Math.sin(phi) * Math.cos(theta) * baseRadius * outerPulse;
                double y = Math.cos(phi) * baseRadius * outerPulse;
                double z = Math.sin(phi) * Math.sin(theta) * baseRadius * outerPulse;
                outerShell.get(i).entity().teleport(sc.clone().add(x, y, z));
            }

            // Magenta surface: slightly behind outer shell
            float surfacePulse = 1.0f + (float) Math.sin(((ticksAlive + 60) % 80) / 80.0 * Math.PI * 2) * 0.3f;
            for (int i = 0; i < magentaSurface.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + 1.0;
                double x = Math.sin(phi) * Math.cos(theta) * 6 * surfacePulse;
                double y = Math.cos(phi) * 6 * surfacePulse;
                double z = Math.sin(phi) * Math.sin(theta) * 6 * surfacePulse;
                magentaSurface.get(i).entity().teleport(sc.clone().add(x, y, z));
            }

            // Sunspots drift: 0.3 deg/tick across surface
            float sunspotRot = ticksAlive * 0.00524f;
            for (int i = 0; i < sunspots.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 6);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + 2.0 + sunspotRot;
                double x = Math.sin(phi) * Math.cos(theta) * 2.5 * pulseScale;
                double y = Math.cos(phi) * 2.5 * pulseScale;
                double z = Math.sin(phi) * Math.sin(theta) * 2.5 * pulseScale;
                sunspots.get(i).entity().teleport(sc.clone().add(x, y, z));
            }

            // Polar jets: extend further on pulse (1.0 → 2.0), retract to 1.2
            float jetScale = 1.0f + (float) Math.max(0, Math.sin(pulseCycle / 80.0 * Math.PI * 2)) * 1.0f;
            float jetRestScale = Math.max(1.2f, jetScale);
            polarJets.get(0).scale(0.4f, jetRestScale, 0.4f);
            polarJets.get(0).entity().teleport(sc.clone().add(0, 3 * jetRestScale, 0));
            polarJets.get(0).interpolation(3, 0);
            polarJets.get(1).scale(0.4f, jetRestScale, 0.4f);
            polarJets.get(1).entity().teleport(sc.clone().add(0, -3 * jetRestScale, 0));
            polarJets.get(1).interpolation(3, 0);

            // Reverse portal from polar jets during pulse
            if (pulseScale > 1.2f) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, sc.clone().add(0, 4, 0), 5, 0.2, 0.5, 0.2, 0.02);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, sc.clone().add(0, -4, 0), 5, 0.2, 0.5, 0.2, 0.02);
            }

            // Lava eruptions from outer surface
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, sc, 8, 4.0, 4.0, 4.0, 0);
            }

            // Stellar-red dust covering structure
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.crimsonDust(sc, 15, 5.0);
            }

            // Fire ambient sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(sc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DyingStar(plugin); }
    }

    // ================================================================
    // 88. WANDERING BLACK HOLE — Moving accretion disc with relativistic jets
    // ================================================================
    public static class WanderingBlackHole extends BlockDisplayAttack {

        private BlockDisplayHandle blackHole;
        private final List<BlockDisplayHandle> accretionDisc = new ArrayList<>();
        private final List<BlockDisplayHandle> northJet = new ArrayList<>();
        private final List<BlockDisplayHandle> southJet = new ArrayList<>();
        private final List<BlockDisplayHandle> photonHighlights = new ArrayList<>();
        private final List<BlockDisplayHandle> lensingHalo = new ArrayList<>();
        private final List<BlockDisplayHandle> jetCollimators = new ArrayList<>();

        public WanderingBlackHole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wandering_black_hole", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(550);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location bc = center.clone().add(0, 4, 0);

            // The black hole itself: obsidian at center
            blackHole = displayBuilder.spawnBlock(bc, Material.OBSIDIAN);
            blackHole.scale(0.8f, 0.8f, 0.8f).glow(10, 0, 10).interpolation(2, 0);
            spawnedEntities.add(blackHole.entity());

            // Accretion disc: 12 black concrete at 2.5-block radius
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = bc.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
                Material mat = (i < 4) ? Material.CRYING_OBSIDIAN : Material.BLACK_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.2f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                accretionDisc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Photon ring highlights: 4 amethyst at inner disc edge
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                Location loc = bc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.25f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
                photonHighlights.add(h);
                spawnedEntities.add(h.entity());
            }

            // Relativistic jets: 3 dark prismarine per pole
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle n = displayBuilder.spawnBlock(bc.clone().add(0, 2 + i * 1.5, 0), Material.DARK_PRISMARINE);
                n.scale(0.4f, 1.0f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                northJet.add(n);
                spawnedEntities.add(n.entity());

                BlockDisplayHandle s = displayBuilder.spawnBlock(bc.clone().add(0, -2 - i * 1.5, 0), Material.DARK_PRISMARINE);
                s.scale(0.4f, 1.0f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                southJet.add(s);
                spawnedEntities.add(s.entity());
            }

            // Jet collimation rings: 4 netherite (2 per jet base)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle nc = displayBuilder.spawnBlock(bc.clone().add(0, 1.5 + i * 0.4, 0), Material.NETHERITE_BLOCK);
                nc.scale(0.7f, 0.2f, 0.7f).glow(20, 20, 30).interpolation(2, 0);
                jetCollimators.add(nc);
                spawnedEntities.add(nc.entity());

                BlockDisplayHandle sc2 = displayBuilder.spawnBlock(bc.clone().add(0, -1.5 - i * 0.4, 0), Material.NETHERITE_BLOCK);
                sc2.scale(0.7f, 0.2f, 0.7f).glow(20, 20, 30).interpolation(2, 0);
                jetCollimators.add(sc2);
                spawnedEntities.add(sc2.entity());
            }

            // Gravitational lensing halo: 6 purple stained glass at 3.5 radius
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = bc.clone().add(Math.cos(angle) * 3.5, 0.3, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                lensingHalo.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lissajous wandering path: 4x4 block area, 500-tick period
            double lissT = (ticksAlive % 500) / 500.0 * Math.PI * 2;
            double wanderX = Math.sin(lissT * 3) * 2.0;
            double wanderZ = Math.cos(lissT * 2) * 2.0;
            Location bc = c.clone().add(wanderX, 4, wanderZ);

            // Update center for damage calculations
            setCenter(bc.clone());

            // Black hole follows lissajous
            blackHole.entity().teleport(bc);

            // Fast disc rotation: 2 deg/tick
            float discRot = ticksAlive * 0.0349f;

            // Accretion disc rotates with black hole center
            for (int i = 0; i < accretionDisc.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + discRot;
                Location target = bc.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
                accretionDisc.get(i).entity().teleport(target);
            }

            // Photon highlights follow
            for (int i = 0; i < photonHighlights.size(); i++) {
                double angle = (Math.PI / 2) * i + discRot;
                Location target = bc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                photonHighlights.get(i).entity().teleport(target);
            }

            // Jets remain vertical, move with black hole
            for (int i = 0; i < northJet.size(); i++) {
                northJet.get(i).entity().teleport(bc.clone().add(0, 2 + i * 1.5, 0));
                southJet.get(i).entity().teleport(bc.clone().add(0, -2 - i * 1.5, 0));
            }

            // Collimators follow
            for (int i = 0; i < jetCollimators.size(); i++) {
                double yOff = (i < 2) ? (1.5 + (i % 2) * 0.4) : (-1.5 - (i % 2) * 0.4);
                jetCollimators.get(i).entity().teleport(bc.clone().add(0, yOff, 0));
            }

            // Lensing halo: counter-rotate at 1 deg/tick
            float haloRot = -ticksAlive * 0.01745f;
            for (int i = 0; i < lensingHalo.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + haloRot;
                Location target = bc.clone().add(Math.cos(angle) * 3.5, 0.3, Math.sin(angle) * 3.5);
                lensingHalo.get(i).entity().teleport(target);
            }

            // Massive reverse portal inflow from center
            if (ticksAlive % 1 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, bc, 20, 0.5, 0.5, 0.5, 0.05);
            }

            // End rod from jet tops
            if (ticksAlive % 3 == 0) {
                Location jetTop = bc.clone().add(0, 6.5, 0);
                Location jetBottom = bc.clone().add(0, -6.5, 0);
                c.getWorld().spawnParticle(Particle.END_ROD, jetTop, 6, 0.2, 0.5, 0.2, 0.03);
                c.getWorld().spawnParticle(Particle.END_ROD, jetBottom, 6, 0.2, 0.5, 0.2, 0.03);
            }

            // Accretion-orange dust on disc
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(bc, 8, 2.5, 200, 100, 0, 1.2f);
            }

            // Heartbeat constant
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(bc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }

            // Portal travel sound on movement peaks
            if (ticksAlive % 125 == 0) {
                DisplayBuilder.playSound(bc, Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WanderingBlackHole(plugin); }
    }

    // ================================================================
    // 89. DEEP FIELD — 30 scattered galaxy-points in a vast cube, silent cosmos
    // ================================================================
    public static class DeepField extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> galaxyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> brightGalaxies = new ArrayList<>();
        private final List<BlockDisplayHandle> anomalies = new ArrayList<>();
        private final List<BlockDisplayHandle> deepObjects = new ArrayList<>();
        private BlockDisplayHandle observerBlock;
        private boolean halfwaySoundPlayed = false;

        // Store random positions for galaxies (generated once)
        private final double[][] galaxyPositions = new double[30][3];

        public DeepField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_field", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location dc = center.clone().add(0, 5, 0);

            // Generate random positions for the 30 galaxy blocks in 10x10x10 cube
            java.util.Random rand = new java.util.Random(center.hashCode());
            for (int i = 0; i < 30; i++) {
                galaxyPositions[i][0] = (rand.nextDouble() - 0.5) * 10;
                galaxyPositions[i][1] = (rand.nextDouble() - 0.5) * 10;
                galaxyPositions[i][2] = (rand.nextDouble() - 0.5) * 10;
            }

            // 30 black concrete galaxy-points
            for (int i = 0; i < 30; i++) {
                Location loc = dc.clone().add(galaxyPositions[i][0], galaxyPositions[i][1], galaxyPositions[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.2f, 0.2f, 0.2f).glow(60, 50, 80).interpolation(5, 0);
                galaxyBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 bright amethyst galaxies at specific positions
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 4;
                double y = Math.cos(phi) * 4;
                double z = Math.sin(phi) * Math.sin(theta) * 4;
                Location loc = dc.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(5, 0);
                brightGalaxies.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 crying obsidian anomalies at volume edge
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                Location loc = dc.clone().add(Math.cos(angle) * 4.5, 0, Math.sin(angle) * 4.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.25f, 0.25f, 0.25f).glow(128, 0, 255).interpolation(5, 0);
                anomalies.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 foreground dark prismarine
            for (int i = 0; i < 2; i++) {
                Location loc = dc.clone().add(i * 2 - 1, 0, 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
            }

            // 2 netherite deep objects at center — the most ancient
            for (int i = 0; i < 2; i++) {
                Location loc = dc.clone().add(i * 0.5 - 0.25, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(10, 0, 20).interpolation(5, 0);
                deepObjects.add(h);
                spawnedEntities.add(h.entity());
            }

            // Observer block: obsidian slightly outside, in front
            observerBlock = displayBuilder.spawnBlock(dc.clone().add(0, 0, 6), Material.OBSIDIAN);
            observerBlock.scale(0.4f, 0.4f, 0.4f).glow(60, 60, 80).interpolation(2, 0);
            spawnedEntities.add(observerBlock.entity());

            // Deep field is silent at spawn — no sound
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location dc = c.clone().add(0, 5, 0);

            // Entire field: barely perceptible Y rotation at 0.05 deg/tick
            float fieldRot = ticksAlive * 0.000873f;

            // Rotate all 30 galaxy positions
            for (int i = 0; i < galaxyBlocks.size(); i++) {
                double ox = galaxyPositions[i][0];
                double oz = galaxyPositions[i][2];
                double rx = ox * Math.cos(fieldRot) - oz * Math.sin(fieldRot);
                double rz = ox * Math.sin(fieldRot) + oz * Math.cos(fieldRot);
                Location target = dc.clone().add(rx, galaxyPositions[i][1], rz);
                galaxyBlocks.get(i).entity().teleport(target);
            }

            // Bright galaxies: twinkling — scale 1.0 → 1.2 with 50-tick phase offsets
            for (int i = 0; i < brightGalaxies.size(); i++) {
                float twinkle = 0.3f + (float) Math.sin((ticksAlive + i * 50) * 0.126f) * 0.06f;
                brightGalaxies.get(i).scale(twinkle, twinkle, twinkle);
                brightGalaxies.get(i).interpolation(5, 0);
            }

            // Deep netherite objects: very slow scale 0.3 → 0.6, 500-tick cycle
            for (int i = 0; i < deepObjects.size(); i++) {
                float deepScale = 0.3f + (float) Math.sin(ticksAlive * 0.00628f) * 0.15f;
                deepObjects.get(i).scale(deepScale, deepScale, deepScale);
                deepObjects.get(i).interpolation(5, 0);
            }

            // Observer block: spins at 3 deg/tick
            float obsRot = ticksAlive * 0.05236f;
            observerBlock.rotate(obsRot, 0, 1, 0);
            observerBlock.interpolation(2, 0);

            // Very sparse particles: end rod from galaxies, 1 every 40 ticks each
            if (ticksAlive % 40 == 0) {
                int galaxyIdx = (ticksAlive / 40) % galaxyBlocks.size();
                Location gLoc = galaxyBlocks.get(galaxyIdx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.END_ROD, gLoc, 1, 0, 0, 0, 0);
            }

            // Faint cosmological void dust
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.dustParticles(dc, 1, 5.0, 2, 0, 8, 0.5f);
            }

            // Single dragon growl at halfway point
            int halfway = config.getDurationTicks() / 2;
            if (ticksAlive >= halfway && !halfwaySoundPlayed) {
                halfwaySoundPlayed = true;
                DisplayBuilder.playSound(dc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.2f, 0.1f);
            }

            // Purple ambient dust ring (very sparse)
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.darkPurpleDust(dc, 2, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepField(plugin); }
    }

    // ================================================================
    // 90. COSMIC WEB — 3D filament network with super-cluster nodes
    // ================================================================
    public static class CosmicWeb extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> filaments = new ArrayList<>();
        private final List<BlockDisplayHandle> webNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> superClusterNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> condensedMatter = new ArrayList<>();
        private final List<BlockDisplayHandle> voidMarkers = new ArrayList<>();
        private BlockDisplayHandle cosmicSpine;
        private final double[][] voidMarkerOriginal = new double[6][3];

        // 8 filament spoke directions (varying 3D angles)
        private static final double[][] SPOKE_DIRS = {
            {1, 0.3, 0}, {-1, -0.2, 0.5}, {0.5, 0.5, 1}, {-0.5, 0.4, -1},
            {0, -0.5, 1}, {0.7, 0.7, -0.7}, {-0.7, -0.3, -0.5}, {0.3, -0.6, 0.8}
        };

        public CosmicWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_web", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(550);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wc = center.clone().add(0, 5, 0);

            // Cosmic spine: black glazed terracotta at absolute center
            cosmicSpine = displayBuilder.spawnBlock(wc, Material.BLACK_GLAZED_TERRACOTTA);
            cosmicSpine.scale(0.5f, 0.5f, 0.5f).glow(60, 30, 80).interpolation(3, 0);
            spawnedEntities.add(cosmicSpine.entity());

            // 8 filament spokes, 3 blackstone blocks each
            for (int f = 0; f < 8; f++) {
                List<BlockDisplayHandle> filament = new ArrayList<>();
                double[] dir = SPOKE_DIRS[f];
                double len = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
                double nx = dir[0] / len, ny = dir[1] / len, nz = dir[2] / len;

                for (int b = 1; b <= 3; b++) {
                    double dist = b * 2.0;
                    Location loc = wc.clone().add(nx * dist, ny * dist, nz * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.3f, 0.3f, 0.3f).glow(40, 20, 60).interpolation(3, 0);
                    filament.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Obsidian node at filament midpoint
                double midDist = 3.0;
                Location midLoc = wc.clone().add(nx * midDist, ny * midDist, nz * midDist);
                BlockDisplayHandle node = displayBuilder.spawnBlock(midLoc, Material.OBSIDIAN);
                node.scale(0.4f, 0.4f, 0.4f).glow(60, 0, 100).interpolation(3, 0);
                webNodes.add(node);
                spawnedEntities.add(node.entity());

                filaments.add(filament);
            }

            // 3 super-cluster nodes: netherite at the thickest intersections
            double[][] scPositions = {{3, 1, 2}, {-2, -1, 3}, {1, 2, -3}};
            for (double[] pos : scPositions) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(wc.clone().add(pos[0], pos[1], pos[2]), Material.NETHERITE_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                superClusterNodes.add(h);
                spawnedEntities.add(h.entity());

                // Second netherite per super-cluster
                BlockDisplayHandle h2 = displayBuilder.spawnBlock(
                        wc.clone().add(pos[0] + 0.4, pos[1], pos[2] + 0.3), Material.NETHERITE_BLOCK);
                h2.scale(0.5f, 0.5f, 0.5f).glow(100, 0, 200).interpolation(3, 0);
                spawnedEntities.add(h2.entity());
            }

            // 4 amethyst condensed matter near super-cluster nodes
            for (int i = 0; i < 4; i++) {
                int scIdx = i % 3;
                Location loc = wc.clone().add(
                        scPositions[scIdx][0] + (i * 0.3), scPositions[scIdx][1] + 0.5, scPositions[scIdx][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(3, 0);
                condensedMatter.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 void markers: crying obsidian in the empty spaces between filaments
            double[][] voidPositions = {
                {2, 2, 2}, {-2, 2, -2}, {2, -2, -2}, {-2, -2, 2}, {3, 0, -1}, {-1, 0, 3}
            };
            for (int i = 0; i < 6; i++) {
                voidMarkerOriginal[i] = voidPositions[i].clone();
                Location loc = wc.clone().add(voidPositions[i][0], voidPositions[i][1], voidPositions[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.35f, 0.35f, 0.35f).glow(128, 0, 255).interpolation(3, 0);
                voidMarkers.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 galaxy group cores: dark prismarine
            for (int i = 0; i < 2; i++) {
                Location loc = wc.clone().add(i * 4 - 2, 1, i * 2 - 1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.3f, 0.08f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location wc = c.clone().add(0, 5, 0);

            // Y-axis rotation: 0.15 deg/tick
            float yRot = ticksAlive * 0.002618f;
            // X-axis rotation: 0.07 deg/tick — slow tumble
            float xRot = ticksAlive * 0.001222f;

            // Apply rotation to all filament blocks (manual rotation around center)
            for (int f = 0; f < filaments.size(); f++) {
                double[] dir = SPOKE_DIRS[f];
                double len = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
                double nx = dir[0] / len, ny = dir[1] / len, nz = dir[2] / len;

                for (int b = 0; b < filaments.get(f).size(); b++) {
                    double dist = (b + 1) * 2.0;
                    double ox = nx * dist, oy = ny * dist, oz = nz * dist;
                    // Apply Y rotation
                    double rx = ox * Math.cos(yRot) - oz * Math.sin(yRot);
                    double rz = ox * Math.sin(yRot) + oz * Math.cos(yRot);
                    // Apply X rotation
                    double ry = oy * Math.cos(xRot) - rz * Math.sin(xRot);
                    double rz2 = oy * Math.sin(xRot) + rz * Math.cos(xRot);

                    Location target = wc.clone().add(rx, ry, rz2);
                    filaments.get(f).get(b).entity().teleport(target);
                }

                // Web nodes at midpoints
                if (f < webNodes.size()) {
                    double midDist = 3.0;
                    double ox = nx * midDist, oy = ny * midDist, oz = nz * midDist;
                    double rx = ox * Math.cos(yRot) - oz * Math.sin(yRot);
                    double rz = ox * Math.sin(yRot) + oz * Math.cos(yRot);
                    double ry = oy * Math.cos(xRot) - rz * Math.sin(xRot);
                    double rz2 = oy * Math.sin(xRot) + rz * Math.cos(xRot);
                    webNodes.get(f).entity().teleport(wc.clone().add(rx, ry, rz2));
                }
            }

            // Super-cluster node pulse: scale 1.0 → 1.4, 150-tick cycle, 50-tick phase offsets
            for (int i = 0; i < superClusterNodes.size(); i++) {
                float pulse = 0.6f + (float) Math.sin((ticksAlive + i * 50) * 0.0419f) * 0.24f;
                superClusterNodes.get(i).scale(pulse, pulse, pulse);
                superClusterNodes.get(i).interpolation(3, 0);
            }

            // Void markers: drift inward 0.003 blocks/tick, snap back every 300 ticks
            float driftFactor = (ticksAlive % 300) * 0.003f;
            for (int i = 0; i < voidMarkers.size(); i++) {
                double ox = voidMarkerOriginal[i][0];
                double oy = voidMarkerOriginal[i][1];
                double oz = voidMarkerOriginal[i][2];
                // Drift toward center
                double shrink = 1.0 - driftFactor * 0.15;
                double dx = ox * shrink, dy = oy * shrink, dz = oz * shrink;
                // Apply web rotation
                double rx = dx * Math.cos(yRot) - dz * Math.sin(yRot);
                double rz = dx * Math.sin(yRot) + dz * Math.cos(yRot);
                double ry = dy * Math.cos(xRot) - rz * Math.sin(xRot);
                double rz2 = dy * Math.sin(xRot) + rz * Math.cos(xRot);
                voidMarkers.get(i).entity().teleport(wc.clone().add(rx, ry, rz2));
            }

            // Particles: filament-faint dust tracing all 8 filaments
            if (ticksAlive % 3 == 0) {
                for (int f = 0; f < 8; f++) {
                    if (!filaments.get(f).isEmpty()) {
                        Location fLoc = filaments.get(f).get(1).entity().getLocation();
                        DisplayBuilder.dustParticles(fLoc, 2, 0.5, 20, 10, 40, 0.8f);
                    }
                }
            }

            // Falling obsidian tears from super-cluster nodes
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle sc : superClusterNodes) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            sc.entity().getLocation(), 2, 0.2, 0.2, 0.2, 0);
                }
            }

            // End rod from condensed matter (sparse)
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle cm : condensedMatter) {
                    c.getWorld().spawnParticle(Particle.END_ROD, cm.entity().getLocation(), 1, 0, 0, 0, 0);
                }
            }

            // Ambient cave continuous
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(wc, Sound.AMBIENT_CAVE, 0.2f, 0.08f);
            }

            // Amethyst cluster break on super-cluster pulse peak
            if (ticksAlive % 150 == 0) {
                DisplayBuilder.playSound(wc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.4f);
            }

            // Cyan dust from web nodes
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle node : webNodes) {
                    DisplayBuilder.cyanDust(node.entity().getLocation(), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicWeb(plugin); }
    }
}
