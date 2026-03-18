package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.boss;

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
 * Phase 3 Boss Attacks - TIER 4 "THE INFERNO" GROUP 1 (#61-70)
 * Dweller HP: 40%-20%. The brimstone environment becomes the weapon.
 * Corruption zones ignite, gravity slams, fire walks, eruption paths.
 * NO status effects - damage only.
 */
public final class InfernoStrike {

    private InfernoStrike() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TrailIgnite(plugin));
        registry.register(new BrimstoneAegis(plugin));
        registry.register(new GravitySlam(plugin));
        registry.register(new FireWalk(plugin));
        registry.register(new WrathDive(plugin));
        registry.register(new EruptionPath(plugin));
        registry.register(new MarkLeash(plugin));
        registry.register(new FourCornerBurn(plugin));
        registry.register(new DreadCollapse(plugin));
        registry.register(new HuntingPack(plugin));
    }

    // ================================================================
    // 61. TRAIL IGNITE - All corruption zones blaze to full flame
    // ================================================================
    public static class TrailIgnite extends BossAttack {
        private final List<BlockDisplayHandle> flameHandles = new ArrayList<>();
        private boolean ignited = false;

        public TrailIgnite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_trail_ignite", AttackType.BOSS, 3), "dweller");
            config.setDamage(6.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(230);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph: corruption zones begin to smolder - ring of fire tiles
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(angle) * 8, 0.1, Math.sin(angle) * 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.5f, 0.15f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                flameHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.crimsonDust(center, 20, 10.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph phase: smoldering pulses (0-30 ticks)
            if (ticksAlive < 30 && !ignited) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center, 15, 12.0);
                    DisplayBuilder.dustParticles(center, 10, 10.0, 255, 100, 0, 1.5f);
                }
            }
            // Full ignition at tick 30
            else if (ticksAlive == 30 && !ignited) {
                ignited = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                // Expand fire field outward
                for (int ring = 0; ring < 3; ring++) {
                    double radius = 5 + ring * 4;
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i + ring * 0.3;
                        Location loc = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_STAINED_GLASS);
                        h.scale(2.0f, 0.2f, 2.0f).glow(255, 100, 0).interpolation(5, 0);
                        flameHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.dustParticles(center, 40, 14.0, 255, 100, 0, 2.0f);
            }
            // Burning phase: persistent fire particles (30-200 ticks)
            else if (ignited && ticksAlive < 200) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 12, 12.0, 255, 100, 0, 1.5f);
                    DisplayBuilder.crimsonDust(center, 8, 10.0);
                }
                // Pulse existing flames
                if (ticksAlive % 20 == 0) {
                    for (BlockDisplayHandle h : flameHandles) {
                        float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.2) * 0.3f;
                        h.scale(1.5f * pulse, 0.2f, 1.5f * pulse);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailIgnite(plugin); }
    }

    // ================================================================
    // 62. BRIMSTONE AEGIS - Orbiting shard shield reflects damage
    // ================================================================
    public static class BrimstoneAegis extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean shattered = false;

        public BrimstoneAegis(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_aegis", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Orbiting brimstone shard fragments
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(angle) * 2, 1.5, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.5f, 0.8f, 0.5f).glow(200, 0, 50).interpolation(2, 0);
                shardHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.crimsonDust(center, 15, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (!shattered) {
                // Orbiting shield phase
                float angularSpeed = 0.12f;
                for (int i = 0; i < shardHandles.size(); i++) {
                    double angle = ticksAlive * angularSpeed + (Math.PI * 2 / shardHandles.size()) * i;
                    double yOff = 1.5 + Math.sin(ticksAlive * 0.1 + i) * 0.3;
                    shardHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2, yOff, Math.sin(angle) * 2));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center, 6, 2.5);
                    DisplayBuilder.dustParticles(center, 4, 2.0, 255, 100, 0, 1.0f);
                }
                // Shield shatters at tick 120 (6 seconds)
                if (ticksAlive == 120) {
                    shattered = true;
                    // Shards fly outward
                    for (int i = 0; i < shardHandles.size(); i++) {
                        double angle = (Math.PI * 2 / shardHandles.size()) * i;
                        Location outward = center.clone().add(Math.cos(angle) * 5, 1.5, Math.sin(angle) * 5);
                        shardHandles.get(i).entity().teleport(outward);
                    }
                    DisplayBuilder.crimsonDust(center, 30, 6.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneAegis(plugin); }
    }

    // ================================================================
    // 63. GRAVITY SLAM - Void tendril grabs and drops player onto corruption
    // ================================================================
    public static class GravitySlam extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean grabbed = false;
        private boolean dropped = false;

        public GravitySlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_gravity_slam", AttackType.BOSS, 3), "dweller");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Void tendril rising from dweller position
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, i * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 1.0f, 0.4f).glow(0, 150, 255).interpolation(2, 0);
                tendrilHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center, 15, 3.0, 0, 150, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendril extends upward (0-30 ticks)
            if (ticksAlive < 30 && !grabbed) {
                float progress = ticksAlive / 30.0f;
                for (int i = 0; i < tendrilHandles.size(); i++) {
                    double y = i * 1.0 * progress;
                    double sway = Math.sin(ticksAlive * 0.3 + i * 0.5) * 0.3;
                    tendrilHandles.get(i).entity().teleport(
                        center.clone().add(sway, y, sway * 0.5));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 8 * progress, 0),
                        6, 1.0, 0, 150, 255, 1.2f);
                }
            }
            // Grab and lift (tick 30)
            else if (ticksAlive == 30 && !grabbed) {
                grabbed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                // Tendril snaps to full height
                for (int i = 0; i < tendrilHandles.size(); i++) {
                    tendrilHandles.get(i).entity().teleport(
                        center.clone().add(0, i * 1.0, 0));
                }
            }
            // Hold phase (30-50 ticks)
            else if (grabbed && !dropped && ticksAlive < 50) {
                // Tendril holds at max height, pulsing
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 8, 0),
                        8, 1.5, 0, 150, 255, 1.5f);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 5, 1.0);
                }
            }
            // Drop (tick 50)
            else if (ticksAlive == 50 && !dropped) {
                dropped = true;
                // Impact zone eruption
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location impactLoc = center.clone().add(Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2);
                    BlockDisplayHandle ih = displayBuilder.spawnBlock(impactLoc, Material.MAGMA_BLOCK);
                    ih.scale(0.8f, 0.3f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    impactHandles.add(ih);
                    spawnedEntities.add(ih.entity());
                }
                DisplayBuilder.crimsonDust(center, 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.4f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravitySlam(plugin); }
    }

    // ================================================================
    // 64. FIRE WALK - Spiral walk igniting floor tiles
    // ================================================================
    public static class FireWalk extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean walking = false;
        private int walkTick = 0;
        private double spiralAngle = 0;
        private double spiralRadius = 12;

        public FireWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_fire_walk", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller feet ignite - ground fire at start
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 / 4) * i;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.05, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.6f, 0.1f, 0.6f).glow(255, 100, 0).interpolation(2, 0);
                trailHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center, 12, 1.5, 255, 100, 0, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Foot ignition telegraph (0-30 ticks)
            if (ticksAlive < 30 && !walking) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 1.0, 255, 100, 0, 1.2f);
                }
            }
            // Walk begins (tick 30)
            else if (ticksAlive == 30 && !walking) {
                walking = true;
                walkTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
            }
            // Spiral walk (30-230 ticks)
            else if (walking && walkTick < 200) {
                walkTick++;
                spiralAngle += 0.06;
                spiralRadius = Math.max(2, 12 - walkTick * 0.05);

                double x = Math.cos(spiralAngle) * spiralRadius;
                double z = Math.sin(spiralAngle) * spiralRadius;
                Location stepLoc = center.clone().add(x, 0.05, z);

                // Leave fire trail every 4 ticks
                if (walkTick % 4 == 0 && trailHandles.size() < 100) {
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(stepLoc, Material.NETHERRACK);
                    trail.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                    trailHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.dustParticles(stepLoc, 5, 0.5, 255, 100, 0, 1.0f);
                }
                // Lava eruption particles every 8 ticks
                if (walkTick % 8 == 0) {
                    DisplayBuilder.crimsonDust(stepLoc.clone().add(0, 1, 0), 8, 1.0);
                    DisplayBuilder.playSound(stepLoc, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireWalk(plugin); }
    }

    // ================================================================
    // 65. WRATH DIVE - Ceiling dive targeting Marked player
    // ================================================================
    public static class WrathDive extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean diveStarted = false;
        private boolean impacted = false;

        public WrathDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_wrath_dive", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller silhouette at ceiling height
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 25 + i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                float scale = 1.0f - i * 0.1f;
                h.scale(scale, 1.2f, scale).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ground target marker
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location crossLoc = center.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3);
                BlockDisplayHandle ch = displayBuilder.spawnBlock(crossLoc, Material.RED_STAINED_GLASS);
                ch.scale(0.4f, 0.1f, 0.4f).glow(200, 0, 50).interpolation(3, 0);
                impactHandles.add(ch);
                spawnedEntities.add(ch.entity());
            }
            DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 15, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Apex silhouette (0-30 ticks)
            if (ticksAlive < 30 && !diveStarted) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 8, 2.0);
                    DisplayBuilder.crimsonDust(center, 5, 3.0);
                }
            }
            // Dive begins (tick 30)
            else if (ticksAlive == 30 && !diveStarted) {
                diveStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
            }
            // Dive descent (30-50 ticks, fast)
            else if (diveStarted && !impacted && ticksAlive < 50) {
                int t = ticksAlive - 30;
                float progress = t / 20.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.04f));
                    double y = 25 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (t % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 25 * (1 - progress), 0), 8, 1.5);
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 25 * (1 - progress), 0), 5, 1.0, 255, 100, 0, 1.5f);
                }
            }
            // Impact (tick 50)
            else if (ticksAlive == 50 && !impacted) {
                impacted = true;
                for (BlockDisplayHandle ch : impactHandles) ch.entity().remove();
                impactHandles.clear();
                // Corruption zone expansion
                for (int ring = 0; ring < 2; ring++) {
                    double radius = 3 + ring * 3;
                    for (int i = 0; i < 16; i++) {
                        double angle = (Math.PI * 2 / 16) * i;
                        Location impactLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                        BlockDisplayHandle sh = displayBuilder.spawnBlock(impactLoc, Material.MAGMA_BLOCK);
                        sh.scale(0.8f, 0.2f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                        impactHandles.add(sh);
                        spawnedEntities.add(sh.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center, 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WrathDive(plugin); }
    }

    // ================================================================
    // 66. ERUPTION PATH - Line of brimstone eruptions toward target
    // ================================================================
    public static class EruptionPath extends BossAttack {
        private final List<BlockDisplayHandle> pathHandles = new ArrayList<>();
        private boolean eruptionFired = false;
        private int eruptTick = 0;

        public EruptionPath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_eruption_path", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial ground crack at dweller position
            BlockDisplayHandle start = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.CRACKED_STONE_BRICKS);
            start.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
            pathHandles.add(start);
            spawnedEntities.add(start.entity());
            DisplayBuilder.crimsonDust(center, 10, 1.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph rumble (0-20 ticks)
            if (ticksAlive < 20 && !eruptionFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 6, 2.0);
                }
            }
            // Eruption fires (tick 20)
            else if (ticksAlive == 20 && !eruptionFired) {
                eruptionFired = true;
                eruptTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 1.0f);
            }
            // Sequential eruptions along line (20-100 ticks)
            else if (eruptionFired && eruptTick < 80) {
                eruptTick++;
                // Eruption advances every 6 ticks
                if (eruptTick % 6 == 0 && pathHandles.size() < 50) {
                    int step = eruptTick / 6;
                    // Line extends in X direction toward 16 blocks
                    double xOff = step * 1.2;
                    Location eruptLoc = center.clone().add(xOff, 0.05, 0);
                    BlockDisplayHandle eh = displayBuilder.spawnBlock(eruptLoc, Material.NETHERRACK);
                    eh.scale(1.0f, 0.15f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    pathHandles.add(eh);
                    spawnedEntities.add(eh.entity());

                    // Eruption column above tile
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        eruptLoc.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
                    col.scale(0.5f, 1.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                    pathHandles.add(col);
                    spawnedEntities.add(col.entity());

                    DisplayBuilder.crimsonDust(eruptLoc, 8, 1.0);
                    DisplayBuilder.dustParticles(eruptLoc.clone().add(0, 1, 0),
                        5, 0.5, 255, 100, 0, 1.2f);
                    DisplayBuilder.playSound(eruptLoc, Sound.BLOCK_STONE_PLACE, 0.8f, 0.4f);
                }
            }
            // Final eruption at terminus
            else if (eruptTick == 80) {
                eruptTick++;
                DisplayBuilder.crimsonDust(center.clone().add(16, 0, 0), 25, 3.0);
                DisplayBuilder.playSound(center.clone().add(16, 0, 0),
                    Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EruptionPath(plugin); }
    }

    // ================================================================
    // 67. MARK LEASH - Void tether binds Marked player to Dweller
    // ================================================================
    public static class MarkLeash extends BossAttack {
        private final List<BlockDisplayHandle> tetherHandles = new ArrayList<>();
        private boolean tethered = false;

        public MarkLeash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_leash", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tether chain links
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(i * 0.8, 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 150, 255).interpolation(2, 0);
                tetherHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center, 12, 2.0, 0, 150, 255, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (!tethered && ticksAlive < 20) {
                // Tether forming
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 3.0, 0, 150, 255, 1.0f);
                }
            }
            else if (ticksAlive == 20) {
                tethered = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
            }
            // Tether active (20-180 ticks = 8 seconds)
            else if (tethered && ticksAlive < 180) {
                // Tether chain undulates
                for (int i = 0; i < tetherHandles.size(); i++) {
                    double wave = Math.sin(ticksAlive * 0.15 + i * 0.6) * 0.4;
                    double x = i * 0.8;
                    double yWave = 1.0 + wave;
                    tetherHandles.get(i).entity().teleport(
                        center.clone().add(x, yWave, wave * 0.3));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 5, 4.0, 0, 150, 255, 1.0f);
                    DisplayBuilder.crimsonDust(center, 3, 2.0);
                }
                // Tether tightening visual (getting urgent)
                if (ticksAlive > 140 && ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkLeash(plugin); }
    }

    // ================================================================
    // 68. FOUR-CORNER BURN - Brimstone pillar crossfire from corners
    // ================================================================
    public static class FourCornerBurn extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private boolean pillarsUp = false;
        private int fireTick = 0;

        public FourCornerBurn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_four_corner_burn", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Four corner pillars
            double[][] corners = {{-14, 0, -14}, {14, 0, -14}, {-14, 0, 14}, {14, 0, 14}};
            for (double[] corner : corners) {
                for (int y = 0; y < 5; y++) {
                    Location loc = center.clone().add(corner[0], y, corner[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(1.2f, 1.0f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
                    pillarHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pillars rise (0-30 ticks)
            if (ticksAlive < 30 && !pillarsUp) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 14.0, 255, 100, 0, 1.2f);
                }
            }
            // Firing begins (tick 30)
            else if (ticksAlive == 30 && !pillarsUp) {
                pillarsUp = true;
                fireTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.8f);
            }
            // Alternating bolt pairs fire (30-270 ticks)
            else if (pillarsUp && fireTick < 240) {
                fireTick++;
                // Fire bolt every 40 ticks, alternating pairs
                if (fireTick % 40 == 0) {
                    boolean pairA = (fireTick / 40) % 2 == 0;
                    // Clear old bolts
                    for (BlockDisplayHandle bh : boltHandles) bh.entity().remove();
                    boltHandles.clear();

                    if (pairA) {
                        // NW-SE diagonal bolt line
                        for (int i = 0; i < 20; i++) {
                            float t = i / 19.0f;
                            Location boltLoc = center.clone().add(
                                -14 + 28 * t, 2.5, -14 + 28 * t);
                            BlockDisplayHandle bh = displayBuilder.spawnBlock(boltLoc, Material.ORANGE_STAINED_GLASS);
                            bh.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 0).interpolation(1, 0);
                            boltHandles.add(bh);
                            spawnedEntities.add(bh.entity());
                        }
                    } else {
                        // NE-SW diagonal bolt line
                        for (int i = 0; i < 20; i++) {
                            float t = i / 19.0f;
                            Location boltLoc = center.clone().add(
                                14 - 28 * t, 2.5, -14 + 28 * t);
                            BlockDisplayHandle bh = displayBuilder.spawnBlock(boltLoc, Material.ORANGE_STAINED_GLASS);
                            bh.scale(0.6f, 0.6f, 0.6f).glow(200, 0, 50).interpolation(1, 0);
                            boltHandles.add(bh);
                            spawnedEntities.add(bh.entity());
                        }
                    }
                    DisplayBuilder.crimsonDust(center, 15, 10.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.2f);
                }
                // Pillar ambient glow
                if (fireTick % 15 == 0) {
                    DisplayBuilder.dustParticles(center, 5, 14.0, 255, 100, 0, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FourCornerBurn(plugin); }
    }

    // ================================================================
    // 69. DREAD COLLAPSE - Charges high-Dread player with catastrophic impact
    // ================================================================
    public static class DreadCollapse extends BossAttack {
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private boolean charging = false;
        private boolean impacted = false;
        private int chargeTick = 0;

        public DreadCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_dread_collapse", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller eyes crackling white - charge buildup
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.CRYING_OBSIDIAN);
            core.scale(1.0f, 1.0f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
            chargeHandles.add(core);
            spawnedEntities.add(core.entity());

            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 2, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 150, 255).interpolation(2, 0);
                chargeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center, 15, 2.0, 0, 150, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge telegraph (0-30 ticks)
            if (ticksAlive < 30 && !charging) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                        8, 2.0, 0, 150, 255, 1.5f);
                }
                // Energy condensing toward core
                float shrink = 1.0f - ticksAlive / 60.0f;
                for (int i = 1; i < chargeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 6) * (i - 1);
                    double r = 1.5 * shrink;
                    chargeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * r, 2, Math.sin(angle) * r));
                }
            }
            // Charge begins (tick 30)
            else if (ticksAlive == 30 && !charging) {
                charging = true;
                chargeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.6f);
            }
            // Charge forward (30-50 ticks)
            else if (charging && !impacted && chargeTick < 20) {
                chargeTick++;
                float progress = chargeTick / 20.0f;
                double chargeX = progress * 12;
                for (BlockDisplayHandle h : chargeHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(center.clone().add(chargeX, loc.getY() - center.getY(), loc.getZ() - center.getZ()));
                }
                if (chargeTick % 3 == 0) {
                    DisplayBuilder.dustParticles(
                        center.clone().add(chargeX, 2, 0), 6, 1.0, 0, 150, 255, 1.2f);
                }
            }
            // Impact (tick 50)
            else if (chargeTick >= 20 && !impacted) {
                impacted = true;
                Location impactLoc = center.clone().add(12, 0, 0);
                // Dread collapse detonation
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location debrisLoc = impactLoc.clone().add(Math.cos(angle) * 4, 0.5, Math.sin(angle) * 4);
                    BlockDisplayHandle dh = displayBuilder.spawnBlock(debrisLoc, Material.CRYING_OBSIDIAN);
                    dh.scale(0.5f, 0.5f, 0.5f).glow(0, 150, 255).interpolation(2, 0);
                    chargeHandles.add(dh);
                    spawnedEntities.add(dh.entity());
                }
                DisplayBuilder.dustParticles(impactLoc, 35, 5.0, 0, 150, 255, 2.0f);
                DisplayBuilder.crimsonDust(impactLoc, 20, 4.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DreadCollapse(plugin); }
    }

    // ================================================================
    // 70. HUNTING PACK - Two shadow clones target different players
    // ================================================================
    public static class HuntingPack extends BossAttack {
        private final List<BlockDisplayHandle> cloneHandlesA = new ArrayList<>();
        private final List<BlockDisplayHandle> cloneHandlesB = new ArrayList<>();
        private boolean clonesDeployed = false;
        private int cloneTick = 0;

        public HuntingPack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_hunting_pack", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(340);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Clone A - left flank
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-3, i * 0.8, -2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.7f, 0.8f, 0.7f).glow(200, 0, 50).interpolation(3, 0);
                cloneHandlesA.add(h);
                spawnedEntities.add(h.entity());
            }
            // Clone B - right flank
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(3, i * 0.8, 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.7f, 0.8f, 0.7f).glow(200, 0, 50).interpolation(3, 0);
                cloneHandlesB.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
            DisplayBuilder.crimsonDust(center, 20, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Clone formation (0-20 ticks)
            if (ticksAlive < 20 && !clonesDeployed) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-3, 1, -2), 5, 1.0);
                    DisplayBuilder.crimsonDust(center.clone().add(3, 1, 2), 5, 1.0);
                }
            }
            // Clones deploy (tick 20)
            else if (ticksAlive == 20 && !clonesDeployed) {
                clonesDeployed = true;
                cloneTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            }
            // Clone hunting phase (20-320 ticks = 15 seconds)
            else if (clonesDeployed && cloneTick < 300) {
                cloneTick++;
                float progress = cloneTick / 300.0f;
                // Clone A circles left
                double angleA = cloneTick * 0.05;
                double radiusA = 6 + Math.sin(cloneTick * 0.03) * 3;
                for (int i = 0; i < cloneHandlesA.size(); i++) {
                    double a = angleA + i * 0.15;
                    cloneHandlesA.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * radiusA, i * 0.8, Math.sin(a) * radiusA));
                }
                // Clone B circles right
                double angleB = -cloneTick * 0.05 + Math.PI;
                double radiusB = 6 + Math.cos(cloneTick * 0.04) * 3;
                for (int i = 0; i < cloneHandlesB.size(); i++) {
                    double b = angleB + i * 0.15;
                    cloneHandlesB.get(i).entity().teleport(
                        center.clone().add(Math.cos(b) * radiusB, i * 0.8, Math.sin(b) * radiusB));
                }
                // Hunt particles
                if (cloneTick % 15 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(Math.cos(angleA) * radiusA, 1, Math.sin(angleA) * radiusA), 5, 1.0);
                    DisplayBuilder.crimsonDust(
                        center.clone().add(Math.cos(angleB) * radiusB, 1, Math.sin(angleB) * radiusB), 5, 1.0);
                }
            }
            // Clones dissolve - vulnerability window
            else if (cloneTick >= 300) {
                if (cloneTick == 300) {
                    DisplayBuilder.crimsonDust(center, 25, 6.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                }
                cloneTick++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HuntingPack(plugin); }
    }
}
