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
 * Phase 4D Mini Boss 1 — THE VOID SHARD
 * A shattered fragment of Voidmaw essence — purple void energy.
 * Floating obsidian core with orbiting shard cluster, PORTAL particle body.
 * 20 attacks total.
 * NO status effects — damage only.
 */
public final class VoidShardAttacks {

    private VoidShardAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TendrilLash(plugin));
        registry.register(new DimensionalPull(plugin));
        registry.register(new ShardSpray(plugin));
        registry.register(new VoidZoneSeed(plugin));
        registry.register(new CoreSurge(plugin));
        registry.register(new RiftWalk(plugin));
        registry.register(new GravityInversion(plugin));
        registry.register(new VoidLattice(plugin));
        registry.register(new ShardStorm(plugin));
        registry.register(new SingularityPoint(plugin));
        registry.register(new VoidBlink(plugin));
        registry.register(new OrbitDisruption(plugin));
        registry.register(new DarkMatterAnchor(plugin));
        registry.register(new FractureLine(plugin));
        registry.register(new VoidMirror(plugin));
        registry.register(new PhaseTear(plugin));
        registry.register(new NullPressure(plugin));
        registry.register(new ShardBarrier(plugin));
        registry.register(new ConsumingDarkness(plugin));
        registry.register(new FinalCollapse(plugin));
    }

    // ================================================================
    // 1. TENDRIL LASH — Fast melee tendril projectile from shard cluster
    // ================================================================
    public static class TendrilLash extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private boolean tendrilFired = false;
        private int fireStartTick = 0;

        public TendrilLash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_tendril_lash", AttackType.BOSS, 4), "void_shard");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(120);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Shard cluster splits — one fragment vibrates outward
            BlockDisplayHandle clusterShard = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 1.5), Material.OBSIDIAN);
            clusterShard.scale(0.3f, 0.3f, 0.8f).glow(80, 0, 180).interpolation(2, 0);
            tendrilHandles.add(clusterShard);
            spawnedEntities.add(clusterShard.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vibration telegraph (0-24 ticks = 1.2 seconds)
            if (ticksAlive < 24 && !tendrilFired) {
                if (ticksAlive % 4 == 0) {
                    float offsetX = (float) (Math.random() - 0.5) * 0.3f;
                    float offsetZ = (float) (Math.random() - 0.5) * 0.3f;
                    tendrilHandles.get(0).entity().teleport(
                        center.clone().add(offsetX, 3, 1.5 + offsetZ));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 1.5), 4, 0.5);
                }
            }
            // Tendril fires forward (tick 24)
            else if (ticksAlive == 24 && !tendrilFired) {
                tendrilFired = true;
                fireStartTick = ticksAlive;
                // Create 12-block tendril line of connected PORTAL-colored segments
                for (int i = 0; i < 12; i++) {
                    Location segLoc = center.clone().add(0, 2.5, 1.5 + i * 1.0);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                    seg.scale(0.15f, 0.15f, 0.8f).glow(100, 0, 200).interpolation(1, 0);
                    tendrilHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 2.0f);
            }
            // Tendril dissolves (24-44 ticks)
            else if (tendrilFired && ticksAlive - fireStartTick < 20) {
                if (ticksAlive % 4 == 0) {
                    int segIdx = (ticksAlive - fireStartTick) / 4;
                    if (segIdx + 1 < tendrilHandles.size()) {
                        DisplayBuilder.cyanDust(
                            tendrilHandles.get(segIdx + 1).entity().getLocation(), 3, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TendrilLash(plugin); }
    }

    // ================================================================
    // 2. DIMENSIONAL PULL — Gravitational pull toward the Shard core
    // ================================================================
    public static class DimensionalPull extends BossAttack {
        private final List<BlockDisplayHandle> pullHandles = new ArrayList<>();
        private boolean pullActive = false;
        private int pullStartTick = 0;

        public DimensionalPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_dimensional_pull", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Core pulse rings expanding outward — 3 pulses
            BlockDisplayHandle coreGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.CRYING_OBSIDIAN);
            coreGlow.scale(1.0f, 1.0f, 1.0f).glow(120, 0, 200).interpolation(3, 0);
            pullHandles.add(coreGlow);
            spawnedEntities.add(coreGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 12, 10.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pulse telegraph (0-40 ticks = 2 seconds, 3 pulses)
            if (ticksAlive < 40 && !pullActive) {
                if (ticksAlive == 0 || ticksAlive == 13 || ticksAlive == 26) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 10, 10.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.6f);
                }
            }
            // Pull activates (tick 40) — sustained for 60 ticks (3 seconds)
            else if (ticksAlive == 40 && !pullActive) {
                pullActive = true;
                pullStartTick = ticksAlive;
                // Gravitational distortion core — intensified BlockDisplay
                BlockDisplayHandle distortion = displayBuilder.spawnBlock(
                    center.clone().add(0, 3, 0), Material.PURPLE_STAINED_GLASS);
                distortion.scale(2.0f, 2.0f, 2.0f).glow(160, 0, 255).interpolation(4, 0);
                pullHandles.add(distortion);
                spawnedEntities.add(distortion.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
            }
            // Pull sustained (40-100 ticks)
            else if (pullActive && ticksAlive - pullStartTick < 60) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 2.0);
                }
            }
            // Pull ends — vacuum burst
            else if (pullActive && ticksAlive - pullStartTick == 60) {
                pullActive = false;
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalPull(plugin); }
    }

    // ================================================================
    // 3. SHARD SPRAY — 8 obsidian shard projectiles in radial spread
    // ================================================================
    public static class ShardSpray extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean shardsFired = false;
        private int fireStartTick = 0;

        public ShardSpray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_shard_spray", AttackType.BOSS, 4), "void_shard");
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
            // Orbiting cluster freezes — shards point outward like bristles
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location shardLoc = center.clone().add(
                    Math.cos(angle) * 1.5, 3, Math.sin(angle) * 1.5);
                BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.OBSIDIAN);
                shard.scale(0.2f, 0.2f, 0.6f).glow(80, 0, 160).interpolation(2, 0);
                shardHandles.add(shard);
                spawnedEntities.add(shard.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Freeze telegraph (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !shardsFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 2.0);
                }
            }
            // Shards fire outward (tick 20)
            else if (ticksAlive == 20 && !shardsFired) {
                shardsFired = true;
                fireStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.4f);
            }
            // Shards travel outward (20-50 ticks)
            else if (shardsFired && ticksAlive - fireStartTick < 30) {
                float dist = (ticksAlive - fireStartTick) * 0.7f;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location shardLoc = center.clone().add(
                        Math.cos(angle) * (1.5 + dist), 3 - dist * 0.15, Math.sin(angle) * (1.5 + dist));
                    if (i < shardHandles.size()) {
                        shardHandles.get(i).entity().teleport(shardLoc);
                    }
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 4, dist);
                }
            }
            // Shards embed in ground (tick 50)
            else if (shardsFired && ticksAlive - fireStartTick == 30) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location embedLoc = center.clone().add(
                        Math.cos(angle) * 22.5, 0.1, Math.sin(angle) * 22.5);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(embedLoc, Material.OBSIDIAN);
                    tile.scale(0.8f, 0.1f, 0.8f).glow(60, 0, 120).interpolation(2, 0);
                    shardHandles.add(tile);
                    spawnedEntities.add(tile.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardSpray(plugin); }
    }

    // ================================================================
    // 4. VOID ZONE SEED — Places 3 temporary void zones on the ground
    // ================================================================
    public static class VoidZoneSeed extends BossAttack {
        private final List<BlockDisplayHandle> zoneHandles = new ArrayList<>();
        private boolean zonesPlaced = false;

        public VoidZoneSeed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_void_zone_seed", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 dark rings appear on the ground at random offsets
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                Location ringLoc = center.clone().add(ox, 0.05, oz);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.PURPLE_STAINED_GLASS);
                ring.scale(0.2f, 0.05f, 0.2f).glow(100, 0, 200).interpolation(4, 0);
                zoneHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rings expand (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !zonesPlaced) {
                float scale = 0.2f + (ticksAlive / 30.0f) * 3.8f;
                for (BlockDisplayHandle ring : zoneHandles) {
                    ring.entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 0, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, 0.05f, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle ring : zoneHandles) {
                        DisplayBuilder.cyanDust(ring.entity().getLocation(), 6, 2.0);
                    }
                }
            }
            // Zones activate (tick 30)
            else if (ticksAlive == 30 && !zonesPlaced) {
                zonesPlaced = true;
                // Replace ring displays with full void zone tiles
                for (BlockDisplayHandle ring : zoneHandles) {
                    Location zoneLoc = ring.entity().getLocation();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockDisplayHandle tile = displayBuilder.spawnBlock(
                                zoneLoc.clone().add(dx, 0.03, dz), Material.CRYING_OBSIDIAN);
                            tile.scale(0.9f, 0.06f, 0.9f).glow(120, 0, 200).interpolation(2, 0);
                            spawnedEntities.add(tile.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.8f);
            }
            // Zone ambient particles (30-330 ticks = 15 seconds of zones)
            else if (zonesPlaced && ticksAlive < 330) {
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle ring : zoneHandles) {
                        DisplayBuilder.cyanDust(ring.entity().getLocation(), 4, 2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidZoneSeed(plugin); }
    }

    // ================================================================
    // 5. CORE SURGE — Radial explosion from the Shard's core
    // ================================================================
    public static class CoreSurge extends BossAttack {
        private final List<BlockDisplayHandle> surgeHandles = new ArrayList<>();
        private boolean surgeDetonated = false;

        public CoreSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_core_surge", AttackType.BOSS, 4), "void_shard");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Core intensifies — shards retract inward
            BlockDisplayHandle coreGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.GLOWSTONE);
            coreGlow.scale(1.5f, 1.5f, 1.5f).glow(200, 100, 255).interpolation(5, 0);
            surgeHandles.add(coreGlow);
            spawnedEntities.add(coreGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Core "inhaling" — shrinks and brightens (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !surgeDetonated) {
                float scale = 1.5f - (ticksAlive / 40.0f) * 1.0f;
                surgeHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 3 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 10, 3.0);
                }
            }
            // Core detonates (tick 40)
            else if (ticksAlive == 40 && !surgeDetonated) {
                surgeDetonated = true;
                // Explosion ring of obsidian fragments
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location fragLoc = center.clone().add(Math.cos(angle) * 2, 3, Math.sin(angle) * 2);
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(fragLoc, Material.OBSIDIAN);
                    frag.scale(0.3f, 0.3f, 0.3f).glow(160, 0, 255).interpolation(1, 0);
                    surgeHandles.add(frag);
                    spawnedEntities.add(frag.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }
            // Fragments fly outward (40-60 ticks)
            else if (surgeDetonated && ticksAlive < 60) {
                float dist = (ticksAlive - 40) * 0.5f;
                for (int i = 1; i < surgeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 12) * (i - 1);
                    surgeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 3 - dist * 0.1, Math.sin(angle) * (2 + dist)));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CoreSurge(plugin); }
    }

    // ================================================================
    // 6. RIFT WALK — Teleport with AoE on arrival
    // ================================================================
    public static class RiftWalk extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean teleported = false;

        public RiftWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_rift_walk", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Shard starts flickering — alternating visibility
            BlockDisplayHandle flickerCore = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.OBSIDIAN);
            flickerCore.scale(1.2f, 1.2f, 1.2f).glow(100, 0, 200).interpolation(2, 0);
            riftHandles.add(flickerCore);
            spawnedEntities.add(flickerCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flicker telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !teleported) {
                // Toggle visibility by scaling
                boolean visible = (ticksAlive / 2) % 2 == 0;
                float s = visible ? 1.2f : 0.1f;
                riftHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-s / 2, 3 - s / 2, -s / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(s, s, s),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 1.5);
                }
            }
            // Teleport (tick 30) — departure implosion
            else if (ticksAlive == 30 && !teleported) {
                teleported = true;
                // Departure implosion visual
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 25, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);

                // Arrival at offset location
                double arrivalX = (Math.random() - 0.5) * 16;
                double arrivalZ = (Math.random() - 0.5) * 16;
                Location arrivalLoc = center.clone().add(arrivalX, 0, arrivalZ);

                // Arrival eruption display
                BlockDisplayHandle arrivalBurst = displayBuilder.spawnBlock(
                    arrivalLoc.clone().add(0, 3, 0), Material.CRYING_OBSIDIAN);
                arrivalBurst.scale(2.0f, 2.0f, 2.0f).glow(160, 0, 255).interpolation(3, 0);
                riftHandles.add(arrivalBurst);
                spawnedEntities.add(arrivalBurst.entity());

                DisplayBuilder.cyanDust(arrivalLoc.clone().add(0, 3, 0), 20, 4.0);
                DisplayBuilder.playSound(arrivalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.9f);
                DisplayBuilder.playSound(arrivalLoc, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.6f);
            }
            // Void tear residue at departure (30-130 ticks = 5 seconds)
            else if (teleported && ticksAlive < 130) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 4, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftWalk(plugin); }
    }

    // ================================================================
    // 7. GRAVITY INVERSION — Horizontal disk launches players upward
    // ================================================================
    public static class GravityInversion extends BossAttack {
        private final List<BlockDisplayHandle> diskHandles = new ArrayList<>();
        private boolean diskFired = false;

        public GravityInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_gravity_inversion", AttackType.BOSS, 4), "void_shard");
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Horizontal disk of PORTAL energy forming at 6-block altitude
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location diskPt = center.clone().add(Math.cos(angle) * 4, 6, Math.sin(angle) * 4);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(diskPt, Material.PURPLE_STAINED_GLASS);
                seg.scale(2.0f, 0.1f, 2.0f).glow(120, 0, 200).interpolation(3, 0);
                diskHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Disk expands outward (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !diskFired) {
                float radius = 4 + (ticksAlive / 40.0f) * 8;
                for (int i = 0; i < diskHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    diskHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 6, Math.sin(angle) * radius));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 10, radius);
                }
            }
            // Disk fires — inversion pulse (tick 40)
            else if (ticksAlive == 40 && !diskFired) {
                diskFired = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 30, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.5f);
            }
            // Disk fades (40-60 ticks)
            else if (diskFired && ticksAlive < 60) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 6, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversion(plugin); }
    }

    // ================================================================
    // 8. VOID LATTICE — Grid of obsidian low walls across the arena
    // ================================================================
    public static class VoidLattice extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean wallsRisen = false;
        private int riseStartTick = 0;

        public VoidLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_void_lattice", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground-level grid lines forming — 4 crossing lines
            for (int line = 0; line < 4; line++) {
                double angle = (Math.PI / 4) * line;
                for (int seg = -10; seg <= 10; seg++) {
                    Location linePt = center.clone().add(
                        Math.cos(angle) * seg, 0.05, Math.sin(angle) * seg);
                    BlockDisplayHandle gridLine = displayBuilder.spawnBlock(linePt, Material.OBSIDIAN);
                    gridLine.scale(0.3f, 0.05f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                    wallHandles.add(gridLine);
                    spawnedEntities.add(gridLine.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.2f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Grid lines glow (0-50 ticks = 2.5 seconds)
            if (ticksAlive < 50 && !wallsRisen) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 10.0);
                }
            }
            // Walls rise (tick 50)
            else if (ticksAlive == 50 && !wallsRisen) {
                wallsRisen = true;
                riseStartTick = ticksAlive;
                // Rise all grid segments to 1-block height
                for (BlockDisplayHandle wall : wallHandles) {
                    Location wallLoc = wall.entity().getLocation();
                    wall.entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, 0, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 1.0f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.5f, 0.7f);
            }
            // Walls active (50-250 ticks = 10 seconds)
            else if (wallsRisen && ticksAlive < 250) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 6, 10.0);
                }
            }
            // Walls sink (tick 250)
            else if (wallsRisen && ticksAlive == 250) {
                for (BlockDisplayHandle wall : wallHandles) {
                    wall.entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, 0, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.05f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidLattice(plugin); }
    }

    // ================================================================
    // 9. SHARD STORM — 12 shards fire downward in radial pattern
    // ================================================================
    public static class ShardStorm extends BossAttack {
        private final List<BlockDisplayHandle> stormHandles = new ArrayList<>();
        private boolean stormFired = false;
        private int fireStartTick = 0;

        public ShardStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_shard_storm", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 12 shards form ring at 8-block altitude
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location shardLoc = center.clone().add(Math.cos(angle) * 3, 8, Math.sin(angle) * 3);
                BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.OBSIDIAN);
                shard.scale(0.25f, 0.6f, 0.25f).glow(80, 0, 160).interpolation(2, 0);
                stormHandles.add(shard);
                spawnedEntities.add(shard.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spin-up telegraph (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !stormFired) {
                float rotSpeed = ticksAlive * 0.02f;
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i + ticksAlive * rotSpeed;
                    stormHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 8, Math.sin(angle) * 3));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 8, 3.0);
                }
            }
            // Shards fire downward (tick 36)
            else if (ticksAlive == 36 && !stormFired) {
                stormFired = true;
                fireStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.2f);
            }
            // Shards descend to ground (36-56 ticks)
            else if (stormFired && ticksAlive - fireStartTick < 20) {
                float drop = (ticksAlive - fireStartTick) * 0.4f;
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    float radius = 3 + (ticksAlive - fireStartTick) * 0.35f;
                    stormHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 8 - drop, Math.sin(angle) * radius));
                }
            }
            // Impact — landing pillars (tick 56)
            else if (stormFired && ticksAlive - fireStartTick == 20) {
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location impactLoc = center.clone().add(
                        Math.cos(angle) * 10, 0.05, Math.sin(angle) * 10);
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(impactLoc, Material.BLACK_CONCRETE_POWDER);
                    pillar.scale(0.8f, 0.1f, 0.8f).glow(60, 0, 120).interpolation(2, 0);
                    spawnedEntities.add(pillar.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardStorm(plugin); }
    }

    // ================================================================
    // 10. SINGULARITY POINT — Major attack: expanding detonation + permanent void zone
    // ================================================================
    public static class SingularityPoint extends BossAttack {
        private final List<BlockDisplayHandle> singularityHandles = new ArrayList<>();
        private boolean detonated = false;

        public SingularityPoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_singularity_point", AttackType.BOSS, 4), "void_shard");
            config.setDamage(16.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Single bright point begins forming at random arena location
            double ox = (Math.random() - 0.5) * 14;
            double oz = (Math.random() - 0.5) * 14;
            Location singLoc = center.clone().add(ox, 2, oz);
            BlockDisplayHandle singPoint = displayBuilder.spawnBlock(singLoc, Material.GLOWSTONE);
            singPoint.scale(0.1f, 0.1f, 0.1f).glow(200, 100, 255).interpolation(5, 0);
            singularityHandles.add(singPoint);
            spawnedEntities.add(singPoint.entity());
            DisplayBuilder.playSound(singLoc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (singularityHandles.isEmpty()) return;

            Location singLoc = singularityHandles.get(0).entity().getLocation();

            // Charging — sphere expands (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !detonated) {
                float scale = 0.1f + (ticksAlive / 60.0f) * 0.9f;
                singularityHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(singLoc, 8, 1.5);
                }
            }
            // Detonation (tick 60)
            else if (ticksAlive == 60 && !detonated) {
                detonated = true;
                // Massive particle burst
                DisplayBuilder.cyanDust(singLoc, 50, 14.0);

                // Vertical pillar at detonation point
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle pillarSeg = displayBuilder.spawnBlock(
                        singLoc.clone().add(0, y, 0), Material.END_ROD);
                    pillarSeg.scale(0.3f, 0.8f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
                    singularityHandles.add(pillarSeg);
                    spawnedEntities.add(pillarSeg.entity());
                }

                // Permanent void zone at center
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(
                            singLoc.clone().add(dx, 0.03, dz), Material.CRYING_OBSIDIAN);
                        zone.scale(0.9f, 0.06f, 0.9f).glow(120, 0, 200).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                    }
                }

                DisplayBuilder.playSound(singLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(singLoc, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SingularityPoint(plugin); }
    }

    // ================================================================
    // 11. VOID BLINK — Instant melee teleport to highest-health player
    // ================================================================
    public static class VoidBlink extends BossAttack {
        private final List<BlockDisplayHandle> blinkHandles = new ArrayList<>();
        private boolean blinked = false;

        public VoidBlink(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_void_blink", AttackType.BOSS, 4), "void_shard");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.OBSIDIAN);
            core.scale(0.8f, 0.8f, 0.8f).glow(100, 0, 200).interpolation(1, 0);
            blinkHandles.add(core);
            spawnedEntities.add(core.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Cluster stops for 0.3 seconds (0-6 ticks) — minimal warning
            if (ticksAlive < 6 && !blinked) {
                // Stillness — no movement, no particles
            }
            // Blink (tick 6) — instant teleport and contact
            else if (ticksAlive == 6 && !blinked) {
                blinked = true;
                // Arrival burst
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.3f);

                // Departure puff
                BlockDisplayHandle departPuff = displayBuilder.spawnBlock(
                    center.clone().add(0, 3, 0), Material.PURPLE_STAINED_GLASS);
                departPuff.scale(1.5f, 1.5f, 1.5f).glow(80, 0, 160).interpolation(3, 0);
                blinkHandles.add(departPuff);
                spawnedEntities.add(departPuff.entity());
            }
            // Retreat (6-18 ticks)
            else if (blinked && ticksAlive < 18) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 4, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBlink(plugin); }
    }

    // ================================================================
    // 12. ORBIT DISRUPTION — 6 shards break orbit and fly at players
    // ================================================================
    public static class OrbitDisruption extends BossAttack {
        private final List<BlockDisplayHandle> orbitHandles = new ArrayList<>();
        private boolean shardsFired = false;
        private int fireStartTick = 0;

        public OrbitDisruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_orbit_disruption", AttackType.BOSS, 4), "void_shard");
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
            // 6 shards in orbit stuttering
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location shardLoc = center.clone().add(Math.cos(angle) * 2, 3, Math.sin(angle) * 2);
                BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.OBSIDIAN);
                shard.scale(0.2f, 0.2f, 0.5f).glow(80, 0, 160).interpolation(1, 0);
                orbitHandles.add(shard);
                spawnedEntities.add(shard.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.0f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Stutter telegraph (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !shardsFired) {
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    double jitter = (Math.random() - 0.5) * 0.4;
                    orbitHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + jitter), 3, Math.sin(angle) * (2 + jitter)));
                }
            }
            // Shards fire outward (tick 20)
            else if (ticksAlive == 20 && !shardsFired) {
                shardsFired = true;
                fireStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.5f, 1.6f);
            }
            // Shards travel outward (20-60 ticks)
            else if (shardsFired && ticksAlive - fireStartTick < 40) {
                float dist = (ticksAlive - fireStartTick) * 0.5f;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    orbitHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 3, Math.sin(angle) * (2 + dist)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 4, 2 + dist);
                }
            }
            // Shards vanish with puff at max range (tick 60)
            else if (shardsFired && ticksAlive - fireStartTick == 40) {
                for (int i = 0; i < 6; i++) {
                    DisplayBuilder.cyanDust(orbitHandles.get(i).entity().getLocation(), 4, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitDisruption(plugin); }
    }

    // ================================================================
    // 13. DARK MATTER ANCHOR — Debuff ring that slows all players inside
    // ================================================================
    public static class DarkMatterAnchor extends BossAttack {
        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private boolean anchorActive = false;

        public DarkMatterAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_dark_matter_anchor", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Expanding ring of void particles
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 2, 3, Math.sin(angle) * 2);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                seg.scale(0.5f, 0.1f, 0.5f).glow(100, 0, 200).interpolation(3, 0);
                anchorHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring expands (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !anchorActive) {
                float radius = 2 + (ticksAlive / 40.0f) * 6;
                for (int i = 0; i < anchorHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    anchorHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 3, Math.sin(angle) * radius));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, radius);
                }
            }
            // Anchor activates (tick 40) — debuff aura for 100 ticks (5 seconds)
            else if (ticksAlive == 40 && !anchorActive) {
                anchorActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
            }
            // Anchor active (40-140 ticks)
            else if (anchorActive && ticksAlive < 140) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkMatterAnchor(plugin); }
    }

    // ================================================================
    // 14. FRACTURE LINE — Ground line erupts into wall of void energy
    // ================================================================
    public static class FractureLine extends BossAttack {
        private final List<BlockDisplayHandle> lineHandles = new ArrayList<>();
        private boolean lineDetonated = false;
        private int lineLength = 0;

        public FractureLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_fracture_line", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Starting point of the line at the Shard's position
            BlockDisplayHandle startPt = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 0), Material.OBSIDIAN);
            startPt.scale(0.4f, 0.05f, 0.4f).glow(100, 0, 200).interpolation(2, 0);
            lineHandles.add(startPt);
            spawnedEntities.add(startPt.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Line grows toward a direction (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !lineDetonated) {
                if (ticksAlive % 2 == 0 && lineLength < 14) {
                    lineLength++;
                    Location segLoc = center.clone().add(0, 0.05, lineLength);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                    seg.scale(0.4f, 0.05f, 0.4f).glow(100, 0, 200).interpolation(2, 0);
                    lineHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.6f, 0.6f);
                }
            }
            // Line detonates — wall eruption (tick 30)
            else if (ticksAlive == 30 && !lineDetonated) {
                lineDetonated = true;
                // Raise all line segments into 3-block walls
                for (BlockDisplayHandle seg : lineHandles) {
                    Location wallLoc = seg.entity().getLocation();
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(
                            wallLoc.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                        wall.scale(0.4f, 0.8f, 0.4f).glow(160, 0, 255).interpolation(1, 0);
                        spawnedEntities.add(wall.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 7), 20, 7.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
            // Wall fades (30-50 ticks = 1 second of wall)
            else if (lineDetonated && ticksAlive == 50) {
                // Leave scar tiles on ground
                for (int i = 0; i <= lineLength; i++) {
                    BlockDisplayHandle scar = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.03, i), Material.BLACK_CONCRETE);
                    scar.scale(0.5f, 0.04f, 0.5f).glow(40, 0, 80).interpolation(2, 0);
                    spawnedEntities.add(scar.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureLine(plugin); }
    }

    // ================================================================
    // 15. VOID MIRROR — Hexagonal reflective shield around the Shard
    // ================================================================
    public static class VoidMirror extends BossAttack {
        private final List<BlockDisplayHandle> mirrorHandles = new ArrayList<>();
        private boolean shieldActive = false;
        private int shieldStartTick = 0;

        public VoidMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_void_mirror", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hexagonal shield panels forming
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location panelLoc = center.clone().add(
                    Math.cos(angle) * 2.5, 3, Math.sin(angle) * 2.5);
                BlockDisplayHandle panel = displayBuilder.spawnBlock(panelLoc, Material.END_ROD);
                panel.scale(0.1f, 2.0f, 1.5f).glow(200, 100, 255).interpolation(3, 0);
                mirrorHandles.add(panel);
                spawnedEntities.add(panel.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shield forms (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !shieldActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 3.0);
                }
            }
            // Shield active (tick 20) — reflects for 80 ticks (4 seconds)
            else if (ticksAlive == 20 && !shieldActive) {
                shieldActive = true;
                shieldStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f);
            }
            // Shield pulsing (20-100 ticks)
            else if (shieldActive && ticksAlive - shieldStartTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 3.0);
                }
                // Rotate shield panels slowly
                float rot = (ticksAlive - shieldStartTick) * 0.02f;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i + rot;
                    mirrorHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2.5, 3, Math.sin(angle) * 2.5));
                }
            }
            // Shield collapses (tick 100)
            else if (shieldActive && ticksAlive - shieldStartTick == 80) {
                shieldActive = false;
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 15, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMirror(plugin); }
    }

    // ================================================================
    // 16. PHASE TEAR — AoE fog zone detonation with upward column
    // ================================================================
    public static class PhaseTear extends BossAttack {
        private final List<BlockDisplayHandle> tearHandles = new ArrayList<>();
        private boolean tearDetonated = false;
        private Location tearLocation;

        public PhaseTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_phase_tear", AttackType.BOSS, 4), "void_shard");
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(520);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Circular fog zone forming at random location
            double ox = (Math.random() - 0.5) * 16;
            double oz = (Math.random() - 0.5) * 16;
            tearLocation = center.clone().add(ox, 0, oz);

            // Fog ground tiles
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx + dz * dz > 12) continue;
                    BlockDisplayHandle fog = displayBuilder.spawnBlock(
                        tearLocation.clone().add(dx, 0.05, dz), Material.PURPLE_STAINED_GLASS);
                    fog.scale(0.8f, 0.1f, 0.8f).glow(80, 0, 160).interpolation(4, 0);
                    tearHandles.add(fog);
                    spawnedEntities.add(fog.entity());
                }
            }
            DisplayBuilder.playSound(tearLocation, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (tearLocation == null) return;

            // Fog settles (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !tearDetonated) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(tearLocation.clone().add(0, 0.5, 0), 10, 4.0);
                }
            }
            // Detonation — column erupts (tick 40)
            else if (ticksAlive == 40 && !tearDetonated) {
                tearDetonated = true;
                // Column of void energy 12 blocks high
                for (int y = 0; y < 12; y++) {
                    BlockDisplayHandle colSeg = displayBuilder.spawnBlock(
                        tearLocation.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                    colSeg.scale(1.5f, 0.8f, 1.5f).glow(160, 0, 255).interpolation(1, 0);
                    tearHandles.add(colSeg);
                    spawnedEntities.add(colSeg.entity());
                }
                DisplayBuilder.cyanDust(tearLocation.clone().add(0, 6, 0), 30, 4.0);
                DisplayBuilder.playSound(tearLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.3f);
                DisplayBuilder.playSound(tearLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
            }
            // Column fades (40-56 ticks = 0.8 seconds)
            else if (tearDetonated && ticksAlive < 56) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(tearLocation.clone().add(0, 6, 0), 8, 3.0);
                }
            }
            // Pit tiles remain (56-256 ticks = 10 seconds)
            else if (tearDetonated && ticksAlive == 56) {
                // Replace fog with dark pit tiles
                for (int dx = -1; dx <= 0; dx++) {
                    for (int dz = -1; dz <= 0; dz++) {
                        BlockDisplayHandle pit = displayBuilder.spawnBlock(
                            tearLocation.clone().add(dx, 0.02, dz), Material.BLACK_CONCRETE);
                        pit.scale(0.9f, 0.04f, 0.9f).glow(40, 0, 80).interpolation(2, 0);
                        spawnedEntities.add(pit.entity());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseTear(plugin); }
    }

    // ================================================================
    // 17. NULL PRESSURE — Downward cone AoE with nausea-like disorientation
    // ================================================================
    public static class NullPressure extends BossAttack {
        private final List<BlockDisplayHandle> pressureHandles = new ArrayList<>();
        private boolean pressureFired = false;

        public NullPressure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_null_pressure", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Shard rises 3 blocks higher than normal
            BlockDisplayHandle elevated = displayBuilder.spawnBlock(
                center.clone().add(0, 6, 0), Material.OBSIDIAN);
            elevated.scale(1.2f, 1.2f, 1.2f).glow(200, 100, 255).interpolation(3, 0);
            pressureHandles.add(elevated);
            spawnedEntities.add(elevated.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Core flash (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !pressureFired) {
                if (ticksAlive == 10) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 15, 2.0);
                }
            }
            // Pressure cone fires downward (tick 20)
            else if (ticksAlive == 20 && !pressureFired) {
                pressureFired = true;
                // Downward cone of void panels
                for (int layer = 0; layer < 5; layer++) {
                    float radius = layer * 2.0f;
                    float y = 6 - layer * 1.2f;
                    int segments = Math.max(4, layer * 4);
                    for (int i = 0; i < segments; i++) {
                        double angle = (Math.PI * 2 / segments) * i;
                        Location coneLoc = center.clone().add(
                            Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                        BlockDisplayHandle cone = displayBuilder.spawnBlock(coneLoc, Material.PURPLE_STAINED_GLASS);
                        cone.scale(0.6f, 0.15f, 0.6f).glow(120, 0, 200).interpolation(1, 0);
                        pressureHandles.add(cone);
                        spawnedEntities.add(cone.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 25, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.9f);
            }
            // Cone dissipates (20-40 ticks)
            else if (pressureFired && ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullPressure(plugin); }
    }

    // ================================================================
    // 18. SHARD BARRIER — 6 large obsidian barrier pillars cage the area
    // ================================================================
    public static class ShardBarrier extends BossAttack {
        private final List<BlockDisplayHandle> barrierHandles = new ArrayList<>();
        private boolean barriersActive = false;
        private int barrierStartTick = 0;

        public ShardBarrier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_shard_barrier", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 large barrier shards hovering at 2 blocks altitude
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location barrierLoc = center.clone().add(
                    Math.cos(angle) * 8, 2, Math.sin(angle) * 8);
                BlockDisplayHandle barrier = displayBuilder.spawnBlock(barrierLoc, Material.OBSIDIAN);
                barrier.scale(1.5f, 3.0f, 0.8f).glow(80, 0, 160).interpolation(3, 0);
                barrierHandles.add(barrier);
                spawnedEntities.add(barrier.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_STEP, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Barriers hover with ambient particles (0-20 ticks)
            if (ticksAlive < 20 && !barriersActive) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle barrier : barrierHandles) {
                        DisplayBuilder.cyanDust(barrier.entity().getLocation(), 3, 1.0);
                    }
                }
            }
            // Barriers sink to ground level (tick 20)
            else if (ticksAlive == 20 && !barriersActive) {
                barriersActive = true;
                barrierStartTick = ticksAlive;
                for (int i = 0; i < barrierHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    barrierHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 8, 0, Math.sin(angle) * 8));
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_STEP, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.6f);
            }
            // Barriers persist (20-180 ticks = 8 seconds)
            else if (barriersActive && ticksAlive - barrierStartTick < 160) {
                if (ticksAlive % 15 == 0) {
                    for (BlockDisplayHandle barrier : barrierHandles) {
                        DisplayBuilder.cyanDust(barrier.entity().getLocation(), 3, 1.5);
                    }
                }
            }
            // Barriers dissolve upward (tick 180)
            else if (barriersActive && ticksAlive - barrierStartTick == 160) {
                for (int i = 0; i < barrierHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    barrierHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 8, 4, Math.sin(angle) * 8));
                    DisplayBuilder.cyanDust(barrierHandles.get(i).entity().getLocation(), 5, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShardBarrier(plugin); }
    }

    // ================================================================
    // 19. CONSUMING DARKNESS — Darkness wave + 4 rapid tendrils
    // ================================================================
    public static class ConsumingDarkness extends BossAttack {
        private final List<BlockDisplayHandle> darknessHandles = new ArrayList<>();
        private boolean darknessFired = false;
        private int tendrilsFired = 0;

        public ConsumingDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_consuming_darkness", AttackType.BOSS, 4), "void_shard");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Darkness pulse emanation — dark overlay expanding
            BlockDisplayHandle darknessCore = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.BLACK_CONCRETE);
            darknessCore.scale(3.0f, 3.0f, 3.0f).glow(20, 0, 40).interpolation(5, 0);
            darknessHandles.add(darknessCore);
            spawnedEntities.add(darknessCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Darkness warning (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !darknessFired) {
                float scale = 3.0f + (ticksAlive / 20.0f) * 5.0f;
                darknessHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 3 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
            }
            // Darkness fires (tick 20) — tendrils in darkness
            else if (ticksAlive == 20 && !darknessFired) {
                darknessFired = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 1.1f);
            }
            // 4 tendrils fire in rapid succession (20-60 ticks, one every 10 ticks)
            else if (darknessFired && tendrilsFired < 4 && ticksAlive >= 20 + tendrilsFired * 10) {
                tendrilsFired++;
                double angle = (Math.PI * 2 / 4) * (tendrilsFired - 1);
                // Tendril line in that direction
                for (int seg = 0; seg < 10; seg++) {
                    Location segLoc = center.clone().add(
                        Math.cos(angle) * seg * 1.2, 2.5, Math.sin(angle) * seg * 1.2);
                    BlockDisplayHandle tendril = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                    tendril.scale(0.15f, 0.15f, 0.6f).glow(100, 0, 200).interpolation(1, 0);
                    darknessHandles.add(tendril);
                    spawnedEntities.add(tendril.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.1f);
            }
            // Darkness persists for 100 ticks (5 seconds) then fades
            else if (darknessFired && ticksAlive == 120) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 15, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConsumingDarkness(plugin); }
    }

    // ================================================================
    // 20. FINAL COLLAPSE — Death attack: implosion + permanent void zones
    // ================================================================
    public static class FinalCollapse extends BossAttack {
        private final List<BlockDisplayHandle> collapseHandles = new ArrayList<>();
        private boolean collapseTriggered = false;

        public FinalCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shard_final_collapse", AttackType.BOSS, 4), "void_shard");
            config.setDamage(16.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Core implosion begins — bright flash
            BlockDisplayHandle coreBurst = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.GLOWSTONE);
            coreBurst.scale(2.0f, 2.0f, 2.0f).glow(255, 150, 255).interpolation(3, 0);
            collapseHandles.add(coreBurst);
            spawnedEntities.add(coreBurst.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Core implosion (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !collapseTriggered) {
                float shrink = 2.0f - (ticksAlive / 10.0f) * 1.5f;
                collapseHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-shrink / 2, 3 - shrink / 2, -shrink / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(shrink, shrink, shrink),
                    new AxisAngle4f(0, 0, 1, 0)));
            }
            // Shards fly outward as projectiles (tick 10)
            else if (ticksAlive == 10 && !collapseTriggered) {
                collapseTriggered = true;
                // 12 shard projectiles in all directions
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location shardLoc = center.clone().add(
                        Math.cos(angle) * 2, 3, Math.sin(angle) * 2);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.OBSIDIAN);
                    shard.scale(0.25f, 0.25f, 0.5f).glow(100, 0, 200).interpolation(1, 0);
                    collapseHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 40, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 2.0f, 0.4f);
            }
            // Shards fly outward (10-30 ticks)
            else if (collapseTriggered && ticksAlive < 30) {
                float dist = (ticksAlive - 10) * 0.8f;
                for (int i = 1; i < collapseHandles.size() && i <= 12; i++) {
                    double angle = (Math.PI * 2 / 12) * (i - 1);
                    collapseHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 3 - dist * 0.15, Math.sin(angle) * (2 + dist)));
                }
            }
            // Permanent void zones stamp (tick 30)
            else if (collapseTriggered && ticksAlive == 30) {
                // 5 permanent void zones around death location
                for (int zone = 0; zone < 5; zone++) {
                    double ox = (Math.random() - 0.5) * 12;
                    double oz = (Math.random() - 0.5) * 12;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockDisplayHandle tile = displayBuilder.spawnBlock(
                                center.clone().add(ox + dx, 0.03, oz + dz), Material.CRYING_OBSIDIAN);
                            tile.scale(0.9f, 0.06f, 0.9f).glow(120, 0, 200).interpolation(2, 0);
                            spawnedEntities.add(tile.entity());
                        }
                    }
                }
                // Vertical beam from death point
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.END_ROD);
                    beam.scale(0.4f, 0.8f, 0.4f).glow(200, 100, 255).interpolation(2, 0);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalCollapse(plugin); }
    }
}
