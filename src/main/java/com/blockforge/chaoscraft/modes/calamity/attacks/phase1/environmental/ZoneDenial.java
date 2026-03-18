package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

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
import java.util.Random;

/**
 * GROUP 5: ZONE DENIAL
 * Ten attacks that create persistent dangerous areas players must avoid.
 */
public final class ZoneDenial {

    private ZoneDenial() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidPool(plugin));
        registry.register(new NullField(plugin));
        registry.register(new BurnRing(plugin));
        registry.register(new SporeVeil(plugin));
        registry.register(new StaticWeb(plugin));
        registry.register(new SeismicZone(plugin));
        registry.register(new InvertedGravityTile(plugin));
        registry.register(new ScreamerField(plugin));
        registry.register(new ColdSpot(plugin));
        registry.register(new HungerZone(plugin));
    }

    // =========================================================================
    // ATTACK 41 — Void Pool
    // Circular 6-block void liquid pool materializes; rapid damage + Levitation.
    // =========================================================================
    public static class VoidPool extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean spawned = false;

        public VoidPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pool", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per second
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(400); // 20s
            config.setCooldownTicks(700); // 35s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();

            // Shimmering distortion warning for 2s
            for (int i = 0; i < 16; i++) {
                double angle = i / 16.0 * 2 * Math.PI;
                DisplayBuilder.purpleDust(center.clone().add(Math.cos(angle) * 3, 0.2, Math.sin(angle) * 3), 3, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40) {
                // Distortion rises
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.2, 0), 6, 3.0);
                }
                return;
            }

            if (!spawned) {
                spawned = true;
                // Pool of void liquid: concentric rings of dark blocks
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        if (x * x + z * z <= 9) {
                            Material mat = (x * x + z * z < 4) ? Material.OBSIDIAN : Material.BLACK_CONCRETE;
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(x, 0.02, z), mat);
                            h.scale(0.98f, 0.05f, 0.98f).glow(40, 0, 100).interpolation(2, 8);
                            handles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                DisplayBuilder.purpleDust(center, 20, 3.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.3f);
            }

            // Infinite-depth shimmer from center
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.1, 0), 5, 2.5);
            }

            // Launch players in pool upward (Levitation proxy)
            if (ticksAlive % 10 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        if (ploc.distanceSquared(center) <= 9 && Math.abs(ploc.getY() - groundY) < 2) {
                            // Gentle upward push (Levitation proxy)
                            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 0.25, 0)));
                        }
                    }
                }
            }

            // Drain animation near end
            if (ticksAlive > 340 && ticksAlive % 8 == 0) {
                // Spiral drain
                int idx = (int)(Math.random() * Math.max(1, handles.size()));
                if (idx < handles.size() && handles.get(idx).entity().isValid()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.49f, (float)(-0.02 - Math.random() * 0.1), -0.49f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.98f, 0.05f, 0.98f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(6);
                }
                DisplayBuilder.purpleDust(center, 4, 2.5);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidPool(plugin);
        }
    }

    // =========================================================================
    // ATTACK 42 — Null Field
    // 10x10 zone looks "erased" — matte black tiles. No sprint, no speed effects.
    // =========================================================================
    public static class NullField extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public NullField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_field", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // No direct damage — ability denial only
            config.setDamageRadius(0.0);
            config.setDurationTicks(600); // ~30s (1s warning + 18s field)
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Static flicker warning
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 2.0f);
            DisplayBuilder.purpleDust(center, 8, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20) {
                // Flicker
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 5, 5.0);
                }
                return;
            }

            if (!active) {
                active = true;
                // Matte black 10x10 zone
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.04, z), Material.BLACK_CONCRETE);
                        h.scale(0.99f, 0.06f, 0.99f).glow(0, 0, 0).interpolation(2, 5);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.3f);
            }

            // Null field: cancel sprint velocity boosts (reduce velocity toward normal walk)
            if (ticksAlive % 5 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        if (Math.abs(ploc.getX() - center.getX()) <= 5
                                && Math.abs(ploc.getZ() - center.getZ()) <= 5
                                && Math.abs(ploc.getY() - groundY) < 2) {
                            org.bukkit.util.Vector vel = player.getVelocity();
                            double horizontalSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
                            if (horizontalSpeed > 0.22) {
                                // Clamp to walk speed
                                double factor = 0.22 / horizontalSpeed;
                                player.setVelocity(new org.bukkit.util.Vector(vel.getX() * factor, vel.getY(), vel.getZ() * factor));
                            }
                            // Darkness visual via crimson particles (subtle)
                            DisplayBuilder.purpleDust(ploc, 1, 0.2);
                        }
                    }
                }
            }

            // Subtle null zone pulse
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.crimsonDust(center, 4, 5.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new NullField(plugin);
        }
    }

    // =========================================================================
    // ATTACK 43 — Burn Ring
    // Ring of void fire encircles an 8-block zone; traps players inside.
    // Ring slowly shrinks over 15s; damages anyone on the boundary.
    // =========================================================================
    public static class BurnRing extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double ringRadius = 4.0;
        private double groundY;
        private boolean ringUp = false;

        public BurnRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burn_ring", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(6.0); // 3 hearts per second in boundary
            config.setDamageRadius(1.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(640); // ~32s
            config.setCooldownTicks(1300); // 65s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Ring circumference glow warning
            for (int i = 0; i < 24; i++) {
                double angle = i / 24.0 * 2 * Math.PI;
                DisplayBuilder.purpleDust(center.clone().add(
                        Math.cos(angle) * 4, 0.5, Math.sin(angle) * 4), 4, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 60) {
                // Intensifying ring glow
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 16; i++) {
                        double angle = i / 16.0 * 2 * Math.PI;
                        DisplayBuilder.crimsonDust(center.clone().add(
                                Math.cos(angle) * 4, 0.3 + ticksAlive * 0.01, Math.sin(angle) * 4), 3, 0.4);
                    }
                }
                return;
            }

            if (!ringUp) {
                ringUp = true;
                buildRing(center, ringRadius);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.3f);
                DisplayBuilder.purpleDust(center, 20, ringRadius);
            }

            // Shrink ring over 15 seconds (300 ticks): from r=4 to r=0
            if (ticksAlive >= 60 && ticksAlive <= 360 && ticksAlive % 10 == 0) {
                ringRadius = Math.max(0.3, 4.0 - (ticksAlive - 60) / 75.0);
                // Rebuild ring at new radius
                for (BlockDisplayHandle h : handles) {
                    if (h.entity().isValid()) h.entity().remove();
                }
                handles.clear();
                if (ringRadius > 0.5) {
                    buildRing(center, ringRadius);
                }
            }

            // Damage players touching the ring boundary
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(center);
                        if (Math.abs(dist - ringRadius) < 1.2 && Math.abs(player.getLocation().getY() - groundY) < 2) {
                            player.damage(6.0);
                            DisplayBuilder.crimsonDust(player.getLocation(), 6, 0.6);
                        }
                    }
                }
            }

            // Ring pulse
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = i / 12.0 * 2 * Math.PI;
                    DisplayBuilder.crimsonDust(center.clone().add(
                            Math.cos(angle) * ringRadius, 0.5, Math.sin(angle) * ringRadius), 2, 0.3);
                    DisplayBuilder.purpleDust(center.clone().add(
                            Math.cos(angle) * ringRadius, 1.0, Math.sin(angle) * ringRadius), 2, 0.3);
                }
            }
        }

        private void buildRing(Location center, double r) {
            int segments = Math.max(8, (int)(r * 8));
            for (int i = 0; i < segments; i++) {
                double angle = i / (double) segments * 2 * Math.PI;
                double px = center.getX() + Math.cos(angle) * r;
                double pz = center.getZ() + Math.sin(angle) * r;
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(center.getWorld(), px, groundY + y, pz), Material.MAGMA_BLOCK);
                    h.scale(0.35f, 1.0f, 0.35f).glow(200, 0, 50).interpolation(2, 5);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new BurnRing(plugin);
        }
    }

    // =========================================================================
    // ATTACK 44 — Spore Veil
    // Luminescent black-purple spore cloud over a 12x12 zone.
    // Poisons players; sprinting worsens the effect.
    // =========================================================================
    public static class SporeVeil extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public SporeVeil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spore_veil", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // 0.5 hearts per tick interval (Poison II proxy)
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(40); // Every 2s
            config.setDurationTicks(800); // ~40s
            config.setCooldownTicks(1000); // 50s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Spores drifting down warning
            for (int i = 0; i < 12; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 12;
                double oz = (RNG.nextDouble() - 0.5) * 12;
                DisplayBuilder.purpleDust(center.clone().add(ox, 5, oz), 3, 0.7);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 60) {
                // Spores accumulating — sparse then dense
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double ox = (RNG.nextDouble() - 0.5) * 12;
                        double oz = (RNG.nextDouble() - 0.5) * 12;
                        double startY = 3 + RNG.nextDouble() * 4;
                        DisplayBuilder.purpleDust(center.clone().add(ox, startY, oz), 2, 0.5);
                    }
                }
                return;
            }

            if (!active) {
                active = true;
                // Dense spore cloud: floating blocks at various heights
                for (int i = 0; i < 28; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 12;
                    double oz = (RNG.nextDouble() - 0.5) * 12;
                    double oy = 0.5 + RNG.nextDouble() * 3.0;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(ox, oy, oz), Material.PURPLE_CONCRETE);
                    float s = 0.15f + RNG.nextFloat() * 0.2f;
                    h.scale(s, s, s).glow(100, 0, 200).interpolation(3, 20);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.purpleDust(center, 25, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.0f, 0.3f);
            }

            // Drift spore blocks gently
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    Location loc = h.entity().getLocation();
                    float driftX = (float)(Math.sin(ticksAlive * 0.04 + loc.getX()) * 0.03);
                    float driftZ = (float)(Math.cos(ticksAlive * 0.03 + loc.getZ()) * 0.03);
                    float driftY = (float)(Math.sin(ticksAlive * 0.05 + loc.getZ()) * 0.015);
                    float s = 0.15f + RNG.nextFloat() * 0.2f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-s / 2 + driftX, -s / 2 + driftY, -s / 2 + driftZ),
                            new AxisAngle4f(ticksAlive * 0.03f, 0, 1, 0),
                            new Vector3f(s, s, s),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(8);
                }
            }

            // Extra damage for sprinting players (Poison III equivalent)
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        if (ploc.distanceSquared(center) <= 36 && Math.abs(ploc.getY() - groundY) < 4) {
                            if (player.isSprinting()) {
                                player.damage(2.0); // Extra Poison III damage for sprinting
                                DisplayBuilder.crimsonDust(ploc, 5, 0.5);
                            }
                        }
                    }
                }
            }

            // Ambient buzz
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), 10, 5.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SporeVeil(plugin);
        }
    }

    // =========================================================================
    // ATTACK 45 — Static Web
    // Crackling void-static web across a 10x10 zone. Moving slowly avoids strands.
    // =========================================================================
    public static class StaticWeb extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean webActive = false;

        public StaticWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("static_web", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // 1 heart per strand touched
            config.setDamageRadius(0.8);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(600); // ~30s
            config.setCooldownTicks(800); // 40s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();

            // Anchor points glow warning
            for (int i = 0; i < 8; i++) {
                double angle = i / 8.0 * 2 * Math.PI;
                double px = center.getX() + Math.cos(angle) * 5;
                double pz = center.getZ() + Math.sin(angle) * 5;
                DisplayBuilder.cyanDust(new Location(w, px, groundY + 0.3, pz), 4, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = i / 8.0 * 2 * Math.PI;
                        DisplayBuilder.cyanDust(center.clone().add(Math.cos(angle) * 5, 0.5, Math.sin(angle) * 5), 3, 0.6);
                    }
                }
                return;
            }

            if (!webActive) {
                webActive = true;
                // Build web: radial strands from center + concentric rings
                // Radial strands: 16 strands from center outward
                for (int strand = 0; strand < 16; strand++) {
                    double angle = strand / 16.0 * 2 * Math.PI;
                    for (int r = 1; r <= 5; r++) {
                        double px = center.getX() + Math.cos(angle) * r;
                        double pz = center.getZ() + Math.sin(angle) * r;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(w, px, groundY + 0.15 + r * 0.05, pz), Material.WHITE_STAINED_GLASS);
                        h.scale(0.08f, 0.08f, 0.7f).glow(200, 150, 255).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Concentric ring at r=3
                for (int i = 0; i < 20; i++) {
                    double angle = i / 20.0 * 2 * Math.PI;
                    double px = center.getX() + Math.cos(angle) * 3;
                    double pz = center.getZ() + Math.sin(angle) * 3;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY + 0.25, pz), Material.WHITE_STAINED_GLASS);
                    h.scale(0.06f, 0.06f, 0.6f).glow(200, 180, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center, 20, 5.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 1.0f);
            }

            // Light up touched strands & damage players moving through fast
            if (ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (ploc.distanceSquared(center) > 36) continue;

                    // Check proximity to any strand
                    for (BlockDisplayHandle h : handles) {
                        if (!h.entity().isValid()) continue;
                        Location sloc = h.entity().getLocation();
                        if (ploc.distanceSquared(sloc) <= 1.0) {
                            // Strand touched — light up
                            BlockDisplay bd = (BlockDisplay) h.entity();
                            bd.setTransformation(new Transformation(
                                    new Vector3f(-0.04f, -0.04f, -0.35f),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.08f, 0.08f, 0.7f),
                                    new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(2);
                            DisplayBuilder.cyanDust(sloc, 6, 0.4);

                            // Damage if moving fast
                            double speed = player.getVelocity().length();
                            if (speed > 0.18) {
                                player.damage(2.0);
                            }
                            break;
                        }
                    }
                }
            }

            // Crackle ambience
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center, 4, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new StaticWeb(plugin);
        }
    }

    // =========================================================================
    // ATTACK 46 — Seismic Zone
    // 8-block zone erupts with random micro-blasts every 0.5s.
    // Random positions make safe positioning impossible.
    // =========================================================================
    public static class SeismicZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public SeismicZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_zone", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0); // 2 hearts per micro-blast
            config.setImpactRadius(1.5);
            config.setDurationTicks(500); // ~25s
            config.setCooldownTicks(900); // 45s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Zone glow + rapid ticking
            DisplayBuilder.purpleDust(center, 8, 4.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) {
                // Glowing + ticking warning
                if (ticksAlive % 4 == 0) {
                    double ox = (RNG.nextDouble() - 0.5) * 8;
                    double oz = (RNG.nextDouble() - 0.5) * 8;
                    DisplayBuilder.purpleDust(center.clone().add(ox, 0.5, oz), 3, 0.6);
                }
                return;
            }

            if (!active) {
                active = true;
                // Zone marker: subtle pulsing floor blocks
                for (int x = -4; x <= 4; x += 2) {
                    for (int z = -4; z <= 4; z += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.03, z), Material.PURPLE_CONCRETE);
                        h.scale(1.5f, 0.05f, 1.5f).glow(80, 0, 150).interpolation(2, 8);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Micro-blasts every 10 ticks (0.5s)
            if (ticksAlive % 10 == 0) {
                int blastCount = 1 + RNG.nextInt(3);
                for (int b = 0; b < blastCount; b++) {
                    double ox = (RNG.nextDouble() - 0.5) * 8;
                    double oz = (RNG.nextDouble() - 0.5) * 8;
                    Location blastLoc = center.clone().add(ox, 0, oz);
                    triggerImpactDamage(blastLoc);

                    // Micro-eruption visual: short spike
                    BlockDisplayHandle spike = displayBuilder.spawnBlock(blastLoc.clone().add(0, 0.5, 0), Material.OBSIDIAN);
                    spike.scale(0.4f, 1.2f, 0.4f).glow(150, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(spike.entity());
                    handles.add(spike);

                    DisplayBuilder.purpleDust(blastLoc, 8, 1.2);
                    DisplayBuilder.crimsonDust(blastLoc, 4, 0.8);

                    // Knockback players hit
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(blastLoc) <= 2.25) {
                            double angle = RNG.nextDouble() * 2 * Math.PI;
                            player.setVelocity(player.getVelocity().add(
                                    new org.bukkit.util.Vector(
                                            Math.cos(angle) * 0.8, 0.6, Math.sin(angle) * 0.8)));
                        }
                    }
                }

                if (RNG.nextInt(3) == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 1.2f);
                }
            }

            // Remove old spikes to keep entity count low
            if (handles.size() > 40) {
                for (int i = 0; i < 5 && !handles.isEmpty(); i++) {
                    BlockDisplayHandle h = handles.remove(0);
                    if (h.entity().isValid()) h.entity().remove();
                }
            }

            // Zone pulse
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center, 5, 4.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SeismicZone(plugin);
        }
    }

    // =========================================================================
    // ATTACK 47 — Inverted Gravity Tile
    // 6x6 tile zone flips gravity; players float up to 15 blocks high,
    // then fall on deactivation.
    // =========================================================================
    public static class InvertedGravityTile extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean zoneActive = false;
        private boolean released = false;
        private int activeStart = 0;

        public InvertedGravityTile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_gravity_tile", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // Fall damage on deactivation
            config.setImpactRadius(4.0);
            config.setDurationTicks(600); // ~30s (10s active)
            config.setCooldownTicks(1100); // 55s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Ascending particles + low hum warning
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    DisplayBuilder.cyanDust(center.clone().add(x, 0.5 + Math.random(), z), 2, 0.4);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) {
                // Ripple ascending
                if (ticksAlive % 5 == 0) {
                    for (int x = -3; x <= 3; x += 2) {
                        for (int z = -3; z <= 3; z += 2) {
                            DisplayBuilder.cyanDust(center.clone().add(x, ticksAlive * 0.04, z), 2, 0.3);
                        }
                    }
                }
                return;
            }

            if (!zoneActive) {
                zoneActive = true;
                activeStart = ticksAlive;
                // Hovering tiles
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.1, z), Material.CYAN_CONCRETE);
                        h.scale(0.95f, 0.08f, 0.95f).glow(0, 200, 255).interpolation(2, 5);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.cyanDust(center, 20, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
            }

            // Hover tiles slightly (bob animation)
            if (zoneActive && !released && ticksAlive % 5 == 0) {
                float hover = 0.08f + (float) Math.abs(Math.sin(ticksAlive * 0.1)) * 0.04f;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.475f, hover / 2, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, hover, 0.95f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }

            // Launch players on tiles upward
            if (zoneActive && !released && ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - center.getX());
                    double dz = Math.abs(ploc.getZ() - center.getZ());
                    if (dx <= 3 && dz <= 3 && Math.abs(ploc.getY() - groundY) < 3) {
                        double currentY = ploc.getY();
                        if (currentY < groundY + 15) {
                            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 0.35, 0)));
                        } else {
                            // At ceiling: hold in place
                            player.setVelocity(new org.bukkit.util.Vector(
                                    player.getVelocity().getX() * 0.2, 0, player.getVelocity().getZ() * 0.2));
                        }
                    }
                }
            }

            // Ascending particle streams
            if (ticksAlive % 6 == 0 && zoneActive && !released) {
                for (int x = -3; x <= 3; x += 3) {
                    for (int z = -3; z <= 3; z += 3) {
                        double height = (ticksAlive - activeStart) * 0.1 % 15;
                        DisplayBuilder.cyanDust(center.clone().add(x, height, z), 2, 0.3);
                    }
                }
            }

            // Deactivate after 10s (200 ticks)
            if (!released && ticksAlive >= activeStart + 200) {
                released = true;
                // Tiles drop
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.475f, -0.5f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.08f, 0.95f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                // Trigger impact damage for fall
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 15, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new InvertedGravityTile(plugin);
        }
    }

    // =========================================================================
    // ATTACK 48 — Screamer Field
    // 10x10 zone of invisible pressure with screaming mouth shapes in distortion.
    // No damage, but severe visual disturbance (particle-based vision noise).
    // =========================================================================
    public static class ScreamerField extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public ScreamerField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screamer_field", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // Pure sensory denial
            config.setDamageRadius(0.0);
            config.setDurationTicks(680); // ~34s
            config.setCooldownTicks(1400); // 70s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Rising ambient scream warning
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) {
                // Rising scream intensity
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center, (int)(3 + ticksAlive * 0.05), 5.0);
                }
                return;
            }

            if (!active) {
                active = true;
                // Shimmering bent-glass effect: barely-visible thin blocks
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.05 + Math.random() * 2.5, z), Material.TINTED_GLASS);
                        float s = 0.03f + RNG.nextFloat() * 0.06f;
                        h.scale(s, s * 5, s).glow(80, 0, 120).interpolation(3, 10);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.4f);
            }

            // Scream shapes: partial mouth profiles appear and vanish in distortion
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 10;
                double oz = (RNG.nextDouble() - 0.5) * 10;
                double oy = 1 + RNG.nextDouble() * 2;
                // Mouth arc: crimson curved particles
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI + (i / 5.0 * Math.PI);
                    DisplayBuilder.crimsonDust(center.clone().add(ox + Math.cos(angle) * 0.8, oy, oz + Math.sin(angle) * 0.5), 2, 0.2);
                }
            }

            // Distortion effect on players inside: rapid visual-noise particles near face
            if (ticksAlive % 3 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (Math.abs(ploc.getX() - center.getX()) <= 5
                            && Math.abs(ploc.getZ() - center.getZ()) <= 5
                            && Math.abs(ploc.getY() - groundY) < 3) {
                        // Noise at player eye level
                        DisplayBuilder.purpleDust(ploc.clone().add(
                                (RNG.nextDouble() - 0.5) * 1.5, 1.6, (RNG.nextDouble() - 0.5) * 1.5), 2, 0.2);
                        DisplayBuilder.crimsonDust(ploc.clone().add(
                                (RNG.nextDouble() - 0.5) * 1.0, 1.5, (RNG.nextDouble() - 0.5) * 1.0), 1, 0.15);
                    }
                }
            }

            // Screamer audio pulse
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ScreamerField(plugin);
        }
    }

    // =========================================================================
    // ATTACK 49 — Cold Spot
    // 7x7 zone becomes unnaturally cold — ice frost, desaturated, heavy debuffs.
    // Accumulates: 5+ continuous seconds adds Weakness.
    // =========================================================================
    public static class ColdSpot extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public ColdSpot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_spot", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // No direct damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(800); // ~40s
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Cold breath warning + frost particles
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.random() > 0.5) {
                        DisplayBuilder.cyanDust(center.clone().add(x, 0.3, z), 2, 0.3);
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) {
                // Frost spreading
                if (ticksAlive % 5 == 0) {
                    double spread = ticksAlive / 60.0 * 4;
                    for (int i = 0; i < 8; i++) {
                        double angle = i / 8.0 * 2 * Math.PI;
                        DisplayBuilder.cyanDust(center.clone().add(
                                Math.cos(angle) * spread, 0.2, Math.sin(angle) * spread), 2, 0.4);
                    }
                }
                return;
            }

            if (!active) {
                active = true;
                // Frost tiles + ice crystals
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Material mat = (Math.random() < 0.3) ? Material.BLUE_ICE : Material.LIGHT_BLUE_CONCRETE;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.04, z), mat);
                        h.scale(0.97f, 0.07f, 0.97f).glow(0, 150, 255).interpolation(2, 8);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Ice crystal spires (sparse)
                for (int i = 0; i < 5; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 6;
                    double oz = (RNG.nextDouble() - 0.5) * 6;
                    BlockDisplayHandle crystal = displayBuilder.spawnBlock(
                            center.clone().add(ox, 0.5, oz), Material.PACKED_ICE);
                    crystal.scale(0.2f, 1.2f, 0.2f).glow(150, 200, 255).interpolation(2, 5);
                    handles.add(crystal);
                    spawnedEntities.add(crystal.entity());
                }
                DisplayBuilder.cyanDust(center, 20, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            }

            // Slow players in zone (velocity dampening = Mining Fatigue + Slowness proxy)
            if (ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (Math.abs(ploc.getX() - center.getX()) <= 3.5
                            && Math.abs(ploc.getZ() - center.getZ()) <= 3.5
                            && Math.abs(ploc.getY() - groundY) < 2) {
                        // Slowness: clamp horizontal velocity
                        org.bukkit.util.Vector vel = player.getVelocity();
                        double hSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());
                        double maxSpeed = 0.15;
                        if (hSpeed > maxSpeed) {
                            double factor = maxSpeed / hSpeed;
                            player.setVelocity(new org.bukkit.util.Vector(vel.getX() * factor, vel.getY(), vel.getZ() * factor));
                        }
                        // Cold breath particle
                        DisplayBuilder.cyanDust(ploc.clone().add(0, 1.6, 0), 2, 0.25);
                    }
                }
            }

            // Frost crystal shimmer
            if (ticksAlive % 12 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 7;
                    double oz = (RNG.nextDouble() - 0.5) * 7;
                    DisplayBuilder.cyanDust(center.clone().add(ox, 0.5, oz), 3, 0.4);
                }
            }

            // Ice crystal animations
            if (ticksAlive % 15 == 0) {
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    if (h.entity().getLocation().getY() > groundY + 0.3) { // Is a crystal not a tile
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        float shimmer = 1.2f + (float) Math.sin(ticksAlive * 0.12) * 0.15f;
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.1f, -shimmer / 2, -0.1f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.2f, shimmer, 0.2f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(10);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ColdSpot(plugin);
        }
    }

    // =========================================================================
    // ATTACK 50 — Hunger Zone
    // 10x10 zone darkens to absolute black, suppresses healing.
    // Wither damage after 8s inside. Wet organic breathing from center.
    // =========================================================================
    public static class HungerZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double groundY;
        private boolean active = false;

        public HungerZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunger_zone", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // Wither-proxy damage (1 heart) after 8s
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(40); // Every 2s once active
            config.setDurationTicks(720); // ~36s
            config.setCooldownTicks(1600); // 80s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Gradual dim warning over 4s (80 ticks)
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 80) {
                // Gradually deepening darkness
                double radius = 5.0 * (ticksAlive / 80.0);
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = i / 8.0 * 2 * Math.PI;
                        DisplayBuilder.purpleDust(center.clone().add(
                                Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius), 2, 0.4);
                    }
                }
                return;
            }

            if (!active) {
                active = true;
                // Absolute black zone tiles
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, 0.03, z), Material.BLACK_CONCRETE);
                        h.scale(0.99f, 0.06f, 0.99f).glow(0, 0, 0).interpolation(2, 10);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Central red pulse entity
                BlockDisplayHandle pulse = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.MAGMA_BLOCK);
                pulse.scale(0.3f, 0.15f, 0.3f).glow(80, 0, 0).interpolation(2, 10);
                handles.add(pulse);
                spawnedEntities.add(pulse.entity());

                DisplayBuilder.purpleDust(center, 20, 5.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.2f, 0.2f);
            }

            // Breathing pulse at center
            if (ticksAlive % 12 == 0) {
                float breathScale = 0.3f + (float) Math.abs(Math.sin(ticksAlive * 0.08)) * 0.3f;
                if (!handles.isEmpty() && handles.get(handles.size() - 1).entity().isValid()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(handles.size() - 1).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-breathScale / 2, -0.075f, -breathScale / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(breathScale, 0.15f, breathScale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);
                }
                // Breathing sound
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4f, 0.3f + (float)(ticksAlive % 60) * 0.005f);
            }

            // Red central glow pulse
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.2, 0), 4, 0.5);
            }

            // Zone darkness swirl
            if (ticksAlive % 8 == 0) {
                double swirl = ticksAlive * 0.15;
                DisplayBuilder.purpleDust(center.clone().add(
                        Math.cos(swirl) * 2, 1.0, Math.sin(swirl) * 2), 4, 0.6);
            }

            // Suppress healing visual (no actual healing suppression without potion API,
            // but visual telegraphs that healing is blocked — particles rise from players)
            if (ticksAlive % 20 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (Math.abs(ploc.getX() - center.getX()) <= 5
                            && Math.abs(ploc.getZ() - center.getZ()) <= 5
                            && Math.abs(ploc.getY() - groundY) < 2) {
                        // Healing-suppression: dark rising particles from player
                        DisplayBuilder.purpleDust(ploc.clone().add(0, 1, 0), 3, 0.4);
                        DisplayBuilder.crimsonDust(ploc.clone().add(0, 1.5, 0), 2, 0.3);
                    }
                }
            }

            // Wither damage begins at 8s (160 ticks after active = tick 240 total)
            // Base class handles interval damage; this fires extra damage for prolonged exposure
            if (ticksAlive > 240 && ticksAlive % 40 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (Math.abs(ploc.getX() - center.getX()) <= 5
                            && Math.abs(ploc.getZ() - center.getZ()) <= 5
                            && Math.abs(ploc.getY() - groundY) < 2) {
                        player.damage(2.0); // Wither I equivalent (1 heart per 2s)
                        DisplayBuilder.crimsonDust(ploc, 6, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new HungerZone(plugin);
        }
    }
}
