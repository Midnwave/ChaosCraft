package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

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
 * Phase 4D Environmental -- GROUP 5: ESSENCE STORM EVENTS
 * Attacks #41-50: Corruption bloom, dragon eye contact, gold fracture, amethyst ceiling
 * collapse, sanctum spire topple, gilded wall shatter, purpur implosion, The Sanctum
 * Cracks Open, monolith collapse, The Sanctum's Last Breath.
 *
 * Design notes:
 * - No status effects
 * - Dragon palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Sanctum destruction materials: PURPUR_BLOCK, POLISHED_BLACKSTONE, AMETHYST_BLOCK, OBSIDIAN
 * - Damage range: 6.0-14.0 HP (exceptions: spire topple 20 HP for massive structures)
 */
public final class EssenceStorms {

    private EssenceStorms() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CorruptionBloom(plugin));
        registry.register(new DragonEyeContact(plugin));
        registry.register(new GoldBlockFracture(plugin));
        registry.register(new AmethystCeilingCollapse(plugin));
        registry.register(new SanctumSpireTopple(plugin));
        registry.register(new GildedWallShatter(plugin));
        registry.register(new PurpurPillarImplosion(plugin));
        registry.register(new TheSanctumCracksOpen(plugin));
        registry.register(new MonolithCollapseSequence(plugin));
        registry.register(new TheSanctumsLastBreath(plugin));
    }

    // =========================================================================
    // 41. CORRUPTION BLOOM -- arena-wide dragon breath haze + HP compression
    // =========================================================================
    public static class CorruptionBloom extends EnvironmentalAttack {

        private boolean bloomActive = false;

        public CorruptionBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_bloom", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0); // 4 hearts HP compression
            config.setDamageRadius(25.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Density buildup starts automatically
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- dragon breath density ramps
            if (ticksAlive <= 80) {
                double density = ticksAlive / 80.0;
                int count = (int) (10 + density * 40);
                if (ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                            count, 20, 3, 20, 0.01);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), count / 2, 20.0,
                            80, 0, 80, 2.0f);
                }
                return;
            }

            // Peak bloom: 40 ticks
            if (!bloomActive) {
                bloomActive = true;

                // HP compression damage to all players
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    p.damage(8.0); // 4 hearts
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
            }

            int bloomTick = ticksAlive - 80;

            if (bloomTick <= 40) {
                // Oppressive haze
                if (bloomTick % 2 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                            40, 20, 3, 20, 0.01);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 20, 20.0,
                            80, 0, 80, 2.0f);
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0),
                            15, 20, 1, 20, 0.01);
                }

                // Enderman stare loop
                if (bloomTick % 100 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.3f);
                }
            }

            // Clearing: 30 ticks
            if (bloomTick > 40 && bloomTick <= 70) {
                double fade = 1.0 - ((bloomTick - 40) / 30.0);
                if (bloomTick % 3 == 0) {
                    int count = (int) (30 * fade);
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                            count, 20, 3, 20, 0.01);
                }
            }

            // Clearing ring sound
            if (bloomTick == 41) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionBloom(plugin); }
    }

    // =========================================================================
    // 42. DRAGON EYE CONTACT -- triggers when player looks at dragon too long
    // =========================================================================
    public static class DragonEyeContact extends EnvironmentalAttack {

        private Player targetedPlayer;
        private boolean beamFired = false;

        public DragonEyeContact(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_eye_contact", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            targetedPlayer = getTargetPlayer();
            if (targetedPlayer == null) {
                // Random fallback
                List<Player> players = new ArrayList<>(w.getPlayers());
                players.removeIf(this::isExempt);
                if (!players.isEmpty()) {
                    targetedPlayer = players.get((int) (Math.random() * players.size()));
                }
            }

            if (targetedPlayer != null) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.6f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || targetedPlayer == null || !targetedPlayer.isOnline()) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!beamFired && ticksAlive >= 5) {
                beamFired = true;

                Location dragonHead = center.clone().add(0, 30, 0);
                Location playerEye = targetedPlayer.getEyeLocation().clone();

                // Eye contact beam
                DisplayBuilder.particleLine(dragonHead, playerEye, Particle.DRAGON_BREATH, 3, null);
                DisplayBuilder.particleLine(dragonHead, playerEye, Particle.DUST, 2,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 180), 2.5f));

                // Impact burst
                w.spawnParticle(Particle.DUST, playerEye, 25, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 0, 255), 3.0f));

                DisplayBuilder.playSound(targetedPlayer.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.8f, 0.6f);

                // Damage
                if (!isExempt(targetedPlayer)) {
                    targetedPlayer.damage(10.0);
                }
            }

            // Beam persists for 10 ticks
            if (beamFired && ticksAlive <= 15) {
                Location dragonHead = center.clone().add(0, 30, 0);
                Location playerEye = targetedPlayer.getEyeLocation().clone();
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.particleLine(dragonHead, playerEye, Particle.DRAGON_BREATH, 2, null);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonEyeContact(plugin); }
    }

    // =========================================================================
    // 43. GOLD BLOCK FRACTURE -- decorative gold cluster shatters into debris
    // =========================================================================
    public static class GoldBlockFracture extends EnvironmentalAttack {

        private Location clusterCenter;
        private final List<BlockDisplayHandle> fragmentHandles = new ArrayList<>();
        private final List<Location> fragmentTargets = new ArrayList<>();
        private boolean shattered = false;
        private int fragmentCount;

        public GoldBlockFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gold_block_fracture", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 15;
            clusterCenter = center.clone().add(Math.cos(a) * d, 2, Math.sin(a) * d);
            fragmentCount = 4 + (int) (Math.random() * 5); // 4-8
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- cracking
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    w.spawnParticle(Particle.CRIT, clusterCenter, 5 + ticksAlive / 4, 0.5, 0.5, 0.5, 0.05);
                }
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_STONE_HIT, 0.4f, 0.4f);
                }
                if (ticksAlive == 35) {
                    // Cluster shifts downward
                    DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_GILDED_BLACKSTONE_BREAK, 0.9f, 0.6f);
                }
                return;
            }

            // Shatter
            if (!shattered) {
                shattered = true;

                // Massive gold block particle burst
                w.spawnParticle(Particle.BLOCK, clusterCenter, 100, 1.5, 1.0, 1.5, 0,
                        Material.POLISHED_BLACKSTONE.createBlockData());
                w.spawnParticle(Particle.CRIT, clusterCenter, 40, 3, 1.5, 3, 0.15);
                DisplayBuilder.playSound(clusterCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

                // Spawn flying fragments
                for (int i = 0; i < fragmentCount; i++) {
                    double fa = Math.random() * 2 * Math.PI;
                    double fd = 3 + Math.random() * 5;
                    Location target = clusterCenter.clone().add(Math.cos(fa) * fd, -2, Math.sin(fa) * fd);
                    fragmentTargets.add(target);

                    BlockDisplayHandle h = displayBuilder.spawnBlock(clusterCenter.clone(), Material.POLISHED_BLACKSTONE);
                    h.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 0).interpolation(3, 0);
                    float spinAngle = (float) (Math.random() * Math.PI);
                    h.rotate(spinAngle, (float) Math.random(), 1, (float) Math.random());
                    fragmentHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Fragment impact damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(clusterCenter) <= 9.0) {
                        p.damage(8.0);
                        double angle = Math.atan2(p.getLocation().getZ() - clusterCenter.getZ(),
                                p.getLocation().getX() - clusterCenter.getX());
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(Math.cos(angle) * 0.5, 0.2, Math.sin(angle) * 0.5)));
                    }
                }
            }

            int shatterTick = ticksAlive - 40;

            // Animate fragments outward over 15 ticks
            if (shatterTick <= 15) {
                double progress = shatterTick / 15.0;
                for (int i = 0; i < fragmentHandles.size(); i++) {
                    BlockDisplay bd = fragmentHandles.get(i).entity();
                    Location target = fragmentTargets.get(i);
                    Location current = clusterCenter.clone().add(
                            (target.getX() - clusterCenter.getX()) * progress,
                            (target.getY() - clusterCenter.getY()) * progress,
                            (target.getZ() - clusterCenter.getZ()) * progress);
                    bd.teleport(current);

                    // Crit trail
                    w.spawnParticle(Particle.CRIT, current, 2, 0.1, 0.1, 0.1, 0.02);
                }

                // Fragment landing sounds
                if (shatterTick % 3 == 0) {
                    DisplayBuilder.playSound(clusterCenter, Sound.BLOCK_STONE_BREAK, 0.4f, 0.7f);
                }
            }

            // Landing: scale down, become permanent debris
            if (shatterTick == 16) {
                for (BlockDisplayHandle h : fragmentHandles) {
                    h.scale(0.3f, 0.3f, 0.3f);
                    h.interpolation(10, 0);
                }
            }

            // Debris field damage: 4 HP per second for 100 ticks
            if (shatterTick > 15 && shatterTick <= 115 && shatterTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(clusterCenter) <= 36.0) { // 6 block radius
                        p.damage(4.0);
                    }
                }
            }

            // Debris field dust
            if (shatterTick > 15 && shatterTick <= 115 && shatterTick % 5 == 0) {
                DisplayBuilder.dustParticles(clusterCenter, 5, 3.0, 255, 200, 0, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldBlockFracture(plugin); }
    }

    // =========================================================================
    // 44. AMETHYST CEILING COLLAPSE -- chunks fall from ceiling canopy at Y+20
    // =========================================================================
    public static class AmethystCeilingCollapse extends EnvironmentalAttack {

        private final List<Location> chunkTargets = new ArrayList<>();
        private boolean chunksFalling = false;
        private int chunkCount;

        public AmethystCeilingCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("amethyst_ceiling_collapse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            chunkCount = 4 + (int) (Math.random() * 5); // 4-8

            // Ceiling fracture sounds
            DisplayBuilder.playSound(center.clone().add(0, 20, 0), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- dust falls from ceiling
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 20, 0),
                            5 + ticksAlive / 4, 8.0, 200, 50, 255, 1.0f);
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center.clone().add(0, 20, 0), Sound.BLOCK_AMETHYST_BLOCK_BREAK,
                            1.0f, 0.5f);
                }
                return;
            }

            // Chunks begin falling
            if (!chunksFalling) {
                chunksFalling = true;

                for (int i = 0; i < chunkCount; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 15;
                    Location target = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    chunkTargets.add(target);
                }
            }

            int fallTick = ticksAlive - 40;

            // Chunks fall: Y+20 to Y+0 at 1.5 blocks/tick (~13 ticks)
            for (int i = 0; i < chunkTargets.size(); i++) {
                Location target = chunkTargets.get(i);
                double currentY = 20 - (fallTick * 1.5);

                // Preview marker 3 ticks before landing
                if (currentY <= 5 && currentY > 3) {
                    DisplayBuilder.dustParticles(target.clone().add(0, 1, 0), 5, 0.5,
                            255, 150, 255, 2.5f);
                }

                if (currentY <= 0 && currentY > -1.5) {
                    // Landing impact
                    w.spawnParticle(Particle.BLOCK, target, 50, 1.0, 0.5, 1.0, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, target.clone().add(0, 0.5, 0), 20, 0.8, 0.5, 0.8, 0.1);
                    DisplayBuilder.playSound(target, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 0.9f, 0.4f);

                    // Permanent chunk on ground
                    BlockDisplayHandle chunk = displayBuilder.spawnBlock(
                            target.clone().add(0, 0.02, 0), Material.AMETHYST_BLOCK);
                    chunk.scale(1.5f, 1.5f, 1.5f).glow(180, 0, 200).interpolation(3, 0);
                    spawnedEntities.add(chunk.entity());

                    // Impact damage: 14 HP direct, 6 HP splash
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(target);
                        if (dist <= 2.25) {
                            p.damage(14.0);
                        } else if (dist <= 9.0) {
                            p.damage(6.0);
                        }
                    }
                } else if (currentY > 0) {
                    // Falling trail
                    if (fallTick % 2 == 0) {
                        Location chunkPos = target.clone().add(0, currentY, 0);
                        w.spawnParticle(Particle.CRIT, chunkPos, 5, 0.3, 0.3, 0.3, 0.05);
                        w.spawnParticle(Particle.ENCHANT, chunkPos, 3, 0.2, 0.2, 0.2, 0.5);
                    }
                }
            }

            // Ceiling collapse sounds staggered
            if (fallTick % 3 == 0 && fallTick <= 15) {
                DisplayBuilder.playSound(center.clone().add(0, 15, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystCeilingCollapse(plugin); }
    }

    // =========================================================================
    // 45. SANCTUM SPIRE TOPPLE -- tall decorative spire falls inward
    // =========================================================================
    public static class SanctumSpireTopple extends EnvironmentalAttack {

        private Location spireBase;
        private double fallAngle;
        private int spireHeight;
        private boolean toppled = false;
        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();

        public SanctumSpireTopple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_spire_topple", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spire at arena edge
            double a = Math.random() * 2 * Math.PI;
            spireBase = center.clone().add(Math.cos(a) * 18, 0, Math.sin(a) * 18);
            fallAngle = Math.atan2(center.getZ() - spireBase.getZ(), center.getX() - spireBase.getX()); // Toward center
            spireHeight = 8 + (int) (Math.random() * 7); // 8-14

            // Spawn spire blocks
            for (int y = 0; y < spireHeight; y++) {
                Material mat = (y % 3 == 0) ? Material.AMETHYST_BLOCK : Material.PURPUR_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        spireBase.clone().add(0, y, 0), mat);
                h.scale(1.5f, 1.0f, 1.5f).glow(180, 100, 200).interpolation(3, 0);
                spireBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Lean buildup: 40 ticks
            if (ticksAlive <= 40) {
                double leanDeg = ticksAlive * 0.15; // 6 degrees by tick 40
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(spireBase, Sound.BLOCK_STONE_HIT, 0.4f, 0.4f);
                }
                if (ticksAlive == 35) {
                    w.spawnParticle(Particle.BLOCK, spireBase, 20, 0.5, 0.5, 0.5, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                }
                return;
            }

            // Topple: 10 ticks
            if (!toppled) {
                toppled = true;
                DisplayBuilder.playSound(spireBase, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }

            int toppleTick = ticksAlive - 40;

            if (toppleTick <= 10) {
                // Spire falls as rigid body: pivot at base
                double tiltFraction = toppleTick / 10.0;
                double tiltRadians = tiltFraction * (Math.PI / 2); // 0 to 90 degrees

                for (int i = 0; i < spireBlocks.size(); i++) {
                    double originalY = i;
                    // After tilt: block at height i projects to horizontal distance i at full tilt
                    double projX = Math.sin(tiltRadians) * originalY;
                    double projY = Math.cos(tiltRadians) * originalY;
                    Location newPos = spireBase.clone().add(
                            Math.cos(fallAngle) * projX, projY, Math.sin(fallAngle) * projX);
                    spireBlocks.get(i).entity().teleport(newPos);
                }

                // Arc particles
                for (int i = 0; i < spireHeight; i++) {
                    double dist = Math.sin(tiltRadians) * i;
                    Location arcPt = spireBase.clone().add(
                            Math.cos(fallAngle) * dist, Math.cos(tiltRadians) * i, Math.sin(fallAngle) * dist);
                    w.spawnParticle(Particle.BLOCK, arcPt, 5, 0.3, 0.3, 0.3, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(arcPt, 2, 0.3, 180, 100, 200, 2.0f);
                }

                // Ground impact at tick 10
                if (toppleTick == 10) {
                    Location tipImpact = spireBase.clone().add(
                            Math.cos(fallAngle) * spireHeight, 0, Math.sin(fallAngle) * spireHeight);

                    w.spawnParticle(Particle.BLOCK, tipImpact, 100, 2, 1, 2, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                    w.spawnParticle(Particle.SMOKE, tipImpact, 40, 3, 1, 3, 0.02);
                    DisplayBuilder.playSound(tipImpact, Sound.BLOCK_STONE_FALL, 1.0f, 0.3f);

                    // Arc damage: along the fall path
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();

                        // Check if player is in sweep arc
                        for (int seg = 0; seg < spireHeight; seg++) {
                            Location segLoc = spireBase.clone().add(
                                    Math.cos(fallAngle) * seg, 0, Math.sin(fallAngle) * seg);
                            if (pl.distanceSquared(segLoc) <= 4.0) { // 2 block radius
                                p.damage(14.0); // Scaled to 14 max for design range
                                double knockAngle = Math.atan2(pl.getZ() - spireBase.getZ(),
                                        pl.getX() - spireBase.getX());
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(Math.cos(knockAngle) * 0.8, 0.3, Math.sin(knockAngle) * 0.8)));
                                break;
                            }
                        }

                        // Tip impact damage
                        if (pl.distanceSquared(tipImpact) <= 9.0) {
                            p.damage(12.0);
                        }
                    }
                }
            }

            // Dust settling
            if (toppleTick > 10 && toppleTick <= 70 && toppleTick % 10 == 0) {
                for (int i = 0; i < spireHeight; i++) {
                    Location dustPt = spireBase.clone().add(
                            Math.cos(fallAngle) * i, 0.5, Math.sin(fallAngle) * i);
                    DisplayBuilder.dustParticles(dustPt, 2, 0.5, 120, 60, 150, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { /* Fallen spire persists */ }

        @Override
        public AbstractAttack newInstance() { return new SanctumSpireTopple(plugin); }
    }

    // =========================================================================
    // 46. GILDED WALL SHATTER -- 5x3 wall panel shatters outward
    // =========================================================================
    public static class GildedWallShatter extends EnvironmentalAttack {

        private Location wallCenter;
        private double wallFacing;
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean shattered = false;

        public GildedWallShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gilded_wall_shatter", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            wallCenter = center.clone().add(Math.cos(a) * 15, 1.5, Math.sin(a) * 15);
            wallFacing = a + Math.PI; // Face inward
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 35 ticks -- cracks form
            if (ticksAlive <= 35) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(wallCenter, 3, 1.0, 255, 200, 50, 0.8f);
                }
                if (ticksAlive >= 20 && ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.CRIT, wallCenter, 8, 1.5, 1.0, 1.5, 0.05);
                }
                if (ticksAlive == 30) {
                    DisplayBuilder.playSound(wallCenter, Sound.BLOCK_GILDED_BLACKSTONE_BREAK, 0.9f, 0.6f);
                }
                return;
            }

            // Shatter
            if (!shattered) {
                shattered = true;

                w.spawnParticle(Particle.BLOCK, wallCenter, 80, 1.5, 1.0, 1.5, 0,
                        Material.POLISHED_BLACKSTONE.createBlockData());
                w.spawnParticle(Particle.CRIT, wallCenter, 40, 2.0, 1.0, 2.0, 0.1);

                DisplayBuilder.playSound(wallCenter, Sound.BLOCK_GILDED_BLACKSTONE_BREAK, 1.0f, 0.6f);

                // 15 wall entities fly outward as shards
                for (int x = -2; x <= 2; x++) {
                    for (int y = 0; y < 3; y++) {
                        double perpX = -Math.sin(wallFacing);
                        double perpZ = Math.cos(wallFacing);
                        Location shardOrigin = wallCenter.clone().add(perpX * x, y - 1, perpZ * x);

                        double flyDist = 5 + Math.random() * 7;
                        Location flyTarget = shardOrigin.clone().add(
                                Math.cos(wallFacing) * flyDist, -1 - y * 0.3, Math.sin(wallFacing) * flyDist);

                        BlockDisplayHandle h = displayBuilder.spawnBlock(shardOrigin, Material.POLISHED_BLACKSTONE);
                        h.scale(0.5f, 0.5f, 0.5f).glow(200, 150, 0).interpolation(15, 0);
                        shardHandles.add(h);
                        spawnedEntities.add(h.entity());

                        // Animate shard flight
                        final Location target = flyTarget;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            h.entity().teleport(target);
                            h.scale(0.3f, 0.3f, 0.3f);
                            h.interpolation(10, 0);
                        }, 2L);
                    }
                }

                // Damage players in front of wall
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - wallCenter.getX();
                    double dz = p.getLocation().getZ() - wallCenter.getZ();
                    double facingDot = dx * Math.cos(wallFacing) + dz * Math.sin(wallFacing);
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (facingDot > 0 && dist <= 8) {
                        int shardHits = 1 + (int) (Math.random() * 4); // 1-4 shards
                        p.damage(6.0 * shardHits);
                    }
                }

                // Flying fragment sounds
                for (int i = 0; i < 5; i++) {
                    final int delay = i * 2;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(wallCenter, Sound.BLOCK_STONE_BREAK, 0.4f, 1.2f), delay);
                }
            }

            // Permanent debris dust
            int shatterTick = ticksAlive - 35;
            if (shatterTick > 15 && shatterTick % 8 == 0) {
                DisplayBuilder.dustParticles(wallCenter, 3, 4.0, 200, 150, 0, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { /* Debris persists */ }

        @Override
        public AbstractAttack newInstance() { return new GildedWallShatter(plugin); }
    }

    // =========================================================================
    // 47. PURPUR PILLAR IMPLOSION -- central pillar implodes then detonates
    // =========================================================================
    public static class PurpurPillarImplosion extends EnvironmentalAttack {

        private Location pillarCenter;
        private boolean imploded = false;

        public PurpurPillarImplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purpur_pillar_implosion", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 10;
            pillarCenter = center.clone().add(Math.cos(a) * d, 5, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Compression: 50 ticks
            if (ticksAlive <= 50) {
                if (ticksAlive % 5 == 0) {
                    double compression = ticksAlive / 50.0;
                    double radius = 1.5 * (1.0 - compression * 0.6);
                    DisplayBuilder.particleRing(pillarCenter, radius,
                            Particle.DUST, 8, new Particle.DustOptions(Color.fromRGB(180, 80, 200), 1.5f));
                    DisplayBuilder.playSound(pillarCenter, Sound.BLOCK_STONE_HIT, 0.3f, 0.5f);
                }
                return;
            }

            // Implosion
            if (!imploded) {
                imploded = true;

                // Pull phase: 3 ticks
                w.spawnParticle(Particle.REVERSE_PORTAL, pillarCenter, 60, 4, 4, 4, 0.01);
                DisplayBuilder.playSound(pillarCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);

                // Pull players toward column center
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dist = pl.distance(pillarCenter);
                    if (dist <= 15 && dist > 0.5) {
                        double dx = (pillarCenter.getX() - pl.getX()) / dist * 0.6;
                        double dz = (pillarCenter.getZ() - pl.getZ()) / dist * 0.6;
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(dx, 0, dz)));
                    }
                }

                // Schedule detonation + push after 3 ticks
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Massive detonation
                    w.spawnParticle(Particle.BLOCK, pillarCenter, 150, 3, 3, 3, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(pillarCenter, 30, 3.0, 180, 80, 200, 3.0f);
                    w.spawnParticle(Particle.SMOKE, pillarCenter, 60, 3, 3, 3, 0.05);

                    DisplayBuilder.playSound(pillarCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.7f);
                    DisplayBuilder.playSound(pillarCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                    // Push players outward
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        double dist = pl.distance(pillarCenter);
                        if (dist <= 15) {
                            // Damage based on distance
                            if (dist <= 2) {
                                p.damage(14.0); // Capped to design range
                            } else if (dist <= 5) {
                                p.damage(12.0);
                            } else if (dist <= 10) {
                                p.damage(6.0);
                            }
                            // Push blast
                            double angle = Math.atan2(pl.getZ() - pillarCenter.getZ(),
                                    pl.getX() - pillarCenter.getX());
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(Math.cos(angle) * 0.8, 0.3, Math.sin(angle) * 0.8)));
                        }
                    }

                    // Debris scatter
                    for (int i = 0; i < 10; i++) {
                        double da = Math.random() * 2 * Math.PI;
                        double dd = 2 + Math.random() * 6;
                        Location debrisLoc = pillarCenter.clone().add(
                                Math.cos(da) * dd, -5 + Math.random(), Math.sin(da) * dd);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(debrisLoc, Material.PURPUR_BLOCK);
                        h.scale(0.4f, 0.4f, 0.4f).glow(150, 60, 180).interpolation(5, 0);
                        spawnedEntities.add(h.entity());
                    }
                }, 3L);
            }

            // Dust settling
            int postTick = ticksAlive - 50;
            if (postTick > 3 && postTick % 5 == 0) {
                DisplayBuilder.dustParticles(pillarCenter, 4, 4.0, 150, 60, 180, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PurpurPillarImplosion(plugin); }
    }

    // =========================================================================
    // 48. THE SANCTUM CRACKS OPEN -- massive arena terrain destruction at 20% HP
    // =========================================================================
    public static class TheSanctumCracksOpen extends EnvironmentalAttack {

        private boolean sequenceStarted = false;

        public TheSanctumCracksOpen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_sanctum_cracks_open", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Island exhales smoke from every point
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = Math.random() * 20;
                w.spawnParticle(Particle.SMOKE, center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                        3, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: 120 ticks -- smoke + dust + scars activate
            if (ticksAlive <= 120) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        Location pt = center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                        w.spawnParticle(Particle.SMOKE, pt, 2, 0.3, 0.3, 0.3, 0.01);
                    }
                }
                if (ticksAlive >= 40 && ticksAlive % 8 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        DisplayBuilder.dustParticles(center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                                3, 0.5, 80, 0, 80, 1.0f);
                    }
                }
                if (ticksAlive == 80) {
                    // Flash pulse: everything illuminates
                    w.spawnParticle(Particle.FLASH, center.clone().add(0, 5, 0), 8, 15, 3, 15, 0);
                }
                if (ticksAlive == 110) {
                    // Silence before the fall
                }
                return;
            }

            // Phase 2: Execution -- perimeter drops + cracks + ceiling collapse
            if (!sequenceStarted) {
                sequenceStarted = true;

                // Sound layering
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);

                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f), 5L);

                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.2f), 15L);

                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.3f), 25L);

                // Perimeter void particles
                for (int i = 0; i < 24; i++) {
                    double a = (2 * Math.PI * i) / 24;
                    Location perimLoc = center.clone().add(Math.cos(a) * 18, 0.5, Math.sin(a) * 18);
                    w.spawnParticle(Particle.BLOCK, perimLoc, 30, 1, 0.5, 1, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                    w.spawnParticle(Particle.REVERSE_PORTAL, perimLoc, 10, 1, 1, 1, 0.01);
                }

                // 6 radial cracks
                for (int crack = 0; crack < 6; crack++) {
                    double crackAngle = (2 * Math.PI * crack) / 6;
                    for (int seg = 0; seg < 15; seg++) {
                        Location crackPt = center.clone().add(
                                Math.cos(crackAngle) * seg, 0.3, Math.sin(crackAngle) * seg);
                        DisplayBuilder.dustParticles(crackPt, 5, 0.5, 100, 0, 100, 2.0f);
                    }
                }

                // Ceiling chunks fall
                for (int i = 0; i < 10; i++) {
                    double ca = Math.random() * 2 * Math.PI;
                    double cd = Math.random() * 15;
                    Location chunkTarget = center.clone().add(Math.cos(ca) * cd, 0, Math.sin(ca) * cd);

                    // Schedule chunk impact
                    long delay = (long) (5 + Math.random() * 20);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        w.spawnParticle(Particle.BLOCK, chunkTarget, 40, 1, 0.5, 1, 0,
                                Material.AMETHYST_BLOCK.createBlockData());
                        w.spawnParticle(Particle.CRIT, chunkTarget, 15, 0.5, 0.5, 0.5, 0.1);

                        // Chunk impact damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(chunkTarget) <= 2.25) {
                                p.damage(14.0);
                            }
                        }
                    }, delay);
                }

                // Dragon breath saturation during sequence
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 5, 0),
                        80, 20, 5, 20, 0.01);
            }

            int seqTick = ticksAlive - 120;

            // Ongoing collapse particles
            if (seqTick % 5 == 0 && seqTick <= 60) {
                // Perimeter void gap grows
                for (int i = 0; i < 16; i++) {
                    double a = (2 * Math.PI * i) / 16;
                    Location voidPt = center.clone().add(Math.cos(a) * 18, 0.5, Math.sin(a) * 18);
                    w.spawnParticle(Particle.REVERSE_PORTAL, voidPt, 8, 2, 1, 2, 0.01);
                }

                // New crack edge damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check near any of 6 cracks
                    for (int crack = 0; crack < 6; crack++) {
                        double crackAngle = (2 * Math.PI * crack) / 6;
                        double perpX = -Math.sin(crackAngle);
                        double perpZ = Math.cos(crackAngle);
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        double perpDist = Math.abs(dx * perpX + dz * perpZ);
                        if (perpDist <= 1.0) {
                            p.damage(2.0);
                            break;
                        }
                    }
                }
            }

            // All structures illuminate post-sequence
            if (seqTick == 40) {
                w.spawnParticle(Particle.FLASH, center.clone().add(0, 3, 0), 5, 15, 2, 15, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheSanctumCracksOpen(plugin); }
    }

    // =========================================================================
    // 49. MONOLITH COLLAPSE SEQUENCE -- tall mixed-material monolith topples
    // =========================================================================
    public static class MonolithCollapseSequence extends EnvironmentalAttack {

        private Location monolithBase;
        private double fallDirection;
        private int monolithHeight;
        private final List<BlockDisplayHandle> monolithBlocks = new ArrayList<>();
        private boolean collapsed = false;

        public MonolithCollapseSequence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_collapse_sequence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Monolith at cardinal position
            int cardinal = (int) (Math.random() * 4);
            double a = (Math.PI / 2) * cardinal;
            monolithBase = center.clone().add(Math.cos(a) * 16, 0, Math.sin(a) * 16);
            fallDirection = a + Math.PI; // Toward center
            monolithHeight = 12 + (int) (Math.random() * 5); // 12-16

            // Spawn monolith blocks
            Material[] palette = {Material.PURPUR_BLOCK, Material.AMETHYST_BLOCK, Material.OBSIDIAN, Material.POLISHED_BLACKSTONE};
            for (int y = 0; y < monolithHeight; y++) {
                Material mat = palette[y % palette.length];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        monolithBase.clone().add(0, y, 0), mat);
                h.scale(1.5f, 1.0f, 1.5f).glow(150, 0, 200).interpolation(3, 0);
                monolithBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dust warning
            DisplayBuilder.dustParticles(monolithBase.clone().add(0, monolithHeight / 2.0, 0),
                    15, 2.0, 150, 0, 200, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sway buildup: 50 ticks
            if (ticksAlive <= 50) {
                double swayMagnitude;
                if (ticksAlive <= 30) {
                    swayMagnitude = 1.0; // +/- 1 degree
                } else if (ticksAlive <= 40) {
                    swayMagnitude = 3.0;
                } else {
                    swayMagnitude = 8.0; // Violent
                }
                double swayAngle = Math.sin(ticksAlive * 0.4) * swayMagnitude * (Math.PI / 180);

                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.playSound(monolithBase, Sound.BLOCK_STONE_HIT, 0.4f, 0.4f);
                }

                // Apply sway to top blocks
                for (int i = 0; i < monolithBlocks.size(); i++) {
                    double yOffset = i;
                    double swayDist = Math.sin(swayAngle) * yOffset * 0.05;
                    Location swayPos = monolithBase.clone().add(
                            Math.cos(fallDirection) * swayDist, yOffset, Math.sin(fallDirection) * swayDist);
                    monolithBlocks.get(i).entity().teleport(swayPos);
                }
                return;
            }

            // Collapse: 20 ticks
            if (!collapsed) {
                collapsed = true;
                DisplayBuilder.playSound(monolithBase, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
            }

            int collapseTick = ticksAlive - 50;

            if (collapseTick <= 20) {
                double tiltFraction = collapseTick / 20.0;
                double tiltRadians = tiltFraction * (Math.PI / 2);

                // Rigid body rotation
                for (int i = 0; i < monolithBlocks.size(); i++) {
                    double origY = i;
                    double projX = Math.sin(tiltRadians) * origY;
                    double projY = Math.cos(tiltRadians) * origY;
                    Location newPos = monolithBase.clone().add(
                            Math.cos(fallDirection) * projX, projY, Math.sin(fallDirection) * projX);
                    monolithBlocks.get(i).entity().teleport(newPos);
                }

                // Arc particles
                if (collapseTick % 2 == 0) {
                    for (int i = 0; i < monolithHeight; i += 2) {
                        double dist = Math.sin(tiltRadians) * i;
                        Location arcPt = monolithBase.clone().add(
                                Math.cos(fallDirection) * dist, Math.cos(tiltRadians) * i,
                                Math.sin(fallDirection) * dist);
                        w.spawnParticle(Particle.BLOCK, arcPt, 5, 0.3, 0.3, 0.3, 0,
                                Material.PURPUR_BLOCK.createBlockData());
                        w.spawnParticle(Particle.CRIT, arcPt, 3, 0.3, 0.3, 0.3, 0.05);
                    }
                }

                // Ground impact
                if (collapseTick == 20) {
                    Location impactCenter = monolithBase.clone().add(
                            Math.cos(fallDirection) * (monolithHeight / 2.0), 0,
                            Math.sin(fallDirection) * (monolithHeight / 2.0));

                    w.spawnParticle(Particle.BLOCK, impactCenter, 150, 4, 1, 4, 0,
                            Material.PURPUR_BLOCK.createBlockData());
                    w.spawnParticle(Particle.SMOKE, impactCenter, 50, 4, 1, 4, 0.03);
                    DisplayBuilder.dustParticles(impactCenter, 30, 4.0, 200, 100, 255, 2.5f);

                    DisplayBuilder.playSound(impactCenter, Sound.BLOCK_STONE_FALL, 1.0f, 0.2f);

                    // Sweep damage along collapse path
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();

                        for (int seg = 0; seg < monolithHeight; seg++) {
                            Location segLoc = monolithBase.clone().add(
                                    Math.cos(fallDirection) * seg, 0, Math.sin(fallDirection) * seg);
                            if (pl.distanceSquared(segLoc) <= 4.0) {
                                p.damage(14.0); // Capped for design range
                                double knockAngle = Math.atan2(pl.getZ() - monolithBase.getZ(),
                                        pl.getX() - monolithBase.getX());
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(Math.cos(knockAngle) * 0.8, 0.3, Math.sin(knockAngle) * 0.8)));
                                break;
                            }
                        }

                        // Impact zone damage
                        if (pl.distanceSquared(impactCenter) <= 9.0) {
                            p.damage(12.0);
                        }
                    }

                    // Scatter debris
                    for (int i = 0; i < 15; i++) {
                        double da = Math.random() * 2 * Math.PI;
                        double dd = 1 + Math.random() * 5;
                        Location debrisLoc = impactCenter.clone().add(
                                Math.cos(da) * dd, 0.02, Math.sin(da) * dd);
                        Material mat = new Material[]{Material.PURPUR_BLOCK, Material.AMETHYST_BLOCK,
                                Material.OBSIDIAN}[(int) (Math.random() * 3)];
                        BlockDisplayHandle h = displayBuilder.spawnBlock(debrisLoc, mat);
                        h.scale(0.4f, 0.4f, 0.4f).glow(120, 50, 160).interpolation(5, 0);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Debris dust settling
            if (collapseTick > 20 && collapseTick <= 50 && collapseTick % 5 == 0) {
                Location impactCenter = monolithBase.clone().add(
                        Math.cos(fallDirection) * (monolithHeight / 2.0), 0.5,
                        Math.sin(fallDirection) * (monolithHeight / 2.0));
                DisplayBuilder.dustParticles(impactCenter, 4, 3.0, 120, 50, 160, 0.8f);

                // Staggered debris settle sounds
                if (collapseTick % 10 == 0) {
                    DisplayBuilder.playSound(impactCenter, Sound.BLOCK_STONE_PLACE, 0.4f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { /* Collapsed monolith + debris persists */ }

        @Override
        public AbstractAttack newInstance() { return new MonolithCollapseSequence(plugin); }
    }

    // =========================================================================
    // 50. THE SANCTUM'S LAST BREATH -- final environmental event at 5% HP
    // =========================================================================
    public static class TheSanctumsLastBreath extends EnvironmentalAttack {

        private boolean saturationPhase = true;
        private boolean silencePhase = false;

        public TheSanctumsLastBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_sanctums_last_breath", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0); // No damage -- pure atmosphere
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // The final event begins
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // SATURATION PHASE: 40 ticks -- every system fires at maximum
            if (ticksAlive <= 40) {
                // Total particle saturation from all environmental systems
                if (ticksAlive % 1 == 0) {
                    // Brimstone
                    for (int i = 0; i < 10; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        Location pt = center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                        w.spawnParticle(Particle.LAVA, pt, 3, 0.3, 0.5, 0.3, 0);
                        w.spawnParticle(Particle.FLAME, pt, 2, 0.3, 0.5, 0.3, 0.01);
                    }
                    // Crystal
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        w.spawnParticle(Particle.ENCHANT,
                                center.clone().add(Math.cos(a) * d, 1, Math.sin(a) * d),
                                8, 0.5, 1, 0.5, 1.0);
                    }
                    // Void
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        w.spawnParticle(Particle.REVERSE_PORTAL,
                                center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                                5, 0.5, 0.5, 0.5, 0.01);
                    }
                    // Dragon breath
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                            40, 20, 5, 20, 0.01);
                    // Soul fire
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 15;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                                3, 0.3, 0.5, 0.3, 0.01);
                    }
                }

                // Every sound plays at once
                if (ticksAlive == 1) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.4f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.3f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.5f);
                    DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.7f, 0.3f);
                }
                return;
            }

            // SILENCE PHASE: transition in 1 tick
            if (saturationPhase) {
                saturationPhase = false;
                silencePhase = true;

                // The single deactivation tone
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.2f);
            }

            // SILENCE: 60 ticks -- near-complete silence
            if (ticksAlive <= 100) {
                // Only sky stream particles remain (conceptual -- minimal visual)
                if (ticksAlive % 10 == 0) {
                    // Barely-present dust from fixtures
                    for (int i = 0; i < 3; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        DisplayBuilder.dustParticles(
                                center.clone().add(Math.cos(a) * d, 0.3, Math.sin(a) * d),
                                1, 0.3, 100, 0, 100, 0.5f);
                    }
                }
                return;
            }

            // RECOVERY: 100 ticks -- systems slowly resume at 50%
            int recoveryTick = ticksAlive - 100;
            double recoveryRate = Math.min(recoveryTick / 60.0, 0.5);

            if (recoveryTick == 1) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.6f);
            }

            // Gradual cascade from center outward
            if (recoveryTick % 5 == 0) {
                double cascadeRadius = Math.min(recoveryTick * 0.5, 20);
                int particleCount = (int) (recoveryRate * 10);

                for (int i = 0; i < particleCount; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * cascadeRadius;
                    Location pt = center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                    DisplayBuilder.dustParticles(pt, 2, 0.3, 100, 0, 100, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheSanctumsLastBreath(plugin); }
    }
}
