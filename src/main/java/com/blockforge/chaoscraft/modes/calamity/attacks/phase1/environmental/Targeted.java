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
 * Phase 1 Environmental — GROUP 7: TARGETED
 * 10 attacks that lock onto and track specific players.
 * Attacks 61-70 from boss1-voidmaw.md.
 *
 * Design notes:
 * - Targeted category: ~50% can setTracksPlayer(true) — exception to global 5% rule
 * - tracksPlayer(true) attacks: ShadeHunter, VoidLock, TheStare, VoidParasite, GravityInversionPersonal
 * - No status effects — damage and visual threat only
 * - Calamity color palette throughout
 */
public final class Targeted {

    private Targeted() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadeHunter(plugin));
        registry.register(new VoidLock(plugin));
        registry.register(new Echo(plugin));
        registry.register(new MarkedForTheVoid(plugin));
        registry.register(new TheStare(plugin));
        registry.register(new VoidParasite(plugin));
        registry.register(new Unraveling(plugin));
        registry.register(new PhantomDoppelganger(plugin));
        registry.register(new GravityInversionPersonal(plugin));
        registry.register(new IsolationCocoon(plugin));
    }

    // =========================================================================
    // 61. SHADE HUNTER — void shadow pursues highest-health player (tracks)
    // =========================================================================
    public static class ShadeHunter extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> bodyParts = new ArrayList<>();
        private int contactTick = -1;

        public ShadeHunter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shade_hunter", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(10.0); // 5 hearts on contact
            config.setDamageRadius(2.0);
            config.setDurationTicks(400); // 20 seconds max
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shade body: distorted humanoid shape from obsidian + crying obsidian
            // Torso
            BlockDisplayHandle torso = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.OBSIDIAN);
            torso.scale(0.7f, 1.2f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
            bodyParts.add(torso);
            spawnedEntities.add(torso.entity());

            // Head (slightly offset — wrong proportions)
            BlockDisplayHandle head = displayBuilder.spawnBlock(center.clone().add(0.15f, 2.2f, 0), Material.CRYING_OBSIDIAN);
            head.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
            bodyParts.add(head);
            spawnedEntities.add(head.entity());

            // Left arm (stretched unnaturally)
            BlockDisplayHandle armL = displayBuilder.spawnBlock(center.clone().add(-0.8f, 1.5f, 0), Material.OBSIDIAN);
            armL.scale(0.4f, 0.9f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
            bodyParts.add(armL);
            spawnedEntities.add(armL.entity());

            // Right arm
            BlockDisplayHandle armR = displayBuilder.spawnBlock(center.clone().add(0.8f, 1.3f, 0), Material.OBSIDIAN);
            armR.scale(0.4f, 1.1f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
            bodyParts.add(armR);
            spawnedEntities.add(armR.entity());

            // Black footprints visual at spawn
            DisplayBuilder.purpleDust(center, 20, 1.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Subtle shift — proportions shift every few seconds
            if (ticksAlive % 30 == 0) {
                float headShift = (float)(Math.random() * 0.3 - 0.15);
                BlockDisplay headBd = (BlockDisplay) bodyParts.get(1).entity();
                Location headLoc = center.clone().add(headShift, 2.2, 0);
                headBd.teleport(headLoc);
                headBd.setInterpolationDuration(15);
                headBd.setInterpolationDelay(0);
            }

            // Move the whole structure (center tracks player via config.tracksPlayer)
            double[][] allOffsets = {{0, 1, 0}, {0.15, 2.2, 0}, {-0.8, 1.5, 0}, {0.8, 1.3, 0}};
            for (int i = 0; i < bodyParts.size(); i++) {
                double[] offsets = allOffsets[i];
                Location partLoc = center.clone().add(offsets[0], offsets[1], offsets[2]);
                BlockDisplay bd = (BlockDisplay) bodyParts.get(i).entity();
                bd.teleport(partLoc);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            // Footprint particles as it moves
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0, 0), 5, 0.5);
            }

            // Closing sound when near target
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadeHunter(plugin); }
    }

    // =========================================================================
    // 62. VOID LOCK — contracting ring roots a player in place (tracks)
    // =========================================================================
    public static class VoidLock extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> lockRing = new ArrayList<>();
        private boolean contracted = false;

        public VoidLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_lock", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts backlash damage
            config.setDamageRadius(1.5);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(100);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring appears at 4-block radius, will contract to player position
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 4, 0.1, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                lockRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 0.8f, 0.5f);
            DisplayBuilder.purpleDust(center, 25, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Contract over 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                float progress = ticksAlive / 30.0f;
                double radius = 4.0 * (1.0 - progress) + 0.6;
                for (int i = 0; i < lockRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / lockRing.size();
                    Location newLoc = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                    BlockDisplay bd = (BlockDisplay) lockRing.get(i).entity();
                    bd.teleport(newLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
            }

            // Lock sound at full contraction
            if (ticksAlive == 30 && !contracted) {
                contracted = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);
                DisplayBuilder.crimsonDust(center, 30, 2.0);
            }

            // Locked visual — crackling ring pulses
            if (ticksAlive > 30 && ticksAlive % 8 == 0) {
                float pulse = 0.5f + (float)(Math.sin(ticksAlive * 0.5) * 0.15f);
                for (BlockDisplayHandle h : lockRing) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -0.3f, -pulse / 2),
                            new AxisAngle4f(0, 1, 0, ticksAlive * 0.05f),
                            new Vector3f(pulse, 0.6f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
                DisplayBuilder.purpleDust(center, 5, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidLock(plugin); }
    }

    // =========================================================================
    // 63. ECHO — void copy follows player, rushes to merge after 8 seconds
    // =========================================================================
    public static class Echo extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> echoBody = new ArrayList<>();
        private boolean rushing = false;

        public Echo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(8.0); // 4 hearts on merge
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(280); // once at end
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Echo body: player-sized deep purple structure, spawns 5 blocks behind center
            Location echoBehind = center.clone().add(0, 0, 5);

            // Body
            BlockDisplayHandle body = displayBuilder.spawnBlock(echoBehind.clone().add(0, 1, 0), Material.PURPLE_STAINED_GLASS);
            body.scale(0.6f, 1.0f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
            echoBody.add(body);
            spawnedEntities.add(body.entity());

            // Head
            BlockDisplayHandle head = displayBuilder.spawnBlock(echoBehind.clone().add(0, 2.1, 0), Material.PURPLE_STAINED_GLASS);
            head.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
            echoBody.add(head);
            spawnedEntities.add(head.entity());

            // Legs
            BlockDisplayHandle legL = displayBuilder.spawnBlock(echoBehind.clone().add(-0.25, 0, 0), Material.PURPLE_STAINED_GLASS);
            legL.scale(0.25f, 0.9f, 0.25f).glow(80, 0, 160).interpolation(3, 0);
            echoBody.add(legL);
            spawnedEntities.add(legL.entity());

            BlockDisplayHandle legR = displayBuilder.spawnBlock(echoBehind.clone().add(0.25, 0, 0), Material.PURPLE_STAINED_GLASS);
            legR.scale(0.25f, 0.9f, 0.25f).glow(80, 0, 160).interpolation(3, 0);
            echoBody.add(legR);
            spawnedEntities.add(legR.entity());

            // Shadow separation visual
            DisplayBuilder.purpleDust(center, 15, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Follow the player with 1-second (20 tick) delay
            if (ticksAlive < 160 && !rushing) {
                double[][] offsets = {{0, 1, 0}, {0, 2.1, 0}, {-0.25, 0, 0}, {0.25, 0, 0}};
                for (int i = 0; i < echoBody.size(); i++) {
                    Location followLoc = center.clone().add(0, 0, 5 - (ticksAlive * 0.025)); // close in slowly
                    followLoc.add(offsets[i][0], offsets[i][1], offsets[i][2]);
                    BlockDisplay bd = (BlockDisplay) echoBody.get(i).entity();
                    bd.teleport(followLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0, 5 - (ticksAlive * 0.025)), 5, 0.8);
                }
            }

            // At tick 160 (8 seconds) — echo rushes forward
            if (ticksAlive == 160) {
                rushing = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.5f);
                DisplayBuilder.crimsonDust(center, 40, 4.0);
            }

            // Rush phase — snap to center
            if (rushing && ticksAlive > 160) {
                double[][] rushOffsets = {{0, 1, 0}, {0, 2.1, 0}, {-0.25, 0, 0}, {0.25, 0, 0}};
                for (int i = 0; i < echoBody.size(); i++) {
                    Location mergeLoc = center.clone().add(rushOffsets[i][0], rushOffsets[i][1], rushOffsets[i][2]);
                    BlockDisplay bd = (BlockDisplay) echoBody.get(i).entity();
                    bd.teleport(mergeLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
                DisplayBuilder.purpleDust(center, 15, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Echo(plugin); }
    }

    // =========================================================================
    // 64. MARKED FOR THE VOID — glowing mark above player, leaves corruption trail
    // =========================================================================
    public static class MarkedForTheVoid extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> markSymbol = new ArrayList<>();
        private final List<BlockDisplayHandle> trailTiles = new ArrayList<>();

        public MarkedForTheVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("marked_for_the_void", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // contact with trail tiles
            config.setDamageRadius(1.5);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(30);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Fractured circle glyph above the player (3 blocks up)
            // Outer ring
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 3.2, Math.sin(angle) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.2f, 0.2f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
                markSymbol.add(h);
                spawnedEntities.add(h.entity());
            }
            // Center dot (fractured)
            BlockDisplayHandle center_dot = displayBuilder.spawnBlock(center.clone().add(0, 3.2, 0), Material.PURPLE_STAINED_GLASS);
            center_dot.scale(0.3f, 0.15f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
            markSymbol.add(center_dot);
            spawnedEntities.add(center_dot.entity());

            // Instant appearance — no pre-warning
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
            DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 20, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Move mark symbol above the player (center tracks via tracksPlayer)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 3.2 + Math.sin(ticksAlive * 0.1) * 0.1, Math.sin(angle) * 0.8);
                BlockDisplay bd = (BlockDisplay) markSymbol.get(i).entity();
                bd.teleport(loc);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }
            // Center dot follows too
            if (markSymbol.size() > 8) {
                BlockDisplay dotBd = (BlockDisplay) markSymbol.get(8).entity();
                dotBd.teleport(center.clone().add(0, 3.2, 0));
                dotBd.setInterpolationDelay(0);
                dotBd.setInterpolationDuration(3);
            }

            // Leave corruption trail tiles every 10 ticks
            if (ticksAlive % 10 == 0 && trailTiles.size() < 30) {
                BlockDisplayHandle trail = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.PURPLE_CONCRETE);
                trail.scale(0.9f, 0.02f, 0.9f).glow(80, 0, 160).interpolation(2, 0);
                trailTiles.add(trail);
                spawnedEntities.add(trail.entity());
                DisplayBuilder.purpleDust(center.clone().add(0, 0.05, 0), 4, 0.5);
            }

            // Dripping particles downward from mark
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 2.5, 0), 3, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkedForTheVoid(plugin); }
    }

    // =========================================================================
    // 65. THE STARE — enormous eyes open in sky above player, beam damage (tracks)
    // =========================================================================
    public static class TheStare extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();

        public TheStare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_stare", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // 1 heart per second
            config.setDamageRadius(3.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Two enormous eyes (5 blocks wide) in the sky at Y+18
            // Left eye whites
            for (int dx = -4; dx <= -1; dx++) {
                Location loc = center.clone().add(dx - 3, 18, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(1f, 0.8f, 0.3f).glow(200, 200, 255).interpolation(4, 0);
                eyeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left pupil
            BlockDisplayHandle leftPupil = displayBuilder.spawnBlock(center.clone().add(-5, 18, 0), Material.OBSIDIAN);
            leftPupil.scale(1.2f, 1.2f, 0.4f).glow(128, 0, 255).interpolation(4, 0);
            eyeBlocks.add(leftPupil);
            spawnedEntities.add(leftPupil.entity());

            // Right eye whites
            for (int dx = 1; dx <= 4; dx++) {
                Location loc = center.clone().add(dx + 3, 18, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(1f, 0.8f, 0.3f).glow(200, 200, 255).interpolation(4, 0);
                eyeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pupil
            BlockDisplayHandle rightPupil = displayBuilder.spawnBlock(center.clone().add(5, 18, 0), Material.OBSIDIAN);
            rightPupil.scale(1.2f, 1.2f, 0.4f).glow(128, 0, 255).interpolation(4, 0);
            eyeBlocks.add(rightPupil);
            spawnedEntities.add(rightPupil.entity());

            // Beams descending from each eye
            for (int y = 1; y < 18; y++) {
                Location beamL = center.clone().add(-5, y, 0);
                BlockDisplayHandle hL = displayBuilder.spawnBlock(beamL, Material.PURPLE_STAINED_GLASS);
                hL.scale(0.2f, 1f, 0.2f).glow(128, 0, 255).interpolation(3, 0);
                beamBlocks.add(hL);
                spawnedEntities.add(hL.entity());

                Location beamR = center.clone().add(5, y, 0);
                BlockDisplayHandle hR = displayBuilder.spawnBlock(beamR, Material.PURPLE_STAINED_GLASS);
                hR.scale(0.2f, 1f, 0.2f).glow(128, 0, 255).interpolation(3, 0);
                beamBlocks.add(hR);
                spawnedEntities.add(hR.entity());
            }

            // 2-second "being watched" sensation
            DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 20, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Move eye blocks and beams with player (center tracks via tracksPlayer)
            // Update eye positions
            int idx = 0;
            for (int dx = -4; dx <= -1; dx++) {
                if (idx < eyeBlocks.size()) {
                    BlockDisplay bd = (BlockDisplay) eyeBlocks.get(idx++).entity();
                    bd.teleport(center.clone().add(dx - 3, 18, 0));
                    bd.setInterpolationDuration(3);
                    bd.setInterpolationDelay(0);
                }
            }

            // Beam flicker
            if (ticksAlive % 6 == 0) {
                float beamWidth = 0.15f + (float)(Math.random() * 0.1);
                for (BlockDisplayHandle h : beamBlocks) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-beamWidth / 2, -0.5f, -beamWidth / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(beamWidth, 1f, beamWidth),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                DisplayBuilder.purpleDust(center, 8, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheStare(plugin); }
    }

    // =========================================================================
    // 66. VOID PARASITE — dark mass attaches to player, explodes at 10 seconds (tracks)
    // =========================================================================
    public static class VoidParasite extends EnvironmentalAttack {

        private BlockDisplayHandle parasiteBody;
        private boolean exploded = false;

        public VoidParasite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_parasite", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // 1 heart per 2 seconds
            config.setDamageRadius(1.5);
            config.setDurationTicks(220);
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(40);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Small pulsing black mass on the player's back
            parasiteBody = displayBuilder.spawnBlock(center.clone().add(0, 1.2, -0.3), Material.OBSIDIAN);
            parasiteBody.scale(0.5f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(parasiteBody.entity());

            // Slithering attachment
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.4f);
            DisplayBuilder.purpleDust(center, 20, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!exploded) {
                // Parasite stays on player's back (center tracks via tracksPlayer)
                BlockDisplay bd = (BlockDisplay) parasiteBody.entity();
                bd.teleport(center.clone().add(0, 1.2, -0.3));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);

                // Pulsing animation
                if (ticksAlive % 12 == 0) {
                    float pulse = 0.4f + (float)(Math.sin(ticksAlive * 0.3) * 0.15f);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -pulse / 2, -0.15f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, 0.3f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(6);
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.2, -0.3), 3, 0.4);
                }

                // At 10 seconds (200 ticks) — detach check / explosion
                if (ticksAlive >= 200) {
                    exploded = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                    DisplayBuilder.crimsonDust(center, 60, 4.0);
                    // Explosion: 4 hearts in 4-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 16) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidParasite(plugin); }
    }

    // =========================================================================
    // 67. UNRAVELING — threads peel away from player, progressive damage amp
    // =========================================================================
    public static class Unraveling extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> threads = new ArrayList<>();

        public Unraveling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("unraveling", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // progressive health drain
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 thread fragments around the player
            double[] threadAngles = {0, 45, 90, 135, 180, 225, 270, 315};
            for (double angleDeg : threadAngles) {
                double angle = Math.toRadians(angleDeg);
                Location loc = center.clone().add(Math.cos(angle) * 0.4, 1.0, Math.sin(angle) * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.08f, 0.8f, 0.08f).glow(128, 0, 255).interpolation(3, 0);
                threads.add(h);
                spawnedEntities.add(h.entity());
            }

            // 1-second flash warning
            DisplayBuilder.purpleDust(center, 15, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Threads stretch outward progressively over 8 seconds (160 ticks)
            float progress = Math.min(ticksAlive / 160.0f, 1.0f);
            double threadReach = 0.4 + progress * 3.5;

            double[] threadAngles = {0, 45, 90, 135, 180, 225, 270, 315};
            for (int i = 0; i < threads.size(); i++) {
                double angle = Math.toRadians(threadAngles[i]);
                Location loc = center.clone().add(Math.cos(angle) * threadReach, 1.0, Math.sin(angle) * threadReach);
                BlockDisplay bd = (BlockDisplay) threads.get(i).entity();
                bd.teleport(loc);
                // Thread becomes longer as it unravels
                float threadLen = 0.8f + progress * 2.0f;
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.04f, -threadLen / 2, -0.04f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.08f, threadLen, 0.08f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center, 8, 2.0 + progress * 3);
            }

            // Reversal visual after 160 ticks (threads contract back)
            if (ticksAlive > 160 && ticksAlive % 10 == 0) {
                float reverseProgress = (ticksAlive - 160) / 140.0f;
                if (reverseProgress < 1f) {
                    double contractReach = threadReach * (1f - reverseProgress);
                    for (int i = 0; i < threads.size(); i++) {
                        double angle = Math.toRadians(threadAngles[i]);
                        Location contractLoc = center.clone().add(Math.cos(angle) * contractReach, 1.0, Math.sin(angle) * contractReach);
                        threads.get(i).entity().teleport(contractLoc);
                        threads.get(i).entity().setInterpolationDuration(5);
                        threads.get(i).entity().setInterpolationDelay(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Unraveling(plugin); }
    }

    // =========================================================================
    // 68. PHANTOM DOPPELGANGER — void copy rushes toward original player
    // =========================================================================
    public static class PhantomDoppelganger extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> doppelBody = new ArrayList<>();
        private Location spawnLoc;

        public PhantomDoppelganger(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_doppelganger", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(12.0); // 6 hearts on contact
            config.setDamageRadius(2.0);
            config.setDurationTicks(240); // max 12 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(3);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 10 blocks away from the player
            spawnLoc = center.clone().add(10, 0, 0);

            // Player-shaped void copy — same sizing as a player, purple-tinted
            BlockDisplayHandle dBody = displayBuilder.spawnBlock(spawnLoc.clone().add(0, 1, 0), Material.PURPLE_STAINED_GLASS);
            dBody.scale(0.6f, 0.9f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            doppelBody.add(dBody);
            spawnedEntities.add(dBody.entity());

            BlockDisplayHandle dHead = displayBuilder.spawnBlock(spawnLoc.clone().add(0, 2.1, 0), Material.PURPLE_STAINED_GLASS);
            dHead.scale(0.6f, 0.6f, 0.6f).glow(80, 0, 160).interpolation(2, 0);
            doppelBody.add(dHead);
            spawnedEntities.add(dHead.entity());

            BlockDisplayHandle dLegL = displayBuilder.spawnBlock(spawnLoc.clone().add(-0.2, 0, 0), Material.PURPLE_CONCRETE);
            dLegL.scale(0.25f, 0.9f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            doppelBody.add(dLegL);
            spawnedEntities.add(dLegL.entity());

            BlockDisplayHandle dLegR = displayBuilder.spawnBlock(spawnLoc.clone().add(0.2, 0, 0), Material.PURPLE_CONCRETE);
            dLegR.scale(0.25f, 0.9f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            doppelBody.add(dLegR);
            spawnedEntities.add(dLegR.entity());

            // Crackling particle materialization
            DisplayBuilder.purpleDust(spawnLoc, 30, 2.0);
            DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || spawnLoc == null) return;

            // Sprint toward the player's current position
            // Move at sprint speed: ~10 blocks/second = 0.5 blocks/tick
            double speedPerTick = 0.45;
            double dx = center.getX() - spawnLoc.getX();
            double dz = center.getZ() - spawnLoc.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 0.5) {
                spawnLoc = spawnLoc.clone().add(
                        (dx / dist) * speedPerTick,
                        0,
                        (dz / dist) * speedPerTick
                );
            }

            // Move doppelganger blocks to new position
            double[][] offsets = {{0, 1, 0}, {0, 2.1, 0}, {-0.2, 0, 0}, {0.2, 0, 0}};
            for (int i = 0; i < doppelBody.size(); i++) {
                Location partLoc = spawnLoc.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                BlockDisplay bd = (BlockDisplay) doppelBody.get(i).entity();
                bd.teleport(partLoc);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Trail particles while charging
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(spawnLoc, 5, 0.5);
            }

            // Crackling escalation near player
            if (dist < 5 && ticksAlive % 10 == 0) {
                DisplayBuilder.crimsonDust(spawnLoc, 10, 1.0);
                DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomDoppelganger(plugin); }
    }

    // =========================================================================
    // 69. GRAVITY INVERSION (PERSONAL) — single player floated 15 blocks up (tracks)
    // =========================================================================
    public static class GravityInversionPersonal extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> auraRing = new ArrayList<>();
        private boolean inverted = false;

        public GravityInversionPersonal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_personal", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 4-7 hearts full fall from 15 blocks
            config.setImpactRadius(2.0);
            config.setDurationTicks(160); // 5s inversion + ramp
            config.setCooldownTicks(700); // 35 seconds
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Upward void-energy aura around the lifting player
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 0.5, Math.sin(angle) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.2f, 0.4f, 0.2f).glow(128, 0, 255).interpolation(3, 0);
                auraRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Brief lightening hint
            DisplayBuilder.cyanDust(center, 20, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Inversion: player floats up 15 blocks over 20 ticks
            if (ticksAlive <= 20 && !inverted) {
                float liftHeight = ticksAlive * 0.75f; // 0 -> 15 blocks

                // Aura rises with player
                for (int i = 0; i < auraRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / auraRing.size();
                    Location auraLoc = center.clone().add(Math.cos(angle) * 0.8, liftHeight + 0.5, Math.sin(angle) * 0.8);
                    BlockDisplay bd = (BlockDisplay) auraRing.get(i).entity();
                    bd.teleport(auraLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                DisplayBuilder.cyanDust(center.clone().add(0, liftHeight, 0), 6, 0.8);
            }

            if (ticksAlive == 20) {
                inverted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.7f, 0.5f);
            }

            // Hold at peak (ticks 20-120), then release
            if (inverted && ticksAlive > 20 && ticksAlive < 120) {
                float holdHeight = 15.0f;
                for (int i = 0; i < auraRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / auraRing.size();
                    Location holdLoc = center.clone().add(Math.cos(angle) * 0.8, holdHeight + 0.5, Math.sin(angle) * 0.8);
                    auraRing.get(i).entity().teleport(holdLoc);
                    auraRing.get(i).entity().setInterpolationDuration(3);
                    auraRing.get(i).entity().setInterpolationDelay(0);
                }
                if (ticksAlive % 10 == 0) DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 5, 1.0);
            }

            // Release at tick 120 — fall impact
            if (ticksAlive == 120) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 40, 3.0);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            DisplayBuilder.crimsonDust(impactLocation, 50, 4.0);
            DisplayBuilder.playSound(impactLocation, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionPersonal(plugin); }
    }

    // =========================================================================
    // 70. ISOLATION COCOON — void-black sphere encases a player briefly
    // =========================================================================
    public static class IsolationCocoon extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> cocoonShell = new ArrayList<>();
        private boolean sealing = false;
        private boolean open = false;

        public IsolationCocoon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("isolation_cocoon", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 6s cocoon + emergence
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Void particles converging toward target (2-second warning)
            DisplayBuilder.purpleDust(center, 30, 6.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.4f);
            sealing = true;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 2-second particle convergence warning
            if (ticksAlive <= 40 && sealing) {
                if (ticksAlive % 5 == 0) DisplayBuilder.purpleDust(center, 6, 4.0 - (ticksAlive * 0.08));
            }

            // Seal at tick 40 — form the obsidian-black cocoon
            if (ticksAlive == 40 && !open) {
                open = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.3f);

                // Sphere of black concrete blocks (2 block radius)
                int[][] sphereOffsets = {
                    {0,2,0}, {0,-2,0}, {2,0,0}, {-2,0,0}, {0,0,2}, {0,0,-2},
                    {1,1,0}, {-1,1,0}, {1,-1,0}, {-1,-1,0},
                    {0,1,1}, {0,1,-1}, {0,-1,1}, {0,-1,-1},
                    {1,0,1}, {1,0,-1}, {-1,0,1}, {-1,0,-1}
                };
                for (int[] off : sphereOffsets) {
                    Location loc = center.clone().add(off[0], off[1] + 1, off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 0, 30).interpolation(5, 0);
                    cocoonShell.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Cocoon holds from tick 40-160 (6 seconds)
            if (open && ticksAlive > 40 && ticksAlive < 160) {
                // Slow gentle rotation on the shell
                if (ticksAlive % 20 == 0) {
                    for (BlockDisplayHandle h : cocoonShell) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.5f, -0.5f, -0.5f),
                                new AxisAngle4f(ticksAlive * 0.01f, 0, 1, 0),
                                new Vector3f(1f, 1f, 1f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(10);
                    }
                }
            }

            // Open at tick 160 — speed burst visual + shell shatters
            if (ticksAlive == 160) {
                DisplayBuilder.cyanDust(center, 60, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.5f);
                // Shell scatter — move all blocks outward
                for (int i = 0; i < cocoonShell.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) cocoonShell.get(i).entity();
                    Location scatter = bd.getLocation().clone().add(
                            (Math.random() - 0.5) * 6,
                            (Math.random() - 0.5) * 6,
                            (Math.random() - 0.5) * 6
                    );
                    bd.teleport(scatter);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IsolationCocoon(plugin); }
    }
}
