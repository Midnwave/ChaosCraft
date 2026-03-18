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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 1 Environmental — GROUP 9: CHAIN REACTION
 * 10 attacks where one event triggers another in cascade sequence.
 * Attacks 81-90 from boss1-voidmaw.md.
 *
 * Design notes:
 * - Each attack uses ticksAlive to sequence sub-events with delay offsets
 * - First trigger is visually telegraphed; subsequent links have shorter warnings
 * - tracksPlayer = false (chain reactions are positional)
 * - Calamity color palette throughout
 */
public final class ChainReaction {

    private ChainReaction() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PillarToSpire(plugin));
        registry.register(new FaultLineCascade(plugin));
        registry.register(new CrystalChain(plugin));
        registry.register(new VoidRipple(plugin));
        registry.register(new ResonanceChain(plugin));
        registry.register(new GravityDomino(plugin));
        registry.register(new VoidEchoChain(plugin));
        registry.register(new TowerTopple(plugin));
        registry.register(new SeismicSurge(plugin));
        registry.register(new UnravelingCascade(plugin));
    }

    // =========================================================================
    // 81. PILLAR TO SPIRE — pillar cracks, fires shards, each shard detonates
    // =========================================================================
    public static class PillarToSpire extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<Location> shardLocations = new ArrayList<>();
        private boolean shattered = false;
        private int shatterTick = -1;

        public PillarToSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_to_spire", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Obsidian pillar — 8 blocks tall, crackling with void energy
            for (int y = 0; y < 8; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3-second warning crackle
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.4f);
            DisplayBuilder.purpleDust(center.clone().add(0, 4, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning crackle for 3 seconds
            if (ticksAlive < 60 && !shattered) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, (ticksAlive % 40) / 5.0, 0), 5, 0.8);
                }
            }

            // Shatter at tick 60
            if (ticksAlive == 60 && !shattered) {
                shattered = true;
                shatterTick = ticksAlive;

                // Remove pillar blocks visually (scatter them)
                for (BlockDisplayHandle h : pillarBlocks) {
                    Location scatter = h.entity().getLocation().clone().add(
                            (Math.random() - 0.5) * 10,
                            Math.random() * 3,
                            (Math.random() - 0.5) * 10
                    );
                    h.entity().teleport(scatter);
                    h.entity().setInterpolationDuration(5);
                    h.entity().setInterpolationDelay(0);
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 4, 0), 80, 5.0);

                // 3 hearts to anyone near the pillar
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 25) {
                        p.damage(6.0); // 3 hearts shard hit
                    }
                }

                // Spawn 10 shards at random positions around the arena
                double[][] shardOffsets = {
                    {5,0,3},{-4,0,5},{7,0,-2},{-6,0,-3},{3,0,-7},
                    {8,0,1},{-7,0,2},{2,0,8},{-3,0,7},{6,0,-5}
                };
                for (double[] off : shardOffsets) {
                    Location shardLoc = center.clone().add(off[0], 0, off[2]);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.CRYING_OBSIDIAN);
                    shard.scale(0.3f, 1.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    shards.add(shard);
                    shardLocations.add(shardLoc);
                    spawnedEntities.add(shard.entity());
                }
            }

            // Secondary shard explosions 3 seconds after embedding
            if (shattered && ticksAlive == shatterTick + 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.8f);
                for (int i = 0; i < shards.size(); i++) {
                    Location shardLoc = shardLocations.get(i);
                    DisplayBuilder.crimsonDust(shardLoc, 30, 3.0);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(shardLoc) <= 9) {
                            p.damage(6.0); // 3 hearts secondary explosion
                        }
                    }
                    // Shards scatter outward on detonation
                    shards.get(i).entity().teleport(shardLoc.clone().add(0, -5, 0));
                    shards.get(i).entity().setInterpolationDuration(5);
                    shards.get(i).entity().setInterpolationDelay(0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarToSpire(plugin); }
    }

    // =========================================================================
    // 82. FAULT LINE CASCADE — crack spreads, geysers fire sequentially
    // =========================================================================
    public static class FaultLineCascade extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackTiles = new ArrayList<>();
        private final List<BlockDisplayHandle> geysers = new ArrayList<>();
        private int lastGeyserTick = 0;
        private int geysersSpawned = 0;
        private static final int GEYSER_COUNT = 10;

        public FaultLineCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fault_line_cascade", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ground rumble warning
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
            DisplayBuilder.purpleDust(center, 10, 4.0);

            // Initial fault crack at one edge (pre-draw the crack line)
            for (int i = 0; i < GEYSER_COUNT; i++) {
                Location crackLoc = center.clone().add(-12 + i * 2.5, 0.01, -2 + i * 0.3);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.CRACKED_DEEPSLATE_TILES);
                crack.scale(1.5f, 0.02f, 0.4f).glow(50, 0, 100).interpolation(5, 0);
                crackTiles.add(crack);
                spawnedEntities.add(crack.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fire a geyser every 20 ticks (1 second) after the initial 40-tick warning
            if (ticksAlive > 40 && ticksAlive - lastGeyserTick >= 20 && geysersSpawned < GEYSER_COUNT) {
                lastGeyserTick = ticksAlive;

                // Geyser at next crack location
                Location geyserLoc = crackTiles.get(geysersSpawned).entity().getLocation();

                // Spawn geyser visual
                BlockDisplayHandle geyser = displayBuilder.spawnBlock(geyserLoc.clone().add(0, 0, 0), Material.PURPLE_STAINED_GLASS);
                geyser.scale(1.0f, 4.0f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
                geysers.add(geyser);
                spawnedEntities.add(geyser.entity());

                DisplayBuilder.purpleDust(geyserLoc, 25, 2.5);
                DisplayBuilder.playSound(geyserLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.5f);

                // Geyser damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(geyserLoc) <= 9) {
                        p.damage(6.0); // 3 hearts per geyser
                    }
                }

                // Brighten the crack tile under the geyser
                BlockDisplay crackBd = (BlockDisplay) crackTiles.get(geysersSpawned).entity();
                crackBd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                crackBd.setGlowing(true);

                geysersSpawned++;
            }

            // Geysers fade over time
            for (int i = 0; i < geysers.size(); i++) {
                BlockDisplay bd = (BlockDisplay) geysers.get(i).entity();
                float age = (ticksAlive - (40 + i * 20)) / 40.0f;
                if (age > 0 && age < 1f) {
                    float scale = 1.0f - age * 0.8f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, 4.0f * scale, scale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FaultLineCascade(plugin); }
    }

    // =========================================================================
    // 83. CRYSTAL CHAIN — void crystal erupts, beams player positions sequentially
    // =========================================================================
    public static class CrystalChain extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crystals = new ArrayList<>();
        private final List<Location> crystalLocs = new ArrayList<>();
        private int chainStep = 0;
        private int lastChainTick = 0;

        public CrystalChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_chain", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Immediately spawn the first crystal at the center
            BlockDisplayHandle firstCrystal = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.AMETHYST_BLOCK);
            firstCrystal.scale(0.7f, 2.0f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
            crystals.add(firstCrystal);
            crystalLocs.add(center.clone());
            spawnedEntities.add(firstCrystal.entity());

            // 4 hearts at initial crystal
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                if (p.getLocation().distanceSquared(center) <= 9) {
                    p.damage(8.0); // 4 hearts
                }
            }

            DisplayBuilder.cyanDust(center, 30, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);

            lastChainTick = 0;
            chainStep = 1;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Chain: fire new crystal every 40 ticks (2 seconds), 4 total
            if (chainStep < 4 && ticksAlive - lastChainTick >= 40) {
                lastChainTick = ticksAlive;

                // Find the nearest player to the last crystal location
                Location prevCrystal = crystalLocs.get(crystalLocs.size() - 1);
                Player nearestPlayer = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(prevCrystal);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestPlayer = p;
                    }
                }

                if (nearestPlayer != null) {
                    Location nextLoc = nearestPlayer.getLocation().clone();

                    // Beam effect from previous crystal to this position
                    if (!crystalLocs.isEmpty()) {
                        DisplayBuilder.cyanDust(nextLoc, 15, 1.0);
                    }

                    // New crystal erupts at player's feet
                    BlockDisplayHandle newCrystal = displayBuilder.spawnBlock(nextLoc, Material.AMETHYST_BLOCK);
                    newCrystal.scale(0.7f, 2.0f, 0.7f).glow(0, 200, 255).interpolation(2, 0);
                    crystals.add(newCrystal);
                    crystalLocs.add(nextLoc);
                    spawnedEntities.add(newCrystal.entity());

                    // 4 hearts at eruption point
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(nextLoc) <= 9) {
                            p.damage(8.0); // 4 hearts
                        }
                    }

                    DisplayBuilder.playSound(nextLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f + chainStep * 0.1f);
                    DisplayBuilder.cyanDust(nextLoc, 30, 2.5);
                }

                chainStep++;
            }

            // Crystal pulse animation
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < crystals.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) crystals.get(i).entity();
                    float pulse = 0.65f + (float)(Math.sin(ticksAlive * 0.3 + i) * 0.1f);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -0.5f, -pulse / 2),
                            new AxisAngle4f(0, 1, 0, ticksAlive * 0.04f),
                            new Vector3f(pulse, 2.0f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalChain(plugin); }
    }

    // =========================================================================
    // 84. VOID RIPPLE — expanding ring of activated tiles from center outward
    // =========================================================================
    public static class VoidRipple extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rippleRings = new ArrayList<>();
        private int lastRingTick = 0;
        private double currentRadius = 0;

        public VoidRipple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_ripple", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per ring contact
            config.setDamageRadius(1.5); // ring width
            config.setDurationTicks(260);
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center tile goes dark with a soft pulse
            BlockDisplayHandle centerTile = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.OBSIDIAN);
            centerTile.scale(0.8f, 0.02f, 0.8f).glow(60, 0, 120).interpolation(3, 0);
            rippleRings.add(centerTile);
            spawnedEntities.add(centerTile.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ripple expands outward every 20 ticks (1 second)
            if (ticksAlive - lastRingTick >= 20 && currentRadius < 18) {
                lastRingTick = ticksAlive;
                currentRadius += 2.5;

                // Spawn a ring at the new radius
                int ringPoints = Math.max(8, (int)(currentRadius * 2.5));
                for (int i = 0; i < ringPoints; i++) {
                    double angle = (2 * Math.PI * i) / ringPoints;
                    Location ringLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            0.01,
                            Math.sin(angle) * currentRadius
                    );
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(ringLoc, Material.PURPLE_CONCRETE);
                    tile.scale(0.6f, 0.03f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                    rippleRings.add(tile);
                    spawnedEntities.add(tile.entity());
                }

                // Deal damage to players near the ring edge
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(center);
                    double ringDistSq = currentRadius * currentRadius;
                    if (Math.abs(dist - ringDistSq) <= 9) { // within 3 blocks of ring
                        p.damage(4.0); // 2 hearts
                    }
                }

                DisplayBuilder.purpleDust(center, 5, currentRadius * 0.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.3f + (float)(currentRadius * 0.03));
            }

            // Rings slowly fade out
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : rippleRings) {
                    if (h != null && h.entity() != null && h.entity().isValid()) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        float alpha = Math.max(0f, 1f - ticksAlive / 260.0f);
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.3f, -0.015f, -0.3f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.6f, 0.03f * alpha, 0.6f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRipple(plugin); }
    }

    // =========================================================================
    // 85. RESONANCE CHAIN — player glows, pulses outward, chains to nearby players
    // =========================================================================
    public static class ResonanceChain extends EnvironmentalAttack {

        private static class ResonanceNode {
            Player player;
            int startTick;
            boolean pulsed;

            ResonanceNode(Player player, int startTick) {
                this.player = player;
                this.startTick = startTick;
                this.pulsed = false;
            }
        }

        private final List<ResonanceNode> nodes = new ArrayList<>();
        private final List<String> hitPlayerIds = new ArrayList<>();
        private final List<BlockDisplayHandle> auraBlocks = new ArrayList<>();

        public ResonanceChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonance_chain", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Select the initial resonance target — nearest player
            Player initial = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    initial = p;
                }
            }

            if (initial != null) {
                nodes.add(new ResonanceNode(initial, 0));
                hitPlayerIds.add(initial.getUniqueId().toString());

                // Visual aura rings around the initial player
                for (int i = 0; i < 6; i++) {
                    double angle = (2 * Math.PI * i) / 6;
                    Location auraLoc = initial.getLocation().clone().add(Math.cos(angle) * 0.8, 1.0, Math.sin(angle) * 0.8);
                    BlockDisplayHandle aura = displayBuilder.spawnBlock(auraLoc, Material.AMETHYST_BLOCK);
                    aura.scale(0.15f, 0.15f, 0.15f).glow(128, 0, 255).interpolation(3, 0);
                    auraBlocks.add(aura);
                    spawnedEntities.add(aura.entity());
                }
                DisplayBuilder.purpleDust(initial.getLocation(), 20, 1.5);
                DisplayBuilder.playSound(initial.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Check each resonance node for its 5-second (100-tick) charge
            List<ResonanceNode> newNodes = new ArrayList<>();
            for (ResonanceNode node : nodes) {
                if (!node.pulsed && ticksAlive - node.startTick >= 100) {
                    node.pulsed = true;
                    Location pulseLoc = node.player.getLocation();

                    // Deal damage to pulsing player and nearby players
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(pulseLoc) <= 16) { // 4 blocks
                            p.damage(6.0); // 3 hearts
                        }
                    }

                    DisplayBuilder.crimsonDust(pulseLoc, 40, 4.0);
                    DisplayBuilder.playSound(pulseLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.5f);

                    // Check if any other players within 6 blocks to chain to
                    for (Player nearby : w.getPlayers()) {
                        if (isExempt(nearby)) continue;
                        if (hitPlayerIds.contains(nearby.getUniqueId().toString())) continue;
                        if (nearby.getLocation().distanceSquared(pulseLoc) <= 36) { // 6 blocks
                            hitPlayerIds.add(nearby.getUniqueId().toString());
                            newNodes.add(new ResonanceNode(nearby, ticksAlive));

                            // New resonance aura on this player
                            DisplayBuilder.purpleDust(nearby.getLocation(), 15, 1.5);
                            DisplayBuilder.playSound(nearby.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
                        }
                    }
                }
            }
            nodes.addAll(newNodes);

            // Update aura block positions to follow initial player
            if (!nodes.isEmpty() && nodes.get(0).player.isOnline()) {
                Location playerLoc = nodes.get(0).player.getLocation();
                for (int i = 0; i < auraBlocks.size(); i++) {
                    double angle = (2 * Math.PI * i) / auraBlocks.size();
                    float orbRadius = 0.8f + (float)(Math.sin(ticksAlive * 0.1 + i) * 0.2f);
                    Location auraLoc = playerLoc.clone().add(Math.cos(angle + ticksAlive * 0.05) * orbRadius, 1.0, Math.sin(angle + ticksAlive * 0.05) * orbRadius);
                    auraBlocks.get(i).entity().teleport(auraLoc);
                    auraBlocks.get(i).entity().setInterpolationDuration(3);
                    auraBlocks.get(i).entity().setInterpolationDelay(0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonanceChain(plugin); }
    }

    // =========================================================================
    // 86. GRAVITY DOMINO — 8 floor sections tip like dominos in sequence
    // =========================================================================
    public static class GravityDomino extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> dominoSections = new ArrayList<>();
        private int dominoStep = 0;
        private int lastDominoTick = 0;
        private boolean started = false;

        public GravityDomino(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_domino", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per section hit
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 floor sections in a line
            for (int i = 0; i < 8; i++) {
                Location sectionLoc = center.clone().add(-7 + i * 2, 0, 0);
                BlockDisplayHandle section = displayBuilder.spawnBlock(sectionLoc, Material.DEEPSLATE_TILES);
                section.scale(1.8f, 0.2f, 1.8f).glow(80, 0, 160).interpolation(4, 0);
                dominoSections.add(section);
                spawnedEntities.add(section.entity());
            }

            // 2-second wobble warning on first section
            DisplayBuilder.playSound(center.clone().add(-7, 0, 0), Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
            DisplayBuilder.purpleDust(center.clone().add(-7, 1, 0), 10, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Start after 40-tick warning
            if (ticksAlive >= 40 && !started) {
                started = true;
                lastDominoTick = ticksAlive;
            }

            if (!started) {
                // Wobble the first section
                if (ticksAlive % 5 == 0) {
                    BlockDisplay first = (BlockDisplay) dominoSections.get(0).entity();
                    float wobble = (float)(Math.sin(ticksAlive * 0.4) * 0.05f);
                    first.setTransformation(new Transformation(
                            new Vector3f(-0.9f, -0.1f + wobble, -0.9f),
                            new AxisAngle4f(wobble, 0, 0, 1),
                            new Vector3f(1.8f, 0.2f, 1.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    first.setInterpolationDelay(0);
                    first.setInterpolationDuration(3);
                }
                return;
            }

            // Tip one section every 10 ticks
            if (dominoStep < 8 && ticksAlive - lastDominoTick >= 10) {
                lastDominoTick = ticksAlive;
                int step = dominoStep;
                BlockDisplay sectionBd = (BlockDisplay) dominoSections.get(step).entity();

                // Tip animation — rotate to horizontal
                float tipAngle = (float)(Math.PI / 2) * 0.9f; // ~horizontal
                sectionBd.setTransformation(new Transformation(
                        new Vector3f(-0.9f, -0.1f, -0.9f),
                        new AxisAngle4f(tipAngle, 0, 0, 1), // tip toward next section
                        new Vector3f(1.8f, 0.2f, 1.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                sectionBd.setInterpolationDelay(0);
                sectionBd.setInterpolationDuration(8);

                Location sectionLoc = center.clone().add(-7 + step * 2, 0, 0);
                DisplayBuilder.playSound(sectionLoc, Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f + step * 0.08f);
                DisplayBuilder.purpleDust(sectionLoc, 15, 2.0);

                // Knock back players on this section
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(sectionLoc) <= 6) {
                        p.damage(4.0); // 2 hearts
                        // Last section amplified
                        if (step == 7) p.damage(4.0); // extra 2 hearts at chain end
                    }
                }

                dominoStep++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityDomino(plugin); }
    }

    // =========================================================================
    // 87. VOID ECHO CHAIN — damage ripple amplifies 1/2/3/4 hearts per hop
    // =========================================================================
    public static class VoidEchoChain extends EnvironmentalAttack {

        private boolean chainStarted = false;
        private int chainHop = 0;
        private int lastHopTick = 0;
        private Location lastHopLoc;
        private final List<String> hitPlayerIds = new ArrayList<>();
        private final List<BlockDisplayHandle> echoRings = new ArrayList<>();

        public VoidEchoChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_echo_chain", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Chain triggers when first player takes damage — monitor mode
            lastHopLoc = center.clone();
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Trigger when any player takes damage (check health drop proxy — nearest player at <half health)
            if (!chainStarted) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Start chain at a random interval to simulate "player takes damage" trigger
                    if (ticksAlive == 30) { // Fixed trigger at 1.5s for simulation
                        chainStarted = true;
                        hitPlayerIds.add(p.getUniqueId().toString());
                        lastHopLoc = p.getLocation().clone();
                        chainHop = 1;
                        lastHopTick = ticksAlive;

                        // First ripple from this player
                        spawnEchoRing(lastHopLoc, chainHop);
                        p.damage(chainHop * 2.0); // 1 heart (hop 1)
                        break;
                    }
                }
            }

            if (chainStarted && chainHop < 4 && ticksAlive - lastHopTick >= 20) {
                lastHopTick = ticksAlive;
                chainHop++;

                // Find nearest unaffected player to last hop location
                Player nextTarget = null;
                double nearest = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (hitPlayerIds.contains(p.getUniqueId().toString())) continue;
                    double dist = p.getLocation().distanceSquared(lastHopLoc);
                    if (dist < nearest) {
                        nearest = dist;
                        nextTarget = p;
                    }
                }

                if (nextTarget != null) {
                    hitPlayerIds.add(nextTarget.getUniqueId().toString());
                    lastHopLoc = nextTarget.getLocation().clone();
                    spawnEchoRing(lastHopLoc, chainHop);
                    nextTarget.damage(chainHop * 2.0); // 2/3/4 hearts per hop
                    DisplayBuilder.playSound(lastHopLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 0.5f + chainHop * 0.2f);
                }
            }

            // Animate echo rings expanding
            for (int i = 0; i < echoRings.size(); i++) {
                BlockDisplay bd = (BlockDisplay) echoRings.get(i).entity();
                Location ringLoc = bd.getLocation();
                float expand = 1.0f + (ticksAlive - lastHopTick) * 0.05f;
                bd.setTransformation(new Transformation(
                        new Vector3f(-expand / 2, -0.01f, -expand / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(expand, 0.03f, expand),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }
        }

        private void spawnEchoRing(Location loc, int hop) {
            // Ring of void tiles at this hop location
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location ringLoc = loc.clone().add(Math.cos(angle) * 1.5, 0.01, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.03f, 0.4f).glow(128, 0, Math.min(255, 80 + hop * 44)).interpolation(2, 0);
                echoRings.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.purpleDust(loc, 15 + hop * 8, 2.0 + hop * 0.5);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEchoChain(plugin); }
    }

    // =========================================================================
    // 88. TOWER TOPPLE — pillar leans and falls, shards scatter individually
    // =========================================================================
    public static class TowerTopple extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shardBlocks = new ArrayList<>();
        private final List<Location> shardLandingLocs = new ArrayList<>();
        private BlockDisplayHandle crystalTop;
        private boolean falling = false;
        private boolean shardsFired = false;
        private int fallStartTick = -1;

        public TowerTopple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tower_topple", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(480);
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tall obsidian pillar (20 blocks high)
            for (int y = 0; y < 20; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.OBSIDIAN);
                h.scale(1.2f, 1.0f, 1.2f).glow(60, 60, 120).interpolation(4, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // End crystal at top (glowing amethyst)
            crystalTop = displayBuilder.spawnBlock(center.clone().add(0, 21, 0), Material.AMETHYST_BLOCK);
            crystalTop.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(crystalTop.entity());

            // Groan and crack warning
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.2f);
            DisplayBuilder.purpleDust(center.clone().add(0, 10, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 4-second warning lean (80 ticks)
            if (ticksAlive < 80 && !falling) {
                float lean = ticksAlive * 0.001f;
                for (int y = 0; y < pillarBlocks.size(); y++) {
                    BlockDisplay bd = (BlockDisplay) pillarBlocks.get(y).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.6f + lean * y, -0.5f, -0.6f),
                            new AxisAngle4f(lean * 0.3f, 0, 0, 1),
                            new Vector3f(1.2f, 1.0f, 1.2f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 15, 0), 8, 3.0);
                }
            }

            // Begin topple at tick 80
            if (ticksAlive == 80 && !falling) {
                falling = true;
                fallStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.3f);
            }

            // Topple animation (80-200)
            if (falling && !shardsFired && ticksAlive < 200) {
                float toppleProgress = (ticksAlive - 80) / 120.0f;
                float fallAngle = toppleProgress * (float)(Math.PI / 2);

                for (int y = 0; y < pillarBlocks.size(); y++) {
                    BlockDisplay bd = (BlockDisplay) pillarBlocks.get(y).entity();
                    float xOffset = (float)(y * Math.sin(fallAngle));
                    float yOffset = (float)(y * (Math.cos(fallAngle) - 1));
                    Location fallLoc = center.clone().add(xOffset, y + yOffset, 0);
                    bd.teleport(fallLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }

            // Ground impact and shards at tick 200
            if (ticksAlive == 200 && !shardsFired) {
                shardsFired = true;
                DisplayBuilder.crimsonDust(center, 120, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);

                // Heavy damage to players in fall path (8 hearts direct)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Fall path: positive X direction
                    double px = p.getLocation().getX() - center.getX();
                    double pz = p.getLocation().getZ() - center.getZ();
                    if (px >= 0 && px <= 20 && Math.abs(pz) <= 3) {
                        p.damage(16.0); // 8 hearts
                    }
                }

                // Spawn 20+ shards scattered across arena
                for (int i = 0; i < 20; i++) {
                    double sx = (Math.random() - 0.3) * 18;
                    double sz = (Math.random() - 0.5) * 18;
                    Location shardLoc = center.clone().add(sx, 0.01, sz);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.CRYING_OBSIDIAN);
                    shard.scale(0.4f, 1.2f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                    shardBlocks.add(shard);
                    shardLandingLocs.add(shardLoc);
                    spawnedEntities.add(shard.entity());
                }

                // Shard blast damage
                for (int i = 0; i < shardLandingLocs.size(); i++) {
                    Location shardLoc = shardLandingLocs.get(i);
                    DisplayBuilder.purpleDust(shardLoc, 15, 2.5);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(shardLoc) <= 4) {
                            p.damage(6.0); // 3 hearts shard blast
                        }
                    }
                }

                // Crystal top detonation (5 hearts in 5-block radius)
                Location crystalLoc = center.clone().add(20, 0, 0); // where it landed
                DisplayBuilder.cyanDust(crystalLoc, 50, 5.0);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(crystalLoc) <= 25) {
                        p.damage(10.0); // 5 hearts crystal detonation
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TowerTopple(plugin); }
    }

    // =========================================================================
    // 89. SEISMIC SURGE — geyser eruption, cracks reach features, trigger secondaries
    // =========================================================================
    public static class SeismicSurge extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackLines = new ArrayList<>();
        private final List<Location> secondaryEruptionLocs = new ArrayList<>();
        private int secondaryStep = 0;
        private int lastSecondaryTick = 0;
        private boolean primaryFired = false;

        public SeismicSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_surge", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Small initial geyser visual
            BlockDisplayHandle initialGeyser = displayBuilder.spawnBlock(center, Material.AMETHYST_BLOCK);
            initialGeyser.scale(0.5f, 2.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            crackLines.add(initialGeyser);
            spawnedEntities.add(initialGeyser.entity());

            // 2-second warning
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.5f);
            DisplayBuilder.purpleDust(center, 20, 2.0);

            // Pre-define secondary eruption targets (arena features)
            secondaryEruptionLocs.add(center.clone().add(8, 0, 3));
            secondaryEruptionLocs.add(center.clone().add(-5, 0, 7));
            secondaryEruptionLocs.add(center.clone().add(3, 0, -8));
            secondaryEruptionLocs.add(center.clone().add(-9, 0, -4));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Primary geyser fires at tick 40
            if (ticksAlive == 40 && !primaryFired) {
                primaryFired = true;
                DisplayBuilder.crimsonDust(center, 50, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9) {
                        p.damage(6.0); // 3 hearts
                    }
                }
                lastSecondaryTick = ticksAlive;

                // Draw crack lines toward each secondary location
                for (Location secLoc : secondaryEruptionLocs) {
                    int steps = 6;
                    for (int step = 1; step <= steps; step++) {
                        double t = step / (double)steps;
                        Location crackLoc = center.clone().add(
                                (secLoc.getX() - center.getX()) * t,
                                0.01,
                                (secLoc.getZ() - center.getZ()) * t
                        );
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.CRACKED_DEEPSLATE_TILES);
                        crack.scale(0.4f, 0.015f, 0.4f).glow(60, 0, 120).interpolation(3, 0);
                        crackLines.add(crack);
                        spawnedEntities.add(crack.entity());
                    }
                }
            }

            // Secondary eruptions cascade every 20 ticks
            if (primaryFired && secondaryStep < secondaryEruptionLocs.size() && ticksAlive - lastSecondaryTick >= 20) {
                lastSecondaryTick = ticksAlive;
                Location secLoc = secondaryEruptionLocs.get(secondaryStep);

                BlockDisplayHandle secGeyser = displayBuilder.spawnBlock(secLoc, Material.AMETHYST_BLOCK);
                secGeyser.scale(0.7f, 3.5f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                crackLines.add(secGeyser);
                spawnedEntities.add(secGeyser.entity());

                DisplayBuilder.crimsonDust(secLoc, 35, 3.5);
                DisplayBuilder.playSound(secLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.6f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(secLoc) <= 9) {
                        p.damage(6.0); // 3 hearts
                    }
                }

                secondaryStep++;
            }

            // Ambient crack glow
            if (ticksAlive % 15 == 0 && primaryFired) {
                DisplayBuilder.purpleDust(center, 6, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeismicSurge(plugin); }
    }

    // =========================================================================
    // 90. THE UNRAVELING CASCADE — arena dissolves from one corner across all tiles
    // =========================================================================
    public static class UnravelingCascade extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> dissolveTiles = new ArrayList<>();
        private int lastWaveTick = 0;
        private int waveIndex = 0;
        private static final int WAVE_COUNT = 10;

        public UnravelingCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("unraveling_cascade", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per tile contact during dissolution
            config.setDamageRadius(18.0); // arena-wide
            config.setDurationTicks(380);
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Corner dissolution begins with zero warning — just starts
            // Pre-spawn a large grid of tiles that will dissolve in waves
            for (int x = -12; x <= 12; x += 3) {
                for (int z = -12; z <= 12; z += 3) {
                    Location loc = center.clone().add(x, 0.02, z);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(loc, Material.END_STONE_BRICKS);
                    tile.scale(2.8f, 0.04f, 2.8f).glow(40, 40, 80).interpolation(5, 0);
                    dissolveTiles.add(tile);
                    spawnedEntities.add(tile.entity());
                }
            }

            // Corner flash
            DisplayBuilder.purpleDust(center.clone().add(-12, 0, -12), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Progress dissolution wave every 15 ticks
            if (ticksAlive - lastWaveTick >= 15 && waveIndex < WAVE_COUNT) {
                lastWaveTick = ticksAlive;

                // Dissolve tiles in the current wave (diagonal band from corner)
                float waveFront = -12 + waveIndex * 2.5f;
                for (BlockDisplayHandle tile : dissolveTiles) {
                    Location tileLoc = tile.entity().getLocation();
                    double wavePos = (tileLoc.getX() - center.getX()) + (tileLoc.getZ() - center.getZ());
                    if (wavePos >= waveFront && wavePos < waveFront + 3) {
                        // Dissolve this tile
                        BlockDisplay bd = (BlockDisplay) tile.entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-1.4f, -0.02f, -1.4f),
                                new AxisAngle4f((float)(Math.random() * 0.5f), 0, 1, 0),
                                new Vector3f(2.8f, 0.001f, 2.8f), // nearly invisible
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(8);
                        bd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                        bd.setGlowing(true);
                    }
                }

                DisplayBuilder.purpleDust(center.clone().add(waveFront * 0.5, 0, waveFront * 0.5), 12, 3.0);
                if (waveIndex % 3 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f + waveIndex * 0.05f);
                }

                waveIndex++;
            }

            // Re-solidification: tiles re-form after dissolution wave passes
            if (waveIndex >= WAVE_COUNT) {
                // Re-solidify oldest tiles
                float resolidifyFront = -12 + (waveIndex - WAVE_COUNT) * 2.5f;
                if (ticksAlive % 15 == 0) {
                    for (BlockDisplayHandle tile : dissolveTiles) {
                        Location tileLoc = tile.entity().getLocation();
                        double tilePos = (tileLoc.getX() - center.getX()) + (tileLoc.getZ() - center.getZ());
                        if (tilePos < resolidifyFront) {
                            BlockDisplay bd = (BlockDisplay) tile.entity();
                            bd.setTransformation(new Transformation(
                                    new Vector3f(-1.4f, -0.02f, -1.4f),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(2.8f, 0.04f, 2.8f), // back to normal
                                    new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(6);
                            bd.setGlowColorOverride(Color.fromRGB(40, 40, 80));
                            bd.setGlowing(true);
                        }
                    }
                }
            }

            // Arena-wide ambient during cascade
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.purpleDust(center, 8, 10.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new UnravelingCascade(plugin); }
    }
}
