package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Blue Moon Block Display — GROUP: MOONBEAM & LIGHT
 * 13 moonbeam/light-themed BlockDisplay attacks.
 * Blue Moon palette: pale blue (180,210,255), silver (200,200,220), moonlight white (240,240,255).
 * NO status effects. Min 10 BlockDisplays per attack.
 */
public final class MoonbeamLight {

    private MoonbeamLight() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LunarSpotlight(plugin));
        registry.register(new PrismRefractor(plugin));
        registry.register(new MoonrisePillars(plugin));
        registry.register(new SilverLantern(plugin));
        registry.register(new AuroraRibbon(plugin));
        registry.register(new PhotonBurst(plugin));
        registry.register(new LightWell(plugin));
        registry.register(new BeaconSpear(plugin));
        registry.register(new HaloRing(plugin));
        registry.register(new RadiantCross(plugin));
        registry.register(new MoonbeamCage(plugin));
        registry.register(new StarlightCascade(plugin));
        registry.register(new DawnBreaker(plugin));
    }

    // ================================================================
    // 1. LUNAR SPOTLIGHT — Vertical column of light from Y+15
    // ================================================================
    public static class LunarSpotlight extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private int warningTicks = 40;
        private boolean activated = false;

        public LunarSpotlight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_spotlight", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 SEA_LANTERN blocks forming vertical column from Y+15 down to ground
            for (int i = 0; i < 10; i++) {
                double y = 15.0 - i * 1.5;
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                block.scale(0.8f, 1.5f, 0.8f).glow(240, 240, 255).interpolation(3, 0)
                     .brightness(15, 15);
                // Start invisible (tiny scale), reveal on activation
                block.scale(0.1f, 0.1f, 0.1f);
                columnBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Warning phase: circle on ground pulses
            if (ticksAlive < warningTicks) {
                if (ticksAlive % 3 == 0) {
                    double pulseRadius = 2.0 + Math.sin(ticksAlive * 0.3) * 0.5;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), pulseRadius, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(240, 240, 255), 1.0f));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f + ticksAlive * 0.02f);
                }
                return;
            }

            // Activate column
            if (!activated) {
                activated = true;
                for (int i = 0; i < columnBlocks.size(); i++) {
                    columnBlocks.get(i).scale(0.8f, 1.5f, 0.8f).interpolation(5, i);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.5f);
            }

            // Pulse the column brightness
            float pulse = (float) (0.7 + Math.sin(ticksAlive * 0.1) * 0.3);
            for (BlockDisplayHandle block : columnBlocks) {
                block.scale(0.8f * pulse, 1.5f, 0.8f * pulse);
            }

            // Light beam particles
            if (ticksAlive % 2 == 0) {
                double py = (ticksAlive * 0.5) % 15.0;
                DisplayBuilder.dustParticles(c.clone().add(0, py, 0), 5, 0.8, 240, 240, 255, 1.5f);
            }

            // Ground glow
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 8, 2.0, 200, 200, 220, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarSpotlight(plugin); }
    }

    // ================================================================
    // 2. PRISM REFRACTOR — Triangular prism shooting 3 rotating beams
    // ================================================================
    public static class PrismRefractor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> prismBlocks = new ArrayList<>();
        private float beamAngle = 0;

        public PrismRefractor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prism_refractor", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double height = 4.0;

            // Triangular prism: 3 vertical edges, each 4 blocks tall
            for (int edge = 0; edge < 3; edge++) {
                double angle = (Math.PI * 2 * edge) / 3;
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(x, height + y * 0.6, z);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    block.scale(0.4f, 0.6f, 0.4f).glow(200, 220, 255).interpolation(3, 0)
                         .brightness(15, 15);
                    prismBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            // Face fillers: 2 blocks per face = 6 more
            for (int face = 0; face < 3; face++) {
                double a1 = (Math.PI * 2 * face) / 3;
                double a2 = (Math.PI * 2 * (face + 1)) / 3;
                double mx = (Math.cos(a1) + Math.cos(a2)) * 0.5;
                double mz = (Math.sin(a1) + Math.sin(a2)) * 0.5;
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(mx, height + 0.6 + y * 1.0, mz);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    block.scale(0.5f, 0.8f, 0.3f).glow(220, 230, 255).interpolation(3, 0)
                         .brightness(15, 15);
                    prismBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            beamAngle += 0.04f;

            // Slow rotation of prism
            int idx = 0;
            double height = 4.0;
            for (int edge = 0; edge < 3; edge++) {
                double angle = (Math.PI * 2 * edge) / 3 + beamAngle * 0.2;
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                for (int y = 0; y < 4; y++) {
                    if (idx < prismBlocks.size()) {
                        prismBlocks.get(idx).entity().teleport(c.clone().add(x, height + y * 0.6, z));
                    }
                    idx++;
                }
            }

            // 3 rotating light beams
            if (ticksAlive % 2 == 0) {
                for (int beam = 0; beam < 3; beam++) {
                    double bAngle = beamAngle + (Math.PI * 2 * beam) / 3;
                    for (int d = 2; d <= 10; d++) {
                        double bx = Math.cos(bAngle) * d;
                        double bz = Math.sin(bAngle) * d;
                        // Beam color varies by direction
                        int r = beam == 0 ? 180 : (beam == 1 ? 200 : 240);
                        int g = beam == 0 ? 210 : (beam == 1 ? 200 : 240);
                        int b = 255;
                        DisplayBuilder.dustParticles(c.clone().add(bx, height + 1.0, bz), 2, 0.2, r, g, b, 0.8f);
                    }
                }
            }

            // Ambient hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismRefractor(plugin); }
    }

    // ================================================================
    // 3. MOONRISE PILLARS — 4 pillars with light beams connecting tops
    // ================================================================
    public static class MoonrisePillars extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final double[][] pillarPositions = {{3, 3}, {3, -3}, {-3, 3}, {-3, -3}};

        public MoonrisePillars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonrise_pillars", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 pillars, each 4 blocks tall = 16 blocks
            for (double[] pos : pillarPositions) {
                for (int y = 0; y < 4; y++) {
                    Material mat = (y == 3) ? Material.SEA_LANTERN : Material.QUARTZ_BLOCK;
                    Location loc = center.clone().add(pos[0], y, pos[1]);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    if (y == 3) {
                        block.scale(0.8f, 0.8f, 0.8f).glow(240, 240, 255).brightness(15, 15);
                    } else {
                        block.scale(0.7f, 1.0f, 0.7f).glow(220, 220, 230);
                    }
                    block.interpolation(3, 0);
                    pillarBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Light beams connecting pillar tops (4 edges of the square)
            if (ticksAlive % 3 == 0) {
                double topY = 3.5;
                for (int i = 0; i < 4; i++) {
                    int next = (i + 1) % 4;
                    Location start = c.clone().add(pillarPositions[i][0], topY, pillarPositions[i][1]);
                    Location end = c.clone().add(pillarPositions[next][0], topY, pillarPositions[next][1]);
                    DisplayBuilder.particleLine(start, end, Particle.DUST, 4,
                            new Particle.DustOptions(Color.fromRGB(240, 240, 255), 1.2f));
                }
            }

            // Pulsing glow on lantern blocks (every 4th block, indices 3, 7, 11, 15)
            float pulse = (float) (0.7 + Math.sin(ticksAlive * 0.08) * 0.3);
            for (int i = 3; i < pillarBlocks.size(); i += 4) {
                pillarBlocks.get(i).scale(0.8f * pulse, 0.8f * pulse, 0.8f * pulse);
            }

            // Interior light particles (inside the square)
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 8, 2.5, 200, 200, 220, 1.0f);
            }

            // Upward light rays from tops
            if (ticksAlive % 4 == 0) {
                for (double[] pos : pillarPositions) {
                    DisplayBuilder.dustParticles(c.clone().add(pos[0], 4.5, pos[1]), 3, 0.3, 240, 240, 255, 0.8f);
                }
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonrisePillars(plugin); }
    }

    // ================================================================
    // 4. SILVER LANTERN — Lantern shape swinging back and forth
    // ================================================================
    public static class SilverLantern extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> lanternBlocks = new ArrayList<>();
        private float swingAngle = 0;
        private float swingDirection = 1;

        public SilverLantern(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("silver_lantern", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double anchorY = 8.0;

            // Chain/arm (2 iron blocks)
            BlockDisplayHandle chain1 = displayBuilder.spawnBlock(center.clone().add(0, anchorY, 0), Material.IRON_BLOCK);
            chain1.scale(0.2f, 2.0f, 0.2f).glow(200, 200, 220).interpolation(3, 0);
            lanternBlocks.add(chain1);
            spawnedEntities.add(chain1.entity());

            BlockDisplayHandle chain2 = displayBuilder.spawnBlock(center.clone().add(0, anchorY - 2, 0), Material.IRON_BLOCK);
            chain2.scale(0.2f, 1.5f, 0.2f).glow(200, 200, 220).interpolation(3, 0);
            lanternBlocks.add(chain2);
            spawnedEntities.add(chain2.entity());

            // Lantern body: top cap
            BlockDisplayHandle topCap = displayBuilder.spawnBlock(center.clone().add(0, anchorY - 3.5, 0), Material.IRON_BLOCK);
            topCap.scale(1.2f, 0.3f, 1.2f).glow(200, 200, 220).interpolation(3, 0);
            lanternBlocks.add(topCap);
            spawnedEntities.add(topCap.entity());

            // Lantern body: light core (4 sea lanterns around center)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double x = Math.cos(angle) * 0.4;
                double z = Math.sin(angle) * 0.4;
                BlockDisplayHandle light = displayBuilder.spawnBlock(center.clone().add(x, anchorY - 4.2, z), Material.SEA_LANTERN);
                light.scale(0.5f, 0.8f, 0.5f).glow(240, 240, 255).brightness(15, 15).interpolation(3, 0);
                lanternBlocks.add(light);
                spawnedEntities.add(light.entity());
            }

            // Bottom cap
            BlockDisplayHandle botCap = displayBuilder.spawnBlock(center.clone().add(0, anchorY - 5.0, 0), Material.IRON_BLOCK);
            botCap.scale(1.0f, 0.3f, 1.0f).glow(200, 200, 220).interpolation(3, 0);
            lanternBlocks.add(botCap);
            spawnedEntities.add(botCap.entity());

            // Decorative iron finials (3)
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                double x = Math.cos(angle) * 0.6;
                double z = Math.sin(angle) * 0.6;
                BlockDisplayHandle finial = displayBuilder.spawnBlock(center.clone().add(x, anchorY - 3.8, z), Material.IRON_BLOCK);
                finial.scale(0.15f, 0.5f, 0.15f).glow(180, 180, 200).interpolation(3, 0);
                lanternBlocks.add(finial);
                spawnedEntities.add(finial.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing
            swingAngle = (float) (Math.sin(ticksAlive * 0.04) * 3.0);
            double swingX = Math.sin(swingAngle * 0.3) * 2.0;
            double anchorY = 8.0;

            // Update all blocks with swing offset
            // Chain segments
            lanternBlocks.get(0).entity().teleport(c.clone().add(swingX * 0.3, anchorY, 0));
            lanternBlocks.get(1).entity().teleport(c.clone().add(swingX * 0.6, anchorY - 2, 0));

            // Top cap
            lanternBlocks.get(2).entity().teleport(c.clone().add(swingX, anchorY - 3.5, 0));

            // Lights
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double lx = Math.cos(angle) * 0.4;
                double lz = Math.sin(angle) * 0.4;
                lanternBlocks.get(3 + i).entity().teleport(c.clone().add(swingX + lx, anchorY - 4.2, lz));
            }

            // Bottom cap
            lanternBlocks.get(7).entity().teleport(c.clone().add(swingX, anchorY - 5.0, 0));

            // Finials
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                double fx = Math.cos(angle) * 0.6;
                double fz = Math.sin(angle) * 0.6;
                if (8 + i < lanternBlocks.size()) {
                    lanternBlocks.get(8 + i).entity().teleport(c.clone().add(swingX + fx, anchorY - 3.8, fz));
                }
            }

            // Light pool beneath lantern
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(swingX, 0.2, 0), 6, 2.5, 240, 240, 255, 1.2f);
            }

            // Chain creak
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SilverLantern(plugin); }
    }

    // ================================================================
    // 5. AURORA RIBBON — Waving ribbon of colored glass in sine wave
    // ================================================================
    public static class AuroraRibbon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ribbonBlocks = new ArrayList<>();
        private float waveOffset = 0;

        public AuroraRibbon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aurora_ribbon", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] colors = {
                    Material.LIGHT_BLUE_STAINED_GLASS,
                    Material.WHITE_STAINED_GLASS,
                    Material.CYAN_STAINED_GLASS
            };

            // 18 blocks forming a ribbon across 18 blocks width
            for (int i = 0; i < 18; i++) {
                double x = (i - 9) * 0.8;
                double y = 4.0 + Math.sin(i * 0.5) * 1.5;
                Material mat = colors[i % 3];
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                block.scale(0.7f, 0.4f, 0.7f).glow(200, 220, 255).interpolation(3, 0)
                     .brightness(15, 15);
                ribbonBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            waveOffset += 0.06f;

            // Animate sine wave motion
            for (int i = 0; i < ribbonBlocks.size(); i++) {
                double x = (i - 9) * 0.8;
                double y = 4.0 + Math.sin(i * 0.5 + waveOffset) * 1.5;
                double z = Math.cos(i * 0.5 + waveOffset) * 0.8;
                ribbonBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Shimmer particles along ribbon
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % 18;
                double x = (idx - 9) * 0.8;
                double y = 4.0 + Math.sin(idx * 0.5 + waveOffset) * 1.5;
                double z = Math.cos(idx * 0.5 + waveOffset) * 0.8;
                DisplayBuilder.dustParticles(c.clone().add(x, y, z), 4, 0.4, 240, 240, 255, 0.8f);
            }

            // Falling sparkles beneath
            if (ticksAlive % 5 == 0) {
                double rx = (Math.random() - 0.5) * 12;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(rx, 3.5, 0), 2, 0.5, 0.3, 0.5, 0);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AuroraRibbon(plugin); }
    }

    // ================================================================
    // 6. PHOTON BURST — Compressed glowstone that fires outward
    // ================================================================
    public static class PhotonBurst extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> photons = new ArrayList<>();
        private final List<double[]> photonDirs = new ArrayList<>();
        private final List<Double> photonDist = new ArrayList<>();
        private boolean fired = false;
        private int chargeTime = 30;

        public PhotonBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("photon_burst", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(150);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 glowstone blocks clustered at center, Y+3
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * 0.3;
                double z = Math.sin(angle) * 0.3;
                Location loc = center.clone().add(x, 3.0, z);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.GLOWSTONE);
                block.scale(0.4f, 0.4f, 0.4f).glow(240, 240, 255).interpolation(3, 0)
                     .brightness(15, 15);
                photons.add(block);
                spawnedEntities.add(block.entity());

                // Store radial direction for firing
                photonDirs.add(new double[]{Math.cos(angle), Math.sin(angle)});
                photonDist.add(0.0);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Charge phase: compress inward
            if (ticksAlive < chargeTime) {
                float compressionScale = 0.4f - (ticksAlive * 0.01f);
                if (compressionScale < 0.15f) compressionScale = 0.15f;
                for (int i = 0; i < photons.size(); i++) {
                    double contractR = 0.3 * (1.0 - (double) ticksAlive / chargeTime);
                    double angle = (Math.PI * 2 * i) / 10;
                    double x = Math.cos(angle) * contractR;
                    double z = Math.sin(angle) * contractR;
                    photons.get(i).entity().teleport(c.clone().add(x, 3.0, z));
                    photons.get(i).scale(compressionScale, compressionScale, compressionScale);
                }

                // Charging particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, 1.5, 240, 240, 255, 1.0f);
                }
                return;
            }

            // Fire!
            if (!fired) {
                fired = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.5f);
                for (BlockDisplayHandle p : photons) {
                    p.scale(0.5f, 0.5f, 0.5f);
                }
            }

            // Expand outward
            for (int i = 0; i < photons.size(); i++) {
                double dist = photonDist.get(i) + 0.6;
                photonDist.set(i, dist);

                double[] dir = photonDirs.get(i);
                double x = dir[0] * dist;
                double z = dir[1] * dist;
                photons.get(i).entity().teleport(c.clone().add(x, 3.0, z));

                // Trail particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(x, 3.0, z), 3, 0.2, 200, 200, 220, 0.8f);
                }

                // Impact at range 8
                if (dist >= 8 && dist < 9) {
                    Location impactLoc = c.clone().add(x, 0, z);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 12, 1.5, 240, 240, 255, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhotonBurst(plugin); }
    }

    // ================================================================
    // 7. LIGHT WELL — Cylinder of sea lanterns with upward pull
    // ================================================================
    public static class LightWell extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wellBlocks = new ArrayList<>();
        private float pulsePhase = 0;

        public LightWell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("light_well", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Cylinder: 2 rings of 7 blocks each at different heights = 14 blocks
            for (int ring = 0; ring < 2; ring++) {
                double y = ring * 2.5;
                for (int i = 0; i < 7; i++) {
                    double angle = (Math.PI * 2 * i) / 7 + (ring * Math.PI / 7);
                    double x = Math.cos(angle) * 2.0;
                    double z = Math.sin(angle) * 2.0;
                    Location loc = center.clone().add(x, y, z);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                    block.scale(0.6f, 2.0f, 0.6f).glow(200, 220, 255).interpolation(3, 0)
                         .brightness(15, 15);
                    wellBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            pulsePhase += 0.08f;

            // Pulse blocks scale
            for (int i = 0; i < wellBlocks.size(); i++) {
                float pulse = (float) (0.5 + Math.sin(pulsePhase + i * 0.3) * 0.15);
                wellBlocks.get(i).scale(pulse, 2.0f, pulse);
            }

            // Upward particles inside the well
            if (ticksAlive % 2 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.5;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                double y = (ticksAlive * 0.3) % 5.0;
                DisplayBuilder.dustParticles(c.clone().add(x, y, z), 3, 0.2, 240, 240, 255, 1.0f);
            }

            // Upward velocity effect: snowflakes rising
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), 5, 1.0, 0.1, 1.0, 0.08);
            }

            // Hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LightWell(plugin); }
    }

    // ================================================================
    // 8. BEACON SPEAR — Quartz javelin that hovers then launches
    // ================================================================
    public static class BeaconSpear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spearBlocks = new ArrayList<>();
        private double spearY = 8.0;
        private int hoverTicks = 0;
        private boolean launched = false;
        private boolean impacted = false;
        private double launchDirX, launchDirZ;

        public BeaconSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("beacon_spear", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Javelin: 12 blocks forming elongated spear shape
            // Shaft: 8 quartz blocks in a line
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, spearY, i * 0.5 - 2);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                float taper = (i < 2 || i > 5) ? 0.2f : 0.35f;
                block.scale(taper, taper, 0.5f).glow(220, 220, 230).interpolation(3, 0);
                spearBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Tip: 2 narrow blocks
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, spearY, 2.0 + i * 0.4);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                block.scale(0.15f, 0.15f, 0.4f).glow(240, 240, 255).brightness(15, 15).interpolation(3, 0);
                spearBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Fletching: 2 wider blocks at back
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i == 0 ? 0.3 : -0.3, spearY, -2.5);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                block.scale(0.4f, 0.1f, 0.6f).glow(200, 200, 220).interpolation(3, 0);
                spearBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Random launch direction
            double angle = Math.random() * Math.PI * 2;
            launchDirX = Math.cos(angle);
            launchDirZ = Math.sin(angle);

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            if (!launched) {
                // Hover phase: subtle vibration for 50 ticks
                hoverTicks++;
                double wobble = Math.sin(hoverTicks * 0.3) * 0.05;
                spearY = 8.0 + wobble;

                // Aim toward player direction over time
                if (hoverTicks % 5 == 0) {
                    float rotAngle = (float) Math.atan2(launchDirZ, launchDirX);
                    for (BlockDisplayHandle block : spearBlocks) {
                        block.rotate(rotAngle, 0, 1, 0);
                    }
                }

                // Update positions
                for (int i = 0; i < 8; i++) {
                    spearBlocks.get(i).entity().teleport(c.clone().add(0, spearY, i * 0.5 - 2));
                }
                for (int i = 0; i < 2; i++) {
                    spearBlocks.get(8 + i).entity().teleport(c.clone().add(0, spearY, 2.0 + i * 0.4));
                }
                for (int i = 0; i < 2; i++) {
                    spearBlocks.get(10 + i).entity().teleport(c.clone().add(i == 0 ? 0.3 : -0.3, spearY, -2.5));
                }

                // Charging glow
                if (hoverTicks % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, spearY, 0), 5, 1.0, 200, 200, 220, 1.0f);
                }

                if (hoverTicks >= 50) {
                    launched = true;
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.8f);
                }
                return;
            }

            // Launch: fly in direction at 1.0 blocks/tick, descend
            spearY -= 0.15;
            double moveDist = (ticksAlive - 50 - hoverTicks) * 1.0;

            for (int i = 0; i < 8; i++) {
                double offsetZ = i * 0.5 - 2;
                double fwdX = launchDirX * (moveDist + offsetZ);
                double fwdZ = launchDirZ * (moveDist + offsetZ);
                spearBlocks.get(i).entity().teleport(c.clone().add(fwdX, spearY, fwdZ));
            }
            for (int i = 0; i < 2; i++) {
                double offsetZ = 2.0 + i * 0.4;
                spearBlocks.get(8 + i).entity().teleport(
                        c.clone().add(launchDirX * (moveDist + offsetZ), spearY, launchDirZ * (moveDist + offsetZ)));
            }
            for (int i = 0; i < 2; i++) {
                double offsetZ = -2.5;
                double sideOff = i == 0 ? 0.3 : -0.3;
                spearBlocks.get(10 + i).entity().teleport(
                        c.clone().add(launchDirX * (moveDist + offsetZ) + sideOff, spearY, launchDirZ * (moveDist + offsetZ)));
            }

            // Trail
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(
                        c.clone().add(launchDirX * moveDist, spearY, launchDirZ * moveDist),
                        5, 0.3, 240, 240, 255, 1.2f);
            }

            // Impact when reaching ground
            if (spearY <= 0.5) {
                impacted = true;
                Location impactLoc = c.clone().add(launchDirX * moveDist, 0, launchDirZ * moveDist);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.dustParticles(impactLoc, 30, 2.5, 240, 240, 255, 2.0f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
                displayBuilder.removeAll();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BeaconSpear(plugin); }
    }

    // ================================================================
    // 9. HALO RING — Horizontal ring at Y+4 that contracts/expands
    // ================================================================
    public static class HaloRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private double currentRadius = 4.0;
        private boolean expanding = false;

        public HaloRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("halo_ring", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 blocks: alternating gold and sea lantern
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                Material mat = (i % 2 == 0) ? Material.GOLD_BLOCK : Material.SEA_LANTERN;
                Location loc = center.clone().add(x, 4.0, z);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                if (mat == Material.SEA_LANTERN) {
                    block.scale(0.5f, 0.3f, 0.5f).glow(240, 240, 255).brightness(15, 15);
                } else {
                    block.scale(0.5f, 0.3f, 0.5f).glow(220, 200, 100);
                }
                block.interpolation(3, 0);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathe: contract to 1.5, expand to 5.0
            if (!expanding) {
                currentRadius -= 0.04;
                if (currentRadius <= 1.5) expanding = true;
            } else {
                currentRadius += 0.04;
                if (currentRadius >= 5.0) expanding = false;
            }

            // Slow rotation
            double rotOffset = ticksAlive * 0.02;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + rotOffset;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                ringBlocks.get(i).entity().teleport(c.clone().add(x, 4.0, z));
            }

            // Golden light trail
            if (ticksAlive % 3 == 0) {
                double trailAngle = rotOffset * 3;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(trailAngle) * currentRadius, 4.0, Math.sin(trailAngle) * currentRadius),
                        4, 0.3, 240, 220, 150, 1.0f);
            }

            // Downward light shaft
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 6, currentRadius * 0.5, 240, 240, 255, 0.8f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HaloRing(plugin); }
    }

    // ================================================================
    // 10. RADIANT CROSS — Cross shape rotating 45 degrees alternating
    // ================================================================
    public static class RadiantCross extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crossBlocks = new ArrayList<>();
        private float rotationAngle = 0;
        private boolean rotatingForward = true;

        public RadiantCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("radiant_cross", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double height = 3.0;

            // Cross: 2 arms, each 7 blocks long (center shared) = 14 blocks total
            // Horizontal arm (7 blocks along X)
            for (int i = -3; i <= 3; i++) {
                Location loc = center.clone().add(i * 1.0, height, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                float s = (i == 0) ? 0.8f : 0.6f;
                block.scale(s, 0.6f, s).glow(220, 220, 230).interpolation(3, 0);
                crossBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Vertical arm (7 blocks along Z, skip center)
            for (int i = -3; i <= 3; i++) {
                if (i == 0) continue; // center already placed
                Location loc = center.clone().add(0, height, i * 1.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                block.scale(0.6f, 0.6f, 0.6f).glow(220, 220, 230).interpolation(3, 0);
                crossBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double height = 3.0;

            // Rotate 45 degrees (PI/4), then back, oscillating
            if (rotatingForward) {
                rotationAngle += 0.03f;
                if (rotationAngle >= Math.PI / 4) rotatingForward = false;
            } else {
                rotationAngle -= 0.03f;
                if (rotationAngle <= -Math.PI / 4) rotatingForward = true;
            }

            // Update horizontal arm (indices 0-6)
            for (int i = 0; i < 7; i++) {
                double origX = (i - 3) * 1.0;
                double rx = origX * Math.cos(rotationAngle);
                double rz = origX * Math.sin(rotationAngle);
                crossBlocks.get(i).entity().teleport(c.clone().add(rx, height, rz));
            }

            // Update vertical arm (indices 7-12, corresponding to Z offsets -3,-2,-1,1,2,3)
            int[] zOffsets = {-3, -2, -1, 1, 2, 3};
            for (int i = 0; i < 6; i++) {
                double origZ = zOffsets[i] * 1.0;
                double rx = -origZ * Math.sin(rotationAngle);
                double rz = origZ * Math.cos(rotationAngle);
                if (7 + i < crossBlocks.size()) {
                    crossBlocks.get(7 + i).entity().teleport(c.clone().add(rx, height, rz));
                }
            }

            // Light particles at arm tips
            if (ticksAlive % 4 == 0) {
                double tipDist = 3.0;
                double[] angles = {rotationAngle, rotationAngle + Math.PI / 2, rotationAngle + Math.PI, rotationAngle + Math.PI * 1.5};
                for (double a : angles) {
                    double tx = Math.cos(a) * tipDist;
                    double tz = Math.sin(a) * tipDist;
                    DisplayBuilder.dustParticles(c.clone().add(tx, height, tz), 3, 0.3, 240, 240, 255, 1.0f);
                }
            }

            // Center glow
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, height, 0), 5, 0.5, 200, 200, 220, 1.2f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RadiantCross(plugin); }
    }

    // ================================================================
    // 11. MOONBEAM CAGE — Sea lantern pillars closing inward
    // ================================================================
    public static class MoonbeamCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private double cageRadius = 5.0;
        private static final double CONTRACT_RATE = 0.025;

        public MoonbeamCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonbeam_cage", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 sea lantern pillars in a circle, each 1 block tall (stacked appearance)
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * cageRadius;
                double z = Math.sin(angle) * cageRadius;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                pillar.scale(0.4f, 4.0f, 0.4f).glow(240, 240, 255).brightness(15, 15)
                      .interpolation(3, 0);
                pillarBlocks.add(pillar);
                spawnedEntities.add(pillar.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract
            if (cageRadius > 1.0) {
                cageRadius -= CONTRACT_RATE;
                if (cageRadius < 1.0) cageRadius = 1.0;
            }

            for (int i = 0; i < pillarBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * cageRadius;
                double z = Math.sin(angle) * cageRadius;
                pillarBlocks.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            // Connecting beams between adjacent pillars
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    int next = (i + 1) % 12;
                    double a1 = (Math.PI * 2 * i) / 12;
                    double a2 = (Math.PI * 2 * next) / 12;
                    Location start = c.clone().add(Math.cos(a1) * cageRadius, 2.0, Math.sin(a1) * cageRadius);
                    Location end = c.clone().add(Math.cos(a2) * cageRadius, 2.0, Math.sin(a2) * cageRadius);
                    DisplayBuilder.particleLine(start, end, Particle.DUST, 3,
                            new Particle.DustOptions(Color.fromRGB(200, 200, 220), 0.8f));
                }
            }

            // Warning pulse as cage tightens
            if (cageRadius < 2.5 && ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 8, cageRadius, 240, 240, 255, 1.5f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.0f + (float)(5.0 - cageRadius) * 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonbeamCage(plugin); }
    }

    // ================================================================
    // 12. STARLIGHT CASCADE — Falling waterfall of sea lanterns
    // ================================================================
    public static class StarlightCascade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cascadeBlocks = new ArrayList<>();
        private final List<Double> blockY = new ArrayList<>();

        public StarlightCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("starlight_cascade", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 sea lantern blocks distributed in a waterfall pattern
            Random rand = new Random();
            for (int i = 0; i < 15; i++) {
                double ox = (rand.nextDouble() - 0.5) * 3.0;
                double oz = (rand.nextDouble() - 0.5) * 3.0;
                double startY = 20.0 - (i * 1.3) + rand.nextDouble() * 2.0;
                blockY.add(startY);

                Location loc = center.clone().add(ox, startY, oz);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                float scale = 0.3f + rand.nextFloat() * 0.4f;
                block.scale(scale, scale, scale).glow(240, 240, 255).brightness(15, 15)
                     .interpolation(2, 0);
                cascadeBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Random rand = new Random(42); // deterministic for position offsets
            for (int i = 0; i < cascadeBlocks.size(); i++) {
                double ox = (rand.nextDouble() - 0.5) * 3.0;
                double oz = (rand.nextDouble() - 0.5) * 3.0;
                rand.nextDouble(); // consume the startY random

                double y = blockY.get(i);
                y -= 0.3; // continuous fall

                // Reset to top when reaching ground
                if (y < 0) {
                    y = 20.0 + Math.random() * 2.0;
                }
                blockY.set(i, y);

                // Add slight horizontal drift
                double driftX = ox + Math.sin(ticksAlive * 0.05 + i) * 0.3;
                double driftZ = oz + Math.cos(ticksAlive * 0.05 + i) * 0.3;
                cascadeBlocks.get(i).entity().teleport(c.clone().add(driftX, y, driftZ));
            }

            // Splash particles at bottom
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 8, 2.0, 200, 200, 220, 1.0f);
                c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, 0.2, 0), 5, 1.5, 0.1, 1.5, 0);
            }

            // Light glow at stream
            if (ticksAlive % 5 == 0) {
                double midY = 10.0 + Math.sin(ticksAlive * 0.1) * 3;
                DisplayBuilder.dustParticles(c.clone().add(0, midY, 0), 4, 1.5, 240, 240, 255, 0.8f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarlightCascade(plugin); }
    }

    // ================================================================
    // 13. DAWN BREAKER — Sun-like burst (sphere + 8 spikes) expanding
    // ================================================================
    public static class DawnBreaker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private double expandRadius = 0.5;
        private boolean impacted = false;
        private static final double MAX_RADIUS = 6.0;
        private static final double EXPAND_SPEED = 0.08;

        public DawnBreaker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dawn_breaker", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double height = 4.0;

            // Central sphere: 10 blocks in sphere arrangement
            List<BlockDisplayHandle> sphere = displayBuilder.spawnSphere(
                    center.clone().add(0, height, 0), Material.SEA_LANTERN, 0.8, 10);
            for (BlockDisplayHandle h : sphere) {
                h.scale(0.5f, 0.5f, 0.5f).glow(240, 240, 255).brightness(15, 15).interpolation(3, 0);
                coreBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 spike directions (cardinal + diagonal)
            double[][] spikeDirs = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {0.707, 0.707}, {-0.707, 0.707}, {0.707, -0.707}, {-0.707, -0.707}
            };

            for (double[] dir : spikeDirs) {
                Location loc = center.clone().add(dir[0] * 1.5, height, dir[1] * 1.5);
                BlockDisplayHandle spike = displayBuilder.spawnBlock(loc, Material.GLOWSTONE);
                spike.scale(0.3f, 0.3f, 0.8f).glow(240, 230, 200).brightness(15, 15).interpolation(3, 0)
                     .rotate((float) Math.atan2(dir[1], dir[0]), 0, 1, 0);
                spikeBlocks.add(spike);
                spawnedEntities.add(spike.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double height = 4.0;

            if (expandRadius < MAX_RADIUS) {
                expandRadius += EXPAND_SPEED;

                // Expand sphere core
                float coreScale = (float) (0.5 + expandRadius * 0.1);
                for (BlockDisplayHandle core : coreBlocks) {
                    core.scale(coreScale, coreScale, coreScale);
                }

                // Expand spikes outward
                double[][] spikeDirs = {
                        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                        {0.707, 0.707}, {-0.707, 0.707}, {0.707, -0.707}, {-0.707, -0.707}
                };
                for (int i = 0; i < spikeBlocks.size() && i < spikeDirs.length; i++) {
                    double[] dir = spikeDirs[i];
                    spikeBlocks.get(i).entity().teleport(
                            c.clone().add(dir[0] * expandRadius, height, dir[1] * expandRadius));
                    float spikeScale = (float) (0.3 + expandRadius * 0.05);
                    spikeBlocks.get(i).scale(spikeScale, spikeScale, spikeScale + 0.5f);
                }

                // Radiant particles at spike tips
                if (ticksAlive % 3 == 0) {
                    for (double[] dir : spikeDirs) {
                        DisplayBuilder.dustParticles(
                                c.clone().add(dir[0] * expandRadius, height, dir[1] * expandRadius),
                                3, 0.3, 240, 240, 255, 1.2f);
                    }
                }

                // Central glow
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, height, 0), 8, expandRadius * 0.3, 200, 200, 220, 1.0f);
                }
            }

            // Impact when fully expanded
            if (expandRadius >= MAX_RADIUS && !impacted) {
                impacted = true;

                // Damage at each spike tip
                double[][] spikeDirs = {
                        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                        {0.707, 0.707}, {-0.707, 0.707}, {0.707, -0.707}, {-0.707, -0.707}
                };
                for (double[] dir : spikeDirs) {
                    Location tipLoc = c.clone().add(dir[0] * MAX_RADIUS, 0, dir[1] * MAX_RADIUS);
                    triggerImpactDamage(tipLoc);
                }

                // Massive light burst
                DisplayBuilder.dustParticles(c.clone().add(0, height, 0), 40, 5.0, 240, 240, 255, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1.2f, 1.0f);
                c.getWorld().spawnParticle(Particle.FLASH, c.clone().add(0, height, 0), 3, 0, 0, 0, 0);
            }

            if (ticksAlive % 20 == 0 && !impacted) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f + (float)(expandRadius / MAX_RADIUS));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DawnBreaker(plugin); }
    }
}
