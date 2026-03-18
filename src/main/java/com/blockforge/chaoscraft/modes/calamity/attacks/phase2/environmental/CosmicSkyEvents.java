package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * Phase 2 Environmental — GROUP 2: COSMIC SKY EVENTS
 * 10 attacks that descend or fire from above.
 * Attacks 11-20 from boss2-dog.md.
 *
 * Design notes:
 * - No status effects
 * - DoG crystalline plague palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * - Impact-only damage for falling attacks
 * - Materials: AMETHYST_BLOCK, CYAN_STAINED_GLASS, DARK_PRISMARINE, POLISHED_BLACKSTONE
 */
public final class CosmicSkyEvents {

    private CosmicSkyEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GodShardRain(plugin));
        registry.register(new CosmicDebrisCluster(plugin));
        registry.register(new GodLaserStrike(plugin));
        registry.register(new FallingVoidEye(plugin));
        registry.register(new CrystalSkyFragment(plugin));
        registry.register(new PrismaticBeamArray(plugin));
        registry.register(new OverheadRiftPulse(plugin));
        registry.register(new StarCollapse(plugin));
        registry.register(new MeteoricDogEcho(plugin));
        registry.register(new GodShardSuppression(plugin));
    }

    // =========================================================================
    // 11. GOD SHARD RAIN — 8-12 cyan shards fall from sky, AoE on impact
    // =========================================================================
    public static class GodShardRain extends EnvironmentalAttack {

        private static final int SHARD_COUNT = 10;
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private final List<Location> impactTargets = new ArrayList<>();
        private final boolean[] impacted;

        public GodShardRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_shard_rain", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(3.0);
            config.setDurationTicks(160); // ~8 seconds (fall + linger)
            config.setCooldownTicks(560); // 28 seconds
            impacted = new boolean[SHARD_COUNT];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: metallic shriek 3 seconds before impact
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.2f);

            // Spawn shards at Y+80, random XZ positions around center
            for (int i = 0; i < SHARD_COUNT; i++) {
                double offsetX = (Math.random() - 0.5) * 30;
                double offsetZ = (Math.random() - 0.5) * 30;
                Location target = center.clone().add(offsetX, 0, offsetZ);
                impactTargets.add(target);

                Location spawnLoc = target.clone().add(0, 80, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnLoc, Material.CYAN_STAINED_GLASS);
                h.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(4, 0);
                shardHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shards descend over 120 ticks (6 seconds), staggered start
            for (int i = 0; i < SHARD_COUNT; i++) {
                int shardStartTick = i * 6; // each shard starts 0.3s apart
                int shardAge = ticksAlive - shardStartTick;
                if (shardAge < 0 || impacted[i]) continue;

                // Fall from Y+80 to ground over ~100 ticks
                // Accelerate: slow at top, fast near ground
                float progress = Math.min(1.0f, shardAge / 100.0f);
                float eased = progress * progress; // quadratic acceleration
                double currentY = 80.0 * (1.0 - eased);

                Location shardLoc = impactTargets.get(i).clone().add(0, currentY, 0);
                shardHandles.get(i).entity().teleport(shardLoc);

                // Scale grows as shard approaches ground
                float s = 0.6f + progress * 0.4f;
                shardHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-s / 2, -s / 2, -s / 2),
                        new AxisAngle4f(shardAge * 0.05f, 0, 1, 0),
                        new Vector3f(s, s, s),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                shardHandles.get(i).entity().setInterpolationDelay(0);
                shardHandles.get(i).entity().setInterpolationDuration(3);

                // Trail particles while falling
                if (shardAge % 4 == 0) {
                    DisplayBuilder.cyanDust(shardLoc, 4, 0.3);
                }

                // Impact at ground level
                if (currentY <= 0.5) {
                    impacted[i] = true;
                    Location impactLoc = impactTargets.get(i);

                    // Impact burst
                    DisplayBuilder.cyanDust(impactLoc, 30, 3.0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_STONE_PLACE, 0.7f, 0.6f);

                    // Impact damage — 8 HP (4 hearts) in 3-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(impactLoc) <= 9.0) {
                            p.damage(8.0);
                        }
                    }

                    // Shrink shard on impact
                    shardHandles.get(i).entity().setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.0f, 0.0f, 0.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    shardHandles.get(i).entity().setInterpolationDelay(0);
                    shardHandles.get(i).entity().setInterpolationDuration(10);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodShardRain(plugin); }
    }

    // =========================================================================
    // 12. COSMIC DEBRIS CLUSTER — dense mass of blackstone/amethyst descends, scatters
    // =========================================================================
    public static class CosmicDebrisCluster extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> clusterHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisHandles = new ArrayList<>();
        private Location impactTarget;
        private boolean impactFired = false;

        public CosmicDebrisCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_debris_cluster", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(3.5);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(900); // 45 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            impactTarget = center.clone().add(
                    (Math.random() - 0.5) * 20, 0, (Math.random() - 0.5) * 20);

            // Warning: wrong underwater sound
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);

            // Spawn cluster at Y+60 — 5 block wide mass of mixed blocks
            Material[] mats = {Material.POLISHED_BLACKSTONE, Material.AMETHYST_BLOCK,
                    Material.POLISHED_BLACKSTONE, Material.DARK_PRISMARINE};
            for (int i = 0; i < 8; i++) {
                double ox = (Math.random() - 0.5) * 4;
                double oy = (Math.random() - 0.5) * 3;
                double oz = (Math.random() - 0.5) * 4;
                Location loc = impactTarget.clone().add(ox, 60 + oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i % mats.length]);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                clusterHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Cluster descends over 100 ticks (5 seconds)
            if (ticksAlive <= 100 && !impactFired) {
                float progress = ticksAlive / 100.0f;
                double currentY = 60.0 * (1.0 - progress * progress);

                for (BlockDisplayHandle h : clusterHandles) {
                    Location orig = h.entity().getLocation();
                    Location newLoc = orig.clone();
                    newLoc.setY(impactTarget.getY() + currentY + (Math.random() - 0.5) * 2);
                    h.entity().teleport(newLoc);

                    // Tumble rotation
                    float rot = ticksAlive * 0.08f + clusterHandles.indexOf(h) * 0.5f;
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.4f, -0.4f, -0.4f),
                            new AxisAngle4f(rot, 0.5f, 1, 0.3f),
                            new Vector3f(0.8f, 0.8f, 0.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(3);
                }

                // Trail particles
                if (ticksAlive % 4 == 0) {
                    Location pLoc = impactTarget.clone().add(0, currentY, 0);
                    DisplayBuilder.cyanDust(pLoc, 8, 2.0);
                }
            }

            // Impact
            if (ticksAlive > 100 && !impactFired) {
                impactFired = true;

                // Impact burst + damage
                DisplayBuilder.cyanDust(impactTarget, 60, 5.0);
                DisplayBuilder.playSound(impactTarget, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                DisplayBuilder.playSound(impactTarget, Sound.BLOCK_STONE_PLACE, 1.0f, 0.4f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(impactTarget) <= 12.25) {
                        p.damage(10.0); // 5 hearts direct
                    }
                }

                // Scatter debris outward
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double dist = 4 + Math.random() * 6;
                    Location debrisLoc = impactTarget.clone().add(
                            Math.cos(angle) * dist, 0.5 + Math.random() * 2, Math.sin(angle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(debrisLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(5, 0);
                    debrisHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Remove cluster handles (collapse to zero)
                for (BlockDisplayHandle h : clusterHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }
            }

            // Debris damage — 4 HP (2 hearts) on contact, persists 5 seconds
            if (impactFired && ticksAlive > 100 && ticksAlive <= 200 && ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : debrisHandles) {
                    Location dLoc = h.entity().getLocation();
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(dLoc) <= 2.25) {
                            p.damage(4.0); // 2 hearts debris
                        }
                    }
                }
            }

            // Fade debris
            if (ticksAlive > 160) {
                float fade = Math.max(0f, 1f - (ticksAlive - 160) / 40.0f);
                for (BlockDisplayHandle h : debrisHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.2f * fade, -0.2f * fade, -0.2f * fade),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f * fade, 0.4f * fade, 0.4f * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicDebrisCluster(plugin); }
    }

    // =========================================================================
    // 13. GOD LASER STRIKE — targeting reticle tracks player, then vertical beam fires
    // =========================================================================
    public static class GodLaserStrike extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> reticleHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private Location lockTarget;
        private boolean beamFired = false;

        public GodLaserStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_laser_strike", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds (3s reticle + 2s beam + 2s linger)
            config.setCooldownTicks(640); // 32 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            lockTarget = center.clone();

            // Reticle warning sound
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.7f);

            // Circular reticle on ground — 4-block diameter ring of end rod particles
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.15f, 0.04f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                reticleHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Reticle tracks nearest player for first 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double d = p.getLocation().distanceSquared(center);
                    if (d < nearestDist) {
                        nearestDist = d;
                        nearest = p;
                    }
                }
                if (nearest != null) {
                    lockTarget = nearest.getLocation().clone();
                    lockTarget.setY(center.getY());
                }

                // Move reticle to tracking position
                for (int i = 0; i < reticleHandles.size(); i++) {
                    double angle = (2 * Math.PI * i) / reticleHandles.size() + ticksAlive * 0.05;
                    Location loc = lockTarget.clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2);
                    reticleHandles.get(i).entity().teleport(loc);
                }
            }

            // Phase 2: Locked (tick 30-60), reticle stays fixed but rotates faster
            if (ticksAlive > 30 && ticksAlive <= 60) {
                for (int i = 0; i < reticleHandles.size(); i++) {
                    double angle = (2 * Math.PI * i) / reticleHandles.size() + ticksAlive * 0.15;
                    Location loc = lockTarget.clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2);
                    reticleHandles.get(i).entity().teleport(loc);
                }
                if (ticksAlive % 5 == 0) DisplayBuilder.cyanDust(lockTarget, 8, 2.0);
            }

            // Phase 3: BEAM FIRES at tick 60
            if (ticksAlive == 60 && !beamFired) {
                beamFired = true;

                DisplayBuilder.playSound(lockTarget, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 1.2f);
                DisplayBuilder.playSound(lockTarget, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

                // Vertical beam — column of sea lantern from ground to Y+30
                for (int y = 0; y < 30; y++) {
                    Location bLoc = lockTarget.clone().add(0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(bLoc, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.9f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.cyanDust(lockTarget, 50, 2.0);

                // Damage: 12 HP (6 hearts) in 2-block radius
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(lockTarget) <= 4.0) {
                        p.damage(12.0);
                    }
                }
            }

            // Beam persists and fades
            if (beamFired && ticksAlive > 60 && ticksAlive <= 100) {
                float fade = Math.max(0f, 1f - (ticksAlive - 60) / 40.0f);
                for (BlockDisplayHandle h : beamHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.15f * fade, -0.45f, -0.15f * fade),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.3f * fade, 0.9f, 0.3f * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }
                if (ticksAlive % 5 == 0) DisplayBuilder.cyanDust(lockTarget, 6, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodLaserStrike(plugin); }
    }

    // =========================================================================
    // 14. FALLING VOID EYE — large sphere descends slowly, creates darkness field
    // =========================================================================
    public static class FallingVoidEye extends EnvironmentalAttack {

        private BlockDisplayHandle eyeCore;
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private Location landingTarget;
        private boolean landed = false;

        public FallingVoidEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_void_eye", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(2.0); // 1 heart every 3 seconds in field
            config.setDamageRadius(6.0);
            config.setDurationTicks(420); // ~21 seconds (6s fall + 15s field)
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(60); // every 3 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            landingTarget = center.clone().add(
                    (Math.random() - 0.5) * 20, 0, (Math.random() - 0.5) * 20);

            // Spawn eye at Y+100
            Location eyeLoc = landingTarget.clone().add(0, 100, 0);
            eyeCore = displayBuilder.spawnBlock(eyeLoc, Material.DARK_PRISMARINE);
            eyeCore.scale(2.5f, 2.5f, 2.5f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(eyeCore.entity());

            // Audible drone
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: Y+100 to ground over 120 ticks (6 seconds)
            if (ticksAlive <= 120 && !landed) {
                float progress = ticksAlive / 120.0f;
                double currentY = 100.0 * (1.0 - progress);
                Location eyeLoc = landingTarget.clone().add(0, currentY, 0);
                eyeCore.entity().teleport(eyeLoc);

                // Slow rotation
                float angle = ticksAlive * 0.03f;
                eyeCore.entity().setTransformation(new Transformation(
                        new Vector3f(-1.25f, -1.25f, -1.25f),
                        new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(2.5f, 2.5f, 2.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                eyeCore.entity().setInterpolationDelay(0);
                eyeCore.entity().setInterpolationDuration(4);

                // Trail particles
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(eyeLoc, 10, 1.5, 128, 0, 255, 1.5f);
                }
            }

            // Landing — creates persistent darkness field
            if (ticksAlive > 120 && !landed) {
                landed = true;

                DisplayBuilder.playSound(landingTarget, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
                DisplayBuilder.dustParticles(landingTarget, 40, 3.0, 128, 0, 255, 1.8f);

                // Shrink eye to 1.0 scale at ground
                eyeCore.entity().teleport(landingTarget.clone().add(0, 1.5, 0));
                eyeCore.entity().setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                eyeCore.entity().setInterpolationDelay(0);
                eyeCore.entity().setInterpolationDuration(10);

                // Spawn field indicator ring
                for (int i = 0; i < 12; i++) {
                    double a = (2 * Math.PI * i) / 12;
                    Location loc = landingTarget.clone().add(Math.cos(a) * 6, 0.02, Math.sin(a) * 6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.5f, 0.03f, 0.5f).glow(128, 0, 255).interpolation(4, 0);
                    fieldHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Field active — upward particle column + ambient
            if (landed && ticksAlive % 8 == 0) {
                Location colLoc = landingTarget.clone().add(0, Math.random() * 8, 0);
                DisplayBuilder.dustParticles(colLoc, 6, 2.0, 128, 0, 255, 1.2f);
            }

            // Update center to landing target for radius damage
            if (landed) {
                setCenter(landingTarget);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FallingVoidEye(plugin); }
    }

    // =========================================================================
    // 15. CRYSTAL SKY FRAGMENT — massive 3x3 amethyst block tumbles down, shatters
    // =========================================================================
    public static class CrystalSkyFragment extends EnvironmentalAttack {

        private BlockDisplayHandle fragmentHandle;
        private final List<BlockDisplayHandle> shrapnelHandles = new ArrayList<>();
        private Location targetLoc;
        private boolean shattered = false;

        public CrystalSkyFragment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_sky_fragment", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts direct
            config.setImpactRadius(3.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(800); // 40 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target random player zone
            targetLoc = center.clone().add(
                    (Math.random() - 0.5) * 12, 0, (Math.random() - 0.5) * 12);

            // Warning sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

            // Spawn massive 3x3 amethyst block at Y+50
            Location spawnLoc = targetLoc.clone().add(0, 50, 0);
            fragmentHandle = displayBuilder.spawnBlock(spawnLoc, Material.AMETHYST_BLOCK);
            fragmentHandle.scale(3.0f, 3.0f, 3.0f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(fragmentHandle.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fragment descends with tumbling rotation over 80 ticks (4 seconds)
            if (ticksAlive <= 80 && !shattered) {
                float progress = ticksAlive / 80.0f;
                double currentY = 50.0 * (1.0 - progress * progress);
                Location fragLoc = targetLoc.clone().add(0, currentY, 0);
                fragmentHandle.entity().teleport(fragLoc);

                float rot = ticksAlive * 0.06f;
                fragmentHandle.entity().setTransformation(new Transformation(
                        new Vector3f(-1.5f, -1.5f, -1.5f),
                        new AxisAngle4f(rot, 0.3f, 1, 0.5f),
                        new Vector3f(3.0f, 3.0f, 3.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                fragmentHandle.entity().setInterpolationDelay(0);
                fragmentHandle.entity().setInterpolationDuration(3);

                // Glowing trail
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(fragLoc, 8, 1.5);
                }
            }

            // SHATTER at ground
            if (ticksAlive > 80 && !shattered) {
                shattered = true;

                // Collapse main fragment to 0
                fragmentHandle.entity().setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                fragmentHandle.entity().setInterpolationDelay(0);
                fragmentHandle.entity().setInterpolationDuration(5);

                // Impact effects
                DisplayBuilder.cyanDust(targetLoc, 60, 4.0);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);

                // Direct impact damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetLoc) <= 9.0) {
                        p.damage(8.0); // 4 hearts
                    }
                }

                // 8 shrapnel pieces fly outward
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double dist = 4 + Math.random() * 2;
                    Location shrapLoc = targetLoc.clone().add(
                            Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(shrapLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(4, 0);
                    shrapnelHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Shrapnel damage + fade
            if (shattered && ticksAlive > 80 && ticksAlive <= 160) {
                // Check shrapnel contact
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle h : shrapnelHandles) {
                        Location sLoc = h.entity().getLocation();
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(sLoc) <= 2.25) {
                                p.damage(4.0); // 2 hearts shrapnel
                            }
                        }
                    }
                }

                // Fade shrapnel
                if (ticksAlive > 120) {
                    float fade = Math.max(0f, 1f - (ticksAlive - 120) / 40.0f);
                    for (BlockDisplayHandle h : shrapnelHandles) {
                        h.entity().setTransformation(new Transformation(
                                new Vector3f(-0.4f * fade, -0.4f * fade, -0.4f * fade),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.8f * fade, 0.8f * fade, 0.8f * fade),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        h.entity().setInterpolationDelay(0);
                        h.entity().setInterpolationDuration(5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSkyFragment(plugin); }
    }

    // =========================================================================
    // 16. PRISMATIC BEAM ARRAY — three simultaneous targeting beams from sky
    // =========================================================================
    public static class PrismaticBeamArray extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private final Location[] targets = new Location[3];
        private boolean beamsFired = false;

        public PrismaticBeamArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prismatic_beam_array", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three targeting positions spread across island
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 + Math.random() * 0.5;
                double dist = 5 + Math.random() * 10;
                targets[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Descending indicator line
                for (int y = 20; y >= 0; y -= 2) {
                    Location loc = targets[i].clone().add(0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                    h.scale(0.1f, 1.5f, 0.1f).glow(0, 200, 255).interpolation(3, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(targets[i], Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: indicators pulse for 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    for (Location t : targets) {
                        if (t != null) DisplayBuilder.cyanDust(t, 8, 1.5);
                    }
                }
                return;
            }

            // ALL THREE BEAMS FIRE at tick 60
            if (ticksAlive == 61 && !beamsFired) {
                beamsFired = true;

                for (Location t : targets) {
                    if (t == null) continue;

                    DisplayBuilder.playSound(t, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                    DisplayBuilder.cyanDust(t, 40, 2.0);

                    // 10 HP (5 hearts) per beam
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(t) <= 6.25) { // 2.5 block radius
                            p.damage(10.0);
                        }
                    }
                }

                // Intensify beam visuals
                for (BlockDisplayHandle h : beamHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.15f, -0.75f, -0.15f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.3f, 1.5f, 0.3f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(3);
                    h.entity().setGlowColorOverride(Color.fromRGB(240, 240, 255));
                }
            }

            // Beams persist and fade (tick 61-90, ~1.5 seconds)
            if (beamsFired && ticksAlive > 60 && ticksAlive <= 90) {
                float fade = Math.max(0f, 1f - (ticksAlive - 60) / 30.0f);
                for (BlockDisplayHandle h : beamHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.15f * fade, -0.75f, -0.15f * fade),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.3f * fade, 1.5f * fade, 0.3f * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismaticBeamArray(plugin); }
    }

    // =========================================================================
    // 17. OVERHEAD RIFT PULSE — sky distortion descends as damage wave
    // =========================================================================
    public static class OverheadRiftPulse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private double waveY = 50;

        public OverheadRiftPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("overhead_rift_pulse", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually by Y position
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds (2s warning + 4s descent)
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sky shimmer at Y+50
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.5f);

            // Descending wave — wide flat panel
            for (int x = -12; x <= 12; x += 4) {
                for (int z = -12; z <= 12; z += 4) {
                    Location loc = center.clone().add(x, 50, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    h.scale(3.5f, 0.1f, 3.5f).glow(0, 200, 255).interpolation(4, 0);
                    waveHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning shimmer: 0-40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 50, 0), 20, 12.0);
                }
                return;
            }

            // Wave descends from Y+50 to Y-5 over 80 ticks (4 seconds)
            waveY = 50.0 - ((ticksAlive - 40) / 80.0) * 55.0;

            for (BlockDisplayHandle h : waveHandles) {
                Location loc = h.entity().getLocation();
                loc.setY(center.getY() + waveY);
                h.entity().teleport(loc);
            }

            // Damage players at the wave's current Y level
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double playerY = p.getLocation().getY();
                    double centerY = center.getY() + waveY;
                    if (Math.abs(playerY - centerY) <= 2.0) {
                        double hDist = Math.sqrt(
                                Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                        if (hDist <= 15.0) {
                            p.damage(6.0); // 3 hearts
                        }
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, waveY, 0), 15, 10.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OverheadRiftPulse(plugin); }
    }

    // =========================================================================
    // 18. STAR COLLAPSE — expanding light point detonates into radial projectiles
    // =========================================================================
    public static class StarCollapse extends EnvironmentalAttack {

        private BlockDisplayHandle starCore;
        private final List<BlockDisplayHandle> projectileHandles = new ArrayList<>();
        private boolean detonated = false;

        public StarCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_collapse", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds (5s expand + 1s collapse + travel)
            config.setCooldownTicks(960); // 48 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Star core at Y+80
            Location starLoc = center.clone().add(0, 80, 0);
            starCore = displayBuilder.spawnBlock(starLoc, Material.SEA_LANTERN);
            starCore.scale(0.1f, 0.1f, 0.1f).glow(240, 240, 255).interpolation(5, 0);
            spawnedEntities.add(starCore.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location starLoc = center.clone().add(0, 80, 0);

            // Phase 1: Expand from point to 2-block sphere over 100 ticks (5 seconds)
            if (ticksAlive <= 100) {
                float scale = (ticksAlive / 100.0f) * 2.0f;
                starCore.entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                starCore.entity().setInterpolationDelay(0);
                starCore.entity().setInterpolationDuration(5);

                // Rising pitch sound
                if (ticksAlive % 20 == 0) {
                    float pitch = 0.6f + ticksAlive * 0.008f;
                    DisplayBuilder.playSound(starLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, pitch);
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(starLoc, 6, scale, 240, 240, 255, 1.0f);
                }
            }

            // Phase 2: Collapse (tick 100-120) — shrink back to point rapidly
            if (ticksAlive > 100 && ticksAlive <= 120) {
                float scale = Math.max(0.1f, 2.0f * (1f - (ticksAlive - 100) / 20.0f));
                starCore.entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                starCore.entity().setInterpolationDelay(0);
                starCore.entity().setInterpolationDuration(2);
            }

            // Phase 3: DETONATION at tick 120
            if (ticksAlive == 120 && !detonated) {
                detonated = true;

                DisplayBuilder.dustParticles(starLoc, 80, 5.0, 240, 240, 255, 1.5f);
                DisplayBuilder.playSound(starLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                // Fire 20 radial projectiles downward toward island
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20;
                    Location pLoc = starLoc.clone();
                    BlockDisplayHandle h = displayBuilder.spawnBlock(pLoc, Material.AMETHYST_CLUSTER);
                    h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    projectileHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Phase 4: Projectiles travel outward and downward
            if (detonated && ticksAlive > 120 && ticksAlive <= 150) {
                float travelProgress = (ticksAlive - 120) / 30.0f;
                for (int i = 0; i < projectileHandles.size(); i++) {
                    double angle = (2 * Math.PI * i) / projectileHandles.size();
                    double dist = travelProgress * 15.0;
                    double dropY = 80.0 - travelProgress * 80.0;
                    Location pLoc = center.clone().add(
                            Math.cos(angle) * dist, dropY, Math.sin(angle) * dist);
                    projectileHandles.get(i).entity().teleport(pLoc);
                }

                // Check projectile damage: 6 HP (3 hearts)
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle h : projectileHandles) {
                        Location pLoc = h.entity().getLocation();
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(pLoc) <= 4.0) {
                                p.damage(6.0);
                                DisplayBuilder.cyanDust(pLoc, 6, 0.3);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarCollapse(plugin); }
    }

    // =========================================================================
    // 19. METEORIC DOG ECHO — ghost worm head dives through island
    // =========================================================================
    public static class MeteoricDogEcho extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> echoHandles = new ArrayList<>();
        private Location entryPoint;
        private Location exitPoint;

        public MeteoricDogEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteoric_dog_echo", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage on path intersection
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // ~3 seconds (1s warning + 2s travel)
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random entry angle from above
            double angle = Math.random() * 2 * Math.PI;
            entryPoint = center.clone().add(Math.cos(angle) * 15, 30, Math.sin(angle) * 15);
            exitPoint = center.clone().add(-Math.cos(angle) * 15, -5, -Math.sin(angle) * 15);

            // Faint movement sound warning
            DisplayBuilder.playSound(entryPoint, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.5f);

            // Spawn echo segments — worm head silhouette in amethyst
            for (int seg = 0; seg < 6; seg++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(entryPoint, Material.AMETHYST_BLOCK);
                float size = 1.5f - seg * 0.15f;
                h.scale(size, size, size).glow(0, 200, 255).interpolation(2, 0);
                echoHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1 second)
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) DisplayBuilder.cyanDust(entryPoint, 8, 1.5);
                return;
            }

            // Travel phase: 40 ticks (2 seconds) — echo moves from entry to exit
            int travelTick = ticksAlive - 20;
            float travelProgress = Math.min(1.0f, travelTick / 40.0f);

            // Move each segment along the path, staggered
            for (int seg = 0; seg < echoHandles.size(); seg++) {
                float segProgress = Math.max(0f, Math.min(1f, travelProgress - seg * 0.05f));
                double x = entryPoint.getX() + (exitPoint.getX() - entryPoint.getX()) * segProgress;
                double y = entryPoint.getY() + (exitPoint.getY() - entryPoint.getY()) * segProgress;
                double z = entryPoint.getZ() + (exitPoint.getZ() - entryPoint.getZ()) * segProgress;

                Location segLoc = new Location(w, x, y, z);
                echoHandles.get(seg).entity().teleport(segLoc);

                // Trail particles
                if (travelTick % 3 == 0) {
                    DisplayBuilder.cyanDust(segLoc, 4, 0.5);
                }
            }

            // Damage players in the path
            if (travelTick % 4 == 0) {
                Location headLoc = echoHandles.get(0).entity().getLocation();
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(headLoc) <= 6.25) { // 2.5 block radius
                        p.damage(8.0); // 4 hearts
                        DisplayBuilder.cyanDust(p.getLocation(), 10, 0.5);
                    }
                }
            }

            // Fade out near end
            if (travelProgress > 0.8f) {
                float fade = Math.max(0f, (1f - travelProgress) / 0.2f);
                for (BlockDisplayHandle h : echoHandles) {
                    float size = 1.5f * fade;
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-size / 2, -size / 2, -size / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(size, size, size),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MeteoricDogEcho(plugin); }
    }

    // =========================================================================
    // 20. GOD SHARD SUPPRESSION — coordinated 5-shard barrage in 10x10 zone
    // =========================================================================
    public static class GodShardSuppression extends EnvironmentalAttack {

        private static final int SHARD_COUNT = 5;
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> zoneIndicator = new ArrayList<>();
        private final Location[] shardTargets = new Location[SHARD_COUNT];
        private Location zoneCenter;
        private final boolean[] impacted = new boolean[SHARD_COUNT];
        private boolean zoneFlashed = false;

        public GodShardSuppression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_shard_suppression", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts per shard
            config.setImpactRadius(3.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1200); // 60 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Zone center offset from arena center
            zoneCenter = center.clone().add(
                    (Math.random() - 0.5) * 16, 0, (Math.random() - 0.5) * 16);

            // Warning: thunder + zone indicator ring
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.4f);

            // Zone indicator: 10x10 block ring
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                Location loc = zoneCenter.clone().add(Math.cos(angle) * 5, 0.05, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.4f, 0.03f, 0.4f).glow(240, 240, 255).interpolation(3, 0);
                zoneIndicator.add(h);
                spawnedEntities.add(h.entity());
            }

            // Generate 5 shard targets within the zone
            for (int i = 0; i < SHARD_COUNT; i++) {
                shardTargets[i] = zoneCenter.clone().add(
                        (Math.random() - 0.5) * 8, 0, (Math.random() - 0.5) * 8);

                Location spawnLoc = shardTargets[i].clone().add(0, 70, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnLoc, Material.CYAN_STAINED_GLASS);
                h.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                shardHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Zone flash warning at tick 40 (2 seconds)
            if (ticksAlive == 40 && !zoneFlashed) {
                zoneFlashed = true;
                DisplayBuilder.dustParticles(zoneCenter, 30, 5.0, 240, 240, 255, 1.0f);
            }

            // Zone indicator pulses
            if (ticksAlive < 60 && ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(zoneCenter, 12, 5.0, 240, 240, 255, 0.8f);
            }

            // Shards begin falling at tick 60 (3 seconds warning), staggered
            for (int i = 0; i < SHARD_COUNT; i++) {
                int shardStartTick = 60 + i * 16; // each shard 0.8s apart
                int shardAge = ticksAlive - shardStartTick;
                if (shardAge < 0 || impacted[i]) continue;

                float progress = Math.min(1.0f, shardAge / 60.0f);
                float eased = progress * progress;
                double currentY = 70.0 * (1.0 - eased);

                Location shardLoc = shardTargets[i].clone().add(0, currentY, 0);
                shardHandles.get(i).entity().teleport(shardLoc);

                float rot = shardAge * 0.06f;
                shardHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.3f, -0.3f),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                shardHandles.get(i).entity().setInterpolationDelay(0);
                shardHandles.get(i).entity().setInterpolationDuration(3);

                if (shardAge % 4 == 0) {
                    DisplayBuilder.cyanDust(shardLoc, 3, 0.3);
                }

                // Impact
                if (currentY <= 0.5) {
                    impacted[i] = true;
                    Location impactLoc = shardTargets[i];

                    DisplayBuilder.cyanDust(impactLoc, 25, 3.0);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.5f);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(impactLoc) <= 9.0) {
                            p.damage(8.0); // 4 hearts
                        }
                    }

                    // Scale to 0
                    shardHandles.get(i).entity().setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    shardHandles.get(i).entity().setInterpolationDelay(0);
                    shardHandles.get(i).entity().setInterpolationDuration(8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodShardSuppression(plugin); }
    }
}
