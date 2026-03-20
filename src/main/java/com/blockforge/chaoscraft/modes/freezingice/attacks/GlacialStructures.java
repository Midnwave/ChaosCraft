package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class GlacialStructures {
    private GlacialStructures() {}

    // Ice palette helpers
    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.2f);
    private static final Particle.DustOptions FROST_DUST = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.0f);

    private static Material iceMat(int i) {
        return switch (i % 4) {
            case 0 -> Material.BLUE_ICE;
            case 1 -> Material.PACKED_ICE;
            case 2 -> Material.ICE;
            default -> Material.SNOW_BLOCK;
        };
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IcebergRise(plugin));
        registry.register(new FrostWall(plugin));
        registry.register(new GlacierCrush(plugin));
        registry.register(new IcePillarCircle(plugin));
        registry.register(new CrystalCave(plugin));
        registry.register(new FrozenArch(plugin));
        registry.register(new IceSheet(plugin));
        registry.register(new GlacialSpire(plugin));
        registry.register(new FrostDome(plugin));
        registry.register(new IceBridge(plugin));
        registry.register(new PermafrostRidge(plugin));
        registry.register(new CrystalTower(plugin));
        registry.register(new FrozenMonolith(plugin));
    }

    // ================================================================
    // 1. ICEBERG RISE — 18 blocks rising from ground, tilts slightly
    // ================================================================
    public static class IcebergRise extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float emergeProgress = 0;

        public IcebergRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iceberg_rise", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Core column: 8 blocks stacked (start underground)
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, -10 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, i < 4 ? Material.BLUE_ICE : Material.PACKED_ICE);
                float taper = 1.2f - (i * 0.08f);
                h.scale(taper, 1.0f, taper).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Base flare: 6 blocks splayed outward
            double[][] baseOff = {{1.5,0,0},{-1.5,0,0},{0,0,1.5},{0,0,-1.5},{1,0,1},{-1,0,-1}};
            for (int i = 0; i < baseOff.length; i++) {
                Location loc = center.clone().add(baseOff[i][0], -10, baseOff[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(1.0f, 0.7f, 1.0f).glow(220, 240, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Detail shards: 4 angled pieces at top
            double[][] shardOff = {{0.6,8,0.3},{-0.5,8,-0.4},{0.3,7,0.6},{-0.4,7,-0.5}};
            for (int i = 0; i < shardOff.length; i++) {
                Location loc = center.clone().add(shardOff[i][0], -10 + shardOff[i][1], shardOff[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.5f, 0.8f, 0.5f).glow(100, 180, 255).interpolation(3, 0);
                h.rotate(0.3f + i * 0.15f, 1, 0, 1);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 100, 180, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Emerge over 50 ticks
            if (tick <= 50) {
                emergeProgress = tick / 50.0f;
                float yOffset = emergeProgress * 10.0f;
                for (int i = 0; i < all.size(); i++) {
                    BlockDisplay bd = all.get(i).entity();
                    Location cur = bd.getLocation();
                    bd.teleport(cur.clone().add(0, yOffset / 50.0f, 0));
                }
                // Tilt slightly as it rises
                float tilt = emergeProgress * 0.15f;
                for (BlockDisplayHandle h : all) {
                    h.rotate(tilt, 0, 0, 1);
                    h.interpolation(5, 0);
                }
            }
            // Phase 2: Tremble and damage
            else {
                float tremble = (float) Math.sin(tick * 1.5) * 0.1f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    Location base = bd.getLocation();
                    bd.teleport(base.clone().add(0, tremble - (float) Math.sin((tick - 1) * 1.5) * 0.1f, 0));
                }
            }

            // Frost particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 8, 1.5, 3.0, 1.5, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 5, 1.5, 100, 180, 255, 1.0f);
            }
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IcebergRise(plugin); }
    }

    // ================================================================
    // 2. FROST WALL — 20 blocks, 8 wide x 4 tall, slides forward
    // ================================================================
    public static class FrostWall extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private double slideOffset = 0;

        public FrostWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_wall", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Main wall: 4 tall x 5 wide = 20 blocks (mix materials)
            for (int x = -2; x <= 2; x++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(x * 1.1, y, -8);
                    Material mat = (y == 3) ? Material.LIGHT_BLUE_STAINED_GLASS :
                                   (y == 0) ? Material.BLUE_ICE :
                                   iceMat(x + y);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float scaleX = 1.1f;
                    float scaleY = (y == 3) ? 0.8f : 1.0f;
                    h.scale(scaleX, scaleY, 1.2f).glow(100, 180, 255).interpolation(2, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.6f);
            DisplayBuilder.dustParticles(center.clone().add(0, 2, -8), 50, 3.0, 220, 240, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slide wall forward (toward player) over duration
            double speed = 0.12;
            slideOffset += speed;

            for (BlockDisplayHandle h : all) {
                BlockDisplay bd = h.entity();
                Location loc = bd.getLocation();
                bd.teleport(loc.clone().add(0, 0, speed));
            }

            // Update damage center to wall's current position
            setCenter(c.clone().add(0, 0, slideOffset - 8));

            // Frost trail behind wall
            if (tick % 2 == 0) {
                for (int x = -2; x <= 2; x++) {
                    Location trail = c.clone().add(x * 1.1, 0.5, slideOffset - 9);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, trail, 3, 0.3, 0.2, 0.3, 0.01);
                }
            }
            // Ice dust along wall face
            if (tick % 4 == 0) {
                Location wallCenter = c.clone().add(0, 2, slideOffset - 8);
                DisplayBuilder.dustParticles(wallCenter, 10, 2.5, 100, 180, 255, 1.2f);
                c.getWorld().spawnParticle(Particle.END_ROD, wallCenter, 3, 2.0, 1.5, 0.3, 0.01);
            }
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 0, slideOffset - 8), Sound.BLOCK_POWDER_SNOW_STEP, 0.7f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostWall(plugin); }
    }

    // ================================================================
    // 3. GLACIER CRUSH — 24 blocks, two walls closing from sides
    // ================================================================
    public static class GlacierCrush extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> leftWall = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWall = new ArrayList<>();
        private double closeOffset = 0;

        public GlacierCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_crush", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Left wall: 12 blocks (3 wide x 4 tall) at x=-10
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(-10, y, z * 1.1);
                    Material mat = (y < 2) ? Material.BLUE_ICE : Material.PACKED_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(1.3f, 1.0f, 1.1f).glow(100, 180, 255).interpolation(2, 0);
                    leftWall.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Right wall: 12 blocks at x=+10
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(10, y, z * 1.1);
                    Material mat = (y < 2) ? Material.BLUE_ICE : Material.PACKED_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(1.3f, 1.0f, 1.1f).glow(100, 180, 255).interpolation(2, 0);
                    rightWall.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Close walls inward over 80 ticks, then hold
            if (tick <= 80) {
                double speed = 0.125; // ~10 blocks in 80 ticks
                closeOffset += speed;

                for (BlockDisplayHandle h : leftWall) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(speed, 0, 0));
                }
                for (BlockDisplayHandle h : rightWall) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(-speed, 0, 0));
                }
            }

            // Crushing particles where walls meet
            if (tick > 60 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 15, 1.0, 2.0, 1.0, 0.05);
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 8, 1.0, 220, 240, 255, 1.5f);
            }
            // Rumbling dust from each wall
            if (tick % 5 == 0) {
                double leftX = -10 + closeOffset;
                double rightX = 10 - closeOffset;
                DisplayBuilder.dustParticles(c.clone().add(leftX, 2, 0), 5, 0.5, 100, 180, 255, 1.0f);
                DisplayBuilder.dustParticles(c.clone().add(rightX, 2, 0), 5, 0.5, 100, 180, 255, 1.0f);
            }
            if (tick % 8 == 0 && tick <= 80) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.6f + (tick / 160.0f));
            }
            // Impact crunch when walls meet
            if (tick == 80) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.2f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2, 0), 30, 0.5, 2.0, 0.5, 0.1);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacierCrush(plugin); }
    }

    // ================================================================
    // 4. ICE PILLAR CIRCLE — 18 blocks (6 pillars of 3) around player
    // ================================================================
    public static class IcePillarCircle extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float emergeProgress = 0;

        public IcePillarCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_pillar_circle", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double radius = 5.0;
            // 6 pillars, each 3 blocks tall, arranged in circle
            for (int p = 0; p < 6; p++) {
                double angle = (2 * Math.PI * p) / 6;
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;

                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(px, -6 + y, pz);
                    Material mat = (y == 2) ? Material.LIGHT_BLUE_STAINED_GLASS :
                                   (y == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float taper = 1.0f - (y * 0.15f);
                    h.scale(taper, 1.2f, taper).glow(100, 180, 255).interpolation(3, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.particleRing(center, radius, Particle.SNOWFLAKE, 24, null);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 40 ticks
            if (tick <= 40) {
                emergeProgress = tick / 40.0f;
                float yOffset = emergeProgress * 6.0f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, yOffset / 40.0f, 0));
                }
                // Progressive scale
                float scale = 0.2f + emergeProgress * 0.8f;
                for (int i = 0; i < all.size(); i++) {
                    int yIndex = i % 3;
                    float taper = (1.0f - (yIndex * 0.15f)) * scale;
                    all.get(i).scale(taper, 1.2f * scale, taper);
                    all.get(i).interpolation(4, 0);
                }
            }
            // Tremble phase
            else {
                float tremble = (float) Math.sin(tick * 2.0) * 0.08f;
                for (BlockDisplayHandle h : all) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, tremble - (float) Math.sin((tick - 1) * 2.0) * 0.08f, 0));
                }
            }

            // Frost particles between pillars
            if (tick % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), 5.0, Particle.SNOWFLAKE, 12, null);
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 4.0, 100, 180, 255, 1.0f);
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.7f);
            }
            // Sequential rise sound per pillar
            if (tick > 0 && tick <= 36 && tick % 6 == 0) {
                int pillarIdx = (tick / 6) - 1;
                if (pillarIdx < 6) {
                    double angle = (2 * Math.PI * pillarIdx) / 6;
                    Location pLoc = c.clone().add(Math.cos(angle) * 5, 1, Math.sin(angle) * 5);
                    DisplayBuilder.playSound(pLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.6f + pillarIdx * 0.1f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IcePillarCircle(plugin); }
    }

    // ================================================================
    // 5. CRYSTAL CAVE — 16 blocks (8 stalactites + 8 stalagmites)
    // ================================================================
    public static class CrystalCave extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> stalactites = new ArrayList<>();
        private final List<BlockDisplayHandle> stalagmites = new ArrayList<>();

        public CrystalCave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cave", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 8 stalactites from above (start at y+10, descend)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double r = 2.0 + (i % 3) * 0.8;
                Location loc = center.clone().add(Math.cos(angle) * r, 10, Math.sin(angle) * r);
                Material mat = (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 1.5f, 0.6f).glow(100, 180, 255).interpolation(3, 0);
                h.rotate(3.14159f, 1, 0, 0); // Point downward
                stalactites.add(h);
                spawnedEntities.add(h.entity());
            }
            // 8 stalagmites from below (start at y-6, rise)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.PI / 8; // Offset from stalactites
                double r = 2.0 + (i % 3) * 0.8;
                Location loc = center.clone().add(Math.cos(angle) * r, -6, Math.sin(angle) * r);
                Material mat = (i % 2 == 0) ? Material.ICE : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 1.5f, 0.6f).glow(220, 240, 255).interpolation(3, 0);
                stalagmites.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Stalactites descend, stalagmites rise — meet at y+2 over 60 ticks
            if (tick <= 60) {
                double progress = tick / 60.0;
                // Stalactites move down: from y+10 to y+2 = -8 total
                double stalDelta = -8.0 / 60.0;
                for (BlockDisplayHandle h : stalactites) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, stalDelta, 0));
                }
                // Stalagmites move up: from y-6 to y+2 = +8 total
                double stagDelta = 8.0 / 60.0;
                for (BlockDisplayHandle h : stalagmites) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, stagDelta, 0));
                }

                // Progressive scale
                float s = 0.3f + (float) progress * 0.7f;
                for (BlockDisplayHandle h : stalactites) {
                    h.scale(0.6f * s, 1.5f * s, 0.6f * s);
                    h.interpolation(4, 0);
                }
                for (BlockDisplayHandle h : stalagmites) {
                    h.scale(0.6f * s, 1.5f * s, 0.6f * s);
                    h.interpolation(4, 0);
                }
            }
            // Impact when they meet
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2, 0), 40, 2.0, 1.0, 2.0, 0.05);
            }

            // Ambient frost particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 10, 3.0, 3.0, 3.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, 2.0, 100, 180, 255, 1.0f);
            }
            // Drip particles from stalactites
            if (tick > 60 && tick % 6 == 0) {
                for (BlockDisplayHandle h : stalactites) {
                    Location tip = h.entity().getLocation().clone().add(0, -0.5, 0);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, tip, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalCave(plugin); }
    }

    // ================================================================
    // 6. FROZEN ARCH — 14 arch blocks + chunks that break off and fall
    // ================================================================
    public static class FrozenArch extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fallingChunks = new ArrayList<>();
        private int nextChunkTick = 80;

        public FrozenArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_arch", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Arch: 14 blocks forming a semicircular arch in the XY plane
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * i / 13.0; // 0 to PI
                double x = Math.cos(angle) * 5.0;
                double y = Math.sin(angle) * 5.0;
                Location loc = center.clone().add(x, y - 4, 0);
                Material mat = (i == 0 || i == 13) ? Material.BLUE_ICE :
                               (i == 6 || i == 7) ? Material.LIGHT_BLUE_STAINED_GLASS :
                               iceMat(i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float archScale = (i >= 5 && i <= 8) ? 1.3f : 1.0f;
                h.scale(archScale, archScale, 1.2f).glow(100, 180, 255).interpolation(3, 0);
                // Rotate each block to follow the arch curve
                float rotAngle = (float)(angle - Math.PI / 2);
                h.rotate(rotAngle, 0, 0, 1);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 40, 3.0, 220, 240, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Scale up arch over first 30 ticks
            if (tick <= 30) {
                float s = 0.1f + (tick / 30.0f) * 0.9f;
                for (int i = 0; i < archBlocks.size(); i++) {
                    float archScale = (i >= 5 && i <= 8) ? 1.3f * s : 1.0f * s;
                    archBlocks.get(i).scale(archScale, archScale, 1.2f * s);
                    archBlocks.get(i).interpolation(4, 0);
                }
            }

            // Chunks break off and fall periodically
            if (tick >= nextChunkTick && tick < 250 && fallingChunks.size() < 8) {
                // Pick a random arch position for chunk origin
                int idx = (fallingChunks.size() * 3 + 2) % 14;
                double angle = Math.PI * idx / 13.0;
                double x = Math.cos(angle) * 5.0;
                double y = Math.sin(angle) * 5.0;
                Location chunkLoc = c.clone().add(x, y - 4, 0);

                BlockDisplayHandle chunk = displayBuilder.spawnBlock(chunkLoc, Material.ICE);
                chunk.scale(0.5f, 0.5f, 0.5f).glow(220, 240, 255).interpolation(2, 0);
                fallingChunks.add(chunk);
                spawnedEntities.add(chunk.entity());

                DisplayBuilder.playSound(chunkLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
                nextChunkTick = tick + 20 + (fallingChunks.size() * 3);
            }

            // Animate falling chunks
            for (BlockDisplayHandle chunk : fallingChunks) {
                BlockDisplay bd = chunk.entity();
                if (bd.isValid()) {
                    Location loc = bd.getLocation();
                    if (loc.getY() > c.getY() - 2) {
                        bd.teleport(loc.clone().add(0, -0.3, 0));
                    }
                }
            }

            // Frost particles along arch
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI * (i * 2 + 1) / 13.0;
                    double x = Math.cos(angle) * 5.0;
                    double y = Math.sin(angle) * 5.0;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(x, y - 4, 0), 2, 0.3, 0.3, 0.3, 0.01);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 4, 3.0, 100, 180, 255, 1.0f);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.4f, 0.9f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrozenArch(plugin); }
    }

    // ================================================================
    // 7. ICE SHEET — 16 flat blocks expanding outward at player level
    // ================================================================
    public static class IceSheet extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private double expandRadius = 0;

        public IceSheet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_sheet", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 16 flat ice panels in two concentric rings
            // Inner ring: 6 blocks at radius 0.5
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.1, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.1f, 0.15f, 0.1f).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Outer ring: 10 blocks at radius 1.0
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10 + Math.PI / 10;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 0.1, Math.sin(angle) * 1.0);
                Material mat = (i % 3 == 0) ? Material.LIGHT_BLUE_STAINED_GLASS :
                               (i % 2 == 0) ? Material.PACKED_ICE : Material.ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.1f, 0.15f, 0.1f).glow(220, 240, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expand outward over 60 ticks
            if (tick <= 60) {
                expandRadius = (tick / 60.0) * 8.0;
                float scaleXZ = 0.1f + (tick / 60.0f) * 1.8f;

                // Inner ring expands
                for (int i = 0; i < 6; i++) {
                    double angle = (2 * Math.PI * i) / 6;
                    double r = expandRadius * 0.5;
                    Location target = c.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                    all.get(i).entity().teleport(target);
                    all.get(i).scale(scaleXZ, 0.15f, scaleXZ);
                    all.get(i).interpolation(3, 0);
                }
                // Outer ring expands further
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI * i) / 10 + Math.PI / 10;
                    double r = expandRadius;
                    Location target = c.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                    all.get(6 + i).entity().teleport(target);
                    all.get(6 + i).scale(scaleXZ * 0.9f, 0.15f, scaleXZ * 0.9f);
                    all.get(6 + i).interpolation(3, 0);
                }
            }

            // Ice crackling effect on surface
            if (tick % 2 == 0) {
                double currentR = Math.min(expandRadius, 8.0);
                for (int i = 0; i < 4; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = Math.random() * currentR;
                    Location p = c.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.05, 0.1, 0.01);
                }
            }
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), Math.min(expandRadius, 8.0),
                        Particle.DUST, 16, ICE_DUST);
            }
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceSheet(plugin); }
    }

    // ================================================================
    // 8. GLACIAL SPIRE — 12 spire blocks + 4 orbiting shards, twisting up
    // ================================================================
    public static class GlacialSpire extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> orbitShards = new ArrayList<>();
        private float emergeProgress = 0;

        public GlacialSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_spire", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Spire: 12 blocks in a twisting column (start underground)
            for (int i = 0; i < 12; i++) {
                double twistAngle = i * 0.3;
                double twistR = 0.3 * Math.sin(twistAngle);
                Location loc = center.clone().add(twistR * Math.cos(twistAngle), -10 + i, twistR * Math.sin(twistAngle));
                Material mat = (i < 3) ? Material.BLUE_ICE :
                               (i < 7) ? Material.PACKED_ICE :
                               (i < 10) ? Material.ICE : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.2f - (i * 0.07f);
                h.scale(taper, 1.0f, taper).glow(100, 180, 255).interpolation(3, 0);
                // Twist rotation per block
                h.rotate(i * 0.25f, 0, 1, 0);
                spireBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // 4 orbiting shards
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 2, -5 + i * 2, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.4f, 0.7f, 0.4f).glow(220, 240, 255).interpolation(2, 0);
                h.rotate(0.5f + i * 0.3f, 1, 0, 1);
                orbitShards.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 50, 2.0, 100, 180, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise spire over 50 ticks
            if (tick <= 50) {
                emergeProgress = tick / 50.0f;
                float yDelta = 10.0f / 50.0f;
                for (BlockDisplayHandle h : spireBlocks) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, yDelta, 0));
                }
                for (BlockDisplayHandle h : orbitShards) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, yDelta, 0));
                }
            }

            // Orbit shards rotate around spire
            double orbitSpeed = 0.06;
            for (int i = 0; i < orbitShards.size(); i++) {
                double angle = tick * orbitSpeed + (2 * Math.PI * i) / 4;
                double orbitR = 2.5;
                double shardY = c.getY() + 1 + i * 2.5;
                if (tick <= 50) shardY = c.getY() - 5 + i * 2 + emergeProgress * 10;
                Location target = c.clone();
                target.setX(c.getX() + Math.cos(angle) * orbitR);
                target.setY(shardY);
                target.setZ(c.getZ() + Math.sin(angle) * orbitR);
                orbitShards.get(i).entity().teleport(target);
                orbitShards.get(i).rotate((float)(tick * 0.1), 0, 1, 0);
                orbitShards.get(i).interpolation(2, 0);
            }

            // Twist animation on spire blocks
            if (tick > 50) {
                for (int i = 0; i < spireBlocks.size(); i++) {
                    float twist = i * 0.25f + tick * 0.02f;
                    spireBlocks.get(i).rotate(twist, 0, 1, 0);
                    spireBlocks.get(i).interpolation(3, 0);
                }
            }

            // Particles spiraling up
            if (tick % 2 == 0) {
                double pAngle = tick * 0.2;
                double pR = 1.5;
                for (int i = 0; i < 3; i++) {
                    double a = pAngle + i * 2.1;
                    double py = (tick * 0.15 + i * 3) % 12;
                    Location pLoc = c.clone().add(Math.cos(a) * pR, py, Math.sin(a) * pR);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 12, 0), 2, 0.5, 0.5, 0.5, 0.02);
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacialSpire(plugin); }
    }

    // ================================================================
    // 9. FROST DOME — 20 dome panels rising and curving inward
    // ================================================================
    public static class FrostDome extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] panelAngles;
        private final double[] panelElevations;

        public FrostDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_dome", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            panelAngles = new double[20];
            panelElevations = new double[20];
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double domeRadius = 6.0;
            // Bottom ring: 10 panels
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                panelAngles[i] = angle;
                panelElevations[i] = 0.2; // Start elevation (radians from horizontal)
                Location loc = center.clone().add(Math.cos(angle) * domeRadius, 0, Math.sin(angle) * domeRadius);
                Material mat = (i % 3 == 0) ? Material.LIGHT_BLUE_STAINED_GLASS :
                               (i % 2 == 0) ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.5f, 1.2f, 1.5f).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }
            // Top ring: 10 panels (offset, higher)
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10 + Math.PI / 10;
                panelAngles[10 + i] = angle;
                panelElevations[10 + i] = 0.6;
                Location loc = center.clone().add(Math.cos(angle) * (domeRadius - 1.5), 3, Math.sin(angle) * (domeRadius - 1.5));
                Material mat = (i % 2 == 0) ? Material.ICE : Material.SNOW_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.3f, 1.0f, 1.3f).glow(220, 240, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.9f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 4.0, 100, 180, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double domeRadius = 6.0;
            // Animate dome closing over 70 ticks
            if (tick <= 70) {
                double progress = tick / 70.0;

                // Bottom ring: rise and curve inward
                for (int i = 0; i < 10; i++) {
                    double angle = panelAngles[i];
                    double elevation = progress * (Math.PI / 3); // 0 to 60 degrees
                    double r = domeRadius * Math.cos(elevation);
                    double y = domeRadius * Math.sin(elevation);
                    Location target = c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    all.get(i).entity().teleport(target);
                    // Tilt panels to follow dome curve
                    all.get(i).rotate((float) elevation, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                    all.get(i).interpolation(4, 0);
                }
                // Top ring: converge toward apex
                for (int i = 0; i < 10; i++) {
                    double angle = panelAngles[10 + i];
                    double elevation = progress * (Math.PI / 2.2);
                    double r = (domeRadius - 1.5) * Math.cos(elevation);
                    double y = 3 + (domeRadius - 1.5) * Math.sin(elevation);
                    Location target = c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    all.get(10 + i).entity().teleport(target);
                    all.get(10 + i).rotate((float) elevation * 1.2f, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                    all.get(10 + i).interpolation(4, 0);
                }
            }

            // Frost interior particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 12, 3.0, 2.0, 3.0, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 6, 3.0, 220, 240, 255, 1.0f);
            }
            // Ring at dome base
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), domeRadius, Particle.DUST, 20, FROST_DUST);
            }
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.6f, 0.7f);
            }
            // Seal sound when dome closes
            if (tick == 70) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.3f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 6, 0), 25, 1.0, 0.5, 1.0, 0.05);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostDome(plugin); }
    }

    // ================================================================
    // 10. ICE BRIDGE — 14 blocks extending then collapsing behind
    // ================================================================
    public static class IceBridge extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private int extendedCount = 0;
        private final boolean[] collapsed = new boolean[14];

        public IceBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 14 bridge segments extending in +X direction, each starts tiny
            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add(i * 1.2, 3, 0);
                Material mat = (i == 0 || i == 13) ? Material.BLUE_ICE :
                               (i % 3 == 0) ? Material.PACKED_ICE :
                               (i % 3 == 1) ? Material.ICE : Material.SNOW_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Extend bridge: one segment every 5 ticks
            if (tick % 5 == 0 && extendedCount < 14) {
                BlockDisplayHandle h = all.get(extendedCount);
                h.scale(1.3f, 0.8f, 1.5f);
                h.interpolation(4, 0);
                DisplayBuilder.playSound(h.entity().getLocation(), Sound.BLOCK_GLASS_BREAK, 0.4f, 0.8f + extendedCount * 0.05f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, h.entity().getLocation(), 8, 0.5, 0.3, 0.5, 0.02);
                extendedCount++;
            }

            // Collapse behind: start collapsing once 6+ segments are built, 8 ticks after extension
            int collapseDelay = 40; // ticks after extension to start collapsing
            for (int i = 0; i < extendedCount - 3; i++) {
                int extendTick = (i + 1) * 5;
                if (tick > extendTick + collapseDelay && !collapsed[i]) {
                    collapsed[i] = true;
                    BlockDisplayHandle h = all.get(i);
                    // Collapse: scale to zero and drop
                    h.scale(0.3f, 0.1f, 0.3f);
                    h.interpolation(6, 0);
                    h.entity().teleport(h.entity().getLocation().clone().add(0, -3, 0));
                    DisplayBuilder.playSound(h.entity().getLocation(), Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, h.entity().getLocation().clone().add(0, 3, 0), 5, 0.3, 0.2, 0.3, 0.03);
                }
            }

            // Frost trail particles on active bridge
            if (tick % 3 == 0) {
                for (int i = 0; i < extendedCount; i++) {
                    if (!collapsed[i]) {
                        Location segLoc = all.get(i).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, segLoc.clone().add(0, -0.3, 0), 1, 0.3, 0.1, 0.3, 0.01);
                    }
                }
            }
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(extendedCount * 0.6, 3, 0), 4, 1.0, 100, 180, 255, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceBridge(plugin); }
    }

    // ================================================================
    // 11. PERMAFROST RIDGE — 16 blocks erupting along a line, each taller
    // ================================================================
    public static class PermafrostRidge extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private int eruptedCount = 0;

        public PermafrostRidge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_ridge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 16 blocks along a line in +Z direction, each starts underground
            for (int i = 0; i < 16; i++) {
                Location loc = center.clone().add(0, -5, i * 1.0 - 8);
                Material mat = (i < 4) ? Material.BLUE_ICE :
                               (i < 8) ? Material.PACKED_ICE :
                               (i < 12) ? Material.ICE : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                // Each block taller than the last
                float height = 0.8f + (i * 0.2f);
                float width = 1.0f - (i * 0.02f);
                h.scale(width, height, width).glow(100, 180, 255).interpolation(3, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sequential eruption: one block every 3 ticks
            if (tick % 3 == 0 && eruptedCount < 16) {
                BlockDisplayHandle h = all.get(eruptedCount);
                // Launch upward
                float targetY = 5.0f + eruptedCount * 0.3f;
                Location target = c.clone().add(0, targetY, eruptedCount * 1.0 - 8);
                h.entity().teleport(target);

                // Scale pop animation
                float height = 0.8f + (eruptedCount * 0.2f);
                float width = 1.0f - (eruptedCount * 0.02f);
                h.scale(width * 1.3f, height * 1.2f, width * 1.3f);
                h.interpolation(5, 0);

                // Eruption effects
                DisplayBuilder.playSound(target, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f + eruptedCount * 0.06f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, target, 10, 0.5, 1.0, 0.5, 0.05);
                DisplayBuilder.dustParticles(target, 6, 0.5, 100, 180, 255, 1.2f);

                eruptedCount++;
            }

            // Settle animation: scale back after eruption pop
            for (int i = 0; i < eruptedCount; i++) {
                int eruptTick = i * 3;
                if (tick == eruptTick + 8) {
                    float height = 0.8f + (i * 0.2f);
                    float width = 1.0f - (i * 0.02f);
                    all.get(i).scale(width, height, width);
                    all.get(i).interpolation(6, 0);
                }
            }

            // Tremble on erupted blocks
            if (tick > 48 && tick % 2 == 0) {
                float tremble = (float) Math.sin(tick * 1.8) * 0.06f;
                for (int i = 0; i < eruptedCount; i++) {
                    BlockDisplay bd = all.get(i).entity();
                    bd.teleport(bd.getLocation().clone().add(0, tremble - (float) Math.sin((tick - 2) * 1.8) * 0.06f, 0));
                }
            }

            // Ridge frost line particles
            if (tick % 4 == 0) {
                for (int i = 0; i < Math.min(eruptedCount, 16); i++) {
                    Location base = all.get(i).entity().getLocation().clone().add(0, -0.5, 0);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, base, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PermafrostRidge(plugin); }
    }

    // ================================================================
    // 12. CRYSTAL TOWER — 14 tower blocks that shatter into projectile shards
    // ================================================================
    public static class CrystalTower extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shardProjectiles = new ArrayList<>();
        private boolean shattered = false;
        private int shatterTick = 100;

        public CrystalTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_tower", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Tower: 14 blocks stacking up, alternating materials, tapering
            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add(0, -10 + i, 0);
                Material mat = (i < 3) ? Material.BLUE_ICE :
                               (i < 6) ? Material.PACKED_ICE :
                               (i < 10) ? Material.ICE :
                               (i < 13) ? Material.LIGHT_BLUE_STAINED_GLASS : Material.PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.3f - (i * 0.06f);
                h.scale(taper, 1.0f, taper).glow(100, 180, 255).interpolation(3, 0);
                towerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 100, 180, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise tower over 50 ticks
            if (tick <= 50) {
                float yDelta = 10.0f / 50.0f;
                for (BlockDisplayHandle h : towerBlocks) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, yDelta, 0));
                }
                // Scale in
                float s = 0.1f + (tick / 50.0f) * 0.9f;
                for (int i = 0; i < towerBlocks.size(); i++) {
                    float taper = (1.3f - (i * 0.06f)) * s;
                    towerBlocks.get(i).scale(taper, s, taper);
                    towerBlocks.get(i).interpolation(4, 0);
                }
            }

            // Tower pulses before shatter
            if (tick > 70 && tick < shatterTick) {
                float pulse = 1.0f + (float) Math.sin(tick * 0.5) * 0.1f;
                for (int i = 0; i < towerBlocks.size(); i++) {
                    float taper = (1.3f - (i * 0.06f)) * pulse;
                    towerBlocks.get(i).scale(taper, pulse, taper);
                    towerBlocks.get(i).interpolation(2, 0);
                }
                // Warning particles
                if (tick % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 7, 0), 5, 0.5, 5.0, 0.5, 0.02);
                }
            }

            // SHATTER at tick 100
            if (tick == shatterTick && !shattered) {
                shattered = true;
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.2f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 7, 0), 60, 1.0, 5.0, 1.0, 0.1);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 7, 0), 40, 2.0, 5.0, 2.0, 0.08);

                // Tower blocks shrink to zero
                for (BlockDisplayHandle h : towerBlocks) {
                    h.scale(0.0f, 0.0f, 0.0f);
                    h.interpolation(5, 0);
                }

                // Spawn shard projectiles flying outward
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    double shardY = 2 + (i % 5) * 2;
                    Location shardLoc = c.clone().add(0, shardY, 0);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.ICE);
                    shard.scale(0.4f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(2, 0);
                    shard.rotate((float)(angle * 2), 1, 1, 0);
                    shardProjectiles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
            }

            // Animate shards flying outward
            if (shattered && tick > shatterTick) {
                int shardTick = tick - shatterTick;
                for (int i = 0; i < shardProjectiles.size(); i++) {
                    BlockDisplayHandle shard = shardProjectiles.get(i);
                    if (!shard.entity().isValid()) continue;
                    double angle = (2 * Math.PI * i) / 12;
                    double speed = 0.4;
                    double dx = Math.cos(angle) * speed;
                    double dz = Math.sin(angle) * speed;
                    double dy = -0.05 * shardTick * 0.05; // Slight gravity
                    shard.entity().teleport(shard.entity().getLocation().clone().add(dx, dy, dz));
                    shard.rotate((float)(tick * 0.3), 1, 1, 0);
                    shard.interpolation(1, 0);

                    // Trail particles
                    if (shardTick % 2 == 0) {
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, shard.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Shard damage radius updates to spread area
                if (shardTick % 5 == 0 && shardTick < 40) {
                    triggerImpactDamage(c.clone().add(0, 3, 0));
                }
            }

            // Ambient tower particles (pre-shatter)
            if (!shattered && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 7, 0), 5, 0.5, 5.0, 0.5, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, 0.8, 100, 180, 255, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalTower(plugin); }
    }

    // ================================================================
    // 13. FROZEN MONOLITH — 1 massive (scale 4.0) + 10 detail blocks
    // ================================================================
    public static class FrozenMonolith extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle monolith;
        private final List<BlockDisplayHandle> details = new ArrayList<>();

        public FrozenMonolith(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_monolith", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Massive central monolith (scale 4.0) — starts underground
            Location monoLoc = center.clone().add(0, -12, 0);
            monolith = displayBuilder.spawnBlock(monoLoc, Material.BLUE_ICE);
            monolith.scale(4.0f, 6.0f, 4.0f).glow(100, 180, 255).interpolation(5, 0);
            spawnedEntities.add(monolith.entity());

            // 10 detail blocks around the base and sides
            // 4 base corner blocks
            double[][] cornerOff = {{2.5,0,2.5},{-2.5,0,2.5},{2.5,0,-2.5},{-2.5,0,-2.5}};
            for (double[] off : cornerOff) {
                Location loc = center.clone().add(off[0], -12, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.5f, 2.0f, 1.5f).glow(220, 240, 255).interpolation(5, 0);
                details.add(h);
                spawnedEntities.add(h.entity());
            }
            // 4 side accent blocks
            double[][] sideOff = {{3.0,3,0},{-3.0,3,0},{0,3,3.0},{0,3,-3.0}};
            for (double[] off : sideOff) {
                Location loc = center.clone().add(off[0], -12 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(1.0f, 1.5f, 1.0f).glow(100, 180, 255).interpolation(5, 0);
                h.rotate(0.3f, 0, 1, 0);
                details.add(h);
                spawnedEntities.add(h.entity());
            }
            // 2 crown blocks at top
            Location crownL = center.clone().add(0.8, -12 + 6, 0.8);
            BlockDisplayHandle c1 = displayBuilder.spawnBlock(crownL, Material.LIGHT_BLUE_STAINED_GLASS);
            c1.scale(1.0f, 2.0f, 1.0f).glow(220, 240, 255).interpolation(5, 0);
            c1.rotate(0.2f, 0, 0, 1);
            details.add(c1);
            spawnedEntities.add(c1.entity());

            Location crownR = center.clone().add(-0.8, -12 + 6.5, -0.8);
            BlockDisplayHandle c2 = displayBuilder.spawnBlock(crownR, Material.PRISMARINE);
            c2.scale(0.8f, 1.8f, 0.8f).glow(100, 180, 255).interpolation(5, 0);
            c2.rotate(-0.15f, 0, 0, 1);
            details.add(c2);
            spawnedEntities.add(c2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 60, 4.0, 100, 180, 255, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 70 ticks
            if (tick <= 70) {
                float yDelta = 12.0f / 70.0f;
                monolith.entity().teleport(monolith.entity().getLocation().clone().add(0, yDelta, 0));
                for (BlockDisplayHandle h : details) {
                    h.entity().teleport(h.entity().getLocation().clone().add(0, yDelta, 0));
                }

                // Progressive scale on monolith
                float s = 0.3f + (tick / 70.0f) * 0.7f;
                monolith.scale(4.0f * s, 6.0f * s, 4.0f * s);
                monolith.interpolation(5, 0);
            }

            // Monolith pulse / breathing effect
            if (tick > 70) {
                float pulse = 1.0f + (float) Math.sin(tick * 0.08) * 0.05f;
                monolith.scale(4.0f * pulse, 6.0f * pulse, 4.0f * pulse);
                monolith.interpolation(6, 0);

                // Slow tremble
                float tremble = (float) Math.sin(tick * 0.6) * 0.04f;
                monolith.entity().teleport(monolith.entity().getLocation().clone().add(
                        0, tremble - (float) Math.sin((tick - 1) * 0.6) * 0.04f, 0));
            }

            // Massive frost aura particles
            if (tick % 2 == 0) {
                // Snowflake blizzard around monolith
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4, 0), 15, 5.0, 4.0, 5.0, 0.03);
                // Colored dust aura
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 8, 5.0, 100, 180, 255, 1.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 5, 3.0, 220, 240, 255, 1.2f);
            }
            // End rod sparkle at crown
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 8, 0), 4, 1.0, 0.5, 1.0, 0.02);
            }
            // Ground frost ring
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 6.0, Particle.DUST, 24, ICE_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 8.0, Particle.DUST, 20, FROST_DUST);
            }
            // Deep rumble
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.3f);
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrozenMonolith(plugin); }
    }
}
