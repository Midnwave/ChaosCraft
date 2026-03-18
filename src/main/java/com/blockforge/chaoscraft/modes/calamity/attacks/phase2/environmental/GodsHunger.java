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
 * GROUP 9: GOD'S HUNGER (Attacks 81-90)
 * Phase 2 (50% HP) — the arena shrinking, void encroaching, existential threats.
 * Void advance, hunger fields, absolute zero rings, void tendrils, event
 * horizon pulses, shroud, DoG wake consumption, hunger pulses, total void
 * suppression, and the island's last breath at 15% HP.
 *
 * Palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * Materials: POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SEA_LANTERN, DARK_PRISMARINE
 */
public final class GodsHunger {

    private GodsHunger() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidAdvance(plugin));
        registry.register(new HungerField(plugin));
        registry.register(new AbsoluteZeroRing(plugin));
        registry.register(new VoidTendril(plugin));
        registry.register(new EventHorizonPulse(plugin));
        registry.register(new Shroud(plugin));
        registry.register(new DoGWakeConsumption(plugin));
        registry.register(new HungerPulse(plugin));
        registry.register(new TotalVoidSuppression(plugin));
        registry.register(new IslandsLastBreath(plugin));
    }

    // =========================================================================
    // 81. VOID ADVANCE
    // The void at the island's edge advances inward — a 4-block-tall wall of
    // black REVERSE_PORTAL and LARGE_SMOKE particles slowly consumes the
    // outer 2 blocks. Permanently reduces the playable area.
    // 10 HP per second in void wall contact, fires at fixed HP thresholds.
    // =========================================================================
    public static class VoidAdvance extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double currentRadius = 18.0;

        public VoidAdvance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_advance", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(10.0);
            config.setDamageRadius(0.0); // Custom check
            config.setDurationTicks(340);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Void wall ring — 16 tall blackstone pillars at the edge
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(
                        Math.cos(angle) * currentRadius, 0, Math.sin(angle) * currentRadius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(2.0f, 4.0f, 0.5f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(128, 0, 255)
                 .interpolation(5, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 80 ticks (4 seconds)
            if (ticksAlive < 80) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.particleRing(center, currentRadius, Particle.LARGE_SMOKE, 24, null);
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.5f);
                }
                return;
            }

            // Advance: shrink radius by 2 blocks over 300 ticks
            int advanceTick = ticksAlive - 80;
            if (advanceTick < 300) {
                double shrinkRate = 2.0 / 300.0;
                currentRadius -= shrinkRate;

                // Move void wall blocks inward
                if (advanceTick % 10 == 0) {
                    for (int i = 0; i < handles.size(); i++) {
                        double angle = (2 * Math.PI * i) / handles.size();
                        Location newLoc = center.clone().add(
                                Math.cos(angle) * currentRadius, 0,
                                Math.sin(angle) * currentRadius);
                        handles.get(i).entity().teleport(newLoc);
                    }
                }

                // Void wall particles
                if (advanceTick % 4 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        for (int i = 0; i < 12; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            Location wallPt = center.clone().add(
                                    Math.cos(angle) * currentRadius, Math.random() * 4,
                                    Math.sin(angle) * currentRadius);
                            w.spawnParticle(Particle.REVERSE_PORTAL, wallPt, 4, 0.3, 1.0, 0.3, 0.02);
                            w.spawnParticle(Particle.LARGE_SMOKE, wallPt, 3, 0.5, 1.0, 0.5, 0.01);
                        }
                    }
                }

                // Damage players in void wall (beyond current radius)
                if (advanceTick % 20 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        double r2 = currentRadius * currentRadius;
                        for (var p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - center.getX();
                            double dz = p.getLocation().getZ() - center.getZ();
                            if (dx * dx + dz * dz > r2) {
                                p.damage(config.getDamage());
                                DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5,
                                        128, 0, 255, 1.5f);
                            }
                        }
                    }
                }
            }

            // Ambient void sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidAdvance(plugin); }
    }

    // =========================================================================
    // 82. HUNGER FIELD
    // An invisible field spreads from the island edge inward, visible only by
    // REVERSE_PORTAL particles draining downward. Covers 30% of the arena.
    // Passive health drain to players inside.
    // 8 HP per exposure interval, 55s cooldown.
    // =========================================================================
    public static class HungerField extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double fieldAngle;
        private double fieldSpread;

        public HungerField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunger_field", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(8.0);
            config.setDamageRadius(0.0); // Custom sector check
            config.setDurationTicks(400);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            fieldAngle = Math.random() * 2 * Math.PI;
            fieldSpread = Math.PI * 0.6; // ~108 degree arc = ~30% coverage
            // Field boundary markers
            for (int side = -1; side <= 1; side += 2) {
                double boundaryAngle = fieldAngle + side * fieldSpread / 2;
                for (int r = 8; r <= 16; r += 4) {
                    Location loc = center.clone().add(
                            Math.cos(boundaryAngle) * r, 0, Math.sin(boundaryAngle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — leading edge drain particles
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = fieldAngle + (Math.random() - 0.5) * fieldSpread;
                        double d = 10.0 + Math.random() * 6.0;
                        Location pt = center.clone().add(Math.cos(a) * d, 1, Math.sin(a) * d);
                        World w = pt.getWorld();
                        if (w != null) {
                            w.spawnParticle(Particle.REVERSE_PORTAL, pt, 4, 0.5, 0.5, 0.5, 0.02);
                        }
                    }
                }
                return;
            }

            // Active field: REVERSE_PORTAL drain in the sector
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = fieldAngle + (Math.random() - 0.5) * fieldSpread;
                    double d = 6.0 + Math.random() * 10.0;
                    Location pt = center.clone().add(Math.cos(a) * d, 1.5, Math.sin(a) * d);
                    World w = pt.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.REVERSE_PORTAL, pt, 6, 0.8, 0.8, 0.8, 0.02);
                    }
                    DisplayBuilder.dustParticles(pt, 3, 0.6, 128, 0, 255, 1.0f);
                }
            }

            // Damage players in the field sector
            if (ticksAlive % 60 == 0 && ticksAlive >= 40) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (isInSector(p.getLocation(), center, fieldAngle, fieldSpread, 6.0, 18.0)) {
                        p.damage(config.getDamage());
                        DisplayBuilder.dustParticles(p.getLocation(), 6, 0.4, 128, 0, 255, 1.2f);
                    }
                }
            }

            // Ambient drain sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.4f);
            }
        }

        private boolean isInSector(Location point, Location center, double angle,
                                   double spread, double minDist, double maxDist) {
            double dx = point.getX() - center.getX();
            double dz = point.getZ() - center.getZ();
            double dist2 = dx * dx + dz * dz;
            if (dist2 < minDist * minDist || dist2 > maxDist * maxDist) return false;
            double pointAngle = Math.atan2(dz, dx);
            double diff = Math.abs(normalizeAngle(pointAngle - angle));
            return diff <= spread / 2;
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HungerField(plugin); }
    }

    // =========================================================================
    // 83. ABSOLUTE ZERO RING
    // A ring of white SNOWFLAKE and ELECTRIC_SPARK particles appears at the
    // island's perimeter and contracts inward at 0.5 blocks/second. 2 blocks
    // wide, unmistakable. Contact deals damage and applies a push-back.
    // 12 HP on ring contact, 70s cooldown.
    // =========================================================================
    public static class AbsoluteZeroRing extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double ringRadius = 18.0;
        private boolean contracting = false;

        public AbsoluteZeroRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero_ring", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(0.0); // Custom ring check
            config.setDurationTicks(500);
            config.setCooldownTicks(1400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ring of sea lantern blocks
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                Location loc = center.clone().add(
                        Math.cos(angle) * ringRadius, 0, Math.sin(angle) * ringRadius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(1.5f, 0.3f, 2.0f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(240, 240, 255)
                 .interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Formation: 100 ticks (5 seconds) — ring solidifies
            if (ticksAlive < 100) {
                if (ticksAlive % 10 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        DisplayBuilder.particleRing(center, ringRadius, Particle.SNOWFLAKE, 24, null);
                        DisplayBuilder.particleRing(center, ringRadius, Particle.ELECTRIC_SPARK, 16, null);
                    }
                }
                return;
            }

            contracting = true;

            // Contract: 0.5 blocks/second = 0.025 blocks/tick
            if (ringRadius > 8.0) {
                ringRadius -= 0.025;
            }

            // Move ring blocks inward
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < handles.size(); i++) {
                    double angle = (2 * Math.PI * i) / handles.size();
                    Location newLoc = center.clone().add(
                            Math.cos(angle) * ringRadius, 0,
                            Math.sin(angle) * ringRadius);
                    handles.get(i).entity().teleport(newLoc);
                }
            }

            // Ring particle effects
            if (ticksAlive % 4 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        Location pt = center.clone().add(
                                Math.cos(a) * ringRadius, 0.5, Math.sin(a) * ringRadius);
                        w.spawnParticle(Particle.SNOWFLAKE, pt, 4, 0.8, 0.5, 0.8, 0.01);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 3, 0.5, 0.3, 0.5, 0.02);
                    }
                    DisplayBuilder.dustParticles(
                            center.clone().add(0, 0.3, 0), 4, (float)ringRadius,
                            240, 240, 255, 1.0f);
                }
            }

            // Ring contact damage — players at ring radius +/- 2 blocks
            if (ticksAlive % 15 == 0 && contracting) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(dist - ringRadius) <= 2.0) {
                        p.damage(config.getDamage());
                        DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                240, 240, 255, 1.6f);
                        // Push player inward
                        double inwardX = -dx / dist * 0.5;
                        double inwardZ = -dz / dist * 0.5;
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(inwardX, 0.1, inwardZ)));
                    }
                }
            }

            // Ambient cracking sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteZeroRing(plugin); }
    }

    // =========================================================================
    // 84. VOID TENDRIL
    // From the island's edge, a tendril of black LARGE_SMOKE particles extends
    // inward — 2 blocks wide, reaching 12 blocks in. Weaves like something
    // alive. Up to 3 simultaneous tendrils.
    // 16 HP per second of contact, 40s cooldown.
    // =========================================================================
    public static class VoidTendril extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> tendrilPath = new ArrayList<>();
        private double tendrilAngle;
        private int tendrilLength = 0;

        public VoidTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendril", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(320);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            tendrilAngle = Math.random() * 2 * Math.PI;
            // Start at island edge
            Location edgeStart = center.clone().add(
                    Math.cos(tendrilAngle + Math.PI) * 16, 0,
                    Math.sin(tendrilAngle + Math.PI) * 16);
            tendrilPath.add(edgeStart);
            DisplayBuilder.playSound(edgeStart, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 60 ticks (3 seconds)
            if (ticksAlive < 60) {
                if (ticksAlive % 10 == 0 && !tendrilPath.isEmpty()) {
                    DisplayBuilder.dustParticles(tendrilPath.get(0), 8, 1.5, 128, 0, 255, 1.2f);
                }
                return;
            }

            // Extend tendril: one segment every 10 ticks, up to 12 blocks
            if ((ticksAlive - 60) % 10 == 0 && tendrilLength < 12) {
                tendrilLength++;
                Location prev = tendrilPath.get(tendrilPath.size() - 1);
                // Slight weaving — sine wave
                double weave = Math.sin(tendrilLength * 0.8) * 1.5;
                double perpAngle = tendrilAngle + Math.PI / 2;
                Location next = prev.clone().add(
                        Math.cos(tendrilAngle) * 1.0 + Math.cos(perpAngle) * weave * 0.1,
                        0,
                        Math.sin(tendrilAngle) * 1.0 + Math.sin(perpAngle) * weave * 0.1);
                tendrilPath.add(next);
                // Spawn tendril segment block
                BlockDisplayHandle h = displayBuilder.spawnBlock(next, Material.CRYING_OBSIDIAN);
                h.scale(2.0f, 0.3f, 1.0f)
                 .rotate((float)tendrilAngle, 0, 1, 0)
                 .glow(128, 0, 255)
                 .interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tendril smoke particles along full length
            if (ticksAlive % 4 == 0) {
                for (Location pt : tendrilPath) {
                    World w = pt.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.LARGE_SMOKE, pt.clone().add(0, 0.5, 0),
                                3, 0.8, 0.3, 0.8, 0.01);
                    }
                    DisplayBuilder.dustParticles(pt, 3, 0.6, 128, 0, 255, 1.0f);
                }
            }

            // Weaving animation — undulate existing segments
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < handles.size(); i++) {
                    float wave = (float)Math.sin((ticksAlive + i * 10) * 0.1) * 0.15f;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-1.0f, -0.15f + wave, -0.5f),
                            new AxisAngle4f((float)tendrilAngle, 0, 1, 0),
                            new Vector3f(2.0f, 0.3f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }

            // Contact damage along tendril
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location pt : tendrilPath) {
                        if (p.getLocation().distanceSquared(pt) <= 2.25) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 6, 0.4,
                                    128, 0, 255, 1.3f);
                            break;
                        }
                    }
                }
            }

            // Ambient tendril sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendril(plugin); }
    }

    // =========================================================================
    // 85. EVENT HORIZON PULSE
    // Two simultaneous pulse waves — one from center outward, one from edge
    // inward. Where they collide at mid-island creates a damage ring. Players
    // at the collision zone take reduced damage from pulse cancellation.
    // 14 HP per pulse contact (6 HP in collision ring), 50s cooldown.
    // =========================================================================
    public static class EventHorizonPulse extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double outerPulseRadius;
        private double innerPulseRadius;
        private boolean collided = false;

        public EventHorizonPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("event_horizon_pulse", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(14.0);
            config.setDamageRadius(0.0); // Custom
            config.setDurationTicks(100);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            outerPulseRadius = 0.0;  // Expands from center
            innerPulseRadius = 18.0; // Contracts from edge
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Expand outward pulse and contract inward pulse
            outerPulseRadius += 0.3; // ~6 blocks/second
            innerPulseRadius -= 0.3;

            // Outer pulse ring (from center)
            if (ticksAlive % 2 == 0 && outerPulseRadius < 18) {
                World w = center.getWorld();
                if (w != null) {
                    DisplayBuilder.particleRing(center, outerPulseRadius,
                            Particle.REVERSE_PORTAL, 16, null);
                }
                DisplayBuilder.dustParticles(center, 4, (float)outerPulseRadius,
                        0, 200, 255, 1.2f);
            }

            // Inner pulse ring (from edge)
            if (ticksAlive % 2 == 0 && innerPulseRadius > 0) {
                World w = center.getWorld();
                if (w != null) {
                    DisplayBuilder.particleRing(center, innerPulseRadius,
                            Particle.REVERSE_PORTAL, 16, null);
                }
                DisplayBuilder.dustParticles(center, 4, (float)innerPulseRadius,
                        128, 0, 255, 1.2f);
            }

            // Collision check — when pulses meet
            if (!collided && Math.abs(outerPulseRadius - innerPulseRadius) < 2.0) {
                collided = true;
                double collisionRadius = (outerPulseRadius + innerPulseRadius) / 2;
                // Collision ring burst
                DisplayBuilder.particleRing(center, collisionRadius,
                        Particle.ELECTRIC_SPARK, 32, null);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                World w = center.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);

                        // In collision ring (reduced damage)
                        if (Math.abs(dist - collisionRadius) <= 2.0) {
                            p.damage(6.0);
                            DisplayBuilder.cyanDust(p.getLocation(), 8, 0.5);
                        }
                        // Hit by either pulse
                        else if (Math.abs(dist - outerPulseRadius) <= 2.0 ||
                                 Math.abs(dist - innerPulseRadius) <= 2.0) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5,
                                    240, 240, 255, 1.5f);
                        }
                    }
                }
            }

            // Post-collision dissipation
            if (collided && ticksAlive % 8 == 0) {
                double midRadius = (outerPulseRadius + innerPulseRadius) / 2;
                DisplayBuilder.cyanDust(center.clone().add(midRadius, 0.5, 0), 4, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EventHorizonPulse(plugin); }
    }

    // =========================================================================
    // 86. SHROUD
    // The outer 15 blocks are covered in dense black LARGE_SMOKE — waist-high
    // fog that hides the island edge, scar hazards, and ground effects.
    // Positional disorientation — players can't see the edge and may fall off.
    // No direct damage (fall damage from invisible edges), 60s cooldown.
    // =========================================================================
    public static class Shroud extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public Shroud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shroud", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(340);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ring of dark fog blocks at the outer zone
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                for (double r = 8; r <= 18; r += 3) {
                    Location loc = center.clone().add(
                            Math.cos(angle) * r, 0.8, Math.sin(angle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(3.5f, 0.6f, 3.5f).glow(0, 0, 0).interpolation(5, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 60 ticks
            if (ticksAlive < 60) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.particleRing(center, 12.0, Particle.LARGE_SMOKE, 16, null);
                }
                return;
            }

            // Dense smoke in outer zone
            if (ticksAlive % 3 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (int i = 0; i < 10; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = 8.0 + Math.random() * 10.0;
                        Location pt = center.clone().add(
                                Math.cos(a) * d, 0.5 + Math.random() * 1.0,
                                Math.sin(a) * d);
                        w.spawnParticle(Particle.LARGE_SMOKE, pt, 4, 1.5, 0.5, 1.5, 0.01);
                    }
                }
            }

            // Edge-concealing darkness particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center, 6, 12.0, 0, 0, 0, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Shroud(plugin); }
    }

    // =========================================================================
    // 87. DoG WAKE CONSUMPTION
    // DoG's movement wake expands from 2 to 8 blocks in diameter over 3
    // seconds. The expanded wake glows blinding white and deals continuous
    // damage. Simulated as a fast-expanding cylindrical damage zone.
    // 14 HP per second inside expanded wake, fires on every 3rd overhead pass.
    // =========================================================================
    public static class DoGWakeConsumption extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double wakeRadius = 1.0;
        private Location wakeCenter;
        private double wakeAngle;

        public DoGWakeConsumption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_wake_consumption", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(14.0);
            config.setDamageRadius(0.0); // Custom expanding check
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            wakeAngle = Math.random() * Math.PI;
            wakeCenter = center.clone();
            // Initial narrow wake trail
            for (int i = -6; i <= 6; i++) {
                Location loc = center.clone().add(
                        Math.cos(wakeAngle) * i * 2, 0.5,
                        Math.sin(wakeAngle) * i * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(1.0f, 0.2f, 1.0f).glow(240, 240, 255).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Expansion phase: 60 ticks (3 seconds) — wake expands 1 to 4 blocks radius
            if (ticksAlive < 60) {
                wakeRadius = 1.0 + (ticksAlive / 60.0) * 3.0;
                // Expand wake blocks
                if (ticksAlive % 6 == 0) {
                    float scale = (float)(wakeRadius * 2);
                    for (BlockDisplayHandle h : handles) {
                        h.scale(scale, 0.2f, scale).interpolation(4, 0);
                    }
                }
            }

            // Wake particle effects
            if (ticksAlive % 3 == 0) {
                for (int i = -6; i <= 6; i += 2) {
                    Location wakePt = wakeCenter.clone().add(
                            Math.cos(wakeAngle) * i * 2, 1.0,
                            Math.sin(wakeAngle) * i * 2);
                    World w = wakePt.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.END_ROD, wakePt, 4,
                                wakeRadius * 0.5, 0.5, wakeRadius * 0.5, 0.01);
                    }
                    DisplayBuilder.dustParticles(wakePt, 6, (float)wakeRadius,
                            240, 240, 255, 1.5f);
                }
            }

            // Wake damage — cylinder along the wake line
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check distance to wake line
                    Location pLoc = p.getLocation();
                    for (int i = -6; i <= 6; i++) {
                        Location wakePt = wakeCenter.clone().add(
                                Math.cos(wakeAngle) * i * 2, 0,
                                Math.sin(wakeAngle) * i * 2);
                        double dx = pLoc.getX() - wakePt.getX();
                        double dz = pLoc.getZ() - wakePt.getZ();
                        if (dx * dx + dz * dz <= wakeRadius * wakeRadius) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(pLoc, 8, 0.5,
                                    240, 240, 255, 1.5f);
                            break;
                        }
                    }
                }
            }

            // Ambient trail sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGWakeConsumption(plugin); }
    }

    // =========================================================================
    // 88. HUNGER PULSE
    // A single slow-expanding ring of REVERSE_PORTAL particles — 40 blocks
    // in diameter at origin, expanding to island edges over 10 seconds.
    // Players hit by the ring as it passes through them take heavy damage.
    // 16 HP per pulse ring contact, 65s cooldown.
    // =========================================================================
    public static class HungerPulse extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double pulseRadius = 0;

        public HungerPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunger_pulse", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(0.0); // Custom ring check
            config.setDurationTicks(300);
            config.setCooldownTicks(1300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Central marker
            BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.CRYING_OBSIDIAN);
            h.scale(1.0f, 0.5f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 100 ticks (5 seconds) — heartbeat building
            if (ticksAlive < 100) {
                if (ticksAlive % Math.max(5, 20 - ticksAlive / 5) == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.2f);
                    DisplayBuilder.dustParticles(center, 8, 2.0, 128, 0, 255, 1.2f);
                }
                return;
            }

            // Pulse expansion: 200 ticks (10 seconds) to expand to ~20 blocks
            int pulseTick = ticksAlive - 100;
            if (pulseTick < 200) {
                pulseRadius = (pulseTick / 200.0) * 20.0;

                // Pulse ring particles
                if (pulseTick % 3 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        DisplayBuilder.particleRing(center, pulseRadius,
                                Particle.REVERSE_PORTAL, (int)(pulseRadius * 2) + 8, null);
                    }
                    DisplayBuilder.dustParticles(center, 4, (float)pulseRadius,
                            128, 0, 255, 1.3f);
                }

                // Ring contact damage — players at pulse front
                if (pulseTick % 10 == 0) {
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (Math.abs(dist - pulseRadius) <= 2.5) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                    128, 0, 255, 1.5f);
                        }
                    }
                }

                // Pulse sound at intervals
                if (pulseTick % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HungerPulse(plugin); }
    }

    // =========================================================================
    // 89. TOTAL VOID SUPPRESSION
    // All visual light extinguished — LARGE_SMOKE fills the air island-wide.
    // Only DoG's white END_ROD trail remains visible. Environmental attack
    // warnings invisible. Pure positional awareness test.
    // No direct damage, 80s cooldown.
    // =========================================================================
    public static class TotalVoidSuppression extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public TotalVoidSuppression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_void_suppression", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dark overlay blocks — hemisphere of blackstone
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                double dist = 8.0 + Math.random() * 6.0;
                double y = 3.0 + Math.random() * 5.0;
                Location loc = center.clone().add(
                        Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(5.0f, 2.0f, 5.0f).glow(0, 0, 0).interpolation(5, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — barely audible cave sound
            if (ticksAlive < 40) {
                return; // Near-silent approach
            }

            // Dense smoke filling the island
            if (ticksAlive % 3 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 3, 0),
                            15, 12, 4, 12, 0.01);
                }
            }

            // Scale up darkness blocks gradually
            if (ticksAlive < 80) {
                float progress = (ticksAlive - 40) / 40.0f;
                float scale = 5.0f + progress * 5.0f;
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle h : handles) {
                        h.scale(scale, 2.0f + progress * 3.0f, scale).interpolation(8, 0);
                    }
                }
            }

            // Fade phase: last 40 ticks — blocks shrink back
            if (ticksAlive > 160) {
                float fade = (ticksAlive - 160) / 40.0f;
                float scale = 10.0f * (1.0f - fade);
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle h : handles) {
                        h.scale(Math.max(0.1f, scale), Math.max(0.1f, scale * 0.5f),
                                Math.max(0.1f, scale)).interpolation(8, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalVoidSuppression(plugin); }
    }

    // =========================================================================
    // 90. THE ISLAND'S LAST BREATH
    // Fires at 15% DoG HP. The entire island shudders — all displays oscillate.
    // Central sinkhole emits a massive REVERSE_PORTAL column 30 blocks high.
    // All active environmental attacks fire simultaneously at double damage
    // and double speed for 5 seconds.
    // Double current active-event damage values, fires exactly once.
    // =========================================================================
    public static class IslandsLastBreath extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean triggered = false;

        public IslandsLastBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("islands_last_breath", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(999999); // Fires exactly once
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.0f);
            // Central sinkhole column blocks
            for (int y = 0; y < 30; y += 2) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                h.scale(1.5f, 2.0f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 20 ticks (1 second)
            if (ticksAlive < 20) {
                return;
            }

            if (!triggered) {
                triggered = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
            }

            // Island shudder — oscillate column blocks
            if (ticksAlive % 2 == 0) {
                float shudder = (float)Math.sin(ticksAlive * 2.0) * 0.2f;
                for (int i = 0; i < handles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.75f + shudder, -1.0f, -0.75f - shudder),
                            new AxisAngle4f(shudder * 0.5f, 0, 1, 0),
                            new Vector3f(1.5f, 2.0f, 1.5f),
                            new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
            }

            // Massive REVERSE_PORTAL column from sinkhole
            if (ticksAlive % 3 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (int y = 0; y < 30; y += 3) {
                        w.spawnParticle(Particle.REVERSE_PORTAL,
                                center.clone().add(0, y, 0), 12, 1.0, 1.0, 1.0, 0.05);
                    }
                }
            }

            // Island-wide chaos particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(center, 20, 15.0, 240, 240, 255, 1.8f);
                DisplayBuilder.cyanDust(center, 12, 12.0);
                DisplayBuilder.dustParticles(center, 8, 10.0, 128, 0, 255, 1.5f);
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 2, 0),
                            15, 12, 3, 12, 0.05);
                }
            }

            // Ground rupture particles across the island
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 16;
                    Location rupture = center.clone().add(
                            Math.cos(a) * d, 0.3, Math.sin(a) * d);
                    World w = rupture.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, rupture, 8, 0.5, 0.5, 0.5, 0.03);
                    }
                    DisplayBuilder.dustParticles(rupture, 6, 1.0, 240, 240, 255, 1.4f);
                }
            }

            // Continuous shudder sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.5f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IslandsLastBreath(plugin); }
    }
}
