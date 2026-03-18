package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * GROUP 8: REALITY COLLAPSE (Attacks 71-80)
 * Phase 2 (50% HP) — the End dimension itself breaking down.
 * Dimension bleeds, reality fractures, void surfaces, atmospheric dissolution,
 * island edge collapse, space-time shards, dimension overlaps, null zones,
 * and a collapse cascade that sweeps across the island.
 *
 * Palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * Materials: AMETHYST_BLOCK, POLISHED_BLACKSTONE, SEA_LANTERN, END_ROD, CRYING_OBSIDIAN
 */
public final class RealityCollapse {

    private RealityCollapse() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DimensionBleed(plugin));
        registry.register(new RealityFractureLine(plugin));
        registry.register(new VoidSurface(plugin));
        registry.register(new AtmosphericDissolution(plugin));
        registry.register(new EndCollapseSequence(plugin));
        registry.register(new SpaceTimeShard(plugin));
        registry.register(new DimensionOverlap(plugin));
        registry.register(new RealityScream(plugin));
        registry.register(new NullZone(plugin));
        registry.register(new CollapseCascade(plugin));
    }

    // =========================================================================
    // 71. DIMENSION BLEED
    // Brief flashes of incorrect biome content — patches of foreign particles
    // appear on the island surface for 1 second each, dealing minor damage
    // in the affected area. Up to 4 simultaneous bleeds.
    // 8 HP per bleed contact, 20s cooldown.
    // =========================================================================
    public static class DimensionBleed extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> bleedPoints = new ArrayList<>();
        private final List<Integer> bleedTimers = new ArrayList<>();

        public DimensionBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimension_bleed", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn 4 bleed patches at random positions
            for (int i = 0; i < 4; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3.0 + Math.random() * 12.0;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                bleedPoints.add(loc);
                bleedTimers.add(20 + (int)(Math.random() * 40)); // Staggered start
                // Bleed patch marker — different material per biome type
                Material mat = switch (i % 3) {
                    case 0 -> Material.POLISHED_BLACKSTONE; // nether bleed
                    case 1 -> Material.SEA_LANTERN;         // end bleed
                    default -> Material.AMETHYST_BLOCK;     // crystal bleed
                };
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(3.0f, 0.05f, 3.0f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            for (int i = 0; i < bleedPoints.size(); i++) {
                int startTick = bleedTimers.get(i);
                Location bleed = bleedPoints.get(i);

                // Warning: portal ripple 10 ticks before bleed
                if (ticksAlive >= startTick - 10 && ticksAlive < startTick) {
                    if (ticksAlive % 4 == 0) {
                        World w = bleed.getWorld();
                        if (w != null) {
                            w.spawnParticle(Particle.PORTAL, bleed.clone().add(0, 0.5, 0),
                                    6, 1.5, 0.3, 1.5, 0.05);
                        }
                    }
                }

                // Active bleed: 20 ticks (1 second) of biome particles
                if (ticksAlive >= startTick && ticksAlive < startTick + 20) {
                    if (ticksAlive % 3 == 0) {
                        Particle biomeParticle = switch (i % 3) {
                            case 0 -> Particle.CRIMSON_SPORE;
                            case 1 -> Particle.SNOWFLAKE;
                            default -> Particle.WARPED_SPORE;
                        };
                        World w = bleed.getWorld();
                        if (w != null) {
                            w.spawnParticle(biomeParticle, bleed.clone().add(0, 1, 0),
                                    12, 3.0, 1.0, 3.0, 0.02);
                        }
                        DisplayBuilder.cyanDust(bleed, 6, 2.0);
                    }
                    // Damage in bleed area
                    if (ticksAlive % 20 == 0) {
                        World w = bleed.getWorld();
                        if (w != null) {
                            for (var p : w.getPlayers()) {
                                if (isExempt(p)) continue;
                                if (p.getLocation().distanceSquared(bleed) <= 9.0) {
                                    p.damage(config.getDamage());
                                }
                            }
                        }
                    }
                }

                // Flash the block during active phase then fade
                if (ticksAlive == startTick && i < handles.size()) {
                    handles.get(i).glow(240, 240, 255);
                }
                if (ticksAlive == startTick + 20 && i < handles.size()) {
                    handles.get(i).glow(128, 0, 255);
                    handles.get(i).scale(3.0f, 0.02f, 3.0f).interpolation(5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionBleed(plugin); }
    }

    // =========================================================================
    // 72. REALITY FRACTURE LINE
    // A white crack appears in the air at head height, stretching 8 blocks.
    // END_ROD particles outline the crack. Walking through deals heavy damage.
    // 12 HP on passing through, 35s cooldown.
    // =========================================================================
    public static class RealityFractureLine extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location fractureStart;
        private Location fractureEnd;
        private boolean active = false;

        public RealityFractureLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_fracture_line", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(270);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI;
            double yOffset = 1.6; // Head height
            fractureStart = center.clone().add(
                    Math.cos(angle) * -4, yOffset, Math.sin(angle) * -4);
            fractureEnd = center.clone().add(
                    Math.cos(angle) * 4, yOffset, Math.sin(angle) * 4);
            // Fracture line blocks — thin white bars along the crack
            int segments = 8;
            for (int i = 0; i < segments; i++) {
                double t = (double) i / (segments - 1);
                Location loc = fractureStart.clone().add(
                        (fractureEnd.getX() - fractureStart.getX()) * t,
                        (fractureEnd.getY() - fractureStart.getY()) * t + (Math.random() - 0.5) * 0.3,
                        (fractureEnd.getZ() - fractureStart.getZ()) * t);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.15f, 0.08f, 0.15f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(240, 240, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 30 ticks
            if (ticksAlive < 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.6, 0),
                            6, 2.0, 240, 240, 255, 1.0f);
                }
                return;
            }

            active = true;

            // END_ROD particle outline
            if (ticksAlive % 4 == 0) {
                World w = fractureStart.getWorld();
                if (w != null) {
                    DisplayBuilder.particleLine(fractureStart, fractureEnd,
                            Particle.END_ROD, 2, null);
                }
                // White dust along fracture
                DisplayBuilder.dustParticles(center.clone().add(0, 1.6, 0),
                        4, 3.0, 240, 240, 255, 1.3f);
            }

            // Ambient cracking sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.2f);
            }

            // Damage check — players crossing the fracture line
            if (ticksAlive % 10 == 0 && active) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (isNearLine3D(p.getLocation(), fractureStart, fractureEnd, 1.5)) {
                        p.damage(config.getDamage());
                        DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                240, 240, 255, 1.5f);
                    }
                }
            }
        }

        private boolean isNearLine3D(Location p, Location a, Location b, double threshold) {
            double dx = b.getX() - a.getX();
            double dy = b.getY() - a.getY();
            double dz = b.getZ() - a.getZ();
            double len2 = dx * dx + dy * dy + dz * dz;
            if (len2 < 0.01) return p.distanceSquared(a) <= threshold * threshold;
            double t = Math.max(0, Math.min(1,
                    ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy +
                     (p.getZ() - a.getZ()) * dz) / len2));
            double cx = a.getX() + t * dx;
            double cy = a.getY() + t * dy;
            double cz = a.getZ() + t * dz;
            double dist2 = (p.getX() - cx) * (p.getX() - cx) +
                    (p.getY() - cy) * (p.getY() - cy) +
                    (p.getZ() - cz) * (p.getZ() - cz);
            return dist2 <= threshold * threshold;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityFractureLine(plugin); }
    }

    // =========================================================================
    // 73. VOID SURFACE
    // A 6x6 area of the floor becomes transparently void — REVERSE_PORTAL
    // particles form a glassy window revealing the void beneath. Any falling-
    // object attack that lands on it has double explosion radius.
    // No direct damage, 40s cooldown.
    // =========================================================================
    public static class VoidSurface extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location surfaceCenter;

        public VoidSurface(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_surface", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 4.0 + Math.random() * 8.0;
            surfaceCenter = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            // 6x6 grid of glass-like void blocks
            for (int x = -3; x < 3; x++) {
                for (int z = -3; z < 3; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            surfaceCenter.clone().add(x, -0.1, z), Material.CYAN_STAINED_GLASS);
                    h.scale(1.0f, 0.05f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(surfaceCenter, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(surfaceCenter, 8, 3.0, 128, 0, 255, 1.0f);
                }
                return;
            }

            // Void surface effect: REVERSE_PORTAL particles from ground
            if (ticksAlive % 6 == 0) {
                World w = surfaceCenter.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.REVERSE_PORTAL,
                            surfaceCenter.clone().add(0, 0.2, 0), 10, 3.0, 0.2, 3.0, 0.02);
                }
                // Edge glow
                DisplayBuilder.particleRing(surfaceCenter.clone().add(0, 0.1, 0),
                        3.0, Particle.ELECTRIC_SPARK, 12, null);
            }

            // Pulsing void visual
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.dustParticles(surfaceCenter.clone().add(0, 0.1, 0),
                        6, 2.5, 128, 0, 255, 1.2f);
                DisplayBuilder.playSound(surfaceCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSurface(plugin); }
    }

    // =========================================================================
    // 74. ATMOSPHERIC DISSOLUTION
    // Particle atmosphere triples in density — visibility reduced to 8 blocks.
    // Dense white haze covers the island. Environmental attack warnings become
    // nearly invisible. Pure sensory disruption.
    // No direct damage, 50s cooldown.
    // =========================================================================
    public static class AtmosphericDissolution extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public AtmosphericDissolution(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("atmospheric_dissolution", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            // Spawn a hemisphere of glowing fog blocks around center
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                double dist = 6.0 + Math.random() * 6.0;
                double y = 2.0 + Math.random() * 4.0;
                Location fogLoc = center.clone().add(
                        Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(fogLoc, Material.SEA_LANTERN);
                h.scale(3.0f, 0.8f, 3.0f).glow(240, 240, 255).interpolation(5, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Buildup phase: 60 ticks (3 seconds) — gradual increase
            float density = Math.min(1.0f, ticksAlive / 60.0f);

            // Dense white particle fog
            if (ticksAlive % 3 == 0) {
                int count = (int)(density * 20);
                for (int i = 0; i < count; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oy = Math.random() * 6;
                    double oz = (Math.random() - 0.5) * 20;
                    DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz),
                            4, 1.5, 240, 240, 255, 2.0f);
                }
            }

            // Cloud particles for fog effect
            if (ticksAlive % 5 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.CLOUD, center.clone().add(0, 2, 0),
                            (int)(density * 15), 10, 3, 10, 0.01);
                }
            }

            // Scale fog blocks based on density
            if (ticksAlive % 20 == 0) {
                float s = 3.0f + density * 4.0f;
                for (BlockDisplayHandle h : handles) {
                    h.scale(s, 0.8f + density * 1.5f, s).interpolation(10, 0);
                }
            }

            // Fade phase: last 80 ticks
            if (ticksAlive > 200 && ticksAlive % 10 == 0) {
                float fade = (ticksAlive - 200) / 80.0f;
                float fs = 3.0f * (1.0f - fade);
                for (BlockDisplayHandle h : handles) {
                    h.scale(Math.max(0.1f, fs), Math.max(0.1f, fs * 0.3f), Math.max(0.1f, fs))
                     .interpolation(8, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AtmosphericDissolution(plugin); }
    }

    // =========================================================================
    // 75. END COLLAPSE SEQUENCE
    // Three sections of the island border collapse inward — BlockDisplay end_stone
    // entities tilt and fall toward center. Permanently narrows the safe area.
    // 8 HP to players within 4 blocks of a collapsing section, 90s cooldown.
    // =========================================================================
    public static class EndCollapseSequence extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> collapsePoints = new ArrayList<>();
        private boolean collapsed = false;

        public EndCollapseSequence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_collapse_sequence", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three collapse sections at 120 degree intervals on the island edge
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 + Math.random() * 0.5;
                double dist = 16.0;
                Location collapseCenter = center.clone().add(
                        Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                collapsePoints.add(collapseCenter);
                // Edge blocks — 5 end_stone blocks per section
                for (int j = -2; j <= 2; j++) {
                    double perpAngle = angle + Math.PI / 2;
                    Location blockLoc = collapseCenter.clone().add(
                            Math.cos(perpAngle) * j * 1.5, 0, Math.sin(perpAngle) * j * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, Material.POLISHED_BLACKSTONE);
                    h.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 80 ticks (4 seconds) — heartbeat building in speed
            if (ticksAlive < 80) {
                if (ticksAlive % Math.max(5, 20 - ticksAlive / 4) == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
                    for (Location pt : collapsePoints) {
                        DisplayBuilder.cyanDust(pt, 8, 2.0);
                    }
                }
                return;
            }

            // Collapse phase: 120 ticks (6 seconds)
            if (!collapsed) {
                collapsed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 0.3f);
            }

            int collapseTick = ticksAlive - 80;
            if (collapseTick < 120) {
                // Tilt blocks inward progressively
                float tiltProgress = Math.min(1.0f, collapseTick / 60.0f);
                float tiltAngle = tiltProgress * (float)(Math.PI / 3); // 60 degree tilt

                for (int i = 0; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    int section = i / 5;
                    if (section >= collapsePoints.size()) continue;
                    Location collapseCenter = collapsePoints.get(section);
                    double toCenter = Math.atan2(
                            center.getZ() - collapseCenter.getZ(),
                            center.getX() - collapseCenter.getX());

                    // Axis of tilt: perpendicular to the direction toward center
                    float axX = (float)(-Math.sin(toCenter));
                    float axZ = (float)(Math.cos(toCenter));

                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.75f, -0.75f, -0.75f),
                            new AxisAngle4f(tiltAngle, axX, 0, axZ),
                            new Vector3f(1.5f, 1.5f, 1.5f),
                            new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);

                    // Move blocks slightly inward
                    if (collapseTick % 10 == 0) {
                        Location current = bd.getLocation();
                        double inwardX = (center.getX() - current.getX()) * 0.02;
                        double inwardZ = (center.getZ() - current.getZ()) * 0.02;
                        bd.teleport(current.add(inwardX, -tiltProgress * 0.1, inwardZ));
                    }
                }

                // Collapse particles
                if (collapseTick % 6 == 0) {
                    for (Location pt : collapsePoints) {
                        DisplayBuilder.dustParticles(pt, 12, 3.0, 240, 240, 255, 1.4f);
                        World w = pt.getWorld();
                        if (w != null) {
                            w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 8, 2, 1, 2, 0.03);
                        }
                    }
                }

                // Damage to nearby players
                if (collapseTick % 20 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        for (var p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            for (Location pt : collapsePoints) {
                                if (p.getLocation().distanceSquared(pt) <= 16.0) {
                                    p.damage(config.getDamage());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndCollapseSequence(plugin); }
    }

    // =========================================================================
    // 76. SPACE-TIME SHARD
    // A translucent 2x2x2 amethyst block appears frozen at Y+12, rotating
    // slowly. Emits white sparks. In its 10-block radius, environmental
    // attack cooldowns are effectively reduced — the shard itself does no
    // damage but creates a danger zone of accelerated attacks.
    // No direct damage, 60s cooldown.
    // =========================================================================
    public static class SpaceTimeShard extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location shardLocation;

        public SpaceTimeShard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("space_time_shard", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 5.0 + Math.random() * 8.0;
            shardLocation = center.clone().add(Math.cos(angle) * dist, 12, Math.sin(angle) * dist);
            // 2x2x2 amethyst shard
            BlockDisplayHandle h = displayBuilder.spawnBlock(shardLocation, Material.AMETHYST_BLOCK);
            h.scale(2.0f, 2.0f, 2.0f).glow(240, 240, 255).interpolation(3, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            // Smaller satellite shards
            for (int i = 0; i < 4; i++) {
                double a = (2 * Math.PI * i) / 4;
                Location satLoc = shardLocation.clone().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
                BlockDisplayHandle sat = displayBuilder.spawnBlock(satLoc, Material.AMETHYST_BLOCK);
                sat.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(sat);
                spawnedEntities.add(sat.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow rotation of main shard
            if (ticksAlive % 4 == 0 && !handles.isEmpty()) {
                BlockDisplay bd = (BlockDisplay) handles.get(0).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-1.0f, -1.0f, -1.0f),
                        new AxisAngle4f(ticksAlive * 0.02f, 0, 1, 0),
                        new Vector3f(2.0f, 2.0f, 2.0f),
                        new AxisAngle4f(ticksAlive * 0.01f, 1, 0, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(4);
            }

            // White spark emissions from all faces
            if (ticksAlive % 5 == 0) {
                World w = shardLocation.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, shardLocation, 8, 1.5, 1.5, 1.5, 0.02);
                }
                DisplayBuilder.dustParticles(shardLocation, 6, 1.5, 240, 240, 255, 1.2f);
            }

            // 10-block radius indicator ring
            if (ticksAlive % 15 == 0) {
                Location groundBelow = shardLocation.clone();
                groundBelow.setY(center.getY());
                DisplayBuilder.particleRing(groundBelow, 10.0, Particle.ELECTRIC_SPARK, 20, null);
                DisplayBuilder.cyanDust(groundBelow, 6, 5.0);
            }

            // Low constant hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(shardLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpaceTimeShard(plugin); }
    }

    // =========================================================================
    // 77. DIMENSION OVERLAP
    // Two versions of the terrain briefly coexist — a translucent white dust
    // layer sweeps across the island at ground level. Ghost terrain creates
    // visual confusion. Environmental warnings appear on both layers.
    // No direct damage, 45s cooldown.
    // =========================================================================
    public static class DimensionOverlap extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public DimensionOverlap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimension_overlap", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ghost terrain layer — translucent sea lantern grid at ground level
            for (int x = -8; x <= 8; x += 4) {
                for (int z = -8; z <= 8; z += 4) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.3, z), Material.SEA_LANTERN);
                    h.scale(4.0f, 0.04f, 4.0f).glow(240, 240, 255).interpolation(5, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 6.0, 240, 240, 255, 1.0f);
                }
                return;
            }

            // Overlap effect: dense white dust sweep at ground level
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.4, oz),
                            5, 1.0, 240, 240, 255, 1.5f);
                }
            }

            // Ghost terrain pulse — blocks oscillate in opacity
            if (ticksAlive % 10 == 0) {
                float pulse = 0.5f + (float)Math.sin(ticksAlive * 0.15) * 0.3f;
                for (BlockDisplayHandle h : handles) {
                    h.scale(4.0f, 0.04f * pulse * 3, 4.0f).interpolation(8, 0);
                }
            }

            // Ambient disorientation
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionOverlap(plugin); }
    }

    // =========================================================================
    // 78. REALITY SCREAM
    // No particle effect — purely audio. All ambient sounds cut, 1 second of
    // silence, then a devastating sonic boom. Applies damage as the sheer
    // dimensional pressure.
    // 10 HP arena-wide, 55s cooldown.
    // =========================================================================
    public static class RealityScream extends EnvironmentalAttack {
        private boolean screamed = false;

        public RealityScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_scream", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(10.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(140);
        }

        @Override
        protected void onSpawn(Location center) {
            // Silence — no spawn sound. The absence IS the warning.
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 20 ticks of silence (1 second), then scream
            if (ticksAlive == 20 && !screamed) {
                screamed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.1f);
                // No visual effect — this is pure audio horror
                // Damage all players island-wide
                World w = center.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 625.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Post-scream: subtle ground-level spark residue (the only visual)
            if (ticksAlive > 20 && ticksAlive < 60 && ticksAlive % 10 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, center, 4, 10, 0.5, 10, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityScream(plugin); }
    }

    // =========================================================================
    // 79. NULL ZONE
    // A 5-block-radius circle goes completely dark — all light suppressed,
    // a perfect black disc in the white Phase 2 environment. Extreme visual
    // contrast. Players inside take damage from the void exposure.
    // 8 HP per second inside, 50s cooldown.
    // =========================================================================
    public static class NullZone extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location zoneCenter;

        public NullZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_zone", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 5.0 + Math.random() * 8.0;
            zoneCenter = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            // Dark disc — blackstone blocks forming a 5-block-radius floor
            for (int x = -5; x <= 5; x += 2) {
                for (int z = -5; z <= 5; z += 2) {
                    if (x * x + z * z > 25) continue;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            zoneCenter.clone().add(x, 0.02, z), Material.POLISHED_BLACKSTONE);
                    h.scale(2.0f, 0.03f, 2.0f).glow(0, 0, 0).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Dark columns at edges for visual definition
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8;
                BlockDisplayHandle edge = displayBuilder.spawnBlock(
                        zoneCenter.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5),
                        Material.CRYING_OBSIDIAN);
                edge.scale(0.3f, 3.0f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(edge);
                spawnedEntities.add(edge.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dark smoke rising from null zone
            if (ticksAlive % 5 == 0) {
                World w = zoneCenter.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.LARGE_SMOKE, zoneCenter.clone().add(0, 0.5, 0),
                            8, 3.0, 0.5, 3.0, 0.01);
                }
            }

            // Edge definition particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(zoneCenter.clone().add(0, 0.5, 0),
                        5.0, Particle.ELECTRIC_SPARK, 16, null);
                DisplayBuilder.dustParticles(zoneCenter.clone().add(0, 0.3, 0),
                        4, 2.5, 128, 0, 255, 1.0f);
            }

            // Ambient void hum
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(zoneCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullZone(plugin); }
    }

    // =========================================================================
    // 80. COLLAPSE CASCADE
    // A visible cascade of end_stone particle explosions travels across the
    // island at 5 blocks/second, fragmenting the surface. Players can outrun
    // it by sprinting away from the wave front.
    // 16 HP per wave contact, 60s cooldown.
    // =========================================================================
    public static class CollapseCascade extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location cascadeOrigin;
        private double cascadeAngle;
        private double cascadeDistance = 0;

        public CollapseCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapse_cascade", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            cascadeAngle = Math.random() * 2 * Math.PI;
            double dist = 14.0;
            cascadeOrigin = center.clone().add(
                    Math.cos(cascadeAngle + Math.PI) * dist, 0,
                    Math.sin(cascadeAngle + Math.PI) * dist);
            DisplayBuilder.playSound(cascadeOrigin, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 30 ticks
            if (ticksAlive < 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(cascadeOrigin, 10, 2.0, 240, 240, 255, 1.3f);
                    World w = cascadeOrigin.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, cascadeOrigin, 6, 1, 0.5, 1, 0.02);
                    }
                }
                return;
            }

            // Cascade travels at 5 blocks/second = 0.25 blocks/tick
            cascadeDistance += 0.25;
            Location waveFront = cascadeOrigin.clone().add(
                    Math.cos(cascadeAngle) * cascadeDistance, 0,
                    Math.sin(cascadeAngle) * cascadeDistance);

            // Wave front visual — erupting end_stone particles
            if (ticksAlive % 2 == 0) {
                World w = waveFront.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, waveFront.clone().add(0, 0.5, 0),
                            12, 2, 1, 2, 0.04);
                }
                DisplayBuilder.dustParticles(waveFront, 15, 2.5, 240, 240, 255, 1.6f);
                DisplayBuilder.cyanDust(waveFront, 8, 1.5);

                // Spawn debris blocks at wave front
                if (ticksAlive % 6 == 0) {
                    BlockDisplayHandle debris = displayBuilder.spawnBlock(
                            waveFront.clone().add(0, 0.5, 0), Material.POLISHED_BLACKSTONE);
                    debris.scale(0.4f, 0.4f, 0.4f).glow(240, 240, 255).interpolation(2, 0);
                    handles.add(debris);
                    spawnedEntities.add(debris.entity());
                    // Launch debris upward
                    debris.entity().teleport(waveFront.clone().add(
                            (Math.random() - 0.5) * 2, 1 + Math.random() * 3,
                            (Math.random() - 0.5) * 2));
                }
            }

            // Wave front damage
            if (ticksAlive % 8 == 0) {
                World w = waveFront.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(waveFront) <= 9.0) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5,
                                    240, 240, 255, 1.5f);
                        }
                    }
                }
            }

            // Sound at wave front
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(waveFront, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
                DisplayBuilder.playSound(waveFront, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapseCascade(plugin); }
    }
}
