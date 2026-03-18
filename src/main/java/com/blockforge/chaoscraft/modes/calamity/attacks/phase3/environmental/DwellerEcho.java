package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

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

/**
 * Phase 3 Environmental -- GROUP 7: DWELLER ECHO EVENTS
 * 10 attacks (#61-70) where the environment mimics the Dweller.
 * Phantom footsteps, echo screams, shadow impressions, residual memories.
 * The island has become an extension of the Dweller itself.
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Echo/phantom materials: BLACKSTONE, OBSIDIAN, SOUL_SOIL, CRYING_OBSIDIAN
 * - Psychological horror through environmental mimicry
 */
public final class DwellerEcho {

    private DwellerEcho() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PhantomStep(plugin));
        registry.register(new EchoScream(plugin));
        registry.register(new BurnedImpression(plugin));
        registry.register(new MimicryFootfall(plugin));
        registry.register(new TheOtherVoice(plugin));
        registry.register(new EchoTrail(plugin));
        registry.register(new ScreamInTheStone(plugin));
        registry.register(new ResidualMemory(plugin));
        registry.register(new ShadowCrawl(plugin));
        registry.register(new TheIslandSpeaks(plugin));
    }

    // =========================================================================
    // 61. PHANTOM STEP -- invisible footsteps walk toward nearest player
    // =========================================================================
    public static class PhantomStep extends EnvironmentalAttack {

        private Player targetPlayer;
        private Location currentStepPos;
        private int stepCount = 0;
        private double stepAngle;

        public PhantomStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_step", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(260); // 13 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            currentStepPos = center.clone();

            // Find nearest player
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    targetPlayer = p;
                }
            }

            if (targetPlayer != null) {
                double dx = targetPlayer.getLocation().getX() - center.getX();
                double dz = targetPlayer.getLocation().getZ() - center.getZ();
                stepAngle = Math.atan2(dz, dx);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Steps every ~12 ticks (biped gait at walking speed)
            if (ticksAlive % 12 == 0 && stepCount < 12) {
                stepCount++;

                // Recalculate angle toward current player position
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    double dx = targetPlayer.getLocation().getX() - currentStepPos.getX();
                    double dz = targetPlayer.getLocation().getZ() - currentStepPos.getZ();
                    stepAngle = Math.atan2(dz, dx);
                }

                // Advance step position
                double strideLength = 1.2;
                // Alternate left/right offset for biped gait
                double perpOffset = (stepCount % 2 == 0) ? 0.3 : -0.3;
                double perpAngle = stepAngle + Math.PI / 2;
                currentStepPos = currentStepPos.clone().add(
                        Math.cos(stepAngle) * strideLength + Math.cos(perpAngle) * perpOffset,
                        0,
                        Math.sin(stepAngle) * strideLength + Math.sin(perpAngle) * perpOffset);

                // Each step sound
                DisplayBuilder.playSound(currentStepPos, Sound.BLOCK_STONE_PLACE, 0.7f, 0.3f);

                // Step impression display
                BlockDisplayHandle h = displayBuilder.spawnBlock(currentStepPos, Material.SOUL_SOIL);
                h.scale(0.6f, 0.03f, 0.9f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());

                // Soul fire pulse at step
                DisplayBuilder.dustParticles(currentStepPos, 8, 0.3, 0, 150, 255, 0.8f);

                // Ash ring spreading outward
                DisplayBuilder.dustParticles(currentStepPos, 5, 0.8, 120, 115, 110, 0.5f);

                // Fade step after 30 ticks
                BlockDisplay stepEntity = h.entity();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (stepEntity.isValid()) stepEntity.remove();
                }, 30L);

                // Final contact step -- damage
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    if (currentStepPos.distanceSquared(targetPlayer.getLocation()) <= 2.25) {
                        targetPlayer.damage(16.0); // 8 hearts
                        DisplayBuilder.crimsonDust(targetPlayer.getLocation(), 15, 1.0);
                        DisplayBuilder.playSound(currentStepPos, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomStep(plugin); }
    }

    // =========================================================================
    // 62. ECHO SCREAM -- expanding rings of damage from center
    // =========================================================================
    public static class EchoScream extends EnvironmentalAttack {

        private final double[] ringRadii = {6.0, 12.0, 18.0, 24.0};
        private final double[] ringProgress = {0, 0, 0, 0};
        private final double[] ringSpeeds = {0.15, 0.2, 0.3, 0.45}; // blocks per tick
        private boolean screamed = false;

        public EchoScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_scream", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Overlapping scream warning
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 1.0f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f), 10L);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1 second)
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 2.0, 5, 240, 250, 0.8f);
                }
                return;
            }

            if (!screamed) {
                screamed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.6f);
            }

            // Expand rings
            for (int i = 0; i < 4; i++) {
                ringProgress[i] += ringSpeeds[i];
                double currentRadius = ringProgress[i];

                if (currentRadius > ringRadii[i]) continue; // Ring has reached max

                // Draw ring particles
                if (ticksAlive % 2 == 0) {
                    int points = Math.max(8, (int) (currentRadius * 3));
                    for (int p = 0; p < points; p++) {
                        double angle = (2 * Math.PI * p) / points;
                        Location ringLoc = center.clone().add(
                                Math.cos(angle) * currentRadius, 0.5, Math.sin(angle) * currentRadius);
                        DisplayBuilder.dustParticles(ringLoc, 2, 0.1, 5, 240, 250, 1.0f);
                    }
                }

                // Check player hits at ring front
                if (ticksAlive % 3 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center);
                        if (Math.abs(dist - currentRadius) < 1.5) {
                            p.damage(16.0); // 8 hearts per ring
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.3, 255, 60, 0, 0.8f);
                        }
                    }
                }
            }

            // Crimson spore rain at mid-height
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 20;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 3 + Math.random() * 2, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 190, 25, 15, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoScream(plugin); }
    }

    // =========================================================================
    // 63. BURNED IMPRESSION -- Dweller silhouette burned into surface
    // =========================================================================
    public static class BurnedImpression extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> impressionHandles = new ArrayList<>();
        private Location impressionLoc;
        private double walkAngle;
        private boolean steppedOut = false;
        private int walkStep = 0;

        public BurnedImpression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burned_impression", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(320); // 16 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pick impression location at arena boundary
            double angle = Math.random() * 2 * Math.PI;
            impressionLoc = center.clone().add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12);
            walkAngle = angle + Math.PI; // Walk inward

            DisplayBuilder.playSound(impressionLoc, Sound.ENTITY_BLAZE_AMBIENT, 1.8f, 0.7f);

            // Smoke warning
            w.spawnParticle(Particle.SMOKE, impressionLoc.clone().add(0, 1.5, 0), 20, 0.5, 1.0, 0.5, 0.02);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- smoke rises
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.SMOKE, impressionLoc.clone().add(0, 1.5, 0), 8, 0.4, 0.8, 0.4, 0.01);
                }
                return;
            }

            // Burn impression into surface
            if (ticksAlive == 41) {
                DisplayBuilder.playSound(impressionLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);

                // Build Dweller silhouette from glass panes -- simplified humanoid form
                // Body column
                for (int y = 0; y < 4; y++) {
                    Location partLoc = impressionLoc.clone().add(0, y * 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(partLoc, Material.OBSIDIAN);
                    float bodyScale = (y == 0 || y == 3) ? 0.6f : 0.8f;
                    h.scale(bodyScale, 0.5f, 0.15f).glow(200, 0, 50).interpolation(3, 0);
                    impressionHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Arms
                for (int side = -1; side <= 1; side += 2) {
                    Location armLoc = impressionLoc.clone().add(side * 0.6, 1.0, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(armLoc, Material.OBSIDIAN);
                    h.scale(0.3f, 0.8f, 0.12f).glow(0, 150, 255).interpolation(3, 0);
                    impressionHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Soul fire outline
                for (BlockDisplayHandle h : impressionHandles) {
                    DisplayBuilder.dustParticles(h.entity().getLocation(), 4, 0.2, 0, 230, 220, 0.8f);
                }

                // Flame pulse at edges
                DisplayBuilder.dustParticles(impressionLoc, 10, 1.0, 255, 110, 0, 0.6f);
            }

            // Step-out animation at tick 60
            if (ticksAlive > 60 && !steppedOut) {
                steppedOut = true;
                DisplayBuilder.playSound(impressionLoc, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
            }

            // Shadow walks 5 steps
            if (steppedOut && ticksAlive % 12 == 0 && walkStep < 5) {
                walkStep++;
                Location walkPos = impressionLoc.clone().add(
                        Math.cos(walkAngle) * walkStep * 1.2, 0, Math.sin(walkAngle) * walkStep * 1.2);

                // Move all impression displays
                for (int i = 0; i < impressionHandles.size(); i++) {
                    BlockDisplayHandle h = impressionHandles.get(i);
                    BlockDisplay bd = h.entity();
                    Location newLoc = bd.getLocation().clone().add(
                            Math.cos(walkAngle) * 1.2, 0, Math.sin(walkAngle) * 1.2);
                    bd.teleport(newLoc);
                }

                // Fade effect on walk
                float fadeAlpha = 1.0f - (walkStep * 0.15f);
                for (BlockDisplayHandle h : impressionHandles) {
                    float scale = Math.max(0.1f, 0.8f * fadeAlpha);
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-scale / 2, -0.25f, -0.05f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, 0.5f, 0.12f * fadeAlpha),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }

                // Contact damage check
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(walkPos) <= 4.0) {
                        p.damage(18.0); // 9 hearts
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }

            // Dissolve at end
            if (ticksAlive > 120) {
                float dissolve = (ticksAlive - 120) / 20.0f;
                for (BlockDisplayHandle h : impressionHandles) {
                    Location loc = h.entity().getLocation();
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 0, 150, 255, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BurnedImpression(plugin); }
    }

    // =========================================================================
    // 64. MIMICRY FOOTFALL -- player's footsteps reproduced 1s behind in lava
    // =========================================================================
    public static class MimicryFootfall extends EnvironmentalAttack {

        private final List<Location> recordedPositions = new ArrayList<>();
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private Player trackedPlayer;
        private boolean mimicActive = false;

        public MimicryFootfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mimicry_footfall", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Track nearest player
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    trackedPlayer = p;
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.8f);

            // Warning: subtle crimson trail at player's feet
            if (trackedPlayer != null) {
                DisplayBuilder.dustParticles(trackedPlayer.getLocation(), 5, 0.3, 170, 20, 10, 0.5f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (trackedPlayer == null || !trackedPlayer.isOnline()) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(trackedPlayer.getLocation(), 3, 0.2, 170, 20, 10, 0.4f);
                }
                // Record positions during warning too
                recordedPositions.add(trackedPlayer.getLocation().clone());
                return;
            }

            if (!mimicActive) {
                mimicActive = true;
            }

            // Record player position every tick
            recordedPositions.add(trackedPlayer.getLocation().clone());

            // Replay positions from 20 ticks (1 second) ago
            int replayIndex = recordedPositions.size() - 20;
            if (replayIndex >= 0 && replayIndex < recordedPositions.size()) {
                Location replayLoc = recordedPositions.get(replayIndex);

                // Only spawn display every 4 ticks for performance
                if (ticksAlive % 4 == 0) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(replayLoc, Material.MAGMA_BLOCK);
                    h.scale(0.5f, 0.05f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                    trailHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Lava particles
                    w.spawnParticle(Particle.LAVA, replayLoc.clone().add(0, 0.3, 0), 3, 0.1, 0.2, 0.1, 0);
                    DisplayBuilder.dustParticles(replayLoc, 4, 0.2, 255, 90, 10, 0.7f);

                    // Remove old trail after 80 ticks (4 seconds)
                    BlockDisplay trailEntity = h.entity();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (trailEntity.isValid()) trailEntity.remove();
                    }, 80L);
                }

                // Damage players who step back into their trail
                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(replayLoc) <= 1.0) {
                            p.damage(16.0); // 8 hearts
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.3, 255, 90, 10, 1.0f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MimicryFootfall(plugin); }
    }

    // =========================================================================
    // 65. THE OTHER VOICE -- phantom presence behind player, misdirection
    // =========================================================================
    public static class TheOtherVoice extends EnvironmentalAttack {

        private Player targetPlayer;
        private Location phantomPos;

        public TheOtherVoice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_other_voice", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(240); // 12 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    targetPlayer = p;
                }
            }

            if (targetPlayer == null) return;

            // Phantom position 4 blocks behind player's facing
            Location pl = targetPlayer.getLocation();
            double facingRad = Math.toRadians(pl.getYaw());
            // Behind = opposite of facing direction
            phantomPos = pl.clone().add(Math.sin(facingRad) * 4, 0, -Math.cos(facingRad) * 4);

            // Sound from behind
            DisplayBuilder.playSound(phantomPos, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (targetPlayer == null || phantomPos == null) return;

            // 20 tick delay (1 second warning)
            if (ticksAlive <= 20) {
                return;
            }

            // Phantom visual cluster
            if (ticksAlive % 5 == 0) {
                // Soul particles at phantom position
                DisplayBuilder.dustParticles(phantomPos, 6, 0.5, 0, 255, 240, 0.8f);
                DisplayBuilder.dustParticles(phantomPos, 4, 0.3, 0, 160, 140, 0.6f);

                // Smoke
                w.spawnParticle(Particle.SMOKE, phantomPos.clone().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0.01);
            }

            // Small head-height black glass displays at phantom pos
            if (ticksAlive == 25) {
                for (int i = 0; i < 3; i++) {
                    Location displayLoc = phantomPos.clone().add(
                            (Math.random() - 0.5) * 0.4, 1.5 + Math.random() * 0.3, (Math.random() - 0.5) * 0.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(displayLoc, Material.BLACK_CONCRETE);
                    h.scale(0.1f, 0.1f, 0.1f).glow(200, 0, 50).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // No direct damage -- purely psychological misdirection
            // The danger is looking away from the Dweller
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheOtherVoice(plugin); }
    }

    // =========================================================================
    // 66. ECHO TRAIL -- 4 ghost copies follow the Dweller's path
    // =========================================================================
    public static class EchoTrail extends EnvironmentalAttack {

        private final List<BlockDisplayHandle>[] echoGhosts;
        private final Location[] echoPositions = new Location[4];
        private final List<Location> pathHistory = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public EchoTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_trail", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(360); // 18 seconds
            config.setTicksBetweenDamage(999);
            echoGhosts = new List[4];
            for (int i = 0; i < 4; i++) {
                echoGhosts[i] = new ArrayList<>();
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.8f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.9f), 5L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 1.0f), 10L);

            // Smoke warning
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 20, 0.5, 1.0, 0.5, 0.02);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 8, 0.4, 0.8, 0.4, 0.01);
                }
                return;
            }

            // Record center (Dweller's) path
            pathHistory.add(center.clone());

            // Position echoes at 0.5s (10 ticks), 1.0s, 1.5s, 2.0s behind
            int[] delayTicks = {10, 20, 30, 40};
            for (int i = 0; i < 4; i++) {
                int idx = pathHistory.size() - 1 - delayTicks[i];
                if (idx >= 0) {
                    echoPositions[i] = pathHistory.get(idx);
                }
            }

            // Render echo ghosts every 6 ticks
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    if (echoPositions[i] == null) continue;

                    // Remove old ghost displays
                    for (BlockDisplayHandle h : echoGhosts[i]) {
                        h.entity().remove();
                    }
                    echoGhosts[i].clear();

                    // Spawn ghost silhouette (simplified tall shape)
                    float opacity = 1.0f - (i * 0.2f); // 100%, 80%, 60%, 40%
                    float scale = opacity * 0.8f;
                    Location ghostLoc = echoPositions[i];

                    BlockDisplayHandle bodyH = displayBuilder.spawnBlock(
                            ghostLoc.clone().add(0, 0.5, 0), Material.OBSIDIAN);
                    bodyH.scale(scale, 2.0f * opacity, scale * 0.5f).glow(0, 150, 255).interpolation(3, 0);
                    echoGhosts[i].add(bodyH);
                    spawnedEntities.add(bodyH.entity());

                    // Soul fire halo around each echo
                    DisplayBuilder.dustParticles(ghostLoc, (int) (6 * opacity), 0.5, 0, 210, 255, 0.8f * opacity);

                    // Flame trail from oldest echo
                    if (i == 3) {
                        DisplayBuilder.dustParticles(ghostLoc, 3, 0.3, 255, 80, 0, 0.5f);
                    }
                }
            }

            // Damage check -- all echoes have hitboxes
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 4; i++) {
                    if (echoPositions[i] == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(echoPositions[i]) <= 4.0) {
                            p.damage(16.0); // 8 hearts per echo
                            DisplayBuilder.dustParticles(p.getLocation(), 6, 0.3, 0, 150, 255, 0.8f);
                        }
                    }
                }
            }

            // Crimson spore pulse on Dweller movement beat
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < 4; i++) {
                    if (echoPositions[i] != null) {
                        DisplayBuilder.dustParticles(echoPositions[i], 4, 0.4, 200, 0, 50, 0.6f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoTrail(plugin); }
    }

    // =========================================================================
    // 67. SCREAM IN THE STONE -- screaming faces vent lava columns
    // =========================================================================
    public static class ScreamInTheStone extends EnvironmentalAttack {

        private final List<Location> faceLocations = new ArrayList<>();
        private boolean vented = false;

        public ScreamInTheStone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scream_in_the_stone", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(340); // 17 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.6f);

            // 10 random face positions across arena
            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 2 + Math.random() * 12;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0.02, Math.sin(angle) * dist);
                faceLocations.add(loc);

                // Face display -- flat on floor
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                h.scale(0.8f, 0.03f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                spawnedEntities.add(h.entity());

                // Flame from mouth, soul from eyes
                DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, 0.2), 3, 0.1, 255, 70, 0, 0.6f);
                DisplayBuilder.dustParticles(loc.clone().add(-0.15, 0.1, -0.2), 2, 0.05, 0, 255, 220, 0.5f);
                DisplayBuilder.dustParticles(loc.clone().add(0.15, 0.1, -0.2), 2, 0.05, 0, 255, 220, 0.5f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: faces visible for 60 ticks (3 seconds) -- this IS the warning
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    for (Location loc : faceLocations) {
                        DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, 0.2), 2, 0.1, 255, 70, 0, 0.5f);
                        DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, -0.2), 2, 0.1, 0, 255, 220, 0.4f);
                    }
                }
                return;
            }

            // All faces vent simultaneously
            if (!vented) {
                vented = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);

                for (Location loc : faceLocations) {
                    // Lava column particles
                    for (int y = 0; y < 6; y++) {
                        w.spawnParticle(Particle.LAVA, loc.clone().add(0, y, 0), 8, 0.3, 0.3, 0.3, 0);
                    }

                    // Damage players in vent columns
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist2d = Math.sqrt(
                                Math.pow(p.getLocation().getX() - loc.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - loc.getZ(), 2));
                        if (dist2d <= 1.5 && p.getLocation().getY() - loc.getY() < 6) {
                            p.damage(20.0); // 10 hearts per vent column
                            DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                        }
                    }
                }
            }

            // Sustained vent particles
            int ventTick = ticksAlive - 60;
            if (ventTick % 4 == 0 && ventTick <= 30) {
                for (Location loc : faceLocations) {
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 3, 0), 4, 0.2, 2.0, 0.2, 0);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2, 0), 3, 0.4, 255, 100, 0, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScreamInTheStone(plugin); }
    }

    // =========================================================================
    // 68. RESIDUAL MEMORY -- ghost replay of Dweller's past position
    // =========================================================================
    public static class ResidualMemory extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ghostHandles = new ArrayList<>();
        private Location ghostLoc;
        private boolean ghostActive = false;

        public ResidualMemory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("residual_memory", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ghost at a random remembered position (simulate 30s ago)
            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 8;
            ghostLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            DisplayBuilder.playSound(ghostLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- ash drifts
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(ghostLoc, 5, 1.0, 140, 135, 125, 0.5f);
                }
                return;
            }

            // Spawn ghost
            if (!ghostActive) {
                ghostActive = true;
                DisplayBuilder.playSound(ghostLoc, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);

                // Build semi-transparent Dweller ghost form
                for (int y = 0; y < 3; y++) {
                    Location partLoc = ghostLoc.clone().add(0, y * 0.7, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(partLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.7f, 0.7f, 0.4f).glow(0, 150, 255).interpolation(5, 0);
                    ghostHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Warped spore + soul fire outline
                DisplayBuilder.dustParticles(ghostLoc.clone().add(0, 1, 0), 12, 0.8, 0, 200, 175, 0.8f);
                DisplayBuilder.dustParticles(ghostLoc.clone().add(0, 1.5, 0), 10, 0.6, 0, 240, 255, 0.7f);
            }

            int ghostTick = ticksAlive - 40;

            // Ghost replay animation -- subtle oscillation
            if (ghostTick % 8 == 0) {
                float sway = (float) Math.sin(ghostTick * 0.1) * 0.15f;
                for (BlockDisplayHandle h : ghostHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.35f + sway, -0.35f, -0.2f),
                            new AxisAngle4f(sway * 0.3f, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.4f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }

                // Dripping lava at ghost's floor contact
                w.spawnParticle(Particle.DRIPPING_LAVA, ghostLoc, 2, 0.3, 0, 0.3, 0);
            }

            // Ambient particles
            if (ghostTick % 6 == 0) {
                DisplayBuilder.dustParticles(ghostLoc.clone().add(0, 1, 0), 4, 0.5, 0, 200, 175, 0.6f);
            }

            // Damage players who walk into ghost
            if (ghostTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(ghostLoc) <= 4.0) {
                        p.damage(16.0); // 8 hearts
                        DisplayBuilder.dustParticles(p.getLocation(), 8, 0.4, 0, 240, 255, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResidualMemory(plugin); }
    }

    // =========================================================================
    // 69. SHADOW CRAWL -- Dweller's shadow detaches and hunts player
    // =========================================================================
    public static class ShadowCrawl extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> shadowHandles = new ArrayList<>();
        private Location shadowPos;
        private Player targetPlayer;
        private boolean shadowDetached = false;
        private boolean engulfed = false;

        public ShadowCrawl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_crawl", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            shadowPos = center.clone();

            // Find nearest player
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    targetPlayer = p;
                }
            }

            // Silence as warning -- then wither ambient
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (targetPlayer == null || !targetPlayer.isOnline()) return;

            // Warning: 30 ticks (1.5 seconds) -- shadow separating
            if (ticksAlive <= 30) {
                if (ticksAlive % 6 == 0) {
                    // Shadow at Dweller's feet gradually extending
                    double extend = ticksAlive / 30.0;
                    double angle = Math.atan2(
                            targetPlayer.getLocation().getZ() - center.getZ(),
                            targetPlayer.getLocation().getX() - center.getX());
                    Location shadowEdge = center.clone().add(Math.cos(angle) * extend * 2, 0, Math.sin(angle) * extend * 2);
                    DisplayBuilder.dustParticles(shadowEdge, 4, 0.3, 100, 95, 90, 0.4f);
                }
                return;
            }

            // Detach shadow
            if (!shadowDetached) {
                shadowDetached = true;

                // Spawn shadow cluster
                for (int i = 0; i < 5; i++) {
                    Location sLoc = shadowPos.clone().add(
                            (Math.random() - 0.5) * 1.5, 0.01, (Math.random() - 0.5) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(sLoc, Material.BLACK_CONCRETE);
                    h.scale(0.6f, 0.02f, 0.6f).glow(200, 0, 50).interpolation(3, 0);
                    shadowHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Move shadow toward player at 4 blocks/second (0.2 blocks/tick)
            double dx = targetPlayer.getLocation().getX() - shadowPos.getX();
            double dz = targetPlayer.getLocation().getZ() - shadowPos.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.5) {
                shadowPos = shadowPos.clone().add(dx / dist * 0.2, 0, dz / dist * 0.2);

                // Move shadow displays
                for (BlockDisplayHandle h : shadowHandles) {
                    Location newLoc = h.entity().getLocation().clone().add(dx / dist * 0.2, 0, dz / dist * 0.2);
                    h.entity().teleport(newLoc);
                }
            }

            // Particles at shadow edges
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(shadowPos, 4, 0.5, 0, 220, 255, 0.6f);
                DisplayBuilder.dustParticles(shadowPos.clone().add(0, 0, 0.5), 2, 0.2, 100, 95, 90, 0.4f);
                // Fire suppression effect around shadow
                w.spawnParticle(Particle.SMOKE, shadowPos.clone().add(0, 0.3, 0), 3, 0.5, 0, 0.5, 0.01);
            }

            // Engulf check
            if (!engulfed && targetPlayer.getLocation().distanceSquared(shadowPos) <= 2.25) {
                engulfed = true;
                targetPlayer.damage(18.0); // 9 hearts
                DisplayBuilder.crimsonDust(targetPlayer.getLocation(), 15, 1.0);
                DisplayBuilder.playSound(shadowPos, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);

                // World dims -- smoke particles around player
                w.spawnParticle(Particle.SMOKE, targetPlayer.getLocation().clone().add(0, 1, 0),
                        30, 1.0, 1.0, 1.0, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowCrawl(plugin); }
    }

    // =========================================================================
    // 70. THE ISLAND SPEAKS -- massive face pattern of fire across entire island
    // =========================================================================
    public static class TheIslandSpeaks extends EnvironmentalAttack {

        private final List<Location> pupilLocations = new ArrayList<>();
        private final List<Location> outlineLocations = new ArrayList<>();
        private boolean faceActive = false;

        public TheIslandSpeaks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_island_speaks", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // All sounds cut to silence (indicated by no sounds for 1.5s)
            // Then all layer sounds at once
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
            }, 30L);

            // Pre-generate face pattern locations
            // Pupils: two dense zones at (-4,0,-3) and (4,0,-3) relative to center
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pupilLocations.add(center.clone().add(-4 + dx, 0, -3 + dz));
                    pupilLocations.add(center.clone().add(4 + dx, 0, -3 + dz));
                }
            }

            // Face outline: oval shape
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                double x = Math.cos(angle) * 9;
                double z = Math.sin(angle) * 7;
                outlineLocations.add(center.clone().add(x, 0, z));
            }
            // Mouth line
            for (int x = -3; x <= 3; x++) {
                outlineLocations.add(center.clone().add(x, 0, 4));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds) of silence
            if (ticksAlive <= 30) {
                return;
            }

            // Activate face
            if (!faceActive) {
                faceActive = true;
            }

            int faceTick = ticksAlive - 30;

            // Render face particle columns
            if (faceTick % 3 == 0) {
                // Pupil zones: dense LAVA columns
                for (Location loc : pupilLocations) {
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 1, 0), 4, 0.2, 2.0, 0.2, 0);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 0.5, 0), 3, 0.2, 255, 100, 0, 1.0f);
                }

                // Outline: soul fire outline
                for (Location loc : outlineLocations) {
                    DisplayBuilder.dustParticles(loc.clone().add(0, 0.3, 0), 2, 0.2, 0, 255, 200, 0.8f);
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 1, 0.1, 0.5, 0.1, 0);
                }

                // Interior fill: crimson spore
                for (int i = 0; i < 12; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 7;
                    Location fillLoc = center.clone().add(Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(fillLoc, 2, 0.3, 210, 35, 20, 0.6f);
                }
            }

            // Damage zones
            if (faceTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();

                    // Check pupil zones (concentrated damage)
                    for (Location pupil : pupilLocations) {
                        if (pl.distanceSquared(pupil) <= 2.25) {
                            p.damage(12.0); // 12 hearts total in pupil per exposure
                            DisplayBuilder.dustParticles(pl, 6, 0.3, 255, 100, 0, 1.0f);
                            break;
                        }
                    }

                    // Check face outline zones (lighter damage)
                    for (Location outline : outlineLocations) {
                        if (pl.distanceSquared(outline) <= 2.25) {
                            p.damage(10.0); // 5 hearts
                            break;
                        }
                    }
                }
            }

            // Spawn block displays at key positions for visual impact
            if (faceTick == 5) {
                // Eye blocks
                for (int i = 0; i < 2; i++) {
                    Location eyeLoc = center.clone().add(i == 0 ? -4 : 4, 0.05, -3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(eyeLoc, Material.MAGMA_BLOCK);
                    h.scale(3.0f, 0.1f, 2.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheIslandSpeaks(plugin); }
    }
}
