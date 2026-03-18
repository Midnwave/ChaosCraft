package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Mini Boss 2 — THE CRYSTAL WARDEN
 * A fragment of the Devourer of Gods' crystalline plague consciousness.
 * Amethyst crystal humanoid torso, floating, arm-crystal weapons.
 * Cyan/purple crystal theme. Unique Mechanic: Crystal Counter (3 shards on every hit).
 * 18 attacks total.
 * NO status effects — damage only.
 */
public final class CrystalWardenAttacks {

    private CrystalWardenAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShardJavelin(plugin));
        registry.register(new CrystalWall(plugin));
        registry.register(new CrystalPrison(plugin));
        registry.register(new ChainLightning(plugin));
        registry.register(new AmethystRain(plugin));
        registry.register(new ReflectiveCarapace(plugin));
        registry.register(new ShardBurstStep(plugin));
        registry.register(new GeodeSlam(plugin));
        registry.register(new CrystalThorns(plugin));
        registry.register(new VoidCrystalFusion(plugin));
        registry.register(new ResonantFrequency(plugin));
        registry.register(new TwinCrystalSweep(plugin));
        registry.register(new CrystallineMirage(plugin));
        registry.register(new SpireEruption(plugin));
        registry.register(new CrystalAnchorField(plugin));
        registry.register(new PrismCannon(plugin));
        registry.register(new CrystallineDeathBurst(plugin));
        registry.register(new DualSummonCoordination(plugin));
    }

    // ================================================================
    // 1. SHARD JAVELIN — Large crystal projectile, pierces first target
    // ================================================================
    public static class ShardJavelin extends BossAttack {
        private final List<BlockDisplayHandle> javelinHandles = new ArrayList<>();
        private boolean javelinFired = false;
        private int fireStartTick = 0;

        public ShardJavelin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_shard_javelin", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(140);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm-crystal extends forward, pointing at target
            BlockDisplayHandle armCrystal = displayBuilder.spawnBlock(
                center.clone().add(1.5, 3, 0), Material.AMETHYST_BLOCK);
            armCrystal.scale(0.7f, 0.7f, 2.5f).glow(180, 0, 255).interpolation(3, 0);
            javelinHandles.add(armCrystal);
            spawnedEntities.add(armCrystal.entity());
            DisplayBuilder.cyanDust(center.clone().add(1.5, 3, 0), 8, 1.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge telegraph (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24 && !javelinFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(1.5, 3, 0), 5, 1.0);
                }
            }
            // Javelin fires (tick 24)
            else if (ticksAlive == 24 && !javelinFired) {
                javelinFired = true;
                fireStartTick = ticksAlive;
                // Create javelin projectile segments
                for (int i = 0; i < 18; i++) {
                    Location segLoc = center.clone().add(1.5 + i * 1.0, 3, 0);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_CLUSTER);
                    seg.scale(0.15f, 0.15f, 0.5f).glow(180, 0, 255).interpolation(1, 0);
                    javelinHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 1.0f, 0.7f);
            }
            // Javelin travels (24-44 ticks)
            else if (javelinFired && ticksAlive - fireStartTick < 20) {
                float travel = (ticksAlive - fireStartTick) * 1.0f;
                for (int i = 1; i < javelinHandles.size(); i++) {
                    int idx = i - 1;
                    javelinHandles.get(i).entity().teleport(
                        center.clone().add(1.5 + idx + travel, 3, 0));
                }
            }
            // Javelin shatters at max range (tick 44)
            else if (javelinFired && ticksAlive - fireStartTick == 20) {
                // 6 small shards spray from impact
                Location impactLoc = center.clone().add(20, 3, 0);
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(-45 + i * 18);
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(
                        impactLoc.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2),
                        Material.AMETHYST_CLUSTER);
                    frag.scale(0.15f, 0.15f, 0.3f).glow(180, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(frag.entity());
                }
                DisplayBuilder.cyanDust(impactLoc, 15, 3.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_ARROW_HIT, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardJavelin(plugin); }
    }

    // ================================================================
    // 2. CRYSTAL WALL — Amethyst panel wall erupts from ground
    // ================================================================
    public static class CrystalWall extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean wallErupted = false;

        public CrystalWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystal_wall", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Line of amethyst buds on ground — 3-block telegraph
            for (int i = -1; i <= 1; i++) {
                Location budLoc = center.clone().add(i, 0.05, 4);
                BlockDisplayHandle bud = displayBuilder.spawnBlock(budLoc, Material.SMALL_AMETHYST_BUD);
                bud.scale(0.5f, 0.3f, 0.5f).glow(180, 0, 255).interpolation(3, 0);
                wallHandles.add(bud);
                spawnedEntities.add(bud.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph glow (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !wallErupted) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 4), 6, 2.0);
                }
            }
            // Wall erupts (tick 30)
            else if (ticksAlive == 30 && !wallErupted) {
                wallErupted = true;
                // 5 crystal panels rise to 4-block height
                for (int i = -2; i <= 2; i++) {
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle panel = displayBuilder.spawnBlock(
                            center.clone().add(i, y, 4), Material.AMETHYST_BLOCK);
                        panel.scale(0.9f, 0.9f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                        wallHandles.add(panel);
                        spawnedEntities.add(panel.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
            }
            // Wall active ambient (30-270 ticks = 12 seconds)
            else if (wallErupted && ticksAlive < 270) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 4), 4, 3.0);
                }
            }
            // Wall retracts (tick 270)
            else if (wallErupted && ticksAlive == 270) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalWall(plugin); }
    }

    // ================================================================
    // 3. CRYSTAL PRISON — Cage formation trapping one player
    // ================================================================
    public static class CrystalPrison extends BossAttack {
        private final List<BlockDisplayHandle> prisonHandles = new ArrayList<>();
        private boolean prisonActive = false;

        public CrystalPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystal_prison", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Targeting ring at player's feet
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(ringPt, Material.AMETHYST_CLUSTER);
                seg.scale(0.3f, 0.05f, 0.3f).glow(180, 0, 255).interpolation(3, 0);
                prisonHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring pulses twice (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !prisonActive) {
                if (ticksAlive == 10 || ticksAlive == 20) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 2.0);
                }
            }
            // Prison rises (tick 30)
            else if (ticksAlive == 30 && !prisonActive) {
                prisonActive = true;
                // 8 cage panels — 4 sides + partial top
                for (int side = 0; side < 4; side++) {
                    double angle = (Math.PI / 2) * side;
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle bar = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 1.5, y, Math.sin(angle) * 1.5),
                            Material.AMETHYST_BLOCK);
                        bar.scale(0.3f, 0.9f, 1.5f).glow(180, 0, 255).interpolation(2, 0);
                        prisonHandles.add(bar);
                        spawnedEntities.add(bar.entity());
                    }
                }
                // Partial top
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i + Math.PI / 4;
                    BlockDisplayHandle top = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 0.8, 3, Math.sin(angle) * 0.8),
                        Material.AMETHYST_CLUSTER);
                    top.scale(0.8f, 0.3f, 0.8f).glow(180, 0, 255).interpolation(2, 0);
                    prisonHandles.add(top);
                    spawnedEntities.add(top.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            }
            // Prison active (30-110 ticks = 4 seconds)
            else if (prisonActive && ticksAlive < 110) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 4, 1.5);
                }
            }
            // Prison shatters (tick 110)
            else if (prisonActive && ticksAlive == 110) {
                // 8 crystal shards spray outward
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 1.5, Math.sin(angle) * 3),
                        Material.AMETHYST_CLUSTER);
                    shard.scale(0.2f, 0.2f, 0.4f).glow(180, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalPrison(plugin); }
    }

    // ================================================================
    // 4. CHAIN LIGHTNING — Crystal bolt that chains between players
    // ================================================================
    public static class ChainLightning extends BossAttack {
        private final List<BlockDisplayHandle> lightningHandles = new ArrayList<>();
        private boolean lightningFired = false;

        public ChainLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_chain_lightning", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm-crystals extend horizontally, arcing between them
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                center.clone().add(-2, 3, 0), Material.AMETHYST_BLOCK);
            leftArm.scale(0.5f, 0.5f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            lightningHandles.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                center.clone().add(2, 3, 0), Material.AMETHYST_BLOCK);
            rightArm.scale(0.5f, 0.5f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            lightningHandles.add(rightArm);
            spawnedEntities.add(rightArm.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Buildup (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !lightningFired) {
                if (ticksAlive % 6 == 0) {
                    // Arc between arms
                    for (int seg = 0; seg < 5; seg++) {
                        float t = seg / 4.0f;
                        Location arcLoc = center.clone().add(-2 + 4 * t, 3 + (float)(Math.random() * 0.5), 0);
                        DisplayBuilder.cyanDust(arcLoc, 2, 0.3);
                    }
                }
            }
            // Lightning fires (tick 36)
            else if (ticksAlive == 36 && !lightningFired) {
                lightningFired = true;
                // Chain bolt segments — primary target direction
                for (int seg = 0; seg < 14; seg++) {
                    Location boltLoc = center.clone().add(0, 3, seg * 1.0);
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.SEA_LANTERN);
                    bolt.scale(0.1f, 0.1f, 0.6f).glow(0, 200, 255).interpolation(0, 0);
                    lightningHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                }
                // Chain jump segments (perpendicular)
                for (int jump = 0; jump < 8; jump++) {
                    Location jumpLoc = center.clone().add(jump * 0.8 - 3, 3, 14);
                    BlockDisplayHandle chain = displayBuilder.spawnBlock(jumpLoc, Material.SEA_LANTERN);
                    chain.scale(0.6f, 0.08f, 0.08f).glow(0, 200, 255).interpolation(0, 0);
                    lightningHandles.add(chain);
                    spawnedEntities.add(chain.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 7), 20, 7.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.8f);
            }
            // Bolt fades (36-46 ticks)
            else if (lightningFired && ticksAlive < 46) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 7), 5, 4.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainLightning(plugin); }
    }

    // ================================================================
    // 5. AMETHYST RAIN — 20 crystal shards fall from cloud above
    // ================================================================
    public static class AmethystRain extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private boolean rainFalling = false;
        private int rainStartTick = 0;

        public AmethystRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_amethyst_rain", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Glittering cloud 10 blocks above
            for (int i = 0; i < 6; i++) {
                double ox = (Math.random() - 0.5) * 8;
                double oz = (Math.random() - 0.5) * 8;
                BlockDisplayHandle cloudSeg = displayBuilder.spawnBlock(
                    center.clone().add(ox, 10, oz), Material.AMETHYST_CLUSTER);
                cloudSeg.scale(1.5f, 0.3f, 1.5f).glow(180, 0, 255).interpolation(3, 0);
                rainHandles.add(cloudSeg);
                spawnedEntities.add(cloudSeg.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 15, 6.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Cloud forms (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !rainFalling) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 8, 6.0);
                }
            }
            // Rain begins (tick 40) — 20 shards staggered
            else if (ticksAlive == 40 && !rainFalling) {
                rainFalling = true;
                rainStartTick = ticksAlive;
                // Create all 20 shard projectiles at cloud altitude
                for (int i = 0; i < 20; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        center.clone().add(ox, 10, oz), Material.AMETHYST_CLUSTER);
                    shard.scale(0.2f, 0.4f, 0.2f).glow(180, 0, 255).interpolation(1, 0);
                    rainHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.2f);
            }
            // Shards fall staggered (40-64 ticks = 1.2 seconds)
            else if (rainFalling && ticksAlive - rainStartTick < 24) {
                int elapsed = ticksAlive - rainStartTick;
                for (int i = 6; i < rainHandles.size(); i++) {
                    int shardIdx = i - 6;
                    int shardDelay = (shardIdx * 24) / 20;
                    if (elapsed >= shardDelay) {
                        float drop = (elapsed - shardDelay) * 0.5f;
                        Location loc = rainHandles.get(i).entity().getLocation();
                        rainHandles.get(i).entity().teleport(
                            loc.clone().add(0, -drop, 0));
                    }
                }
            }
            // Shards embed on ground (tick 64)
            else if (rainFalling && ticksAlive - rainStartTick == 24) {
                // Ground embed tiles
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(
                        center.clone().add(ox, 0.05, oz), Material.AMETHYST_CLUSTER);
                    tile.scale(0.5f, 0.1f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                    spawnedEntities.add(tile.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystRain(plugin); }
    }

    // ================================================================
    // 6. REFLECTIVE CARAPACE — Full-body reflect for 3 seconds
    // ================================================================
    public static class ReflectiveCarapace extends BossAttack {
        private final List<BlockDisplayHandle> carapaceHandles = new ArrayList<>();
        private boolean carapaceActive = false;
        private int carapaceStartTick = 0;

        public ReflectiveCarapace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_reflective_carapace", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal surfaces refract light outward
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location beamDir = center.clone().add(Math.cos(angle) * 4, 3, Math.sin(angle) * 4);
                BlockDisplayHandle refractBeam = displayBuilder.spawnBlock(beamDir, Material.END_ROD);
                refractBeam.scale(0.08f, 0.08f, 2.0f).glow(180, 0, 255).interpolation(2, 0);
                carapaceHandles.add(refractBeam);
                spawnedEntities.add(refractBeam.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Glow buildup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !carapaceActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 2.0);
                }
            }
            // Carapace activates (tick 20)
            else if (ticksAlive == 20 && !carapaceActive) {
                carapaceActive = true;
                carapaceStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f);
            }
            // Active (20-80 ticks = 3 seconds)
            else if (carapaceActive && ticksAlive - carapaceStartTick < 60) {
                if (ticksAlive % 6 == 0) {
                    // Rotate refraction beams
                    float rot = (ticksAlive - carapaceStartTick) * 0.03f;
                    for (int i = 0; i < carapaceHandles.size(); i++) {
                        double angle = (Math.PI * 2 / 8) * i + rot;
                        carapaceHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * 4, 3, Math.sin(angle) * 4));
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 4, 3.0);
                }
            }
            // Carapace fades (tick 80)
            else if (carapaceActive && ticksAlive - carapaceStartTick == 60) {
                carapaceActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 1.0f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ReflectiveCarapace(plugin); }
    }

    // ================================================================
    // 7. SHARD BURST STEP — Dash through players spraying crystals
    // ================================================================
    public static class ShardBurstStep extends BossAttack {
        private final List<BlockDisplayHandle> burstHandles = new ArrayList<>();
        private boolean dashActive = false;
        private int dashStartTick = 0;

        public ShardBurstStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_shard_burst_step", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal trail intensifies around the Warden
            BlockDisplayHandle trailCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.AMETHYST_BLOCK);
            trailCore.scale(1.0f, 2.0f, 1.0f).glow(180, 0, 255).interpolation(2, 0);
            burstHandles.add(trailCore);
            spawnedEntities.add(trailCore.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Buildup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !dashActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 1.5);
                }
            }
            // Dash fires (tick 20)
            else if (ticksAlive == 20 && !dashActive) {
                dashActive = true;
                dashStartTick = ticksAlive;
                // Dash trail — 12 blocks of crystal debris
                for (int i = 0; i < 12; i++) {
                    Location trailLoc = center.clone().add(0, 1.5, i);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.AMETHYST_CLUSTER);
                    trail.scale(0.3f, 0.3f, 0.3f).glow(180, 0, 255).interpolation(1, 0);
                    burstHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                // Perpendicular spray shards
                for (int i = 0; i < 6; i++) {
                    Location sprayLoc = center.clone().add(
                        (Math.random() - 0.5) * 6, 2, (Math.random()) * 12);
                    BlockDisplayHandle spray = displayBuilder.spawnBlock(sprayLoc, Material.AMETHYST_CLUSTER);
                    spray.scale(0.15f, 0.15f, 0.3f).glow(180, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(spray.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 6), 20, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 1.0f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardBurstStep(plugin); }
    }

    // ================================================================
    // 8. GEODE SLAM — Dual arm-crystal overhead slam with shockwave
    // ================================================================
    public static class GeodeSlam extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private boolean slamDetonated = false;

        public GeodeSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_geode_slam", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Both arm-crystals rise overhead
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                center.clone().add(-1, 5, 0), Material.AMETHYST_BLOCK);
            leftArm.scale(0.6f, 2.0f, 0.6f).glow(180, 0, 255).interpolation(3, 0);
            slamHandles.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                center.clone().add(1, 5, 0), Material.AMETHYST_BLOCK);
            rightArm.scale(0.6f, 2.0f, 0.6f).glow(180, 0, 255).interpolation(3, 0);
            slamHandles.add(rightArm);
            spawnedEntities.add(rightArm.entity());

            DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Windup (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !slamDetonated) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, 2.0);
                }
            }
            // Slam down (tick 40)
            else if (ticksAlive == 40 && !slamDetonated) {
                slamDetonated = true;
                // Arms slam to ground level
                slamHandles.get(0).entity().teleport(center.clone().add(-1, 0, 0));
                slamHandles.get(1).entity().teleport(center.clone().add(1, 0, 0));

                // Shockwave ring
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3),
                        Material.AMETHYST_CLUSTER);
                    wave.scale(0.8f, 0.3f, 0.8f).glow(180, 0, 255).interpolation(1, 0);
                    slamHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }

                // Cracked ground
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(
                            center.clone().add(dx, 0.03, dz), Material.CRACKED_DEEPSLATE_TILES);
                        crack.scale(0.9f, 0.05f, 0.9f).glow(80, 0, 160).interpolation(2, 0);
                        spawnedEntities.add(crack.entity());
                    }
                }

                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 30, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GeodeSlam(plugin); }
    }

    // ================================================================
    // 9. CRYSTAL THORNS — Surface spikes punish melee attackers
    // ================================================================
    public static class CrystalThorns extends BossAttack {
        private final List<BlockDisplayHandle> thornHandles = new ArrayList<>();
        private boolean thornsActive = false;
        private int thornStartTick = 0;

        public CrystalThorns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystal_thorns", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal growths sprouting from body surface
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 / 10) * i;
                float height = 1.5f + (float)(Math.random() * 2.0);
                Location thornLoc = center.clone().add(
                    Math.cos(angle) * 1.2, height, Math.sin(angle) * 1.2);
                BlockDisplayHandle thorn = displayBuilder.spawnBlock(thornLoc, Material.AMETHYST_CLUSTER);
                thorn.scale(0.2f, 0.5f, 0.2f).glow(180, 0, 255).interpolation(2, 0);
                thornHandles.add(thorn);
                spawnedEntities.add(thorn.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Growth buildup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !thornsActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 1.5);
                }
            }
            // Thorns activate (tick 20)
            else if (ticksAlive == 20 && !thornsActive) {
                thornsActive = true;
                thornStartTick = ticksAlive;
                // Thorns extend outward
                for (BlockDisplayHandle thorn : thornHandles) {
                    Location loc = thorn.entity().getLocation();
                    thorn.entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, 0, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.8f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 0.6f);
            }
            // Thorns active (20-180 ticks = 8 seconds)
            else if (thornsActive && ticksAlive - thornStartTick < 160) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 4, 1.5);
                }
            }
            // Thorns retract (tick 180)
            else if (thornsActive && ticksAlive - thornStartTick == 160) {
                thornsActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalThorns(plugin); }
    }

    // ================================================================
    // 10. VOID CRYSTAL FUSION — Void-infused javelin (if Void Shard dead)
    // ================================================================
    public static class VoidCrystalFusion extends BossAttack {
        private final List<BlockDisplayHandle> fusionHandles = new ArrayList<>();
        private boolean fusionFired = false;

        public VoidCrystalFusion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_void_crystal_fusion", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Channeling void energy into crystal structure
            BlockDisplayHandle fusionCore = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.AMETHYST_BLOCK);
            fusionCore.scale(1.0f, 1.0f, 1.0f).glow(100, 0, 200).interpolation(3, 0);
            fusionHandles.add(fusionCore);
            spawnedEntities.add(fusionCore.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fusionFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 2.0);
                }
            }
            // Fusion javelin fires (tick 40)
            else if (ticksAlive == 40 && !fusionFired) {
                fusionFired = true;
                // Larger, darker javelin
                for (int i = 0; i < 16; i++) {
                    Location segLoc = center.clone().add(0, 3, i * 1.2);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.CRYING_OBSIDIAN);
                    seg.scale(0.2f, 0.2f, 0.8f).glow(120, 0, 200).interpolation(1, 0);
                    fusionHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.2f);
            }
            // Impact creates temp void zone (tick 50)
            else if (fusionFired && ticksAlive == 50) {
                Location impactLoc = center.clone().add(0, 0, 19);
                for (int dx = -1; dx <= 0; dx++) {
                    for (int dz = -1; dz <= 0; dz++) {
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(
                            impactLoc.clone().add(dx, 0.03, dz), Material.CRYING_OBSIDIAN);
                        zone.scale(0.9f, 0.06f, 0.9f).glow(120, 0, 200).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                    }
                }
                DisplayBuilder.cyanDust(impactLoc, 15, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCrystalFusion(plugin); }
    }

    // ================================================================
    // 11. RESONANT FREQUENCY — AoE vibration aura with armor debuff
    // ================================================================
    public static class ResonantFrequency extends BossAttack {
        private final List<BlockDisplayHandle> freqHandles = new ArrayList<>();
        private boolean frequencyActive = false;
        private int freqStartTick = 0;

        public ResonantFrequency(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_resonant_frequency", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(640);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal body begins humming — visible vibration
            BlockDisplayHandle vibrateCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2.5, 0), Material.AMETHYST_BLOCK);
            vibrateCore.scale(1.5f, 2.0f, 1.5f).glow(180, 0, 255).interpolation(2, 0);
            freqHandles.add(vibrateCore);
            spawnedEntities.add(vibrateCore.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vibration buildup (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !frequencyActive) {
                // Oscillate position
                float jitter = (float) Math.sin(ticksAlive * 0.8) * 0.15f;
                freqHandles.get(0).entity().teleport(
                    center.clone().add(jitter, 2.5, jitter));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 6, 3.0);
                }
            }
            // Frequency activates (tick 30)
            else if (ticksAlive == 30 && !frequencyActive) {
                frequencyActive = true;
                freqStartTick = ticksAlive;
                // Expanding wave rings
                for (int ring = 0; ring < 3; ring++) {
                    float radius = 3 + ring * 3;
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        BlockDisplayHandle wave = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius),
                            Material.AMETHYST_CLUSTER);
                        wave.scale(0.4f, 0.1f, 0.4f).glow(180, 0, 255).interpolation(2, 0);
                        freqHandles.add(wave);
                        spawnedEntities.add(wave.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
            }
            // Frequency sustained (30-110 ticks = 4 seconds)
            else if (frequencyActive && ticksAlive - freqStartTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 10.0);
                }
            }
            // Frequency ends (tick 110)
            else if (frequencyActive && ticksAlive - freqStartTick == 80) {
                frequencyActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_HURT, 1.0f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonantFrequency(plugin); }
    }

    // ================================================================
    // 12. TWIN CRYSTAL SWEEP — 360-degree arm rotation melee
    // ================================================================
    public static class TwinCrystalSweep extends BossAttack {
        private final List<BlockDisplayHandle> sweepHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepStartTick = 0;

        public TwinCrystalSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_twin_crystal_sweep", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(11.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arms extend to horizontal
            for (int arm = 0; arm < 2; arm++) {
                float dir = arm == 0 ? -1 : 1;
                for (int seg = 0; seg < 5; seg++) {
                    BlockDisplayHandle armSeg = displayBuilder.spawnBlock(
                        center.clone().add(dir * (seg + 1), 2.5, 0), Material.AMETHYST_BLOCK);
                    armSeg.scale(0.5f, 0.5f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                    sweepHandles.add(armSeg);
                    spawnedEntities.add(armSeg.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Windup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !sweepActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 6, 3.0);
                }
            }
            // Sweep begins (tick 20) — 360 degree rotation in 16 ticks
            else if (ticksAlive == 20 && !sweepActive) {
                sweepActive = true;
                sweepStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.3f);
            }
            // Sweep rotation (20-36 ticks = 0.8 seconds)
            else if (sweepActive && ticksAlive - sweepStartTick < 16) {
                float rotation = ((ticksAlive - sweepStartTick) / 16.0f) * (float)(Math.PI * 2);
                for (int arm = 0; arm < 2; arm++) {
                    float baseAngle = arm == 0 ? 0 : (float) Math.PI;
                    for (int seg = 0; seg < 5; seg++) {
                        int idx = arm * 5 + seg;
                        float dist = seg + 1;
                        double angle = baseAngle + rotation;
                        if (idx < sweepHandles.size()) {
                            sweepHandles.get(idx).entity().teleport(
                                center.clone().add(
                                    Math.cos(angle) * dist, 2.5, Math.sin(angle) * dist));
                        }
                    }
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 8, 5.0);
                }
            }
            // Sweep completes (tick 36)
            else if (sweepActive && ticksAlive - sweepStartTick == 16) {
                sweepActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 1.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinCrystalSweep(plugin); }
    }

    // ================================================================
    // 13. CRYSTALLINE MIRAGE — 2 decoy copies for 5 seconds
    // ================================================================
    public static class CrystallineMirage extends BossAttack {
        private final List<BlockDisplayHandle> mirageHandles = new ArrayList<>();
        private boolean miragesActive = false;

        public CrystallineMirage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystalline_mirage", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Split animation — CRIT particle clusters form at flanks
            DisplayBuilder.cyanDust(center.clone().add(-3, 2, 0), 8, 1.5);
            DisplayBuilder.cyanDust(center.clone().add(3, 2, 0), 8, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Materialization (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !miragesActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-3, 2, 0), 5, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(3, 2, 0), 5, 1.0);
                }
            }
            // Mirages activate (tick 16)
            else if (ticksAlive == 16 && !miragesActive) {
                miragesActive = true;
                // Two mirage constructs
                for (int mirage = 0; mirage < 2; mirage++) {
                    float offsetX = mirage == 0 ? -4 : 4;
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle body = displayBuilder.spawnBlock(
                            center.clone().add(offsetX, y, 0), Material.AMETHYST_BLOCK);
                        body.scale(0.8f, 0.9f, 0.8f).glow(150, 0, 220).interpolation(2, 0);
                        mirageHandles.add(body);
                        spawnedEntities.add(body.entity());
                    }
                    // Arm crystals
                    BlockDisplayHandle arm = displayBuilder.spawnBlock(
                        center.clone().add(offsetX + 1.2f, 2, 0), Material.AMETHYST_CLUSTER);
                    arm.scale(0.4f, 0.4f, 1.2f).glow(150, 0, 220).interpolation(2, 0);
                    mirageHandles.add(arm);
                    spawnedEntities.add(arm.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.5f);
            }
            // Mirages active (16-116 ticks = 5 seconds)
            else if (miragesActive && ticksAlive < 116) {
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle h : mirageHandles) {
                        DisplayBuilder.cyanDust(h.entity().getLocation(), 2, 0.5);
                    }
                }
            }
            // Mirages shatter (tick 116)
            else if (miragesActive && ticksAlive == 116) {
                miragesActive = false;
                // 4 crystal shards from each mirage
                for (int mirage = 0; mirage < 2; mirage++) {
                    float mx = mirage == 0 ? -4 : 4;
                    Location mirageLoc = center.clone().add(mx, 1.5, 0);
                    for (int s = 0; s < 4; s++) {
                        double angle = (Math.PI * 2 / 4) * s;
                        BlockDisplayHandle shard = displayBuilder.spawnBlock(
                            mirageLoc.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2),
                            Material.AMETHYST_CLUSTER);
                        shard.scale(0.15f, 0.15f, 0.3f).glow(180, 0, 255).interpolation(1, 0);
                        spawnedEntities.add(shard.entity());
                    }
                    DisplayBuilder.cyanDust(mirageLoc, 12, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineMirage(plugin); }
    }

    // ================================================================
    // 14. SPIRE ERUPTION — 3 amethyst spires erupt from ground circles
    // ================================================================
    public static class SpireEruption extends BossAttack {
        private final List<BlockDisplayHandle> spireHandles = new ArrayList<>();
        private boolean spiresErupted = false;
        private final Location[] spireLocations = new Location[3];

        public SpireEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_spire_eruption", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 ground circles at random positions
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * 16;
                double oz = (Math.random() - 0.5) * 16;
                spireLocations[i] = center.clone().add(ox, 0, oz);
                for (int ring = 0; ring < 6; ring++) {
                    double angle = (Math.PI * 2 / 6) * ring;
                    BlockDisplayHandle circle = displayBuilder.spawnBlock(
                        spireLocations[i].clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2),
                        Material.SMALL_AMETHYST_BUD);
                    circle.scale(0.4f, 0.1f, 0.4f).glow(180, 0, 255).interpolation(3, 0);
                    spireHandles.add(circle);
                    spawnedEntities.add(circle.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Circles accumulate particles (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !spiresErupted) {
                if (ticksAlive % 8 == 0) {
                    for (Location loc : spireLocations) {
                        if (loc != null) DisplayBuilder.cyanDust(loc.clone().add(0, 0.2, 0), 6, 2.0);
                    }
                }
            }
            // Spires erupt (tick 40)
            else if (ticksAlive == 40 && !spiresErupted) {
                spiresErupted = true;
                for (Location spireLoc : spireLocations) {
                    if (spireLoc == null) continue;
                    // 6-block tall crystal column
                    for (int y = 0; y < 6; y++) {
                        BlockDisplayHandle spire = displayBuilder.spawnBlock(
                            spireLoc.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                        spire.scale(0.8f, 0.9f, 0.8f).glow(180, 0, 255).interpolation(2, 0);
                        spireHandles.add(spire);
                        spawnedEntities.add(spire.entity());
                    }
                    // 4 ejected shards from top
                    for (int s = 0; s < 4; s++) {
                        double angle = (Math.PI * 2 / 4) * s;
                        BlockDisplayHandle shard = displayBuilder.spawnBlock(
                            spireLoc.clone().add(Math.cos(angle) * 3, 6, Math.sin(angle) * 3),
                            Material.AMETHYST_CLUSTER);
                        shard.scale(0.2f, 0.2f, 0.4f).glow(180, 0, 255).interpolation(1, 0);
                        spawnedEntities.add(shard.entity());
                    }
                    DisplayBuilder.cyanDust(spireLoc.clone().add(0, 3, 0), 15, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            }
            // Spires persist (40-340 ticks = 15 seconds)
            else if (spiresErupted && ticksAlive < 340) {
                if (ticksAlive % 20 == 0) {
                    for (Location loc : spireLocations) {
                        if (loc != null) DisplayBuilder.cyanDust(loc.clone().add(0, 3, 0), 3, 1.0);
                    }
                }
            }
            // Spires collapse (tick 340)
            else if (spiresErupted && ticksAlive == 340) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpireEruption(plugin); }
    }

    // ================================================================
    // 15. CRYSTAL ANCHOR FIELD — Ground veins root players in place
    // ================================================================
    public static class CrystalAnchorField extends BossAttack {
        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private boolean fieldActive = false;

        public CrystalAnchorField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystal_anchor_field", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Amethyst veins spreading outward on ground
            for (int ray = 0; ray < 8; ray++) {
                double angle = (Math.PI * 2 / 8) * ray;
                for (int seg = 1; seg <= 10; seg++) {
                    Location veinLoc = center.clone().add(
                        Math.cos(angle) * seg, 0.03, Math.sin(angle) * seg);
                    BlockDisplayHandle vein = displayBuilder.spawnBlock(veinLoc, Material.AMETHYST_CLUSTER);
                    vein.scale(0.3f, 0.03f, 0.3f).glow(180, 0, 255).interpolation(3, 0);
                    anchorHandles.add(vein);
                    spawnedEntities.add(vein.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Veins spread (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fieldActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.1, 0), 10, 10.0);
                }
            }
            // Field locks (tick 40) — root effect for 60 ticks (3 seconds)
            else if (ticksAlive == 40 && !fieldActive) {
                fieldActive = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 20, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.7f);
            }
            // Field sustained (40-100 ticks)
            else if (fieldActive && ticksAlive < 100) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 10.0);
                }
            }
            // Field retracts (tick 100)
            else if (fieldActive && ticksAlive == 100) {
                fieldActive = false;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalAnchorField(plugin); }
    }

    // ================================================================
    // 16. PRISM CANNON — Continuous tracking beam from arm-crystal
    // ================================================================
    public static class PrismCannon extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int beamStartTick = 0;

        public PrismCannon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_prism_cannon", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm-crystal charges — bright white-purple glow
            BlockDisplayHandle armGlow = displayBuilder.spawnBlock(
                center.clone().add(2, 3, 0), Material.SEA_LANTERN);
            armGlow.scale(0.5f, 0.5f, 1.5f).glow(200, 100, 255).interpolation(3, 0);
            beamHandles.add(armGlow);
            spawnedEntities.add(armGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 3, 0), 5, 1.0);
                }
            }
            // Beam fires (tick 40)
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                beamStartTick = ticksAlive;
                // Dense beam column
                for (int seg = 0; seg < 20; seg++) {
                    Location beamPt = center.clone().add(2, 3, seg * 1.0);
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamPt, Material.END_ROD);
                    beamSeg.scale(0.2f, 0.2f, 0.8f).glow(200, 100, 255).interpolation(1, 0);
                    beamHandles.add(beamSeg);
                    spawnedEntities.add(beamSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.7f);
            }
            // Beam sweeps slowly (40-80 ticks = 2 seconds)
            else if (beamActive && ticksAlive - beamStartTick < 40) {
                float sweepAngle = ((ticksAlive - beamStartTick) / 40.0f) * 0.5f;
                for (int seg = 1; seg < beamHandles.size(); seg++) {
                    int idx = seg - 1;
                    float dist = idx * 1.0f;
                    beamHandles.get(seg).entity().teleport(
                        center.clone().add(2 + dist * Math.cos(sweepAngle), 3, dist * Math.sin(sweepAngle)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 3, 10), 5, 2.0);
                }
            }
            // Beam ends — recoil (tick 80)
            else if (beamActive && ticksAlive - beamStartTick == 40) {
                beamActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_HURT, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismCannon(plugin); }
    }

    // ================================================================
    // 17. CRYSTALLINE DEATH BURST — Death attack: geode explosion + shards
    // ================================================================
    public static class CrystallineDeathBurst extends BossAttack {
        private final List<BlockDisplayHandle> deathHandles = new ArrayList<>();
        private boolean burstTriggered = false;

        public CrystallineDeathBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_crystalline_death_burst", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial burst visual
            BlockDisplayHandle geodeCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.AMETHYST_BLOCK);
            geodeCore.scale(2.0f, 2.0f, 2.0f).glow(255, 100, 255).interpolation(2, 0);
            deathHandles.add(geodeCore);
            spawnedEntities.add(geodeCore.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Burst (tick 5)
            if (ticksAlive == 5 && !burstTriggered) {
                burstTriggered = true;
                // 14 crystal shards spray in all directions
                for (int i = 0; i < 14; i++) {
                    double angle = (Math.PI * 2 / 14) * i;
                    float dist = 3 + (float)(Math.random() * 4);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * dist, 2, Math.sin(angle) * dist),
                        Material.AMETHYST_CLUSTER);
                    shard.scale(0.2f, 0.2f, 0.4f).glow(180, 0, 255).interpolation(1, 0);
                    deathHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                // 8 counter shards (double normal)
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + 0.3;
                    BlockDisplayHandle counter = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 5, 1.5, Math.sin(angle) * 5),
                        Material.AMETHYST_CLUSTER);
                    counter.scale(0.15f, 0.15f, 0.3f).glow(180, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(counter.entity());
                }
                // Vertical column from death point
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.END_ROD);
                    col.scale(0.3f, 0.8f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
                    spawnedEntities.add(col.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 40, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.5f, 0.7f);
            }
            // Hazard tiles on ground (tick 10)
            else if (burstTriggered && ticksAlive == 10) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * 10;
                    double oz = (Math.random() - 0.5) * 10;
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(
                        center.clone().add(ox, 0.05, oz), Material.AMETHYST_CLUSTER);
                    tile.scale(0.5f, 0.1f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                    spawnedEntities.add(tile.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineDeathBurst(plugin); }
    }

    // ================================================================
    // 18. DUAL SUMMON COORDINATION — One-time scripted combo with Ember Wraith
    // ================================================================
    public static class DualSummonCoordination extends BossAttack {
        private final List<BlockDisplayHandle> coordHandles = new ArrayList<>();
        private boolean coordFired = false;

        public DualSummonCoordination(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_warden_dual_summon_coordination", AttackType.BOSS, 4), "crystal_warden");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Both bosses pulse their colors simultaneously
            BlockDisplayHandle crystalPulse = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.AMETHYST_BLOCK);
            crystalPulse.scale(2.5f, 2.5f, 2.5f).glow(180, 0, 255).interpolation(4, 0);
            coordHandles.add(crystalPulse);
            spawnedEntities.add(crystalPulse.entity());

            // Fire-side pulse (Ember Wraith representation)
            BlockDisplayHandle firePulse = displayBuilder.spawnBlock(
                center.clone().add(10, 3, 0), Material.MAGMA_BLOCK);
            firePulse.scale(2.5f, 2.5f, 2.5f).glow(255, 100, 0).interpolation(4, 0);
            coordHandles.add(firePulse);
            spawnedEntities.add(firePulse.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.1f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sync warning (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !coordFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 3.0);
                    DisplayBuilder.cyanDust(center.clone().add(10, 3, 0), 8, 3.0);
                }
            }
            // Coordination fires (tick 30) — crystal wall + soul fire pillar
            else if (ticksAlive == 30 && !coordFired) {
                coordFired = true;
                // Crystal wall section (funneling)
                for (int i = -3; i <= 3; i++) {
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(
                            center.clone().add(i, y, 5), Material.AMETHYST_BLOCK);
                        wall.scale(0.9f, 0.9f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                        coordHandles.add(wall);
                        spawnedEntities.add(wall.entity());
                    }
                }
                // Soul fire pillar at the funnel exit
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 8), Material.SOUL_LANTERN);
                    pillar.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                    coordHandles.add(pillar);
                    spawnedEntities.add(pillar.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DualSummonCoordination(plugin); }
    }
}
