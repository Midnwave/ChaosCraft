package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Environmental — GROUP 8: DELAYED / TRAP
 * 10 attacks that set up and trigger after a time delay.
 * Attacks 71-80 from boss1-voidmaw.md.
 *
 * Design notes:
 * - All attacks use ticksAlive to gate the real damaging effect
 * - Visual state changes to signal arming / danger thresholds
 * - No status effects — damage only on trigger
 * - tracksPlayer = false (traps are positional, not target-following)
 * - Calamity color palette throughout
 */
public final class DelayedTrap {

    private DelayedTrap() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RuneTrap(plugin));
        registry.register(new FalseFloor(plugin));
        registry.register(new VoidMine(plugin));
        registry.register(new CountdownPillar(plugin));
        registry.register(new SleepingAnchor(plugin));
        registry.register(new PhantomChest(plugin));
        registry.register(new SlowCorruption(plugin));
        registry.register(new EchoBomb(plugin));
        registry.register(new RiftSeed(plugin));
        registry.register(new MemoryTrap(plugin));
    }

    // =========================================================================
    // 71. RUNE TRAP — glowing floor glyph arms over 10 seconds, then detonates
    // =========================================================================
    public static class RuneTrap extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> runeGlyph = new ArrayList<>();
        private boolean detonated = false;

        public RuneTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rune_trap", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(500); // 25 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Complex void rune: 2-block wide glyph on the floor
            // Outer ring of small tiles
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 0.01, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_CONCRETE);
                h.scale(0.25f, 0.02f, 0.25f).glow(30, 0, 60).interpolation(3, 0); // starts dim
                runeGlyph.add(h);
                spawnedEntities.add(h.entity());
            }
            // Inner cross strokes
            double[][] crossStrokes = {{0.6, 0}, {-0.6, 0}, {0, 0.6}, {0, -0.6}};
            for (double[] stroke : crossStrokes) {
                Location loc = center.clone().add(stroke[0], 0.01, stroke[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.15f, 0.02f, 0.4f).glow(30, 0, 60).interpolation(3, 0);
                runeGlyph.add(h);
                spawnedEntities.add(h.entity());
            }
            // Center
            BlockDisplayHandle rCenter = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.OBSIDIAN);
            rCenter.scale(0.3f, 0.02f, 0.3f).glow(30, 0, 60).interpolation(3, 0);
            runeGlyph.add(rCenter);
            spawnedEntities.add(rCenter.entity());

            // No fanfare — appears quietly
            DisplayBuilder.purpleDust(center, 5, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arm window: 0-200 ticks (10 seconds). Brightness increases linearly.
            if (ticksAlive <= 200 && !detonated) {
                float armProgress = ticksAlive / 200.0f; // 0.0 -> 1.0
                int glowR = (int)(30 + armProgress * 98);   // 30 -> 128
                int glowG = 0;
                int glowB = (int)(60 + armProgress * 195);  // 60 -> 255

                for (BlockDisplayHandle h : runeGlyph) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(glowR, glowG, glowB));
                    bd.setGlowing(true);
                }

                // Warning buzz at 80% (tick 160)
                if (ticksAlive == 160) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.7f, 1.5f);
                }

                // Tick particle activity near full-armed
                if (ticksAlive > 160 && ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 1.5);
                }
            }

            // Detonation at tick 200
            if (ticksAlive == 200 && !detonated) {
                detonated = true;
                DisplayBuilder.crimsonDust(center, 60, 3.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
                // 6 hearts to anyone standing on the rune (2 block radius)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 4) {
                        p.damage(12.0); // 6 hearts
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RuneTrap(plugin); }
    }

    // =========================================================================
    // 72. FALSE FLOOR — barely-visible 5x5 trap tiles that collapse on step
    // =========================================================================
    public static class FalseFloor extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> trapTiles = new ArrayList<>();
        private boolean active = false;

        public FalseFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("false_floor", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(6.0); // 3 hearts from void-catch fall
            config.setDamageRadius(2.5);
            config.setDurationTicks(420); // 20-second window
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5x5 area of very subtly different floor tiles
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = center.clone().add(x, 0.005, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_STONE);
                    // Nearly invisible — very thin, barely different shade
                    h.scale(0.98f, 0.008f, 0.98f).glow(20, 20, 40).interpolation(2, 0);
                    trapTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Near-silent creak
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Activate the trap (check player proximity) from tick 1 onward
            if (!active && ticksAlive > 5) active = true;

            if (!active) return;

            // Check if any player steps on the tiles
            if (ticksAlive % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    if (pLoc.distanceSquared(center) <= 8) { // ~2.8 block radius
                        // Tile collapse visual
                        for (BlockDisplayHandle h : trapTiles) {
                            BlockDisplay bd = (BlockDisplay) h.entity();
                            bd.setTransformation(new Transformation(
                                    new Vector3f(-0.49f, -2.0f, -0.49f), // drop tiles down
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.98f, 0.008f, 0.98f),
                                    new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(10);
                        }
                        DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);
                        DisplayBuilder.purpleDust(center, 25, 3.0);
                        p.damage(6.0); // 3 hearts void-catch
                        active = false;
                        return;
                    }
                }
            }

            // Faint shimmer at the 20-second mark (tiles solidifying back)
            if (ticksAlive > 380) {
                if (ticksAlive % 10 == 0) DisplayBuilder.purpleDust(center, 3, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FalseFloor(plugin); }
    }

    // =========================================================================
    // 73. VOID MINE — nearly invisible proximity mines, up to 5 scattered
    // =========================================================================
    public static class VoidMine extends EnvironmentalAttack {

        private static class Mine {
            BlockDisplayHandle marker;
            Location loc;
            boolean triggered = false;

            Mine(BlockDisplayHandle marker, Location loc) {
                this.marker = marker;
                this.loc = loc;
            }
        }

        private final List<Mine> mines = new ArrayList<>();

        public VoidMine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_mine", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(640); // 32 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 mines scattered across the arena at random offsets
            double[][] minePositions = {
                {4, 0, 3}, {-5, 0, 2}, {2, 0, -6}, {-3, 0, -4}, {6, 0, -1}
            };
            for (double[] pos : minePositions) {
                Location mineLoc = center.clone().add(pos[0], 0.01, pos[2]);
                // Nearly invisible — hairline crack look: tiny purple dot
                BlockDisplayHandle h = displayBuilder.spawnBlock(mineLoc, Material.PURPLE_CONCRETE);
                h.scale(0.06f, 0.015f, 0.06f).glow(40, 0, 80).interpolation(2, 0);
                mines.add(new Mine(h, mineLoc));
                spawnedEntities.add(h.entity());
            }

            // Near-silent placement clicks
            for (Mine mine : mines) {
                DisplayBuilder.playSound(mine.loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.2f, 2.0f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Check each mine for player proximity
            for (Mine mine : mines) {
                if (mine.triggered) continue;

                // Trigger radius: 1 block
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(mine.loc) <= 1.0) {
                        // Detonate
                        mine.triggered = true;
                        DisplayBuilder.crimsonDust(mine.loc, 50, 3.5);
                        DisplayBuilder.playSound(mine.loc, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.7f);
                        // 5 hearts to nearby players (3-block radius)
                        for (Player nearby : w.getPlayers()) {
                            if (isExempt(nearby)) continue;
                            if (nearby.getLocation().distanceSquared(mine.loc) <= 9) {
                                nearby.damage(10.0); // 5 hearts
                            }
                        }
                        // Remove the marker
                        mine.marker.entity().remove();
                        break;
                    }
                }
            }

            // 30-second defuse — remaining mines defuse silently
            if (ticksAlive >= 600) {
                for (Mine mine : mines) {
                    if (!mine.triggered) {
                        mine.triggered = true;
                        DisplayBuilder.purpleDust(mine.loc, 5, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMine(plugin); }
    }

    // =========================================================================
    // 74. COUNTDOWN PILLAR — 5-band pillar, one band fades per 5 seconds, then explodes
    // =========================================================================
    public static class CountdownPillar extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> bands = new ArrayList<>();
        private int lastBandRemovedAt = 0;
        private int bandsRemaining = 5;
        private boolean detonated = false;

        public CountdownPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("countdown_pillar", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(560); // 28 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5-block-tall void energy pillar with 5 distinct glowing bands
            for (int y = 0; y < 5; y++) {
                Location bandLoc = center.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(bandLoc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.8f, 0.9f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                bands.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dramatic eruption
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(center, 40, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each band fades at 100-tick intervals (5 seconds each)
            if (bandsRemaining > 0 && ticksAlive - lastBandRemovedAt >= 100 && !detonated) {
                lastBandRemovedAt = ticksAlive;
                int bandIdx = 5 - bandsRemaining; // remove from top down
                if (bandIdx < bands.size()) {
                    BlockDisplay bd = (BlockDisplay) bands.get(bandIdx).entity();
                    // Shrink to nothing
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.4f, -0.45f, -0.4f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(15);
                    DisplayBuilder.purpleDust(center.clone().add(0, bandIdx, 0), 10, 1.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.5f + (bandIdx * 0.1f));
                }
                bandsRemaining--;
            }

            // Expansion visual — pillar grows with each band removed
            if (ticksAlive % 15 == 0 && !detonated) {
                float expansionFactor = 0.8f + (5 - bandsRemaining) * 0.15f;
                for (int i = 5 - bandsRemaining; i < bands.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) bands.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-expansionFactor / 2, -0.45f, -expansionFactor / 2),
                            new AxisAngle4f(0, 1, 0, ticksAlive * 0.02f),
                            new Vector3f(expansionFactor, 0.9f, expansionFactor),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(8);
                }
            }

            // All bands gone — full detonation
            if (bandsRemaining == 0 && !detonated) {
                detonated = true;
                DisplayBuilder.crimsonDust(center, 120, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.0f);
                // 8 hearts direct, scale down with distance
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 10) {
                        double dmg = 16.0 * Math.max(0, 1.0 - (dist / 10.0)); // 0-8 hearts
                        dmg = Math.max(dmg, 2.0); // minimum 1 heart
                        p.damage(dmg);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CountdownPillar(plugin); }
    }

    // =========================================================================
    // 75. SLEEPING ANCHOR — dormant anchor sits 15 seconds, then slams
    // =========================================================================
    public static class SleepingAnchor extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> anchorParts = new ArrayList<>();
        private boolean armed = false;
        private boolean slammed = false;

        public SleepingAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sleeping_anchor", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts/sec during armed phase contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(360);
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Anchor shape — crossbar + vertical shaft
            // Shaft
            for (int y = 0; y < 4; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.OBSIDIAN);
                h.scale(0.5f, 1f, 0.5f).glow(40, 40, 80).interpolation(5, 0); // starts dim (dormant)
                anchorParts.add(h);
                spawnedEntities.add(h.entity());
            }
            // Crossbar
            for (int x = -2; x <= 2; x++) {
                if (x == 0) continue;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 3, 0), Material.OBSIDIAN);
                h.scale(1f, 0.5f, 0.5f).glow(40, 40, 80).interpolation(5, 0);
                anchorParts.add(h);
                spawnedEntities.add(h.entity());
            }
            // Hook arms
            BlockDisplayHandle hookL = displayBuilder.spawnBlock(center.clone().add(-1.5, -0.5, 0), Material.CRYING_OBSIDIAN);
            hookL.scale(0.4f, 0.8f, 0.4f).glow(40, 40, 80).interpolation(5, 0);
            anchorParts.add(hookL);
            spawnedEntities.add(hookL.entity());

            BlockDisplayHandle hookR = displayBuilder.spawnBlock(center.clone().add(1.5, -0.5, 0), Material.CRYING_OBSIDIAN);
            hookR.scale(0.4f, 0.8f, 0.4f).glow(40, 40, 80).interpolation(5, 0);
            anchorParts.add(hookR);
            spawnedEntities.add(hookR.entity());

            // Dormant thud
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Dormant for 10 seconds (200 ticks) — passes freely
            if (ticksAlive < 200) {
                // Subtle ambient pulse at 5 second mark
                if (ticksAlive == 100) {
                    DisplayBuilder.purpleDust(center, 5, 1.5);
                }
                return; // No damage before arming
            }

            // At tick 200 — arm the anchor
            if (!armed) {
                armed = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
                DisplayBuilder.purpleDust(center, 30, 3.0);
                // Glow brightens on all parts
                for (BlockDisplayHandle h : anchorParts) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                    bd.setGlowing(true);
                }
                // Vibrate starts
            }

            // Armed phase (200-300): vibrate + contact damage applied by parent radius
            if (armed && !slammed && ticksAlive < 300) {
                if (ticksAlive % 6 == 0) {
                    float shake = (float)(Math.random() * 0.08 - 0.04);
                    for (BlockDisplayHandle h : anchorParts) {
                        Location shakeLoc = h.entity().getLocation().clone().add(shake, 0, shake);
                        h.entity().teleport(shakeLoc);
                        h.entity().setInterpolationDuration(2);
                        h.entity().setInterpolationDelay(0);
                    }
                    DisplayBuilder.purpleDust(center, 6, 2.0);
                }
            }

            // At tick 300 — slam
            if (ticksAlive == 300 && !slammed) {
                slammed = true;
                config.setDamage(0.0); // Stop continuous damage
                DisplayBuilder.crimsonDust(center, 100, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
                // 7 hearts in 10-block radius
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100) {
                        p.damage(14.0); // 7 hearts
                    }
                }
                // Slam visual — anchor buries into ground
                for (BlockDisplayHandle h : anchorParts) {
                    Location sinkLoc = h.entity().getLocation().clone().add(0, -3, 0);
                    h.entity().teleport(sinkLoc);
                    h.entity().setInterpolationDuration(5);
                    h.entity().setInterpolationDelay(0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SleepingAnchor(plugin); }
    }

    // =========================================================================
    // 76. PHANTOM CHEST — lure chest that detonates 3 seconds after interaction
    // =========================================================================
    public static class PhantomChest extends EnvironmentalAttack {

        private BlockDisplayHandle chestDisplay;
        private boolean triggered = false;
        private boolean detonated = false;
        private int triggerTick = -1;

        public PhantomChest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_chest", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(420); // 20-second window if never touched
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ornate glowing chest in the center
            chestDisplay = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.CHEST);
            chestDisplay.scale(0.9f, 0.9f, 0.9f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(chestDisplay.entity());

            // Enticing particle drift from the lid
            DisplayBuilder.purpleDust(center.clone().add(0, 1.2, 0), 10, 0.5);
            DisplayBuilder.cyanDust(center.clone().add(0, 1.1, 0), 5, 0.3);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!detonated) {
                // Lid slowly opens animation
                if (ticksAlive % 8 == 0) {
                    float openAngle = Math.min(ticksAlive * 0.005f, 0.4f);
                    BlockDisplay bd = (BlockDisplay) chestDisplay.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.45f, -0.45f),
                            new AxisAngle4f(openAngle, 1, 0, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.2, 0), 3, 0.3);
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.0, 0), 2, 0.3);
                }

                // Check if a player gets close enough to "open" it (within 2 blocks)
                if (!triggered) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 4) {
                            triggered = true;
                            triggerTick = ticksAlive;
                            // 3-second countdown warning
                            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.5f);
                            DisplayBuilder.crimsonDust(center, 20, 2.0);
                            break;
                        }
                    }
                }

                // After 3-second countdown
                if (triggered && ticksAlive - triggerTick >= 60) {
                    detonated = true;
                    DisplayBuilder.crimsonDust(center, 80, 8.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                    // 7 hearts if couldn't flee 8 blocks in time
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist <= 64) { // 8 blocks
                            p.damage(14.0); // 7 hearts
                        } else if (dist <= 225) { // 15 blocks — defused version
                            p.damage(4.0); // 2 hearts
                        }
                    }
                    // Destroy chest visual
                    chestDisplay.entity().remove();
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomChest(plugin); }
    }

    // =========================================================================
    // 77. SLOW CORRUPTION — tile darkens over 20 seconds, then activates for damage
    // =========================================================================
    public static class SlowCorruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final List<Integer> activationTicks = new ArrayList<>();

        public SlowCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_corruption", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts/sec on fully activated tiles
            config.setDamageRadius(1.5);
            config.setDurationTicks(1200); // 60 seconds total
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial marked tile — faintest shimmer
            Location seed = center.clone().add(2, 0.01, 1);
            BlockDisplayHandle seedTile = displayBuilder.spawnBlock(seed, Material.PURPLE_CONCRETE);
            seedTile.scale(0.95f, 0.02f, 0.95f).glow(10, 0, 20).interpolation(5, 0);
            tiles.add(seedTile);
            activationTicks.add(600); // fully active at tick 600 (30 seconds)
            spawnedEntities.add(seedTile.entity());

            // Surrounding tiles spread outward in rings, each delayed 5 seconds more
            int[][] ring1 = {{3,1},{1,2},{3,2},{2,0},{1,0},{4,1},{2,2}};
            int[][] ring2 = {{0,1},{5,1},{2,3},{4,0},{0,2},{5,2},{3,3},{1,-1},{4,-1}};
            for (int[] pos : ring1) {
                Location loc = center.clone().add(pos[0], 0.01, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_CONCRETE);
                h.scale(0.95f, 0.02f, 0.95f).glow(8, 0, 16).interpolation(5, 0);
                tiles.add(h);
                activationTicks.add(700); // 5 seconds later
                spawnedEntities.add(h.entity());
            }
            for (int[] pos : ring2) {
                Location loc = center.clone().add(pos[0], 0.01, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_CONCRETE);
                h.scale(0.95f, 0.02f, 0.95f).glow(6, 0, 12).interpolation(5, 0);
                tiles.add(h);
                activationTicks.add(800); // 10 seconds later
                spawnedEntities.add(h.entity());
            }

            // Silent appearance — no sound
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Gradually darken each tile as it approaches activation
            for (int i = 0; i < tiles.size(); i++) {
                int activateAt = activationTicks.get(i);
                float progress = Math.min(1f, ticksAlive / (float) activateAt);
                int glow = (int)(10 + progress * 118); // 10 -> 128

                BlockDisplay bd = (BlockDisplay) tiles.get(i).entity();
                bd.setGlowColorOverride(Color.fromRGB(glow, 0, glow * 2));
                bd.setGlowing(true);
            }

            // Ambient presence when tiles begin activating
            if (ticksAlive == 400) {
                DisplayBuilder.purpleDust(center, 8, 5.0);
            }
            if (ticksAlive == 600) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.4f);
            }

            // Fade tiles out after max duration (tick 1100-1200)
            if (ticksAlive > 1100) {
                for (BlockDisplayHandle h : tiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(20, 0, 40));
                    bd.setGlowing(true);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SlowCorruption(plugin); }
    }

    // =========================================================================
    // 78. ECHO BOMB — invisible entity leaves footprints; all detonate at tick 300
    // =========================================================================
    public static class EchoBomb extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> footprints = new ArrayList<>();
        private int lastFootprintTick = 0;
        private boolean detonating = false;
        private int detonationStep = 0;

        public EchoBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_bomb", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(480);
            config.setCooldownTicks(1300); // 65 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Invisible entity walking — no initial spawn particles, just silence
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Leave footprints every 20 ticks (1 second) along a curved path
            if (!detonating && ticksAlive - lastFootprintTick >= 20 && footprints.size() < 15) {
                lastFootprintTick = ticksAlive;
                double pathAngle = footprints.size() * 0.4; // curved path
                double radius = 3.0 + footprints.size() * 0.5;
                Location footLoc = center.clone().add(
                        Math.cos(pathAngle) * radius,
                        0.01,
                        Math.sin(pathAngle) * radius
                );
                BlockDisplayHandle footprint = displayBuilder.spawnBlock(footLoc, Material.PURPLE_CONCRETE);
                footprint.scale(0.3f, 0.02f, 0.5f).glow(60, 0, 120).interpolation(3, 0);
                footprints.add(footprint);
                spawnedEntities.add(footprint.entity());
            }

            // At tick 240 — footprints start glowing intensely (12 seconds warning)
            if (ticksAlive == 240) {
                for (BlockDisplayHandle h : footprints) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                    bd.setGlowing(true);
                    bd.setInterpolationDuration(10);
                    bd.setInterpolationDelay(0);
                }
            }

            // Ticking sound at tick 280
            if (ticksAlive == 280) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 2.0f);
            }

            // Sequential detonation starts at tick 300
            if (ticksAlive >= 300 && !detonating) {
                detonating = true;
            }

            // Detonate one footprint every 8 ticks
            if (detonating && ticksAlive % 8 == 0 && detonationStep < footprints.size()) {
                BlockDisplayHandle footprint = footprints.get(detonationStep);
                Location blastLoc = footprint.entity().getLocation();
                boolean isFinal = (detonationStep == footprints.size() - 1);

                DisplayBuilder.crimsonDust(blastLoc, isFinal ? 50 : 25, isFinal ? 4.0 : 2.5);
                DisplayBuilder.playSound(blastLoc, Sound.ENTITY_WITHER_SHOOT, 0.6f, isFinal ? 0.4f : 0.8f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double blastRadius = isFinal ? 3.0 : 2.0;
                    if (p.getLocation().distanceSquared(blastLoc) <= blastRadius * blastRadius) {
                        p.damage(isFinal ? 12.0 : 6.0); // final = 6 hearts; others = 3 hearts
                    }
                }

                footprint.entity().remove();
                detonationStep++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoBomb(plugin); }
    }

    // =========================================================================
    // 79. RIFT SEED — tiny sphere grows over 20 seconds, then tears void rift
    // =========================================================================
    public static class RiftSeed extends EnvironmentalAttack {

        private BlockDisplayHandle seedSphere;
        private final List<BlockDisplayHandle> riftTear = new ArrayList<>();
        private boolean riftOpen = false;

        public RiftSeed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_seed", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts/sec inside rift pull
            config.setDamageRadius(12.0);
            config.setDurationTicks(520); // 20s growth + 6s rift
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tiny 1/4-block sphere at chest height — barely visible
            seedSphere = displayBuilder.spawnBlock(center.clone().add(0, 1.2, 0), Material.OBSIDIAN);
            seedSphere.scale(0.05f, 0.05f, 0.05f).glow(20, 0, 40).interpolation(3, 0);
            spawnedEntities.add(seedSphere.entity());

            // No sound — nearly invisible
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!riftOpen) {
                // Growth phase: 400 ticks (20 seconds), size 0.05 -> 3 blocks
                float growthProgress = Math.min(ticksAlive / 400.0f, 1.0f);
                float size = 0.05f + growthProgress * 2.95f;

                BlockDisplay bd = (BlockDisplay) seedSphere.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-size / 2, -size / 2, -size / 2),
                        new AxisAngle4f(0, 1, 0, ticksAlive * 0.03f),
                        new Vector3f(size, size, size),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(5);

                // Glow intensifies with growth
                int glowStrength = (int)(20 + growthProgress * 108);
                bd.setGlowColorOverride(Color.fromRGB(glowStrength, 0, glowStrength * 2));
                bd.setGlowing(true);

                // Shaking at tick 360 (18 seconds)
                if (ticksAlive >= 360 && ticksAlive % 4 == 0) {
                    Location shakeLoc = center.clone().add(
                            0 + (Math.random() - 0.5) * 0.15,
                            1.2,
                            0 + (Math.random() - 0.5) * 0.15
                    );
                    bd.teleport(shakeLoc);
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.2, 0), 5, size * 0.5);
                }

                // Detonation at tick 400
                if (ticksAlive == 400) {
                    riftOpen = true;
                    // Explosion
                    DisplayBuilder.crimsonDust(center, 80, 5.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
                    // 5 hearts explosion
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 25) {
                            p.damage(10.0); // 5 hearts
                        }
                    }

                    // Spawn rift tear — vertical slice of void
                    seedSphere.entity().remove();
                    for (int y = 0; y < 6; y++) {
                        BlockDisplayHandle rift = displayBuilder.spawnBlock(center.clone().add(0, y - 1, 0), Material.CRYING_OBSIDIAN);
                        rift.scale(0.15f, 1f, 0.15f).glow(128, 0, 255).interpolation(3, 0);
                        riftTear.add(rift);
                        spawnedEntities.add(rift.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
                }
            } else {
                // Rift pull phase (ticks 400-520): pull visual
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center, 12, 8.0);
                }
                // Rift flicker
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle h : riftTear) {
                        float flicker = 0.1f + (float)(Math.random() * 0.08f);
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-flicker / 2, -0.5f, -flicker / 2),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(flicker, 1f, flicker),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftSeed(plugin); }
    }

    // =========================================================================
    // 80. MEMORY TRAP — warm false safe zone, inverts to void after 5-second lure
    // =========================================================================
    public static class MemoryTrap extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> zoneTiles = new ArrayList<>();
        private boolean inverted = false;
        private boolean playerInZone = false;
        private int entryTick = -1;

        public MemoryTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_trap", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(620); // 30-second active window + ramp
            config.setCooldownTicks(2000); // 100 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warm yellow-white glow zone — completely unlike void palette (the deception)
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = center.clone().add(x, 0.02, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GLOWSTONE);
                    h.scale(0.95f, 0.04f, 0.95f).glow(255, 220, 100).interpolation(5, 0);
                    zoneTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Warm light appearance — inviting, no threat sound
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!inverted) {
                // Warm glow intensifies to encourage entry
                if (ticksAlive % 20 == 0 && !playerInZone) {
                    // Brighter glow pulse on the tiles
                    for (BlockDisplayHandle h : zoneTiles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        float pulseSize = 0.95f + (float)(Math.sin(ticksAlive * 0.15) * 0.03f);
                        bd.setTransformation(new Transformation(
                                new Vector3f(-pulseSize / 2, -0.02f, -pulseSize / 2),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(pulseSize, 0.04f, pulseSize),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(10);
                    }
                }

                // Check if player steps into zone
                if (!playerInZone) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 9) { // ~3 block radius
                            playerInZone = true;
                            entryTick = ticksAlive;
                            // Speed boost — confirms player the zone is safe
                            DisplayBuilder.cyanDust(center, 15, 3.0);
                            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
                            // Warn intensification of warm glow (inversion incoming)
                            for (BlockDisplayHandle h : zoneTiles) {
                                BlockDisplay bd = (BlockDisplay) h.entity();
                                bd.setGlowColorOverride(Color.fromRGB(255, 200, 80));
                                bd.setGlowing(true);
                            }
                            break;
                        }
                    }
                }

                // 5-second countdown after entry
                if (playerInZone && ticksAlive - entryTick >= 100) {
                    inverted = true;

                    // Inversion — warm becomes void-purple
                    for (BlockDisplayHandle h : zoneTiles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setBlock(Material.PURPLE_CONCRETE.createBlockData());
                        bd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                        bd.setGlowing(true);
                    }

                    DisplayBuilder.crimsonDust(center, 80, 6.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);

                    // 4 hearts to everyone inside
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 9) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }

            // Post-inversion: zone persists with damaging purple tiles for 10 more seconds
            if (inverted && ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 10, 3.0);
                // Confusion damage proxy
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9) {
                        p.damage(2.0); // minor confusion proxy
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MemoryTrap(plugin); }
    }
}
