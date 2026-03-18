package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Block Display -- GROUP 10: FINAL SEQUENCE ARCHITECTURE (Structures #93-100)
 * Supreme Calamitas (Boss 5) arena: Calamitas Throne (pre-state, rise, collapse),
 * Requiem Pillars (NW/NE/SW/SE), Convergence State, Maximum Emission State,
 * Architect's Seal (post-death remnant).
 *
 * Palette: obsidian/crying obsidian dark, black concrete funereal,
 *          soul fire blue, crimson red, dragon breath purple.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * triggerImpactDamage() for proximity hits. spawnedEntities.add() always.
 */
public final class FinalSequenceArch {

    private FinalSequenceArch() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CalamitasThronePreState(plugin));
        registry.register(new CalamitasThroneRise(plugin));
        registry.register(new ThroneCollapse(plugin));
        registry.register(new RequiemPillarsNorthPair(plugin));
        registry.register(new RequiemPillarsSouthPair(plugin));
        registry.register(new LastSkyConvergence(plugin));
        registry.register(new LastSkyMaxEmission(plugin));
        registry.register(new ArchitectsSeal(plugin));
    }

    // ================================================================
    // #93 -- CALAMITAS THRONE: UNDERGROUND PRE-STATE
    // ~47 entities: OBSIDIAN + CRYING_OBSIDIAN throne assembled 8 blocks
    // below arena floor. Seat (8 entities), back pillars (14), back-rest
    // infill (16 CRYING_OBSIDIAN), crown (5), armrests (4). Hidden until
    // 10% HP trigger. No particles or sounds in pre-state.
    // ================================================================
    public static class CalamitasThronePreState extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();

        public CalamitasThronePreState(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_throne_pre_state", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Throne hidden at Y_floor - 8
            Location throneBase = center.clone().add(0, -8, 0);

            // Seat section: 2x2 base at Y+0 (4 front legs) + 2x2 top at Y+1 (seat surface)
            for (int y = 0; y <= 1; y++) {
                for (int x = 0; x <= 1; x++) {
                    for (int z = 0; z <= 1; z++) {
                        Location loc = throneBase.clone().add(x - 0.5, y, z - 0.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(1.0f, 1.0f, 1.0f).interpolation(0, 0);
                        throneBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Back pillars: left (X-1) and right (X+1), 7 blocks tall each (Y+0 to Y+6)
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 7; y++) {
                    Location loc = throneBase.clone().add(side, y, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.8f, 1.0f, 0.8f).interpolation(0, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Back-rest infill: 2-wide x 4-tall CRYING_OBSIDIAN (Y+3 to Y+6, thin panels)
            for (int y = 3; y <= 6; y++) {
                for (int x = 0; x < 4; x++) {
                    Location loc = throneBase.clone().add(-0.4 + x * 0.4, y, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 1.0f, 0.2f).interpolation(0, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crown cap at Y+7: center + 4 flanking pieces
            BlockDisplayHandle crownCenter = displayBuilder.spawnBlock(
                    throneBase.clone().add(0, 7, -0.5), Material.CRYING_OBSIDIAN);
            crownCenter.scale(1.0f, 1.2f, 0.8f).interpolation(0, 0);
            throneBlocks.add(crownCenter);
            spawnedEntities.add(crownCenter.entity());

            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location loc = throneBase.clone().add(side * (0.5 + j * 0.3), 7, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.6f, 1.0f, 0.6f).interpolation(0, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Armrests at Y+2
            for (int side = -1; side <= 1; side += 2) {
                Location loc = throneBase.clone().add(side, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.2f, 0.3f, 0.6f).interpolation(0, 0);
                throneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Pre-state: no animation, no particles, no sound
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CalamitasThronePreState(plugin); }
    }

    // ================================================================
    // #94 -- CALAMITAS THRONE: RISE AND REVEAL
    // Triggers at 10% HP. All throne entities rise from Y_floor-8 to
    // Y_floor+0 over 80 ticks (+0.1 blocks/tick). FALLING_DUST burst
    // on crossing floor plane. Post-reveal: SOUL_FIRE_FLAME from
    // CRYING_OBSIDIAN infill, DRAGON_BREATH from crown cap. Throne
    // gains full brightness. Calamitas descends to stand before throne.
    // ================================================================
    public static class CalamitasThroneRise extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();

        public CalamitasThroneRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_throne_rise", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(14400); // One-shot
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rebuild throne at hidden position (will rise in onTick)
            Location throneBase = center.clone().add(0, -8, 0);

            // Simplified throne: seat + pillars + infill + crown + armrests
            // Seat: 4 blocks
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    Location loc = throneBase.clone().add(x - 0.5, 1, z - 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(1.0f, 1.0f, 1.0f).glow(60, 0, 80).interpolation(5, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Back pillars: 2 x 7
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 7; y++) {
                    Location loc = throneBase.clone().add(side, y, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.8f, 1.0f, 0.8f).glow(60, 0, 80).interpolation(5, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Back-rest infill: CRYING_OBSIDIAN
            for (int y = 3; y <= 6; y++) {
                for (int x = 0; x < 4; x++) {
                    Location loc = throneBase.clone().add(-0.4 + x * 0.4, y, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 1.0f, 0.2f).glow(180, 40, 255).interpolation(5, 0);
                    throneBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crown
            BlockDisplayHandle crown = displayBuilder.spawnBlock(
                    throneBase.clone().add(0, 7, -0.5), Material.CRYING_OBSIDIAN);
            crown.scale(1.0f, 1.2f, 0.8f).glow(180, 40, 255).interpolation(5, 0);
            throneBlocks.add(crown);
            spawnedEntities.add(crown.entity());

            // Armrests
            for (int side = -1; side <= 1; side += 2) {
                Location loc = throneBase.clone().add(side, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.2f, 0.3f, 0.6f).glow(60, 0, 80).interpolation(5, 0);
                throneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise: +0.1 blocks/tick over 80 ticks
            if (ticksAlive <= 80) {
                double riseOffset = ticksAlive * 0.1;
                for (BlockDisplayHandle block : throneBlocks) {
                    Location currentLoc = block.entity().getLocation();
                    block.entity().teleport(currentLoc.clone().add(0, 0.1, 0));
                }

                // FALLING_DUST burst as entities cross floor plane
                if (ticksAlive == 40) { // Approximately when crossing floor
                    w.spawnParticle(Particle.FALLING_DUST, center, 20,
                            1.5, 0.5, 1.5, 0,
                            Material.OBSIDIAN.createBlockData());
                }
            }

            // Post-reveal particles
            if (ticksAlive > 80) {
                // SOUL_FIRE_FLAME from CRYING_OBSIDIAN back-rest
                Location backCenter = center.clone().add(0, 4, -0.5);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, backCenter, 2,
                        0.4, 1.0, 0.1, 0.01);

                // DRAGON_BREATH from crown cap
                Location crownLoc = center.clone().add(0, 7, -0.5);
                w.spawnParticle(Particle.DRAGON_BREATH, crownLoc, 2,
                        0.3, 0.3, 0.3, 0.02);
            }

            // Rise completion sound
            if (ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.6f);
            }

            // Continuous ambient post-reveal
            if (ticksAlive > 80 && ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CalamitasThroneRise(plugin); }
    }

    // ================================================================
    // #95 -- THRONE COLLAPSE (DEATH ANIMATION COMPONENT)
    // Death sequence: all throne entities converge toward Calamitas over
    // ticks 0-20 (exponential ease-in). Ticks 20-60: scale shrinks at
    // 5% then 10% per tick. Tick 60: entities removed, massive particle
    // burst (30 SOUL_FIRE_FLAME + 20 DRAGON_BREATH + 15 CRIT).
    // ================================================================
    public static class ThroneCollapse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> collapseBlocks = new ArrayList<>();

        public ThroneCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("throne_collapse", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(14400); // One-shot death sequence
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn representative throne structure for collapse animation
            // Simplified: seat + pillars + crown
            for (int y = 0; y < 8; y++) {
                for (int x = -1; x <= 1; x++) {
                    Location loc = center.clone().add(x, y, -0.5);
                    Material mat = (y >= 3 && y <= 6) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.8f, 1.0f, 0.8f).glow(180, 40, 255).interpolation(3, 0);
                    collapseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Convergence target: 1 block in front of throne
            Location target = center.clone().add(0, 1, 1);

            if (ticksAlive < 20) {
                // Move toward target: 5% of remaining distance per tick
                for (BlockDisplayHandle block : collapseBlocks) {
                    Location blockLoc = block.entity().getLocation();
                    double dx = target.getX() - blockLoc.getX();
                    double dy = target.getY() - blockLoc.getY();
                    double dz = target.getZ() - blockLoc.getZ();
                    Location newLoc = blockLoc.clone().add(dx * 0.05, dy * 0.05, dz * 0.05);
                    block.entity().teleport(newLoc);
                }
            } else if (ticksAlive < 40) {
                // Scale reduction: 5% per tick
                float shrinkFactor = (float) Math.pow(0.95, ticksAlive - 20);
                for (BlockDisplayHandle block : collapseBlocks) {
                    block.scale(0.8f * shrinkFactor, 1.0f * shrinkFactor, 0.8f * shrinkFactor);
                    block.interpolation(3, 0);
                }
            } else if (ticksAlive < 60) {
                // Rapid scale reduction: 10% per tick
                float shrinkFactor = (float) (Math.pow(0.95, 20) * Math.pow(0.90, ticksAlive - 40));
                for (BlockDisplayHandle block : collapseBlocks) {
                    float s = Math.max(0.01f, 0.8f * shrinkFactor);
                    block.scale(s, s, s);
                    block.interpolation(3, 0);
                }
            } else if (ticksAlive == 60) {
                // Removal burst
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, target, 30, 2.0, 2.0, 2.0, 0.1);
                w.spawnParticle(Particle.DRAGON_BREATH, target, 20, 2.0, 2.0, 2.0, 0.1);
                w.spawnParticle(Particle.CRIT, target, 15, 2.0, 2.0, 2.0, 0.1);

                DisplayBuilder.playSound(target, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThroneCollapse(plugin); }
    }

    // ================================================================
    // #96 -- REQUIEM PILLARS: NORTHWEST AND NORTHEAST
    // 2 solid BLACK_CONCRETE pillars (2x2x12 = 48 entities each, 96
    // total). NW at (X-22, Z-22), NE at (X+22, Z-22). Rise from below
    // floor over 100 ticks (0.12 blocks/tick), starting 20 ticks after
    // 10% HP trigger. END_ROD + SOUL_FIRE_FLAME streams from tops.
    // FALLING_DUST on crossing floor plane.
    // ================================================================
    public static class RequiemPillarsNorthPair extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> nwPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> nePillar = new ArrayList<>();

        public RequiemPillarsNorthPair(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("requiem_pillars_north_pair", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(14400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // NW pillar at X-22, Z-22 -- start 12 blocks below floor
            Location nwBase = center.clone().add(-22, -12, -22);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        Location loc = nwBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(20, 0, 30).interpolation(3, 0);
                        nwPillar.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // NE pillar at X+22, Z-22
            Location neBase = center.clone().add(22, -12, -22);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        Location loc = neBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(20, 0, 30).interpolation(3, 0);
                        nePillar.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center.clone().add(-22, 0, -22),
                    Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center.clone().add(22, 0, -22),
                    Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise: 0.12 blocks/tick over 100 ticks
            if (ticksAlive <= 100) {
                for (BlockDisplayHandle block : nwPillar) {
                    Location loc = block.entity().getLocation();
                    block.entity().teleport(loc.clone().add(0, 0.12, 0));
                }
                for (BlockDisplayHandle block : nePillar) {
                    Location loc = block.entity().getLocation();
                    block.entity().teleport(loc.clone().add(0, 0.12, 0));
                }

                // FALLING_DUST when crossing floor
                if (ticksAlive == 50) {
                    w.spawnParticle(Particle.FALLING_DUST,
                            center.clone().add(-22, 0, -22), 10, 1.0, 0.5, 1.0, 0,
                            Material.BLACK_CONCRETE.createBlockData());
                    w.spawnParticle(Particle.FALLING_DUST,
                            center.clone().add(22, 0, -22), 10, 1.0, 0.5, 1.0, 0,
                            Material.BLACK_CONCRETE.createBlockData());
                }
            }

            // Post-rise: END_ROD + SOUL_FIRE_FLAME from tops
            if (ticksAlive > 100) {
                Location nwTop = center.clone().add(-22, 12, -22);
                Location neTop = center.clone().add(22, 12, -22);

                w.spawnParticle(Particle.END_ROD, nwTop, 2, 0.3, 0.1, 0.3, 0.12);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, nwTop, 1, 0.2, 0.1, 0.2, 0.03);
                w.spawnParticle(Particle.END_ROD, neTop, 2, 0.3, 0.1, 0.3, 0.12);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, neTop, 1, 0.2, 0.1, 0.2, 0.03);
            }

            // Rise complete sound
            if (ticksAlive == 100) {
                DisplayBuilder.playSound(center.clone().add(-22, 6, -22),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
                DisplayBuilder.playSound(center.clone().add(22, 6, -22),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RequiemPillarsNorthPair(plugin); }
    }

    // ================================================================
    // #97 -- REQUIEM PILLARS: SOUTHWEST AND SOUTHEAST
    // Same construction as #96. SW at (X-22, Z+22), SE at (X+22, Z+22).
    // Rises 10 ticks after NW/NE pillars (30 ticks total after 10% HP
    // trigger). NW+NE appear first, then SW+SE, giving a sequential
    // "lockdown" feel. END_ROD emissions interact visually with tether
    // lines (#91).
    // ================================================================
    public static class RequiemPillarsSouthPair extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> sePillar = new ArrayList<>();

        public RequiemPillarsSouthPair(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("requiem_pillars_south_pair", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(14400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // SW pillar at X-22, Z+22
            Location swBase = center.clone().add(-22, -12, 22);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        Location loc = swBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(20, 0, 30).interpolation(3, 0);
                        swPillar.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // SE pillar at X+22, Z+22
            Location seBase = center.clone().add(22, -12, 22);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        Location loc = seBase.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(20, 0, 30).interpolation(3, 0);
                        sePillar.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center.clone().add(-22, 0, 22),
                    Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center.clone().add(22, 0, 22),
                    Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 100) {
                for (BlockDisplayHandle block : swPillar) {
                    Location loc = block.entity().getLocation();
                    block.entity().teleport(loc.clone().add(0, 0.12, 0));
                }
                for (BlockDisplayHandle block : sePillar) {
                    Location loc = block.entity().getLocation();
                    block.entity().teleport(loc.clone().add(0, 0.12, 0));
                }

                if (ticksAlive == 50) {
                    w.spawnParticle(Particle.FALLING_DUST,
                            center.clone().add(-22, 0, 22), 10, 1.0, 0.5, 1.0, 0,
                            Material.BLACK_CONCRETE.createBlockData());
                    w.spawnParticle(Particle.FALLING_DUST,
                            center.clone().add(22, 0, 22), 10, 1.0, 0.5, 1.0, 0,
                            Material.BLACK_CONCRETE.createBlockData());
                }
            }

            if (ticksAlive > 100) {
                Location swTop = center.clone().add(-22, 12, 22);
                Location seTop = center.clone().add(22, 12, 22);

                w.spawnParticle(Particle.END_ROD, swTop, 2, 0.3, 0.1, 0.3, 0.12);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, swTop, 1, 0.2, 0.1, 0.2, 0.03);
                w.spawnParticle(Particle.END_ROD, seTop, 2, 0.3, 0.1, 0.3, 0.12);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, seTop, 1, 0.2, 0.1, 0.2, 0.03);
            }

            if (ticksAlive == 100) {
                DisplayBuilder.playSound(center.clone().add(-22, 6, 22),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
                DisplayBuilder.playSound(center.clone().add(22, 6, 22),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RequiemPillarsSouthPair(plugin); }
    }

    // ================================================================
    // #98 -- THE LAST SKY: CONVERGENCE STATE (PREPARATION)
    // At 1% HP: global modifier for ALL active structures. Directional
    // structures face center over 60 ticks. All particle emissions x2.
    // All pulse periods halved. 5 simultaneous sounds. Runs for 100
    // ticks. No new entities -- behavioral state change.
    // ================================================================
    public static class LastSkyConvergence extends BlockDisplayAttack {

        public LastSkyConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_sky_convergence", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(14400); // One-shot
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 simultaneous sounds at arena center
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Intense particle emission representing the convergence state
            // All existing structure emissions are doubled by the fight system;
            // this structure adds supplemental convergence particles

            // Radial inward particle streams (8 directions)
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double dist = 20.0 - (ticksAlive * 0.15); // Particles contract inward
                if (dist < 3) dist = 3;
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;
                Location streamLoc = center.clone().add(x, 5, z);
                w.spawnParticle(Particle.CRIT, streamLoc, 3, 0.5, 0.5, 0.5, 0.05);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, streamLoc, 2, 0.3, 0.3, 0.3, 0.02);
            }

            // Central intensifying glow
            w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                    5, 1.0, 1.0, 1.0, 0.02);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 3, 0),
                    3, 1.5, 1.5, 1.5, 0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LastSkyConvergence(plugin); }
    }

    // ================================================================
    // #99 -- THE LAST SKY: MAXIMUM EMISSION STATE
    // Global particle enhancement during 1% HP convergence and death
    // sequence. Arena-wide fills: 10 DRAGON_BREATH random, 5 CRIT rain
    // from Y+30, 8 TOTEM floor glow from Y+0.5. Near-maximum particle
    // density. Sustained until boss removal.
    // ================================================================
    public static class LastSkyMaxEmission extends BlockDisplayAttack {

        public LastSkyMaxEmission(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_sky_max_emission", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(14400); // One-shot
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle system -- no entities
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Global arena fill: 10 DRAGON_BREATH at random positions
            for (int i = 0; i < 10; i++) {
                double rx = (Math.random() - 0.5) * 56; // +-28
                double ry = Math.random() * 25;
                double rz = (Math.random() - 0.5) * 56;
                Location fillLoc = center.clone().add(rx, ry, rz);
                w.spawnParticle(Particle.DRAGON_BREATH, fillLoc, 1, 0, 0, 0, 0);
            }

            // CRIT rain from canopy level (Y+30)
            for (int i = 0; i < 5; i++) {
                double rx = (Math.random() - 0.5) * 56;
                double rz = (Math.random() - 0.5) * 56;
                Location rainLoc = center.clone().add(rx, 30, rz);
                w.spawnParticle(Particle.CRIT, rainLoc, 1, 0.2, 0.1, 0.2, 0.05);
            }

            // TOTEM floor glow
            for (int i = 0; i < 8; i++) {
                double rx = (Math.random() - 0.5) * 40; // +-20
                double rz = (Math.random() - 0.5) * 40;
                Location floorLoc = center.clone().add(rx, 0.5, rz);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, floorLoc, 1, 0.1, 0.1, 0.1, 0.04);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LastSkyMaxEmission(plugin); }
    }

    // ================================================================
    // #100 -- ARCHITECT'S SEAL (POST-DEATH ARENA MEMORY)
    // Single CRYING_OBSIDIAN BlockDisplay (3.0x0.2x3.0) at arena center
    // Y+0.5. Materializes at death sequence tick 80 (scale 0.0 to full
    // over 20 ticks). SOUL_FIRE_FLAME + DRAGON_BREATH + crimson DUST
    // during death sequence. Post-death: reduced SOUL_FIRE_FLAME ambient
    // and slow scale pulse (period 200 ticks). BLOCK_LIGHT 15. Persists
    // after boss removal as permanent arena scar.
    // ================================================================
    public static class ArchitectsSeal extends BlockDisplayAttack {

        private BlockDisplayHandle seal;

        public ArchitectsSeal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("architects_seal", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000); // Long-lived, persists
            config.setCooldownTicks(14400); // One-shot
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Materializes at center floor
            Location sealLoc = center.clone().add(0, 0.5, 0);
            seal = displayBuilder.spawnBlock(sealLoc, Material.CRYING_OBSIDIAN);
            seal.scale(0.01f, 0.01f, 0.01f).glow(180, 40, 255).interpolation(20, 0);
            spawnedEntities.add(seal.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location sealLoc = center.clone().add(0, 0.5, 0);

            // Materialization: scale 0 to 3.0x0.2x3.0 over 20 ticks
            if (ticksAlive < 20) {
                float progress = ticksAlive / 20.0f;
                seal.scale(3.0f * progress, 0.2f * progress, 3.0f * progress);
                seal.interpolation(20, 0);
            } else {
                // Post-materialization: slow scale pulse, period 200 ticks
                float xzScale = 3.0f + 0.2f * (float) Math.sin(2.0 * Math.PI * ticksAlive / 200.0);
                seal.scale(xzScale, 0.2f, xzScale);
                seal.interpolation(20, 0);
            }

            // Death sequence particles (ticks 0-120)
            if (ticksAlive < 120) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, sealLoc, 3, 1.0, 0.2, 1.0, 0.03);
                w.spawnParticle(Particle.DRAGON_BREATH, sealLoc, 2, 1.0, 0.2, 1.0, 0.02);
                w.spawnParticle(Particle.DUST, sealLoc, 1, 0.5, 0.1, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.0f));
            } else {
                // Post-death ambient: reduced emission
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, sealLoc, 1, 0.5, 0.1, 0.5, 0.01);
            }

            // Post-death ambient sound every 400 ticks
            if (ticksAlive > 120 && ticksAlive % 400 == 0) {
                DisplayBuilder.playSound(sealLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.2f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArchitectsSeal(plugin); }
    }
}
