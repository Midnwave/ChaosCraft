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
 * Phase 4D Mini Boss 3 — THE EMBER WRAITH
 * A fragment of the Calamity Dweller's brimstone consciousness.
 * Smoke and fire silhouette, twin cyan eyes, erratic teleporting movement.
 * Crimson/orange fire theme. Glass cannon — high damage, low HP.
 * Unique Mechanic: Brimstone Death (massive explosion on death).
 * 17 attacks total.
 * NO status effects — damage only.
 */
public final class EmberWraithAttacks {

    private EmberWraithAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BrimstoneBolt(plugin));
        registry.register(new EmberRain(plugin));
        registry.register(new CloneStep(plugin));
        registry.register(new SoulFirePillar(plugin));
        registry.register(new BrimstoneTrail(plugin));
        registry.register(new CorruptionSeed(plugin));
        registry.register(new PhantomGrasp(plugin));
        registry.register(new BrimstoneWave(plugin));
        registry.register(new AshCloud(plugin));
        registry.register(new CyanEyeBeam(plugin));
        registry.register(new SmokeSiphon(plugin));
        registry.register(new BrimstoneCross(plugin));
        registry.register(new WraithLunge(plugin));
        registry.register(new BrimstoneSpiral(plugin));
        registry.register(new SoulHunger(plugin));
        registry.register(new CinderCluster(plugin));
        registry.register(new BrimstoneDetonation(plugin));
    }

    // ================================================================
    // 1. BRIMSTONE BOLT — Fast fire projectile at targeted player
    // ================================================================
    public static class BrimstoneBolt extends BossAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private boolean boltFired = false;
        private int fireStartTick = 0;

        public BrimstoneBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_bolt", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(150);
            config.setCooldownTicks(100);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Cyan eye brightens — chest FLAME particles accelerate outward
            BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 3.2, 0), Material.SEA_LANTERN);
            eyeGlow.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
            boltHandles.add(eyeGlow);
            spawnedEntities.add(eyeGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fast telegraph (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !boltFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 4, 0.8);
                }
            }
            // Bolt fires (tick 16)
            else if (ticksAlive == 16 && !boltFired) {
                boltFired = true;
                fireStartTick = ticksAlive;
                // Dense brimstone bolt — fire-colored segments
                for (int i = 0; i < 15; i++) {
                    Location segLoc = center.clone().add(0, 2.5, i * 1.0);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.MAGMA_BLOCK);
                    seg.scale(0.3f, 0.3f, 0.6f).glow(255, 100, 0).interpolation(0, 0);
                    boltHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.9f);
            }
            // Bolt travels (16-36 ticks)
            else if (boltFired && ticksAlive - fireStartTick < 20) {
                float travel = (ticksAlive - fireStartTick) * 1.0f;
                for (int i = 1; i < boltHandles.size(); i++) {
                    int idx = i - 1;
                    boltHandles.get(i).entity().teleport(
                        center.clone().add(0, 2.5, idx + travel));
                }
            }
            // Impact (tick 36)
            else if (boltFired && ticksAlive - fireStartTick == 20) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 20), 10, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBolt(plugin); }
    }

    // ================================================================
    // 2. EMBER RAIN — 16 embers rain down in scattered pattern
    // ================================================================
    public static class EmberRain extends BossAttack {
        private final List<BlockDisplayHandle> emberHandles = new ArrayList<>();
        private boolean embersRaining = false;
        private int rainStartTick = 0;

        public EmberRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_ember_rain", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Gathering effect — embers spiral upward
            BlockDisplayHandle gatherCore = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.GLOWSTONE);
            gatherCore.scale(0.8f, 0.8f, 0.8f).glow(255, 150, 0).interpolation(3, 0);
            emberHandles.add(gatherCore);
            spawnedEntities.add(gatherCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gathering (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !embersRaining) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 8, 3.0);
                }
                // Move gathering point upward
                emberHandles.get(0).entity().teleport(
                    center.clone().add(0, 3 + (ticksAlive / 40.0f) * 9, 0));
            }
            // Rain starts (tick 40)
            else if (ticksAlive == 40 && !embersRaining) {
                embersRaining = true;
                rainStartTick = ticksAlive;
                // Create 16 ember projectiles at altitude
                for (int i = 0; i < 16; i++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    BlockDisplayHandle ember = displayBuilder.spawnBlock(
                        center.clone().add(ox, 12, oz), Material.MAGMA_BLOCK);
                    ember.scale(0.4f, 0.4f, 0.4f).glow(255, 100, 0).interpolation(1, 0);
                    emberHandles.add(ember);
                    spawnedEntities.add(ember.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
            }
            // Embers fall staggered (40-70 ticks = 1.5 seconds)
            else if (embersRaining && ticksAlive - rainStartTick < 30) {
                int elapsed = ticksAlive - rainStartTick;
                for (int i = 1; i < emberHandles.size(); i++) {
                    int shardIdx = i - 1;
                    int delay = (shardIdx * 30) / 16;
                    if (elapsed >= delay) {
                        float drop = (elapsed - delay) * 0.6f;
                        Location loc = emberHandles.get(i).entity().getLocation();
                        if (loc.getY() > center.getY() + 0.5) {
                            emberHandles.get(i).entity().teleport(
                                loc.clone().add(0, -drop * 0.5, 0));
                        }
                    }
                }
            }
            // Ground fire patches (tick 70)
            else if (embersRaining && ticksAlive - rainStartTick == 30) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    BlockDisplayHandle patch = displayBuilder.spawnBlock(
                        center.clone().add(ox, 0.05, oz), Material.ORANGE_STAINED_GLASS);
                    patch.scale(1.5f, 0.08f, 1.5f).glow(255, 80, 0).interpolation(2, 0);
                    spawnedEntities.add(patch.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EmberRain(plugin); }
    }

    // ================================================================
    // 3. CLONE STEP — Teleport leaving fire clone decoys
    // ================================================================
    public static class CloneStep extends BossAttack {
        private final List<BlockDisplayHandle> cloneHandles = new ArrayList<>();
        private int stepsCompleted = 0;

        public CloneStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_clone_step", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No warning — immediate step
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 clone steps, 12 ticks apart (0.6 seconds each)
            if (stepsCompleted < 3 && ticksAlive == stepsCompleted * 12) {
                stepsCompleted++;
                double ox = (Math.random() - 0.5) * 12;
                double oz = (Math.random() - 0.5) * 12;
                Location cloneLoc = center.clone().add(ox, 0, oz);

                // Fire clone silhouette — tall thin shape
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle cloneSeg = displayBuilder.spawnBlock(
                        cloneLoc.clone().add(0, y, 0), Material.SOUL_LANTERN);
                    cloneSeg.scale(0.6f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                    cloneHandles.add(cloneSeg);
                    spawnedEntities.add(cloneSeg.entity());
                }
                // Transition flash
                DisplayBuilder.cyanDust(cloneLoc.clone().add(0, 2, 0), 10, 1.5);
                DisplayBuilder.playSound(cloneLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.1f);
                DisplayBuilder.playSound(cloneLoc, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
            }
            // Clones persist for 80 ticks (4 seconds) then fade
            else if (stepsCompleted == 3 && ticksAlive == 36 + 80) {
                for (BlockDisplayHandle h : cloneHandles) {
                    DisplayBuilder.cyanDust(h.entity().getLocation(), 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CloneStep(plugin); }
    }

    // ================================================================
    // 4. SOUL FIRE PILLAR — Erupting pillar of soul fire
    // ================================================================
    public static class SoulFirePillar extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private boolean pillarErupted = false;

        public SoulFirePillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_soul_fire_pillar", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground circle — soul fire blue particles
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                BlockDisplayHandle circle = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2),
                    Material.SOUL_LANTERN);
                circle.scale(0.4f, 0.05f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                pillarHandles.add(circle);
                spawnedEntities.add(circle.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Circle pulses (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24 && !pillarErupted) {
                if (ticksAlive == 12) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 10, 2.0);
                }
            }
            // Pillar erupts (tick 24)
            else if (ticksAlive == 24 && !pillarErupted) {
                pillarErupted = true;
                // Soul fire pillar — 8 blocks tall
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.SOUL_LANTERN);
                    seg.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                    pillarHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 20, 2.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 1.5f, 0.5f);
            }
            // Pillar active (24-224 ticks = 10 seconds)
            else if (pillarErupted && ticksAlive < 224) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 5, 1.5);
                }
            }
            // Pillar collapses (tick 224)
            else if (pillarErupted && ticksAlive == 224) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFirePillar(plugin); }
    }

    // ================================================================
    // 5. BRIMSTONE TRAIL — Moving fire trail behind the Wraith
    // ================================================================
    public static class BrimstoneTrail extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean trailActive = false;
        private int trailStartTick = 0;

        public BrimstoneTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_trail", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith brightens — double FLAME density
            BlockDisplayHandle fireCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.MAGMA_BLOCK);
            fireCore.scale(0.8f, 2.5f, 0.8f).glow(255, 150, 0).interpolation(2, 0);
            trailHandles.add(fireCore);
            spawnedEntities.add(fireCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brief brightening (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !trailActive) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 1.0);
                }
            }
            // Trail activates (tick 10) — leaves fire tiles for 100 ticks (5 seconds)
            else if (ticksAlive == 10 && !trailActive) {
                trailActive = true;
                trailStartTick = ticksAlive;
            }
            // Trail being laid (10-110 ticks)
            else if (trailActive && ticksAlive - trailStartTick < 100) {
                if (ticksAlive % 4 == 0) {
                    // Lay fire tile at simulated movement position
                    float moveZ = (ticksAlive - trailStartTick) * 0.3f;
                    float moveX = (float) Math.sin(ticksAlive * 0.1) * 3;
                    Location tileLoc = center.clone().add(moveX, 0.05, moveZ);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.ORANGE_STAINED_GLASS);
                    tile.scale(0.8f, 0.06f, 0.8f).glow(255, 80, 0).interpolation(2, 0);
                    trailHandles.add(tile);
                    spawnedEntities.add(tile.entity());

                    // Move the wraith core representation
                    trailHandles.get(0).entity().teleport(
                        center.clone().add(moveX, 2, moveZ));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneTrail(plugin); }
    }

    // ================================================================
    // 6. CORRUPTION SEED — Slow homing ember that creates corrupted zone
    // ================================================================
    public static class CorruptionSeed extends BossAttack {
        private final List<BlockDisplayHandle> seedHandles = new ArrayList<>();
        private boolean seedFired = false;
        private int fireStartTick = 0;

        public CorruptionSeed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_corruption_seed", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith coils — denser and darker
            BlockDisplayHandle seedCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2.5, 0), Material.SOUL_LANTERN);
            seedCore.scale(0.6f, 0.6f, 0.6f).glow(0, 150, 200).interpolation(2, 0);
            seedHandles.add(seedCore);
            spawnedEntities.add(seedCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Coil telegraph (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !seedFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 4, 0.5);
                }
            }
            // Seed fires (tick 16) — slow projectile
            else if (ticksAlive == 16 && !seedFired) {
                seedFired = true;
                fireStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
            }
            // Seed travels slowly (16-56 ticks = 2 seconds at half speed)
            else if (seedFired && ticksAlive - fireStartTick < 40) {
                float travel = (ticksAlive - fireStartTick) * 0.35f;
                seedHandles.get(0).entity().teleport(
                    center.clone().add(0, 2.5, travel));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(seedHandles.get(0).entity().getLocation(), 3, 0.5);
                }
            }
            // Impact — corrupted ground spot (tick 56)
            else if (seedFired && ticksAlive - fireStartTick == 40) {
                Location impactLoc = center.clone().add(0, 0, 14);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle corrupt = displayBuilder.spawnBlock(
                            impactLoc.clone().add(dx, 0.03, dz), Material.SOUL_SOIL);
                        corrupt.scale(0.9f, 0.05f, 0.9f).glow(0, 100, 150).interpolation(2, 0);
                        seedHandles.add(corrupt);
                        spawnedEntities.add(corrupt.entity());
                    }
                }
                DisplayBuilder.cyanDust(impactLoc, 10, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSeed(plugin); }
    }

    // ================================================================
    // 7. PHANTOM GRASP — Extended arm melee reach attack
    // ================================================================
    public static class PhantomGrasp extends BossAttack {
        private final List<BlockDisplayHandle> graspHandles = new ArrayList<>();
        private boolean graspFired = false;

        public PhantomGrasp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_phantom_grasp", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm elongates toward nearest player
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle armSeg = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.5, i * 1.0), Material.COAL_BLOCK);
                armSeg.scale(0.25f, 0.25f, 0.8f).glow(40, 40, 40).interpolation(2, 0);
                graspHandles.add(armSeg);
                spawnedEntities.add(armSeg.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_BITE, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Elongation telegraph (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !graspFired) {
                // Arm extends progressively
                float extend = ticksAlive / 16.0f;
                for (int i = 0; i < graspHandles.size(); i++) {
                    graspHandles.get(i).entity().teleport(
                        center.clone().add(0, 2.5, i * (1.0 + extend * 0.5)));
                }
            }
            // Grasp connects (tick 16)
            else if (ticksAlive == 16 && !graspFired) {
                graspFired = true;
                // Contact burst at reach endpoint
                Location contactLoc = center.clone().add(0, 2.5, 9);
                BlockDisplayHandle burst = displayBuilder.spawnBlock(contactLoc, Material.MAGMA_BLOCK);
                burst.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(1, 0);
                graspHandles.add(burst);
                spawnedEntities.add(burst.entity());
                DisplayBuilder.cyanDust(contactLoc, 12, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 1.2f, 0.7f);
            }
            // Arm retracts (16-26 ticks)
            else if (graspFired && ticksAlive < 26) {
                float retract = (ticksAlive - 16) / 10.0f;
                for (int i = 0; i < 6; i++) {
                    float dist = i * (1.5f - retract * 1.0f);
                    graspHandles.get(i).entity().teleport(
                        center.clone().add(0, 2.5, dist));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomGrasp(plugin); }
    }

    // ================================================================
    // 8. BRIMSTONE WAVE — Expanding ground-level ring
    // ================================================================
    public static class BrimstoneWave extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean waveFired = false;
        private int waveStartTick = 0;

        public BrimstoneWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_wave", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith stands still — motionless, unsettling
            BlockDisplayHandle stillCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.COAL_BLOCK);
            stillCore.scale(0.6f, 3.0f, 0.6f).glow(40, 40, 40).interpolation(2, 0);
            waveHandles.add(stillCore);
            spawnedEntities.add(stillCore.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Motionless telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !waveFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 2.0);
                }
            }
            // Wave fires (tick 30)
            else if (ticksAlive == 30 && !waveFired) {
                waveFired = true;
                waveStartTick = ticksAlive;
                // Initial ring segments
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2, 0.2, Math.sin(angle) * 2),
                        Material.MAGMA_BLOCK);
                    seg.scale(0.8f, 0.3f, 0.8f).glow(255, 80, 0).interpolation(1, 0);
                    waveHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_HURT, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
            // Wave expands (30-54 ticks = 1.2 seconds to 16-block radius)
            else if (waveFired && ticksAlive - waveStartTick < 24) {
                float radius = 2 + ((ticksAlive - waveStartTick) / 24.0f) * 14;
                for (int i = 1; i < waveHandles.size(); i++) {
                    int idx = i - 1;
                    double angle = (Math.PI * 2 / 16) * idx;
                    waveHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 8, radius);
                }
            }
            // Wave reaches max and dissipates (tick 54)
            else if (waveFired && ticksAlive - waveStartTick == 24) {
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneWave(plugin); }
    }

    // ================================================================
    // 9. ASH CLOUD — Obscuring smoke cloud with damage
    // ================================================================
    public static class AshCloud extends BossAttack {
        private final List<BlockDisplayHandle> cloudHandles = new ArrayList<>();
        private boolean cloudActive = false;

        public AshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_ash_cloud", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Exhale — smoke puff from midsection
            DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Cloud forming (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !cloudActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 3.0);
                }
            }
            // Cloud expands and persists (tick 20)
            else if (ticksAlive == 20 && !cloudActive) {
                cloudActive = true;
                // Dense cloud of dark glass blocks
                for (int i = 0; i < 24; i++) {
                    double ox = (Math.random() - 0.5) * 14;
                    double oy = Math.random() * 4;
                    double oz = (Math.random() - 0.5) * 14;
                    BlockDisplayHandle smoke = displayBuilder.spawnBlock(
                        center.clone().add(ox, oy + 0.5, oz), Material.GRAY_STAINED_GLASS);
                    smoke.scale(2.0f, 1.5f, 2.0f).glow(30, 30, 30).interpolation(3, 0);
                    cloudHandles.add(smoke);
                    spawnedEntities.add(smoke.entity());
                }
                // Wraith's cyan eyes visible through cloud
                BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                    center.clone().add(-0.3, 3, 0), Material.SEA_LANTERN);
                leftEye.scale(0.15f, 0.15f, 0.15f).glow(0, 255, 255).interpolation(1, 0);
                cloudHandles.add(leftEye);
                spawnedEntities.add(leftEye.entity());

                BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                    center.clone().add(0.3, 3, 0), Material.SEA_LANTERN);
                rightEye.scale(0.15f, 0.15f, 0.15f).glow(0, 255, 255).interpolation(1, 0);
                cloudHandles.add(rightEye);
                spawnedEntities.add(rightEye.entity());

                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.5f, 0.6f);
            }
            // Cloud active (20-140 ticks = 6 seconds)
            else if (cloudActive && ticksAlive < 140) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 8.0);
                }
            }
            // Cloud dissipates (tick 140)
            else if (cloudActive && ticksAlive == 140) {
                cloudActive = false;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AshCloud(plugin); }
    }

    // ================================================================
    // 10. CYAN EYE BEAM — Twin beam projectiles from both eyes
    // ================================================================
    public static class CyanEyeBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamFired = false;

        public CyanEyeBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_cyan_eye_beam", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes widen — glow intensifies
            for (int eye = 0; eye < 2; eye++) {
                float offX = eye == 0 ? -0.3f : 0.3f;
                BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                    center.clone().add(offX, 3.2, 0), Material.SEA_LANTERN);
                eyeGlow.scale(0.2f, 0.2f, 0.2f).glow(0, 255, 255).interpolation(2, 0);
                beamHandles.add(eyeGlow);
                spawnedEntities.add(eyeGlow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Whine buildup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !beamFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3.2, 0), 4, 0.5);
                }
            }
            // Beams fire (tick 20)
            else if (ticksAlive == 20 && !beamFired) {
                beamFired = true;
                // Two thin beam lines from each eye
                for (int eye = 0; eye < 2; eye++) {
                    float offX = eye == 0 ? -0.3f : 0.3f;
                    for (int seg = 0; seg < 20; seg++) {
                        Location beamPt = center.clone().add(offX, 3.2, seg * 1.0);
                        BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamPt, Material.SEA_LANTERN);
                        beamSeg.scale(0.08f, 0.08f, 0.6f).glow(0, 255, 255).interpolation(0, 0);
                        beamHandles.add(beamSeg);
                        spawnedEntities.add(beamSeg.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.4f);
            }
            // Beams scatter on impact (tick 30)
            else if (beamFired && ticksAlive == 30) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 20), 12, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CyanEyeBeam(plugin); }
    }

    // ================================================================
    // 11. SMOKE SIPHON — Pull smoke inward, damage players, self-heal
    // ================================================================
    public static class SmokeSiphon extends BossAttack {
        private final List<BlockDisplayHandle> siphonHandles = new ArrayList<>();
        private boolean siphonActive = false;

        public SmokeSiphon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_smoke_siphon", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith becomes more transparent — smoke flows inward
            BlockDisplayHandle fadeCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GRAY_STAINED_GLASS);
            fadeCore.scale(0.8f, 3.0f, 0.8f).glow(20, 20, 20).interpolation(3, 0);
            siphonHandles.add(fadeCore);
            spawnedEntities.add(fadeCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_BITE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Transparency phase (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !siphonActive) {
                if (ticksAlive % 6 == 0) {
                    // Inward flowing smoke particles
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI * 2 / 4) * i;
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(angle) * 6, 2, Math.sin(angle) * 6), 3, 1.0);
                    }
                }
            }
            // Siphon activates (tick 30)
            else if (ticksAlive == 30 && !siphonActive) {
                siphonActive = true;
                // Wraith re-emerges brighter
                siphonHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f, 0, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.0f, 3.0f, 1.0f),
                    new AxisAngle4f(0, 0, 1, 0)));
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 2.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.0f);
            }
            // Post-siphon intensity (30-90 ticks = 3 seconds of intensified flames)
            else if (siphonActive && ticksAlive < 90) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SmokeSiphon(plugin); }
    }

    // ================================================================
    // 12. BRIMSTONE CROSS — Plus-shaped ground detonation
    // ================================================================
    public static class BrimstoneCross extends BossAttack {
        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private boolean crossDetonated = false;

        public BrimstoneCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_cross", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Two intersecting ground lines forming
            for (int arm = 0; arm < 4; arm++) {
                double angle = (Math.PI / 2) * arm;
                for (int seg = 1; seg <= 10; seg++) {
                    Location linePt = center.clone().add(
                        Math.cos(angle) * seg, 0.05, Math.sin(angle) * seg);
                    BlockDisplayHandle line = displayBuilder.spawnBlock(linePt, Material.ORANGE_STAINED_GLASS);
                    line.scale(0.5f, 0.04f, 0.5f).glow(255, 80, 0).interpolation(3, 0);
                    crossHandles.add(line);
                    spawnedEntities.add(line.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Lines grow (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !crossDetonated) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 8.0);
                }
            }
            // Cross detonates (tick 30) — erupts 2 blocks high
            else if (ticksAlive == 30 && !crossDetonated) {
                crossDetonated = true;
                // Rise all segments to 2-block walls
                for (BlockDisplayHandle seg : crossHandles) {
                    Location loc = seg.entity().getLocation();
                    seg.entity().setTransformation(new Transformation(
                        new Vector3f(-0.25f, 0, -0.25f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, 2.0f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
            }
            // Eruption collapses (tick 40) — fire patches remain
            else if (crossDetonated && ticksAlive == 40) {
                // Replace with ground fire patches
                for (int arm = 0; arm < 4; arm++) {
                    double angle = (Math.PI / 2) * arm;
                    for (int seg = 1; seg <= 10; seg += 2) {
                        Location patchLoc = center.clone().add(
                            Math.cos(angle) * seg, 0.04, Math.sin(angle) * seg);
                        BlockDisplayHandle patch = displayBuilder.spawnBlock(patchLoc, Material.ORANGE_STAINED_GLASS);
                        patch.scale(0.8f, 0.05f, 0.8f).glow(200, 60, 0).interpolation(2, 0);
                        spawnedEntities.add(patch.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCross(plugin); }
    }

    // ================================================================
    // 13. WRAITH LUNGE — High-speed charge at target
    // ================================================================
    public static class WraithLunge extends BossAttack {
        private final List<BlockDisplayHandle> lungeHandles = new ArrayList<>();
        private boolean lungeFired = false;

        public WraithLunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_wraith_lunge", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith leans back — compressed spring pose
            BlockDisplayHandle leanCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, -1), Material.COAL_BLOCK);
            leanCore.scale(0.6f, 3.0f, 0.6f).glow(40, 40, 40).interpolation(2, 0);
            lungeHandles.add(leanCore);
            spawnedEntities.add(leanCore.entity());

            // Cyan eyes locked on
            BlockDisplayHandle eye = displayBuilder.spawnBlock(
                center.clone().add(0, 3.2, -1), Material.SEA_LANTERN);
            eye.scale(0.25f, 0.15f, 0.15f).glow(0, 255, 255).interpolation(1, 0);
            lungeHandles.add(eye);
            spawnedEntities.add(eye.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Lean-back telegraph (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !lungeFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, -1), 3, 0.5);
                }
            }
            // Lunge fires (tick 16)
            else if (ticksAlive == 16 && !lungeFired) {
                lungeFired = true;
                // Charge trail — 14 blocks of fire trail
                for (int i = 0; i < 14; i++) {
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.5, i), Material.MAGMA_BLOCK);
                    trail.scale(0.5f, 0.3f, 0.5f).glow(255, 80, 0).interpolation(1, 0);
                    lungeHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                // Move wraith core to endpoint
                lungeHandles.get(0).entity().teleport(center.clone().add(0, 2, 14));
                lungeHandles.get(1).entity().teleport(center.clone().add(0, 3.2, 14));

                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 7), 15, 7.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 1.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 1.2f, 0.7f);
            }
            // Impact burst at contact (tick 20)
            else if (lungeFired && ticksAlive == 20) {
                Location impactLoc = center.clone().add(0, 1, 14);
                BlockDisplayHandle burst = displayBuilder.spawnBlock(impactLoc, Material.MAGMA_BLOCK);
                burst.scale(1.5f, 1.5f, 1.5f).glow(255, 100, 0).interpolation(2, 0);
                spawnedEntities.add(burst.entity());
                DisplayBuilder.cyanDust(impactLoc, 10, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WraithLunge(plugin); }
    }

    // ================================================================
    // 14. BRIMSTONE SPIRAL — Spinning projectile pattern (24 bolts)
    // ================================================================
    public static class BrimstoneSpiral extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean spiralActive = false;
        private int spiralStartTick = 0;
        private int rotationsFired = 0;

        public BrimstoneSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_spiral", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wraith begins spinning
            BlockDisplayHandle spinCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.MAGMA_BLOCK);
            spinCore.scale(0.8f, 2.5f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
            spiralHandles.add(spinCore);
            spawnedEntities.add(spinCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spin-up (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !spiralActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 2.0);
                }
            }
            // Spiral fires (tick 20) — 3 rotations of 8 bolts each
            else if (ticksAlive == 20 && !spiralActive) {
                spiralActive = true;
                spiralStartTick = ticksAlive;
            }
            // Fire rotations (20-60 ticks, one rotation per 13 ticks)
            else if (spiralActive && rotationsFired < 3) {
                int elapsed = ticksAlive - spiralStartTick;
                if (elapsed == rotationsFired * 13) {
                    rotationsFired++;
                    float baseAngle = rotationsFired * 0.3f;
                    for (int i = 0; i < 8; i++) {
                        double angle = baseAngle + (Math.PI * 2 / 8) * i;
                        Location boltLoc = center.clone().add(Math.cos(angle) * 3, 2.5, Math.sin(angle) * 3);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.MAGMA_BLOCK);
                        bolt.scale(0.3f, 0.3f, 0.3f).glow(255, 80, 0).interpolation(1, 0);
                        spiralHandles.add(bolt);
                        spawnedEntities.add(bolt.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
                }
                // Move fired bolts outward with spiral curve
                for (int i = 1; i < spiralHandles.size(); i++) {
                    Location loc = spiralHandles.get(i).entity().getLocation();
                    double dx = loc.getX() - center.getX();
                    double dz = loc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < 18) {
                        double currentAngle = Math.atan2(dz, dx);
                        double newAngle = currentAngle + 0.05;
                        double newDist = dist + 0.4;
                        spiralHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(newAngle) * newDist, 2.5, Math.sin(newAngle) * newDist));
                    }
                }
            }
            // Spiral ends — wobble (tick 60)
            else if (spiralActive && ticksAlive == spiralStartTick + 40) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSpiral(plugin); }
    }

    // ================================================================
    // 15. SOUL HUNGER — Debuff aura draining food and applying weakness
    // ================================================================
    public static class SoulHunger extends BossAttack {
        private final List<BlockDisplayHandle> hungerHandles = new ArrayList<>();
        private boolean hungerActive = false;

        public SoulHunger(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_soul_hunger", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes pulse 3 times rapidly
            for (int eye = 0; eye < 2; eye++) {
                float offX = eye == 0 ? -0.3f : 0.3f;
                BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                    center.clone().add(offX, 3.2, 0), Material.SEA_LANTERN);
                eyeGlow.scale(0.2f, 0.2f, 0.2f).glow(0, 255, 255).interpolation(2, 0);
                hungerHandles.add(eyeGlow);
                spawnedEntities.add(eyeGlow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye pulsing (0-15 ticks)
            if (ticksAlive < 15 && !hungerActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3.2, 0), 6, 0.5);
                }
            }
            // Hunger aura activates (tick 15)
            else if (ticksAlive == 15 && !hungerActive) {
                hungerActive = true;
                // Outward aura wave
                for (int ring = 0; ring < 3; ring++) {
                    float radius = 4 + ring * 4;
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        BlockDisplayHandle aura = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius),
                            Material.SOUL_LANTERN);
                        aura.scale(0.5f, 0.1f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                        hungerHandles.add(aura);
                        spawnedEntities.add(aura.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.8f);
            }
            // Aura sustained (15-135 ticks = 6 seconds)
            else if (hungerActive && ticksAlive < 135) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 8, 12.0);
                }
            }
            // Aura fades (tick 135)
            else if (hungerActive && ticksAlive == 135) {
                hungerActive = false;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulHunger(plugin); }
    }

    // ================================================================
    // 16. CINDER CLUSTER — 5 cinder projectiles targeting different players
    // ================================================================
    public static class CinderCluster extends BossAttack {
        private final List<BlockDisplayHandle> cinderHandles = new ArrayList<>();
        private boolean cindersFired = false;

        public CinderCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_cinder_cluster", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 cinder orbs orbit the Wraith's midsection
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 / 5) * i;
                Location cinderLoc = center.clone().add(
                    Math.cos(angle) * 1.5, 2.5, Math.sin(angle) * 1.5);
                BlockDisplayHandle cinder = displayBuilder.spawnBlock(cinderLoc, Material.GLOWSTONE);
                cinder.scale(0.4f, 0.4f, 0.4f).glow(255, 200, 0).interpolation(2, 0);
                cinderHandles.add(cinder);
                spawnedEntities.add(cinder.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24 && !cindersFired) {
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i + ticksAlive * 0.15;
                    cinderHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 1.5, 2.5, Math.sin(angle) * 1.5));
                }
            }
            // Cinders launch (tick 24)
            else if (ticksAlive == 24 && !cindersFired) {
                cindersFired = true;
                // Launch in spread directions
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i;
                    cinderHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 6, 2, Math.sin(angle) * 6));
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1.1f);
            }
            // Cinders travel outward (24-44 ticks)
            else if (cindersFired && ticksAlive < 44) {
                float dist = 6 + (ticksAlive - 24) * 0.5f;
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i;
                    cinderHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 2, Math.sin(angle) * dist));
                }
            }
            // Impact fire zones (tick 44)
            else if (cindersFired && ticksAlive == 44) {
                for (int i = 0; i < 5; i++) {
                    Location impactLoc = cinderHandles.get(i).entity().getLocation();
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(
                        impactLoc.clone().add(0, -impactLoc.getY() + center.getY() + 0.05, 0),
                        Material.ORANGE_STAINED_GLASS);
                    zone.scale(2.0f, 0.08f, 2.0f).glow(255, 80, 0).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CinderCluster(plugin); }
    }

    // ================================================================
    // 17. BRIMSTONE DETONATION — Death attack: massive explosion + permanent lava patch
    // ================================================================
    public static class BrimstoneDetonation extends BossAttack {
        private final List<BlockDisplayHandle> detonationHandles = new ArrayList<>();
        private boolean detonated = false;

        public BrimstoneDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_wraith_brimstone_detonation", AttackType.BOSS, 4), "ember_wraith");
            config.setDamage(16.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flash of soul fire white-out
            BlockDisplayHandle flash = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.SEA_LANTERN);
            flash.scale(4.0f, 4.0f, 4.0f).glow(0, 255, 255).interpolation(2, 0);
            detonationHandles.add(flash);
            spawnedEntities.add(flash.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flash (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !detonated) {
                // White-out effect
            }
            // Explosion (tick 10)
            else if (ticksAlive == 10 && !detonated) {
                detonated = true;
                // Massive brimstone burst wave
                for (int ring = 0; ring < 3; ring++) {
                    float radius = 3 + ring * 3;
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        BlockDisplayHandle wave = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius),
                            Material.MAGMA_BLOCK);
                        wave.scale(1.0f, 0.5f, 1.0f).glow(255, 80, 0).interpolation(1, 0);
                        detonationHandles.add(wave);
                        spawnedEntities.add(wave.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 50, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 2.0f, 0.6f);
            }
            // Post-explosion smoke cloud (10-110 ticks = 5 seconds)
            else if (detonated && ticksAlive == 15) {
                // Smoke cloud
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * 10;
                    double oy = Math.random() * 5;
                    double oz = (Math.random() - 0.5) * 10;
                    BlockDisplayHandle smoke = displayBuilder.spawnBlock(
                        center.clone().add(ox, oy, oz), Material.GRAY_STAINED_GLASS);
                    smoke.scale(2.0f, 1.5f, 2.0f).glow(30, 30, 30).interpolation(3, 0);
                    spawnedEntities.add(smoke.entity());
                }
            }
            // Permanent lava-glow death patch (tick 30)
            else if (detonated && ticksAlive == 30) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle patch = displayBuilder.spawnBlock(
                            center.clone().add(dx, 0.03, dz), Material.MAGMA_BLOCK);
                        patch.scale(0.9f, 0.06f, 0.9f).glow(255, 100, 0).interpolation(2, 0);
                        spawnedEntities.add(patch.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneDetonation(plugin); }
    }
}
