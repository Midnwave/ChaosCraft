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

public final class FrostWeaponry {
    private FrostWeaponry() {}

    // Frost palette RGB for glow/dust
    private static final int FR = 100, FG = 180, FB = 255;

    private static void frostDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, FR, FG, FB, 1.2f);
    }

    private static void snowflakes(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, count, spread, spread, spread, 0.02);
    }

    private static void endRod(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, count, spread, spread, spread, 0.01);
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IcicleVolley(plugin));
        registry.register(new FrostHammer(plugin));
        registry.register(new IceLance(plugin));
        registry.register(new GlacialSword(plugin));
        registry.register(new FrostAxe(plugin));
        registry.register(new IcicleGatling(plugin));
        registry.register(new FrostScythe(plugin));
        registry.register(new CrystalMace(plugin));
        registry.register(new IceShuriken(plugin));
        registry.register(new FrostTrident(plugin));
        registry.register(new GlacialFlail(plugin));
        registry.register(new IceJavelinRain(plugin));
        registry.register(new FrostWhip(plugin));
    }

    // ================================================================
    // 1. ICICLE VOLLEY -- 8 thin icicles in a fan pattern toward player
    // Each icicle: packed_ice scaled 0.3x0.3x2.0, launched in spread arc
    // Plus 2 blue_ice accent blocks at origin = 10 total
    // ================================================================
    public static class IcicleVolley extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private final List<BlockDisplayHandle> accents = new ArrayList<>();
        private final double[] angles = new double[8];
        private Location target;
        private double dirX, dirZ;

        public IcicleVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_volley", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find nearest player for direction
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            if (nearest != null) {
                target = nearest.getLocation().clone();
                double dx = target.getX() - center.getX();
                double dz = target.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.1) { dirX = 1; dirZ = 0; } else { dirX = dx / len; dirZ = dz / len; }
            } else {
                dirX = 1; dirZ = 0;
            }

            double baseAngle = Math.atan2(dirZ, dirX);

            // 8 icicles in a fan spread of ~60 degrees
            for (int i = 0; i < 8; i++) {
                double spread = ((i - 3.5) / 3.5) * (Math.PI / 6.0); // -30 to +30 deg
                angles[i] = baseAngle + spread;
                Location loc = center.clone().add(0, 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.3f, 0.3f, 2.0f).glow(FR, FG, FB).interpolation(3, 0);
                // Rotate icicle to point in its launch direction
                h.rotate((float) angles[i], 0, 1, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 accent blocks at origin
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i == 0 ? -0.3 : 0.3, 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.5f, 0.5f, 0.5f).glow(FR, FG, FB).interpolation(2, 0);
                accents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Launch icicles outward from center along their fan angles
            double speed = 0.8;
            for (int i = 0; i < icicles.size(); i++) {
                BlockDisplayHandle h = icicles.get(i);
                double dist = tick * speed;
                double ox = Math.cos(angles[i]) * dist;
                double oz = Math.sin(angles[i]) * dist;
                Location loc = c.clone().add(ox, 1.5, oz);
                h.entity().teleport(loc);

                if (tick % 2 == 0) {
                    snowflakes(loc, 3, 0.2);
                    frostDust(loc, 2, 0.15);
                }
            }

            // Damage at the icicle fronts
            if (tick % 5 == 0 && tick > 5) {
                for (int i = 0; i < icicles.size(); i++) {
                    Location iloc = icicles.get(i).entity().getLocation();
                    triggerImpactDamage(iloc);
                }
            }

            if (tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IcicleVolley(plugin); }
    }

    // ================================================================
    // 2. FROST HAMMER -- Massive hammer: 10 head blocks + 4 handle blocks
    // Rises overhead, slams down with icy shockwave ring
    // ================================================================
    public static class FrostHammer extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private boolean risen = false;
        private int slamCount = 0;

        public FrostHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Hammer head: 10 blocks in a 2-wide, 5-tall slab (packed_ice + blue_ice)
            Material[] headMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE,
                    Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE,
                    Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE};
            double[][] headOffsets = {
                    {-0.6, 6.0, 0}, {0.6, 6.0, 0},
                    {-0.6, 7.0, 0}, {0.6, 7.0, 0},
                    {-0.6, 8.0, 0}, {0.6, 8.0, 0},
                    {-0.6, 9.0, 0}, {0.6, 9.0, 0},
                    {-0.6, 10.0, 0}, {0.6, 10.0, 0}
            };
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(headOffsets[i][0], headOffsets[i][1], headOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, headMats[i]);
                h.scale(1.4f, 1.2f, 1.0f).glow(FR, FG, FB).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 4 blocks in a vertical column (snow_block + bone_block)
            Material[] handleMats = {Material.BONE_BLOCK, Material.SNOW_BLOCK, Material.BONE_BLOCK, Material.SNOW_BLOCK};
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 2.0 + i * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, handleMats[i]);
                h.scale(0.5f, 1.2f, 0.5f).glow(FR, FG, FB).interpolation(3, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cycle: rise 40t, hold 10t, slam 10t, shockwave 20t, pause 20t = 100t cycle
            int cycle = tick % 100;

            if (cycle < 40) {
                // Rising phase
                float yOff = (cycle / 40.0f) * 6.0f;
                teleportHammer(c, yOff);
                if (cycle % 6 == 0) snowflakes(c.clone().add(0, 8 + yOff, 0), 5, 0.5);
            } else if (cycle < 50) {
                // Hold at apex
                teleportHammer(c, 6.0f);
                if (cycle == 40) {
                    risen = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 0.5f);
                }
                endRod(c.clone().add(0, 16, 0), 8, 1.0);
            } else if (cycle < 60) {
                // Slam down fast
                float progress = (cycle - 50) / 10.0f;
                float yOff = 6.0f * (1.0f - progress);
                teleportHammer(c, yOff);
                if (cycle == 59) {
                    slamCount++;
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.4f);
                    triggerImpactDamage(c);
                }
            } else if (cycle < 80) {
                // Shockwave ring expanding outward
                teleportHammer(c, 0);
                double radius = (cycle - 60) * 0.6;
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), radius, Particle.SNOWFLAKE, 24,
                        null);
                frostDust(c.clone().add(0, 0.5, 0), 10, radius * 0.5);
            } else {
                teleportHammer(c, 0);
            }

            if (tick % 10 == 0) {
                snowflakes(c.clone().add(0, 4, 0), 6, 1.5);
            }
        }

        private void teleportHammer(Location center, float yOffset) {
            double[][] headOffsets = {
                    {-0.6, 6.0, 0}, {0.6, 6.0, 0},
                    {-0.6, 7.0, 0}, {0.6, 7.0, 0},
                    {-0.6, 8.0, 0}, {0.6, 8.0, 0},
                    {-0.6, 9.0, 0}, {0.6, 9.0, 0},
                    {-0.6, 10.0, 0}, {0.6, 10.0, 0}
            };
            for (int i = 0; i < headBlocks.size(); i++) {
                headBlocks.get(i).entity().teleport(center.clone().add(
                        headOffsets[i][0], headOffsets[i][1] + yOffset, headOffsets[i][2]));
            }
            for (int i = 0; i < handleBlocks.size(); i++) {
                handleBlocks.get(i).entity().teleport(center.clone().add(0, 2.0 + i * 1.0 + yOffset, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostHammer(plugin); }
    }

    // ================================================================
    // 3. ICE LANCE -- 10 blocks forming a long lance, charges forward at player
    // Shaft: 7 packed_ice blocks in a line, tip: 3 blue_ice tapered blocks
    // ================================================================
    public static class IceLance extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tipBlocks = new ArrayList<>();
        private double dirX, dirZ;
        private double travelDist = 0;

        public IceLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_lance", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find nearest player for charging direction
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.1) { dirX = 1; dirZ = 0; } else { dirX = dx / len; dirZ = dz / len; }
            } else { dirX = 1; dirZ = 0; }

            float yaw = (float) Math.atan2(dirZ, dirX);

            // Shaft: 7 blocks forming the long body
            for (int i = 0; i < 7; i++) {
                double offsetAlongAxis = -i * 0.8;
                Location loc = center.clone().add(dirX * offsetAlongAxis, 1.5, dirZ * offsetAlongAxis);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.4f, 0.4f, 1.0f).glow(FR, FG, FB).interpolation(3, 0);
                h.rotate(yaw, 0, 1, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Tip: 3 blue_ice blocks, tapered
            float[] tipScales = {0.35f, 0.25f, 0.15f};
            for (int i = 0; i < 3; i++) {
                double offsetAlongAxis = (i + 1) * 0.6;
                Location loc = center.clone().add(dirX * offsetAlongAxis, 1.5, dirZ * offsetAlongAxis);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(tipScales[i], tipScales[i], 0.8f).glow(FR, FG, FB).interpolation(3, 0);
                h.rotate(yaw, 0, 1, 0);
                tipBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 1.2f);
            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wind-up for first 20 ticks (slight pullback)
            double effectiveDist;
            if (tick < 20) {
                effectiveDist = -(20 - tick) * 0.05; // pull back slightly
            } else {
                effectiveDist = (tick - 20) * 0.6; // charge forward
            }

            float yaw = (float) Math.atan2(dirZ, dirX);

            // Move all shaft blocks
            for (int i = 0; i < shaftBlocks.size(); i++) {
                double offsetAlongAxis = -i * 0.8 + effectiveDist;
                Location loc = c.clone().add(dirX * offsetAlongAxis, 1.5, dirZ * offsetAlongAxis);
                shaftBlocks.get(i).entity().teleport(loc);
            }
            // Move tip blocks
            for (int i = 0; i < tipBlocks.size(); i++) {
                double offsetAlongAxis = (i + 1) * 0.6 + effectiveDist;
                Location loc = c.clone().add(dirX * offsetAlongAxis, 1.5, dirZ * offsetAlongAxis);
                tipBlocks.get(i).entity().teleport(loc);
            }

            // Particles at lance tip
            if (tick > 20 && tick % 2 == 0) {
                Location tipLoc = tipBlocks.get(tipBlocks.size() - 1).entity().getLocation();
                snowflakes(tipLoc, 5, 0.3);
                frostDust(tipLoc, 4, 0.2);
                endRod(tipLoc, 2, 0.1);
            }

            // Trail particles along shaft
            if (tick > 20 && tick % 3 == 0) {
                for (int i = 0; i < shaftBlocks.size(); i += 2) {
                    Location sLoc = shaftBlocks.get(i).entity().getLocation();
                    snowflakes(sLoc, 2, 0.15);
                }
            }

            // Sound
            if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.5f, 0.6f);
            }
            if (tick > 20 && tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceLance(plugin); }
    }

    // ================================================================
    // 4. GLACIAL SWORD -- 12 blade blocks + 4 hilt blocks
    // Sweeping horizontal slash arc, 180-degree sweep
    // ================================================================
    public static class GlacialSword extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> hiltBlocks = new ArrayList<>();
        private boolean risen = false;

        public GlacialSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_sword", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 12 blocks forming a long tapered sword blade
            Material[] bladeMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
                    Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
                    Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE,
                    Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE};
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, 2.0, (i - 2) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, bladeMats[i]);
                float width = 0.8f - (i * 0.04f);
                h.scale(width, 0.3f, 0.8f).glow(FR, FG, FB).interpolation(3, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Hilt: 2 crossguard + 2 grip
            BlockDisplayHandle g1 = displayBuilder.spawnBlock(center.clone().add(-0.8, 2.0, -1.4), Material.PRISMARINE);
            g1.scale(1.8f, 0.4f, 0.4f).glow(60, 140, 200).interpolation(3, 0);
            hiltBlocks.add(g1);
            spawnedEntities.add(g1.entity());

            BlockDisplayHandle g2 = displayBuilder.spawnBlock(center.clone().add(0.8, 2.0, -1.4), Material.PRISMARINE);
            g2.scale(1.8f, 0.4f, 0.4f).glow(60, 140, 200).interpolation(3, 0);
            hiltBlocks.add(g2);
            spawnedEntities.add(g2.entity());

            BlockDisplayHandle grip1 = displayBuilder.spawnBlock(center.clone().add(0, 2.0, -2.2), Material.BONE_BLOCK);
            grip1.scale(0.4f, 0.35f, 0.8f).glow(200, 220, 240).interpolation(3, 0);
            hiltBlocks.add(grip1);
            spawnedEntities.add(grip1.entity());

            BlockDisplayHandle grip2 = displayBuilder.spawnBlock(center.clone().add(0, 2.0, -3.0), Material.BONE_BLOCK);
            grip2.scale(0.5f, 0.5f, 0.5f).glow(200, 220, 240).interpolation(3, 0);
            hiltBlocks.add(grip2);
            spawnedEntities.add(grip2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise phase (first 30 ticks)
            if (tick < 30) {
                float yOff = (tick / 30.0f) * 3.0f;
                teleportSword(c, yOff, 0);
                if (tick % 5 == 0) snowflakes(c.clone().add(0, 4, 0), 6, 0.8);
                return;
            }

            if (!risen) {
                risen = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
            }

            // Sweeping slash cycles: sweep 30t, pause 20t, return 30t, pause 20t = 100t
            float baseY = 3.0f;
            int cycleTick = (tick - 30) % 100;
            float slashAngle = 0;

            if (cycleTick < 30) {
                slashAngle = (cycleTick / 30.0f) * (float) Math.PI;
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
                }
            } else if (cycleTick >= 50 && cycleTick < 80) {
                slashAngle = (float) Math.PI - ((cycleTick - 50) / 30.0f) * (float) Math.PI;
            }

            teleportSword(c, baseY, slashAngle);

            // Sweep particles
            if (cycleTick < 30 && tick % 2 == 0) {
                for (int i = 0; i < bladeBlocks.size(); i += 3) {
                    Location bLoc = bladeBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bLoc, 1, 0.3, 0.3, 0.3, 0);
                    frostDust(bLoc, 3, 0.2);
                }
            }

            if (tick % 6 == 0) {
                snowflakes(c.clone().add(0, 3 + baseY, 0), 4, 1.0);
            }
        }

        private void teleportSword(Location center, float yOffset, float angle) {
            for (int i = 0; i < bladeBlocks.size(); i++) {
                double localZ = (i - 2) * 0.7;
                double rx = Math.cos(angle) * 0 - Math.sin(angle) * localZ;
                double rz = Math.sin(angle) * 0 + Math.cos(angle) * localZ;
                bladeBlocks.get(i).entity().teleport(center.clone().add(rx, 2.0 + yOffset, rz));
                bladeBlocks.get(i).rotate(angle, 0, 1, 0);
                bladeBlocks.get(i).interpolation(2, 0);
            }
            double[][] hiltLocalPos = {{-0.8, 0, -1.4}, {0.8, 0, -1.4}, {0, 0, -2.2}, {0, 0, -3.0}};
            for (int i = 0; i < hiltBlocks.size(); i++) {
                double lx = hiltLocalPos[i][0];
                double lz = hiltLocalPos[i][2];
                double rx = Math.cos(angle) * lx - Math.sin(angle) * lz;
                double rz = Math.sin(angle) * lx + Math.cos(angle) * lz;
                hiltBlocks.get(i).entity().teleport(center.clone().add(rx, 2.0 + yOffset, rz));
                hiltBlocks.get(i).rotate(angle, 0, 1, 0);
                hiltBlocks.get(i).interpolation(2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialSword(plugin); }
    }

    // ================================================================
    // 5. FROST AXE -- 8 head blocks + 4 handle blocks, spinning throw and return
    // Boomerang pattern: spins away from center, arcs, returns
    // ================================================================
    public static class FrostAxe extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private double throwAngle = 0;

        public FrostAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_axe", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pick random throw direction
            throwAngle = Math.random() * Math.PI * 2;

            // Axe head: 8 blocks forming an axe blade shape (curved/wedge)
            // Two rows of 4: top wider, bottom narrower
            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE,
                    Material.ICE, Material.PACKED_ICE, Material.ICE, Material.PACKED_ICE};
            double[][] headLocal = {
                    {-0.5, 0.5, 0}, {0.5, 0.5, 0}, {1.2, 0.5, 0}, {1.8, 0.5, 0},   // top blade edge
                    {-0.3, -0.3, 0}, {0.3, -0.3, 0}, {0.9, -0.3, 0}, {1.4, -0.3, 0}  // bottom blade
            };
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(headLocal[i][0], 3.0 + headLocal[i][1], headLocal[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                float sy = i < 4 ? 0.9f : 0.7f;
                h.scale(0.7f, sy, 0.35f).glow(FR, FG, FB).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 4 blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-0.5 - i * 0.6, 3.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.7f, 0.35f, 0.35f).glow(200, 220, 240).interpolation(3, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            int halfDuration = config.getDurationTicks() / 2;
            double maxDist = 15.0;

            // Boomerang path: go out for first half, come back second half
            double progress;
            if (tick < halfDuration) {
                progress = (double) tick / halfDuration;
            } else {
                progress = 1.0 - ((double) (tick - halfDuration) / halfDuration);
            }
            double dist = progress * maxDist;

            // Arc slightly to the side (boomerang curve)
            double curveOffset = Math.sin(progress * Math.PI) * 5.0;
            double perpAngle = throwAngle + Math.PI / 2;

            double ox = Math.cos(throwAngle) * dist + Math.cos(perpAngle) * curveOffset;
            double oz = Math.sin(throwAngle) * dist + Math.sin(perpAngle) * curveOffset;

            Location axeCenter = c.clone().add(ox, 3.0, oz);

            // Spin rotation
            float spin = (float) (tick * 0.3);

            // Teleport all parts with spin
            double[][] headLocal = {
                    {-0.5, 0.5, 0}, {0.5, 0.5, 0}, {1.2, 0.5, 0}, {1.8, 0.5, 0},
                    {-0.3, -0.3, 0}, {0.3, -0.3, 0}, {0.9, -0.3, 0}, {1.4, -0.3, 0}
            };
            for (int i = 0; i < headBlocks.size(); i++) {
                double lx = headLocal[i][0];
                double ly = headLocal[i][1];
                double rx = Math.cos(spin) * lx - Math.sin(spin) * ly;
                double ry = Math.sin(spin) * lx + Math.cos(spin) * ly;
                headBlocks.get(i).entity().teleport(axeCenter.clone().add(rx, ry, 0));
                headBlocks.get(i).rotate(spin, 0, 0, 1);
                headBlocks.get(i).interpolation(2, 0);
            }
            for (int i = 0; i < handleBlocks.size(); i++) {
                double lx = -0.5 - i * 0.6;
                double ly = 0;
                double rx = Math.cos(spin) * lx - Math.sin(spin) * ly;
                double ry = Math.sin(spin) * lx + Math.cos(spin) * ly;
                handleBlocks.get(i).entity().teleport(axeCenter.clone().add(rx, ry, 0));
                handleBlocks.get(i).rotate(spin, 0, 0, 1);
                handleBlocks.get(i).interpolation(2, 0);
            }

            // Particles
            if (tick % 2 == 0) {
                snowflakes(axeCenter, 6, 0.8);
                frostDust(axeCenter, 4, 0.5);
            }
            if (tick % 3 == 0) {
                endRod(axeCenter, 3, 0.4);
            }

            // Sound
            if (tick % 8 == 0) {
                DisplayBuilder.playSound(axeCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostAxe(plugin); }
    }

    // ================================================================
    // 6. ICICLE GATLING -- 16 rotating icicles, rapid-fire stream tracking player
    // Barrel: 12 icicles in a ring, body: 4 ice blocks in center
    // Rotates and fires frost projectile bursts
    // ================================================================
    public static class IcicleGatling extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> barrelIcicles = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private Player trackTarget;

        public IcicleGatling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_gatling", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find nearest player to track
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; trackTarget = p; }
            }

            // Body: 4 central ice blocks forming the gun body
            Material[] bodyMats = {Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE};
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 4.0, -i * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, bodyMats[i]);
                h.scale(0.8f, 0.8f, 1.0f).glow(FR, FG, FB).interpolation(3, 0);
                bodyBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Barrel: 12 icicles in a ring around the front
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double bx = Math.cos(angle) * 0.8;
                double by = Math.sin(angle) * 0.8;
                Location loc = center.clone().add(bx, 4.0 + by, 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.2f, 0.2f, 1.5f).glow(FR, FG, FB).interpolation(3, 0);
                barrelIcicles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spin barrels
            float spinOffset = tick * 0.15f;
            for (int i = 0; i < barrelIcicles.size(); i++) {
                double angle = (2 * Math.PI * i) / 12 + spinOffset;
                double bx = Math.cos(angle) * 0.8;
                double by = Math.sin(angle) * 0.8;
                barrelIcicles.get(i).entity().teleport(c.clone().add(bx, 4.0 + by, 0.8));
            }

            // Fire frost bursts every 5 ticks toward tracked player
            if (tick % 5 == 0 && trackTarget != null && trackTarget.isOnline()) {
                Location gunFront = c.clone().add(0, 4.0, 1.5);
                Location playerLoc = trackTarget.getLocation().clone().add(0, 1, 0);

                // Particle line toward player
                DisplayBuilder.particleLine(gunFront, playerLoc, Particle.SNOWFLAKE, 3, null);
                frostDust(playerLoc, 8, 0.5);
                endRod(gunFront, 3, 0.3);

                DisplayBuilder.playSound(gunFront, Sound.BLOCK_GLASS_BREAK, 0.7f, 2.0f);
            }

            // Ambient spinning particles
            if (tick % 3 == 0) {
                Location barrel = c.clone().add(0, 4.0, 0.8);
                DisplayBuilder.particleRing(barrel, 0.8, Particle.SNOWFLAKE, 8, null);
            }

            // Body glow pulse
            if (tick % 10 == 0) {
                snowflakes(c.clone().add(0, 4, 0), 5, 0.6);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IcicleGatling(plugin); }
    }

    // ================================================================
    // 7. FROST SCYTHE -- 8 curved blade blocks + 4 handle blocks
    // Sweeping arc from below, rising slash attack
    // ================================================================
    public static class FrostScythe extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();

        public FrostScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_scythe", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 8 blocks forming a curved scythe blade (arc shape)
            // Each block positioned along a curve
            Material[] bladeMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE,
                    Material.ICE, Material.PACKED_ICE, Material.ICE, Material.LIGHT_BLUE_STAINED_GLASS};
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * (Math.PI * 0.8); // 144-degree arc
                double bx = Math.cos(angle) * (2.0 + i * 0.3);
                double by = Math.sin(angle) * (2.0 + i * 0.3);
                Location loc = center.clone().add(bx, 0.5 + by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, bladeMats[i]);
                float width = 0.7f - (i * 0.04f); // taper toward tip
                h.scale(width, 0.25f, 0.4f).glow(FR, FG, FB).interpolation(3, 0);
                h.rotate((float) angle, 0, 0, 1);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 4 blocks extending downward
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, -i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.35f, 1.0f, 0.35f).glow(200, 220, 240).interpolation(3, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slash cycle: rise from below (30t), sweep arc (40t), settle (30t) = 100t
            int cycleTick = tick % 100;
            float yOffset;
            float sweepAngle;

            if (cycleTick < 30) {
                // Rising from below
                yOffset = -3.0f + (cycleTick / 30.0f) * 6.0f; // -3 to +3
                sweepAngle = 0;
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                }
            } else if (cycleTick < 70) {
                // Sweeping arc
                yOffset = 3.0f;
                sweepAngle = ((cycleTick - 30) / 40.0f) * (float) Math.PI;
            } else {
                // Settling back down
                yOffset = 3.0f - ((cycleTick - 70) / 30.0f) * 6.0f;
                sweepAngle = 0;
            }

            // Teleport blade blocks with sweep rotation
            for (int i = 0; i < bladeBlocks.size(); i++) {
                double baseAngle = (i / 8.0) * (Math.PI * 0.8);
                double bx = Math.cos(baseAngle + sweepAngle) * (2.0 + i * 0.3);
                double bz = Math.sin(baseAngle + sweepAngle) * (2.0 + i * 0.3);
                bladeBlocks.get(i).entity().teleport(c.clone().add(bx, 0.5 + yOffset, bz));
                bladeBlocks.get(i).rotate((float) (baseAngle + sweepAngle), 0, 1, 0);
                bladeBlocks.get(i).interpolation(2, 0);
            }

            // Handle follows
            for (int i = 0; i < handleBlocks.size(); i++) {
                handleBlocks.get(i).entity().teleport(c.clone().add(0, yOffset - i * 0.9, 0));
            }

            // Particles
            if (tick % 2 == 0 && cycleTick >= 30 && cycleTick < 70) {
                Location tipLoc = bladeBlocks.get(bladeBlocks.size() - 1).entity().getLocation();
                snowflakes(tipLoc, 5, 0.3);
                frostDust(tipLoc, 4, 0.2);
                c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tipLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            if (tick % 8 == 0) {
                endRod(c.clone().add(0, yOffset + 2, 0), 3, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostScythe(plugin); }
    }

    // ================================================================
    // 8. CRYSTAL MACE -- 10 spiked head blocks + 4 shaft blocks
    // Vertical circle swing: pendulum over and down
    // ================================================================
    public static class CrystalMace extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();

        public CrystalMace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_mace", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spiked head: 10 blocks (central mass + spikes)
            // Core: 4 blocks, Spikes: 6 blocks protruding outward
            Material[] coreMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE};
            double[][] coreOffsets = {{0, 0, 0}, {0.5, 0, 0}, {0, 0, 0.5}, {0.5, 0, 0.5}};
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(coreOffsets[i][0], 6.0 + coreOffsets[i][1], coreOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, coreMats[i]);
                h.scale(0.7f, 0.7f, 0.7f).glow(FR, FG, FB).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Spikes
            Material[] spikeMats = {Material.PRISMARINE, Material.ICE, Material.PRISMARINE,
                    Material.ICE, Material.PRISMARINE, Material.ICE};
            double[][] spikeOffsets = {
                    {1.0, 0.2, 0.25}, {-0.5, 0.2, 0.25}, {0.25, 0.2, 1.0},
                    {0.25, 0.2, -0.5}, {0.25, 1.0, 0.25}, {0.25, -0.5, 0.25}
            };
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(spikeOffsets[i][0], 6.0 + spikeOffsets[i][1], spikeOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, spikeMats[i]);
                h.scale(0.3f, 0.3f, 0.3f).glow(60, 200, 255).interpolation(3, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shaft: 4 blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0.25, 2.0 + i * 1.0, 0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.4f, 1.2f, 0.4f).glow(200, 220, 240).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Vertical circle swing: full rotation in vertical plane over 80 ticks
            float swingAngle = (tick % 80) / 80.0f * (float) (Math.PI * 2);

            // Pivot point is at the top of the shaft
            double pivotY = 6.0;
            double armLength = 4.0; // distance from pivot to mace head center

            // Mace head orbits around pivot in the XY plane
            double headX = Math.sin(swingAngle) * armLength;
            double headY = pivotY + Math.cos(swingAngle) * armLength;

            // Teleport head blocks
            double[][] coreLocal = {{0, 0, 0}, {0.5, 0, 0}, {0, 0, 0.5}, {0.5, 0, 0.5}};
            for (int i = 0; i < 4; i++) {
                headBlocks.get(i).entity().teleport(c.clone().add(
                        headX + coreLocal[i][0], headY + coreLocal[i][1], coreLocal[i][2]));
            }
            double[][] spikeLocal = {
                    {1.0, 0.2, 0.25}, {-0.5, 0.2, 0.25}, {0.25, 0.2, 1.0},
                    {0.25, 0.2, -0.5}, {0.25, 1.0, 0.25}, {0.25, -0.5, 0.25}
            };
            for (int i = 0; i < 6; i++) {
                headBlocks.get(4 + i).entity().teleport(c.clone().add(
                        headX + spikeLocal[i][0], headY + spikeLocal[i][1], spikeLocal[i][2]));
            }

            // Shaft segments stretch between pivot and head
            for (int i = 0; i < shaftBlocks.size(); i++) {
                double t = (i + 1.0) / (shaftBlocks.size() + 1);
                double sx = headX * t;
                double sy = pivotY + (headY - pivotY) * t;
                shaftBlocks.get(i).entity().teleport(c.clone().add(sx, sy, 0.25));
            }

            // Impact at bottom of swing
            int cycleTick = tick % 80;
            if (cycleTick == 40) { // bottom of swing
                Location impactLoc = c.clone().add(headX, headY, 0);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
                // Shockwave ring
                for (int r = 0; r < 5; r++) {
                    final int ring = r;
                    DisplayBuilder.particleRing(impactLoc.clone().add(0, 0.2, 0),
                            1.0 + ring, Particle.SNOWFLAKE, 16, null);
                }
            }

            // Particles trailing head
            if (tick % 2 == 0) {
                Location headLoc = c.clone().add(headX, headY, 0.25);
                snowflakes(headLoc, 5, 0.5);
                frostDust(headLoc, 3, 0.3);
            }

            if (tick % 4 == 0) {
                endRod(c.clone().add(headX, headY, 0.25), 2, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalMace(plugin); }
    }

    // ================================================================
    // 9. ICE SHURIKEN -- 10 blocks forming 5-pointed star
    // Spins on Y-axis, flies toward player
    // ================================================================
    public static class IceShuriken extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private double dirX, dirZ;

        public IceShuriken(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shuriken", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Direction toward nearest player
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.1) { dirX = 1; dirZ = 0; } else { dirX = dx / len; dirZ = dz / len; }
            } else { dirX = 1; dirZ = 0; }

            // 5-pointed star: 5 points with 2 blocks each (tip + inner)
            Material[] mats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE,
                    Material.ICE, Material.PACKED_ICE, Material.ICE, Material.PACKED_ICE,
                    Material.BLUE_ICE, Material.LIGHT_BLUE_STAINED_GLASS};
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5 - Math.PI / 2;
                // Outer point
                double ox = Math.cos(angle) * 2.0;
                double oz = Math.sin(angle) * 2.0;
                Location loc = center.clone().add(ox, 2.5, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i * 2]);
                h.scale(0.35f, 0.35f, 0.35f).glow(FR, FG, FB).interpolation(3, 0);
                starBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Inner point (closer to center)
                double ix = Math.cos(angle) * 0.8;
                double iz = Math.sin(angle) * 0.8;
                Location iloc = center.clone().add(ix, 2.5, iz);
                BlockDisplayHandle ih = displayBuilder.spawnBlock(iloc, mats[i * 2 + 1]);
                ih.scale(0.5f, 0.4f, 0.5f).glow(FR, FG, FB).interpolation(3, 0);
                starBlocks.add(ih);
                spawnedEntities.add(ih.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.5f);
            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double travelDist = tick * 0.5;
            double cx = c.getX() + dirX * travelDist;
            double cz = c.getZ() + dirZ * travelDist;
            Location shurikenCenter = new Location(c.getWorld(), cx, c.getY() + 2.5, cz);

            // Spin on Y-axis
            float spin = tick * 0.25f;

            for (int i = 0; i < 5; i++) {
                double baseAngle = (2 * Math.PI * i) / 5 - Math.PI / 2;

                // Outer point
                double outerR = 2.0;
                double ox = Math.cos(baseAngle + spin) * outerR;
                double oz = Math.sin(baseAngle + spin) * outerR;
                starBlocks.get(i * 2).entity().teleport(shurikenCenter.clone().add(ox, 0, oz));

                // Inner point
                double innerR = 0.8;
                double ix = Math.cos(baseAngle + spin) * innerR;
                double iz = Math.sin(baseAngle + spin) * innerR;
                starBlocks.get(i * 2 + 1).entity().teleport(shurikenCenter.clone().add(ix, 0, iz));
            }

            // Particles
            if (tick % 2 == 0) {
                snowflakes(shurikenCenter, 6, 1.0);
                frostDust(shurikenCenter, 4, 0.8);
            }
            if (tick % 3 == 0) {
                DisplayBuilder.particleRing(shurikenCenter, 2.0, Particle.END_ROD, 10, null);
            }

            if (tick % 8 == 0) {
                DisplayBuilder.playSound(shurikenCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceShuriken(plugin); }
    }

    // ================================================================
    // 10. FROST TRIDENT -- 8 shaft blocks + 6 prong blocks
    // Erupts from ground then launches upward and forward
    // ================================================================
    public static class FrostTrident extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> prongBlocks = new ArrayList<>();
        private boolean launched = false;
        private double dirX, dirZ;

        public FrostTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_trident", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Direction toward nearest player
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.1) { dirX = 1; dirZ = 0; } else { dirX = dx / len; dirZ = dz / len; }
            } else { dirX = 1; dirZ = 0; }

            // Shaft: 8 blocks forming the long handle (starts underground)
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, -4.0 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc,
                        i % 2 == 0 ? Material.PACKED_ICE : Material.BONE_BLOCK);
                h.scale(0.35f, 1.0f, 0.35f).glow(FR, FG, FB).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Prongs: 3 prongs, 2 blocks each (center + 2 outer)
            // Center prong (tallest)
            BlockDisplayHandle cp1 = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.BLUE_ICE);
            cp1.scale(0.3f, 1.2f, 0.3f).glow(FR, FG, FB).interpolation(3, 0);
            prongBlocks.add(cp1);
            spawnedEntities.add(cp1.entity());

            BlockDisplayHandle cp2 = displayBuilder.spawnBlock(center.clone().add(0, 3.7, 0), Material.PRISMARINE);
            cp2.scale(0.2f, 0.8f, 0.2f).glow(60, 200, 255).interpolation(3, 0);
            prongBlocks.add(cp2);
            spawnedEntities.add(cp2.entity());

            // Left prong
            BlockDisplayHandle lp1 = displayBuilder.spawnBlock(center.clone().add(-0.5, 2.2, 0), Material.BLUE_ICE);
            lp1.scale(0.25f, 1.0f, 0.25f).glow(FR, FG, FB).interpolation(3, 0);
            prongBlocks.add(lp1);
            spawnedEntities.add(lp1.entity());

            BlockDisplayHandle lp2 = displayBuilder.spawnBlock(center.clone().add(-0.5, 3.2, 0), Material.PRISMARINE);
            lp2.scale(0.18f, 0.6f, 0.18f).glow(60, 200, 255).interpolation(3, 0);
            prongBlocks.add(lp2);
            spawnedEntities.add(lp2.entity());

            // Right prong
            BlockDisplayHandle rp1 = displayBuilder.spawnBlock(center.clone().add(0.5, 2.2, 0), Material.BLUE_ICE);
            rp1.scale(0.25f, 1.0f, 0.25f).glow(FR, FG, FB).interpolation(3, 0);
            prongBlocks.add(rp1);
            spawnedEntities.add(rp1.entity());

            BlockDisplayHandle rp2 = displayBuilder.spawnBlock(center.clone().add(0.5, 3.2, 0), Material.PRISMARINE);
            rp2.scale(0.18f, 0.6f, 0.18f).glow(60, 200, 255).interpolation(3, 0);
            prongBlocks.add(rp2);
            spawnedEntities.add(rp2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick < 40) {
                // Phase 1: Erupt from ground (rise up)
                float riseOffset = (tick / 40.0f) * 6.0f;
                teleportTrident(c, riseOffset, 0, 0);

                if (tick % 4 == 0) {
                    Location base = c.clone().add(0, riseOffset - 2, 0);
                    snowflakes(base, 8, 0.5);
                    frostDust(base, 5, 0.3);
                }
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 0.6f, 0.8f);
                }
                return;
            }

            if (!launched) {
                launched = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.5f, 0.8f);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 1.5f);
            }

            // Phase 2: Launch forward toward player
            double launchTick = tick - 40;
            double speed = 0.7;
            double forwardDist = launchTick * speed;
            double yArc = Math.sin((launchTick / (config.getDurationTicks() - 40.0)) * Math.PI) * 4.0;

            teleportTrident(c, 6.0f + (float) yArc, forwardDist, 0);

            // Particles at prong tips
            if (tick % 2 == 0) {
                Location tipLoc = prongBlocks.get(1).entity().getLocation();
                snowflakes(tipLoc, 4, 0.2);
                frostDust(tipLoc, 3, 0.15);
                endRod(tipLoc, 2, 0.1);
            }

            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.4f, 1.8f);
            }
        }

        private void teleportTrident(Location center, float yOffset, double forwardDist, float tiltAngle) {
            double fx = dirX * forwardDist;
            double fz = dirZ * forwardDist;

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = center.clone().add(fx, -4.0 + i * 0.8 + yOffset, fz);
                shaftBlocks.get(i).entity().teleport(loc);
            }

            double[][] prongLocalY = {{2.5, 0}, {3.7, 0}, {2.2, -0.5}, {3.2, -0.5}, {2.2, 0.5}, {3.2, 0.5}};
            for (int i = 0; i < prongBlocks.size(); i++) {
                Location loc = center.clone().add(fx + prongLocalY[i][1], prongLocalY[i][0] + yOffset, fz);
                prongBlocks.get(i).entity().teleport(loc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostTrident(plugin); }
    }

    // ================================================================
    // 11. GLACIAL FLAIL -- 6 ball blocks + 4 chain blocks + 4 handle blocks
    // Swinging pendulum motion, slams into ground periodically
    // ================================================================
    public static class GlacialFlail extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ballBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> chainBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();

        public GlacialFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_flail", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ball: 6 blocks forming a spiked sphere
            Material[] ballMats = {Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE,
                    Material.PACKED_ICE, Material.PRISMARINE, Material.PRISMARINE};
            double[][] ballOffsets = {
                    {0, 0, 0}, {0.5, 0, 0}, {-0.5, 0, 0},
                    {0, 0.5, 0}, {0, 0, 0.5}, {0, 0, -0.5}
            };
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(ballOffsets[i][0], 1.0 + ballOffsets[i][1], ballOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, ballMats[i]);
                float s = i < 4 ? 0.6f : 0.35f; // spikes smaller
                h.scale(s, s, s).glow(FR, FG, FB).interpolation(3, 0);
                ballBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Chain: 4 links connecting ball to handle
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 2.0 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.25f, 0.8f, 0.25f).glow(FR, FG, FB).interpolation(3, 0);
                chainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 4 blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 5.5 + i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.4f, 0.9f, 0.4f).glow(200, 220, 240).interpolation(3, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Handle stays at top; ball swings like a pendulum
            double pivotY = 8.0;
            double chainLength = 5.0;

            // Pendulum: swing angle oscillates
            double swingAngle = Math.sin(tick * 0.08) * (Math.PI * 0.6); // ~108 degrees max swing

            // Ball position (hanging from pivot, swinging)
            double ballX = Math.sin(swingAngle) * chainLength;
            double ballY = pivotY - Math.cos(swingAngle) * chainLength;

            // Teleport ball
            double[][] ballOffsets = {
                    {0, 0, 0}, {0.5, 0, 0}, {-0.5, 0, 0},
                    {0, 0.5, 0}, {0, 0, 0.5}, {0, 0, -0.5}
            };
            for (int i = 0; i < ballBlocks.size(); i++) {
                ballBlocks.get(i).entity().teleport(c.clone().add(
                        ballX + ballOffsets[i][0], ballY + ballOffsets[i][1], ballOffsets[i][2]));
            }

            // Chain links interpolated between pivot and ball
            for (int i = 0; i < chainBlocks.size(); i++) {
                double t = (i + 1.0) / (chainBlocks.size() + 1);
                double cx = ballX * t;
                double cy = pivotY + (ballY - pivotY) * t;
                chainBlocks.get(i).entity().teleport(c.clone().add(cx, cy, 0));
            }

            // Handle stays at pivot
            for (int i = 0; i < handleBlocks.size(); i++) {
                handleBlocks.get(i).entity().teleport(c.clone().add(0, pivotY + 0.5 + i * 0.7, 0));
            }

            // Slam detection: when ball is near lowest point and moving fast
            double swingVelocity = Math.cos(tick * 0.08) * 0.08 * (Math.PI * 0.6);
            if (ballY < pivotY - chainLength * 0.95 && Math.abs(swingVelocity) < 0.02) {
                Location impactLoc = c.clone().add(ballX, ballY, 0);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.4f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                // Frost shockwave
                DisplayBuilder.particleRing(impactLoc, 2.0, Particle.SNOWFLAKE, 16, null);
                DisplayBuilder.particleRing(impactLoc, 4.0, Particle.SNOWFLAKE, 24, null);
            }

            // Trailing particles on ball
            if (tick % 2 == 0) {
                Location ballLoc = c.clone().add(ballX, ballY, 0);
                snowflakes(ballLoc, 5, 0.4);
                frostDust(ballLoc, 3, 0.3);
            }

            if (tick % 4 == 0) {
                endRod(c.clone().add(ballX, ballY, 0), 2, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialFlail(plugin); }
    }

    // ================================================================
    // 12. ICE JAVELIN RAIN -- 12 javelins launched up then raining down
    // Phase 1: 12 javelins rise from center, Phase 2: rain down on player area
    // ================================================================
    public static class IceJavelinRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> javelins = new ArrayList<>();
        private final double[] javelinOffsetX = new double[12];
        private final double[] javelinOffsetZ = new double[12];
        private final double[] targetX = new double[12];
        private final double[] targetZ = new double[12];
        private boolean raining = false;

        public IceJavelinRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_javelin_rain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find nearest player for rain target area
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            Location targetArea = nearest != null ? nearest.getLocation().clone() : center.clone();

            // 12 javelins in a ring pattern at spawn
            Material[] mats = {Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE,
                    Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE,
                    Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE};
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                javelinOffsetX[i] = Math.cos(angle) * 2.0;
                javelinOffsetZ[i] = Math.sin(angle) * 2.0;

                // Random target positions around the player
                targetX[i] = targetArea.getX() + (Math.random() - 0.5) * 12.0;
                targetZ[i] = targetArea.getZ() + (Math.random() - 0.5) * 12.0;

                Location loc = center.clone().add(javelinOffsetX[i], 0, javelinOffsetZ[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                h.scale(0.25f, 2.0f, 0.25f).glow(FR, FG, FB).interpolation(3, 0);
                javelins.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.2f, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (tick < 40) {
                // Phase 1: All javelins rise upward from center
                float riseY = (tick / 40.0f) * 20.0f;
                for (int i = 0; i < javelins.size(); i++) {
                    Location loc = c.clone().add(javelinOffsetX[i], riseY, javelinOffsetZ[i]);
                    javelins.get(i).entity().teleport(loc);
                }

                if (tick % 4 == 0) {
                    for (int i = 0; i < javelins.size(); i += 3) {
                        Location jLoc = javelins.get(i).entity().getLocation();
                        snowflakes(jLoc, 3, 0.2);
                    }
                }

                if (tick == 35) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.5f);
                }
                return;
            }

            if (!raining) {
                raining = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.5f, 0.6f);
            }

            // Phase 2: Javelins rain down to their target positions
            // Stagger: each javelin starts falling at a different time
            for (int i = 0; i < javelins.size(); i++) {
                int staggerDelay = i * 3; // 3 ticks apart
                int rainTick = tick - 40 - staggerDelay;

                if (rainTick < 0) {
                    // Still at apex
                    Location loc = c.clone().add(javelinOffsetX[i], 20.0, javelinOffsetZ[i]);
                    javelins.get(i).entity().teleport(loc);
                    continue;
                }

                int fallDuration = 20;
                if (rainTick < fallDuration) {
                    // Falling: interpolate from apex position to target
                    double t = rainTick / (double) fallDuration;
                    double startX = c.getX() + javelinOffsetX[i];
                    double startZ = c.getZ() + javelinOffsetZ[i];
                    double currentX = startX + (targetX[i] - startX) * t;
                    double currentZ = startZ + (targetZ[i] - startZ) * t;
                    double currentY = 20.0 * (1.0 - t) + c.getY() * t;

                    Location loc = new Location(c.getWorld(), currentX, currentY, currentZ);
                    javelins.get(i).entity().teleport(loc);

                    // Trail
                    if (rainTick % 2 == 0) {
                        snowflakes(loc, 3, 0.15);
                        frostDust(loc, 2, 0.1);
                    }
                } else if (rainTick == fallDuration) {
                    // Impact
                    Location impactLoc = new Location(c.getWorld(), targetX[i], c.getY(), targetZ[i]);
                    javelins.get(i).entity().teleport(impactLoc);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                    snowflakes(impactLoc, 15, 0.8);
                    frostDust(impactLoc, 10, 0.6);
                    endRod(impactLoc, 5, 0.4);
                } else {
                    // Embedded in ground
                    Location impactLoc = new Location(c.getWorld(), targetX[i], c.getY(), targetZ[i]);
                    javelins.get(i).entity().teleport(impactLoc);
                    if (rainTick % 6 == 0) {
                        snowflakes(impactLoc, 2, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IceJavelinRain(plugin); }
    }

    // ================================================================
    // 13. FROST WHIP -- 14 thin segments forming a sine-wave lash
    // Tip moves fastest, cracks toward player with wave motion
    // ================================================================
    public static class FrostWhip extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private double dirX, dirZ;

        public FrostWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_whip", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Direction toward nearest player
            Player nearest = null;
            double bestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < bestDist) { bestDist = d; nearest = p; }
            }
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.1) { dirX = 1; dirZ = 0; } else { dirX = dx / len; dirZ = dz / len; }
            } else { dirX = 1; dirZ = 0; }

            // 14 segments: base thicker, tip thinner. Alternating ice materials
            Material[] mats = {Material.WHITE_CONCRETE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE,
                    Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE,
                    Material.PACKED_ICE, Material.BLUE_ICE, Material.ICE, Material.LIGHT_BLUE_STAINED_GLASS,
                    Material.PACKED_ICE, Material.BLUE_ICE};
            for (int i = 0; i < 14; i++) {
                double dist = i * 0.7;
                Location loc = center.clone().add(dirX * dist, 2.0, dirZ * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                float thickness = 0.45f - (i * 0.025f); // thins toward tip
                h.scale(thickness, thickness, 0.7f).glow(FR, FG, FB).interpolation(3, 0);
                segments.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lash cycle: crack forward (20t) + retract (30t) + pause (10t) = 60t
            int cycleTick = tick % 60;

            // Perpendicular direction for sine wave
            double perpX = -dirZ;
            double perpZ = dirX;

            for (int i = 0; i < segments.size(); i++) {
                double segDist = i * 0.7;
                double segProgress = (double) i / segments.size(); // 0 (base) to 1 (tip)

                double forwardDist;
                double sineAmplitude;

                if (cycleTick < 20) {
                    // Crack forward: tip extends out faster than base
                    double crackProgress = cycleTick / 20.0;
                    // Tip moves fastest (exponential curve)
                    double speedMult = 1.0 + segProgress * 3.0;
                    forwardDist = segDist + crackProgress * speedMult * 2.0;
                    // Sine wave travels from base to tip
                    double wavePhase = tick * 0.4 - i * 0.5;
                    sineAmplitude = Math.sin(wavePhase) * (0.3 + segProgress * 1.2);

                    if (cycleTick == 0 && i == 0) {
                        DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.0f);
                    }
                } else if (cycleTick < 50) {
                    // Retract
                    double retractProgress = (cycleTick - 20) / 30.0;
                    double crackDist = segDist + (1.0 + segProgress * 3.0) * 2.0;
                    forwardDist = crackDist * (1.0 - retractProgress) + segDist * retractProgress;
                    double wavePhase = tick * 0.3 - i * 0.4;
                    sineAmplitude = Math.sin(wavePhase) * (0.5 + segProgress * 0.5) * (1.0 - retractProgress);
                } else {
                    // Pause at rest
                    forwardDist = segDist;
                    sineAmplitude = Math.sin(tick * 0.1 - i * 0.3) * 0.15; // gentle idle sway
                }

                double ox = dirX * forwardDist + perpX * sineAmplitude;
                double oz = dirZ * forwardDist + perpZ * sineAmplitude;
                Location loc = c.clone().add(ox, 2.0, oz);
                segments.get(i).entity().teleport(loc);
            }

            // Tip crack particles
            if (cycleTick < 20 && tick % 2 == 0) {
                Location tipLoc = segments.get(segments.size() - 1).entity().getLocation();
                snowflakes(tipLoc, 6, 0.4);
                frostDust(tipLoc, 5, 0.3);
                endRod(tipLoc, 3, 0.2);
            }

            // Crack sound at apex
            if (cycleTick == 18) {
                Location tipLoc = segments.get(segments.size() - 1).entity().getLocation();
                DisplayBuilder.playSound(tipLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 1.5f);
                DisplayBuilder.playSound(tipLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.8f);
            }

            // Ambient snow along whip
            if (tick % 5 == 0) {
                for (int i = 0; i < segments.size(); i += 3) {
                    Location sLoc = segments.get(i).entity().getLocation();
                    snowflakes(sLoc, 2, 0.15);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostWhip(plugin); }
    }
}
