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

public final class FrozenArchitecture {
    private FrozenArchitecture() {}

    // Ice dust color presets
    private static final int ICE_R = 100, ICE_G = 180, ICE_B = 255;
    private static final int FROST_R = 220, FROST_G = 240, FROST_B = 255;

    private static void iceDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, ICE_R, ICE_G, ICE_B, 1.2f);
    }

    private static void frostDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, FROST_R, FROST_G, FROST_B, 1.0f);
    }

    private static void snowflakes(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, count, spread, spread, spread, 0.02);
    }

    private static void endRods(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, count, spread, spread, spread, 0.01);
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceCastle(plugin));
        registry.register(new FrozenBridge(plugin));
        registry.register(new CrystallineTower(plugin));
        registry.register(new IceThrone(plugin));
        registry.register(new FrostGate(plugin));
        registry.register(new GlacialAltar(plugin));
        registry.register(new IceObelisk(plugin));
        registry.register(new FrozenFountain(plugin));
        registry.register(new CrystalChandelier(plugin));
        registry.register(new IceStatue(plugin));
        registry.register(new FrostCathedral(plugin));
        registry.register(new GlacialPyramid(plugin));
        registry.register(new IceMirrorHall(plugin));
    }

    // =========================================================================
    // 1. ICE CASTLE — 20+ blocks forming miniature castle, builds block-by-block
    // =========================================================================
    public static class IceCastle extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[][] offsets = {
            // Base walls (8 blocks) — square perimeter
            {-2, 0, -2}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {2, 0, -2},
            {-2, 0, 2}, {-1, 0, 2}, {0, 0, 2}, {1, 0, 2}, {2, 0, 2},
            {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1},
            {2, 0, -1}, {2, 0, 0}, {2, 0, 1},
            // Corner towers (4 pillars, 2 high each = 4 blocks at Y+1)
            {-2, 1, -2}, {2, 1, -2}, {-2, 1, 2}, {2, 1, 2},
            // Battlements on towers (Y+2)
            {-2, 2, -2}, {2, 2, -2}, {-2, 2, 2}, {2, 2, 2},
        };
        private final Material[] mats = {
            Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.BLUE_ICE,
            Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE,
            Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE,
            Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE,
            Material.PRISMARINE, Material.PRISMARINE, Material.PRISMARINE, Material.PRISMARINE,
        };

        public IceCastle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_castle", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Build block-by-block: one block every 3 ticks
            int buildIndex = tick / 3;
            if (buildIndex < offsets.length && buildIndex == (tick / 3) && tick % 3 == 0) {
                double[] off = offsets[buildIndex];
                Location loc = c.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[buildIndex]);
                h.scale(0.95f, 0.95f, 0.95f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
                snowflakes(loc, 8, 0.3);
                DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.5f);
            }

            // Castle fully built — area damage pulse + ambient effects
            if (tick > offsets.length * 3) {
                if (tick % 20 == 0) {
                    iceDust(c.clone().add(0, 1, 0), 30, 4.0);
                    snowflakes(c.clone().add(0, 2, 0), 20, 3.0);
                    DisplayBuilder.particleRing(c, 5.0, Particle.SNOWFLAKE, 24, null);
                }
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.8f);
                }
            }

            // Glow shift on towers
            if (tick % 10 == 0 && all.size() > 16) {
                boolean blueCycle = (tick / 10) % 2 == 0;
                for (int i = 16; i < all.size(); i++) {
                    if (blueCycle) all.get(i).glow(ICE_R, ICE_G, ICE_B);
                    else all.get(i).glow(FROST_R, FROST_G, FROST_B);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceCastle(plugin); }
    }

    // =========================================================================
    // 2. FROZEN BRIDGE — 14 bridge blocks extending overhead + 4 icicles that fall
    // =========================================================================
    public static class FrozenBridge extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bridgeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private final List<Location> icicleTargets = new ArrayList<>();
        private boolean bridgeComplete = false;

        public FrozenBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setImpactDamage(50.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Build bridge span: 14 blocks from Z-7 to Z+6 at Y+6
            int buildIndex = tick / 4;
            if (!bridgeComplete && buildIndex < 14 && tick % 4 == 0) {
                double z = -7 + buildIndex;
                Location loc = c.clone().add(0, 6, z);
                Material mat = buildIndex % 3 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.2f, 0.4f, 1.0f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                bridgeBlocks.add(h);
                spawnedEntities.add(h.entity());
                snowflakes(loc, 6, 0.3);
                DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.6f);

                if (buildIndex == 13) bridgeComplete = true;
            }

            // Spawn 4 icicles hanging from bridge
            if (bridgeComplete && icicles.isEmpty()) {
                for (int i = 0; i < 4; i++) {
                    double z = -5 + i * 3;
                    Location loc = c.clone().add(0, 5.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                    h.scale(0.3f, 1.2f, 0.3f).glow(FROST_R, FROST_G, FROST_B).interpolation(3, 0);
                    icicles.add(h);
                    spawnedEntities.add(h.entity());
                    icicleTargets.add(c.clone().add(0, 0, z));
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }

            // Drop icicles periodically: one every 30 ticks after bridge is complete
            if (bridgeComplete && !icicles.isEmpty()) {
                int dropTick = tick - (14 * 4 + 5); // ticks since bridge finished
                if (dropTick > 0 && dropTick % 30 == 0) {
                    int dropIndex = (dropTick / 30) - 1;
                    if (dropIndex >= 0 && dropIndex < icicles.size()) {
                        BlockDisplay bd = icicles.get(dropIndex).entity();
                        Location target = icicleTargets.get(dropIndex);
                        bd.teleport(target);
                        bd.setInterpolationDuration(3);
                        bd.setInterpolationDelay(0);
                        triggerImpactDamage(target);
                        DisplayBuilder.playSound(target, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                        snowflakes(target, 20, 1.5);
                        iceDust(target, 15, 1.0);
                    }
                }
            }

            // Ambient snowflakes under bridge
            if (tick % 10 == 0 && bridgeComplete) {
                for (int i = 0; i < 3; i++) {
                    double rz = (Math.random() - 0.5) * 12;
                    snowflakes(c.clone().add(0, 4, rz), 5, 0.5);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrozenBridge(plugin); }
    }

    // =========================================================================
    // 3. CRYSTALLINE TOWER — 16 faceted crystal blocks, rotates on Y, glow shifts
    // =========================================================================
    public static class CrystallineTower extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();

        public CrystallineTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_tower", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // 16-block tower: 4 columns of 4, arranged in a tight diamond
            Material[] towerMats = {Material.BLUE_ICE, Material.PRISMARINE, Material.PACKED_ICE, Material.LIGHT_BLUE_STAINED_GLASS};
            double[][] colOffsets = {{0.4, 0, 0}, {-0.4, 0, 0}, {0, 0, 0.4}, {0, 0, -0.4}};

            for (int col = 0; col < 4; col++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(colOffsets[col][0], y * 1.1, colOffsets[col][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, towerMats[(col + y) % 4]);
                    h.scale(0.6f, 1.0f, 0.6f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                    towerBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);
            iceDust(center, 30, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Rotate entire tower around Y-axis
            double angle = (tick * 0.05);
            for (int col = 0; col < 4; col++) {
                double baseAngle = (Math.PI / 2) * col + angle;
                double ox = Math.cos(baseAngle) * 0.5;
                double oz = Math.sin(baseAngle) * 0.5;
                for (int y = 0; y < 4; y++) {
                    int idx = col * 4 + y;
                    if (idx >= towerBlocks.size()) break;
                    BlockDisplay bd = towerBlocks.get(idx).entity();
                    Location newLoc = c.clone().add(ox, y * 1.1, oz);
                    bd.teleport(newLoc);
                }
            }

            // Glow color shift: cycle blue -> white -> cyan
            if (tick % 15 == 0) {
                int cycle = (tick / 15) % 3;
                int gr, gg, gb;
                switch (cycle) {
                    case 0: gr = ICE_R; gg = ICE_G; gb = ICE_B; break;
                    case 1: gr = FROST_R; gg = FROST_G; gb = FROST_B; break;
                    default: gr = 0; gg = 220; gb = 240; break; // cyan
                }
                for (BlockDisplayHandle h : towerBlocks) {
                    h.glow(gr, gg, gb);
                }
            }

            // Ambient particles
            if (tick % 8 == 0) {
                endRods(c.clone().add(0, 2, 0), 5, 1.0);
                snowflakes(c.clone().add(0, 3.5, 0), 8, 0.8);
            }

            // Frost ring pulse every 40 ticks
            if (tick % 40 == 0) {
                DisplayBuilder.particleRing(c, 5.0, Particle.SNOWFLAKE, 20, null);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 1.2f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystallineTower(plugin); }
    }

    // =========================================================================
    // 4. ICE THRONE — 14 ornate throne blocks forming behind the nearest player
    // =========================================================================
    public static class IceThrone extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();
        private Location throneCenter;

        public IceThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_throne", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
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

            // Find nearest player and place throne behind them
            Player nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < minDist) { minDist = d; nearest = p; }
            }

            if (nearest != null) {
                // Behind the player: opposite their facing direction
                Location pLoc = nearest.getLocation();
                double yaw = Math.toRadians(pLoc.getYaw() + 180);
                throneCenter = pLoc.clone().add(Math.sin(yaw) * 2, 0, -Math.cos(yaw) * 2);
            } else {
                throneCenter = center.clone();
            }

            // Build throne structure: seat, back, armrests, ornamental top
            // Seat base (3 wide)
            spawnThroneBlock(throneCenter.clone().add(-1, 0, 0), Material.BLUE_ICE, 1.0f, 0.5f, 1.0f);
            spawnThroneBlock(throneCenter.clone().add(0, 0, 0), Material.PACKED_ICE, 1.0f, 0.5f, 1.0f);
            spawnThroneBlock(throneCenter.clone().add(1, 0, 0), Material.BLUE_ICE, 1.0f, 0.5f, 1.0f);
            // Back rest (3 blocks high, 3 wide)
            spawnThroneBlock(throneCenter.clone().add(-1, 0.5, -0.5), Material.PACKED_ICE, 0.2f, 1.5f, 0.2f);
            spawnThroneBlock(throneCenter.clone().add(0, 0.5, -0.5), Material.BLUE_ICE, 1.0f, 2.0f, 0.2f);
            spawnThroneBlock(throneCenter.clone().add(1, 0.5, -0.5), Material.PACKED_ICE, 0.2f, 1.5f, 0.2f);
            // High back center
            spawnThroneBlock(throneCenter.clone().add(0, 2.5, -0.5), Material.PRISMARINE, 0.6f, 0.8f, 0.2f);
            // Ornamental crown on top
            spawnThroneBlock(throneCenter.clone().add(0, 3.2, -0.5), Material.LIGHT_BLUE_STAINED_GLASS, 0.4f, 0.4f, 0.4f);
            // Armrests (left and right)
            spawnThroneBlock(throneCenter.clone().add(-1.2, 0.5, 0.3), Material.PACKED_ICE, 0.2f, 0.6f, 0.8f);
            spawnThroneBlock(throneCenter.clone().add(1.2, 0.5, 0.3), Material.PACKED_ICE, 0.2f, 0.6f, 0.8f);
            // Armrest tops
            spawnThroneBlock(throneCenter.clone().add(-1.2, 1.0, 0.3), Material.PRISMARINE, 0.3f, 0.3f, 0.3f);
            spawnThroneBlock(throneCenter.clone().add(1.2, 1.0, 0.3), Material.PRISMARINE, 0.3f, 0.3f, 0.3f);
            // Base platform extension
            spawnThroneBlock(throneCenter.clone().add(-1, -0.2, 0.5), Material.SNOW_BLOCK, 1.0f, 0.2f, 0.5f);
            spawnThroneBlock(throneCenter.clone().add(1, -0.2, 0.5), Material.SNOW_BLOCK, 1.0f, 0.2f, 0.5f);

            DisplayBuilder.playSound(throneCenter, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(throneCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
            iceDust(throneCenter, 40, 2.5);
        }

        private void spawnThroneBlock(Location loc, Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(sx, sy, sz).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
            throneBlocks.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override
        protected void onTick(int tick) {
            if (throneCenter == null) return;

            // Cold radiation: snowflake aura
            if (tick % 8 == 0) {
                snowflakes(throneCenter.clone().add(0, 1.5, 0), 12, 2.0);
                frostDust(throneCenter.clone().add(0, 0.5, 0), 6, 1.5);
            }

            // Frost ring around throne
            if (tick % 30 == 0) {
                DisplayBuilder.particleRing(throneCenter, 4.0, Particle.SNOWFLAKE, 20, null);
                DisplayBuilder.playSound(throneCenter, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.7f);
            }

            // Glow pulse
            if (tick % 20 == 0) {
                boolean bright = (tick / 20) % 2 == 0;
                for (BlockDisplayHandle h : throneBlocks) {
                    if (bright) h.glow(FROST_R, FROST_G, FROST_B);
                    else h.glow(ICE_R, ICE_G, ICE_B);
                }
            }

            // End rod sparkle on crown
            if (tick % 12 == 0) {
                endRods(throneCenter.clone().add(0, 3.5, -0.5), 3, 0.3);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceThrone(plugin); }
    }

    // =========================================================================
    // 5. FROST GATE — 16 gateway blocks (2 pillars + arch + door), cold blast cone
    // =========================================================================
    public static class FrostGate extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> gateBlocks = new ArrayList<>();
        private boolean doorOpen = false;
        private BlockDisplayHandle leftDoor;
        private BlockDisplayHandle rightDoor;

        public FrostGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_gate", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setImpactDamage(55.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Left pillar (5 blocks)
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(-2, y, 0);
                Material mat = y % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 1.0f, 0.8f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pillar (5 blocks)
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(2, y, 0);
                Material mat = y % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 1.0f, 0.8f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Arch (4 blocks across top)
            for (int x = -1; x <= 1; x++) {
                Location loc = center.clone().add(x, 5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(1.0f, 0.8f, 0.8f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Arch keystone
            BlockDisplayHandle keystone = displayBuilder.spawnBlock(center.clone().add(0, 5.6, 0), Material.LIGHT_BLUE_STAINED_GLASS);
            keystone.scale(0.6f, 0.6f, 0.6f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
            gateBlocks.add(keystone);
            spawnedEntities.add(keystone.entity());

            // Door panels (2 blocks — will swing open)
            leftDoor = displayBuilder.spawnBlock(center.clone().add(-0.8, 0, 0), Material.LIGHT_BLUE_STAINED_GLASS);
            leftDoor.scale(0.8f, 4.5f, 0.15f).glow(ICE_R, ICE_G, ICE_B).interpolation(10, 0);
            gateBlocks.add(leftDoor);
            spawnedEntities.add(leftDoor.entity());

            rightDoor = displayBuilder.spawnBlock(center.clone().add(0.8, 0, 0), Material.LIGHT_BLUE_STAINED_GLASS);
            rightDoor.scale(0.8f, 4.5f, 0.15f).glow(ICE_R, ICE_G, ICE_B).interpolation(10, 0);
            gateBlocks.add(rightDoor);
            spawnedEntities.add(rightDoor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.7f);
            iceDust(center, 40, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Door opens at tick 60, firing cold blast
            if (tick == 60 && !doorOpen) {
                doorOpen = true;
                // Swing doors open (teleport to sides)
                leftDoor.entity().teleport(c.clone().add(-2.2, 0, 0.8));
                rightDoor.entity().teleport(c.clone().add(2.2, 0, 0.8));
                leftDoor.interpolation(8, 0);
                rightDoor.interpolation(8, 0);

                // Cold blast cone: particles + damage
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.5f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                for (int i = 1; i <= 8; i++) {
                    Location blastLoc = c.clone().add(0, 1.5, i);
                    snowflakes(blastLoc, 15, 0.5 + i * 0.3);
                    iceDust(blastLoc, 10, 0.3 + i * 0.2);
                }
                triggerImpactDamage(c.clone().add(0, 1, 4));
            }

            // Repeat blast pulses every 50 ticks after opening
            if (doorOpen && tick > 60 && (tick - 60) % 50 == 0) {
                for (int i = 1; i <= 8; i++) {
                    Location blastLoc = c.clone().add(0, 1.5, i);
                    snowflakes(blastLoc, 12, 0.4 + i * 0.25);
                    frostDust(blastLoc, 8, 0.3 + i * 0.15);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.6f);
                triggerImpactDamage(c.clone().add(0, 1, 4));
            }

            // Ambient gate particles
            if (tick % 10 == 0) {
                endRods(c.clone().add(0, 5.6, 0), 3, 0.3);
                snowflakes(c.clone().add(0, 2.5, 0), 6, 1.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostGate(plugin); }
    }

    // =========================================================================
    // 6. GLACIAL ALTAR — 12 altar blocks rising, rune particles, damage pulse
    // =========================================================================
    public static class GlacialAltar extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> altarBlocks = new ArrayList<>();
        private boolean risen = false;

        public GlacialAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_altar", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Altar base — 4 corner posts at ground level (start underground, will rise)
            double[][] corners = {{-1.5, -2, -1.5}, {1.5, -2, -1.5}, {-1.5, -2, 1.5}, {1.5, -2, 1.5}};
            for (double[] off : corners) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.6f, 1.5f, 0.6f).glow(ICE_R, ICE_G, ICE_B).interpolation(20, 0);
                altarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Platform top (4 blocks)
            double[][] platform = {{-0.5, -2, -0.5}, {0.5, -2, -0.5}, {-0.5, -2, 0.5}, {0.5, -2, 0.5}};
            for (double[] off : platform) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.0f, 0.3f, 1.0f).glow(ICE_R, ICE_G, ICE_B).interpolation(20, 0);
                altarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Central crystal (prismarine) and offerings (snow blocks)
            Location crystalLoc = center.clone().add(0, -2, 0);
            BlockDisplayHandle crystal = displayBuilder.spawnBlock(crystalLoc, Material.PRISMARINE);
            crystal.scale(0.5f, 0.8f, 0.5f).glow(0, 220, 240).interpolation(20, 0);
            altarBlocks.add(crystal);
            spawnedEntities.add(crystal.entity());

            // Offering bowls on sides
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3;
                Location bowlLoc = center.clone().add(Math.cos(angle) * 2.2, -2, Math.sin(angle) * 2.2);
                BlockDisplayHandle bowl = displayBuilder.spawnBlock(bowlLoc, Material.SNOW_BLOCK);
                bowl.scale(0.4f, 0.3f, 0.4f).glow(FROST_R, FROST_G, FROST_B).interpolation(20, 0);
                altarBlocks.add(bowl);
                spawnedEntities.add(bowl.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Rise from ground over first 40 ticks
            if (tick <= 40) {
                float rise = (tick / 40.0f) * 2.0f; // rise 2 blocks
                for (BlockDisplayHandle h : altarBlocks) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    loc.setY(loc.getY() + (2.0 / 40.0));
                    bd.teleport(loc);
                }
                if (tick == 40) {
                    risen = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
                    iceDust(c, 50, 3.0);
                }
            }

            // Rune pattern particles: rotating symbols
            if (risen && tick % 4 == 0) {
                double runeAngle = tick * 0.08;
                for (int i = 0; i < 6; i++) {
                    double a = runeAngle + (Math.PI * 2 * i / 6);
                    double rx = Math.cos(a) * 2.5;
                    double rz = Math.sin(a) * 2.5;
                    Location runeLoc = c.clone().add(rx, 0.3, rz);
                    endRods(runeLoc, 2, 0.1);
                }
            }

            // Damage pulse every 35 ticks
            if (risen && tick % 35 == 0) {
                DisplayBuilder.particleRing(c, 5.0, Particle.SNOWFLAKE, 24, null);
                iceDust(c, 20, 4.0);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.9f);
            }

            // Crystal glow pulse
            if (risen && tick % 20 == 0 && altarBlocks.size() > 8) {
                boolean bright = (tick / 20) % 2 == 0;
                altarBlocks.get(8).glow(bright ? 0 : ICE_R, bright ? 255 : ICE_G, bright ? 255 : ICE_B);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacialAltar(plugin); }
    }

    // =========================================================================
    // 7. ICE OBELISK — 10 tall narrow obelisk blocks, orbiting runes, frost ring
    // =========================================================================
    public static class IceObelisk extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> obeliskBlocks = new ArrayList<>();

        public IceObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_obelisk", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // 10-block obelisk: tapers as it goes up
            Material[] mats = {Material.BLUE_ICE, Material.BLUE_ICE, Material.PACKED_ICE, Material.PACKED_ICE,
                Material.BLUE_ICE, Material.PACKED_ICE, Material.PRISMARINE, Material.PRISMARINE,
                Material.LIGHT_BLUE_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS};
            for (int y = 0; y < 10; y++) {
                float taper = 1.0f - (y * 0.06f); // 1.0 at base -> 0.46 at top
                Location loc = center.clone().add(0, y * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[y]);
                h.scale(taper * 0.7f, 0.85f, taper * 0.7f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                obeliskBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.9f);
            iceDust(center, 35, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Orbiting rune particles around the obelisk
            if (tick % 2 == 0) {
                double orbitAngle = tick * 0.1;
                for (int i = 0; i < 3; i++) {
                    double a = orbitAngle + (Math.PI * 2 * i / 3);
                    double height = 1.0 + (tick % 60) / 60.0 * 7.0; // rises and loops
                    double radius = 1.5 + Math.sin(tick * 0.05 + i) * 0.5;
                    Location runeLoc = c.clone().add(Math.cos(a) * radius, height, Math.sin(a) * radius);
                    endRods(runeLoc, 2, 0.05);
                    frostDust(runeLoc, 1, 0.1);
                }
            }

            // Frost ring pulse expanding outward every 30 ticks
            if (tick % 30 == 0) {
                DisplayBuilder.particleRing(c, 5.0, Particle.SNOWFLAKE, 24, null);
                DisplayBuilder.dustParticles(c, 15, 4.0, ICE_R, ICE_G, ICE_B, 1.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 1.0f);
            }

            // Obelisk tip sparkle
            if (tick % 6 == 0) {
                endRods(c.clone().add(0, 9, 0), 3, 0.4);
            }

            // Ambient snowfall around base
            if (tick % 15 == 0) {
                snowflakes(c.clone().add(0, 5, 0), 10, 2.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceObelisk(plugin); }
    }

    // =========================================================================
    // 8. FROZEN FOUNTAIN — 14 blocks with frozen water arcs mid-splash, frost aura
    // =========================================================================
    public static class FrozenFountain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> fountainBlocks = new ArrayList<>();

        public FrozenFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Basin ring (6 blocks)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.8f, 0.5f, 0.8f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                fountainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central pillar (2 blocks)
            BlockDisplayHandle base = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0), Material.PACKED_ICE);
            base.scale(0.6f, 1.2f, 0.6f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
            fountainBlocks.add(base);
            spawnedEntities.add(base.entity());

            BlockDisplayHandle top = displayBuilder.spawnBlock(center.clone().add(0, 1.5, 0), Material.PRISMARINE);
            top.scale(0.4f, 0.4f, 0.4f).glow(0, 220, 240).interpolation(5, 0);
            fountainBlocks.add(top);
            spawnedEntities.add(top.entity());

            // Frozen water arcs: 4 curved splash arcs frozen mid-air
            double[][] arcPositions = {{1.0, 2.0, 0}, {-1.0, 2.0, 0}, {0, 2.0, 1.0}, {0, 2.0, -1.0}};
            for (double[] pos : arcPositions) {
                Location arcLoc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle arc = displayBuilder.spawnBlock(arcLoc, Material.LIGHT_BLUE_STAINED_GLASS);
                arc.scale(0.3f, 0.6f, 0.3f)
                   .rotate((float)(Math.PI / 6), pos[0] > 0 ? -1 : (pos[0] < 0 ? 1 : 0), 0, pos[2] > 0 ? -1 : (pos[2] < 0 ? 1 : 0))
                   .glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
                fountainBlocks.add(arc);
                spawnedEntities.add(arc.entity());
            }

            // Splash droplets frozen in air (2 blocks)
            BlockDisplayHandle drop1 = displayBuilder.spawnBlock(center.clone().add(0.8, 2.5, 0.8), Material.ICE);
            drop1.scale(0.15f, 0.15f, 0.15f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
            fountainBlocks.add(drop1);
            spawnedEntities.add(drop1.entity());

            BlockDisplayHandle drop2 = displayBuilder.spawnBlock(center.clone().add(-0.7, 2.3, -0.6), Material.ICE);
            drop2.scale(0.15f, 0.15f, 0.15f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
            fountainBlocks.add(drop2);
            spawnedEntities.add(drop2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.3f);
            iceDust(center, 30, 2.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Frost aura: constant snowflake ring
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(c, 3.0, Particle.SNOWFLAKE, 16, null);
                snowflakes(c.clone().add(0, 2.0, 0), 8, 1.2);
            }

            // Frozen splash effect: end rods rising from center
            if (tick % 10 == 0) {
                endRods(c.clone().add(0, 2.0, 0), 5, 0.8);
                frostDust(c.clone().add(0, 1.5, 0), 4, 0.6);
            }

            // Damage pulse
            if (tick % 40 == 0) {
                DisplayBuilder.particleRing(c, 5.5, Particle.SNOWFLAKE, 24, null);
                iceDust(c, 20, 5.0);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.1f);
            }

            // Glow pulse on frozen arcs (indices 8-11)
            if (tick % 16 == 0) {
                boolean bright = (tick / 16) % 2 == 0;
                for (int i = 8; i < Math.min(12, fountainBlocks.size()); i++) {
                    if (bright) fountainBlocks.get(i).glow(FROST_R, FROST_G, FROST_B);
                    else fountainBlocks.get(i).glow(ICE_R, ICE_G, ICE_B);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrozenFountain(plugin); }
    }

    // =========================================================================
    // 9. CRYSTAL CHANDELIER — 16 blocks descending from Y+12, icicle drops, area damage
    // =========================================================================
    public static class CrystalChandelier extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> chandelierBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> icicleDrops = new ArrayList<>();
        private boolean descended = false;

        public CrystalChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_chandelier", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setImpactDamage(45.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            Location high = center.clone().add(0, 12, 0);

            // Central stem (2 blocks)
            BlockDisplayHandle stem1 = displayBuilder.spawnBlock(high.clone().add(0, 0, 0), Material.QUARTZ_BLOCK);
            stem1.scale(0.3f, 1.5f, 0.3f).glow(FROST_R, FROST_G, FROST_B).interpolation(10, 0);
            chandelierBlocks.add(stem1);
            spawnedEntities.add(stem1.entity());

            BlockDisplayHandle stem2 = displayBuilder.spawnBlock(high.clone().add(0, -1.5, 0), Material.PACKED_ICE);
            stem2.scale(0.4f, 0.5f, 0.4f).glow(ICE_R, ICE_G, ICE_B).interpolation(10, 0);
            chandelierBlocks.add(stem2);
            spawnedEntities.add(stem2.entity());

            // Ring of crystal arms (8 blocks)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double r = 1.5;
                Location armLoc = high.clone().add(Math.cos(angle) * r, -2.0, Math.sin(angle) * r);
                Material mat = i % 2 == 0 ? Material.BLUE_ICE : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle arm = displayBuilder.spawnBlock(armLoc, mat);
                arm.scale(0.4f, 0.2f, 0.4f).glow(ICE_R, ICE_G, ICE_B).interpolation(10, 0);
                chandelierBlocks.add(arm);
                spawnedEntities.add(arm.entity());
            }

            // Hanging icicles (6 blocks, will drop periodically)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6 + Math.PI / 6;
                double r = 1.2;
                Location icicLoc = high.clone().add(Math.cos(angle) * r, -2.5, Math.sin(angle) * r);
                BlockDisplayHandle icicle = displayBuilder.spawnBlock(icicLoc, Material.ICE);
                icicle.scale(0.2f, 0.8f, 0.2f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
                icicleDrops.add(icicle);
                chandelierBlocks.add(icicle);
                spawnedEntities.add(icicle.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
            iceDust(high, 25, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Descend from Y+12 to Y+7 over first 40 ticks
            if (tick <= 40) {
                double descent = (5.0 / 40.0); // total 5 blocks over 40 ticks
                for (BlockDisplayHandle h : chandelierBlocks) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    loc.setY(loc.getY() - descent);
                    bd.teleport(loc);
                }
                if (tick == 40) {
                    descended = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.0f);
                }
            }

            // Drop icicles periodically after descent
            if (descended) {
                int dropTick = tick - 40;
                if (dropTick > 0 && dropTick % 25 == 0) {
                    int dropIdx = (dropTick / 25) - 1;
                    if (dropIdx >= 0 && dropIdx < icicleDrops.size()) {
                        BlockDisplay bd = icicleDrops.get(dropIdx).entity();
                        Location dropTarget = c.clone().add(bd.getLocation().getX() - c.getX(), 0, bd.getLocation().getZ() - c.getZ());
                        bd.teleport(dropTarget);
                        DisplayBuilder.playSound(dropTarget, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.3f);
                        snowflakes(dropTarget, 15, 1.0);
                        triggerImpactDamage(dropTarget);
                    }
                }
            }

            // Area damage below (cold radiation)
            if (tick % 10 == 0 && descended) {
                snowflakes(c.clone().add(0, 4, 0), 15, 3.0);
                frostDust(c.clone().add(0, 3, 0), 8, 2.5);
            }

            // Sparkle effect on chandelier
            if (tick % 8 == 0) {
                endRods(c.clone().add(0, descended ? 7 : 12 - (tick * 5.0 / 40.0), 0), 4, 1.5);
            }

            // Frost ring at ground
            if (tick % 35 == 0 && descended) {
                DisplayBuilder.particleRing(c, 6.0, Particle.SNOWFLAKE, 24, null);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalChandelier(plugin); }
    }

    // =========================================================================
    // 10. ICE STATUE — 14 humanoid blocks, T-pose then arms reach, slides at player
    // =========================================================================
    public static class IceStatue extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> statueBlocks = new ArrayList<>();
        private boolean armsReached = false;
        private boolean sliding = false;
        private Location slideTarget;
        private double slideX, slideZ;

        public IceStatue(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_statue", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Humanoid T-pose figure
            // Feet (2)
            spawnStatueBlock(center.clone().add(-0.3, 0, 0), Material.BLUE_ICE, 0.4f, 0.5f, 0.4f);
            spawnStatueBlock(center.clone().add(0.3, 0, 0), Material.BLUE_ICE, 0.4f, 0.5f, 0.4f);
            // Legs (2)
            spawnStatueBlock(center.clone().add(-0.3, 0.5, 0), Material.PACKED_ICE, 0.35f, 0.9f, 0.35f);
            spawnStatueBlock(center.clone().add(0.3, 0.5, 0), Material.PACKED_ICE, 0.35f, 0.9f, 0.35f);
            // Torso (2 blocks high)
            spawnStatueBlock(center.clone().add(0, 1.4, 0), Material.BLUE_ICE, 0.7f, 0.5f, 0.4f);
            spawnStatueBlock(center.clone().add(0, 1.9, 0), Material.BLUE_ICE, 0.7f, 0.5f, 0.4f);
            // Arms T-pose (4 blocks: upper+lower each side)
            spawnStatueBlock(center.clone().add(-0.8, 1.9, 0), Material.PACKED_ICE, 0.5f, 0.3f, 0.3f); // idx 6
            spawnStatueBlock(center.clone().add(-1.3, 1.9, 0), Material.PACKED_ICE, 0.5f, 0.25f, 0.25f); // idx 7
            spawnStatueBlock(center.clone().add(0.8, 1.9, 0), Material.PACKED_ICE, 0.5f, 0.3f, 0.3f); // idx 8
            spawnStatueBlock(center.clone().add(1.3, 1.9, 0), Material.PACKED_ICE, 0.5f, 0.25f, 0.25f); // idx 9
            // Head (1 block)
            spawnStatueBlock(center.clone().add(0, 2.4, 0), Material.PRISMARINE, 0.5f, 0.5f, 0.5f); // idx 10
            // Eyes (2 small blocks)
            spawnStatueBlock(center.clone().add(-0.12, 2.55, 0.25), Material.LIGHT_BLUE_STAINED_GLASS, 0.1f, 0.08f, 0.05f);
            spawnStatueBlock(center.clone().add(0.12, 2.55, 0.25), Material.LIGHT_BLUE_STAINED_GLASS, 0.1f, 0.08f, 0.05f);
            // Hands (frozen claw tips)
            spawnStatueBlock(center.clone().add(-1.7, 1.9, 0), Material.ICE, 0.15f, 0.15f, 0.15f); // idx 13
            spawnStatueBlock(center.clone().add(1.7, 1.9, 0), Material.ICE, 0.15f, 0.15f, 0.15f); // idx 14 (15th block for safety but 14 is the target)

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
            iceDust(center, 35, 2.0);
        }

        private void spawnStatueBlock(Location loc, Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(sx, sy, sz).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
            statueBlocks.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;
            World w = c.getWorld();
            if (w == null) return;

            // At tick 60: arms reach forward toward nearest player
            if (tick == 60 && !armsReached) {
                armsReached = true;
                // Rotate arms forward (move arm blocks to Z+ positions)
                if (statueBlocks.size() > 9) {
                    // Left arm forward
                    statueBlocks.get(6).entity().teleport(c.clone().add(-0.4, 1.9, 0.5));
                    statueBlocks.get(7).entity().teleport(c.clone().add(-0.4, 1.9, 1.0));
                    // Right arm forward
                    statueBlocks.get(8).entity().teleport(c.clone().add(0.4, 1.9, 0.5));
                    statueBlocks.get(9).entity().teleport(c.clone().add(0.4, 1.9, 1.0));
                    // Hands forward
                    if (statueBlocks.size() > 13) {
                        statueBlocks.get(13).entity().teleport(c.clone().add(-0.4, 1.9, 1.3));
                    }
                    if (statueBlocks.size() > 14) {
                        statueBlocks.get(14).entity().teleport(c.clone().add(0.4, 1.9, 1.3));
                    }
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.4f);
                frostDust(c.clone().add(0, 2, 0.5), 15, 0.5);
            }

            // At tick 100: begin sliding toward nearest player
            if (tick == 100 && !sliding) {
                Player nearest = null;
                double minDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    double d = p.getLocation().distanceSquared(c);
                    if (d < minDist && d > 4) { minDist = d; nearest = p; }
                }
                if (nearest != null) {
                    slideTarget = nearest.getLocation().clone();
                    double dx = slideTarget.getX() - c.getX();
                    double dz = slideTarget.getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    slideX = (dx / dist) * 0.15; // slide speed
                    slideZ = (dz / dist) * 0.15;
                    sliding = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.5f);
                }
            }

            // Slide movement
            if (sliding && tick > 100) {
                for (BlockDisplayHandle h : statueBlocks) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    loc.add(slideX, 0, slideZ);
                    bd.teleport(loc);
                }
                // Update center for damage radius
                center.add(slideX, 0, slideZ);

                // Trail particles
                if (tick % 4 == 0) {
                    snowflakes(center.clone().add(0, 0.5, 0), 8, 0.5);
                    iceDust(center.clone().add(0, 0, -slideZ * 5), 4, 0.3);
                }
            }

            // Ambient frost
            if (tick % 12 == 0) {
                snowflakes(center.clone().add(0, 2.5, 0), 6, 1.0);
            }

            // Eye glow pulse
            if (tick % 10 == 0 && statueBlocks.size() > 12) {
                boolean bright = (tick / 10) % 2 == 0;
                statueBlocks.get(11).glow(bright ? 255 : ICE_R, bright ? 255 : ICE_G, bright ? 255 : ICE_B);
                statueBlocks.get(12).glow(bright ? 255 : ICE_R, bright ? 255 : ICE_G, bright ? 255 : ICE_B);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceStatue(plugin); }
    }

    // =========================================================================
    // 11. FROST CATHEDRAL — 22 ribbed vault blocks rising from both sides, enclosed damage
    // =========================================================================
    public static class FrostCathedral extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> vaultBlocks = new ArrayList<>();

        public FrostCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_cathedral", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Ribbed vault: 5 ribs on each side (10 base blocks), curving inward overhead
            // Left ribs (11 blocks)
            for (int i = 0; i < 5; i++) {
                double z = -4 + i * 2;
                // Base column (left side)
                Location baseLoc = center.clone().add(-3, 0, z);
                BlockDisplayHandle baseH = displayBuilder.spawnBlock(baseLoc, Material.BLUE_ICE);
                baseH.scale(0.5f, 2.5f, 0.5f).glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                vaultBlocks.add(baseH);
                spawnedEntities.add(baseH.entity());

                // Curve inward (left)
                Location curveLoc = center.clone().add(-2, 3.0, z);
                BlockDisplayHandle curveH = displayBuilder.spawnBlock(curveLoc, Material.PACKED_ICE);
                curveH.scale(0.5f, 1.5f, 0.5f)
                      .rotate((float)(Math.PI / 4), 0, 0, 1)
                      .glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                vaultBlocks.add(curveH);
                spawnedEntities.add(curveH.entity());
            }

            // Right ribs (10 blocks)
            for (int i = 0; i < 5; i++) {
                double z = -4 + i * 2;
                // Base column (right side)
                Location baseLoc = center.clone().add(3, 0, z);
                BlockDisplayHandle baseH = displayBuilder.spawnBlock(baseLoc, Material.BLUE_ICE);
                baseH.scale(0.5f, 2.5f, 0.5f).glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                vaultBlocks.add(baseH);
                spawnedEntities.add(baseH.entity());

                // Curve inward (right)
                Location curveLoc = center.clone().add(2, 3.0, z);
                BlockDisplayHandle curveH = displayBuilder.spawnBlock(curveLoc, Material.PACKED_ICE);
                curveH.scale(0.5f, 1.5f, 0.5f)
                      .rotate((float)(-Math.PI / 4), 0, 0, 1)
                      .glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                vaultBlocks.add(curveH);
                spawnedEntities.add(curveH.entity());
            }

            // Keystone ridge along the top center (2 blocks)
            BlockDisplayHandle ridge1 = displayBuilder.spawnBlock(center.clone().add(0, 4.5, -2), Material.PRISMARINE);
            ridge1.scale(0.4f, 0.4f, 2.5f).glow(0, 220, 240).interpolation(8, 0);
            vaultBlocks.add(ridge1);
            spawnedEntities.add(ridge1.entity());

            BlockDisplayHandle ridge2 = displayBuilder.spawnBlock(center.clone().add(0, 4.5, 2), Material.PRISMARINE);
            ridge2.scale(0.4f, 0.4f, 2.5f).glow(0, 220, 240).interpolation(8, 0);
            vaultBlocks.add(ridge2);
            spawnedEntities.add(ridge2.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
            iceDust(center, 50, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Enclosed cold damage zone — snowflakes filling interior
            if (tick % 6 == 0) {
                for (int i = 0; i < 5; i++) {
                    double rx = (Math.random() - 0.5) * 5;
                    double ry = Math.random() * 4;
                    double rz = (Math.random() - 0.5) * 8;
                    snowflakes(c.clone().add(rx, ry, rz), 3, 0.3);
                }
            }

            // Frost dust along the interior floor
            if (tick % 12 == 0) {
                iceDust(c.clone().add(0, 0.2, 0), 15, 3.0);
            }

            // Cold pulse damage indicator
            if (tick % 30 == 0) {
                DisplayBuilder.particleRing(c, 3.5, Particle.SNOWFLAKE, 20, null);
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 0.6f);
                endRods(c.clone().add(0, 4.5, 0), 6, 1.0);
            }

            // Glow shift on keystones
            if (tick % 20 == 0) {
                boolean bright = (tick / 20) % 2 == 0;
                int lastIdx = vaultBlocks.size();
                if (lastIdx >= 2) {
                    vaultBlocks.get(lastIdx - 1).glow(bright ? FROST_R : 0, bright ? FROST_G : 220, bright ? FROST_B : 240);
                    vaultBlocks.get(lastIdx - 2).glow(bright ? FROST_R : 0, bright ? FROST_G : 220, bright ? FROST_B : 240);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostCathedral(plugin); }
    }

    // =========================================================================
    // 12. GLACIAL PYRAMID — 16 stepped blocks layer by layer, growing frost radius
    // =========================================================================
    public static class GlacialPyramid extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> pyramidBlocks = new ArrayList<>();
        private int builtLayers = 0;

        // Layer data: layer 0 = 9 blocks (3x3), layer 1 = 4 (2x2 offset), layer 2 = 2, layer 3 = 1 (tip)
        private final int[] layerCounts = {9, 4, 2, 1};

        public GlacialPyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_pyramid", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            frostDust(center, 30, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Build layers over time: new layer every 25 ticks
            if (tick % 25 == 0 && builtLayers < 4) {
                int layer = builtLayers;
                double y = layer * 1.2;

                if (layer == 0) {
                    // 3x3 base
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            Location loc = c.clone().add(x * 1.1, y, z * 1.1);
                            Material mat = (x + z) % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                            h.scale(1.0f, 1.0f, 1.0f).glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                            pyramidBlocks.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                } else if (layer == 1) {
                    // 2x2
                    double[][] offsets = {{-0.55, 0, -0.55}, {0.55, 0, -0.55}, {-0.55, 0, 0.55}, {0.55, 0, 0.55}};
                    for (double[] off : offsets) {
                        Location loc = c.clone().add(off[0], y, off[2]);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                        h.scale(0.9f, 1.0f, 0.9f).glow(ICE_R, ICE_G, ICE_B).interpolation(8, 0);
                        pyramidBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                } else if (layer == 2) {
                    // 2 blocks
                    Location loc1 = c.clone().add(-0.3, y, 0);
                    BlockDisplayHandle h1 = displayBuilder.spawnBlock(loc1, Material.PRISMARINE);
                    h1.scale(0.7f, 1.0f, 0.7f).glow(0, 220, 240).interpolation(8, 0);
                    pyramidBlocks.add(h1);
                    spawnedEntities.add(h1.entity());

                    Location loc2 = c.clone().add(0.3, y, 0);
                    BlockDisplayHandle h2 = displayBuilder.spawnBlock(loc2, Material.PRISMARINE);
                    h2.scale(0.7f, 1.0f, 0.7f).glow(0, 220, 240).interpolation(8, 0);
                    pyramidBlocks.add(h2);
                    spawnedEntities.add(h2.entity());
                } else {
                    // Tip
                    Location loc = c.clone().add(0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    h.scale(0.5f, 1.0f, 0.5f).glow(FROST_R, FROST_G, FROST_B).interpolation(8, 0);
                    pyramidBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }

                builtLayers++;
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.8f + layer * 0.2f);
                snowflakes(c.clone().add(0, y, 0), 15, 1.5);
            }

            // Growing frost radius: larger as more layers built
            double frostRadius = 2.0 + builtLayers * 1.5;
            if (tick % 20 == 0 && builtLayers > 0) {
                DisplayBuilder.particleRing(c, frostRadius, Particle.SNOWFLAKE, (int)(frostRadius * 4), null);
                iceDust(c, 10, frostRadius);
            }

            // Ambient particles
            if (tick % 8 == 0) {
                snowflakes(c.clone().add(0, builtLayers * 1.2, 0), 8, 1.0);
            }

            // Damage pulse
            if (tick % 35 == 0 && builtLayers >= 2) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 0.8f);
            }

            // Tip glow pulse when complete
            if (builtLayers == 4 && tick % 15 == 0) {
                endRods(c.clone().add(0, 4.0, 0), 4, 0.4);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacialPyramid(plugin); }
    }

    // =========================================================================
    // 13. ICE MIRROR HALL — 16 blocks: 2 glass walls with ice frames closing inward
    // =========================================================================
    public static class IceMirrorHall extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> leftWall = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWall = new ArrayList<>();
        private double wallOffset = 6.0; // starts 6 blocks apart, closes to 1.5

        public IceMirrorHall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror_hall", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Left wall: 4 glass panels + 4 ice frames = 8 blocks
            for (int i = 0; i < 4; i++) {
                double z = -3 + i * 2;
                // Ice frame post
                Location frameLoc = center.clone().add(-wallOffset, 0, z);
                BlockDisplayHandle frame = displayBuilder.spawnBlock(frameLoc, Material.BLUE_ICE);
                frame.scale(0.3f, 3.0f, 0.3f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                leftWall.add(frame);
                spawnedEntities.add(frame.entity());

                // Glass panel between frames
                Location glassLoc = center.clone().add(-wallOffset, 0, z + 1);
                BlockDisplayHandle glass = displayBuilder.spawnBlock(glassLoc, Material.LIGHT_BLUE_STAINED_GLASS);
                glass.scale(0.15f, 2.8f, 1.5f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
                leftWall.add(glass);
                spawnedEntities.add(glass.entity());
            }

            // Right wall: mirror of left = 8 blocks
            for (int i = 0; i < 4; i++) {
                double z = -3 + i * 2;
                Location frameLoc = center.clone().add(wallOffset, 0, z);
                BlockDisplayHandle frame = displayBuilder.spawnBlock(frameLoc, Material.BLUE_ICE);
                frame.scale(0.3f, 3.0f, 0.3f).glow(ICE_R, ICE_G, ICE_B).interpolation(5, 0);
                rightWall.add(frame);
                spawnedEntities.add(frame.entity());

                Location glassLoc = center.clone().add(wallOffset, 0, z + 1);
                BlockDisplayHandle glass = displayBuilder.spawnBlock(glassLoc, Material.LIGHT_BLUE_STAINED_GLASS);
                glass.scale(0.15f, 2.8f, 1.5f).glow(FROST_R, FROST_G, FROST_B).interpolation(5, 0);
                rightWall.add(glass);
                spawnedEntities.add(glass.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.0f);
            iceDust(center, 40, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null) return;

            // Walls close inward: 6.0 -> 1.5 over 200 ticks
            if (wallOffset > 1.5 && tick < 200) {
                wallOffset -= 0.0225; // (6.0 - 1.5) / 200 = 0.0225 per tick
                if (wallOffset < 1.5) wallOffset = 1.5;

                // Teleport left wall blocks
                for (int i = 0; i < leftWall.size(); i++) {
                    BlockDisplay bd = leftWall.get(i).entity();
                    Location loc = bd.getLocation();
                    double targetX = c.getX() - wallOffset;
                    loc.setX(targetX);
                    bd.teleport(loc);
                }

                // Teleport right wall blocks
                for (int i = 0; i < rightWall.size(); i++) {
                    BlockDisplay bd = rightWall.get(i).entity();
                    Location loc = bd.getLocation();
                    double targetX = c.getX() + wallOffset;
                    loc.setX(targetX);
                    bd.teleport(loc);
                }
            }

            // Reflective particle effects between walls
            if (tick % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    double rz = (Math.random() - 0.5) * 6;
                    double ry = Math.random() * 2.5;
                    // Bouncing end rod effect between walls
                    endRods(c.clone().add(-wallOffset + 0.5, ry, rz), 2, 0.2);
                    endRods(c.clone().add(wallOffset - 0.5, ry, rz), 2, 0.2);
                }
            }

            // Snowflake corridor effect
            if (tick % 6 == 0) {
                snowflakes(c.clone().add(0, 1.5, 0), 10, wallOffset * 0.5);
            }

            // Frost crunch sound as walls get close
            if (wallOffset < 3.0 && tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f + (float)(3.0 - wallOffset) * 0.3f, 0.7f);
                iceDust(c, 12, wallOffset);
            }

            // Close-in panic: stronger effects as walls near
            if (wallOffset < 2.5 && tick % 10 == 0) {
                DisplayBuilder.particleRing(c, wallOffset + 0.5, Particle.SNOWFLAKE, 16, null);
            }

            // Glow pulse on glass panels
            if (tick % 14 == 0) {
                boolean bright = (tick / 14) % 2 == 0;
                for (int i = 1; i < leftWall.size(); i += 2) {
                    leftWall.get(i).glow(bright ? FROST_R : ICE_R, bright ? FROST_G : ICE_G, bright ? FROST_B : ICE_B);
                }
                for (int i = 1; i < rightWall.size(); i += 2) {
                    rightWall.get(i).glow(bright ? FROST_R : ICE_R, bright ? FROST_G : ICE_G, bright ? FROST_B : ICE_B);
                }
            }

            // Wall slam sound when fully closed
            if (Math.abs(wallOffset - 1.5) < 0.03 && tick > 50) {
                if (tick == 200) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
                    iceDust(c, 40, 2.0);
                    snowflakes(c, 30, 1.5);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceMirrorHall(plugin); }
    }
}
