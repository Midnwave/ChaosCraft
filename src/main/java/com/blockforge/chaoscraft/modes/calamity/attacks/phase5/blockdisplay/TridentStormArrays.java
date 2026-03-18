package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Block Display -- GROUP 6: TRIDENT STORM ARRAYS (Structures #51-62)
 * Supreme Calamitas (Boss 5) arena: ItemDisplay TRIDENT arrays forming a
 * comet tail, sentinel perimeter, vertical column, and convergence formation.
 *
 * Palette: crimson(200,10,10), violet(180,40,255), deep violet(140,0,200),
 *          soul blue(120,120,255), white/bright accents.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * triggerImpactDamage() for proximity hits. spawnedEntities.add() always.
 */
public final class TridentStormArrays {

    private TridentStormArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CometTailTrail(plugin));
        registry.register(new CometHeadFlare(plugin));
        registry.register(new OuterPerimeterTridentRing(plugin));
        registry.register(new InnerPerimeterTridentRing(plugin));
        registry.register(new CentralVerticalTridentColumn(plugin));
        registry.register(new NorthSentinelTrident(plugin));
        registry.register(new SouthSentinelTrident(plugin));
        registry.register(new EastSentinelTrident(plugin));
        registry.register(new WestSentinelTrident(plugin));
        registry.register(new NortheastDiagonalSentinel(plugin));
        registry.register(new DiagonalSentinelTriad(plugin));
        registry.register(new SentinelConvergenceFormation(plugin));
    }

    // ================================================================
    // #51 -- COMET TAIL (MOTION TRAIL)
    // 30 TRIDENT ItemDisplays forming a trailing ribbon behind Calamitas.
    // Ring buffer position history updated every 2 ticks. Scale and
    // brightness decrease linearly from head (index 0) to tail (index 29).
    // Y-axis self-rotation at 3.6 deg/tick with 12-deg phase offset per index.
    // ================================================================
    public static class CometTailTrail extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> trailEntities = new ArrayList<>();
        private final Location[] positionHistory = new Location[30];
        private int historyIndex = 0;

        public CometTailTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_tail_trail", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initialize position history to center
            for (int i = 0; i < 30; i++) {
                positionHistory[i] = center.clone();
            }

            // Spawn 30 trident trail entities
            for (int i = 0; i < 30; i++) {
                ItemDisplayHandle h = displayBuilder.spawnItem(center.clone(),
                        new ItemStack(Material.TRIDENT));
                // Scale decreases linearly: index 0 = 0.55, index 29 = 0.08
                float scale = 0.55f - (0.47f / 29.0f) * i;
                h.scale(scale, scale, scale).glow(180, 40, 255).interpolation(2, 0);
                trailEntities.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Push new position every 2 ticks
            if (ticksAlive % 2 == 0) {
                positionHistory[historyIndex] = center.clone();
                historyIndex = (historyIndex + 1) % 30;
            }

            // Update each trail entity
            for (int i = 0; i < trailEntities.size(); i++) {
                // Slot 0 = newest, slot 29 = oldest
                int slotIndex = (historyIndex - 1 - i + 300) % 30;
                Location target = positionHistory[slotIndex];
                if (target == null) continue;

                trailEntities.get(i).entity().teleport(target);

                // Self-rotation: 3.6 deg/tick with 12-deg phase offset per index
                float rotAngle = (float) Math.toRadians(ticksAlive * 3.6 + i * 12.0);
                trailEntities.get(i).rotate(rotAngle, 0, 1, 0);
                trailEntities.get(i).interpolation(2, 0);

                // Particles: indices 0-24 get violet DUST, 25-29 get CRIT
                if (i < 25) {
                    w.spawnParticle(Particle.DUST, target, 1,
                            0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 40, 255), 1.2f));
                } else {
                    w.spawnParticle(Particle.CRIT, target, 2, 0.2, 0.2, 0.2, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CometTailTrail(plugin); }
    }

    // ================================================================
    // #52 -- COMET HEAD FLARE
    // Single oversized TRIDENT at 1.1 scale co-located with Calamitas
    // at Y+0.5. Oriented along movement vector. Sinusoidal scale
    // oscillation (0.9 to 1.2, period 60 ticks). FIREWORKS_SPARK bursts.
    // ================================================================
    public static class CometHeadFlare extends BlockDisplayAttack {

        private ItemDisplayHandle headTrident;

        public CometHeadFlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_head_flare", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(0, 0.5, 0);
            headTrident = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            headTrident.scale(1.1f, 1.1f, 1.1f).glow(200, 10, 10).interpolation(3, 0);
            spawnedEntities.add(headTrident.entity());

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (headTrident == null) return;

            Location loc = center.clone().add(0, 0.5, 0);
            headTrident.entity().teleport(loc);

            // Sinusoidal scale oscillation: period 60 ticks
            float scale = 1.05f + 0.15f * (float) Math.sin(2.0 * Math.PI * ticksAlive / 60.0);
            headTrident.scale(scale, scale, scale);
            headTrident.interpolation(3, 0);

            // FIREWORKS_SPARK burst every 3 ticks
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.FIREWORK, loc, 3, 0.2, 0.2, 0.2, 0.02);
            }

            // DRAGON_BREATH exhaust trail
            w.spawnParticle(Particle.DRAGON_BREATH, loc, 2, 0.3, 0.3, 0.3, 0);

            // Sound every 40 ticks
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.2f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CometHeadFlare(plugin); }
    }

    // ================================================================
    // #53 -- OUTER PERIMETER TRIDENT RING ("Fence of Death")
    // 24 TRIDENT ItemDisplays at radius 28, Y+1.5, tips pointing inward.
    // Rise from 30 blocks below over 80 ticks. Slow Z-axis spin at
    // 0.5 deg/tick. Crimson DUST from each tip. Phase 4 contraction
    // from radius 28 to 18 over 400 ticks.
    // ================================================================
    public static class OuterPerimeterTridentRing extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> perimeterTridents = new ArrayList<>();
        private static final int COUNT = 24;
        private static final double BASE_RADIUS = 28.0;

        public OuterPerimeterTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("outer_perimeter_trident_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 24 tridents 30 blocks below floor (rise animation in onTick)
            for (int i = 0; i < COUNT; i++) {
                double angle = Math.toRadians(i * 15.0);
                double x = Math.cos(angle) * BASE_RADIUS;
                double z = Math.sin(angle) * BASE_RADIUS;
                Location loc = center.clone().add(x, 1.5 - 30, z);

                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
                h.scale(1.8f, 1.8f, 1.8f).glow(200, 0, 0).interpolation(3, 0);
                // Tip points inward (toward center), slight downward pitch
                float facingAngle = (float) Math.toRadians(-15.0);
                h.rotate(facingAngle, 1, 0, 0);
                perimeterTridents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation over 80 ticks
            double yOffset;
            if (ticksAlive < 80) {
                yOffset = 1.5 - 30 + (30.0 * ticksAlive / 80.0);
            } else {
                yOffset = 1.5;
            }

            // Slow spin at 0.5 deg/tick after settling
            float spinAngle = (ticksAlive > 80)
                    ? (float) Math.toRadians((ticksAlive - 80) * 0.5)
                    : (float) Math.toRadians(ticksAlive * 4.5); // Fast spin during ascent

            double radius = BASE_RADIUS;

            for (int i = 0; i < perimeterTridents.size(); i++) {
                double angle = Math.toRadians(i * 15.0);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, yOffset, z);
                perimeterTridents.get(i).entity().teleport(loc);

                // Rotation: tip-to-tail axis spin
                float pitchTilt = (float) Math.toRadians(-15.0);
                perimeterTridents.get(i).rotate(pitchTilt + spinAngle, 0, 0, 1);
                perimeterTridents.get(i).interpolation(3, 0);

                // Crimson DUST from tip
                w.spawnParticle(Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.5f));
            }

            // Ambient beacon sound loop
            if (ticksAlive % 80 == 0 && ticksAlive > 80) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.15f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OuterPerimeterTridentRing(plugin); }
    }

    // ================================================================
    // #54 -- INNER PERIMETER TRIDENT RING (Phase 4 Second Layer)
    // 12 TRIDENT ItemDisplays at radius 14, Y+3, tips pointing skyward.
    // Staggered materialization with FLASH bursts. Vertical sinusoidal
    // oscillation: amplitude +-0.4, period 100 ticks, phase offset 25
    // per index. Y-axis spin at 1.2 deg/tick. END_ROD from tips.
    // ================================================================
    public static class InnerPerimeterTridentRing extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> innerTridents = new ArrayList<>();
        private static final int COUNT = 12;

        public InnerPerimeterTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inner_perimeter_trident_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Staggered materialization: one trident every 5 ticks
            for (int i = 0; i < COUNT; i++) {
                double angle = Math.toRadians(i * 30.0);
                double x = Math.cos(angle) * 14.0;
                double z = Math.sin(angle) * 14.0;
                Location loc = center.clone().add(x, 3, z);

                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
                h.scale(1.4f, 1.4f, 1.4f).glow(200, 10, 10).interpolation(3, 0);
                innerTridents.add(h);
                spawnedEntities.add(h.entity());

                // FLASH particle materialization burst
                w.spawnParticle(Particle.FLASH, loc, 8, 0.3, 0.3, 0.3, 0);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < innerTridents.size(); i++) {
                double angle = Math.toRadians(i * 30.0);
                double x = Math.cos(angle) * 14.0;
                double z = Math.sin(angle) * 14.0;

                // Vertical sinusoidal oscillation
                double yOsc = 0.4 * Math.sin(2.0 * Math.PI * (ticksAlive + i * 25) / 100.0);
                Location loc = center.clone().add(x, 3 + yOsc, z);
                innerTridents.get(i).entity().teleport(loc);

                // Y-axis spin at 1.2 deg/tick
                float spinAngle = (float) Math.toRadians(ticksAlive * 1.2);
                innerTridents.get(i).rotate(spinAngle, 0, 1, 0);
                innerTridents.get(i).interpolation(3, 0);

                // END_ROD particles from upward-pointing tips
                w.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 2,
                        0.1, 0.1, 0.1, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InnerPerimeterTridentRing(plugin); }
    }

    // ================================================================
    // #55 -- CENTRAL VERTICAL TRIDENT COLUMN
    // 16 TRIDENT ItemDisplays stacked vertically at arena center,
    // Y+8 to Y+38 (2-block spacing). Tips pointing down. Scale 1.5.
    // Y-axis self-rotation at 0.8 deg/tick. Tight helical orbit (radius
    // 0.4, period 120 ticks, 22.5-deg phase offset per index).
    // SPELL_WITCH drip particles from tips. Sequential top-down assembly.
    // ================================================================
    public static class CentralVerticalTridentColumn extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> columnTridents = new ArrayList<>();
        private static final int COUNT = 16;

        public CentralVerticalTridentColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("central_vertical_trident_column", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 16 tridents at descending positions (top-down assembly)
            for (int i = 0; i < COUNT; i++) {
                double targetY = 8 + i * 2.0; // Y+8 (lowest) to Y+38 (highest)
                Location loc = center.clone().add(0, 60, 0); // Start high, descend in onTick
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
                h.scale(1.5f, 1.5f, 1.5f).glow(180, 40, 255).interpolation(5, 0);
                columnTridents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < columnTridents.size(); i++) {
                double targetY = 8 + i * 2.0;
                // Top trident (index 15) arrives first, then each lower trident 8 ticks later
                int arrivalTick = (COUNT - 1 - i) * 8;

                double currentY;
                if (ticksAlive < arrivalTick) {
                    currentY = 60; // Still above, waiting
                } else if (ticksAlive < arrivalTick + 40) {
                    // Descending over 40 ticks
                    double progress = (ticksAlive - arrivalTick) / 40.0;
                    currentY = 60 - (60 - targetY) * progress;
                } else {
                    currentY = targetY;
                }

                // Helical orbit: radius 0.4, period 120 ticks, 22.5-deg phase offset per index
                double orbitAngle = Math.toRadians(ticksAlive * 3.0 + i * 22.5);
                double orbitX = Math.cos(orbitAngle) * 0.4;
                double orbitZ = Math.sin(orbitAngle) * 0.4;

                Location loc = center.clone().add(orbitX, currentY, orbitZ);
                columnTridents.get(i).entity().teleport(loc);

                // Y-axis self-rotation at 0.8 deg/tick
                float rotAngle = (float) Math.toRadians(ticksAlive * 0.8);
                columnTridents.get(i).rotate(rotAngle, 0, 1, 0);
                columnTridents.get(i).interpolation(5, 0);

                // SPELL_WITCH particle drip from tip (downward)
                if (currentY == targetY) {
                    w.spawnParticle(Particle.WITCH, loc, 1, 0.1, 0.1, 0.1, 0);
                }

                // Sound as each trident settles
                if (ticksAlive == arrivalTick + 40) {
                    float pitch = 1.2f + i * 0.05f;
                    DisplayBuilder.playSound(loc, Sound.ITEM_TRIDENT_HIT, 0.4f, pitch);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralVerticalTridentColumn(plugin); }
    }

    // ================================================================
    // #56 -- NORTH SENTINEL TRIDENT
    // Single 2.0-scale TRIDENT at due north (Z-20), Y+8. Tip faces
    // south (toward center). Continuous spin at 2.0 deg/tick. Vertical
    // bob: amplitude +-0.6, period 80 ticks. Crimson DUST aura (RGB
    // 180,0,0). Phase 3: scale up to 2.8. Phase 4: FLASH particles.
    // ================================================================
    public static class NorthSentinelTrident extends BlockDisplayAttack {

        private ItemDisplayHandle sentinel;

        public NorthSentinelTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_sentinel_trident", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(0, 8, -20);
            sentinel = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            sentinel.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            spawnedEntities.add(sentinel.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bob: period 80 ticks
            double yBob = 0.6 * Math.sin(2.0 * Math.PI * ticksAlive / 80.0);
            Location loc = center.clone().add(0, 8 + yBob, -20);
            sentinel.entity().teleport(loc);

            // Continuous spin at 2.0 deg/tick
            float spinAngle = (float) Math.toRadians(ticksAlive * 2.0);
            sentinel.rotate(spinAngle, 0, 1, 0);
            sentinel.interpolation(3, 0);

            // Crimson DUST aura
            w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.8f));

            // Sound at bob cycle peak
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.3f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthSentinelTrident(plugin); }
    }

    // ================================================================
    // #57 -- SOUTH SENTINEL TRIDENT
    // Single 2.0-scale TRIDENT at due south (Z+20), Y+8. Reversed spin
    // at -2.0 deg/tick. Bob period 88 ticks (offset from north).
    // Crimson DUST aura.
    // ================================================================
    public static class SouthSentinelTrident extends BlockDisplayAttack {

        private ItemDisplayHandle sentinel;

        public SouthSentinelTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("south_sentinel_trident", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(0, 8, 20);
            sentinel = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            sentinel.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            spawnedEntities.add(sentinel.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bob: period 88 ticks (offset from north)
            double yBob = 0.6 * Math.sin(2.0 * Math.PI * ticksAlive / 88.0);
            Location loc = center.clone().add(0, 8 + yBob, 20);
            sentinel.entity().teleport(loc);

            // Reversed spin at -2.0 deg/tick (counterclockwise)
            float spinAngle = (float) Math.toRadians(-ticksAlive * 2.0);
            sentinel.rotate(spinAngle, 0, 1, 0);
            sentinel.interpolation(3, 0);

            // Crimson DUST aura
            w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.8f));

            // Sound at bob cycle peak
            if (ticksAlive % 88 == 0) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.3f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthSentinelTrident(plugin); }
    }

    // ================================================================
    // #58 -- EAST SENTINEL TRIDENT
    // Single 2.0-scale TRIDENT at due east (X+20), Y+8. Tip points west
    // (toward center). Spin at +2.0 deg/tick. Bob period 76 ticks.
    // Crimson DUST aura.
    // ================================================================
    public static class EastSentinelTrident extends BlockDisplayAttack {

        private ItemDisplayHandle sentinel;

        public EastSentinelTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("east_sentinel_trident", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(20, 8, 0);
            sentinel = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            sentinel.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            spawnedEntities.add(sentinel.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bob: period 76 ticks
            double yBob = 0.6 * Math.sin(2.0 * Math.PI * ticksAlive / 76.0);
            Location loc = center.clone().add(20, 8 + yBob, 0);
            sentinel.entity().teleport(loc);

            // Spin at +2.0 deg/tick
            float spinAngle = (float) Math.toRadians(ticksAlive * 2.0);
            sentinel.rotate(spinAngle, 0, 1, 0);
            sentinel.interpolation(3, 0);

            // Crimson DUST aura
            w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.8f));

            // Sound at bob cycle peak
            if (ticksAlive % 76 == 0) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.3f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastSentinelTrident(plugin); }
    }

    // ================================================================
    // #59 -- WEST SENTINEL TRIDENT
    // Single 2.0-scale TRIDENT at due west (X-20), Y+8. Tip points east
    // (toward center). Reversed spin at -2.0 deg/tick. Bob period 84 ticks.
    // Crimson DUST aura.
    // ================================================================
    public static class WestSentinelTrident extends BlockDisplayAttack {

        private ItemDisplayHandle sentinel;

        public WestSentinelTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("west_sentinel_trident", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location loc = center.clone().add(-20, 8, 0);
            sentinel = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            sentinel.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            spawnedEntities.add(sentinel.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Vertical bob: period 84 ticks
            double yBob = 0.6 * Math.sin(2.0 * Math.PI * ticksAlive / 84.0);
            Location loc = center.clone().add(-20, 8 + yBob, 0);
            sentinel.entity().teleport(loc);

            // Reversed spin at -2.0 deg/tick
            float spinAngle = (float) Math.toRadians(-ticksAlive * 2.0);
            sentinel.rotate(spinAngle, 0, 1, 0);
            sentinel.interpolation(3, 0);

            // Crimson DUST aura
            w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.8f));

            // Sound at bob cycle peak
            if (ticksAlive % 84 == 0) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.3f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WestSentinelTrident(plugin); }
    }

    // ================================================================
    // #60 -- NORTHEAST DIAGONAL SENTINEL TRIDENT
    // Single 1.8-scale TRIDENT at NE (X+14.14, Z-14.14), Y+10. Spin
    // +1.5 deg/tick. Bob period 92 ticks, amplitude +-0.5. Orbit around
    // anchor at radius 1.2, period 200 ticks. Deep violet DUST aura
    // (RGB 140,0,200). Rise from floor on Phase 2 entry (40-tick ascent).
    // ================================================================
    public static class NortheastDiagonalSentinel extends BlockDisplayAttack {

        private ItemDisplayHandle sentinel;
        private static final double ANCHOR_X = 14.14;
        private static final double ANCHOR_Z = -14.14;

        public NortheastDiagonalSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("northeast_diagonal_sentinel", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start at floor level, rise over 40 ticks
            Location loc = center.clone().add(ANCHOR_X, 0, ANCHOR_Z);
            sentinel = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
            sentinel.scale(1.8f, 1.8f, 1.8f).glow(140, 0, 200).interpolation(3, 0);
            spawnedEntities.add(sentinel.entity());

            DisplayBuilder.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation over 40 ticks to Y+10
            double currentY;
            if (ticksAlive < 40) {
                currentY = 10.0 * ticksAlive / 40.0;
            } else {
                currentY = 10.0;
            }

            // Bob: period 92 ticks, amplitude +-0.5
            double yBob = 0.5 * Math.sin(2.0 * Math.PI * ticksAlive / 92.0);

            // Orbit around anchor: radius 1.2, period 200 ticks (1.8 deg/tick)
            double orbitAngle = Math.toRadians(ticksAlive * 1.8);
            double orbitX = Math.cos(orbitAngle) * 1.2;
            double orbitZ = Math.sin(orbitAngle) * 1.2;

            Location loc = center.clone().add(ANCHOR_X + orbitX, currentY + yBob, ANCHOR_Z + orbitZ);
            sentinel.entity().teleport(loc);

            // Spin at +1.5 deg/tick
            float spinAngle = (float) Math.toRadians(ticksAlive * 1.5);
            sentinel.rotate(spinAngle, 0, 1, 0);
            sentinel.interpolation(3, 0);

            // Deep violet DUST aura
            w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(140, 0, 200), 1.5f));
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NortheastDiagonalSentinel(plugin); }
    }

    // ================================================================
    // #61 -- SE, SW, NW DIAGONAL SENTINEL TRIDENTS
    // 3 TRIDENT ItemDisplays at the remaining diagonal positions, all
    // at Y+10. Each mirrors NE sentinel mechanics with orbit phase
    // offsets (SE=0, SW=90, NW=180, NE=270) and bob phase offsets
    // (SE=0, SW=50, NW=100 ticks). Slight color variations per sentinel.
    // ================================================================
    public static class DiagonalSentinelTriad extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> sentinels = new ArrayList<>();
        private static final double[][] POSITIONS = {
                { 14.14,  14.14},  // SE
                {-14.14,  14.14},  // SW
                {-14.14, -14.14},  // NW
        };
        private static final double[] ORBIT_PHASES = {0, 90, 180};  // degrees
        private static final int[] BOB_OFFSETS = {0, 50, 100};       // ticks
        private static final int[][] DUST_COLORS = {
                {140, 0, 200},   // SE: deep violet
                {180, 0, 0},     // SW: crimson
                {100, 0, 180},   // NW: dark purple
        };

        public DiagonalSentinelTriad(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("diagonal_sentinel_triad", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(POSITIONS[i][0], 0, POSITIONS[i][1]);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
                h.scale(1.8f, 1.8f, 1.8f)
                        .glow(DUST_COLORS[i][0], DUST_COLORS[i][1], DUST_COLORS[i][2])
                        .interpolation(3, 0);
                sentinels.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < sentinels.size(); i++) {
                // Rise animation over 40 ticks to Y+10
                double currentY;
                if (ticksAlive < 40) {
                    currentY = 10.0 * ticksAlive / 40.0;
                } else {
                    currentY = 10.0;
                }

                // Bob: period 92 ticks with phase offset
                double yBob = 0.5 * Math.sin(2.0 * Math.PI * (ticksAlive + BOB_OFFSETS[i]) / 92.0);

                // Orbit: radius 1.2, period 200 ticks, with phase offset
                double orbitAngle = Math.toRadians(ticksAlive * 1.8 + ORBIT_PHASES[i]);
                double orbitX = Math.cos(orbitAngle) * 1.2;
                double orbitZ = Math.sin(orbitAngle) * 1.2;

                Location loc = center.clone().add(
                        POSITIONS[i][0] + orbitX,
                        currentY + yBob,
                        POSITIONS[i][1] + orbitZ);
                sentinels.get(i).entity().teleport(loc);

                // Spin at +1.5 deg/tick
                float spinAngle = (float) Math.toRadians(ticksAlive * 1.5);
                sentinels.get(i).rotate(spinAngle, 0, 1, 0);
                sentinels.get(i).interpolation(3, 0);

                // DUST aura with per-sentinel color
                w.spawnParticle(Particle.DUST, loc, 2, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(
                                Color.fromRGB(DUST_COLORS[i][0], DUST_COLORS[i][1], DUST_COLORS[i][2]),
                                1.5f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiagonalSentinelTriad(plugin); }
    }

    // ================================================================
    // #62 -- SENTINEL CONVERGENCE FORMATION
    // At 15% HP all 8 sentinel positions converge to orbit Calamitas
    // at radius 8, Y+2 above boss. 60-tick convergence transition.
    // Rigid 1.2 deg/tick clockwise orbit. All tips face inward.
    // Scale increases to 2.2. CRIT + crimson DUST (RGB 255,50,50).
    // ================================================================
    public static class SentinelConvergenceFormation extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> convergenceTridents = new ArrayList<>();
        private static final int COUNT = 8;

        public SentinelConvergenceFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sentinel_convergence_formation", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 8 tridents in a formation at radius 8, Y+2
            for (int i = 0; i < COUNT; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 2, z);

                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.TRIDENT));
                h.scale(2.2f, 2.2f, 2.2f).glow(255, 50, 50).interpolation(5, 0);
                convergenceTridents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rigid orbit at 1.2 deg/tick clockwise, radius 8
            double orbitAngle = Math.toRadians(ticksAlive * 1.2);

            for (int i = 0; i < convergenceTridents.size(); i++) {
                double angle = Math.toRadians(i * 45.0) + orbitAngle;
                double x = Math.cos(angle) * 8.0;
                double z = Math.sin(angle) * 8.0;
                Location loc = center.clone().add(x, 2, z);
                convergenceTridents.get(i).entity().teleport(loc);

                // Tips face inward toward center -- rotation facing center
                float facingAngle = (float) (angle + Math.PI); // Inward
                convergenceTridents.get(i).rotate(facingAngle, 0, 1, 0);
                convergenceTridents.get(i).interpolation(5, 0);

                // CRIT + crimson DUST particles
                w.spawnParticle(Particle.CRIT, loc, 3, 0.4, 0.4, 0.4, 0);
                w.spawnParticle(Particle.DUST, loc, 3, 0.4, 0.4, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 50, 50), 2.0f));
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.3f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SentinelConvergenceFormation(plugin); }
    }
}
