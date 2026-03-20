package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Block Display — STARFALL METEORS
 * 13 falling celestial attacks themed around meteors, comets, and orbital debris.
 * Blue Moon particle palette: pale blue (180,210,255) / silver (200,200,220) / frost cyan (150,230,255)
 * Sounds: ENTITY_FIREWORK_ROCKET_LAUNCH (falling), ENTITY_GENERIC_EXPLODE (impact), BLOCK_ANVIL_LAND (heavy)
 */
public final class StarfallMeteors {

    private StarfallMeteors() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SilverMeteor(plugin));
        registry.register(new ShootingStar(plugin));
        registry.register(new MoonrockBombardment(plugin));
        registry.register(new CometNucleus(plugin));
        registry.register(new StardustRain(plugin));
        registry.register(new AsteroidCluster(plugin));
        registry.register(new CelestialAnvil(plugin));
        registry.register(new LunarFragment(plugin));
        registry.register(new StarShower(plugin));
        registry.register(new OrbitalDebris(plugin));
        registry.register(new SupernovaFragment(plugin));
        registry.register(new GravityBomb(plugin));
        registry.register(new Moonfall(plugin));
    }

    // ================================================================
    // 1. SILVER METEOR — 12 IRON_BLOCK+CALCITE jagged rock falling from Y+35
    // ================================================================
    public static class SilverMeteor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> meteorBlocks = new ArrayList<>();
        private double currentY;
        private float spinAngle = 0;

        public SilverMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("silver_meteor", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 35.0;

            // Warning dust circle on ground
            DisplayBuilder.dustParticles(center, 30, 6.0, 200, 200, 220, 1.5f);
            DisplayBuilder.particleRing(center, 6.0, Particle.END_ROD, 20, null);

            // 12 jagged rock blocks at Y+35
            Material[] mats = {Material.IRON_BLOCK, Material.CALCITE};
            double[] offX = {0, 0.8, -0.7, 1.2, -1.0, 0.3, -0.4, 1.0, -1.1, 0.5, -0.8, 0.1};
            double[] offY = {0, 0.4, -0.3, 0.8, -0.5, 1.0, 0.6, -0.7, 0.2, -0.4, 0.9, -0.6};
            double[] offZ = {0, -0.6, 0.9, -0.3, 0.7, -1.0, 0.4, 0.8, -0.9, 1.1, -0.2, 0.5};

            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(offX[i], currentY + offY[i], offZ[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = 0.5f + (float)(Math.random() * 0.5);
                h.scale(s, s * 1.3f, s).glow(200, 200, 220).interpolation(3, 0);
                h.rotate((float)(Math.random() * 0.8), 1, 0.5f, 0);
                meteorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 35, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (currentY > 0) {
                currentY -= 1.2;
                spinAngle += 6.0f;

                double cosA = Math.cos(Math.toRadians(spinAngle));
                double sinA = Math.sin(Math.toRadians(spinAngle));

                double[] offX = {0, 0.8, -0.7, 1.2, -1.0, 0.3, -0.4, 1.0, -1.1, 0.5, -0.8, 0.1};
                double[] offY = {0, 0.4, -0.3, 0.8, -0.5, 1.0, 0.6, -0.7, 0.2, -0.4, 0.9, -0.6};
                double[] offZ = {0, -0.6, 0.9, -0.3, 0.7, -1.0, 0.4, 0.8, -0.9, 1.1, -0.2, 0.5};

                for (int i = 0; i < meteorBlocks.size(); i++) {
                    double rx = offX[i] * cosA - offZ[i] * sinA;
                    double rz = offX[i] * sinA + offZ[i] * cosA;
                    meteorBlocks.get(i).entity().teleport(c.clone().add(rx, currentY + offY[i], rz));
                }

                // Falling trail particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, currentY + 2, 0), 10, 1.5, 200, 200, 220, 1.2f);
                }

                // Warning circle pulses
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.particleRing(c, 6.0, Particle.END_ROD, 12, null);
                }
            }

            if (currentY <= 0 && currentY > -1.5) {
                // Impact
                currentY = -10;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.dustParticles(c, 50, 6.0, 200, 200, 220, 2.0f);
                DisplayBuilder.particleRing(c, 6.0, Particle.EXPLOSION, 8, null);

                // Hide blocks below ground
                for (BlockDisplayHandle h : meteorBlocks) {
                    h.entity().teleport(c.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SilverMeteor(plugin); }
    }

    // ================================================================
    // 2. SHOOTING STAR — 10 SEA_LANTERN+DIAMOND_BLOCK diagonal streak
    // ================================================================
    public static class ShootingStar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private double dirX, dirZ;
        private double posX, posY, posZ;
        private boolean impacted = false;

        public ShootingStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shooting_star", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            posX = center.getX() - dirX * 30;
            posY = center.getY() + 30;
            posZ = center.getZ() - dirZ * 30;

            Material[] mats = {Material.SEA_LANTERN, Material.DIAMOND_BLOCK};
            for (int i = 0; i < 10; i++) {
                double trail = -i * 0.8;
                Location loc = new Location(w, posX + dirX * trail, posY + trail * 0.3, posZ + dirZ * trail);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = i < 3 ? 0.5f : 0.3f - (i - 3) * 0.02f;
                if (s < 0.15f) s = 0.15f;
                h.scale(s, s, s).glow(150, 230, 255).interpolation(2, 0);
                starBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 20, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            posX += dirX * 2.0;
            posZ += dirZ * 2.0;
            posY -= 1.5;

            for (int i = 0; i < starBlocks.size(); i++) {
                double trail = -i * 0.8;
                Location loc = new Location(c.getWorld(), posX + dirX * trail, posY + trail * 0.3, posZ + dirZ * trail);
                starBlocks.get(i).entity().teleport(loc);
            }

            // Sparkle trail
            if (ticksAlive % 1 == 0) {
                Location trailLoc = new Location(c.getWorld(), posX - dirX * 4, posY + 1.2, posZ - dirZ * 4);
                DisplayBuilder.dustParticles(trailLoc, 6, 0.5, 150, 230, 255, 0.8f);
                c.getWorld().spawnParticle(Particle.END_ROD, trailLoc, 3, 0.3, 0.3, 0.3, 0.01);
            }

            if (posY <= c.getY()) {
                impacted = true;
                Location impactLoc = new Location(c.getWorld(), posX, c.getY(), posZ);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.0f);
                DisplayBuilder.dustParticles(impactLoc, 30, 3.0, 150, 230, 255, 1.5f);

                for (BlockDisplayHandle h : starBlocks) {
                    h.entity().teleport(impactLoc.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShootingStar(plugin); }
    }

    // ================================================================
    // 3. MOONROCK BOMBARDMENT — 15 END_STONE in 5 groups of 3, staggered
    // ================================================================
    public static class MoonrockBombardment extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> groups = new ArrayList<>();
        private final List<double[]> groupTargets = new ArrayList<>();
        private final List<Double> groupY = new ArrayList<>();
        private final List<Boolean> groupImpacted = new ArrayList<>();
        private int nextGroup = 0;
        private int spawnTimer = 0;

        public MoonrockBombardment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonrock_bombardment", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Create 5 groups of 3 blocks each, with scattered target positions
            Random rand = new Random();
            for (int g = 0; g < 5; g++) {
                double tx = (rand.nextDouble() - 0.5) * 10;
                double tz = (rand.nextDouble() - 0.5) * 10;
                groupTargets.add(new double[]{tx, tz});
                groupY.add(25.0);
                groupImpacted.add(false);

                List<BlockDisplayHandle> group = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 0.8;
                    double oz = (rand.nextDouble() - 0.5) * 0.8;
                    Location loc = center.clone().add(tx + ox, 25 + rand.nextDouble() * 0.5, tz + oz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_STONE);
                    float s = 0.5f + (float)(rand.nextDouble() * 0.4);
                    h.scale(s, s, s).glow(200, 200, 220).interpolation(3, 0);
                    h.rotate((float)(rand.nextDouble() * 1.5), 1, 0, 1);
                    group.add(h);
                    spawnedEntities.add(h.entity());
                }
                groups.add(group);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Release groups every 10 ticks
            spawnTimer++;
            if (spawnTimer % 10 == 0 && nextGroup < 5) {
                nextGroup++;
                DisplayBuilder.playSound(c.clone().add(0, 20, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 0.7f);
            }

            for (int g = 0; g < nextGroup && g < 5; g++) {
                if (groupImpacted.get(g)) continue;

                double y = groupY.get(g) - 1.0;
                groupY.set(g, y);

                double[] target = groupTargets.get(g);
                List<BlockDisplayHandle> group = groups.get(g);
                Random rand = new Random(g * 1000L);

                for (int i = 0; i < group.size(); i++) {
                    double ox = (rand.nextDouble() - 0.5) * 0.8;
                    double oz = (rand.nextDouble() - 0.5) * 0.8;
                    rand.nextDouble(); // consume consistent random
                    group.get(i).entity().teleport(c.clone().add(target[0] + ox, y + i * 0.3, target[1] + oz));
                }

                // Dust trail
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(target[0], y + 1, target[1]), 5, 0.5, 200, 200, 220, 1.0f);
                }

                if (y <= 0) {
                    groupImpacted.set(g, true);
                    Location impactLoc = c.clone().add(target[0], 0, target[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                    DisplayBuilder.dustParticles(impactLoc, 20, 3.0, 200, 200, 220, 1.5f);

                    for (BlockDisplayHandle h : group) {
                        h.entity().teleport(c.clone().add(0, -10, 0));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonrockBombardment(plugin); }
    }

    // ================================================================
    // 4. COMET NUCLEUS — 14 BLUE_ICE+PACKED_ICE large comet with tail
    // ================================================================
    public static class CometNucleus extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private double currentY;
        private boolean impacted = false;

        public CometNucleus(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_nucleus", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 30.0;

            // Core: 8 blocks in a large sphere (scale 2.0)
            Material[] coreMats = {Material.BLUE_ICE, Material.PACKED_ICE};
            for (int i = 0; i < 8; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 1.2;
                double y = Math.cos(phi) * 1.2;
                double z = Math.sin(phi) * Math.sin(theta) * 1.2;
                Location loc = center.clone().add(x, currentY + y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, coreMats[i % 2]);
                h.scale(2.0f, 2.0f, 2.0f).glow(150, 230, 255).interpolation(3, 0);
                coreBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tail: 6 PACKED_ICE trailing upward, getting smaller
            for (int i = 0; i < 6; i++) {
                double tailDist = (i + 1) * 1.5;
                Location loc = center.clone().add(0, currentY + tailDist, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                float s = 1.5f - i * 0.2f;
                if (s < 0.4f) s = 0.4f;
                h.scale(s, s, s).glow(180, 210, 255).interpolation(3, 0);
                tailBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            currentY -= 0.8;

            // Move core
            for (int i = 0; i < coreBlocks.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 8);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.05;
                double x = Math.sin(phi) * Math.cos(theta) * 1.2;
                double y = Math.cos(phi) * 1.2;
                double z = Math.sin(phi) * Math.sin(theta) * 1.2;
                coreBlocks.get(i).entity().teleport(c.clone().add(x, currentY + y, z));
            }

            // Move tail
            for (int i = 0; i < tailBlocks.size(); i++) {
                double tailDist = (i + 1) * 1.5;
                tailBlocks.get(i).entity().teleport(c.clone().add(0, currentY + tailDist, 0));
            }

            // Long particle tail
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location p = c.clone().add(
                            (Math.random() - 0.5) * 1.5,
                            currentY + 3 + i * 1.5,
                            (Math.random() - 0.5) * 1.5
                    );
                    DisplayBuilder.dustParticles(p, 3, 0.3, 150, 230, 255, 1.5f);
                }
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, currentY + 4, 0), 5, 0.5, 2.0, 0.5, 0.02);
            }

            if (currentY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.dustParticles(c, 60, 5.0, 150, 230, 255, 2.5f);
                // Frost ring on impact
                DisplayBuilder.particleRing(c, 5.0, Particle.END_ROD, 30, null);
                DisplayBuilder.particleRing(c, 3.0, Particle.SNOWFLAKE, 20, null);

                for (BlockDisplayHandle h : coreBlocks) h.entity().teleport(c.clone().add(0, -10, 0));
                for (BlockDisplayHandle h : tailBlocks) h.entity().teleport(c.clone().add(0, -10, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CometNucleus(plugin); }
    }

    // ================================================================
    // 5. STARDUST RAIN — 18 AMETHYST_BLOCK (scale 0.3) continuous rain
    // ================================================================
    public static class StardustRain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> rainBlocks = new ArrayList<>();
        private final List<double[]> rainOffsets = new ArrayList<>();
        private final List<Double> rainY = new ArrayList<>();

        public StardustRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stardust_rain", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(3.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rand = new Random();
            for (int i = 0; i < 18; i++) {
                double ox = (rand.nextDouble() - 0.5) * 12;
                double oz = (rand.nextDouble() - 0.5) * 12;
                double startY = 15 + rand.nextDouble() * 10;
                rainOffsets.add(new double[]{ox, oz});
                rainY.add(startY);

                Location loc = center.clone().add(ox, startY, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(180, 160, 255).interpolation(2, 0);
                rainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Random rand = new Random();
            for (int i = 0; i < rainBlocks.size(); i++) {
                double y = rainY.get(i) - 0.6;
                double[] off = rainOffsets.get(i);

                if (y <= 0) {
                    // Impact then reset
                    Location impactLoc = c.clone().add(off[0], 0, off[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 5, 0.5, 180, 160, 255, 0.8f);

                    // Reset to top with new random position
                    y = 15 + rand.nextDouble() * 10;
                    off[0] = (rand.nextDouble() - 0.5) * 12;
                    off[1] = (rand.nextDouble() - 0.5) * 12;
                }

                rainY.set(i, y);
                rainBlocks.get(i).entity().teleport(c.clone().add(off[0], y, off[1]));
            }

            // Ambient sparkle
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 8, 0), 8, 6.0, 180, 160, 255, 0.6f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StardustRain(plugin); }
    }

    // ================================================================
    // 6. ASTEROID CLUSTER — 12 DEEPSLATE+STONE, splits into 3 at Y+15
    // ================================================================
    public static class AsteroidCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> subGroups = new ArrayList<>();
        private final List<double[]> subOffsets = new ArrayList<>();
        private final List<Double> subY = new ArrayList<>();
        private final List<Boolean> subImpacted = new ArrayList<>();
        private double mainY;
        private boolean split = false;

        public AsteroidCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("asteroid_cluster", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            mainY = 30.0;

            // 12 blocks as one cluster
            Material[] mats = {Material.DEEPSLATE, Material.STONE};
            Random rand = new Random();
            for (int i = 0; i < 12; i++) {
                double ox = (rand.nextDouble() - 0.5) * 2.0;
                double oy = (rand.nextDouble() - 0.5) * 2.0;
                double oz = (rand.nextDouble() - 0.5) * 2.0;
                Location loc = center.clone().add(ox, mainY + oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = 0.6f + (float)(rand.nextDouble() * 0.4);
                h.scale(s, s, s).glow(160, 160, 170).interpolation(3, 0);
                h.rotate((float)(rand.nextDouble() * 2), 1, 1, 0);
                mainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pre-define 3 sub-group targets
            double[][] targets = {{-3, -2}, {2, 3}, {3, -3}};
            for (int g = 0; g < 3; g++) {
                subOffsets.add(targets[g]);
                subY.add(15.0);
                subImpacted.add(false);
                subGroups.add(new ArrayList<>());
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!split) {
                mainY -= 1.0;
                Random rand = new Random(42);
                for (int i = 0; i < mainBlocks.size(); i++) {
                    double ox = (rand.nextDouble() - 0.5) * 2.0;
                    double oy = (rand.nextDouble() - 0.5) * 2.0;
                    double oz = (rand.nextDouble() - 0.5) * 2.0;
                    mainBlocks.get(i).entity().teleport(c.clone().add(ox, mainY + oy, oz));
                }

                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, mainY, 0), 8, 1.5, 160, 160, 170, 1.0f);
                }

                if (mainY <= 15) {
                    split = true;
                    DisplayBuilder.playSound(c.clone().add(0, 15, 0), Sound.BLOCK_STONE_BREAK, 1.5f, 0.5f);
                    DisplayBuilder.dustParticles(c.clone().add(0, 15, 0), 30, 2.0, 160, 160, 170, 1.5f);

                    // Distribute 12 blocks into 3 groups of 4
                    for (int i = 0; i < 12; i++) {
                        subGroups.get(i / 4).add(mainBlocks.get(i));
                    }
                }
            } else {
                // Animate sub-groups falling to their targets
                for (int g = 0; g < 3; g++) {
                    if (subImpacted.get(g)) continue;

                    double y = subY.get(g) - 0.8;
                    subY.set(g, y);
                    double[] target = subOffsets.get(g);
                    List<BlockDisplayHandle> group = subGroups.get(g);

                    // Spread toward target
                    double progress = 1.0 - (y / 15.0);
                    for (int i = 0; i < group.size(); i++) {
                        double ox = target[0] * progress + (i - 1.5) * 0.3;
                        double oz = target[1] * progress + (i % 2 - 0.5) * 0.3;
                        group.get(i).entity().teleport(c.clone().add(ox, y, oz));
                    }

                    if (y <= 0) {
                        subImpacted.set(g, true);
                        Location impactLoc = c.clone().add(target[0], 0, target[1]);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                        DisplayBuilder.dustParticles(impactLoc, 20, 3.0, 160, 160, 170, 1.5f);

                        for (BlockDisplayHandle h : group) {
                            h.entity().teleport(c.clone().add(0, -10, 0));
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AsteroidCluster(plugin); }
    }

    // ================================================================
    // 7. CELESTIAL ANVIL — 10 IRON_BLOCK heavy, falls slow then accelerates
    // ================================================================
    public static class CelestialAnvil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> anvilBlocks = new ArrayList<>();
        private double currentY;
        private double fallSpeed = 0.2;
        private boolean impacted = false;

        public CelestialAnvil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_anvil", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 25.0;

            // 10 IRON_BLOCK in a heavy rectangular shape: 2 wide x 2 deep x ~3 tall
            double[][] positions = {
                {-0.4, 0, -0.4}, {0.4, 0, -0.4}, {-0.4, 0, 0.4}, {0.4, 0, 0.4},
                {-0.4, 0.8, -0.4}, {0.4, 0.8, -0.4}, {-0.4, 0.8, 0.4}, {0.4, 0.8, 0.4},
                {0, 1.6, 0}, {0, -0.8, 0}
            };

            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(positions[i][0], currentY + positions[i][1], positions[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                float sx = (i < 8) ? 0.9f : 0.7f;
                float sy = (i == 9) ? 0.5f : 0.9f;
                h.scale(sx, sy, sx).glow(200, 200, 220).interpolation(3, 0);
                anvilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Warning circle
            DisplayBuilder.particleRing(center, 2.5, Particle.END_ROD, 15, null);
            DisplayBuilder.playSound(center.clone().add(0, 25, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            // Accelerate
            fallSpeed += 0.05;
            if (fallSpeed > 2.5) fallSpeed = 2.5;
            currentY -= fallSpeed;

            double[][] positions = {
                {-0.4, 0, -0.4}, {0.4, 0, -0.4}, {-0.4, 0, 0.4}, {0.4, 0, 0.4},
                {-0.4, 0.8, -0.4}, {0.4, 0.8, -0.4}, {-0.4, 0.8, 0.4}, {0.4, 0.8, 0.4},
                {0, 1.6, 0}, {0, -0.8, 0}
            };

            for (int i = 0; i < anvilBlocks.size(); i++) {
                anvilBlocks.get(i).entity().teleport(c.clone().add(positions[i][0], currentY + positions[i][1], positions[i][2]));
            }

            // Warning pulses intensify as it gets closer
            if (ticksAlive % Math.max(2, (int)(currentY / 3)) == 0) {
                DisplayBuilder.particleRing(c, 2.5, Particle.END_ROD, 10, null);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY + 2, 0), 6, 0.8, 200, 200, 220, 1.0f);
            }

            if (currentY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.dustParticles(c, 50, 3.0, 200, 200, 220, 2.5f);

                for (BlockDisplayHandle h : anvilBlocks) {
                    h.entity().teleport(c.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialAnvil(plugin); }
    }

    // ================================================================
    // 8. LUNAR FRAGMENT — 14 CALCITE+QUARTZ moon piece, tumbling
    // ================================================================
    public static class LunarFragment extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fragmentBlocks = new ArrayList<>();
        private final double[][] offsets = new double[14][3];
        private double currentY;
        private float tumbleAngle = 0;
        private boolean impacted = false;

        public LunarFragment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_fragment", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 28.0;

            // Irregular moon piece shape
            Material[] mats = {Material.CALCITE, Material.QUARTZ_BLOCK};
            Random rand = new Random();
            for (int i = 0; i < 14; i++) {
                // Flat-ish disc shape
                double angle = (Math.PI * 2 * i) / 14;
                double r = 1.0 + rand.nextDouble() * 0.8;
                offsets[i][0] = Math.cos(angle) * r;
                offsets[i][1] = (rand.nextDouble() - 0.5) * 0.6;
                offsets[i][2] = Math.sin(angle) * r;

                Location loc = center.clone().add(offsets[i][0], currentY + offsets[i][1], offsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float s = 0.5f + (float)(rand.nextDouble() * 0.3);
                h.scale(s, s * 0.6f, s).glow(240, 240, 255).interpolation(3, 0);
                fragmentBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 28, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            currentY -= 0.9;
            tumbleAngle += 5.0f;

            double cosT = Math.cos(Math.toRadians(tumbleAngle));
            double sinT = Math.sin(Math.toRadians(tumbleAngle));

            for (int i = 0; i < fragmentBlocks.size(); i++) {
                // Tumble rotation around X axis
                double ox = offsets[i][0];
                double oy = offsets[i][1] * cosT - offsets[i][2] * sinT;
                double oz = offsets[i][1] * sinT + offsets[i][2] * cosT;
                fragmentBlocks.get(i).entity().teleport(c.clone().add(ox, currentY + oy, oz));
            }

            // Trail particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY + 1.5, 0), 8, 1.5, 240, 240, 255, 1.0f);
            }

            if (currentY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                // Frost crater particles
                DisplayBuilder.dustParticles(c, 40, 4.0, 200, 230, 255, 2.0f);
                DisplayBuilder.particleRing(c, 4.0, Particle.SNOWFLAKE, 25, null);
                c.getWorld().spawnParticle(Particle.END_ROD, c, 15, 2.0, 0.5, 2.0, 0.02);

                for (BlockDisplayHandle h : fragmentBlocks) {
                    h.entity().teleport(c.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarFragment(plugin); }
    }

    // ================================================================
    // 9. STAR SHOWER — 16 SEA_LANTERN (scale 0.2) sweeping wave
    // ================================================================
    public static class StarShower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> showerBlocks = new ArrayList<>();
        private final List<double[]> positions = new ArrayList<>();
        private final List<Double> blockY = new ArrayList<>();
        private double sweepOffset = -8.0;
        private double dirX, dirZ;

        public StarShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_shower", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            Random rand = new Random();
            for (int i = 0; i < 16; i++) {
                // Spread perpendicular to sweep direction
                double perpSpread = (rand.nextDouble() - 0.5) * 10;
                double sweepPos = (rand.nextDouble() - 0.5) * 4;
                positions.add(new double[]{perpSpread, sweepPos});
                blockY.add(12.0 + rand.nextDouble() * 8);

                double px = -dirZ * perpSpread + dirX * sweepPos;
                double pz = dirX * perpSpread + dirZ * sweepPos;
                Location loc = center.clone().add(px, blockY.get(i), pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.2f, 0.2f, 0.2f).glow(150, 230, 255).interpolation(2, 0);
                showerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            sweepOffset += 0.3;

            for (int i = 0; i < showerBlocks.size(); i++) {
                double[] pos = positions.get(i);
                double y = blockY.get(i) - 0.8;

                double sweepPos = pos[1] + sweepOffset;
                double px = -dirZ * pos[0] + dirX * sweepPos;
                double pz = dirX * pos[0] + dirZ * sweepPos;

                if (y <= 0) {
                    Location impactLoc = c.clone().add(px, 0, pz);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 4, 0.3, 150, 230, 255, 0.6f);

                    // Reset
                    y = 12.0 + Math.random() * 8;
                    positions.set(i, new double[]{(Math.random() - 0.5) * 10, (Math.random() - 0.5) * 4});
                }

                blockY.set(i, y);
                showerBlocks.get(i).entity().teleport(c.clone().add(px, y, pz));
            }

            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(dirX * sweepOffset, 6, dirZ * sweepOffset), 5, 3.0, 2.0, 3.0, 0.01);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarShower(plugin); }
    }

    // ================================================================
    // 10. ORBITAL DEBRIS — 12 mixed blocks orbiting at Y+20, fall one by one
    // ================================================================
    public static class OrbitalDebris extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> orbitBlocks = new ArrayList<>();
        private final List<Double> orbitAngles = new ArrayList<>();
        private final List<Boolean> falling = new ArrayList<>();
        private final List<Double> fallY = new ArrayList<>();
        private final List<double[]> fallTargets = new ArrayList<>();
        private int nextFallIndex = 0;

        public OrbitalDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_debris", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.IRON_BLOCK, Material.DEEPSLATE, Material.CALCITE, Material.END_STONE,
                               Material.STONE, Material.BLUE_ICE, Material.IRON_BLOCK, Material.DEEPSLATE,
                               Material.CALCITE, Material.END_STONE, Material.STONE, Material.BLUE_ICE};

            Random rand = new Random();
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                orbitAngles.add(angle);
                falling.add(false);
                fallY.add(20.0);
                fallTargets.add(new double[]{
                    (rand.nextDouble() - 0.5) * 8,
                    (rand.nextDouble() - 0.5) * 8
                });

                double x = Math.cos(angle) * 5.0;
                double z = Math.sin(angle) * 5.0;
                Location loc = center.clone().add(x, 20, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                float s = 0.4f + (float)(rand.nextDouble() * 0.4);
                h.scale(s, s, s).glow(200, 200, 220).interpolation(3, 0);
                h.rotate((float)(rand.nextDouble() * 2), 1, 0.5f, 0.5f);
                orbitBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Release one block every 15 ticks
            if (ticksAlive % 15 == 0 && nextFallIndex < 12) {
                falling.set(nextFallIndex, true);
                DisplayBuilder.playSound(c.clone().add(0, 20, 0), Sound.BLOCK_STONE_BREAK, 0.6f, 0.8f);
                nextFallIndex++;
            }

            for (int i = 0; i < 12; i++) {
                if (!falling.get(i)) {
                    // Continue orbiting
                    double angle = orbitAngles.get(i) + Math.toRadians(ticksAlive * 2.0);
                    double x = Math.cos(angle) * 5.0;
                    double z = Math.sin(angle) * 5.0;
                    orbitBlocks.get(i).entity().teleport(c.clone().add(x, 20, z));
                } else {
                    double y = fallY.get(i);
                    if (y > -5) {
                        y -= 1.2;
                        fallY.set(i, y);

                        double[] target = fallTargets.get(i);
                        double progress = 1.0 - (y / 20.0);
                        double x = target[0] * progress;
                        double z = target[1] * progress;
                        orbitBlocks.get(i).entity().teleport(c.clone().add(x, Math.max(0, y), z));

                        if (ticksAlive % 3 == 0) {
                            DisplayBuilder.dustParticles(c.clone().add(x, y + 1, z), 3, 0.3, 200, 200, 220, 0.8f);
                        }

                        if (y <= 0 && y > -1.5) {
                            Location impactLoc = c.clone().add(target[0], 0, target[1]);
                            triggerImpactDamage(impactLoc);
                            DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.9f);
                            DisplayBuilder.dustParticles(impactLoc, 15, 3.0, 200, 200, 220, 1.2f);
                        }
                    }
                }
            }

            // Orbit ring particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 20, 0), 5.0, Particle.END_ROD, 8, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalDebris(plugin); }
    }

    // ================================================================
    // 11. SUPERNOVA FRAGMENT — 10 GLOWSTONE+SEA_LANTERN bright, fast fall
    // ================================================================
    public static class SupernovaFragment extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> novaBlocks = new ArrayList<>();
        private double currentY;
        private boolean impacted = false;

        public SupernovaFragment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("supernova_fragment", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 25.0;

            // 10 bright blocks in a tight sphere
            Material[] mats = {Material.GLOWSTONE, Material.SEA_LANTERN};
            for (int i = 0; i < 10; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 10);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 0.8;
                double y = Math.cos(phi) * 0.8;
                double z = Math.sin(phi) * Math.sin(theta) * 0.8;
                Location loc = center.clone().add(x, currentY + y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 255, 200).interpolation(3, 0);
                novaBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 25, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 1.5f);
            // Bright flash at spawn
            DisplayBuilder.dustParticles(center.clone().add(0, 25, 0), 30, 2.0, 255, 255, 200, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            currentY -= 1.8;

            for (int i = 0; i < novaBlocks.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 10);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.1;
                double x = Math.sin(phi) * Math.cos(theta) * 0.8;
                double y = Math.cos(phi) * 0.8;
                double z = Math.sin(phi) * Math.sin(theta) * 0.8;
                novaBlocks.get(i).entity().teleport(c.clone().add(x, currentY + y, z));
            }

            // Bright trail
            if (ticksAlive % 1 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY + 1, 0), 10, 1.0, 255, 255, 200, 1.5f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, currentY + 1.5, 0), 4, 0.5, 0.5, 0.5, 0.03);
            }

            if (currentY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.2f);
                // Light burst
                DisplayBuilder.dustParticles(c, 60, 4.0, 255, 255, 200, 2.5f);
                DisplayBuilder.particleRing(c, 4.0, Particle.END_ROD, 30, null);
                c.getWorld().spawnParticle(Particle.FLASH, c, 3, 0, 0, 0, 0);

                for (BlockDisplayHandle h : novaBlocks) {
                    h.entity().teleport(c.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SupernovaFragment(plugin); }
    }

    // ================================================================
    // 12. GRAVITY BOMB — 14 CRYING_OBSIDIAN+DEEPSLATE, slow fall, gravity pull then explode
    // ================================================================
    public static class GravityBomb extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bombBlocks = new ArrayList<>();
        private double currentY;
        private boolean impacted = false;
        private int impactTick = -1;
        private Location impactLoc;

        public GravityBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_bomb", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 22.0;

            // 14 blocks in a sphere
            Material[] mats = {Material.CRYING_OBSIDIAN, Material.DEEPSLATE};
            for (int i = 0; i < 14; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 14);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 1.0;
                double y = Math.cos(phi) * 1.0;
                double z = Math.sin(phi) * Math.sin(theta) * 1.0;
                Location loc = center.clone().add(x, currentY + y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 2]);
                h.scale(0.7f, 0.7f, 0.7f).glow(120, 80, 180).interpolation(3, 0);
                bombBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 22, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 0.3f);
            DisplayBuilder.dustParticles(center, 20, 5.0, 120, 80, 180, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!impacted) {
                // Slow fall
                currentY -= 0.3;

                for (int i = 0; i < bombBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 14);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.03;
                    double x = Math.sin(phi) * Math.cos(theta) * 1.0;
                    double y = Math.cos(phi) * 1.0;
                    double z = Math.sin(phi) * Math.sin(theta) * 1.0;
                    bombBlocks.get(i).entity().teleport(c.clone().add(x, currentY + y, z));
                }

                // Purple ominous particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, currentY, 0), 6, 1.2, 120, 80, 180, 1.2f);
                }

                // Warning ring
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.particleRing(c, 5.0, Particle.END_ROD, 12, null);
                }

                if (currentY <= 1.0) {
                    impacted = true;
                    impactTick = ticksAlive;
                    impactLoc = c.clone();
                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.3f);
                }
            } else {
                int sinceLand = ticksAlive - impactTick;

                if (sinceLand < 60) {
                    // 3-second gravity well pull
                    // Sphere pulses at ground
                    double pulse = 1.0 + 0.3 * Math.sin(sinceLand * 0.3);
                    for (int i = 0; i < bombBlocks.size(); i++) {
                        double phi = Math.acos(1 - 2.0 * (i + 0.5) / 14);
                        double theta = Math.PI * (1 + Math.sqrt(5)) * i + ticksAlive * 0.05;
                        double x = Math.sin(phi) * Math.cos(theta) * pulse;
                        double y = Math.cos(phi) * pulse + 1.0;
                        double z = Math.sin(phi) * Math.sin(theta) * pulse;
                        bombBlocks.get(i).entity().teleport(impactLoc.clone().add(x, y, z));
                    }

                    // Pull nearby players
                    if (sinceLand % 2 == 0) {
                        for (Player player : c.getWorld().getPlayers()) {
                            double dist = player.getLocation().distance(impactLoc);
                            if (dist <= 8 && dist > 0.5) {
                                Vector pull = impactLoc.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.3);
                                player.setVelocity(player.getVelocity().add(pull));
                            }
                        }
                    }

                    // Swirl particles
                    if (sinceLand % 2 == 0) {
                        DisplayBuilder.particleRing(impactLoc.clone().add(0, 1, 0), 3.0 - sinceLand * 0.04, Particle.PORTAL, 15, null);
                        DisplayBuilder.dustParticles(impactLoc, 10, 3.0, 120, 80, 180, 1.5f);
                    }

                    if (sinceLand % 20 == 0) {
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.3f);
                    }
                } else if (sinceLand == 60) {
                    // EXPLODE
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                    DisplayBuilder.dustParticles(impactLoc, 80, 5.0, 120, 80, 180, 3.0f);
                    DisplayBuilder.particleRing(impactLoc, 5.0, Particle.EXPLOSION, 10, null);

                    for (BlockDisplayHandle h : bombBlocks) {
                        h.entity().teleport(c.clone().add(0, -10, 0));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityBomb(plugin); }
    }

    // ================================================================
    // 13. MOONFALL — 18 CALCITE+QUARTZ+END_STONE mini-moon from Y+40
    // ================================================================
    public static class Moonfall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> moonBlocks = new ArrayList<>();
        private double currentY;
        private float rotAngle = 0;
        private boolean impacted = false;

        public Moonfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonfall", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentY = 40.0;

            // 18 blocks in a sphere for a mini-moon
            Material[] mats = {Material.CALCITE, Material.QUARTZ_BLOCK, Material.END_STONE};
            for (int i = 0; i < 18; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 18);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double x = Math.sin(phi) * Math.cos(theta) * 2.0;
                double y = Math.cos(phi) * 2.0;
                double z = Math.sin(phi) * Math.sin(theta) * 2.0;
                Location loc = center.clone().add(x, currentY + y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % 3]);
                h.scale(1.0f, 1.0f, 1.0f).glow(240, 240, 255).interpolation(3, 0);
                moonBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Warning circle
            DisplayBuilder.particleRing(center, 8.0, Particle.END_ROD, 25, null);
            DisplayBuilder.playSound(center.clone().add(0, 40, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.2f);
            DisplayBuilder.dustParticles(center, 40, 8.0, 240, 240, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impacted) return;

            currentY -= 0.7;
            rotAngle += 2.0f;

            double cosR = Math.cos(Math.toRadians(rotAngle));
            double sinR = Math.sin(Math.toRadians(rotAngle));

            for (int i = 0; i < moonBlocks.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 18);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double ox = Math.sin(phi) * Math.cos(theta) * 2.0;
                double oy = Math.cos(phi) * 2.0;
                double oz = Math.sin(phi) * Math.sin(theta) * 2.0;

                // Rotate around Y axis
                double rx = ox * cosR - oz * sinR;
                double rz = ox * sinR + oz * cosR;
                moonBlocks.get(i).entity().teleport(c.clone().add(rx, currentY + oy, rz));
            }

            // Frost trail
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY + 3, 0), 12, 2.5, 200, 230, 255, 1.5f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, currentY + 2, 0), 8, 2.0, 1.0, 2.0, 0.02);
            }

            // Warning ring intensifies
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(c, 8.0, Particle.END_ROD, 15, null);
            }

            if (currentY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                // Massive frost impact
                DisplayBuilder.dustParticles(c, 100, 8.0, 240, 240, 255, 3.0f);
                DisplayBuilder.particleRing(c, 8.0, Particle.EXPLOSION, 12, null);
                DisplayBuilder.particleRing(c, 6.0, Particle.SNOWFLAKE, 40, null);
                DisplayBuilder.particleRing(c, 4.0, Particle.END_ROD, 25, null);

                for (BlockDisplayHandle h : moonBlocks) {
                    h.entity().teleport(c.clone().add(0, -10, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Moonfall(plugin); }
    }
}
