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
 * GROUP 3: EDGE HAZARDS
 * Ten attacks originating from the island perimeter and void edges.
 */
public final class EdgeHazards {

    private EdgeHazards() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidSurge(plugin));
        registry.register(new EdgeCollapse(plugin));
        registry.register(new VoidTendrilsFromEdge(plugin));
        registry.register(new PerimeterSpikeWall(plugin));
        registry.register(new AbyssalWhip(plugin));
        registry.register(new EdgeVoidMist(plugin));
        registry.register(new VoidShark(plugin));
        registry.register(new GravityInversionZone(plugin));
        registry.register(new TheBeckoning(plugin));
        registry.register(new EdgeShatter(plugin));
    }

    // =========================================================================
    // ATTACK 21 — Void Surge
    // A 4-block-tall horizontal wave of void energy rolls from one edge to center.
    // =========================================================================
    public static class VoidSurge extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double waveX;     // current position of wave leading edge
        private double targetX;   // opposite side
        private double waveDir;   // +1 or -1
        private double groundY;
        private boolean spawned = false;

        public VoidSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_surge", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(3.0);
            config.setDurationTicks(400); // ~20s
            config.setCooldownTicks(1000); // 50s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Pick random edge direction
            waveDir = RNG.nextBoolean() ? 1.0 : -1.0;
            waveX = center.getX() - waveDir * 20;
            targetX = center.getX() + waveDir * 20;

            // Warning particles at the origin edge
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.purpleDust(new Location(w, waveX, groundY + 2, center.getZ() + z), 3, 0.8);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 80) {
                // Warning: ripples advancing across ground tiles
                if (ticksAlive % 8 == 0) {
                    double rippleX = waveX + waveDir * ticksAlive * 0.15;
                    for (int z = -10; z <= 10; z++) {
                        DisplayBuilder.purpleDust(new Location(w, rippleX, groundY + 0.2, center.getZ() + z), 2, 0.4);
                    }
                }
                return;
            }

            // Spawn wave wall on first tick past warning
            if (!spawned) {
                spawned = true;
                for (int y = 0; y < 4; y++) {
                    for (int z = -10; z <= 10; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                new Location(w, waveX, groundY + y, center.getZ() + z),
                                y == 0 ? Material.OBSIDIAN : Material.PURPLE_CONCRETE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(100, 0, 200).interpolation(1, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Move wave
            double speed = 0.45;
            waveX += waveDir * speed;

            for (BlockDisplayHandle h : handles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(waveDir * speed, 0, 0));
            }

            // Damage players hit
            if (ticksAlive % 2 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double distFromWave = Math.abs(ploc.getX() - waveX);
                    double dz = Math.abs(ploc.getZ() - center.getZ());
                    if (distFromWave < 2.0 && dz < 10 && ploc.getY() < groundY + 4.5) {
                        triggerImpactDamage(ploc);
                        // Knockback away from origin
                        org.bukkit.util.Vector kb = new org.bukkit.util.Vector(waveDir * 2.0, 0.4, 0);
                        player.setVelocity(player.getVelocity().add(kb));
                    }
                }
            }

            // Wave front particles
            if (ticksAlive % 3 == 0) {
                for (int z = -10; z <= 10; z += 3) {
                    DisplayBuilder.purpleDust(new Location(w, waveX + waveDir, groundY + 1, center.getZ() + z), 3, 0.6);
                }
            }

            // Done when wave passes target
            if ((waveDir > 0 && waveX > targetX) || (waveDir < 0 && waveX < targetX)) {
                for (BlockDisplayHandle h : handles) {
                    if (h.entity().isValid()) h.entity().remove();
                }
                handles.clear();
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidSurge(plugin);
        }
    }

    // =========================================================================
    // ATTACK 22 — Edge Collapse
    // A 5-block section of island perimeter crumbles into the void.
    // =========================================================================
    public static class EdgeCollapse extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double edgeX;
        private double edgeZ;
        private boolean collapseTriggered = false;
        private double groundY;

        public EdgeCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_collapse", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts on void catch
            config.setImpactRadius(2.5);
            config.setDurationTicks(1000); // ~50s (30s regen wait)
            config.setCooldownTicks(1600); // 80s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Pick a random edge direction and offset
            double[] offsets = {16, -16};
            edgeX = center.getX() + offsets[RNG.nextInt(2)];
            edgeZ = center.getZ() + (RNG.nextDouble() - 0.5) * 6;

            // Render 5 perimeter tile blocks
            for (int i = -2; i <= 2; i++) {
                Location tileLoc = new Location(w, edgeX, groundY, center.getZ() + i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.END_STONE);
                h.scale(1.0f, 0.6f, 1.0f).glow(80, 60, 150).interpolation(2, 10);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Warning: crack particles
            for (int i = -2; i <= 2; i++) {
                DisplayBuilder.crimsonDust(new Location(w, edgeX, groundY + 0.3, center.getZ() + i), 4, 0.6);
            }
            DisplayBuilder.playSound(new Location(w, edgeX, groundY, edgeZ), Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) {
                // Crumbling: tiles darken and shake
                if (ticksAlive % 6 == 0) {
                    for (int i = -2; i <= 2; i++) {
                        DisplayBuilder.crimsonDust(new Location(w, edgeX, groundY + 0.4, center.getZ() + i), 3, 0.5);
                    }
                }
                return;
            }

            if (!collapseTriggered) {
                collapseTriggered = true;
                // Drop tile blocks
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -5f, -0.5f),
                            new AxisAngle4f(0.8f, 1, 0, 0),
                            new Vector3f(1.0f, 0.6f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);
                }

                triggerImpactDamage(new Location(w, edgeX, groundY, edgeZ));
                // Teleport players on the edge tiles to center + damage
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = Math.abs(ploc.getX() - edgeX);
                    double dz = ploc.getZ() - center.getZ();
                    if (dx < 2 && Math.abs(dz) < 3) {
                        player.teleport(center.clone().add(0, 0.5, 0));
                    }
                }

                DisplayBuilder.purpleDust(new Location(w, edgeX, groundY, edgeZ), 20, 4.0);
                DisplayBuilder.playSound(new Location(w, edgeX, groundY, edgeZ), Sound.BLOCK_STONE_PLACE, 1.8f, 0.2f);
            }

            // Dust during regen wait
            if (ticksAlive > 80 && ticksAlive % 25 == 0) {
                DisplayBuilder.crimsonDust(new Location(w, edgeX, groundY, edgeZ), 6, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EdgeCollapse(plugin);
        }
    }

    // =========================================================================
    // ATTACK 23 — Void Tendrils from the Edge
    // 5-8 tendrils creep inward from a single edge at walking speed.
    // =========================================================================
    public static class VoidTendrilsFromEdge extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        private static class Tendril {
            List<BlockDisplayHandle> segments = new ArrayList<>();
            double startX, startZ, dirX, dirZ;
            double length = 0;
            boolean retreating = false;
        }

        private final List<Tendril> tendrils = new ArrayList<>();
        private double groundY;
        private double edgeDir; // the X coordinate of the edge

        public VoidTendrilsFromEdge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendrils_edge", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0); // 2 hearts on touch
            config.setImpactRadius(1.5);
            config.setDurationTicks(500); // ~25s
            config.setCooldownTicks(800); // 40s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            edgeDir = RNG.nextBoolean() ? 1.0 : -1.0;
            double edgeX = center.getX() + edgeDir * 18;

            int tendrilCount = 5 + RNG.nextInt(4);
            for (int t = 0; t < tendrilCount; t++) {
                Tendril td = new Tendril();
                td.startX = edgeX;
                td.startZ = center.getZ() + (RNG.nextDouble() - 0.5) * 20;
                td.dirX = -edgeDir;
                td.dirZ = (RNG.nextDouble() - 0.5) * 0.3;
                tendrils.add(td);
            }

            // Warning: formation at edge boundary
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.purpleDust(new Location(w, edgeX, groundY + 0.5, center.getZ() + z), 2, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) return; // Warning phase

            boolean allDone = true;

            for (Tendril td : tendrils) {
                if (td.retreating && td.segments.isEmpty()) continue;
                allDone = false;

                if (!td.retreating) {
                    // Extend: add a segment every 4 ticks
                    if (ticksAlive % 4 == 0 && td.length < 15) {
                        td.length += 0.6;
                        double sx = td.startX + td.dirX * td.length;
                        double sz = td.startZ + td.dirZ * td.length;
                        Location segLoc = new Location(w, sx, groundY + 0.15, sz);

                        Material mat = td.segments.size() % 4 == 0 ? Material.OBSIDIAN : Material.PURPLE_CONCRETE;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                        h.scale(0.55f, 0.25f, 0.55f).glow(100, 0, 200).interpolation(1, 0);
                        td.segments.add(h);
                        spawnedEntities.add(h.entity());

                        // Damage players in tendril path
                        for (org.bukkit.entity.Player player : w.getPlayers()) {
                            if (isExempt(player)) continue;
                            if (player.getLocation().distanceSquared(segLoc) <= 2.25) {
                                triggerImpactDamage(segLoc);
                            }
                        }
                    }

                    // Begin retract at 15 blocks or after 10 seconds
                    if (td.length >= 15 || ticksAlive > 260) {
                        td.retreating = true;
                    }
                } else {
                    // Retract: remove last segment every 3 ticks
                    if (ticksAlive % 3 == 0 && !td.segments.isEmpty()) {
                        BlockDisplayHandle last = td.segments.remove(td.segments.size() - 1);
                        if (last.entity().isValid()) {
                            DisplayBuilder.purpleDust(last.entity().getLocation(), 3, 0.5);
                            last.entity().remove();
                        }
                    }
                }

                // Tendril pulse particles
                if (ticksAlive % 15 == 0 && !td.segments.isEmpty()) {
                    Location tipLoc = td.segments.get(td.segments.size() - 1).entity().getLocation();
                    DisplayBuilder.purpleDust(tipLoc, 4, 0.8);
                }
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() {
            for (Tendril td : tendrils) {
                for (BlockDisplayHandle h : td.segments) {
                    if (h.entity().isValid()) h.entity().remove();
                }
                td.segments.clear();
            }
            tendrils.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidTendrilsFromEdge(plugin);
        }
    }

    // =========================================================================
    // ATTACK 24 — Perimeter Spike Wall
    // Full circumference of black void spikes erupts simultaneously — 3 blocks tall.
    // =========================================================================
    public static class PerimeterSpikeWall extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean wallUp = false;
        private double groundY;

        public PerimeterSpikeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("perimeter_spike_wall", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts
            config.setImpactRadius(2.0);
            config.setDurationTicks(600); // ~30s (15s wall persist)
            config.setCooldownTicks(1300); // 65s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            // Ring flash warning
            int perimeter = 72; // circumference points
            for (int i = 0; i < perimeter; i++) {
                double angle = i / (double) perimeter * 2 * Math.PI;
                double px = center.getX() + Math.cos(angle) * 18;
                double pz = center.getZ() + Math.sin(angle) * 18;
                DisplayBuilder.purpleDust(new Location(w, px, groundY + 0.5, pz), 2, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) {
                // Crack-line warning
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 36; i++) {
                        double angle = i / 36.0 * 2 * Math.PI;
                        DisplayBuilder.crimsonDust(new Location(w,
                                center.getX() + Math.cos(angle) * 18, groundY + 0.3,
                                center.getZ() + Math.sin(angle) * 18), 2, 0.4);
                    }
                }
                return;
            }

            if (!wallUp) {
                wallUp = true;
                // Spawn spike every 3 degrees = 120 spikes total; every other one is 3 tall
                int spikeCount = 40;
                for (int s = 0; s < spikeCount; s++) {
                    double angle = s / (double) spikeCount * 2 * Math.PI;
                    double px = center.getX() + Math.cos(angle) * 18;
                    double pz = center.getZ() + Math.sin(angle) * 18;
                    int h = 2 + (s % 3 == 0 ? 1 : 0);
                    for (int y = 0; y < h; y++) {
                        BlockDisplayHandle bd = displayBuilder.spawnBlock(
                                new Location(w, px, groundY + y, pz), Material.OBSIDIAN);
                        float scale = 0.85f - y * 0.2f;
                        bd.scale(Math.max(0.1f, scale), 1.0f, Math.max(0.1f, scale))
                                .glow(128, 0, 255).interpolation(2, 5);
                        handles.add(bd);
                        spawnedEntities.add(bd.entity());
                    }
                }

                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 40, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);

                // Knock edge players inward
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dist = ploc.distance(center);
                    if (dist >= 15 && dist <= 20) {
                        org.bukkit.util.Vector inward = center.toVector().subtract(ploc.toVector()).normalize().multiply(1.8);
                        player.setVelocity(player.getVelocity().add(inward.add(new org.bukkit.util.Vector(0, 0.4, 0))));
                        player.damage(6.0);
                    }
                }
            }

            // Wall crackle
            if (ticksAlive % 20 == 0) {
                int idx = (int)(Math.random() * Math.max(1, handles.size()));
                if (idx < handles.size() && handles.get(idx).entity().isValid()) {
                    DisplayBuilder.purpleDust(handles.get(idx).entity().getLocation(), 5, 0.8);
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
            return new PerimeterSpikeWall(plugin);
        }
    }

    // =========================================================================
    // ATTACK 25 — Abyssal Whip
    // A 25-block void whip cracks in from beyond the edge, sweeps a wide arc.
    // =========================================================================
    public static class AbyssalWhip extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double sweepAngle = 0;
        private double sweepDir;
        private double groundY;
        private boolean spawned = false;
        private static final int SEGMENTS = 25;

        public AbyssalWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_whip", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(2.5);
            config.setDurationTicks(200); // ~10s
            config.setCooldownTicks(1100); // 55s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            sweepDir = RNG.nextBoolean() ? 1.0 : -1.0;
            sweepAngle = RNG.nextDouble() * 2 * Math.PI;

            // Crack warning at edge
            Location warnLoc = center.clone().add(Math.cos(sweepAngle) * 20, 1, Math.sin(sweepAngle) * 20);
            DisplayBuilder.purpleDust(warnLoc, 15, 3.0);
            DisplayBuilder.cyanDust(warnLoc, 8, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) return; // Warning

            // Build whip shape on first active tick
            if (!spawned) {
                spawned = true;
                for (int i = 0; i < SEGMENTS; i++) {
                    // Whip curves outward from the edge
                    double dist = (i + 1) * 1.05;
                    double angOffset = sweepAngle + (SEGMENTS - i) * 0.04 * sweepDir;
                    double px = center.getX() + Math.cos(angOffset) * dist;
                    double pz = center.getZ() + Math.sin(angOffset) * dist;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY + 1.0, pz),
                            i % 5 == 0 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN);
                    float scale = 0.5f + (i / (float) SEGMENTS) * 0.3f;
                    h.scale(scale, 0.6f, scale).glow(128, 0, 255).interpolation(1, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Sweep: rotate the whole whip shape
            double sweepSpeed = 0.055 * sweepDir;
            sweepAngle += sweepSpeed;

            for (int i = 0; i < handles.size(); i++) {
                if (!handles.get(i).entity().isValid()) continue;
                double dist = (i + 1) * 1.05;
                double angOffset = sweepAngle + (handles.size() - i) * 0.04 * sweepDir;
                double px = center.getX() + Math.cos(angOffset) * dist;
                double pz = center.getZ() + Math.sin(angOffset) * dist;
                handles.get(i).entity().teleport(new Location(w, px, groundY + 1.0, pz));

                // Animate whip crack
                BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                float scale = 0.5f + (i / (float) SEGMENTS) * 0.3f;
                bd.setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -0.3f, -scale / 2),
                        new AxisAngle4f((float)(sweepAngle + i * 0.1), 0, 1, 0),
                        new Vector3f(scale, 0.6f, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Damage and knock players caught by whip
            if (ticksAlive % 3 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    for (int i = 0; i < handles.size(); i++) {
                        if (!handles.get(i).entity().isValid()) continue;
                        if (ploc.distanceSquared(handles.get(i).entity().getLocation()) <= 6.25) {
                            triggerImpactDamage(ploc);
                            // Lateral knockback perpendicular to whip direction
                            double perpX = -Math.sin(sweepAngle) * sweepDir * 2.5;
                            double perpZ = Math.cos(sweepAngle) * sweepDir * 2.5;
                            player.setVelocity(new org.bukkit.util.Vector(perpX, 0.5, perpZ));
                            break;
                        }
                    }
                }
            }

            // Trailing particles
            if (ticksAlive % 4 == 0 && !handles.isEmpty()) {
                Location tip = handles.get(handles.size() - 1).entity().getLocation();
                DisplayBuilder.purpleDust(tip, 6, 1.2);
                DisplayBuilder.cyanDust(tip, 3, 0.8);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AbyssalWhip(plugin);
        }
    }

    // =========================================================================
    // ATTACK 26 — Edge Void Mist
    // Thick black mist rises from all edges, crawls inward 1 block every 2s.
    // =========================================================================
    public static class EdgeVoidMist extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double mistRadius; // current inward extent from edge
        private static final double ARENA_EDGE = 18.0;
        private double groundY;
        private boolean retreating = false;

        public EdgeVoidMist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_void_mist", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // Wither-proxy damage (0.5 hearts) per interval
            config.setDamageRadius(20.0);
            config.setTicksBetweenDamage(40); // Every 2s
            config.setDurationTicks(2000); // ~100s total
            config.setCooldownTicks(2000); // 100s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            mistRadius = 0.0;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning (0-100 ticks): mist visible rising
            if (ticksAlive < 100) {
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        DisplayBuilder.purpleDust(new Location(w,
                                center.getX() + Math.cos(angle) * ARENA_EDGE,
                                groundY + Math.random() * 2,
                                center.getZ() + Math.sin(angle) * ARENA_EDGE), 3, 0.8);
                    }
                }
                return;
            }

            // Advance mist inward every 40 ticks (2s)
            if (!retreating && ticksAlive % 40 == 0 && mistRadius < 10) {
                mistRadius += 1.0;
                // Add new ring of mist blocks at current radius
                int ringPoints = (int)(mistRadius * 8 + 20);
                for (int i = 0; i < ringPoints; i++) {
                    double angle = i / (double) ringPoints * 2 * Math.PI;
                    double dist = ARENA_EDGE - mistRadius;
                    double px = center.getX() + Math.cos(angle) * dist;
                    double pz = center.getZ() + Math.sin(angle) * dist;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY + Math.random() * 1.5, pz),
                            Material.BLACK_CONCRETE);
                    h.scale(1.2f, 1.0f + (float)(Math.random() * 0.5), 1.2f)
                            .glow(40, 0, 80).interpolation(3, 20);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.3f);
            }

            // Hold (ticks 500-1100)
            if (ticksAlive >= 500 && ticksAlive < 1100 && !retreating) {
                // Mist pulses — damage applied by base class applyRadiusDamage
                if (ticksAlive % 25 == 0) {
                    DisplayBuilder.purpleDust(center, 8, (ARENA_EDGE - mistRadius));
                }
            }

            // Retreat
            if (ticksAlive >= 1100 && !retreating) {
                retreating = true;
            }

            if (retreating && ticksAlive % 20 == 0 && !handles.isEmpty()) {
                // Remove mist blocks from the inside outward
                int removeCount = Math.min(4, handles.size());
                for (int i = 0; i < removeCount; i++) {
                    BlockDisplayHandle h = handles.remove(0);
                    if (h.entity().isValid()) {
                        DisplayBuilder.purpleDust(h.entity().getLocation(), 2, 0.5);
                        h.entity().remove();
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
            return new EdgeVoidMist(plugin);
        }
    }

    // =========================================================================
    // ATTACK 27 — Void Shark
    // Massive translucent shark of void energy breaches through the perimeter twice.
    // =========================================================================
    public static class VoidShark extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int breachCount = 0;
        private double breachAngle;
        private double groundY;
        private int nextBreachTick = 0;

        public VoidShark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shark", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(4.0);
            config.setDurationTicks(500); // ~25s
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            breachAngle = RNG.nextDouble() * 2 * Math.PI;
            nextBreachTick = 60; // First breach at 3s

            // Perimeter ripple warning
            for (int i = 0; i < 16; i++) {
                double angle = i / 16.0 * 2 * Math.PI;
                DisplayBuilder.purpleDust(new Location(w,
                        center.getX() + Math.cos(angle) * 18, groundY - 0.5,
                        center.getZ() + Math.sin(angle) * 18), 4, 0.8);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Pre-breach rumble
            if (ticksAlive == nextBreachTick - 20) {
                double bpx = center.getX() + Math.cos(breachAngle) * 16;
                double bpz = center.getZ() + Math.sin(breachAngle) * 16;
                for (int i = 0; i < 12; i++) {
                    DisplayBuilder.purpleDust(new Location(w, bpx, groundY + i * 0.2, bpz), 4, 1.0);
                }
                DisplayBuilder.playSound(new Location(w, bpx, groundY, bpz),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.5f);
            }

            // Breach event
            if (ticksAlive == nextBreachTick && breachCount < 2) {
                breachCount++;
                double bpx = center.getX() + Math.cos(breachAngle) * 16;
                double bpz = center.getZ() + Math.sin(breachAngle) * 16;
                Location breachLoc = new Location(w, bpx, groundY, bpz);

                // Remove old shark
                for (BlockDisplayHandle h : handles) {
                    if (h.entity().isValid()) h.entity().remove();
                }
                handles.clear();

                // Shark body: snout + fin + tail profile
                // Fin (tall block)
                BlockDisplayHandle fin = displayBuilder.spawnBlock(breachLoc.clone().add(0, 2, 0), Material.OBSIDIAN);
                fin.scale(0.4f, 2.5f, 1.2f).glow(80, 0, 180).interpolation(2, 5);
                handles.add(fin);
                spawnedEntities.add(fin.entity());

                // Body
                for (int seg = -2; seg <= 2; seg++) {
                    double bx = Math.cos(breachAngle + Math.PI / 2) * seg * 1.2;
                    double bz = Math.sin(breachAngle + Math.PI / 2) * seg * 1.2;
                    BlockDisplayHandle body = displayBuilder.spawnBlock(
                            breachLoc.clone().add(bx, 0.5, bz), Material.CRYING_OBSIDIAN);
                    body.scale(1.0f, 1.2f, 1.0f).glow(60, 0, 140).interpolation(2, 5);
                    handles.add(body);
                    spawnedEntities.add(body.entity());
                }

                triggerImpactDamage(breachLoc);
                // Knockback inward
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(breachLoc) <= 16) {
                        org.bukkit.util.Vector inward = center.toVector().subtract(player.getLocation().toVector())
                                .normalize().multiply(2.5).add(new org.bukkit.util.Vector(0, 0.4, 0));
                        player.setVelocity(inward);
                    }
                }

                DisplayBuilder.purpleDust(breachLoc, 25, 4.0);
                DisplayBuilder.cyanDust(breachLoc, 12, 3.0);
                DisplayBuilder.playSound(breachLoc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.4f);

                // Schedule second breach
                if (breachCount == 1) {
                    breachAngle = RNG.nextDouble() * 2 * Math.PI;
                    nextBreachTick = ticksAlive + 160; // 8 seconds later
                }
            }

            // Submerging animation: shark sinks after each breach
            if (ticksAlive > nextBreachTick - 160 + 30 && !handles.isEmpty()) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(
                            Math.cos(breachAngle) * 16, groundY, Math.sin(breachAngle) * 16), 4, 1.5);
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
            return new VoidShark(plugin);
        }
    }

    // =========================================================================
    // ATTACK 28 — Gravity Inversion Zone
    // A 10-block perimeter arc shimmers; players entering float up 15 blocks.
    // =========================================================================
    public static class GravityInversionZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double zoneAngle;
        private double groundY;
        private boolean zoneActive = false;

        public GravityInversionZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_zone", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // Fall damage ~4-6 hearts
            config.setImpactRadius(5.0);
            config.setDurationTicks(700); // ~35s
            config.setCooldownTicks(1400); // 70s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            zoneAngle = RNG.nextDouble() * 2 * Math.PI;

            // Ascending particle shimmer warning
            for (int i = -5; i <= 5; i++) {
                double arcAngle = zoneAngle + i * 0.1;
                double px = center.getX() + Math.cos(arcAngle) * 16;
                double pz = center.getZ() + Math.sin(arcAngle) * 16;
                DisplayBuilder.cyanDust(new Location(w, px, groundY + 0.5, pz), 3, 0.6);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 60) {
                // Shimmer ascending
                if (ticksAlive % 5 == 0) {
                    for (int i = -5; i <= 5; i++) {
                        double arcAngle = zoneAngle + i * 0.1;
                        double px = center.getX() + Math.cos(arcAngle) * 16;
                        double pz = center.getZ() + Math.sin(arcAngle) * 16;
                        double py = groundY + (ticksAlive / 60.0) * 3;
                        DisplayBuilder.cyanDust(new Location(w, px, py, pz), 2, 0.5);
                    }
                }
                return;
            }

            if (!zoneActive) {
                zoneActive = true;
                // Spawn zone tiles (inverted look: upward particles)
                for (int i = -5; i <= 5; i++) {
                    double arcAngle = zoneAngle + i * 0.1;
                    double px = center.getX() + Math.cos(arcAngle) * 16;
                    double pz = center.getZ() + Math.sin(arcAngle) * 16;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY + 0.1, pz), Material.CYAN_CONCRETE);
                    h.scale(1.0f, 0.08f, 1.0f).glow(0, 200, 255).interpolation(2, 5);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Zone effects: launch players who step on inversion tiles
            if (ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    for (BlockDisplayHandle h : handles) {
                        if (!h.entity().isValid()) continue;
                        if (ploc.distanceSquared(h.entity().getLocation()) <= 4) {
                            // Launch upward
                            if (player.getVelocity().getY() <= 0.5) {
                                player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 1.2, 0)));
                            }
                            // If player floats out over void, teleport to center
                            if (ploc.distanceSquared(center) > 400) {
                                player.teleport(center.clone().add(0, 0.5, 0));
                                triggerImpactDamage(ploc);
                            }
                            break;
                        }
                    }
                }
            }

            // Ascending particle stream from tiles
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    Location tileLoc = h.entity().getLocation();
                    DisplayBuilder.cyanDust(tileLoc.clone().add(0, Math.random() * 4, 0), 2, 0.4);
                }
            }

            // Near cleanup: zone deactivates
            if (ticksAlive > 600) {
                if (ticksAlive % 5 == 0 && !handles.isEmpty()) {
                    BlockDisplayHandle last = handles.remove(handles.size() - 1);
                    if (last.entity().isValid()) {
                        DisplayBuilder.cyanDust(last.entity().getLocation(), 3, 0.5);
                        last.entity().remove();
                    }
                }
                // On deactivation: trigger fall damage for floating players
                if (ticksAlive == 605) {
                    triggerImpactDamage(center.clone().add(Math.cos(zoneAngle) * 16, 0, Math.sin(zoneAngle) * 16));
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
            return new GravityInversionZone(plugin);
        }
    }

    // =========================================================================
    // ATTACK 29 — The Beckoning
    // An intensely wrong light beyond the edge pulls players toward it.
    // =========================================================================
    public static class TheBeckoning extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double beckAngle;
        private double groundY;
        private boolean active = false;

        public TheBeckoning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_beckoning", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts void-catch
            config.setImpactRadius(3.0);
            config.setDurationTicks(600); // ~30s
            config.setCooldownTicks(1700); // 85s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();
            beckAngle = RNG.nextDouble() * 2 * Math.PI;
            // Peripheral light starts subtle
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Build up glow over 5s (100 ticks)
            if (ticksAlive < 100) {
                double intensity = ticksAlive / 100.0;
                if (ticksAlive % 5 == 0) {
                    double dist = 22 + Math.random() * 4;
                    for (int i = 0; i < 5; i++) {
                        double spread = (RNG.nextDouble() - 0.5) * 0.3;
                        double px = center.getX() + Math.cos(beckAngle + spread) * dist;
                        double pz = center.getZ() + Math.sin(beckAngle + spread) * dist;
                        // Void-white light proxy
                        DisplayBuilder.cyanDust(new Location(w, px, groundY + 1 + i * 0.5, pz),
                                (int)(3 + intensity * 7), (float)(0.3 + intensity * 1.2));
                    }
                }
                return;
            }

            // Active: spawn beckoning glow structure
            if (!active) {
                active = true;
                double px = center.getX() + Math.cos(beckAngle) * 24;
                double pz = center.getZ() + Math.sin(beckAngle) * 24;
                // Pulsing glow: concentric glowing blocks
                for (int layer = 0; layer < 3; layer++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY + 2 + layer, pz), Material.END_STONE);
                    float s = 1.0f + layer * 0.6f;
                    h.scale(s, s, s).glow(200, 220, 255).interpolation(2, 15);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.2f, 0.6f);
            }

            // Pull players facing the beckoning direction
            if (ticksAlive % 3 == 0) {
                Location beckLoc = new Location(w,
                        center.getX() + Math.cos(beckAngle) * 24, groundY,
                        center.getZ() + Math.sin(beckAngle) * 24);

                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    // Check if player is within pull range
                    if (ploc.distanceSquared(center) > 400) continue; // outside arena

                    // Vector toward beckoning
                    org.bukkit.util.Vector towardBeck = beckLoc.toVector()
                            .subtract(ploc.toVector()).normalize();
                    // Pull strength proportional to closeness to facing direction
                    org.bukkit.util.Vector facing = player.getLocation().getDirection();
                    double dot = facing.dot(towardBeck);
                    if (dot > 0.3) {
                        double pullStrength = 0.08 * dot;
                        player.setVelocity(player.getVelocity().add(
                                towardBeck.multiply(pullStrength)));
                    }

                    // Void-catch: player pulled over edge
                    if (ploc.distance(center) > 19) {
                        player.teleport(center.clone().add(0, 0.5, 0));
                        triggerImpactDamage(ploc);
                    }
                }
            }

            // Animate beckoning glow pulse
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < handles.size(); i++) {
                    if (!handles.get(i).entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float pulse = 1.0f + i * 0.6f + (float) Math.sin(ticksAlive * 0.15 + i) * 0.15f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                double px = center.getX() + Math.cos(beckAngle) * 24;
                double pz = center.getZ() + Math.sin(beckAngle) * 24;
                DisplayBuilder.cyanDust(new Location(w, px, groundY + 2, pz), 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheBeckoning(plugin);
        }
    }

    // =========================================================================
    // ATTACK 30 — Edge Shatter
    // The outer 3-block perimeter ring shatters and briefly becomes passthrough.
    // =========================================================================
    public static class EdgeShatter extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean shattered = false;
        private boolean reassembled = false;
        private double groundY;

        public EdgeShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_shatter", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts void-catch + 2 hearts debris
            config.setImpactRadius(3.0);
            config.setDurationTicks(500); // ~25s
            config.setCooldownTicks(1800); // 90s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();

            // Build the perimeter ring as display blocks
            int ringSegments = 64;
            for (int i = 0; i < ringSegments; i++) {
                double angle = i / (double) ringSegments * 2 * Math.PI;
                // Outer ring (r=16-19) — 3-block-wide band
                for (int r = 16; r <= 18; r++) {
                    double px = center.getX() + Math.cos(angle) * r;
                    double pz = center.getZ() + Math.sin(angle) * r;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, px, groundY, pz), Material.END_STONE);
                    h.scale(1.0f, 0.5f, 1.0f).glow(80, 60, 150).interpolation(2, 10);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Tectonic warning
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive < 40) {
                // Edge glow warning
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = i / 20.0 * 2 * Math.PI;
                        DisplayBuilder.crimsonDust(new Location(w,
                                center.getX() + Math.cos(angle) * 17, groundY + 0.4,
                                center.getZ() + Math.sin(angle) * 17), 3, 0.6);
                    }
                }
                return;
            }

            // Shatter: fling blocks outward + upward
            if (!shattered) {
                shattered = true;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    Location loc = bd.getLocation();
                    double outAngle = Math.atan2(loc.getZ() - center.getZ(), loc.getX() - center.getX());
                    float outX = (float) Math.cos(outAngle) * 3;
                    float outZ = (float) Math.sin(outAngle) * 3;
                    bd.setTransformation(new Transformation(
                            new Vector3f(outX, 1.5f, outZ),
                            new AxisAngle4f((float)(Math.random() * 2 * Math.PI), 1, 1, 0),
                            new Vector3f(0.8f, 0.4f, 0.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }

                triggerImpactDamage(center);
                // Players on perimeter: void catch
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist >= 15 && dist <= 20) {
                        player.damage(6.0);
                        player.teleport(center.clone().add(0, 0.5, 0));
                    }
                }

                DisplayBuilder.purpleDust(center, 40, 20.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 1.5f);
            }

            // Floating debris particles during shatter phase (5 ticks)
            if (ticksAlive >= 40 && ticksAlive < 50) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 17.0);
                }
            }

            // Reassemble: snap back
            if (!reassembled && ticksAlive == 55) {
                reassembled = true;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.0f, 0.5f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(8);
                }
                // Reassembly debris hits
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist >= 15 && dist <= 20) {
                        player.damage(4.0); // 2 hearts from falling debris
                    }
                }
                DisplayBuilder.purpleDust(center, 25, 19.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EdgeShatter(plugin);
        }
    }
}
