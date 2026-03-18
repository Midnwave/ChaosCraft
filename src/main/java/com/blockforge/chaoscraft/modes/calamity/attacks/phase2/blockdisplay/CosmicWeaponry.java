package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.blockdisplay;

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
 * Phase 2 (4B) Block Display -- GROUP 8: COSMIC WEAPONRY
 * 10 attacks featuring crystalline weapons and offensive structures
 * born from DoG's dimensional essence. Giant crystal blades, orbital
 * bombardment platforms, and prismarine war-constructs.
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG palette: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - Damage in HP (4.0-12.0 range)
 */
public final class CosmicWeaponry {

    private CosmicWeaponry() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrystalCleaver(plugin));
        registry.register(new VoidLance(plugin));
        registry.register(new PrismarineBallistaBolt(plugin));
        registry.register(new AmethystSawblade(plugin));
        registry.register(new DevourerFangArray(plugin));
        registry.register(new OrbitalHammer(plugin));
        registry.register(new CrystallineFlail(plugin));
        registry.register(new DimensionalCaltrops(plugin));
        registry.register(new ConvergenceBeacon(plugin));
        registry.register(new WarpedBattlement(plugin));
    }

    // ================================================================
    // 1. CRYSTAL CLEAVER -- Massive vertical blade of amethyst that
    //    slowly falls and slashes across the arena
    // ================================================================
    public static class CrystalCleaver extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private BlockDisplayHandle hiltBlock;
        private BlockDisplayHandle pommelBlock;

        public CrystalCleaver(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cleaver", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 7 amethyst blocks tapering to a point
            for (int i = 0; i < 7; i++) {
                Location loc = center.clone().add(0, 10 + i * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                float taper = 1.0f - (i * 0.1f);
                h.scale(taper, 1.2f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Hilt: polished blackstone
            hiltBlock = displayBuilder.spawnBlock(center.clone().add(0, 9, 0), Material.POLISHED_BLACKSTONE);
            hiltBlock.scale(1.5f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(hiltBlock.entity());

            // Pommel: sea lantern
            pommelBlock = displayBuilder.spawnBlock(center.clone().add(0, 8, 0), Material.SEA_LANTERN);
            pommelBlock.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(pommelBlock.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Hover at height (0-60 ticks)
            // Phase 2: Slam down (60-80 ticks)
            // Phase 3: Embedded in ground, trembling (80+)
            float yOffset;
            float rotation = 0;

            if (ticksAlive < 60) {
                // Hover and slowly rotate
                yOffset = 0;
                rotation = ticksAlive * 0.02f;
                // Slight hover bob
                yOffset = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 15);
            } else if (ticksAlive < 80) {
                // Slam down
                float slamProgress = (ticksAlive - 60) / 20.0f;
                yOffset = -slamProgress * 10.0f;
                rotation = 60 * 0.02f;
            } else {
                // Embedded: tremble
                yOffset = -10.0f;
                rotation = 60 * 0.02f;
                float tremble = 0.05f * (float) Math.sin(ticksAlive * 3.0);
                yOffset += tremble;
            }

            // Update blade positions
            for (int i = 0; i < bladeBlocks.size(); i++) {
                Location target = center.clone().add(0, 10 + i * 1.2 + yOffset, 0);
                bladeBlocks.get(i).entity().teleport(target);
                bladeBlocks.get(i).rotate(rotation, 0, 1, 0);
                bladeBlocks.get(i).interpolation(2, 0);
            }
            if (hiltBlock != null) {
                hiltBlock.entity().teleport(center.clone().add(0, 9 + yOffset, 0));
            }
            if (pommelBlock != null) {
                pommelBlock.entity().teleport(center.clone().add(0, 8 + yOffset, 0));
            }

            // Slam impact
            if (ticksAlive == 79) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                DisplayBuilder.cyanDust(center, 40, 3.0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, center, 25, 2.0, 0.5, 2.0, 0.1);
            }

            // Ambient blade glow
            if (ticksAlive % 5 == 0) {
                Location bladeTip = center.clone().add(0, 18.4 + yOffset, 0);
                DisplayBuilder.dustParticles(bladeTip, 3, 0.3, 128, 0, 255, 1.0f);
            }

            if (ticksAlive % 40 == 0 && ticksAlive < 60) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalCleaver(plugin); }
    }

    // ================================================================
    // 2. VOID LANCE -- Horizontal spear of crystal that charges
    //    forward through the arena
    // ================================================================
    public static class VoidLance extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private BlockDisplayHandle lanceTip;
        private BlockDisplayHandle baseMount;

        public VoidLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_lance", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shaft: 5 dark prismarine blocks in a horizontal line
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-8 + i * 1.5, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(1.5f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tip: large amethyst cluster
            lanceTip = displayBuilder.spawnBlock(center.clone().add(-0.5, 2, 0), Material.LARGE_AMETHYST_BUD);
            lanceTip.scale(0.5f, 0.5f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(lanceTip.entity());

            // Base mount: polished blackstone
            baseMount = displayBuilder.spawnBlock(center.clone().add(-8, 2, 0), Material.POLISHED_BLACKSTONE);
            baseMount.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(baseMount.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Charge (0-40 ticks), accelerating forward
            // Phase 2: Hold extended (40-200), trembling
            // Phase 3: Retract (200+)
            float xOffset;
            if (ticksAlive < 40) {
                float chargeProgress = ticksAlive / 40.0f;
                xOffset = chargeProgress * chargeProgress * 12.0f; // Accelerating
            } else if (ticksAlive < 200) {
                xOffset = 12.0f;
                float tremble = 0.08f * (float) Math.sin(ticksAlive * 2.0);
                xOffset += tremble;
            } else {
                float retractProgress = (ticksAlive - 200) / 50.0f;
                xOffset = 12.0f * (1.0f - Math.min(1.0f, retractProgress));
            }

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location target = center.clone().add(-8 + i * 1.5 + xOffset, 2, 0);
                shaftBlocks.get(i).entity().teleport(target);
            }
            if (lanceTip != null) {
                lanceTip.entity().teleport(center.clone().add(-0.5 + xOffset, 2, 0));
            }
            if (baseMount != null) {
                baseMount.entity().teleport(center.clone().add(-8 + xOffset, 2, 0));
            }

            // Charge arrival impact
            if (ticksAlive == 39) {
                DisplayBuilder.playSound(center.clone().add(12, 0, 0), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
                DisplayBuilder.cyanDust(center.clone().add(12, 2, 0), 20, 1.5);
            }

            // Trail particles during charge
            if (ticksAlive < 40 && ticksAlive % 2 == 0) {
                Location trail = lanceTip != null ? lanceTip.entity().getLocation() : center;
                w.spawnParticle(Particle.END_ROD, trail, 3, 0.1, 0.1, 0.1, 0.05);
            }

            if (ticksAlive % 30 == 0 && ticksAlive >= 40) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidLance(plugin); }
    }

    // ================================================================
    // 3. PRISMARINE BALLISTA BOLT -- Giant crystal projectile that
    //    arcs overhead and embeds in the ground
    // ================================================================
    public static class PrismarineBallistaBolt extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> boltBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> finBlocks = new ArrayList<>();

        public PrismarineBallistaBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prismarine_ballista_bolt", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(11.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(11.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Bolt shaft: 4 prismarine blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 15, i - 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.5f, 0.5f, 1.2f).glow(0, 200, 255).interpolation(3, 0);
                boltBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fins: 4 amethyst blocks at tail
            double[][] finOff = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0.5, 0}, {0, -0.5, 0}};
            for (double[] off : finOff) {
                Location loc = center.clone().add(off[0], 15 + off[1], -2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                finBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Parabolic arc: start at Y+15, arc down to Y+0 over 60 ticks
            float yOffset, zOffset;
            boolean impacted = false;

            if (ticksAlive < 60) {
                float t = ticksAlive / 60.0f;
                yOffset = 15.0f * (1.0f - t); // Linear descent for simplicity
                zOffset = t * 8.0f; // Forward travel
            } else {
                yOffset = 0;
                zOffset = 8.0f;
                impacted = true;
                float tremble = 0.04f * (float) Math.sin(ticksAlive * 2.5);
                yOffset += tremble;
            }

            for (int i = 0; i < boltBlocks.size(); i++) {
                Location target = center.clone().add(0, yOffset, zOffset + i - 2);
                boltBlocks.get(i).entity().teleport(target);
            }
            for (int i = 0; i < finBlocks.size(); i++) {
                double[][] finOff = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0.5, 0}, {0, -0.5, 0}};
                Location target = center.clone().add(finOff[i][0], yOffset + finOff[i][1], zOffset - 2);
                finBlocks.get(i).entity().teleport(target);
            }

            // Impact
            if (ticksAlive == 60) {
                Location impactLoc = center.clone().add(0, 0, 8);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                DisplayBuilder.cyanDust(impactLoc, 35, 2.5);
                w.spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 20, 2.0, 1.0, 2.0, 0.1);
            }

            // Trail particles while flying
            if (ticksAlive < 60 && ticksAlive % 2 == 0) {
                Location trail = boltBlocks.get(0).entity().getLocation();
                w.spawnParticle(Particle.END_ROD, trail, 2, 0.1, 0.1, 0.1, 0.03);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismarineBallistaBolt(plugin); }
    }

    // ================================================================
    // 4. AMETHYST SAWBLADE -- Rotating disc of crystal that spins
    //    horizontally at knee height, sweeping the arena
    // ================================================================
    public static class AmethystSawblade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeSegments = new ArrayList<>();
        private BlockDisplayHandle axleBlock;

        public AmethystSawblade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("amethyst_sawblade", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sawblade disc: 12 segments in a ring
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 1, Math.sin(angle) * 2.5);
                Material mat = (i % 3 == 0) ? Material.LARGE_AMETHYST_BUD : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.15f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                bladeSegments.add(h);
                spawnedEntities.add(h.entity());
            }

            // Axle: sea lantern center
            axleBlock = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.SEA_LANTERN);
            axleBlock.scale(0.5f, 0.3f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(axleBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fast rotation: 3 degrees per tick
            float rotAngle = ticksAlive * 0.0524f; // ~3 deg/tick in radians

            for (int i = 0; i < bladeSegments.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + rotAngle;
                Location target = center.clone().add(Math.cos(angle) * 2.5, 1, Math.sin(angle) * 2.5);
                bladeSegments.get(i).entity().teleport(target);
                bladeSegments.get(i).interpolation(1, 0);
            }

            // Sparks at blade edges
            if (ticksAlive % 3 == 0) {
                double sparkAngle = rotAngle + (ticksAlive % 12) * (Math.PI / 6);
                Location sparkLoc = center.clone().add(Math.cos(sparkAngle) * 2.8, 1, Math.sin(sparkAngle) * 2.8);
                w.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 3, 0.1, 0.05, 0.1, 0.05);
            }

            // Axle glow pulse
            if (axleBlock != null) {
                float s = 0.5f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 10);
                axleBlock.scale(s, 0.3f, s);
                axleBlock.interpolation(2, 0);
            }

            // Dust trail
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 4, 2.5);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystSawblade(plugin); }
    }

    // ================================================================
    // 5. DEVOURER FANG ARRAY -- Ring of crystal fangs that rise from
    //    the ground and snap shut toward the center
    // ================================================================
    public static class DevourerFangArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fangs = new ArrayList<>();
        private final List<BlockDisplayHandle> baseRing = new ArrayList<>();

        public DevourerFangArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devourer_fang_array", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 fangs in a circle, initially underground
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, -2, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.5f, 1.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                fangs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Base ring: dark prismarine
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, -0.3, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.7f, 0.2f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                baseRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Fangs emerge (0-30)
            // Phase 2: Fangs tilt inward to snap (30-45)
            // Phase 3: Hold (45-120)
            // Phase 4: Retract (120+)

            for (int i = 0; i < fangs.size(); i++) {
                double angle = (Math.PI * 2 * i) / 10;

                float yPos, tiltAngle;
                float radius = 3.5f;

                if (ticksAlive < 30) {
                    // Emerge
                    float progress = ticksAlive / 30.0f;
                    yPos = -2 + progress * 2.5f;
                    tiltAngle = 0;
                } else if (ticksAlive < 45) {
                    // Snap inward
                    float snapProgress = (ticksAlive - 30) / 15.0f;
                    yPos = 0.5f;
                    tiltAngle = snapProgress * 0.7f; // Tilt ~40 degrees inward
                    radius = 3.5f - snapProgress * 1.0f;
                } else if (ticksAlive < 120) {
                    // Hold
                    yPos = 0.5f;
                    tiltAngle = 0.7f;
                    radius = 2.5f;
                    float tremble = 0.04f * (float) Math.sin((ticksAlive + i * 5) * 2.0);
                    yPos += tremble;
                } else {
                    // Retract
                    float retract = Math.min(1.0f, (ticksAlive - 120) / 30.0f);
                    yPos = 0.5f - retract * 2.5f;
                    tiltAngle = 0.7f * (1.0f - retract);
                    radius = 2.5f + retract * 1.0f;
                }

                Location target = center.clone().add(Math.cos(angle) * radius, yPos, Math.sin(angle) * radius);
                fangs.get(i).entity().teleport(target);
                fangs.get(i).rotate(tiltAngle, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                fangs.get(i).interpolation(2, 0);
            }

            // Snap sound
            if (ticksAlive == 44) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
                DisplayBuilder.cyanDust(center, 25, 2.0);
            }

            // Particles between fangs
            if (ticksAlive % 6 == 0 && ticksAlive > 30) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 8, 1.5, 128, 0, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DevourerFangArray(plugin); }
    }

    // ================================================================
    // 6. ORBITAL HAMMER -- Massive crystal block that orbits overhead
    //    then slams down onto a target position
    // ================================================================
    public static class OrbitalHammer extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> hammerHead = new ArrayList<>();
        private BlockDisplayHandle shaftBlock;

        public OrbitalHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_hammer", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Hammer head: 3x2 block cluster
            double[][] headOff = {
                    {-0.5, 0, -0.5}, {0.5, 0, -0.5}, {-0.5, 0, 0.5},
                    {0.5, 0, 0.5}, {0, 1, 0}, {0, -1, 0}
            };
            for (double[] off : headOff) {
                Location loc = center.clone().add(off[0], 12 + off[1], off[2]);
                Material mat = (Math.abs(off[1]) > 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                hammerHead.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shaft
            shaftBlock = displayBuilder.spawnBlock(center.clone().add(0, 14, 0), Material.POLISHED_BLACKSTONE);
            shaftBlock.scale(0.4f, 3.0f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(shaftBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Orbit overhead (0-80)
            // Phase 2: Rise higher (80-100)
            // Phase 3: Slam down (100-115)
            // Phase 4: Embedded (115+)

            float yOffset = 0;
            float orbitAngle = ticksAlive * 0.04f;
            float orbitRadius = 3.0f;

            if (ticksAlive < 80) {
                // Orbit
                yOffset = 0;
            } else if (ticksAlive < 100) {
                // Rise
                float riseProgress = (ticksAlive - 80) / 20.0f;
                yOffset = riseProgress * 5.0f;
                orbitRadius = 3.0f * (1.0f - riseProgress);
            } else if (ticksAlive < 115) {
                // Slam
                float slamProgress = (ticksAlive - 100) / 15.0f;
                yOffset = 5.0f - slamProgress * 17.0f; // Slam from +5 to -12
                orbitRadius = 0;
            } else {
                yOffset = -12.0f;
                orbitRadius = 0;
                float tremble = 0.06f * (float) Math.sin(ticksAlive * 2.5);
                yOffset += tremble;
            }

            float xOff = (float) Math.cos(orbitAngle) * orbitRadius;
            float zOff = (float) Math.sin(orbitAngle) * orbitRadius;

            double[][] headOff = {
                    {-0.5, 0, -0.5}, {0.5, 0, -0.5}, {-0.5, 0, 0.5},
                    {0.5, 0, 0.5}, {0, 1, 0}, {0, -1, 0}
            };
            for (int i = 0; i < hammerHead.size(); i++) {
                Location target = center.clone().add(
                        headOff[i][0] + xOff, 12 + headOff[i][1] + yOffset, headOff[i][2] + zOff);
                hammerHead.get(i).entity().teleport(target);
            }
            if (shaftBlock != null) {
                shaftBlock.entity().teleport(center.clone().add(xOff, 14 + yOffset, zOff));
            }

            // Impact
            if (ticksAlive == 114) {
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
                DisplayBuilder.cyanDust(center, 50, 4.0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, center, 30, 3.0, 0.5, 3.0, 0.1);
            }

            // Shadow on ground while orbiting
            if (ticksAlive < 100 && ticksAlive % 4 == 0) {
                Location shadow = center.clone().add(xOff, 0.1, zOff);
                DisplayBuilder.dustParticles(shadow, 5, 0.8, 0, 200, 255, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalHammer(plugin); }
    }

    // ================================================================
    // 7. CRYSTALLINE FLAIL -- Swinging chain of crystal blocks that
    //    arcs back and forth in a pendulum pattern
    // ================================================================
    public static class CrystallineFlail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private BlockDisplayHandle headBlock;
        private BlockDisplayHandle pivotBlock;

        public CrystallineFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_flail", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(9.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(450);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pivot point
            pivotBlock = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.POLISHED_BLACKSTONE);
            pivotBlock.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(pivotBlock.entity());

            // Chain: 5 dark prismarine links
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 5 - i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.6f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                chainLinks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Head: large amethyst mass
            headBlock = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.AMETHYST_BLOCK);
            headBlock.scale(1.2f, 1.2f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(headBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Pendulum swing: angle oscillates from -60 to +60 degrees
            float swingAngle = (float) Math.sin(ticksAlive * Math.PI / 40) * 1.05f; // ~60 degrees
            float chainLength = 6.0f;

            // Update chain links and head along the pendulum arc
            for (int i = 0; i < chainLinks.size(); i++) {
                float segmentDist = (i + 1) * (chainLength / 6);
                float x = (float) Math.sin(swingAngle) * segmentDist;
                float y = 6.0f - (float) Math.cos(swingAngle) * segmentDist;
                Location target = center.clone().add(x, y, 0);
                chainLinks.get(i).entity().teleport(target);
                chainLinks.get(i).rotate(swingAngle, 0, 0, 1);
                chainLinks.get(i).interpolation(2, 0);
            }

            // Head at end of chain
            if (headBlock != null) {
                float x = (float) Math.sin(swingAngle) * chainLength;
                float y = 6.0f - (float) Math.cos(swingAngle) * chainLength;
                Location headLoc = center.clone().add(x, y, 0);
                headBlock.entity().teleport(headLoc);
                headBlock.rotate(swingAngle * 2, 0, 0, 1);
                headBlock.interpolation(2, 0);

                // Sparks at head during fast swing (near center)
                if (Math.abs(swingAngle) < 0.2f && ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, headLoc, 5, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Chain clink sound at swing extremes
            float prevAngle = (float) Math.sin((ticksAlive - 1) * Math.PI / 40) * 1.05f;
            if ((swingAngle > 0 && prevAngle <= 0) || (swingAngle < 0 && prevAngle >= 0)) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            }

            // Trail particles from head
            if (ticksAlive % 3 == 0) {
                Location headLoc = headBlock != null ? headBlock.entity().getLocation() : center;
                DisplayBuilder.dustParticles(headLoc, 3, 0.4, 128, 0, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineFlail(plugin); }
    }

    // ================================================================
    // 8. DIMENSIONAL CALTROPS -- Scattered sharp crystal fragments
    //    that appear across the arena floor as area denial
    // ================================================================
    public static class DimensionalCaltrops extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> caltrops = new ArrayList<>();
        private final double[] caltropX = new double[12];
        private final double[] caltropZ = new double[12];

        public DimensionalCaltrops(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_caltrops", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 caltrops scattered randomly in a 9-block radius
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 4.0;
                caltropX[i] = Math.cos(angle) * dist;
                caltropZ[i] = Math.sin(angle) * dist;
                Location loc = center.clone().add(caltropX[i], -1, caltropZ[i]);
                Material mat = (i % 3 == 0) ? Material.AMETHYST_CLUSTER : Material.LARGE_AMETHYST_BUD;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                caltrops.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Caltrops emerge sequentially over 36 ticks (3 per tick-group)
            for (int i = 0; i < caltrops.size(); i++) {
                int emergeStart = i * 3;
                float yPos;

                if (ticksAlive < emergeStart) {
                    yPos = -1.0f;
                } else if (ticksAlive < emergeStart + 10) {
                    float progress = (ticksAlive - emergeStart) / 10.0f;
                    yPos = -1.0f + progress * 1.0f;
                } else {
                    yPos = 0.0f;
                    // Subtle glow pulse
                    float pulse = 0.03f * (float) Math.sin((ticksAlive + i * 8) * Math.PI / 15);
                    yPos += pulse;
                }

                Location target = center.clone().add(caltropX[i], yPos, caltropZ[i]);
                caltrops.get(i).entity().teleport(target);
                caltrops.get(i).interpolation(2, 0);

                // Emergence particle
                if (ticksAlive == emergeStart) {
                    Location pLoc = center.clone().add(caltropX[i], 0.2, caltropZ[i]);
                    DisplayBuilder.cyanDust(pLoc, 4, 0.3);
                }
            }

            // Ambient danger glow from caltrops
            if (ticksAlive % 10 == 0 && ticksAlive > 36) {
                int idx = (ticksAlive / 10) % caltrops.size();
                Location cLoc = caltrops.get(idx).entity().getLocation().add(0, 0.3, 0);
                DisplayBuilder.dustParticles(cLoc, 2, 0.15, 240, 240, 255, 0.6f);
            }

            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalCaltrops(plugin); }
    }

    // ================================================================
    // 9. CONVERGENCE BEACON -- Tall crystal tower that charges energy
    //    then releases a damaging pulse
    // ================================================================
    public static class ConvergenceBeacon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private BlockDisplayHandle crownLantern;
        private final List<BlockDisplayHandle> orbitalNodes = new ArrayList<>();

        public ConvergenceBeacon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("convergence_beacon", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tower: 6 blocks tapering upward
            Material[] mats = {Material.POLISHED_BLACKSTONE, Material.DARK_PRISMARINE,
                    Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE,
                    Material.AMETHYST_BLOCK, Material.AMETHYST_BLOCK};
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                float taper = 1.0f - (i * 0.1f);
                h.scale(taper, 1.0f, taper).glow(0, 200, 255).interpolation(3, 0);
                towerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown lantern
            crownLantern = displayBuilder.spawnBlock(center.clone().add(0, 6.5, 0), Material.SEA_LANTERN);
            crownLantern.scale(0.5f, 0.5f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(crownLantern.entity());

            // 4 orbital nodes
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 2, 4, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                orbitalNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Orbital nodes orbit and converge on a 80-tick charge cycle
            int cyclePos = ticksAlive % 80;
            float convergeFactor = cyclePos / 80.0f;
            float orbitAngle = ticksAlive * 0.05f;
            float radius = 2.0f - convergeFactor * 1.5f;

            for (int i = 0; i < orbitalNodes.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + orbitAngle;
                float yPos = 4.0f + convergeFactor * 2.5f; // Rise toward crown
                Location target = center.clone().add(Math.cos(angle) * radius, yPos, Math.sin(angle) * radius);
                orbitalNodes.get(i).entity().teleport(target);
                orbitalNodes.get(i).interpolation(2, 0);
            }

            // Crown pulse at charge completion
            if (cyclePos == 79) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.5f);
                DisplayBuilder.cyanDust(center.clone().add(0, 6.5, 0), 25, 3.0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 6.5, 0), 15, 2.0, 1.0, 2.0, 0.1);
            }

            // Crown lantern glow pulse
            if (crownLantern != null) {
                float s = 0.5f + convergeFactor * 0.5f;
                crownLantern.scale(s, s, s);
                crownLantern.interpolation(2, 0);
            }

            // Charging particles: nodes to crown
            if (ticksAlive % 4 == 0 && cyclePos > 40) {
                for (BlockDisplayHandle node : orbitalNodes) {
                    Location nodeLoc = node.entity().getLocation();
                    Location crownLoc = center.clone().add(0, 6.5, 0);
                    DisplayBuilder.particleLine(nodeLoc, crownLoc, Particle.ELECTRIC_SPARK, 2, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConvergenceBeacon(plugin); }
    }

    // ================================================================
    // 10. WARPED BATTLEMENT -- Defensive crystal wall structure with
    //     protruding amethyst spears and periodic energy discharges
    // ================================================================
    public static class WarpedBattlement extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spearBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> merlonBlocks = new ArrayList<>();

        public WarpedBattlement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("warped_battlement", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall base: 8 dark prismarine blocks in a line
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(1.0f, 2.0f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                wallBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Merlons: alternating raised sections on top of wall
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(i * 2 - 3, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.7f).glow(128, 0, 255).interpolation(3, 0);
                merlonBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Spears: 4 amethyst spears protruding forward at angle
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(i * 2 - 3, 1, 1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                h.scale(0.3f, 0.3f, 1.5f)
                        .rotate(-0.5f, 1, 0, 0) // Angled forward
                        .glow(128, 0, 255).interpolation(3, 0);
                spearBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wall slowly advances forward (toward players) and retreats
            float advance = 0.5f * (float) Math.sin(ticksAlive * Math.PI / 100);
            for (int i = 0; i < wallBlocks.size(); i++) {
                Location target = center.clone().add(i - 4, 0, advance);
                wallBlocks.get(i).entity().teleport(target);
            }
            for (int i = 0; i < merlonBlocks.size(); i++) {
                Location target = center.clone().add(i * 2 - 3, 2, advance);
                merlonBlocks.get(i).entity().teleport(target);
            }
            for (int i = 0; i < spearBlocks.size(); i++) {
                Location target = center.clone().add(i * 2 - 3, 1, 1 + advance);
                spearBlocks.get(i).entity().teleport(target);
            }

            // Energy discharge from merlons every 60 ticks
            if (ticksAlive % 60 == 0) {
                for (BlockDisplayHandle m : merlonBlocks) {
                    Location mLoc = m.entity().getLocation().add(0, 0.5, 0.5);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, mLoc, 8, 0.3, 0.3, 0.3, 0.08);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
            }

            // Ambient glow from spear tips
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle s : spearBlocks) {
                    Location sLoc = s.entity().getLocation().add(0, 0, 1.5);
                    DisplayBuilder.dustParticles(sLoc, 2, 0.2, 128, 0, 255, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WarpedBattlement(plugin); }
    }
}
