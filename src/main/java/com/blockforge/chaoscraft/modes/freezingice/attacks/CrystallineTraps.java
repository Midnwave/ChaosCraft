package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Freezing Ice Block Display — GROUP: CRYSTALLINE TRAPS
 * 13 trap-themed BlockDisplay attacks: enclosures, snares, cages.
 * ICE palette: BLUE_ICE, PACKED_ICE, ICE, SNOW_BLOCK, LIGHT_BLUE_STAINED_GLASS, WHITE_CONCRETE.
 * Particles: SNOWFLAKE, END_ROD, DUST RGB(100,180,255).
 * NO status effects. Min 10 BlockDisplays per attack. Damage radius 5-8.
 */
public final class CrystallineTraps {
    private CrystallineTraps() {}

    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f);

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceCage(plugin));
        registry.register(new FrostEncasement(plugin));
        registry.register(new CrystalSnare(plugin));
        registry.register(new IceMine(plugin));
        registry.register(new FrostWeb(plugin));
        registry.register(new GlacialPrison(plugin));
        registry.register(new IceFloor(plugin));
        registry.register(new FrostChain(plugin));
        registry.register(new CrystalMaze(plugin));
        registry.register(new IceTrapDoor(plugin));
        registry.register(new FrostRing(plugin));
        registry.register(new GlacialVice(plugin));
        registry.register(new IceCocoon(plugin));
    }

    // ================================================================
    // 1. ICE CAGE — 16 vertical ice bars rising from ground + ceiling
    // ================================================================
    public static class IceCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private BlockDisplayHandle ceiling;
        private final List<Double> barHeights = new ArrayList<>();

        public IceCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 3.0;
            // 16 vertical bars in a circle
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Material mat = (i % 3 == 0) ? Material.BLUE_ICE : (i % 3 == 1) ? Material.PACKED_ICE : Material.ICE;
                BlockDisplayHandle bar = displayBuilder.spawnBlock(center.clone().add(x, -1.0, z), mat);
                bar.scale(0.3f, 0.1f, 0.3f).glow(100, 180, 255).interpolation(5, 0);
                bars.add(bar);
                barHeights.add(0.0);
                spawnedEntities.add(bar.entity());
            }

            // Ceiling slab (hidden below ground initially)
            ceiling = displayBuilder.spawnBlock(center.clone().add(0, -1.0, 0), Material.BLUE_ICE);
            ceiling.scale(6.0f, 0.3f, 6.0f).glow(100, 180, 255).interpolation(5, 0);
            spawnedEntities.add(ceiling.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double radius = 3.0;
            double maxHeight = 4.0;
            double growSpeed = 0.12;

            // Bars rise from ground
            for (int i = 0; i < bars.size(); i++) {
                double h = barHeights.get(i);
                if (h < maxHeight) {
                    h = Math.min(h + growSpeed, maxHeight);
                    barHeights.set(i, h);
                }
                double angle = (Math.PI * 2 * i) / 16;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                bars.get(i).entity().teleport(c.clone().add(x, 0, z));
                bars.get(i).scale(0.3f, (float) h, 0.3f);
            }

            // Ceiling appears once bars are tall enough
            if (barHeights.get(0) >= maxHeight * 0.8) {
                ceiling.entity().teleport(c.clone().add(0, maxHeight, 0));
            }

            // Snowflake particles around the cage
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 1.5, 0), radius + 0.5, Particle.SNOWFLAKE, 12, null);
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 6, 2.0, 100, 180, 255, 1.2f);
            }

            // End rod particles at bar tips
            if (tick % 8 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 * i * 4) / 16;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(x, barHeights.get(i * 4), z), 3, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Damage sound pulse
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceCage(plugin); }
    }

    // ================================================================
    // 2. FROST ENCASEMENT — 12 panels closing from all 6 sides
    // ================================================================
    public static class FrostEncasement extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<double[]> panelTargets = new ArrayList<>();
        private final List<double[]> panelStarts = new ArrayList<>();

        public FrostEncasement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_encasement", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double startDist = 6.0;
            double endDist = 1.5;
            // 6 directions: +X, -X, +Y, -Y, +Z, -Z — 2 panels per direction
            double[][] dirs = {
                {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}
            };
            Material[] mats = {Material.PACKED_ICE, Material.BLUE_ICE, Material.LIGHT_BLUE_STAINED_GLASS,
                               Material.ICE, Material.SNOW_BLOCK, Material.WHITE_CONCRETE};

            for (int d = 0; d < 6; d++) {
                for (int sub = 0; sub < 2; sub++) {
                    double offsetPerp = (sub == 0) ? -0.8 : 0.8;
                    double sx = dirs[d][0] * startDist + (dirs[d][0] == 0 ? offsetPerp : 0);
                    double sy = dirs[d][1] * startDist + (dirs[d][1] == 0 && dirs[d][0] != 0 ? offsetPerp : 0) + 1.5;
                    double sz = dirs[d][2] * startDist + (dirs[d][2] == 0 && dirs[d][0] == 0 ? offsetPerp : 0);
                    double tx = dirs[d][0] * endDist + (dirs[d][0] == 0 ? offsetPerp * 0.3 : 0);
                    double ty = dirs[d][1] * endDist + (dirs[d][1] == 0 && dirs[d][0] != 0 ? offsetPerp * 0.3 : 0) + 1.5;
                    double tz = dirs[d][2] * endDist + (dirs[d][2] == 0 && dirs[d][0] == 0 ? offsetPerp * 0.3 : 0);

                    BlockDisplayHandle panel = displayBuilder.spawnBlock(center.clone().add(sx, sy, sz), mats[d]);
                    float scaleX = (dirs[d][0] != 0) ? 0.3f : 1.8f;
                    float scaleY = (dirs[d][1] != 0) ? 0.3f : 1.8f;
                    float scaleZ = (dirs[d][2] != 0) ? 0.3f : 1.8f;
                    panel.scale(scaleX, scaleY, scaleZ).glow(100, 180, 255).interpolation(5, 0);
                    panels.add(panel);
                    panelStarts.add(new double[]{sx, sy, sz});
                    panelTargets.add(new double[]{tx, ty, tz});
                    spawnedEntities.add(panel.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double closeDuration = 60.0;
            double t = Math.min(tick / closeDuration, 1.0);
            double ease = t * t; // Ease-in (accelerating close)

            for (int i = 0; i < panels.size(); i++) {
                double[] s = panelStarts.get(i);
                double[] e = panelTargets.get(i);
                double x = s[0] + (e[0] - s[0]) * ease;
                double y = s[1] + (e[1] - s[1]) * ease;
                double z = s[2] + (e[2] - s[2]) * ease;
                panels.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Frost particles while closing
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 10, 3.0, 100, 180, 255, 1.2f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 6, 2.0, 1.5, 2.0, 0.01);
            }

            // Creaking sound as panels close
            if (tick % 20 == 0 && tick < 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.7f, 0.4f);
            }

            // Impact sound on close
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 25, 2.0, 100, 180, 255, 2.0f);
            }

            // Ambient particles after closed
            if (tick > 60 && tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2.0, 0), 3, 1.0, 1.0, 1.0, 0.01);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostEncasement(plugin); }
    }

    // ================================================================
    // 3. CRYSTAL SNARE — 12 ring blocks + 6 pillars growing inward
    // ================================================================
    public static class CrystalSnare extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private double ringRadius = 5.0;

        public CrystalSnare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_snare", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 ring blocks on the ground
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, 0, z), mat);
                block.scale(0.8f, 0.4f, 0.8f).glow(100, 180, 255).interpolation(5, 0);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // 6 pillars at every other ring position
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                Material mat = (i % 2 == 0) ? Material.LIGHT_BLUE_STAINED_GLASS : Material.ICE;
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(center.clone().add(x, 0, z), mat);
                pillar.scale(0.4f, 0.2f, 0.4f).glow(100, 180, 255).interpolation(5, 0);
                pillars.add(pillar);
                spawnedEntities.add(pillar.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Shrink ring inward
            if (ringRadius > 1.0) {
                ringRadius -= 0.04;
            }

            // Update ring positions
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                ringBlocks.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            // Pillars grow taller and move inward
            double pillarHeight = Math.min(tick * 0.06, 3.5);
            for (int i = 0; i < pillars.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                pillars.get(i).entity().teleport(c.clone().add(x, 0, z));
                pillars.get(i).scale(0.4f, (float) pillarHeight, 0.4f);
            }

            // Particles
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), ringRadius, Particle.SNOWFLAKE, 10, null);
                DisplayBuilder.dustParticles(c.clone().add(0, pillarHeight * 0.5, 0), 5, ringRadius * 0.5, 100, 180, 255, 1.0f);
            }

            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, pillarHeight, 0), 2, ringRadius * 0.3, 0.2, ringRadius * 0.3, 0.01);
            }

            // Snap sound when snare tightens
            if (tick == 100) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalSnare(plugin); }
    }

    // ================================================================
    // 4. ICE MINE — 1 disguised block, erupts into 8 spikes on proximity
    // ================================================================
    public static class IceMine extends BlockDisplayAttack {
        private BlockDisplayHandle disguise;
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private boolean detonated = false;
        private int detonationTick = -1;

        public IceMine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mine", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Disguised snow block flush with ground
            disguise = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.SNOW_BLOCK);
            disguise.scale(0.9f, 0.2f, 0.9f).glow(100, 180, 255).interpolation(3, 0);
            spawnedEntities.add(disguise.entity());

            // Pre-spawn 8 spikes hidden below ground
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 1.8;
                double z = Math.sin(angle) * 1.8;
                Material mat = (i % 3 == 0) ? Material.BLUE_ICE : (i % 3 == 1) ? Material.PACKED_ICE : Material.ICE;
                BlockDisplayHandle spike = displayBuilder.spawnBlock(center.clone().add(x, -3.0, z), mat);
                spike.scale(0.4f, 0.1f, 0.4f).glow(100, 180, 255).interpolation(5, 0);
                spikes.add(spike);
                spawnedEntities.add(spike.entity());
            }

            // Two extra inner blocks for 10+ total
            BlockDisplayHandle inner1 = displayBuilder.spawnBlock(center.clone().add(0, -3.0, 0), Material.WHITE_CONCRETE);
            inner1.scale(0.6f, 0.1f, 0.6f).glow(100, 180, 255).interpolation(5, 0);
            spikes.add(inner1);
            spawnedEntities.add(inner1.entity());

            BlockDisplayHandle inner2 = displayBuilder.spawnBlock(center.clone().add(0, -3.0, 0), Material.LIGHT_BLUE_STAINED_GLASS);
            inner2.scale(1.0f, 0.1f, 1.0f).glow(100, 180, 255).interpolation(5, 0);
            spikes.add(inner2);
            spawnedEntities.add(inner2.entity());
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Proximity check — any player within 3 blocks triggers detonation
            if (!detonated) {
                // Subtle particle hint
                if (tick % 10 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.3, 0), 2, 0.3, 0.1, 0.3, 0.01);
                }

                for (Player player : c.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(c) <= 9.0) { // 3 block radius
                        detonated = true;
                        detonationTick = tick;
                        DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.8f);
                        DisplayBuilder.dustParticles(c, 30, 2.5, 100, 180, 255, 2.0f);
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 20, 2.0, 1.5, 2.0, 0.05);

                        // Hide the disguise
                        disguise.entity().teleport(c.clone().add(0, -10, 0));
                        break;
                    }
                }
                return;
            }

            // Eruption animation — spikes shoot up
            int elapsed = tick - detonationTick;
            double targetHeight = 3.5;
            double progress = Math.min(elapsed / 15.0, 1.0);
            double height = targetHeight * progress;

            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 1.8;
                double z = Math.sin(angle) * 1.8;
                spikes.get(i).entity().teleport(c.clone().add(x, 0, z));
                spikes.get(i).scale(0.4f, (float) height, 0.4f);
            }

            // Inner column
            spikes.get(8).entity().teleport(c.clone().add(0, 0, 0));
            spikes.get(8).scale(0.6f, (float)(height * 1.2), 0.6f);
            spikes.get(9).entity().teleport(c.clone().add(0, -0.1, 0));
            spikes.get(9).scale(1.0f, (float)(height * 0.3), 1.0f);

            // Ongoing particles
            if (elapsed % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double x = Math.cos(angle) * 1.8;
                    double z = Math.sin(angle) * 1.8;
                    c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(x, height, z), 1, 0.05, 0.05, 0.05, 0.01);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 5, 1.5, 100, 180, 255, 1.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceMine(plugin); }
    }

    // ================================================================
    // 5. FROST WEB — 14 thin ice blocks in lattice web pattern
    // ================================================================
    public static class FrostWeb extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> strands = new ArrayList<>();
        private final List<double[]> strandPositions = new ArrayList<>();

        public FrostWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_web", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Create a lattice web: horizontal + diagonal strands at player height
            double[][] offsets = {
                // Horizontal cross strands
                {-2.5, 1.0, 0}, {-1.0, 1.0, 0}, {1.0, 1.0, 0}, {2.5, 1.0, 0},
                {0, 1.0, -2.5}, {0, 1.0, -1.0}, {0, 1.0, 1.0}, {0, 1.0, 2.5},
                // Diagonal strands
                {-1.8, 1.5, -1.8}, {1.8, 1.5, -1.8}, {-1.8, 1.5, 1.8}, {1.8, 1.5, 1.8},
                // Upper cross ties
                {-1.5, 2.2, 1.5}, {1.5, 2.2, -1.5}
            };

            for (int i = 0; i < 14; i++) {
                Material mat;
                if (i < 4) mat = Material.LIGHT_BLUE_STAINED_GLASS;
                else if (i < 8) mat = Material.ICE;
                else if (i < 12) mat = Material.PACKED_ICE;
                else mat = Material.BLUE_ICE;

                BlockDisplayHandle strand = displayBuilder.spawnBlock(center.clone().add(offsets[i][0], offsets[i][1] + 5.0, offsets[i][2]), mat);
                // Thin horizontal bars
                float sx, sy, sz;
                if (i < 8) {
                    // Long horizontal strands
                    sx = (offsets[i][2] == 0) ? 1.5f : 0.1f;
                    sy = 0.1f;
                    sz = (offsets[i][0] == 0) ? 1.5f : 0.1f;
                } else {
                    // Diagonal cross strands
                    sx = 0.8f; sy = 0.08f; sz = 0.8f;
                }
                strand.scale(sx, sy, sz).glow(100, 180, 255).interpolation(5, 0);
                strands.add(strand);
                strandPositions.add(offsets[i]);
                spawnedEntities.add(strand.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Web descends from above onto player area
            double dropProgress = Math.min(tick / 40.0, 1.0);
            double yOffset = 5.0 * (1.0 - dropProgress);

            for (int i = 0; i < strands.size(); i++) {
                double[] pos = strandPositions.get(i);
                strands.get(i).entity().teleport(c.clone().add(pos[0], pos[1] + yOffset, pos[2]));
            }

            // Web shimmer particles
            if (tick % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    double rx = (Math.random() - 0.5) * 5.0;
                    double rz = (Math.random() - 0.5) * 5.0;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(rx, 1.2 + yOffset, rz), 1, 0.1, 0.05, 0.1, 0.01);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5 + yOffset, 0), 4, 2.5, 100, 180, 255, 0.8f);
            }

            // Sticky sound when web lands
            if (tick == 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_PLACE, 0.9f, 0.5f);
            }

            // Pulsing glow after landed
            if (tick > 40 && tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0), 2, 2.0, 0.2, 2.0, 0.01);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostWeb(plugin); }
    }

    // ================================================================
    // 6. GLACIAL PRISON — 20 thick wall blocks forming room, 3 tall, no ceiling
    // ================================================================
    public static class GlacialPrison extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<double[]> wallTargets = new ArrayList<>();

        public GlacialPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_prison", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 wall blocks: 5 per side, forming a square room 3 blocks tall
            // North wall (z = -3): 5 blocks along x from -2 to 2
            // South wall (z = 3): 5 blocks
            // East wall (x = 3): 5 blocks along z from -2 to 2
            // West wall (x = -3): 5 blocks

            double wallDist = 3.0;
            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.WHITE_CONCRETE, Material.SNOW_BLOCK};
            int idx = 0;

            // North wall
            for (int i = 0; i < 5; i++) {
                double x = -2.0 + i;
                walls.add(spawnWallBlock(center, x, 0, -wallDist, mats[idx++ % 5]));
                wallTargets.add(new double[]{x, 0, -wallDist});
            }
            // South wall
            for (int i = 0; i < 5; i++) {
                double x = -2.0 + i;
                walls.add(spawnWallBlock(center, x, 0, wallDist, mats[idx++ % 5]));
                wallTargets.add(new double[]{x, 0, wallDist});
            }
            // East wall
            for (int i = 0; i < 5; i++) {
                double z = -2.0 + i;
                walls.add(spawnWallBlock(center, wallDist, 0, z, mats[idx++ % 5]));
                wallTargets.add(new double[]{wallDist, 0, z});
            }
            // West wall
            for (int i = 0; i < 5; i++) {
                double z = -2.0 + i;
                walls.add(spawnWallBlock(center, -wallDist, 0, z, mats[idx++ % 5]));
                wallTargets.add(new double[]{-wallDist, 0, z});
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.2f, 0.4f);
        }

        private BlockDisplayHandle spawnWallBlock(Location center, double x, double y, double z, Material mat) {
            BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, y - 3.0, z), mat);
            block.scale(1.0f, 0.1f, 1.0f).glow(100, 180, 255).interpolation(5, 0);
            spawnedEntities.add(block.entity());
            return block;
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Walls rise from below ground to 3 blocks tall
            double maxHeight = 3.0;
            double riseProgress = Math.min(tick / 50.0, 1.0);
            double currentHeight = maxHeight * riseProgress;

            for (int i = 0; i < walls.size(); i++) {
                double[] target = wallTargets.get(i);
                double yBase = -3.0 + 3.0 * riseProgress;
                walls.get(i).entity().teleport(c.clone().add(target[0], yBase, target[2]));
                walls.get(i).scale(1.0f, (float) currentHeight, 1.0f);
            }

            // Particles along wall tops
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, currentHeight, 0), 3.5, Particle.SNOWFLAKE, 16, null);
                DisplayBuilder.dustParticles(c.clone().add(0, currentHeight * 0.5, 0), 6, 2.5, 100, 180, 255, 1.0f);
            }

            // End rod particles on corners
            if (tick % 8 == 0) {
                double d = 3.0;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(d, currentHeight, d), 1, 0.1, 0.1, 0.1, 0.01);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(-d, currentHeight, d), 1, 0.1, 0.1, 0.1, 0.01);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(d, currentHeight, -d), 1, 0.1, 0.1, 0.1, 0.01);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(-d, currentHeight, -d), 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Rising rumble
            if (tick == 50) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialPrison(plugin); }
    }

    // ================================================================
    // 7. ICE FLOOR — 16 floor blocks appear solid, crack, then collapse
    // ================================================================
    public static class IceFloor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final List<double[]> tilePositions = new ArrayList<>();
        private final List<Boolean> cracked = new ArrayList<>();
        private final List<Boolean> collapsed = new ArrayList<>();

        public IceFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_floor", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid of floor tiles
            int idx = 0;
            for (int gx = -2; gx < 2; gx++) {
                for (int gz = -2; gz < 2; gz++) {
                    double x = gx * 1.5 + 0.75;
                    double z = gz * 1.5 + 0.75;
                    Material mat;
                    if (idx % 4 == 0) mat = Material.BLUE_ICE;
                    else if (idx % 4 == 1) mat = Material.PACKED_ICE;
                    else if (idx % 4 == 2) mat = Material.LIGHT_BLUE_STAINED_GLASS;
                    else mat = Material.ICE;

                    BlockDisplayHandle tile = displayBuilder.spawnBlock(center.clone().add(x, 0.05, z), mat);
                    tile.scale(1.4f, 0.15f, 1.4f).glow(100, 180, 255).interpolation(3, 0);
                    tiles.add(tile);
                    tilePositions.add(new double[]{x, 0.05, z});
                    cracked.add(false);
                    collapsed.add(false);
                    spawnedEntities.add(tile.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1 (tick 0-60): Floor looks solid, subtle shimmer
            if (tick < 60) {
                if (tick % 8 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.3, 0), 3, 2.5, 0.1, 2.5, 0.01);
                }
                return;
            }

            // Phase 2 (tick 60-120): Tiles crack — gaps appear, tiles separate
            if (tick >= 60 && tick < 120) {
                int crackIndex = (tick - 60) / 4; // One crack every 4 ticks
                if (crackIndex < tiles.size() && !cracked.get(crackIndex)) {
                    cracked.set(crackIndex, true);
                    // Shrink tile to show gap
                    tiles.get(crackIndex).scale(1.1f, 0.15f, 1.1f);
                    double[] pos = tilePositions.get(crackIndex);
                    DisplayBuilder.playSound(c.clone().add(pos[0], 0, pos[2]), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
                    DisplayBuilder.dustParticles(c.clone().add(pos[0], 0.2, pos[2]), 8, 0.5, 100, 180, 255, 1.0f);
                }
            }

            // Phase 3 (tick 120+): Tiles collapse downward one by one
            if (tick >= 120) {
                int collapseIndex = (tick - 120) / 3;
                if (collapseIndex < tiles.size() && !collapsed.get(collapseIndex)) {
                    collapsed.set(collapseIndex, true);
                    double[] pos = tilePositions.get(collapseIndex);
                    DisplayBuilder.playSound(c.clone().add(pos[0], 0, pos[2]), Sound.BLOCK_STONE_BREAK, 0.6f, 0.6f);
                }

                // Animate collapsed tiles falling
                for (int i = 0; i < tiles.size(); i++) {
                    if (collapsed.get(i)) {
                        double[] pos = tilePositions.get(i);
                        double fallY = pos[1] - ((tick - 120 - i * 3) * 0.15);
                        if (fallY < -5.0) fallY = -5.0;
                        tiles.get(i).entity().teleport(c.clone().add(pos[0], fallY, pos[2]));
                    }
                }
            }

            // Ongoing frost dust
            if (tick % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 5, 2.0, 100, 180, 255, 1.0f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 0.2, 0), 2, 2.0, 0.1, 2.0, 0.01);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceFloor(plugin); }
    }

    // ================================================================
    // 8. FROST CHAIN — 12 ice chain blocks descending from sky
    // ================================================================
    public static class FrostChain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<double[]> anchorPoints = new ArrayList<>();
        private final List<Double> chainDropY = new ArrayList<>();

        public FrostChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_chain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 chain links descending in 4 chains of 3 links each
            double radius = 3.0;
            for (int chain = 0; chain < 4; chain++) {
                double angle = (Math.PI * 2 * chain) / 4;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                for (int link = 0; link < 3; link++) {
                    Material mat = (link == 0) ? Material.BLUE_ICE : (link == 1) ? Material.PACKED_ICE : Material.ICE;
                    BlockDisplayHandle chainLink = displayBuilder.spawnBlock(center.clone().add(x, 15.0 + link * 1.2, z), mat);
                    chainLink.scale(0.3f, 1.0f, 0.3f).glow(100, 180, 255).interpolation(5, 0);
                    chainLinks.add(chainLink);
                    anchorPoints.add(new double[]{x, z});
                    chainDropY.add(15.0 + link * 1.2);
                    spawnedEntities.add(chainLink.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double dropSpeed = 0.25;
            double anchorHeight = 0.5; // Where chains anchor to ground

            for (int i = 0; i < chainLinks.size(); i++) {
                double y = chainDropY.get(i);
                int chainIdx = i / 3;
                int linkIdx = i % 3;
                double targetY = anchorHeight + linkIdx * 1.2;

                if (y > targetY) {
                    y -= dropSpeed;
                    if (y < targetY) y = targetY;
                    chainDropY.set(i, y);
                }

                double[] anchor = anchorPoints.get(i);
                // Slight swing while descending
                double swing = Math.sin(tick * 0.08 + chainIdx * 1.5) * 0.2;
                chainLinks.get(i).entity().teleport(c.clone().add(anchor[0] + swing, y, anchor[1]));
            }

            // Chain rattle particles
            if (tick % 4 == 0) {
                for (int chain = 0; chain < 4; chain++) {
                    double angle = (Math.PI * 2 * chain) / 4;
                    double x = Math.cos(angle) * 3.0;
                    double z = Math.sin(angle) * 3.0;
                    double topY = chainDropY.get(chain * 3);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(x, topY + 0.5, z), 2, 0.1, 0.3, 0.1, 0.01);
                }
            }

            if (tick % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 4, 3.0, 100, 180, 255, 1.0f);
            }

            // Landing sound
            if (tick % 15 == 0 && tick < 80) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.8f + (tick / 100.0f));
            }

            // End rod at anchor points after landing
            if (tick > 60 && tick % 10 == 0) {
                for (int chain = 0; chain < 4; chain++) {
                    double angle = (Math.PI * 2 * chain) / 4;
                    double x = Math.cos(angle) * 3.0;
                    double z = Math.sin(angle) * 3.0;
                    c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(x, 0.5, z), 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostChain(plugin); }
    }

    // ================================================================
    // 9. CRYSTAL MAZE — 20 wall segments forming random maze pattern
    // ================================================================
    public static class CrystalMaze extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallSegments = new ArrayList<>();
        private final List<double[]> wallPositions = new ArrayList<>();
        private final List<Float> wallRotations = new ArrayList<>();

        public CrystalMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_maze", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 wall segments in a pseudo-random maze layout
            // Use deterministic offsets for a maze-like shape around center
            double[][] positions = {
                // Outer border segments
                {-4, 0, -4}, {-2, 0, -4}, {0, 0, -4}, {2, 0, -4},
                {4, 0, -2}, {4, 0, 0}, {4, 0, 2},
                {-4, 0, -2}, {-4, 0, 0}, {-4, 0, 2},
                {-2, 0, 4}, {0, 0, 4}, {2, 0, 4},
                // Inner maze walls
                {-2, 0, -1}, {0, 0, -2}, {2, 0, -1},
                {-1, 0, 1}, {1, 0, 2}, {-2, 0, 2}, {2, 0, 0}
            };

            float[] rotations = {
                0, 0, 0, 0,                           // North border (E-W oriented)
                (float)(Math.PI / 2), (float)(Math.PI / 2), (float)(Math.PI / 2), // East border (N-S)
                (float)(Math.PI / 2), (float)(Math.PI / 2), (float)(Math.PI / 2), // West border (N-S)
                0, 0, 0,                               // South border (E-W)
                (float)(Math.PI / 2), 0, (float)(Math.PI / 2), // Inner walls
                0, (float)(Math.PI / 2), 0, (float)(Math.PI / 2)
            };

            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.LIGHT_BLUE_STAINED_GLASS, Material.WHITE_CONCRETE};

            for (int i = 0; i < 20; i++) {
                BlockDisplayHandle wall = displayBuilder.spawnBlock(
                    center.clone().add(positions[i][0], -2.0, positions[i][2]), mats[i % 5]);
                wall.scale(2.0f, 0.1f, 0.4f).glow(100, 180, 255).interpolation(5, 0)
                    .rotate(rotations[i], 0, 1, 0);
                wallSegments.add(wall);
                wallPositions.add(positions[i]);
                wallRotations.add(rotations[i]);
                spawnedEntities.add(wall.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Walls rise from ground
            double riseProgress = Math.min(tick / 40.0, 1.0);
            double wallHeight = 2.5 * riseProgress;

            for (int i = 0; i < wallSegments.size(); i++) {
                double[] pos = wallPositions.get(i);
                double yBase = -2.0 + 2.0 * riseProgress;
                wallSegments.get(i).entity().teleport(c.clone().add(pos[0], yBase, pos[2]));
                wallSegments.get(i).scale(2.0f, (float) wallHeight, 0.4f);
            }

            // Frost particles along maze corridors
            if (tick % 5 == 0) {
                double rx = (Math.random() - 0.5) * 8.0;
                double rz = (Math.random() - 0.5) * 8.0;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(rx, 1.0, rz), 3, 0.5, 0.3, 0.5, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, wallHeight * 0.5, 0), 5, 3.0, 100, 180, 255, 1.0f);
            }

            // Eerie glow at intersections
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 0.5, 0), 2, 3.0, 0.5, 3.0, 0.01);
            }

            // Rumble as walls rise
            if (tick == 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalMaze(plugin); }
    }

    // ================================================================
    // 10. ICE TRAP DOOR — 12 platform blocks that hinge open downward
    // ================================================================
    public static class IceTrapDoor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> doorBlocks = new ArrayList<>();
        private final List<double[]> doorPositions = new ArrayList<>();
        private final List<Boolean> hingeOpened = new ArrayList<>();
        private boolean triggered = false;
        private int triggerTick = -1;

        public IceTrapDoor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_trap_door", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 platform blocks arranged as a 3x4 trapdoor platform
            int idx = 0;
            for (int gx = -2; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    double x = gx * 1.4 + 0.7;
                    double z = gz * 1.4;
                    Material mat;
                    if (idx % 4 == 0) mat = Material.PACKED_ICE;
                    else if (idx % 4 == 1) mat = Material.BLUE_ICE;
                    else if (idx % 4 == 2) mat = Material.SNOW_BLOCK;
                    else mat = Material.ICE;

                    BlockDisplayHandle door = displayBuilder.spawnBlock(center.clone().add(x, 0.1, z), mat);
                    door.scale(1.3f, 0.2f, 1.3f).glow(100, 180, 255).interpolation(5, 0);
                    doorBlocks.add(door);
                    doorPositions.add(new double[]{x, 0.1, z});
                    hingeOpened.add(false);
                    spawnedEntities.add(door.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.9f, 0.7f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Platform looks solid initially
            if (!triggered && tick < 80) {
                // Subtle frost shimmer
                if (tick % 10 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.3, 0), 3, 2.0, 0.1, 1.5, 0.01);
                }
                return;
            }

            // Trigger at tick 80
            if (!triggered && tick >= 80) {
                triggered = true;
                triggerTick = tick;
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            }

            // Doors hinge open one by one
            int elapsed = tick - triggerTick;
            int openIndex = elapsed / 4;

            for (int i = 0; i < doorBlocks.size(); i++) {
                double[] pos = doorPositions.get(i);
                if (i <= openIndex && !hingeOpened.get(i)) {
                    hingeOpened.set(i, true);
                    DisplayBuilder.playSound(c.clone().add(pos[0], 0, pos[2]), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.5f, 1.2f);
                }

                if (hingeOpened.get(i)) {
                    // Animate hinging downward — tile rotates and drops
                    int sinceTrigger = elapsed - (i * 4);
                    double rotProgress = Math.min(sinceTrigger / 10.0, 1.0);
                    double dropY = pos[1] - rotProgress * 3.0;
                    // Shrink as it opens/drops
                    float scaleY = 0.2f * (float)(1.0 - rotProgress * 0.5);
                    doorBlocks.get(i).entity().teleport(c.clone().add(pos[0], dropY, pos[2]));
                    doorBlocks.get(i).scale(1.3f, scaleY, 1.3f);

                    // Dust on open
                    if (sinceTrigger == 1) {
                        DisplayBuilder.dustParticles(c.clone().add(pos[0], 0.2, pos[2]), 8, 0.5, 100, 180, 255, 1.2f);
                    }
                }
            }

            // Ongoing particles
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, -1.0, 0), 3, 2.0, 0.5, 1.5, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0), 4, 2.0, 100, 180, 255, 1.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceTrapDoor(plugin); }
    }

    // ================================================================
    // 11. FROST RING — 16 blocks in concentric rings shrinking inward
    // ================================================================
    public static class FrostRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private double outerRadius = 7.0;
        private double innerRadius = 5.0;

        public FrostRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_ring", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 10 blocks
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * outerRadius;
                double z = Math.sin(angle) * outerRadius;
                Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, 0, z), mat);
                block.scale(1.2f, 1.5f, 1.2f).glow(100, 180, 255).interpolation(5, 0);
                outerRing.add(block);
                spawnedEntities.add(block.entity());
            }

            // Inner ring: 6 blocks
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * innerRadius;
                double z = Math.sin(angle) * innerRadius;
                Material mat = (i % 2 == 0) ? Material.LIGHT_BLUE_STAINED_GLASS : Material.ICE;
                BlockDisplayHandle block = displayBuilder.spawnBlock(center.clone().add(x, 0, z), mat);
                block.scale(1.0f, 2.0f, 1.0f).glow(100, 180, 255).interpolation(5, 0);
                innerRing.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Both rings shrink inward over time
            double shrinkRate = 0.02;
            double minOuter = 2.0;
            double minInner = 0.8;

            if (outerRadius > minOuter) outerRadius -= shrinkRate;
            if (innerRadius > minInner) innerRadius -= shrinkRate * 0.8;

            // Rotate outer ring slowly
            double rotOffset = tick * 0.01;

            for (int i = 0; i < outerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 10 + rotOffset;
                double x = Math.cos(angle) * outerRadius;
                double z = Math.sin(angle) * outerRadius;
                outerRing.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            // Rotate inner ring in opposite direction
            for (int i = 0; i < innerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6 - rotOffset * 1.5;
                double x = Math.cos(angle) * innerRadius;
                double z = Math.sin(angle) * innerRadius;
                innerRing.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            // Particle trails between rings
            if (tick % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), outerRadius, Particle.SNOWFLAKE, 12, null);
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), innerRadius, Particle.SNOWFLAKE, 8, null);
            }

            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 6, (outerRadius + innerRadius) * 0.3, 100, 180, 255, 1.2f);
            }

            // End rod sparkle at ring positions
            if (tick % 8 == 0) {
                double angle = (Math.PI * 2 * (tick / 8 % 10)) / 10 + rotOffset;
                double x = Math.cos(angle) * outerRadius;
                double z = Math.sin(angle) * outerRadius;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(x, 1.5, z), 2, 0.1, 0.2, 0.1, 0.01);
            }

            // Tightening sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.5f, 0.6f + (float)(tick / 300.0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostRing(plugin); }
    }

    // ================================================================
    // 12. GLACIAL VICE — Two slabs (10 each) closing horizontally
    // ================================================================
    public static class GlacialVice extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftSlab = new ArrayList<>();
        private final List<BlockDisplayHandle> rightSlab = new ArrayList<>();
        private double slabOffset = 8.0;

        public GlacialVice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_vice", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.WHITE_CONCRETE, Material.SNOW_BLOCK};

            // Left slab: 10 blocks in a 2x5 wall (2 wide, 5 tall... actually 5 wide 2 tall)
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    double y = row * 1.2;
                    double z = (col - 2) * 1.2;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(-slabOffset, y, z), mats[(row * 5 + col) % 5]);
                    block.scale(1.0f, 1.1f, 1.1f).glow(100, 180, 255).interpolation(5, 0);
                    leftSlab.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            // Right slab: 10 blocks mirrored
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    double y = row * 1.2;
                    double z = (col - 2) * 1.2;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(slabOffset, y, z), mats[(row * 5 + col + 2) % 5]);
                    block.scale(1.0f, 1.1f, 1.1f).glow(100, 180, 255).interpolation(5, 0);
                    rightSlab.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slabs close inward
            double closeSpeed = 0.06;
            double minOffset = 0.6;
            if (slabOffset > minOffset) {
                slabOffset -= closeSpeed;
                if (slabOffset < minOffset) slabOffset = minOffset;
            }

            // Update left slab positions
            for (int i = 0; i < leftSlab.size(); i++) {
                int row = i / 5;
                int col = i % 5;
                double y = row * 1.2;
                double z = (col - 2) * 1.2;
                leftSlab.get(i).entity().teleport(c.clone().add(-slabOffset, y, z));
            }

            // Update right slab positions
            for (int i = 0; i < rightSlab.size(); i++) {
                int row = i / 5;
                int col = i % 5;
                double y = row * 1.2;
                double z = (col - 2) * 1.2;
                rightSlab.get(i).entity().teleport(c.clone().add(slabOffset, y, z));
            }

            // Crushing particles between slabs
            if (tick % 4 == 0) {
                double particleX = slabOffset * 0.5;
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 6, particleX, 100, 180, 255, 1.2f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.8, 0), 4, particleX, 0.8, 2.0, 0.01);
            }

            // End rod sparks at slab edges
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(-slabOffset + 0.5, 1.0, 0), 2, 0.1, 0.5, 1.5, 0.01);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(slabOffset - 0.5, 1.0, 0), 2, 0.1, 0.5, 1.5, 0.01);
            }

            // Impact when slabs close
            if (slabOffset <= minOffset + closeSpeed && slabOffset >= minOffset) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 25, 1.0, 100, 180, 255, 2.0f);
            }

            // Grinding sound while closing
            if (tick % 20 == 0 && slabOffset > minOffset) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialVice(plugin); }
    }

    // ================================================================
    // 13. ICE COCOON — 14 blocks wrapping spirally around player from feet up
    // ================================================================
    public static class IceCocoon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cocoonBlocks = new ArrayList<>();
        private final List<double[]> spiralTargets = new ArrayList<>();
        private int revealedCount = 0;

        public IceCocoon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cocoon", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 blocks in a spiral from feet to head (y=0 to y=2.5)
            double spiralRadius = 1.2;
            double totalHeight = 2.5;
            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.LIGHT_BLUE_STAINED_GLASS,
                               Material.SNOW_BLOCK, Material.WHITE_CONCRETE, Material.BLUE_ICE};

            for (int i = 0; i < 14; i++) {
                double t = (double) i / 13;
                double angle = t * Math.PI * 4; // 2 full spiral rotations
                double y = t * totalHeight;
                // Radius narrows slightly toward top (cocoon shape)
                double r = spiralRadius * (1.0 - t * 0.3);
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;

                // Start blocks far away, they'll spiral in
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                    center.clone().add(x * 4, y, z * 4), mats[i % 7]);
                block.scale(0.5f, 0.4f, 0.5f).glow(100, 180, 255).interpolation(5, 0);
                cocoonBlocks.add(block);
                spiralTargets.add(new double[]{x, y, z});
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Reveal one block every 5 ticks (spiraling inward from feet up)
            if (tick % 5 == 0 && revealedCount < 14) {
                revealedCount++;
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f + revealedCount * 0.08f);
            }

            // Animate revealed blocks spiraling inward to their target position
            for (int i = 0; i < revealedCount && i < cocoonBlocks.size(); i++) {
                double[] target = spiralTargets.get(i);
                int revealTick = i * 5;
                int sinceTick = tick - revealTick;
                double progress = Math.min(sinceTick / 20.0, 1.0);
                double ease = 1.0 - Math.pow(1.0 - progress, 3); // Ease-out cubic

                // Interpolate from far position to target
                double farMult = 4.0 * (1.0 - ease) + 1.0 * ease;
                double x = target[0] * farMult;
                double y = target[1];
                double z = target[2] * farMult;

                // Add slight oscillation while in transit
                if (progress < 1.0) {
                    double wobble = Math.sin(tick * 0.15 + i) * 0.2 * (1.0 - progress);
                    x += wobble;
                    z += wobble;
                }

                cocoonBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Spiral particle trail for incoming blocks
            if (tick % 3 == 0 && revealedCount < 14) {
                double t = (double) revealedCount / 13;
                double angle = t * Math.PI * 4;
                double y = t * 2.5;
                double r = 1.2 * (1.0 - t * 0.3);
                for (double mult = 1.5; mult <= 3.5; mult += 1.0) {
                    double px = Math.cos(angle) * r * mult;
                    double pz = Math.sin(angle) * r * mult;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(px, y, pz), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Ambient particles around the cocoon
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.2, 0), 5, 1.5, 100, 180, 255, 1.0f);
            }

            // End rod glow on completed cocoon
            if (revealedCount >= 14 && tick % 8 == 0) {
                double angle = tick * 0.1;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(x, 1.2, z), 2, 0.1, 0.5, 0.1, 0.01);
            }

            // Completion sound
            if (revealedCount == 14 && tick == 14 * 5 + 1) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.2, 0), 20, 1.5, 100, 180, 255, 1.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceCocoon(plugin); }
    }
}
