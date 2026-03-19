package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode Block Display — GROUP: OVERHEAD / SKY STRUCTURES
 * 15 chain-themed BlockDisplay attacks featuring iron, chain, and metallic elements.
 * All attacks use phase 1, metallic sounds, iron/chain color palette.
 * NO status effects. Min 10 BlockDisplays, min 40 HP damage per attack.
 */
public final class OverheadSky {

    private OverheadSky() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainCanopy(plugin));
        registry.register(new SuspendedChainBridge(plugin));
        registry.register(new ChainSkyAnvil(plugin));
        registry.register(new HangingChainForest(plugin));
        registry.register(new ChainChandelier(plugin));
        registry.register(new SkyChainWeb(plugin));
        registry.register(new ChainMeteor(plugin));
        registry.register(new FloatingChainIsland(plugin));
        registry.register(new ChainGuillotine(plugin));
        registry.register(new SkyChainRain(plugin));
        registry.register(new ChainSkywheel(plugin));
        registry.register(new HoveringChainCube(plugin));
        registry.register(new ChainBomber(plugin));
        registry.register(new SkyChainLattice(plugin));
        registry.register(new ChainComet(plugin));
    }

    // ================================================================
    // 1. CHAIN CANOPY — 4x4 grid of chains that descends and crushes
    // ================================================================
    public static class ChainCanopy extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> canopyChains = new ArrayList<>();
        private final List<BlockDisplayHandle> supportPosts = new ArrayList<>();
        private float canopyY = 12.0f;
        private static final float DESCENT_SPEED = 0.04f;

        public ChainCanopy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_canopy", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid of chain blocks forming the canopy (16 chains)
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double ox = (gx - 1.5) * 2.0;
                    double oz = (gz - 1.5) * 2.0;
                    Location loc = center.clone().add(ox, canopyY, oz);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    // Edge chains droop lower
                    boolean isEdge = gx == 0 || gx == 3 || gz == 0 || gz == 3;
                    float droopY = isEdge ? -1.2f : 0f;
                    chain.scale(1.0f, 2.0f, 1.0f)
                         .translate(-0.5f, droopY - 0.5f, -0.5f)
                         .glow(140, 140, 150)
                         .interpolation(5, 0);
                    canopyChains.add(chain);
                    spawnedEntities.add(chain.entity());
                }
            }

            // 4 corner support posts (iron blocks)
            double[][] corners = {{-3.5, 0, -3.5}, {-3.5, 0, 3.5}, {3.5, 0, -3.5}, {3.5, 0, 3.5}};
            for (double[] corner : corners) {
                Location pLoc = center.clone().add(corner[0], canopyY + 1, corner[2]);
                BlockDisplayHandle post = displayBuilder.spawnBlock(pLoc, Material.IRON_BLOCK);
                post.scale(0.5f, 3.0f, 0.5f).glow(180, 180, 190).interpolation(5, 0);
                supportPosts.add(post);
                spawnedEntities.add(post.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, canopyY, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center.clone().add(0, canopyY, 0), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descend slowly
            if (canopyY > 1.5f) {
                canopyY -= DESCENT_SPEED;
            }

            // Move canopy chains
            int idx = 0;
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double ox = (gx - 1.5) * 2.0;
                    double oz = (gz - 1.5) * 2.0;
                    boolean isEdge = gx == 0 || gx == 3 || gz == 0 || gz == 3;
                    // Edge chains sway slightly
                    double swayX = isEdge ? Math.sin(ticksAlive * 0.08 + gx) * 0.3 : 0;
                    double swayZ = isEdge ? Math.cos(ticksAlive * 0.06 + gz) * 0.3 : 0;
                    canopyChains.get(idx).entity().teleport(
                            c.clone().add(ox + swayX, canopyY, oz + swayZ));
                    idx++;
                }
            }

            // Move support posts
            double[][] corners = {{-3.5, 0, -3.5}, {-3.5, 0, 3.5}, {3.5, 0, -3.5}, {3.5, 0, 3.5}};
            for (int i = 0; i < supportPosts.size(); i++) {
                supportPosts.get(i).entity().teleport(
                        c.clone().add(corners[i][0], canopyY + 1, corners[i][2]));
            }

            // Shadow particles below the canopy
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double rx = (Math.random() - 0.5) * 8;
                    double rz = (Math.random() - 0.5) * 8;
                    DisplayBuilder.dustParticles(c.clone().add(rx, 0.2, rz), 2, 0.3, 40, 40, 40, 1.5f);
                }
            }

            // Chain creak sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, canopyY, 0), Sound.BLOCK_CHAIN_STEP, 0.8f, 0.4f);
            }

            // Metallic groan
            if (ticksAlive % 100 == 50) {
                DisplayBuilder.playSound(c.clone().add(0, canopyY, 0), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCanopy(plugin); }
    }

    // ================================================================
    // 2. SUSPENDED CHAIN BRIDGE — Swaying bridge of chains overhead
    // ================================================================
    public static class SuspendedChainBridge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bridgeChains = new ArrayList<>();
        private final List<BlockDisplayHandle> supportChains = new ArrayList<>();
        private final boolean[] dropped;
        private float swayPhase = 0;

        public SuspendedChainBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("suspended_chain_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(700);
            config.setCooldownTicks(450);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(42.0);
            config.setImpactRadius(2.5);
            dropped = new boolean[14];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 bridge sections spanning 10 blocks along X axis, at height 8
            for (int i = 0; i < 14; i++) {
                double x = (i - 6.5) * 0.75;
                // Catenary droop: deeper in the middle
                double droop = -Math.abs(i - 6.5) * 0.15;
                Location loc = center.clone().add(x, 8 + droop, 0);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.8f, 1.5f, 1.2f).glow(160, 160, 170).interpolation(4, 0);
                bridgeChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // 4 support chains at each end (2 per end, hanging down from bridge endpoints)
            double[][] supportOffsets = {{-5.0, 8.5, -1.0}, {-5.0, 8.5, 1.0}, {5.0, 8.5, -1.0}, {5.0, 8.5, 1.0}};
            for (double[] off : supportOffsets) {
                Location sLoc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle support = displayBuilder.spawnBlock(sLoc, Material.IRON_BLOCK);
                support.scale(0.4f, 4.0f, 0.4f).glow(120, 120, 130).interpolation(4, 0);
                supportChains.add(support);
                spawnedEntities.add(support.entity());
            }

            // Anchor blocks at top of supports
            for (double xEnd : new double[]{-5.0, 5.0}) {
                Location aLoc = center.clone().add(xEnd, 12.5, 0);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(aLoc, Material.IRON_BLOCK);
                anchor.scale(1.5f, 0.5f, 2.5f).glow(180, 180, 190).interpolation(4, 0);
                spawnedEntities.add(anchor.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 8, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            swayPhase += 0.04f;

            // Sway bridge side to side
            double swayZ = Math.sin(swayPhase) * 1.5;

            for (int i = 0; i < bridgeChains.size(); i++) {
                if (dropped[i]) continue;

                double x = (i - 6.5) * 0.75;
                double droop = -Math.abs(i - 6.5) * 0.15;
                // Individual chain sway with phase offset
                double localSway = swayZ * (1.0 - Math.abs(i - 6.5) / 7.0);
                bridgeChains.get(i).entity().teleport(
                        c.clone().add(x, 8 + droop, localSway));
            }

            // Check if any player is underneath an undropped section, drop it
            if (ticksAlive > 40 && ticksAlive % 10 == 0) {
                for (org.bukkit.entity.Player player : c.getWorld().getPlayers()) {
                    if (isExempt(player)) continue;
                    Location pLoc = player.getLocation();
                    for (int i = 0; i < bridgeChains.size(); i++) {
                        if (dropped[i]) continue;
                        double x = (i - 6.5) * 0.75;
                        double dist = Math.abs(pLoc.getX() - (c.getX() + x));
                        double distZ = Math.abs(pLoc.getZ() - c.getZ());
                        if (dist < 1.5 && distZ < 3.0 && pLoc.getY() < c.getY() + 8) {
                            dropped[i] = true;
                            // Drop this chain section
                            triggerImpactDamage(pLoc.clone());
                            DisplayBuilder.playSound(bridgeChains.get(i).entity().getLocation(),
                                    Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.5f);
                        }
                    }
                }
            }

            // Animate dropped chains falling
            for (int i = 0; i < bridgeChains.size(); i++) {
                if (!dropped[i]) continue;
                Location bLoc = bridgeChains.get(i).entity().getLocation();
                if (bLoc.getY() > c.getY() + 0.5) {
                    bridgeChains.get(i).entity().teleport(bLoc.add(0, -0.6, 0));
                    // Falling particles
                    DisplayBuilder.dustParticles(bLoc, 3, 0.4, 120, 120, 130, 1.0f);
                }
            }

            // Creaking sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_CHAIN_STEP, 0.7f, 0.3f);
            }

            // Metallic stress sound
            if (ticksAlive % 120 == 60) {
                DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SuspendedChainBridge(plugin); }
    }

    // ================================================================
    // 3. CHAIN SKY ANVIL — Massive anvil that charges then drops
    // ================================================================
    public static class ChainSkyAnvil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> anvilBody = new ArrayList<>();
        private final List<BlockDisplayHandle> supportChains = new ArrayList<>();
        private float anvilY = 15.0f;
        private boolean charging = true;
        private boolean falling = false;
        private boolean impacted = false;
        private int chargeTimer = 0;
        private float glowIntensity = 0;

        public ChainSkyAnvil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_sky_anvil", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(500);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(80.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Anvil base — wide bottom (4 iron blocks in a flat cross)
            double[][] baseOffsets = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
            for (double[] off : baseOffsets) {
                Location loc = center.clone().add(off[0], anvilY, off[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                block.scale(2.0f, 1.0f, 2.0f).glow(180, 180, 190).interpolation(3, 0);
                anvilBody.add(block);
                spawnedEntities.add(block.entity());
            }

            // Anvil neck — narrower middle section (2 iron blocks)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, anvilY + 1 + i, 0);
                BlockDisplayHandle neck = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                neck.scale(1.2f, 1.0f, 1.2f).glow(160, 160, 170).interpolation(3, 0);
                anvilBody.add(neck);
                spawnedEntities.add(neck.entity());
            }

            // Anvil top — flat top (1 anvil block)
            BlockDisplayHandle top = displayBuilder.spawnBlock(
                    center.clone().add(0, anvilY + 3, 0), Material.ANVIL);
            top.scale(2.5f, 0.8f, 2.5f).glow(200, 200, 210).interpolation(3, 0);
            anvilBody.add(top);
            spawnedEntities.add(top.entity());

            // 4 support chains hanging from corners
            double[][] chainCorners = {{-2, 0, -2}, {-2, 0, 2}, {2, 0, -2}, {2, 0, 2}};
            for (double[] cc : chainCorners) {
                Location cLoc = center.clone().add(cc[0], anvilY - 1, cc[2]);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(cLoc, Material.CHAIN);
                chain.scale(0.3f, 3.0f, 0.3f).glow(140, 140, 150).interpolation(3, 0);
                supportChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, anvilY, 0), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            if (charging) {
                chargeTimer++;
                glowIntensity = Math.min(1.0f, chargeTimer / 100.0f);

                // Pulsing glow during charge
                int glowR = (int) (180 + 75 * glowIntensity);
                int glowG = (int) (180 * (1.0f - glowIntensity * 0.5f));
                int glowB = (int) (190 * (1.0f - glowIntensity * 0.7f));
                for (BlockDisplayHandle block : anvilBody) {
                    block.glow(Math.min(255, glowR), Math.max(0, glowG), Math.max(0, glowB));
                }

                // Vibration during charge
                if (chargeTimer > 60) {
                    float shake = (float) Math.sin(chargeTimer * 3.0) * 0.08f * glowIntensity;
                    for (BlockDisplayHandle block : anvilBody) {
                        Location bLoc = block.entity().getLocation();
                        block.entity().teleport(bLoc.clone().add(shake, 0, shake));
                    }
                }

                // Charging particles
                if (chargeTimer % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, anvilY, 0),
                            8, 2.0, 255, 200, 100, 1.5f);
                }

                // Charge-up sound
                if (chargeTimer % 20 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, anvilY, 0),
                            Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.5f + glowIntensity * 0.5f);
                }

                if (chargeTimer >= 120) {
                    charging = false;
                    falling = true;
                    // Release chains — they snap
                    for (BlockDisplayHandle chain : supportChains) {
                        chain.entity().remove();
                    }
                    supportChains.clear();
                    DisplayBuilder.playSound(c.clone().add(0, anvilY, 0),
                            Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.3f);
                }
                return;
            }

            if (falling) {
                // Accelerating fall
                anvilY -= 0.5f + (120 - anvilY) * 0.03f;

                if (anvilY <= 0.5f) {
                    anvilY = 0.5f;
                    impacted = true;
                    falling = false;
                    triggerImpactDamage(c);
                    return;
                }

                // Move all anvil blocks
                double[][] baseOffsets = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
                for (int i = 0; i < Math.min(5, anvilBody.size()); i++) {
                    anvilBody.get(i).entity().teleport(
                            c.clone().add(baseOffsets[i][0], anvilY, baseOffsets[i][2]));
                }
                if (anvilBody.size() > 5) {
                    anvilBody.get(5).entity().teleport(c.clone().add(0, anvilY + 1, 0));
                }
                if (anvilBody.size() > 6) {
                    anvilBody.get(6).entity().teleport(c.clone().add(0, anvilY + 2, 0));
                }
                if (anvilBody.size() > 7) {
                    anvilBody.get(7).entity().teleport(c.clone().add(0, anvilY + 3, 0));
                }

                // Falling trail particles
                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, anvilY + 4, 0),
                            10, 1.0, 0.5, 1.0, 0.05);
                }

                // Wind rush sound
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, anvilY, 0),
                            Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.3f);
                }
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            World w = impactLocation.getWorld();
            if (w == null) return;

            // Massive crater effect: iron block particles + shockwave
            w.spawnParticle(Particle.BLOCK, impactLocation, 200, 3, 1, 3, 0.5,
                    Material.IRON_BLOCK.createBlockData());
            w.spawnParticle(Particle.EXPLOSION, impactLocation, 8, 2, 1, 2, 0);
            DisplayBuilder.particleRing(impactLocation, 5.0, Particle.CAMPFIRE_SIGNAL_SMOKE, 40, null);
            DisplayBuilder.dustParticles(impactLocation, 30, 5.0, 200, 200, 200, 2.0f);

            // Impact sounds
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
            DisplayBuilder.playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.5f);

            displayBuilder.removeAll();
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSkyAnvil(plugin); }
    }

    // ================================================================
    // 4. HANGING CHAIN FOREST — 20 chains hanging from invisible ceiling
    // ================================================================
    public static class HangingChainForest extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> hangingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> topAnchors = new ArrayList<>();
        private final double[] chainX = new double[20];
        private final double[] chainZ = new double[20];
        private final float[] chainHeight = new float[20];
        private final boolean[] chainDropped = new boolean[20];
        private final float[] swayPhase = new float[20];

        public HangingChainForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_chain_forest", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 chains at random positions within 6-block radius
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 6.0;
                chainX[i] = Math.cos(angle) * dist;
                chainZ[i] = Math.sin(angle) * dist;
                chainHeight[i] = 15.0f;
                swayPhase[i] = (float) (Math.random() * Math.PI * 2);

                // Chain body — tall vertical chain
                Location loc = center.clone().add(chainX[i], chainHeight[i], chainZ[i]);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                float chainLen = 3.0f + (float) (Math.random() * 4.0);
                chain.scale(0.5f, chainLen, 0.5f).glow(150, 150, 160).interpolation(4, 0);
                hangingChains.add(chain);
                spawnedEntities.add(chain.entity());

                // Small anchor block at top
                Location aLoc = center.clone().add(chainX[i], chainHeight[i] + chainLen * 0.5 + 0.3, chainZ[i]);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(aLoc, Material.DEEPSLATE);
                anchor.scale(0.6f, 0.3f, 0.6f).glow(80, 80, 90).interpolation(4, 0);
                topAnchors.add(anchor);
                spawnedEntities.add(anchor.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 15, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 20; i++) {
                if (chainDropped[i]) {
                    // Falling chain
                    if (chainHeight[i] > 0.5f) {
                        chainHeight[i] -= 0.8f;
                        hangingChains.get(i).entity().teleport(
                                c.clone().add(chainX[i], chainHeight[i], chainZ[i]));
                        // Impact particles on landing
                        if (chainHeight[i] <= 0.5f) {
                            DisplayBuilder.dustParticles(
                                    c.clone().add(chainX[i], 0.5, chainZ[i]),
                                    10, 0.5, 150, 150, 160, 1.2f);
                            DisplayBuilder.playSound(
                                    c.clone().add(chainX[i], 0.5, chainZ[i]),
                                    Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.6f);
                        }
                    }
                    continue;
                }

                // Gentle sway
                double swayX = Math.sin(ticksAlive * 0.06 + swayPhase[i]) * 0.4;
                double swayZ = Math.cos(ticksAlive * 0.05 + swayPhase[i] * 1.3) * 0.4;
                hangingChains.get(i).entity().teleport(
                        c.clone().add(chainX[i] + swayX, chainHeight[i], chainZ[i] + swayZ));
                topAnchors.get(i).entity().teleport(
                        c.clone().add(chainX[i], chainHeight[i] + 3.5, chainZ[i]));

                // Check for nearby players to trigger drops
                if (ticksAlive > 30 && ticksAlive % 8 == 0) {
                    for (org.bukkit.entity.Player player : c.getWorld().getPlayers()) {
                        if (isExempt(player)) continue;
                        Location pLoc = player.getLocation();
                        double dx = pLoc.getX() - (c.getX() + chainX[i]);
                        double dz = pLoc.getZ() - (c.getZ() + chainZ[i]);
                        if (dx * dx + dz * dz < 4.0 && pLoc.getY() < c.getY() + chainHeight[i]) {
                            chainDropped[i] = true;
                            DisplayBuilder.playSound(
                                    hangingChains.get(i).entity().getLocation(),
                                    Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.7f);
                            triggerImpactDamage(pLoc.clone());
                            break;
                        }
                    }
                }
            }

            // Ambient chain rattling
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 12, 0), Sound.BLOCK_CHAIN_STEP, 0.5f, 0.5f);
            }

            // Occasional metallic groan
            if (ticksAlive % 160 == 80) {
                DisplayBuilder.playSound(c.clone().add(0, 15, 0), Sound.BLOCK_IRON_DOOR_CLOSE, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HangingChainForest(plugin); }
    }

    // ================================================================
    // 5. CHAIN CHANDELIER — Ornate rotating chandelier that descends
    // ================================================================
    public static class ChainChandelier extends BlockDisplayAttack {

        private BlockDisplayHandle centralPillar;
        private final List<BlockDisplayHandle> hangingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> ironBars = new ArrayList<>();
        private BlockDisplayHandle topCap;
        private BlockDisplayHandle bottomGem;
        private float chandelierY = 12.0f;
        private float rotation = 0;

        public ChainChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_chandelier", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(800);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central iron pillar
            centralPillar = displayBuilder.spawnBlock(
                    center.clone().add(0, chandelierY, 0), Material.IRON_BLOCK);
            centralPillar.scale(0.6f, 4.0f, 0.6f).glow(200, 200, 220).interpolation(5, 0);
            spawnedEntities.add(centralPillar.entity());

            // Top cap — decorative heavy_core
            topCap = displayBuilder.spawnBlock(
                    center.clone().add(0, chandelierY + 3, 0), Material.HEAVY_CORE);
            topCap.scale(1.2f, 0.5f, 1.2f).glow(160, 160, 180).interpolation(5, 0);
            spawnedEntities.add(topCap.entity());

            // Bottom gem — netherite accent
            bottomGem = displayBuilder.spawnBlock(
                    center.clone().add(0, chandelierY - 1.5, 0), Material.NETHERITE_BLOCK);
            bottomGem.scale(0.4f, 0.4f, 0.4f).glow(80, 80, 100).interpolation(5, 0);
            spawnedEntities.add(bottomGem.entity());

            // 8 hanging chains at angles
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Location cLoc = center.clone().add(x, chandelierY - 0.5, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(cLoc, Material.CHAIN);
                chain.scale(0.3f, 2.5f, 0.3f).glow(140, 140, 155).interpolation(5, 0);
                hangingChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // 4 iron bar decorations at cardinal positions
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                Location bLoc = center.clone().add(x, chandelierY + 1.5, z);
                BlockDisplayHandle bar = displayBuilder.spawnBlock(bLoc, Material.IRON_BARS);
                bar.scale(0.5f, 1.5f, 0.5f).glow(180, 180, 200).interpolation(5, 0);
                ironBars.add(bar);
                spawnedEntities.add(bar.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, chandelierY, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
            DisplayBuilder.playSound(center.clone().add(0, chandelierY, 0),
                    Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow descent
            if (chandelierY > 2.0f) {
                chandelierY -= 0.02f;
            }

            // Slow rotation
            rotation += 0.015f;

            // Move central pillar
            centralPillar.entity().teleport(c.clone().add(0, chandelierY, 0));
            topCap.entity().teleport(c.clone().add(0, chandelierY + 3, 0));
            bottomGem.entity().teleport(c.clone().add(0, chandelierY - 1.5, 0));
            bottomGem.rotate(rotation * 2, 0, 1, 0).interpolation(3, 0);

            // Rotate hanging chains around center
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8 + rotation;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                // Inertia sway: chains lag behind the rotation slightly
                double inertiaSway = Math.sin(rotation * 3 + i) * 0.3;
                hangingChains.get(i).entity().teleport(
                        c.clone().add(x + inertiaSway, chandelierY - 0.5, z + inertiaSway));
            }

            // Rotate iron bars
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4 + rotation;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                ironBars.get(i).entity().teleport(
                        c.clone().add(x, chandelierY + 1.5, z));
            }

            // Elegant glowing particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(
                        c.clone().add(0, chandelierY, 0), 5, 2.5, 220, 220, 240, 1.0f);
            }

            // Sparkle at chains
            if (ticksAlive % 10 == 0) {
                int idx = ticksAlive / 10 % 8;
                Location chainLoc = hangingChains.get(idx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.END_ROD, chainLoc, 3, 0.2, 0.5, 0.2, 0.02);
            }

            // Crystal chime sound
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, chandelierY, 0),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f);
            }

            // Chain creak
            if (ticksAlive % 70 == 35) {
                DisplayBuilder.playSound(c.clone().add(0, chandelierY, 0),
                        Sound.BLOCK_CHAIN_STEP, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainChandelier(plugin); }
    }

    // ================================================================
    // 6. SKY CHAIN WEB — Horizontal web of chains that descends
    // ================================================================
    public static class SkyChainWeb extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> webChains = new ArrayList<>();
        private float webY = 10.0f;
        private final boolean[] detached = new boolean[18];
        private final float[] detachedY = new float[18];

        public SkyChainWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_chain_web", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(700);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 interlocking chains forming a web pattern at height 10, radius 5
            // 6 radial spokes
            for (int spoke = 0; spoke < 6; spoke++) {
                double angle = (Math.PI * 2 * spoke) / 6;
                for (int seg = 0; seg < 2; seg++) {
                    double dist = 1.5 + seg * 2.5;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    Location loc = center.clone().add(x, webY, z);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    chain.scale(0.4f, 0.4f, 2.0f)
                         .rotate((float) angle, 0, 1, 0)
                         .glow(150, 150, 165)
                         .interpolation(4, 0);
                    webChains.add(chain);
                    spawnedEntities.add(chain.entity());
                }
            }

            // 6 concentric ring segments connecting the spokes
            for (int ring = 0; ring < 6; ring++) {
                double angle = (Math.PI * 2 * ring) / 6 + Math.PI / 6;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                Location loc = center.clone().add(x, webY, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(2.0f, 0.3f, 0.3f)
                     .rotate((float) angle, 0, 1, 0)
                     .glow(140, 140, 155)
                     .interpolation(4, 0);
                webChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, webY, 0), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow descent
            if (webY > 1.5f) {
                webY -= 0.03f;
            }

            // Move web chains
            int chainIdx = 0;
            // Radial spokes
            for (int spoke = 0; spoke < 6; spoke++) {
                double angle = (Math.PI * 2 * spoke) / 6;
                for (int seg = 0; seg < 2; seg++) {
                    if (chainIdx < webChains.size()) {
                        if (!detached[chainIdx]) {
                            double dist = 1.5 + seg * 2.5;
                            double x = Math.cos(angle) * dist;
                            double z = Math.sin(angle) * dist;
                            webChains.get(chainIdx).entity().teleport(
                                    c.clone().add(x, webY, z));
                        } else {
                            // Detached — falling
                            detachedY[chainIdx] -= 0.5f;
                            Location current = webChains.get(chainIdx).entity().getLocation();
                            webChains.get(chainIdx).entity().teleport(
                                    current.clone().add(0, -0.5, 0));
                        }
                    }
                    chainIdx++;
                }
            }

            // Ring segments
            for (int ring = 0; ring < 6; ring++) {
                if (chainIdx < webChains.size()) {
                    if (!detached[chainIdx]) {
                        double angle = (Math.PI * 2 * ring) / 6 + Math.PI / 6;
                        double x = Math.cos(angle) * 3.0;
                        double z = Math.sin(angle) * 3.0;
                        webChains.get(chainIdx).entity().teleport(
                                c.clone().add(x, webY, z));
                    } else {
                        Location current = webChains.get(chainIdx).entity().getLocation();
                        webChains.get(chainIdx).entity().teleport(
                                current.clone().add(0, -0.5, 0));
                    }
                }
                chainIdx++;
            }

            // When web reaches player height, strands detach
            if (webY < 4.0f && ticksAlive % 15 == 0) {
                for (int i = 0; i < webChains.size(); i++) {
                    if (!detached[i] && Math.random() < 0.15) {
                        detached[i] = true;
                        detachedY[i] = webY;
                        DisplayBuilder.playSound(webChains.get(i).entity().getLocation(),
                                Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.7f);
                    }
                }
            }

            // Web shadow particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0),
                        4, 4.0, 60, 60, 70, 1.2f);
            }

            // Chain tension sounds
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, webY, 0),
                        Sound.BLOCK_CHAIN_STEP, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyChainWeb(plugin); }
    }

    // ================================================================
    // 7. CHAIN METEOR — Massive chain ball hurtling from the sky
    // ================================================================
    public static class ChainMeteor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainBall = new ArrayList<>();
        private BlockDisplayHandle netheriteCore;
        private double meteorX;
        private double meteorY;
        private double meteorZ;
        private double velocityX;
        private double velocityY;
        private double velocityZ;
        private boolean impacted = false;

        public ChainMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_meteor", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(96.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start 20 blocks up and 15 blocks to the side, angled trajectory
            meteorX = center.getX() + 15;
            meteorY = center.getY() + 25;
            meteorZ = center.getZ() + (Math.random() - 0.5) * 6;

            // Velocity: angled descent toward center
            velocityX = -0.6;
            velocityY = -0.8;
            velocityZ = (center.getZ() - meteorZ) * 0.02;

            Location spawnLoc = new Location(w, meteorX, meteorY, meteorZ);

            // Netherite core
            netheriteCore = displayBuilder.spawnBlock(spawnLoc, Material.NETHERITE_BLOCK);
            netheriteCore.scale(2.5f, 2.5f, 2.5f).glow(60, 50, 60).interpolation(2, 0);
            spawnedEntities.add(netheriteCore.entity());

            // 12 chain blocks surrounding the core
            double[][] offsets = {
                {1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}
            };
            for (double[] off : offsets) {
                Location cLoc = spawnLoc.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(cLoc, Material.CHAIN);
                chain.scale(1.2f, 1.2f, 1.2f).glow(180, 120, 60).interpolation(2, 0);
                chainBall.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Accelerate downward
            velocityY -= 0.02;

            // Move meteor
            meteorX += velocityX;
            meteorY += velocityY;
            meteorZ += velocityZ;

            Location meteorLoc = new Location(c.getWorld(), meteorX, meteorY, meteorZ);

            // Check for ground impact
            if (meteorY <= c.getY() + 1) {
                impacted = true;
                triggerImpactDamage(meteorLoc);
                return;
            }

            // Move core
            netheriteCore.entity().teleport(meteorLoc);

            // Move chain ball
            double[][] offsets = {
                {1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}
            };
            for (int i = 0; i < chainBall.size(); i++) {
                chainBall.get(i).entity().teleport(
                        meteorLoc.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]));
            }

            // Fiery trail particles
            if (ticksAlive % 1 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                        meteorLoc.clone().add(velocityX * -2, velocityY * -2, velocityZ * -2),
                        15, 0.8, 0.8, 0.8, 0.05);
                c.getWorld().spawnParticle(Particle.LAVA, meteorLoc, 3, 1.0, 1.0, 1.0, 0);
                c.getWorld().spawnParticle(Particle.SMOKE,
                        meteorLoc.clone().add(velocityX * -3, velocityY * -3, velocityZ * -3),
                        10, 1.2, 1.2, 1.2, 0.02);
            }

            // Orange/red glow trail
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(
                        meteorLoc.clone().add(velocityX * -4, velocityY * -4, velocityZ * -4),
                        8, 1.5, 255, 120, 30, 2.0f);
            }

            // Roaring wind sound
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(meteorLoc, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            World w = impactLocation.getWorld();
            if (w == null) return;

            // Enormous explosion effect
            w.spawnParticle(Particle.EXPLOSION_EMITTER, impactLocation, 3, 1, 1, 1, 0);
            w.spawnParticle(Particle.BLOCK, impactLocation, 300, 5, 2, 5, 0.5,
                    Material.CHAIN.createBlockData());
            w.spawnParticle(Particle.FLAME, impactLocation, 100, 4, 2, 4, 0.3);
            w.spawnParticle(Particle.LAVA, impactLocation, 50, 3, 1, 3, 0);

            // Chain shrapnel ring
            DisplayBuilder.particleRing(impactLocation, 6.0, Particle.CAMPFIRE_SIGNAL_SMOKE, 50, null);
            DisplayBuilder.dustParticles(impactLocation, 40, 6.0, 180, 120, 60, 2.5f);

            // Impact sounds
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
            DisplayBuilder.playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.4f);
            DisplayBuilder.playSound(impactLocation, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.3f);

            displayBuilder.removeAll();
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMeteor(plugin); }
    }

    // ================================================================
    // 8. FLOATING CHAIN ISLAND — Drifting island that drops chain bombs
    // ================================================================
    public static class FloatingChainIsland extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> islandBase = new ArrayList<>();
        private final List<BlockDisplayHandle> hangingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> towerChains = new ArrayList<>();
        private final List<BlockDisplayHandle> bombs = new ArrayList<>();
        private float islandY = 12.0f;
        private float driftAngle = 0;
        private int bombTimer = 0;

        public FloatingChainIsland(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_chain_island", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(900);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Flat iron base (6 blocks in a platform shape)
            double[][] baseParts = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}};
            for (double[] bp : baseParts) {
                Location loc = center.clone().add(bp[0], islandY, bp[2]);
                BlockDisplayHandle base = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                base.scale(1.5f, 0.5f, 1.5f).glow(170, 170, 180).interpolation(4, 0);
                islandBase.add(base);
                spawnedEntities.add(base.entity());
            }

            // Hanging chains below the island (6 chains)
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                Location cLoc = center.clone().add(x, islandY - 1.5, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(cLoc, Material.CHAIN);
                float len = 2.0f + (i % 3) * 0.8f;
                chain.scale(0.3f, len, 0.3f).glow(140, 140, 150).interpolation(4, 0);
                hangingChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // Small chain tower on top (3 blocks)
            for (int i = 0; i < 3; i++) {
                Location tLoc = center.clone().add(0, islandY + 0.5 + i, 0);
                Material mat = i == 2 ? Material.HEAVY_CORE : Material.CHAIN;
                BlockDisplayHandle tower = displayBuilder.spawnBlock(tLoc, mat);
                tower.scale(0.5f, 1.0f, 0.5f).glow(160, 160, 175).interpolation(4, 0);
                towerChains.add(tower);
                spawnedEntities.add(tower.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, islandY, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift in a slow circle overhead
            driftAngle += 0.008f;
            double driftX = Math.cos(driftAngle) * 3.0;
            double driftZ = Math.sin(driftAngle) * 3.0;

            // Move island base
            double[][] baseParts = {{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}};
            for (int i = 0; i < islandBase.size(); i++) {
                islandBase.get(i).entity().teleport(
                        c.clone().add(baseParts[i][0] + driftX, islandY, baseParts[i][2] + driftZ));
            }

            // Move hanging chains
            for (int i = 0; i < hangingChains.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6;
                double x = Math.cos(angle) * 1.2 + driftX;
                double z = Math.sin(angle) * 1.2 + driftZ;
                // Sway below
                double sway = Math.sin(ticksAlive * 0.05 + i) * 0.3;
                hangingChains.get(i).entity().teleport(
                        c.clone().add(x + sway, islandY - 1.5, z + sway));
            }

            // Move tower
            for (int i = 0; i < towerChains.size(); i++) {
                towerChains.get(i).entity().teleport(
                        c.clone().add(driftX, islandY + 0.5 + i, driftZ));
            }

            // Shadow particles
            if (ticksAlive % 5 == 0) {
                Location shadow = c.clone().add(driftX, 0.3, driftZ);
                DisplayBuilder.dustParticles(shadow, 3, 1.5, 50, 50, 55, 1.5f);
            }

            // Drop chain bombs periodically
            bombTimer++;
            if (bombTimer >= 80 && bombs.size() < 6) {
                bombTimer = 0;
                Location bombStart = c.clone().add(driftX, islandY - 2, driftZ);
                BlockDisplayHandle bomb = displayBuilder.spawnBlock(bombStart, Material.CHAIN);
                bomb.scale(0.8f, 0.8f, 0.8f).glow(255, 140, 60).interpolation(2, 0);
                bombs.add(bomb);
                spawnedEntities.add(bomb.entity());
                DisplayBuilder.playSound(bombStart, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.8f);
            }

            // Move falling bombs
            List<BlockDisplayHandle> toRemove = new ArrayList<>();
            for (BlockDisplayHandle bomb : bombs) {
                Location bLoc = bomb.entity().getLocation();
                if (bLoc.getY() > c.getY() + 0.5) {
                    bomb.entity().teleport(bLoc.add(0, -0.4, 0));
                    // Trail particles
                    DisplayBuilder.dustParticles(bLoc, 2, 0.3, 200, 100, 40, 0.8f);
                } else {
                    // Impact
                    triggerImpactDamage(bLoc);
                    DisplayBuilder.playSound(bLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.8f);
                    c.getWorld().spawnParticle(Particle.BLOCK, bLoc, 30, 1, 0.5, 1, 0.3,
                            Material.CHAIN.createBlockData());
                    bomb.entity().remove();
                    toRemove.add(bomb);
                }
            }
            bombs.removeAll(toRemove);

            // Ambient hum
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(c.clone().add(driftX, islandY, driftZ),
                        Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingChainIsland(plugin); }
    }

    // ================================================================
    // 9. CHAIN GUILLOTINE — Frame with dropping blade, resets 3 times
    // ================================================================
    public static class ChainGuillotine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> framePosts = new ArrayList<>();
        private BlockDisplayHandle crossbar;
        private BlockDisplayHandle blade;
        private final List<BlockDisplayHandle> frameChains = new ArrayList<>();
        private float bladeY;
        private int dropCount = 0;
        private boolean bladeDropping = false;
        private boolean bladeResetting = false;
        private int pauseTimer = 0;
        private static final float TOP_Y = 6.5f;
        private static final float BOTTOM_Y = 0.5f;

        public ChainGuillotine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_guillotine", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            bladeY = TOP_Y;

            // 2 upright posts (6 blocks tall each) made of chains
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 6; y++) {
                    Location pLoc = center.clone().add(side * 1.5, y, 0);
                    BlockDisplayHandle post = displayBuilder.spawnBlock(pLoc, Material.CHAIN);
                    post.scale(0.5f, 1.0f, 0.5f).glow(140, 140, 150).interpolation(3, 0);
                    framePosts.add(post);
                    spawnedEntities.add(post.entity());
                }
            }

            // Crossbar at top
            crossbar = displayBuilder.spawnBlock(
                    center.clone().add(0, 6.5, 0), Material.IRON_BLOCK);
            crossbar.scale(4.0f, 0.5f, 0.5f).glow(180, 180, 195).interpolation(3, 0);
            spawnedEntities.add(crossbar.entity());

            // Blade — wide flat iron block
            blade = displayBuilder.spawnBlock(
                    center.clone().add(0, bladeY, 0), Material.IRON_BLOCK);
            blade.scale(2.5f, 0.3f, 1.2f).glow(220, 220, 230).interpolation(1, 0);
            spawnedEntities.add(blade.entity());

            // Decorative guide chains on posts
            for (int side = -1; side <= 1; side += 2) {
                Location gLoc = center.clone().add(side * 1.2, 3, 0);
                BlockDisplayHandle guide = displayBuilder.spawnBlock(gLoc, Material.CHAIN);
                guide.scale(0.2f, 5.0f, 0.2f).glow(120, 120, 130).interpolation(3, 0);
                frameChains.add(guide);
                spawnedEntities.add(guide.entity());
            }

            // Base block
            BlockDisplayHandle base = displayBuilder.spawnBlock(
                    center.clone().add(0, -0.3, 0), Material.DEEPSLATE);
            base.scale(3.5f, 0.4f, 2.0f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(base.entity());

            DisplayBuilder.playSound(center.clone().add(0, 3, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.4f);

            // Start first drop after a pause
            pauseTimer = 60;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (dropCount >= 3) return; // Done after 3 drops

            if (pauseTimer > 0) {
                pauseTimer--;
                // Tension building: blade vibrates
                if (pauseTimer < 20 && pauseTimer > 0) {
                    float shake = (float) Math.sin(pauseTimer * 5) * 0.05f;
                    blade.entity().teleport(c.clone().add(shake, bladeY, 0));
                    if (pauseTimer % 5 == 0) {
                        DisplayBuilder.playSound(c.clone().add(0, bladeY, 0),
                                Sound.BLOCK_CHAIN_STEP, 0.6f, 1.2f + (20 - pauseTimer) * 0.05f);
                    }
                }
                if (pauseTimer == 0) {
                    bladeDropping = true;
                    DisplayBuilder.playSound(c.clone().add(0, bladeY, 0),
                            Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f);
                }
                return;
            }

            if (bladeDropping) {
                // Extreme speed drop
                bladeY -= 1.0f;
                blade.entity().teleport(c.clone().add(0, bladeY, 0));

                // Falling particles
                c.getWorld().spawnParticle(Particle.SMOKE,
                        c.clone().add(0, bladeY + 1, 0), 5, 0.8, 0.3, 0.3, 0.05);

                if (bladeY <= BOTTOM_Y) {
                    bladeY = BOTTOM_Y;
                    bladeDropping = false;
                    bladeResetting = true;
                    dropCount++;

                    // Impact
                    triggerImpactDamage(c.clone().add(0, 0.5, 0));
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.5, 0),
                            40, 1.5, 0.3, 0.8, 0.3, Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0),
                            15, 2.0, 200, 200, 210, 1.5f);
                }
                return;
            }

            if (bladeResetting) {
                // Slow reset upward with winch sound
                bladeY += 0.1f;
                blade.entity().teleport(c.clone().add(0, bladeY, 0));

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, bladeY, 0),
                            Sound.BLOCK_CHAIN_STEP, 0.5f, 0.6f);
                }

                if (bladeY >= TOP_Y) {
                    bladeY = TOP_Y;
                    bladeResetting = false;
                    if (dropCount < 3) {
                        pauseTimer = 40; // Shorter pause for subsequent drops
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGuillotine(plugin); }
    }

    // ================================================================
    // 10. SKY CHAIN RAIN — Cloud cluster that rains chain drops
    // ================================================================
    public static class SkyChainRain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cloudBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> raindrops = new ArrayList<>();
        private final List<Float> raindropY = new ArrayList<>();
        private int spawnTimer = 0;

        public SkyChainRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_chain_rain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Cloud cluster of 10 dark iron blocks at height 18
            double[][] cloudOffsets = {
                {0, 0, 0}, {1.5, 0.3, 0}, {-1.5, 0.2, 0}, {0, 0.1, 1.5}, {0, 0.3, -1.5},
                {1, 0.5, 1}, {-1, 0.4, -1}, {2, -0.1, 0.5}, {-0.5, 0.6, 2}, {0.5, 0.2, -2}
            };
            for (double[] off : cloudOffsets) {
                Location loc = center.clone().add(off[0], 18 + off[1], off[2]);
                BlockDisplayHandle cloud = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float size = 1.5f + (float) (Math.random() * 1.0);
                cloud.scale(size, 0.8f, size).glow(50, 50, 60).interpolation(5, 0);
                cloudBlocks.add(cloud);
                spawnedEntities.add(cloud.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 18, 0),
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gently move cloud
            double cloudDriftX = Math.sin(ticksAlive * 0.01) * 0.5;
            double cloudDriftZ = Math.cos(ticksAlive * 0.008) * 0.5;

            double[][] cloudOffsets = {
                {0, 0, 0}, {1.5, 0.3, 0}, {-1.5, 0.2, 0}, {0, 0.1, 1.5}, {0, 0.3, -1.5},
                {1, 0.5, 1}, {-1, 0.4, -1}, {2, -0.1, 0.5}, {-0.5, 0.6, 2}, {0.5, 0.2, -2}
            };
            for (int i = 0; i < cloudBlocks.size(); i++) {
                cloudBlocks.get(i).entity().teleport(
                        c.clone().add(cloudOffsets[i][0] + cloudDriftX,
                                18 + cloudOffsets[i][1], cloudOffsets[i][2] + cloudDriftZ));
            }

            // Spawn new raindrops from cloud
            spawnTimer++;
            if (spawnTimer >= 12 && raindrops.size() < 8) {
                spawnTimer = 0;
                double rx = (Math.random() - 0.5) * 4.0 + cloudDriftX;
                double rz = (Math.random() - 0.5) * 4.0 + cloudDriftZ;
                Location rLoc = c.clone().add(rx, 17.5, rz);
                BlockDisplayHandle drop = displayBuilder.spawnBlock(rLoc, Material.CHAIN);
                drop.scale(0.4f, 1.5f, 0.4f).glow(160, 160, 175).interpolation(2, 0);
                raindrops.add(drop);
                raindropY.add(17.5f);
                spawnedEntities.add(drop.entity());

                DisplayBuilder.playSound(rLoc, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.0f + (float) Math.random() * 0.5f);
            }

            // Move raindrops downward
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < raindrops.size(); i++) {
                float y = raindropY.get(i) - 0.6f;
                raindropY.set(i, y);
                Location dLoc = raindrops.get(i).entity().getLocation();
                raindrops.get(i).entity().teleport(dLoc.clone().add(0, -0.6, 0));

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(dLoc, 2, 0.2, 140, 140, 155, 0.8f);
                }

                // Despawn at ground level
                if (y <= c.getY() + 0.5 - c.getY()) {
                    raindrops.get(i).entity().remove();
                    toRemove.add(i);

                    // Landing splash
                    DisplayBuilder.dustParticles(dLoc, 5, 0.5, 160, 160, 175, 1.0f);
                    DisplayBuilder.playSound(dLoc, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                }
            }

            // Remove in reverse order to maintain indices
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                int idx = toRemove.get(i);
                raindrops.remove(idx);
                raindropY.remove(idx);
            }

            // Thunder rumble
            if (ticksAlive % 200 == 100) {
                DisplayBuilder.playSound(c.clone().add(0, 18, 0),
                        Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.3f);
            }

            // Cloud flash
            if (ticksAlive % 150 == 0) {
                for (BlockDisplayHandle cloud : cloudBlocks) {
                    cloud.glow(200, 200, 210);
                }
            } else if (ticksAlive % 150 == 5) {
                for (BlockDisplayHandle cloud : cloudBlocks) {
                    cloud.glow(50, 50, 60);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyChainRain(plugin); }
    }

    // ================================================================
    // 11. CHAIN SKYWHEEL — Vertical ferris wheel of chains
    // ================================================================
    public static class ChainSkywheel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wheelChains = new ArrayList<>();
        private final List<BlockDisplayHandle> carriages = new ArrayList<>();
        private BlockDisplayHandle hubBlock;
        private BlockDisplayHandle hubBlock2;
        private float wheelAngle = 0;
        private static final float WHEEL_Y = 8.0f;
        private static final float WHEEL_RADIUS = 4.0f;

        public ChainSkywheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_skywheel", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Hub at center of wheel
            hubBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, WHEEL_Y, 0), Material.IRON_BLOCK);
            hubBlock.scale(1.0f, 1.0f, 0.5f).glow(180, 180, 195).interpolation(3, 0);
            spawnedEntities.add(hubBlock.entity());

            hubBlock2 = displayBuilder.spawnBlock(
                    center.clone().add(0, WHEEL_Y, 0), Material.HEAVY_CORE);
            hubBlock2.scale(0.6f, 0.6f, 0.6f).glow(200, 200, 210).interpolation(3, 0);
            spawnedEntities.add(hubBlock2.entity());

            // 12 chains in a circle (on XY plane, vertical wheel)
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double x = Math.cos(angle) * WHEEL_RADIUS;
                double y = Math.sin(angle) * WHEEL_RADIUS;
                Location loc = center.clone().add(x, WHEEL_Y + y, 0);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.5f, 1.0f, 0.5f).glow(150, 150, 165).interpolation(3, 0);
                wheelChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // 4 carriages at 90-degree intervals
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double x = Math.cos(angle) * WHEEL_RADIUS;
                double y = Math.sin(angle) * WHEEL_RADIUS;
                Location loc = center.clone().add(x, WHEEL_Y + y, 0);
                BlockDisplayHandle carriage = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                carriage.scale(1.0f, 0.8f, 1.0f).glow(200, 200, 215).interpolation(3, 0);
                carriages.add(carriage);
                spawnedEntities.add(carriage.entity());
            }

            // Support stands
            for (int side = -1; side <= 1; side += 2) {
                Location sLoc = center.clone().add(0, WHEEL_Y - WHEEL_RADIUS - 1, side * 0.8);
                BlockDisplayHandle stand = displayBuilder.spawnBlock(sLoc, Material.IRON_BLOCK);
                stand.scale(0.4f, WHEEL_RADIUS + 1, 0.4f).glow(120, 120, 130).interpolation(3, 0);
                spawnedEntities.add(stand.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, WHEEL_Y, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow majestic rotation
            wheelAngle += 0.02f;

            // Move wheel chains
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12 + wheelAngle;
                double x = Math.cos(angle) * WHEEL_RADIUS;
                double y = Math.sin(angle) * WHEEL_RADIUS;
                wheelChains.get(i).entity().teleport(
                        c.clone().add(x, WHEEL_Y + y, 0));
            }

            // Move carriages
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + wheelAngle;
                double x = Math.cos(angle) * WHEEL_RADIUS;
                double y = Math.sin(angle) * WHEEL_RADIUS;
                carriages.get(i).entity().teleport(
                        c.clone().add(x, WHEEL_Y + y, 0));
            }

            // Damage zone at the bottom of the wheel where carriages pass
            // (handled by the continuous damage radius centered on the attack center)

            // Metallic creaking
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, WHEEL_Y, 0),
                        Sound.BLOCK_CHAIN_STEP, 0.7f, 0.5f);
            }

            // Sparks at the hub
            if (ticksAlive % 15 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, WHEEL_Y, 0), 3, 0.3, 0.3, 0.3, 0.1);
            }

            // Glow pulse on carriages
            if (ticksAlive % 40 == 0) {
                int pulseIdx = (ticksAlive / 40) % 4;
                for (int i = 0; i < 4; i++) {
                    if (i == pulseIdx) {
                        carriages.get(i).glow(255, 255, 255);
                    } else {
                        carriages.get(i).glow(200, 200, 215);
                    }
                }
            }

            // Iron groan
            if (ticksAlive % 120 == 60) {
                DisplayBuilder.playSound(c.clone().add(0, WHEEL_Y, 0),
                        Sound.BLOCK_IRON_DOOR_CLOSE, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSkywheel(plugin); }
    }

    // ================================================================
    // 12. HOVERING CHAIN CUBE — Rotating cube frame of chains
    // ================================================================
    public static class HoveringChainCube extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> edgeChains = new ArrayList<>();
        private final List<BlockDisplayHandle> vertexGlows = new ArrayList<>();
        private float cubeY = 6.0f;
        private float rotX = 0, rotY = 0, rotZ = 0;
        private static final float HALF_SIZE = 1.5f;

        // 12 edges of a cube defined as pairs of vertex indices
        private static final int[][] EDGE_PAIRS = {
            {0,1},{1,3},{3,2},{2,0}, // bottom face
            {4,5},{5,7},{7,6},{6,4}, // top face
            {0,4},{1,5},{2,6},{3,7}  // vertical edges
        };

        // 8 vertices of a unit cube
        private static final float[][] VERTICES = {
            {-1,-1,-1},{1,-1,-1},{-1,-1,1},{1,-1,1},
            {-1,1,-1},{1,1,-1},{-1,1,1},{1,1,1}
        };

        public HoveringChainCube(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hovering_chain_cube", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 edge chains
            for (int i = 0; i < 12; i++) {
                int[] edge = EDGE_PAIRS[i];
                float[] v1 = VERTICES[edge[0]];
                float[] v2 = VERTICES[edge[1]];
                float mx = (v1[0] + v2[0]) / 2 * HALF_SIZE;
                float my = (v1[1] + v2[1]) / 2 * HALF_SIZE;
                float mz = (v1[2] + v2[2]) / 2 * HALF_SIZE;
                Location loc = center.clone().add(mx, cubeY + my, mz);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                // Scale based on edge direction
                float sx = (v1[0] != v2[0]) ? HALF_SIZE * 2 : 0.3f;
                float sy = (v1[1] != v2[1]) ? HALF_SIZE * 2 : 0.3f;
                float sz = (v1[2] != v2[2]) ? HALF_SIZE * 2 : 0.3f;
                chain.scale(sx, sy, sz).glow(150, 150, 170).interpolation(3, 0);
                edgeChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            // 8 vertex glow blocks
            for (float[] v : VERTICES) {
                Location vLoc = center.clone().add(v[0] * HALF_SIZE, cubeY + v[1] * HALF_SIZE, v[2] * HALF_SIZE);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(vLoc, Material.IRON_BLOCK);
                glow.scale(0.3f, 0.3f, 0.3f).glow(220, 220, 240).interpolation(3, 0);
                vertexGlows.add(glow);
                spawnedEntities.add(glow.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, cubeY, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow descent
            if (cubeY > 2.0f) {
                cubeY -= 0.015f;
            }

            // Rotate on all 3 axes
            rotX += 0.012f;
            rotY += 0.018f;
            rotZ += 0.009f;

            // Apply rotation to each vertex position
            float cosX = (float) Math.cos(rotX), sinX = (float) Math.sin(rotX);
            float cosY = (float) Math.cos(rotY), sinY = (float) Math.sin(rotY);
            float cosZ = (float) Math.cos(rotZ), sinZ = (float) Math.sin(rotZ);

            float[][] rotated = new float[8][3];
            for (int i = 0; i < 8; i++) {
                float x = VERTICES[i][0] * HALF_SIZE;
                float y = VERTICES[i][1] * HALF_SIZE;
                float z = VERTICES[i][2] * HALF_SIZE;

                // Rotate X
                float y1 = y * cosX - z * sinX;
                float z1 = y * sinX + z * cosX;
                // Rotate Y
                float x2 = x * cosY + z1 * sinY;
                float z2 = -x * sinY + z1 * cosY;
                // Rotate Z
                float x3 = x2 * cosZ - y1 * sinZ;
                float y3 = x2 * sinZ + y1 * cosZ;

                rotated[i] = new float[]{x3, y3, z2};
            }

            // Move edge chains to midpoints of rotated edges
            for (int i = 0; i < 12; i++) {
                int[] edge = EDGE_PAIRS[i];
                float mx = (rotated[edge[0]][0] + rotated[edge[1]][0]) / 2;
                float my = (rotated[edge[0]][1] + rotated[edge[1]][1]) / 2;
                float mz = (rotated[edge[0]][2] + rotated[edge[1]][2]) / 2;
                edgeChains.get(i).entity().teleport(c.clone().add(mx, cubeY + my, mz));
            }

            // Move vertex glows
            for (int i = 0; i < 8; i++) {
                vertexGlows.get(i).entity().teleport(
                        c.clone().add(rotated[i][0], cubeY + rotated[i][1], rotated[i][2]));
            }

            // Glowing particles at vertices
            if (ticksAlive % 8 == 0) {
                int vIdx = ticksAlive / 8 % 8;
                Location vLoc = vertexGlows.get(vIdx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.END_ROD, vLoc, 2, 0.1, 0.1, 0.1, 0.02);
            }

            // Shadow below
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0),
                        3, 2.0, 50, 50, 60, 1.0f);
            }

            // Metallic hum
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, cubeY, 0),
                        Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.8f);
            }

            // Chain rattle
            if (ticksAlive % 50 == 25) {
                DisplayBuilder.playSound(c.clone().add(0, cubeY, 0),
                        Sound.BLOCK_CHAIN_STEP, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HoveringChainCube(plugin); }
    }

    // ================================================================
    // 13. CHAIN BOMBER — Aircraft shape that flies overhead dropping bombs
    // ================================================================
    public static class ChainBomber extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bodyChains = new ArrayList<>();
        private final List<BlockDisplayHandle> wingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> tailChains = new ArrayList<>();
        private final List<BlockDisplayHandle> activeBombs = new ArrayList<>();
        private final List<Float> bombY = new ArrayList<>();
        private final List<Double> bombX = new ArrayList<>();
        private final List<Double> bombZ = new ArrayList<>();
        private double bomberX;
        private double bomberZ;
        private double dirX;
        private double dirZ;
        private int bombsDropped = 0;
        private static final float BOMBER_Y = 14.0f;

        public ChainBomber(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_bomber", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start at one edge, fly to the other
            bomberX = center.getX() - 20;
            bomberZ = center.getZ();
            dirX = 0.35;
            dirZ = 0;

            Location spawnLoc = new Location(w, bomberX, center.getY() + BOMBER_Y, bomberZ);

            // Elongated body of 6 chains
            for (int i = 0; i < 6; i++) {
                Location bLoc = spawnLoc.clone().add(-i * 1.2, 0, 0);
                BlockDisplayHandle body = displayBuilder.spawnBlock(bLoc, Material.CHAIN);
                float bodyScale = (i < 2) ? 1.0f : (i < 4) ? 0.8f : 0.6f;
                body.scale(1.2f, bodyScale, bodyScale).glow(140, 140, 155).interpolation(3, 0);
                bodyChains.add(body);
                spawnedEntities.add(body.entity());
            }

            // 2 wing chains (one each side at position 2)
            for (int side = -1; side <= 1; side += 2) {
                Location wLoc = spawnLoc.clone().add(-2.4, 0, side * 2.5);
                BlockDisplayHandle wing = displayBuilder.spawnBlock(wLoc, Material.IRON_BLOCK);
                wing.scale(1.5f, 0.3f, 2.5f).glow(170, 170, 185).interpolation(3, 0);
                wingChains.add(wing);
                spawnedEntities.add(wing.entity());
            }

            // Tail — 2 chains at the back, angled up
            for (int i = 0; i < 2; i++) {
                Location tLoc = spawnLoc.clone().add(-6.5 - i * 0.8, 0.5 + i * 0.5, 0);
                BlockDisplayHandle tail = displayBuilder.spawnBlock(tLoc, Material.CHAIN);
                tail.scale(0.8f, 0.5f, 0.8f).glow(130, 130, 145).interpolation(3, 0);
                tailChains.add(tail);
                spawnedEntities.add(tail.entity());
            }

            // Nose cone (iron block)
            BlockDisplayHandle nose = displayBuilder.spawnBlock(
                    spawnLoc.clone().add(1, 0, 0), Material.IRON_BLOCK);
            nose.scale(1.0f, 0.8f, 0.8f).glow(200, 200, 215).interpolation(3, 0);
            bodyChains.add(nose); // treat as body for movement
            spawnedEntities.add(nose.entity());

            DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Move bomber forward
            bomberX += dirX;
            bomberZ += dirZ;

            Location bomberLoc = new Location(c.getWorld(), bomberX, c.getY() + BOMBER_Y, bomberZ);

            // Move body
            for (int i = 0; i < 6; i++) {
                bodyChains.get(i).entity().teleport(
                        bomberLoc.clone().add(-i * 1.2, 0, 0));
            }
            // Nose
            if (bodyChains.size() > 6) {
                bodyChains.get(6).entity().teleport(bomberLoc.clone().add(1, 0, 0));
            }

            // Wings
            for (int i = 0; i < wingChains.size(); i++) {
                int side = (i == 0) ? -1 : 1;
                wingChains.get(i).entity().teleport(
                        bomberLoc.clone().add(-2.4, 0, side * 2.5));
            }

            // Tail
            for (int i = 0; i < tailChains.size(); i++) {
                tailChains.get(i).entity().teleport(
                        bomberLoc.clone().add(-6.5 - i * 0.8, 0.5 + i * 0.5, 0));
            }

            // Engine exhaust particles
            if (ticksAlive % 2 == 0) {
                Location exhaust = bomberLoc.clone().add(-7.5, 0, 0);
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, exhaust,
                        3, 0.3, 0.1, 0.3, 0.01);
                c.getWorld().spawnParticle(Particle.SMOKE, exhaust,
                        5, 0.5, 0.2, 0.5, 0.02);
            }

            // Engine rumble sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(bomberLoc, Sound.ENTITY_MINECART_RIDING, 0.8f, 0.3f);
            }

            // Drop bombs at intervals (6 total, every 50 ticks starting at tick 40)
            if (ticksAlive >= 40 && bombsDropped < 6 && (ticksAlive - 40) % 50 == 0) {
                bombsDropped++;
                Location bombStart = bomberLoc.clone().add(0, -1, 0);
                BlockDisplayHandle bomb = displayBuilder.spawnBlock(bombStart, Material.CHAIN);
                bomb.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 50).interpolation(2, 0);
                activeBombs.add(bomb);
                bombY.add((float) (c.getY() + BOMBER_Y - 1));
                bombX.add(bomberX);
                bombZ.add(bomberZ);
                spawnedEntities.add(bomb.entity());

                DisplayBuilder.playSound(bombStart, Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 0.5f);
            }

            // Animate falling bombs
            List<Integer> removeIdx = new ArrayList<>();
            for (int i = 0; i < activeBombs.size(); i++) {
                float by = bombY.get(i) - 0.5f;
                bombY.set(i, by);
                activeBombs.get(i).entity().teleport(
                        new Location(c.getWorld(), bombX.get(i), by, bombZ.get(i)));

                // Trail
                DisplayBuilder.dustParticles(
                        new Location(c.getWorld(), bombX.get(i), by + 1, bombZ.get(i)),
                        2, 0.2, 200, 80, 30, 0.8f);

                // Ground impact
                if (by <= c.getY() + 0.5) {
                    Location impactLoc = new Location(c.getWorld(), bombX.get(i), c.getY(), bombZ.get(i));
                    triggerImpactDamage(impactLoc);

                    // Explosion effects
                    c.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 3, 1, 0.5, 1, 0);
                    c.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 40, 2, 1, 2, 0.3,
                            Material.CHAIN.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.7f);

                    activeBombs.get(i).entity().remove();
                    removeIdx.add(i);
                }
            }

            for (int i = removeIdx.size() - 1; i >= 0; i--) {
                int idx = removeIdx.get(i);
                activeBombs.remove(idx);
                bombY.remove(idx);
                bombX.remove(idx);
                bombZ.remove(idx);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainBomber(plugin); }
    }

    // ================================================================
    // 14. SKY CHAIN LATTICE — Geodesic dome lattice that contracts
    // ================================================================
    public static class SkyChainLattice extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> latticeChains = new ArrayList<>();
        private float domeRadius = 6.0f;
        private float domeY = 8.0f;
        private final double[][] chainAngles = new double[20][2]; // [azimuth, elevation]

        public SkyChainLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_chain_lattice", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(800);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 chains forming a geodesic hemisphere using golden angle distribution
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 20; i++) {
                double elevation = Math.acos(1 - (double) i / 20); // 0 to ~pi/2 for hemisphere
                double azimuth = goldenAngle * i;
                chainAngles[i][0] = azimuth;
                chainAngles[i][1] = elevation;

                double x = Math.sin(elevation) * Math.cos(azimuth) * domeRadius;
                double y = Math.cos(elevation) * domeRadius;
                double z = Math.sin(elevation) * Math.sin(azimuth) * domeRadius;

                Location loc = center.clone().add(x, domeY + y - domeRadius, z);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(loc, Material.CHAIN);
                chain.scale(0.5f, 0.5f, 2.0f)
                     .rotate((float) azimuth, 0, 1, 0)
                     .glow(150, 150, 170)
                     .interpolation(4, 0);
                latticeChains.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, domeY, 0),
                    Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
            DisplayBuilder.playSound(center.clone().add(0, domeY, 0),
                    Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract dome
            if (domeRadius > 1.5f) {
                domeRadius -= 0.012f;
            }

            // Descend
            if (domeY > 1.0f) {
                domeY -= 0.02f;
            }

            // Move lattice chains
            for (int i = 0; i < 20; i++) {
                double azimuth = chainAngles[i][0];
                double elevation = chainAngles[i][1];

                double x = Math.sin(elevation) * Math.cos(azimuth) * domeRadius;
                double y = Math.cos(elevation) * domeRadius;
                double z = Math.sin(elevation) * Math.sin(azimuth) * domeRadius;

                latticeChains.get(i).entity().teleport(
                        c.clone().add(x, domeY + y - domeRadius, z));
            }

            // Pulsing glow on triangular sections
            if (ticksAlive % 20 == 0) {
                int pulseGroup = (ticksAlive / 20) % 5;
                for (int i = 0; i < 20; i++) {
                    if (i / 4 == pulseGroup) {
                        latticeChains.get(i).glow(240, 200, 255);
                    } else {
                        latticeChains.get(i).glow(150, 150, 170);
                    }
                }
            }

            // Inner dome particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, domeY - domeRadius / 2, 0),
                        5, domeRadius * 0.6, 160, 160, 180, 1.0f);
            }

            // Contraction sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, domeY, 0),
                        Sound.BLOCK_CHAIN_STEP, 0.8f, 0.5f + (6.0f - domeRadius) * 0.1f);
            }

            // Energy building sound as it gets smaller
            if (domeRadius < 3.0f && ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, domeY, 0),
                        Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.8f + (3.0f - domeRadius) * 0.3f);
            }

            // Ring particles at dome base
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, domeY - domeRadius, 0),
                        domeRadius, Particle.CRIT, 12, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyChainLattice(plugin); }
    }

    // ================================================================
    // 15. CHAIN COMET — Streaking V-formation comet across the sky
    // ================================================================
    public static class ChainComet extends BlockDisplayAttack {

        private BlockDisplayHandle cometHead;
        private final List<BlockDisplayHandle> vFormation = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisPieces = new ArrayList<>();
        private final List<Float> debrisY = new ArrayList<>();
        private final List<Double> debrisX = new ArrayList<>();
        private final List<Double> debrisZ = new ArrayList<>();
        private double cometX;
        private double cometY;
        private double cometZ;
        private double velocityX;
        private double velocityZ;
        private int debrisSpawned = 0;

        public ChainComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_comet", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(500);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Arc from one side to the other at high altitude
            cometX = center.getX() - 25;
            cometY = center.getY() + 18;
            cometZ = center.getZ() - 5;
            velocityX = 0.7;
            velocityZ = 0.2;

            Location spawnLoc = new Location(w, cometX, cometY, cometZ);

            // Bright netherite head
            cometHead = displayBuilder.spawnBlock(spawnLoc, Material.NETHERITE_BLOCK);
            cometHead.scale(1.8f, 1.8f, 1.8f).glow(255, 200, 150).interpolation(2, 0);
            spawnedEntities.add(cometHead.entity());

            // 10 chains in V-formation trailing behind
            for (int i = 0; i < 10; i++) {
                int side = (i % 2 == 0) ? -1 : 1;
                int depth = i / 2 + 1;
                double ox = -depth * 1.5;
                double oz = side * depth * 0.6;
                Location cLoc = spawnLoc.clone().add(ox, -depth * 0.3, oz);
                BlockDisplayHandle chain = displayBuilder.spawnBlock(cLoc, Material.CHAIN);
                float fadeScale = 1.0f - depth * 0.1f;
                chain.scale(fadeScale, fadeScale, fadeScale)
                     .glow(200 - depth * 15, 160 - depth * 15, 120 - depth * 10)
                     .interpolation(2, 0);
                vFormation.add(chain);
                spawnedEntities.add(chain.entity());
            }

            DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Move comet in arc
            cometX += velocityX;
            cometZ += velocityZ;
            // Parabolic arc: slight rise then fall
            cometY = c.getY() + 18 + Math.sin(ticksAlive * 0.02) * 3;

            Location cometLoc = new Location(c.getWorld(), cometX, cometY, cometZ);

            // Move head
            cometHead.entity().teleport(cometLoc);

            // Move V-formation
            for (int i = 0; i < vFormation.size(); i++) {
                int side = (i % 2 == 0) ? -1 : 1;
                int depth = i / 2 + 1;
                double ox = -depth * 1.5;
                double oz = side * depth * 0.6;
                vFormation.get(i).entity().teleport(
                        cometLoc.clone().add(ox, -depth * 0.3, oz));
            }

            // Long particle trail
            if (ticksAlive % 1 == 0) {
                for (int t = 1; t <= 3; t++) {
                    Location trailLoc = cometLoc.clone().add(
                            -velocityX * t * 3, -0.5 * t, -velocityZ * t * 3);
                    c.getWorld().spawnParticle(Particle.END_ROD, trailLoc,
                            3, 0.3, 0.3, 0.3, 0.02);
                    DisplayBuilder.dustParticles(trailLoc, 4, 0.8,
                            255, 180, 80, 1.5f);
                }
                c.getWorld().spawnParticle(Particle.FLAME, cometLoc, 5, 0.5, 0.5, 0.5, 0.05);
            }

            // When comet passes overhead, drop chain debris
            double distToCenter = Math.sqrt(
                    Math.pow(cometX - c.getX(), 2) + Math.pow(cometZ - c.getZ(), 2));
            if (distToCenter < 12 && ticksAlive % 10 == 0 && debrisSpawned < 8) {
                debrisSpawned++;
                double dx = (Math.random() - 0.5) * 4;
                double dz = (Math.random() - 0.5) * 4;
                Location dLoc = cometLoc.clone().add(dx, -2, dz);
                BlockDisplayHandle debris = displayBuilder.spawnBlock(dLoc, Material.CHAIN);
                debris.scale(0.5f, 0.5f, 0.5f).glow(200, 140, 80).interpolation(2, 0);
                debrisPieces.add(debris);
                debrisY.add((float) dLoc.getY());
                debrisX.add(dLoc.getX());
                debrisZ.add(dLoc.getZ());
                spawnedEntities.add(debris.entity());

                DisplayBuilder.playSound(dLoc, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.8f);
            }

            // Animate falling debris
            List<Integer> removeIdx = new ArrayList<>();
            for (int i = 0; i < debrisPieces.size(); i++) {
                float dy = debrisY.get(i) - 0.4f;
                debrisY.set(i, dy);
                debrisPieces.get(i).entity().teleport(
                        new Location(c.getWorld(), debrisX.get(i), dy, debrisZ.get(i)));

                // Trail
                DisplayBuilder.dustParticles(
                        new Location(c.getWorld(), debrisX.get(i), dy + 1, debrisZ.get(i)),
                        2, 0.2, 180, 120, 60, 0.8f);

                // Impact
                if (dy <= c.getY() + 0.5) {
                    Location impactLoc = new Location(c.getWorld(), debrisX.get(i), c.getY(), debrisZ.get(i));
                    triggerImpactDamage(impactLoc);

                    c.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 20, 1, 0.5, 1, 0.2,
                            Material.CHAIN.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.8f);

                    debrisPieces.get(i).entity().remove();
                    removeIdx.add(i);
                }
            }

            for (int i = removeIdx.size() - 1; i >= 0; i--) {
                int idx = removeIdx.get(i);
                debrisPieces.remove(idx);
                debrisY.remove(idx);
                debrisX.remove(idx);
                debrisZ.remove(idx);
            }

            // Comet whoosh sound
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(cometLoc, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.4f);
            }

            // Bright flash as comet passes closest point
            if (distToCenter < 3 && ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.FLASH, cometLoc, 1, 0, 0, 0, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainComet(plugin); }
    }
}
