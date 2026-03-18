package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Block Display -- GROUP 8: NETHER STAR ARRAYS (Structures #71-80)
 * Void Emperor (Boss 4) arena: geometric NETHER_STAR formations floating
 * at mid-height. Hexagram, pentagram, connector beams, shockwave pulses,
 * pedestals. 60-tick heartbeat system, Phase 3 detach/pursuit.
 *
 * Palette: magenta/cyan/crimson/purple per Calamity color spec.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 */
public final class NetherStarArrays {

    private NetherStarArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TriuneTriangleOuter(plugin));
        registry.register(new TriuneTriangleInner(plugin));
        registry.register(new HexagonalArray(plugin));
        registry.register(new PentagramFormation(plugin));
        registry.register(new NestedPentagonInnermost(plugin));
        registry.register(new StarConnectorBeams(plugin));
        registry.register(new StarPulseShockwave(plugin));
        registry.register(new StarPursuitTracking(plugin));
        registry.register(new StarArrayDeathBurst(plugin));
        registry.register(new FloatingStarPedestalRing(plugin));
    }

    // ================================================================
    // #71 -- TRIUNE TRIANGLE (OUTER): 3 NETHER_STARs at equilateral
    //        triangle vertices, radius 14, Y+12. Formation rotates
    //        0.35 deg/tick. 60-tick heartbeat pulse.
    // ================================================================
    public static class TriuneTriangleOuter extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> stars = new ArrayList<>();

        public TriuneTriangleOuter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("triune_triangle_outer", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 vertices at 0, 120, 240 deg, radius 14, Y+12
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120.0);
                double x = Math.cos(angle) * 14.0;
                double z = Math.sin(angle) * 14.0;
                Location starPos = center.clone().add(x, 12, z);

                ItemDisplayHandle star = displayBuilder.spawnItem(starPos, new ItemStack(Material.NETHER_STAR));
                star.scale(1.8f, 1.8f, 1.8f).glow(255, 255, 200).interpolation(3, 0);
                stars.add(star);
                spawnedEntities.add(star.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0), Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Formation rotation: 0.35 deg/tick
            double formRot = Math.toRadians(ticksAlive * 0.35);

            // Heartbeat: scale = 1.8 + 0.6 * sin(tick * 0.105)
            float scalePulse = 1.8f + 0.6f * (float) Math.sin(ticksAlive * 0.105);
            boolean isPulseApex = Math.abs(Math.sin(ticksAlive * 0.105) - 1.0) < 0.05;

            for (int i = 0; i < stars.size(); i++) {
                double baseAngle = Math.toRadians(i * 120.0) + formRot;
                double x = Math.cos(baseAngle) * 14.0;
                double z = Math.sin(baseAngle) * 14.0;
                Location starPos = center.clone().add(x, 12, z);
                stars.get(i).entity().teleport(starPos);

                // Self-rotation: 2.0 deg/tick
                float selfRot = (float) Math.toRadians(ticksAlive * 2.0);
                stars.get(i).rotate(selfRot, 0, 1, 0);
                stars.get(i).scale(scalePulse, scalePulse, scalePulse);
                stars.get(i).interpolation(3, 0);

                // Continuous particles
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 2, 0.8, 0.8, 0.8, 0);
                w.spawnParticle(Particle.CRIT, starPos, 1, 1.0, 1.0, 1.0, 0);

                // Pulse apex burst
                if (isPulseApex && ticksAlive % 60 < 2) {
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 8, 1.2, 1.2, 1.2, 0);
                    w.spawnParticle(Particle.CRIT, starPos, 5, 1.5, 1.5, 1.5, 0);
                }
            }

            // Sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 12, 0),
                        Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TriuneTriangleOuter(plugin); }
    }

    // ================================================================
    // #72 -- TRIUNE TRIANGLE (INNER): 3 stars, radius 7, Y+14.
    //        Counter-rotates at -0.35 deg/tick. Heartbeat offset 30 ticks
    //        from outer triangle (alternating pulse).
    // ================================================================
    public static class TriuneTriangleInner extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> stars = new ArrayList<>();

        public TriuneTriangleInner(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("triune_triangle_inner", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rotated 60 deg from outer triangle (between outer vertices)
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120.0 + 60.0);
                double x = Math.cos(angle) * 7.0;
                double z = Math.sin(angle) * 7.0;
                Location starPos = center.clone().add(x, 14, z);

                ItemDisplayHandle star = displayBuilder.spawnItem(starPos, new ItemStack(Material.NETHER_STAR));
                star.scale(1.4f, 1.4f, 1.4f).glow(255, 255, 200).interpolation(3, 0);
                stars.add(star);
                spawnedEntities.add(star.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 14, 0), Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Counter-rotation: -0.35 deg/tick
            double formRot = Math.toRadians(ticksAlive * -0.35);

            // Phase-shifted heartbeat: offset by 30 ticks (PI)
            float scalePulse = 1.4f + 0.5f * (float) Math.sin(ticksAlive * 0.105 + Math.PI);

            for (int i = 0; i < stars.size(); i++) {
                double baseAngle = Math.toRadians(i * 120.0 + 60.0) + formRot;
                double x = Math.cos(baseAngle) * 7.0;
                double z = Math.sin(baseAngle) * 7.0;
                Location starPos = center.clone().add(x, 14, z);
                stars.get(i).entity().teleport(starPos);

                float selfRot = (float) Math.toRadians(ticksAlive * 2.0);
                stars.get(i).rotate(selfRot, 0, 1, 0);
                stars.get(i).scale(scalePulse, scalePulse, scalePulse);
                stars.get(i).interpolation(3, 0);

                w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 2, 0.6, 0.6, 0.6, 0);

                // Off-beat pulse burst
                boolean isPulseApex = Math.abs(Math.sin(ticksAlive * 0.105 + Math.PI) - 1.0) < 0.05;
                if (isPulseApex && ticksAlive % 60 < 2) {
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 8, 1.0, 1.0, 1.0, 0);
                    w.spawnParticle(Particle.CRIT, starPos, 5, 1.2, 1.2, 1.2, 0);
                }
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 14, 0),
                        Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TriuneTriangleInner(plugin); }
    }

    // ================================================================
    // #73 -- HEXAGONAL ARRAY: 7 stars (6 hex vertices + 1 center).
    //        Radius 10, Y+16. Formation rotates 0.22 deg/tick.
    //        Central star larger (2.0 scale), vertices ripple inward/outward.
    // ================================================================
    public static class HexagonalArray extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> vertexStars = new ArrayList<>();
        private ItemDisplayHandle centerStar;

        public HexagonalArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hexagonal_array", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arrayCenter = center.clone().add(0, 16, 0);

            // Central star (larger)
            centerStar = displayBuilder.spawnItem(arrayCenter, new ItemStack(Material.NETHER_STAR));
            centerStar.scale(2.0f, 2.0f, 2.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(centerStar.entity());

            // 6 vertex stars at 60-deg intervals
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                double x = Math.cos(angle) * 10.0;
                double z = Math.sin(angle) * 10.0;
                Location starPos = arrayCenter.clone().add(x, 0, z);

                ItemDisplayHandle star = displayBuilder.spawnItem(starPos, new ItemStack(Material.NETHER_STAR));
                star.scale(1.2f, 1.2f, 1.2f).glow(255, 255, 200).interpolation(3, 0);
                vertexStars.add(star);
                spawnedEntities.add(star.entity());
            }

            DisplayBuilder.playSound(arrayCenter, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location arrayCenter = center.clone().add(0, 16, 0);
            double formRot = Math.toRadians(ticksAlive * 0.22);

            // Central star pulse: 2.0 + 0.8 * sin
            float centerScale = 2.0f + 0.8f * (float) Math.sin(ticksAlive * 0.105);
            if (centerStar != null) {
                centerStar.scale(centerScale, centerScale, centerScale);
                centerStar.rotate((float) Math.toRadians(ticksAlive * 1.5), 0, 1, 0);
                centerStar.interpolation(3, 0);

                w.spawnParticle(Particle.TOTEM_OF_UNDYING, arrayCenter, 3, 1.0, 1.0, 1.0, 0);
            }

            // Vertex stars: breathe in/out radially
            for (int i = 0; i < vertexStars.size(); i++) {
                double baseAngle = Math.toRadians(i * 60.0) + formRot;
                float radiusOffset = 10.0f + 0.8f * (float) Math.sin(ticksAlive * 0.105 + i * 1.047);
                double x = Math.cos(baseAngle) * radiusOffset;
                double z = Math.sin(baseAngle) * radiusOffset;
                Location starPos = arrayCenter.clone().add(x, 0, z);
                vertexStars.get(i).entity().teleport(starPos);
                vertexStars.get(i).scale(1.2f, 1.2f, 1.2f);
                vertexStars.get(i).interpolation(3, 0);
            }

            // Pulse apex: TOTEM wave outward from center
            boolean isPulseApex = Math.abs(Math.sin(ticksAlive * 0.105) - 1.0) < 0.05;
            if (isPulseApex && ticksAlive % 60 < 2) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60.0) + formRot;
                    for (int p = 1; p <= 10; p++) {
                        double px = Math.cos(angle) * p;
                        double pz = Math.sin(angle) * p;
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, arrayCenter.clone().add(px, 0, pz), 1, 0, 0, 0, 0);
                    }
                }
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(arrayCenter, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HexagonalArray(plugin); }
    }

    // ================================================================
    // #74 -- PENTAGRAM FORMATION: 10 stars (5 pentagon + 5 inner
    //        pentagram points). Fast spinning, counter-rotation on inner.
    //        72-tick pulse cycle.
    // ================================================================
    public static class PentagramFormation extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> outerStars = new ArrayList<>();
        private final List<ItemDisplayHandle> innerStars = new ArrayList<>();

        public PentagramFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pentagram_formation", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location pentCenter = center.clone().add(0, 10, 0);

            // 5 outer pentagon vertices at 72-deg intervals, radius 9
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(i * 72.0);
                double x = Math.cos(angle) * 9.0;
                double z = Math.sin(angle) * 9.0;
                ItemDisplayHandle star = displayBuilder.spawnItem(
                        pentCenter.clone().add(x, 0, z), new ItemStack(Material.NETHER_STAR));
                star.scale(1.5f, 1.5f, 1.5f).glow(255, 255, 200).interpolation(3, 0);
                outerStars.add(star);
                spawnedEntities.add(star.entity());
            }

            // 5 inner pentagram star points at 36-deg offset, radius 5.5
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(i * 72.0 + 36.0);
                double x = Math.cos(angle) * 5.5;
                double z = Math.sin(angle) * 5.5;
                ItemDisplayHandle star = displayBuilder.spawnItem(
                        pentCenter.clone().add(x, 0, z), new ItemStack(Material.NETHER_STAR));
                star.scale(1.0f, 1.0f, 1.0f).glow(255, 255, 200).interpolation(3, 0);
                innerStars.add(star);
                spawnedEntities.add(star.entity());
            }

            DisplayBuilder.playSound(pentCenter, Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location pentCenter = center.clone().add(0, 10, 0);

            // Outer formation: 0.50 deg/tick
            double outerRot = Math.toRadians(ticksAlive * 0.50);
            // 72-tick pulse: 1.5 + 0.5 * sin(tick * 0.087)
            float outerPulse = 1.5f + 0.5f * (float) Math.sin(ticksAlive * 0.087);

            for (int i = 0; i < outerStars.size(); i++) {
                double baseAngle = Math.toRadians(i * 72.0) + outerRot;
                double x = Math.cos(baseAngle) * 9.0;
                double z = Math.sin(baseAngle) * 9.0;
                Location starPos = pentCenter.clone().add(x, 0, z);
                outerStars.get(i).entity().teleport(starPos);
                outerStars.get(i).scale(outerPulse, outerPulse, outerPulse);
                outerStars.get(i).interpolation(3, 0);
            }

            // Inner: counter-rotation at -1.0 deg/tick (2x speed)
            double innerRot = Math.toRadians(ticksAlive * -1.0);
            float innerPulse = 1.0f + 0.35f * (float) Math.sin(ticksAlive * 0.087 + 0.628);

            for (int i = 0; i < innerStars.size(); i++) {
                double baseAngle = Math.toRadians(i * 72.0 + 36.0) + innerRot;
                double x = Math.cos(baseAngle) * 5.5;
                double z = Math.sin(baseAngle) * 5.5;
                Location starPos = pentCenter.clone().add(x, 0, z);
                innerStars.get(i).entity().teleport(starPos);
                innerStars.get(i).scale(innerPulse, innerPulse, innerPulse);
                innerStars.get(i).interpolation(3, 0);
            }

            // 72-tick pulse burst
            if (ticksAlive % 72 < 2) {
                for (ItemDisplayHandle star : outerStars) {
                    Location loc = star.entity().getLocation();
                    w.spawnParticle(Particle.CRIT, loc, 10, 1.0, 1.0, 1.0, 0);
                }
                for (ItemDisplayHandle star : innerStars) {
                    Location loc = star.entity().getLocation();
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 6, 0.8, 0.8, 0.8, 0);
                }
            }

            if (ticksAlive % 72 == 0) {
                DisplayBuilder.playSound(pentCenter, Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PentagramFormation(plugin); }
    }

    // ================================================================
    // #75 -- NESTED PENTAGON INNERMOST: 5 smallest stars, radius 4,
    //        Y+8. Fastest formation at 1.0 deg/tick. Phase 3: accelerates
    //        to 4.0 deg/tick and ascends to Y+40.
    // ================================================================
    public static class NestedPentagonInnermost extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> stars = new ArrayList<>();

        public NestedPentagonInnermost(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nested_pentagon_innermost", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location pentCenter = center.clone().add(0, 8, 0);

            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(i * 72.0);
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                ItemDisplayHandle star = displayBuilder.spawnItem(
                        pentCenter.clone().add(x, 0, z), new ItemStack(Material.NETHER_STAR));
                star.scale(0.8f, 0.8f, 0.8f).glow(255, 255, 200).interpolation(3, 0);
                stars.add(star);
                spawnedEntities.add(star.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location pentCenter = center.clone().add(0, 8, 0);

            // Fastest formation: 1.0 deg/tick
            double formRot = Math.toRadians(ticksAlive * 1.0);
            float scalePulse = 0.8f + 0.3f * (float) Math.sin(ticksAlive * 0.105);

            for (int i = 0; i < stars.size(); i++) {
                double baseAngle = Math.toRadians(i * 72.0) + formRot;
                double x = Math.cos(baseAngle) * 4.0;
                double z = Math.sin(baseAngle) * 4.0;
                Location starPos = pentCenter.clone().add(x, 0, z);
                stars.get(i).entity().teleport(starPos);
                stars.get(i).scale(scalePulse, scalePulse, scalePulse);
                stars.get(i).interpolation(3, 0);

                // Continuous TOTEM
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 1, 0.3, 0.3, 0.3, 0);
            }

            // Pulse apex burst
            if (ticksAlive % 60 < 2) {
                for (ItemDisplayHandle star : stars) {
                    w.spawnParticle(Particle.CRIT, star.entity().getLocation(), 3, 0.5, 0.5, 0.5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NestedPentagonInnermost(plugin); }
    }

    // ================================================================
    // #76 -- STAR CONNECTOR BEAMS: Particle emitters that create
    //        visible connections between formations when vertices
    //        align within 5 blocks. Dynamic web of END_ROD streams.
    // ================================================================
    public static class StarConnectorBeams extends BlockDisplayAttack {

        public StarConnectorBeams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_connector_beams", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle system -- no blocks
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Simulate dynamic beam connections between nearby star positions
            // Use outer triangle (radius 14, Y+12) and hex array (radius 10, Y+16) as example
            double outerFormRot = Math.toRadians(ticksAlive * 0.35);
            double hexFormRot = Math.toRadians(ticksAlive * 0.22);

            int beamsDrawn = 0;
            for (int t = 0; t < 3 && beamsDrawn < 6; t++) {
                double tAngle = Math.toRadians(t * 120.0) + outerFormRot;
                double tx = Math.cos(tAngle) * 14.0;
                double tz = Math.sin(tAngle) * 14.0;
                Location triPos = center.clone().add(tx, 12, tz);

                for (int h = 0; h < 6 && beamsDrawn < 6; h++) {
                    double hAngle = Math.toRadians(h * 60.0) + hexFormRot;
                    double hx = Math.cos(hAngle) * 10.0;
                    double hz = Math.sin(hAngle) * 10.0;
                    Location hexPos = center.clone().add(hx, 16, hz);

                    double dist = triPos.distance(hexPos);
                    if (dist < 8.0) {
                        // Draw END_ROD beam between the two
                        DisplayBuilder.particleLine(triPos, hexPos, Particle.END_ROD, 3, null);
                        beamsDrawn++;
                    }
                }
            }

            // Sound on beam form/break (rate-limited)
            if (beamsDrawn > 0 && ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 14, 0),
                        Sound.BLOCK_GLASS_BREAK, 0.06f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarConnectorBeams(plugin); }
    }

    // ================================================================
    // #77 -- STAR PULSE SHOCKWAVE: Radial TOTEM ring expands from
    //        arena center every 60 ticks. 36 particles per ring,
    //        three stacked rings at Y+12, Y+14, Y+16.
    // ================================================================
    public static class StarPulseShockwave extends BlockDisplayAttack {

        public StarPulseShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_pulse_shockwave", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only shockwave system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shockwave fires every 60 ticks
            int cycleTick = ticksAlive % 60;
            if (cycleTick >= 0 && cycleTick < 20) {
                // Ring 1 at Y+12 (fires at cycleTick 0)
                if (cycleTick < 20) {
                    double radius = cycleTick * 0.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 12, 0),
                            radius, Particle.TOTEM_OF_UNDYING, 36, null);
                }
                // Ring 2 at Y+14 (fires at cycleTick 5)
                if (cycleTick >= 5 && cycleTick < 20) {
                    double radius = (cycleTick - 5) * 0.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 14, 0),
                            radius, Particle.TOTEM_OF_UNDYING, 36, null);
                }
                // Ring 3 at Y+16 (fires at cycleTick 10)
                if (cycleTick >= 10 && cycleTick < 20) {
                    double radius = (cycleTick - 10) * 0.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 16, 0),
                            radius, Particle.TOTEM_OF_UNDYING, 36, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarPulseShockwave(plugin); }
    }

    // ================================================================
    // #78 -- STAR PURSUIT TRACKING: Phase 3 behavior modification.
    //        Detached stars pursue nearest player at 0.025-0.032 b/tick.
    //        END_ROD trail while moving. Hit at 1.5 blocks triggers
    //        impact damage, then star returns to formation.
    // ================================================================
    public static class StarPursuitTracking extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> pursuitStars = new ArrayList<>();

        public StarPursuitTracking(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_pursuit_tracking", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 6 pursuit stars at initial positions around center at Y+12
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                double x = Math.cos(angle) * 14.0;
                double z = Math.sin(angle) * 14.0;
                Location starPos = center.clone().add(x, 12, z);

                ItemDisplayHandle star = displayBuilder.spawnItem(starPos, new ItemStack(Material.NETHER_STAR));
                star.scale(1.6f, 1.6f, 1.6f).glow(255, 255, 200).interpolation(3, 0);
                pursuitStars.add(star);
                spawnedEntities.add(star.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0), Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each star pursues nearest player within 30 blocks
            for (ItemDisplayHandle star : pursuitStars) {
                Location starLoc = star.entity().getLocation();
                Player nearest = null;
                double nearestDist = 30.0;

                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(starLoc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = player;
                    }
                }

                if (nearest != null) {
                    // Move toward player at 0.028 blocks/tick
                    Location target = nearest.getLocation().clone().add(0, 1, 0);
                    double dx = target.getX() - starLoc.getX();
                    double dy = target.getY() - starLoc.getY();
                    double dz = target.getZ() - starLoc.getZ();
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > 0.1) {
                        double speed = 0.028;
                        Location newLoc = starLoc.clone().add(
                                dx / dist * speed, dy / dist * speed, dz / dist * speed);
                        star.entity().teleport(newLoc);
                    }

                    // END_ROD trail behind movement
                    w.spawnParticle(Particle.END_ROD, starLoc, 3, 0.3, 0.3, 0.3, 0);

                    // Impact check at 1.5 blocks
                    if (nearestDist < 1.5) {
                        triggerImpactDamage(starLoc);
                    }
                } else {
                    // Drift toward arena center slowly
                    double dx = center.getX() - starLoc.getX();
                    double dz = center.getZ() - starLoc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.5) {
                        star.entity().teleport(starLoc.clone().add(dx / dist * 0.01, 0, dz / dist * 0.01));
                    }
                }

                // Maintain rotation and pulse
                float rot = (float) Math.toRadians(ticksAlive * 2.0);
                float pulse = 1.6f + 0.4f * (float) Math.sin(ticksAlive * 0.105);
                star.rotate(rot, 0, 1, 0);
                star.scale(pulse, pulse, pulse);
                star.interpolation(3, 0);
            }

            // Pursuit sound with rising pitch based on proximity
            if (ticksAlive % 8 == 0) {
                for (ItemDisplayHandle star : pursuitStars) {
                    DisplayBuilder.playSound(star.entity().getLocation(),
                            Sound.ENTITY_ENDERMITE_AMBIENT, 0.2f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarPursuitTracking(plugin); }
    }

    // ================================================================
    // #79 -- STAR ARRAY DEATH BURST: Massive one-shot particle
    //        explosion from all star positions on dragon death.
    //        TOTEM x50, CRIT x30, END_ROD x20 per star.
    // ================================================================
    public static class StarArrayDeathBurst extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> deathStars = new ArrayList<>();

        public StarArrayDeathBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_array_death_burst", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(14.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(6000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn representative stars at key formation positions
            double[][] positions = {
                {0, 12, 14}, {10.5, 12, -7}, {-10.5, 12, -7},     // outer triangle
                {3.5, 14, 6}, {-3.5, 14, 6}, {0, 14, -7},          // inner triangle
                {0, 16, 0},                                          // hex center
            };

            for (double[] pos : positions) {
                Location starPos = center.clone().add(pos[0], pos[1], pos[2]);
                ItemDisplayHandle star = displayBuilder.spawnItem(starPos, new ItemStack(Material.NETHER_STAR));
                star.scale(1.5f, 1.5f, 1.5f).glow(255, 255, 200).interpolation(3, 0);
                deathStars.add(star);
                spawnedEntities.add(star.entity());
            }

            // Immediate massive burst
            for (ItemDisplayHandle star : deathStars) {
                Location loc = star.entity().getLocation();
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 3.0, 3.0, 3.0, 0.3);
                w.spawnParticle(Particle.CRIT, loc, 30, 4.0, 4.0, 4.0, 0.2);
                w.spawnParticle(Particle.END_ROD, loc, 20, 5.0, 5.0, 5.0, 0.4);
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0),
                    Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Fade stars to invisible over 5 ticks after burst
            if (ticksAlive <= 5) {
                float fadeScale = Math.max(0.01f, 1.5f * (1.0f - ticksAlive / 5.0f));
                for (ItemDisplayHandle star : deathStars) {
                    star.scale(fadeScale, fadeScale, fadeScale);
                    star.interpolation(5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarArrayDeathBurst(plugin); }
    }

    // ================================================================
    // #80 -- FLOATING STAR PEDESTAL RING: 8 small pedestals at
    //        radius 18, Y+6. Decorative anchoring for star formations.
    //        gilded_blackstone base + amethyst_block column + NETHER_STAR.
    // ================================================================
    public static class FloatingStarPedestalRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pedestalBlocks = new ArrayList<>();
        private final List<ItemDisplayHandle> pedestalStars = new ArrayList<>();

        public FloatingStarPedestalRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_star_pedestal_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 18.0;
                double z = Math.sin(angle) * 18.0;
                Location pedBase = center.clone().add(x, 6, z);

                // Base: gilded_blackstone
                BlockDisplayHandle base = displayBuilder.spawnBlock(pedBase, Material.GILDED_BLACKSTONE);
                base.scale(1.2f, 0.6f, 1.2f).glow(200, 150, 0).interpolation(3, 0);
                pedestalBlocks.add(base);
                spawnedEntities.add(base.entity());

                // Column: amethyst_block
                BlockDisplayHandle column = displayBuilder.spawnBlock(pedBase.clone().add(0, 0.6, 0), Material.AMETHYST_BLOCK);
                column.scale(0.6f, 1.8f, 0.6f).glow(180, 0, 255).interpolation(3, 0);
                pedestalBlocks.add(column);
                spawnedEntities.add(column.entity());

                // Top platform: gilded_blackstone
                BlockDisplayHandle platform = displayBuilder.spawnBlock(pedBase.clone().add(0, 2.4, 0), Material.GILDED_BLACKSTONE);
                platform.scale(1.0f, 0.3f, 1.0f).glow(200, 150, 0).interpolation(3, 0);
                pedestalBlocks.add(platform);
                spawnedEntities.add(platform.entity());

                // Mounted NETHER_STAR
                ItemDisplayHandle star = displayBuilder.spawnItem(
                        pedBase.clone().add(0, 2.8, 0), new ItemStack(Material.NETHER_STAR));
                star.scale(0.6f, 0.6f, 0.6f).glow(255, 255, 200).interpolation(3, 0);
                pedestalStars.add(star);
                spawnedEntities.add(star.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Mounted star rotation 1.5 deg/tick + heartbeat sync
            float rot = (float) Math.toRadians(ticksAlive * 1.5);
            float scaleOsc = 0.6f + 0.1f * (float) Math.sin(ticksAlive * 0.105);

            for (ItemDisplayHandle star : pedestalStars) {
                star.rotate(rot, 0, 1, 0);
                star.scale(scaleOsc, scaleOsc, scaleOsc);
                star.interpolation(3, 0);

                // Subtle CRIT sparkle
                if (ticksAlive % 5 == 0) {
                    w.spawnParticle(Particle.CRIT, star.entity().getLocation(), 1, 0.5, 0.5, 0.5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingStarPedestalRing(plugin); }
    }
}
