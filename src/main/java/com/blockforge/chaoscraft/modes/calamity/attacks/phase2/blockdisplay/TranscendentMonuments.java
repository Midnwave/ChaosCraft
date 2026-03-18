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
 * Phase 2 (4B) Block Display -- GROUP 10: TRANSCENDENT MONUMENTS
 * 10 attacks featuring grand crystalline structures that defy physics --
 * floating temples, inverted pyramids, impossible geometries born
 * from DoG's dimensional essence merging with the Crystalline Plague island.
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG palette: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - Damage in HP (4.0-12.0 range)
 */
public final class TranscendentMonuments {

    private TranscendentMonuments() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InvertedObelisk(plugin));
        registry.register(new CrystallineZiggurat(plugin));
        registry.register(new FloatingAltar(plugin));
        registry.register(new VoidSpire(plugin));
        registry.register(new PrismaticArch(plugin));
        registry.register(new DevourerShrine(plugin));
        registry.register(new SuspendedMegalith(plugin));
        registry.register(new CrystalCrown(plugin));
        registry.register(new DimensionalOrrery(plugin));
        registry.register(new PlagueMonolith(plugin));
    }

    // ================================================================
    // 1. INVERTED OBELISK -- Upside-down pyramid that hovers overhead
    //    with its point aimed at the ground, pulsing with energy
    // ================================================================
    public static class InvertedObelisk extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pyramidBlocks = new ArrayList<>();
        private BlockDisplayHandle tipBlock;
        private BlockDisplayHandle capBlock;

        public InvertedObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_obelisk", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pyramid body: 4 layers, widening upward (inverted)
            Material[] layerMats = {Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE,
                    Material.AMETHYST_BLOCK, Material.POLISHED_BLACKSTONE};
            float[] layerScales = {0.5f, 1.0f, 1.5f, 2.0f};

            for (int layer = 0; layer < 4; layer++) {
                Location loc = center.clone().add(0, 8 + layer * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, layerMats[layer]);
                h.scale(layerScales[layer], 1.0f, layerScales[layer])
                        .glow(0, 200, 255).interpolation(3, 0);
                pyramidBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tip: large amethyst cluster pointing downward
            tipBlock = displayBuilder.spawnBlock(center.clone().add(0, 7, 0), Material.LARGE_AMETHYST_BUD);
            tipBlock.scale(0.4f, 0.8f, 0.4f).glow(128, 0, 255)
                    .rotate((float) Math.PI, 1, 0, 0).interpolation(3, 0);
            spawnedEntities.add(tipBlock.entity());

            // Cap: sea lantern on top
            capBlock = displayBuilder.spawnBlock(center.clone().add(0, 12.8, 0), Material.SEA_LANTERN);
            capBlock.scale(1.5f, 0.4f, 1.5f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(capBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Gentle hover bob
            float yBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 40);

            // Slow rotation
            float rot = ticksAlive * 0.015f;

            for (int i = 0; i < pyramidBlocks.size(); i++) {
                Location target = center.clone().add(0, 8 + i * 1.2 + yBob, 0);
                pyramidBlocks.get(i).entity().teleport(target);
                pyramidBlocks.get(i).rotate(rot, 0, 1, 0);
                pyramidBlocks.get(i).interpolation(3, 0);
            }

            if (tipBlock != null) {
                tipBlock.entity().teleport(center.clone().add(0, 7 + yBob, 0));
            }
            if (capBlock != null) {
                capBlock.entity().teleport(center.clone().add(0, 12.8 + yBob, 0));
                capBlock.rotate(rot, 0, 1, 0);
                capBlock.interpolation(3, 0);
            }

            // Energy beam from tip downward
            if (ticksAlive % 5 == 0) {
                Location tipLoc = center.clone().add(0, 7 + yBob, 0);
                Location groundLoc = center.clone().add(0, 0.5, 0);
                DisplayBuilder.particleLine(tipLoc, groundLoc, Particle.ELECTRIC_SPARK, 2, null);
            }

            // Ambient particles from pyramid body
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 10 + yBob, 0), 5, 1.5, 128, 0, 255, 0.8f);
            }

            // Ground shadow pulse
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 2.0);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InvertedObelisk(plugin); }
    }

    // ================================================================
    // 2. CRYSTALLINE ZIGGURAT -- Step pyramid of crystal and prismarine
    //    that rises from the ground in layered stages
    // ================================================================
    public static class CrystallineZiggurat extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> layers = new ArrayList<>();

        public CrystallineZiggurat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_ziggurat", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(550);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 layers, each smaller than the last
            int[] layerSizes = {5, 4, 3, 2, 1};
            Material[] layerMats = {Material.DARK_PRISMARINE, Material.POLISHED_BLACKSTONE,
                    Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE, Material.SEA_LANTERN};

            for (int layer = 0; layer < 5; layer++) {
                List<BlockDisplayHandle> layerBlocks = new ArrayList<>();
                int size = layerSizes[layer];
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        double xOff = (x - size / 2.0) + 0.5;
                        double zOff = (z - size / 2.0) + 0.5;
                        Location loc = center.clone().add(xOff, -5 + layer, zOff);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, layerMats[layer]);
                        h.scale(0.9f, 0.9f, 0.9f).glow(0, 200, 255).interpolation(3, 0);
                        layerBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                layers.add(layerBlocks);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each layer rises in sequence, 15 ticks apart
            int[] layerSizes = {5, 4, 3, 2, 1};

            for (int layer = 0; layer < layers.size(); layer++) {
                int riseStart = layer * 15;
                float yPos;

                if (ticksAlive < riseStart) {
                    yPos = -5 + layer;
                } else if (ticksAlive < riseStart + 25) {
                    float progress = (ticksAlive - riseStart) / 25.0f;
                    yPos = -5 + layer + progress * 5.0f;
                } else {
                    yPos = layer;
                    float tremble = 0.03f * (float) Math.sin((ticksAlive + layer * 10) * 1.5);
                    yPos += tremble;
                }

                int size = layerSizes[layer];
                int idx = 0;
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        if (idx < layers.get(layer).size()) {
                            double xOff = (x - size / 2.0) + 0.5;
                            double zOff = (z - size / 2.0) + 0.5;
                            Location target = center.clone().add(xOff, yPos, zOff);
                            layers.get(layer).get(idx).entity().teleport(target);
                            layers.get(layer).get(idx).interpolation(3, 0);
                            idx++;
                        }
                    }
                }

                // Rise sound
                if (ticksAlive == riseStart) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.6f + layer * 0.15f);
                    DisplayBuilder.cyanDust(center.clone().add(0, yPos + 0.5, 0), 10, 2.0);
                }
            }

            // Top beacon glow
            if (ticksAlive > 75 && ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 5, 0.5, 240, 240, 255, 1.0f);
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 5.5, 0), 2, 0.2, 0.3, 0.2, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineZiggurat(plugin); }
    }

    // ================================================================
    // 3. FLOATING ALTAR -- Levitating platform of crystal with
    //    ascending column of energy above it
    // ================================================================
    public static class FloatingAltar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> platformBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private BlockDisplayHandle altarSurface;

        public FloatingAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_altar", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Platform base: 3x3 of dark prismarine
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 3, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.95f, 0.4f, 0.95f).glow(0, 200, 255).interpolation(3, 0);
                    platformBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Altar surface: budding amethyst centerpiece
            altarSurface = displayBuilder.spawnBlock(center.clone().add(0, 3.4, 0), Material.BUDDING_AMETHYST);
            altarSurface.scale(0.8f, 0.3f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(altarSurface.entity());

            // Energy pillar: 4 amethyst blocks ascending from altar
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 4 + i * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                float taper = 0.6f - (i * 0.1f);
                h.scale(taper, 1.0f, taper).glow(128, 0, 255).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Platform hovers with gentle bob
            float yBob = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 35);

            for (int i = 0; i < platformBlocks.size(); i++) {
                int x = (i % 3) - 1;
                int z = (i / 3) - 1;
                Location target = center.clone().add(x, 3 + yBob, z);
                platformBlocks.get(i).entity().teleport(target);
            }

            if (altarSurface != null) {
                altarSurface.entity().teleport(center.clone().add(0, 3.4 + yBob, 0));
                altarSurface.rotate(ticksAlive * 0.01f, 0, 1, 0);
                altarSurface.interpolation(3, 0);
            }

            // Pillar blocks ascend and pulse
            for (int i = 0; i < pillarBlocks.size(); i++) {
                float yOff = 4 + i * 1.5f + yBob;
                float pulse = 0.1f * (float) Math.sin((ticksAlive + i * 15) * Math.PI / 20);
                Location target = center.clone().add(0, yOff + pulse, 0);
                pillarBlocks.get(i).entity().teleport(target);

                float taper = 0.6f - (i * 0.1f) + pulse * 0.3f;
                pillarBlocks.get(i).scale(taper, 1.0f, taper);
                pillarBlocks.get(i).interpolation(3, 0);
            }

            // Energy beam upward from altar
            if (ticksAlive % 6 == 0) {
                Location altarTop = center.clone().add(0, 3.7 + yBob, 0);
                Location beamTop = center.clone().add(0, 10 + yBob, 0);
                DisplayBuilder.particleLine(altarTop, beamTop, Particle.END_ROD, 2, null);
            }

            // Platform shadow particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 1.5);
            }

            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingAltar(plugin); }
    }

    // ================================================================
    // 4. VOID SPIRE -- Impossibly thin tower of crystal that pierces
    //    upward from the ground, crackling with dimensional static
    // ================================================================
    public static class VoidSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();
        private BlockDisplayHandle antennaTip;

        public VoidSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_spire", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spire: 10 blocks tapering sharply, starting underground
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, -10 + i, 0);
                Material mat;
                if (i < 3) mat = Material.POLISHED_BLACKSTONE;
                else if (i < 7) mat = Material.AMETHYST_BLOCK;
                else mat = Material.AMETHYST_CLUSTER;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 0.8f - (i * 0.06f);
                h.scale(taper, 1.0f, taper).glow(0, 200, 255).interpolation(3, 0);
                spireBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Antenna tip
            antennaTip = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.END_ROD);
            antennaTip.scale(0.1f, 1.5f, 0.1f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(antennaTip.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spire rises over 50 ticks
            float riseOffset;
            if (ticksAlive < 50) {
                riseOffset = (ticksAlive / 50.0f) * 10.0f;
            } else {
                riseOffset = 10.0f;
            }

            for (int i = 0; i < spireBlocks.size(); i++) {
                Location target = center.clone().add(0, -10 + i + riseOffset, 0);
                spireBlocks.get(i).entity().teleport(target);
            }

            if (antennaTip != null) {
                antennaTip.entity().teleport(center.clone().add(0, riseOffset + 0.5, 0));
            }

            // Dimensional static: random spark bursts along the spire
            if (ticksAlive > 50 && ticksAlive % 4 == 0) {
                int sparkIdx = (int)(Math.random() * spireBlocks.size());
                Location sparkLoc = spireBlocks.get(sparkIdx).entity().getLocation();
                w.spawnParticle(Particle.ELECTRIC_SPARK, sparkLoc, 4, 0.2, 0.3, 0.2, 0.04);
            }

            // Sway in the wind
            if (ticksAlive > 50) {
                float sway = 0.15f * (float) Math.sin(ticksAlive * Math.PI / 40);
                for (int i = 0; i < spireBlocks.size(); i++) {
                    float localSway = sway * (i / 10.0f); // More sway at top
                    Location target = center.clone().add(localSway, i + riseOffset - 10, 0);
                    spireBlocks.get(i).entity().teleport(target);
                }
            }

            // Beacon glow from tip
            if (ticksAlive % 8 == 0 && ticksAlive > 50) {
                Location tipLoc = antennaTip != null ? antennaTip.entity().getLocation() : center;
                DisplayBuilder.dustParticles(tipLoc.clone().add(0, 1, 0), 3, 0.3, 240, 240, 255, 1.0f);
            }

            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSpire(plugin); }
    }

    // ================================================================
    // 5. PRISMATIC ARCH -- Grand archway of alternating crystal and
    //    prismarine that frames a dimensional portal effect
    // ================================================================
    public static class PrismaticArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> portalFill = new ArrayList<>();

        public PrismaticArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prismatic_arch", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arch: 12 blocks in a semicircular arc
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * i / 11; // 0 to PI
                double x = Math.cos(angle) * 3;
                double y = Math.sin(angle) * 4;
                Location loc = center.clone().add(x, y, 0);
                Material mat = (i % 3 == 0) ? Material.DARK_PRISMARINE :
                        (i % 3 == 1) ? Material.AMETHYST_BLOCK : Material.PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Portal fill: cyan stained glass inside the arch
            for (int y = 0; y < 3; y++) {
                for (int x = -1; x <= 1; x++) {
                    Location loc = center.clone().add(x, 0.5 + y * 1.2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(0.9f, 1.0f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                    portalFill.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arch blocks shimmer with traveling glow
            int glowIdx = (ticksAlive / 5) % archBlocks.size();
            for (int i = 0; i < archBlocks.size(); i++) {
                if (i == glowIdx || i == (glowIdx + 1) % archBlocks.size()) {
                    archBlocks.get(i).glow(240, 240, 255);
                } else {
                    archBlocks.get(i).glow(0, 200, 255);
                }
            }

            // Portal fill pulses
            for (int i = 0; i < portalFill.size(); i++) {
                float pulse = 0.15f + 0.05f * (float) Math.sin((ticksAlive + i * 10) * Math.PI / 15);
                portalFill.get(i).scale(0.9f, 1.0f, pulse);
                portalFill.get(i).interpolation(2, 0);
            }

            // Portal particles
            if (ticksAlive % 3 == 0) {
                Location portalCenter = center.clone().add(0, 2, 0);
                w.spawnParticle(Particle.PORTAL, portalCenter, 5, 1.0, 1.5, 0.1, 0.1);
            }

            // Arch tip glow
            if (ticksAlive % 8 == 0) {
                double topAngle = Math.PI / 2;
                Location topLoc = center.clone().add(Math.cos(topAngle) * 3, Math.sin(topAngle) * 4, 0);
                DisplayBuilder.dustParticles(topLoc, 3, 0.3, 128, 0, 255, 1.0f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismaticArch(plugin); }
    }

    // ================================================================
    // 6. DEVOURER SHRINE -- Altar-like structure shaped in tribute to
    //    the Devourer, with a crystalline serpent motif
    // ================================================================
    public static class DevourerShrine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> serpentCoil = new ArrayList<>();
        private BlockDisplayHandle altarCore;

        public DevourerShrine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devourer_shrine", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(370);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base: 3x3 polished blackstone platform
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.95f, 0.5f, 0.95f).glow(0, 200, 255).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Altar core: budding amethyst on pedestal
            altarCore = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.BUDDING_AMETHYST);
            altarCore.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(altarCore.entity());

            // Serpent coil: 8 amethyst blocks in a helix around the altar
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double radius = 1.5;
                double yOff = 0.5 + i * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * radius, yOff, Math.sin(angle) * radius);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.4f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                serpentCoil.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Serpent coil rotates around the altar
            float rotOffset = ticksAlive * 0.02f;
            for (int i = 0; i < serpentCoil.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + rotOffset;
                double yOff = 0.5 + i * 0.5;
                float bob = 0.1f * (float) Math.sin((ticksAlive + i * 10) * Math.PI / 20);
                Location target = center.clone().add(Math.cos(angle) * 1.5, yOff + bob, Math.sin(angle) * 1.5);
                serpentCoil.get(i).entity().teleport(target);
                serpentCoil.get(i).interpolation(3, 0);
            }

            // Altar core rotation and pulse
            if (altarCore != null) {
                altarCore.rotate(ticksAlive * 0.03f, 0, 1, 0);
                float s = 0.8f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 20);
                altarCore.scale(s, s, s);
                altarCore.interpolation(3, 0);
            }

            // Worship energy: particles from coil to altar
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % serpentCoil.size();
                Location coilLoc = serpentCoil.get(idx).entity().getLocation();
                Location coreLoc = center.clone().add(0, 0.8, 0);
                DisplayBuilder.particleLine(coilLoc, coreLoc, Particle.DUST, 2,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 0.6f));
            }

            // Ambient glow from base
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 5, 1.5);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DevourerShrine(plugin); }
    }

    // ================================================================
    // 7. SUSPENDED MEGALITH -- Massive rectangular slab of crystal
    //    floating horizontally, slowly tilting and rotating
    // ================================================================
    public static class SuspendedMegalith extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> slabBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> supportChains = new ArrayList<>();

        public SuspendedMegalith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("suspended_megalith", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(9.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main slab: 5x3 blocks, 1 thick
            Material[] slabMats = {Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE,
                    Material.POLISHED_BLACKSTONE};
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 6, z);
                    Material mat = slabMats[(Math.abs(x) + Math.abs(z)) % 3];
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.95f, 0.5f, 0.95f).glow(0, 200, 255).interpolation(3, 0);
                    slabBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Support chains: 4 vertical end rod lines at corners
            int[][] corners = {{-2, -1}, {2, -1}, {-2, 1}, {2, 1}};
            for (int[] corner : corners) {
                Location loc = center.clone().add(corner[0], 8, corner[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.15f, 3.0f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                supportChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Slab tilts gently
            float tiltX = 0.08f * (float) Math.sin(ticksAlive * Math.PI / 60);
            float tiltZ = 0.06f * (float) Math.cos(ticksAlive * Math.PI / 45);
            float yBob = 0.3f * (float) Math.sin(ticksAlive * Math.PI / 35);

            // Slow Y rotation
            float yRot = ticksAlive * 0.005f;

            int idx = 0;
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (idx < slabBlocks.size()) {
                        float localY = 6 + yBob + x * tiltX + z * tiltZ;
                        Location target = center.clone().add(x, localY, z);
                        slabBlocks.get(idx).entity().teleport(target);
                        slabBlocks.get(idx).interpolation(3, 0);
                        idx++;
                    }
                }
            }

            // Support chains follow corners
            int[][] corners = {{-2, -1}, {2, -1}, {-2, 1}, {2, 1}};
            for (int i = 0; i < supportChains.size(); i++) {
                float cornerY = 6 + yBob + corners[i][0] * tiltX + corners[i][1] * tiltZ;
                Location target = center.clone().add(corners[i][0], cornerY + 2, corners[i][1]);
                supportChains.get(i).entity().teleport(target);
            }

            // Gravitational particles below the slab
            if (ticksAlive % 6 == 0) {
                Location below = center.clone().add(0, 5 + yBob, 0);
                DisplayBuilder.dustParticles(below, 5, 2.0, 0, 200, 255, 0.8f);
            }

            // Tilt creak sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.5f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SuspendedMegalith(plugin); }
    }

    // ================================================================
    // 8. CRYSTAL CROWN -- Hovering crown formation above the altar
    //    area, marking DoG's dominion over the arena
    // ================================================================
    public static class CrystalCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bandBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> peakBlocks = new ArrayList<>();
        private BlockDisplayHandle centralGem;

        public CrystalCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_crown", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crown band: 12 dark prismarine in a ring
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 5, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.6f, 0.4f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                bandBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown peaks: 6 amethyst spires at every other position
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 5.5, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LARGE_AMETHYST_BUD);
                float height = 1.0f + (i % 2) * 0.5f;
                h.scale(0.3f, height, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                peakBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central gem: sea lantern
            centralGem = displayBuilder.spawnBlock(center.clone().add(0, 5.5, 0), Material.SEA_LANTERN);
            centralGem.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(centralGem.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crown rotates slowly
            float rotOffset = ticksAlive * 0.012f;
            float yBob = 0.25f * (float) Math.sin(ticksAlive * Math.PI / 40);

            for (int i = 0; i < bandBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + rotOffset;
                Location target = center.clone().add(Math.cos(angle) * 2.5, 5 + yBob, Math.sin(angle) * 2.5);
                bandBlocks.get(i).entity().teleport(target);
                bandBlocks.get(i).interpolation(2, 0);
            }

            for (int i = 0; i < peakBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 + rotOffset;
                float peakBob = 0.1f * (float) Math.sin((ticksAlive + i * 12) * Math.PI / 20);
                Location target = center.clone().add(Math.cos(angle) * 2.5, 5.5 + yBob + peakBob, Math.sin(angle) * 2.5);
                peakBlocks.get(i).entity().teleport(target);
                peakBlocks.get(i).interpolation(2, 0);
            }

            // Central gem counter-rotates and pulses
            if (centralGem != null) {
                centralGem.entity().teleport(center.clone().add(0, 5.5 + yBob, 0));
                centralGem.rotate(-rotOffset * 2, 0, 1, 0);
                float s = 0.6f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 15);
                centralGem.scale(s, s, s);
                centralGem.interpolation(2, 0);
            }

            // Beam from each peak to central gem
            if (ticksAlive % 8 == 0) {
                int beamIdx = (ticksAlive / 8) % peakBlocks.size();
                Location peakLoc = peakBlocks.get(beamIdx).entity().getLocation();
                Location gemLoc = center.clone().add(0, 5.5 + yBob, 0);
                DisplayBuilder.particleLine(peakLoc, gemLoc, Particle.ELECTRIC_SPARK, 2, null);
            }

            // Royal glow below crown
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 4.5 + yBob, 0), 6, 2.0, 128, 0, 255, 0.8f);
            }

            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalCrown(plugin); }
    }

    // ================================================================
    // 9. DIMENSIONAL ORRERY -- Mechanical model of planetary orbits
    //    made from crystal, representing the dimensions DoG devours
    // ================================================================
    public static class DimensionalOrrery extends BlockDisplayAttack {

        private BlockDisplayHandle centralStar;
        private final List<BlockDisplayHandle> orbit1 = new ArrayList<>();
        private final List<BlockDisplayHandle> orbit2 = new ArrayList<>();
        private final List<BlockDisplayHandle> orbit3 = new ArrayList<>();
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();

        public DimensionalOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_orrery", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central star: sea lantern
            centralStar = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.SEA_LANTERN);
            centralStar.scale(0.8f, 0.8f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(centralStar.entity());

            // Orbit 1: 3 amethyst blocks at 1.5 radius
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 5, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                orbit1.add(h);
                spawnedEntities.add(h.entity());
            }

            // Orbit 2: 4 dark prismarine at 3.0 radius
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 3, 5, Math.sin(angle) * 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                orbit2.add(h);
                spawnedEntities.add(h.entity());
            }

            // Orbit 3: 5 polished blackstone at 4.5 radius
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 4.5, 5, Math.sin(angle) * 4.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                orbit3.add(h);
                spawnedEntities.add(h.entity());
            }

            // Connecting arms: 4 end rod displays
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 2.25, 5, Math.sin(angle) * 2.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_ROD);
                h.scale(0.1f, 0.1f, 1.5f).glow(240, 240, 255)
                        .rotate((float) angle, 0, 1, 0).interpolation(2, 0);
                armBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each orbit rotates at different speeds
            float rot1 = ticksAlive * 0.04f;
            float rot2 = -ticksAlive * 0.025f;
            float rot3 = ticksAlive * 0.015f;

            for (int i = 0; i < orbit1.size(); i++) {
                double angle = (Math.PI * 2 * i) / 3 + rot1;
                Location target = center.clone().add(Math.cos(angle) * 1.5, 5, Math.sin(angle) * 1.5);
                orbit1.get(i).entity().teleport(target);
            }

            for (int i = 0; i < orbit2.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + rot2;
                float yBob = 0.2f * (float) Math.sin((ticksAlive + i * 15) * Math.PI / 25);
                Location target = center.clone().add(Math.cos(angle) * 3, 5 + yBob, Math.sin(angle) * 3);
                orbit2.get(i).entity().teleport(target);
            }

            for (int i = 0; i < orbit3.size(); i++) {
                double angle = (Math.PI * 2 * i) / 5 + rot3;
                float yBob = 0.15f * (float) Math.sin((ticksAlive + i * 20) * Math.PI / 30);
                Location target = center.clone().add(Math.cos(angle) * 4.5, 5 + yBob, Math.sin(angle) * 4.5);
                orbit3.get(i).entity().teleport(target);
            }

            // Central star pulse
            if (centralStar != null) {
                float s = 0.8f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 20);
                centralStar.scale(s, s, s);
                centralStar.interpolation(2, 0);
            }

            // Arms rotate with orbit 1
            for (int i = 0; i < armBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + rot1;
                Location target = center.clone().add(Math.cos(angle) * 2.25, 5, Math.sin(angle) * 2.25);
                armBlocks.get(i).entity().teleport(target);
                armBlocks.get(i).rotate((float) angle, 0, 1, 0);
                armBlocks.get(i).interpolation(2, 0);
            }

            // Orbital trail particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 5, 0), 1.5, Particle.DUST, 6,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 0.5f));
                DisplayBuilder.particleRing(center.clone().add(0, 5, 0), 3.0, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 255), 0.5f));
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalOrrery(plugin); }
    }

    // ================================================================
    // 10. PLAGUE MONOLITH -- The ultimate monument: a towering slab of
    //     crystalline plague material that serves as a focal point for
    //     DoG's dimensional presence, radiating area-wide energy
    // ================================================================
    public static class PlagueMonolith extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> monolithBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> runeBlocks = new ArrayList<>();
        private BlockDisplayHandle crownLantern;

        public PlagueMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_monolith", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base: 5x5 platform of polished blackstone
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue; // Skip corners
                    Location loc = center.clone().add(x, -0.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.95f, 0.3f, 0.95f).glow(0, 200, 255).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Monolith body: 8 blocks tall, 2 wide
            Material[] bodyMats = {Material.DARK_PRISMARINE, Material.AMETHYST_BLOCK,
                    Material.DARK_PRISMARINE, Material.AMETHYST_BLOCK,
                    Material.POLISHED_BLACKSTONE, Material.AMETHYST_BLOCK,
                    Material.DARK_PRISMARINE, Material.AMETHYST_BLOCK};
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, -8 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, bodyMats[i]);
                float width = 1.5f - (i * 0.05f);
                h.scale(width, 1.0f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                monolithBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Rune panels: 4 cyan stained glass on the faces
            double[][] runeOffsets = {{0.8, 2, 0}, {-0.8, 3, 0}, {0, 4, 0.35}, {0, 5, -0.35}};
            for (double[] off : runeOffsets) {
                Location loc = center.clone().add(off[0], -8 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.3f, 0.6f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                runeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown lantern at apex
            crownLantern = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.SEA_LANTERN);
            crownLantern.scale(0.8f, 0.4f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(crownLantern.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Monolith rises from the ground over 60 ticks
            float riseOffset;
            if (ticksAlive < 60) {
                riseOffset = (ticksAlive / 60.0f) * 8.0f;
            } else {
                riseOffset = 8.0f;
            }

            for (int i = 0; i < monolithBlocks.size(); i++) {
                Location target = center.clone().add(0, -8 + i + riseOffset, 0);
                monolithBlocks.get(i).entity().teleport(target);
            }

            // Rune panels follow the monolith
            double[][] runeOffsets = {{0.8, 2, 0}, {-0.8, 3, 0}, {0, 4, 0.35}, {0, 5, -0.35}};
            for (int i = 0; i < runeBlocks.size(); i++) {
                Location target = center.clone().add(
                        runeOffsets[i][0], -8 + runeOffsets[i][1] + riseOffset, runeOffsets[i][2]);
                runeBlocks.get(i).entity().teleport(target);

                // Rune glow pulse
                float pulse = 0.3f + 0.15f * (float) Math.sin((ticksAlive + i * 20) * Math.PI / 15);
                runeBlocks.get(i).scale(pulse, 0.6f, 0.15f);
                runeBlocks.get(i).interpolation(2, 0);
            }

            // Crown lantern at the top
            if (crownLantern != null) {
                crownLantern.entity().teleport(center.clone().add(0, 0.5 + riseOffset, 0));
                float s = 0.8f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 15);
                crownLantern.scale(s, 0.4f, s);
                crownLantern.interpolation(2, 0);
            }

            // Rise rumble
            if (ticksAlive < 60 && ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.4f);
                DisplayBuilder.cyanDust(center.clone().add(0, riseOffset * 0.5, 0), 10, 1.5);
            }

            // Area energy pulse from crown every 40 ticks after risen
            if (ticksAlive > 60 && ticksAlive % 40 == 0) {
                Location crownLoc = center.clone().add(0, 8.5, 0);
                DisplayBuilder.playSound(crownLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
                w.spawnParticle(Particle.ELECTRIC_SPARK, crownLoc, 20, 3.0, 1.0, 3.0, 0.08);
                DisplayBuilder.cyanDust(crownLoc, 20, 4.0);
            }

            // Vertical energy beam from crown upward
            if (ticksAlive > 60 && ticksAlive % 6 == 0) {
                Location crownLoc = center.clone().add(0, riseOffset + 0.5, 0);
                Location skyLoc = crownLoc.clone().add(0, 8, 0);
                DisplayBuilder.particleLine(crownLoc, skyLoc, Particle.END_ROD, 2, null);
            }

            // Ambient violet aura from monolith body
            if (ticksAlive % 8 == 0 && ticksAlive > 60) {
                int idx = (ticksAlive / 8) % monolithBlocks.size();
                Location mLoc = monolithBlocks.get(idx).entity().getLocation();
                DisplayBuilder.dustParticles(mLoc, 4, 0.5, 128, 0, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PlagueMonolith(plugin); }
    }
}
