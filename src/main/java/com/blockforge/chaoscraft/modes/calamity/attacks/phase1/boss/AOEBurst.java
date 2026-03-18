package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Boss Attacks — GROUP 6: AOE BURST
 * 10 large radial explosions that expand outward, punishing players who fail to read the telegraph.
 * Attacks 51-60 from boss1-voidmaw.md.
 * NO status effects — damage only.
 */
public final class AOEBurst {

    private AOEBurst() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidRupture(plugin));
        registry.register(new AbyssGeyser(plugin));
        registry.register(new ShatterPoint(plugin));
        registry.register(new DeepBurst(plugin));
        registry.register(new NovaPulse(plugin));
        registry.register(new VoidBombCluster(plugin));
        registry.register(new SeismicSurge(plugin));
        registry.register(new PressureCollapse(plugin));
        registry.register(new VoidFlare(plugin));
        registry.register(new BlackHolePulse(plugin));
    }

    // ================================================================
    // 51. VOID RUPTURE — Expanding circle implodes and scatters debris
    // ================================================================
    public static class VoidRupture extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private float ringRadius = 0.5f;
        private boolean imploded = false;

        public VoidRupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rupture", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(18.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn 16 ring segments at ground level
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.4f, 0.15f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 1 (0-30 ticks): ring expands to 5-block radius
            if (ticksAlive < 30 && !imploded) {
                ringRadius = 0.5f + (ticksAlive / 30.0f) * 5.0f;
                for (int i = 0; i < ringHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location newLoc = center.clone().add(
                        Math.cos(angle) * ringRadius, 0.05, Math.sin(angle) * ringRadius);
                    ringHandles.get(i).entity().teleport(newLoc);
                    DisplayBuilder.purpleDust(newLoc, 2, 0.3);
                }
            }
            // Phase 2 (tick 30): implosion — ring collapses inward instantly
            else if (ticksAlive == 30 && !imploded) {
                imploded = true;
                DisplayBuilder.purpleDust(center, 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.6f);
                // Scatter 12 debris chunks outward
                for (int d = 0; d < 12; d++) {
                    double a = (Math.PI * 2 / 12) * d;
                    for (int r = 1; r <= 4; r++) {
                        Location debrisLoc = center.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r);
                        DisplayBuilder.purpleDust(debrisLoc, 3, 0.5);
                    }
                }
                // Collapse ring blocks back to center
                for (BlockDisplayHandle h : ringHandles) {
                    h.entity().teleport(center.clone().add(0, 0.1, 0));
                }
            }
            // Phase 3: lingering dark energy pulse
            else if (ticksAlive % 10 == 0 && imploded) {
                DisplayBuilder.purpleDust(center, 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRupture(plugin); }
    }

    // ================================================================
    // 52. ABYSS GEYSER — Narrow pillar erupts then collapses outward
    // ================================================================
    public static class AbyssGeyser extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private boolean erupted = false;
        private boolean collapsed = false;

        public AbyssGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_geyser", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-erupt vent glow (3 blocks at ground)
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI * 2 / 3) * i;
                Location loc = center.clone().add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.2f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                pillarHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: vent glows (0-30 ticks)
            if (ticksAlive < 30 && !erupted) {
                if (ticksAlive % 5 == 0) DisplayBuilder.cyanDust(center, 6, 0.8);
            }
            // Erupt: build pillar (tick 30-90)
            else if (ticksAlive >= 30 && ticksAlive < 90 && !erupted) {
                erupted = true;
                // Remove vent blocks, spawn 15 pillar blocks
                for (BlockDisplayHandle h : pillarHandles) h.entity().remove();
                pillarHandles.clear();
                for (int y = 0; y < 15; y++) {
                    Location loc = center.clone().add(0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    float scale = 1.5f - (y * 0.08f);
                    h.scale(scale, 1.0f, scale).glow(0, 200, 255).interpolation(2, 0);
                    pillarHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.9f, 1.5f);
            }
            // Active pillar: pulse particles (30-90 ticks)
            else if (ticksAlive >= 30 && ticksAlive < 90 && !collapsed) {
                if (ticksAlive % 8 == 0) {
                    for (int y = 0; y < 15; y += 3) {
                        DisplayBuilder.cyanDust(center.clone().add(0, y, 0), 4, 1.0);
                    }
                }
            }
            // Collapse shockwave ring (tick 90)
            else if (ticksAlive == 90 && !collapsed) {
                collapsed = true;
                for (BlockDisplayHandle h : pillarHandles) h.entity().remove();
                pillarHandles.clear();
                // Ring wave outward
                for (int ring = 0; ring < 12; ring++) {
                    double a = (Math.PI * 2 / 12) * ring;
                    for (int r = 1; r <= 6; r++) {
                        DisplayBuilder.cyanDust(center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 3, 0.6);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssGeyser(plugin); }
    }

    // ================================================================
    // 53. SHATTER POINT — Diamond hovers then shatters into 24 shards
    // ================================================================
    public static class ShatterPoint extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private BlockDisplayHandle diamondHandle;
        private boolean shattered = false;

        public ShatterPoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shatter_point", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hover diamond at head height
            Location diamondLoc = center.clone().add(0, 2, 0);
            diamondHandle = displayBuilder.spawnBlock(diamondLoc, Material.OBSIDIAN);
            diamondHandle.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(diamondHandle.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: diamond hovers and pulses (0-40 ticks)
            if (ticksAlive < 40 && !shattered) {
                if (ticksAlive % 5 == 0) {
                    float bob = (float) (Math.sin(ticksAlive * 0.3) * 0.15);
                    Location hover = center.clone().add(0, 2.0 + bob, 0);
                    diamondHandle.entity().teleport(hover);
                    DisplayBuilder.purpleDust(hover, 3, 0.4);
                }
            }
            // Shatter (tick 40): explode 24 shards outward in all directions
            else if (ticksAlive == 40 && !shattered) {
                shattered = true;
                if (diamondHandle != null) diamondHandle.entity().remove();
                Location shatterLoc = center.clone().add(0, 2, 0);
                // 24 shards in sphere
                for (int i = 0; i < 24; i++) {
                    double yaw = (Math.PI * 2 / 24) * i;
                    double pitch = Math.PI * (i % 3) / 3.0 - Math.PI / 3;
                    for (int r = 1; r <= 6; r++) {
                        Location s = shatterLoc.clone().add(
                            Math.cos(yaw) * Math.cos(pitch) * r,
                            Math.sin(pitch) * r,
                            Math.sin(yaw) * Math.cos(pitch) * r);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(s, Material.CRYING_OBSIDIAN);
                        h.scale(0.25f, 0.25f, 0.25f).glow(128, 0, 255).interpolation(1, 0);
                        shardHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.purpleDust(shatterLoc, 50, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.3f, 1.2f);
            }
            // Shard linger particles
            else if (shattered && ticksAlive % 15 == 0) {
                for (BlockDisplayHandle h : shardHandles) {
                    DisplayBuilder.purpleDust(h.entity().getLocation(), 1, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatterPoint(plugin); }
    }

    // ================================================================
    // 54. DEEP BURST — Three sequential ground-level pressure rings
    // ================================================================
    public static class DeepBurst extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private int lastRingSpawned = 0;

        public DeepBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_burst", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring 1 spawns at tick 60 (ground level)
            if (ticksAlive == 60 && lastRingSpawned < 1) {
                lastRingSpawned = 1;
                spawnRing(center, 0.1f);
                DisplayBuilder.purpleDust(center, 20, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            }
            // Ring 2 at tick 80 (waist height)
            else if (ticksAlive == 80 && lastRingSpawned < 2) {
                lastRingSpawned = 2;
                spawnRing(center, 1.5f);
                DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.7f);
            }
            // Ring 3 at tick 100 (head height)
            else if (ticksAlive == 100 && lastRingSpawned < 3) {
                lastRingSpawned = 3;
                spawnRing(center, 3.0f);
                DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 1.0f);
            }
            // Expand rings outward each tick
            if (ticksAlive >= 60 && ticksAlive % 2 == 0) {
                for (BlockDisplayHandle h : ringHandles) {
                    Location loc = h.entity().getLocation();
                    double dx = loc.getX() - center.getX();
                    double dz = loc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        double nx = dx / dist * 0.5;
                        double nz = dz / dist * 0.5;
                        h.entity().teleport(loc.clone().add(nx, 0, nz));
                    }
                }
            }
        }

        private void spawnRing(Location center, float yOff) {
            for (int i = 0; i < 20; i++) {
                double a = (Math.PI * 2 / 20) * i;
                Location loc = center.clone().add(Math.cos(a), yOff, Math.sin(a));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.4f, 0.3f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepBurst(plugin); }
    }

    // ================================================================
    // 55. NOVA PULSE — Island-wide unavoidable void energy burst
    // ================================================================
    public static class NovaPulse extends BossAttack {
        private final List<BlockDisplayHandle> glowHandles = new ArrayList<>();
        private boolean pulsed = false;

        public NovaPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nova_pulse", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Island glow from beneath — scattered low blocks
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 / 12) * i;
                double r = 3 + (i % 3) * 2.0;
                Location loc = center.clone().add(Math.cos(a) * r, -0.4, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.1f, 0.5f).glow(128, 0, 255).interpolation(4, 0);
                glowHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Build-up glow (0-40 ticks)
            if (ticksAlive < 40) {
                float intensity = ticksAlive / 40.0f;
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, (int)(8 * intensity + 2), 5.0 * intensity + 1);
                }
            }
            // Pulse release (tick 40)
            else if (ticksAlive == 40 && !pulsed) {
                pulsed = true;
                // Expanding ring
                for (int ring = 1; ring <= 8; ring++) {
                    for (int i = 0; i < 20; i++) {
                        double a = (Math.PI * 2 / 20) * i;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * ring * 2.5, 1.0, Math.sin(a) * ring * 2.5),
                            5, 1.0);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NovaPulse(plugin); }
    }

    // ================================================================
    // 56. VOID BOMB CLUSTER — Three simultaneous 8-block AoE bombs
    // ================================================================
    public static class VoidBombCluster extends BossAttack {
        private final List<BlockDisplayHandle> bombHandles = new ArrayList<>();
        private boolean detonated = false;
        private final double[][] bombOffsets = {{0, 6, 5}, {-4.3, 6, -2.5}, {4.3, 6, -2.5}};

        public VoidBombCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_bomb_cluster", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three orbs in triangle formation
            for (double[] off : bombOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                bombHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pulse while primed (0-40 ticks)
            if (ticksAlive < 40 && !detonated) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle h : bombHandles) {
                        DisplayBuilder.purpleDust(h.entity().getLocation(), 5, 1.0);
                    }
                }
                // Slow spin
                float angle = ticksAlive * 0.05f;
                for (BlockDisplayHandle h : bombHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.4f, -0.4f, -0.4f),
                        new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
            }
            // Detonate all three at tick 40
            else if (ticksAlive == 40 && !detonated) {
                detonated = true;
                for (int b = 0; b < bombHandles.size() && b < bombOffsets.length; b++) {
                    Location blastLoc = center.clone().add(bombOffsets[b][0], 1.0, bombOffsets[b][2]);
                    DisplayBuilder.purpleDust(blastLoc, 30, 8.0);
                    DisplayBuilder.crimsonDust(blastLoc, 20, 6.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBombCluster(plugin); }
    }

    // ================================================================
    // 57. SEISMIC SURGE — 5 expanding ground rings players must jump over
    // ================================================================
    public static class SeismicSurge extends BossAttack {
        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();
        private int ringsSpawned = 0;
        private float[] ringRadii = {0, 0, 0, 0, 0};

        public SeismicSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_surge", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Origin glow
            BlockDisplayHandle origin = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.CRYING_OBSIDIAN);
            origin.scale(0.5f, 0.1f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(origin.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn one ring per 20 ticks after tick 30 telegraph
            int ringIdx = (ticksAlive - 30) / 20;
            if (ticksAlive >= 30 && ringIdx >= 0 && ringIdx < 5 && ringsSpawned <= ringIdx) {
                ringsSpawned = ringIdx + 1;
                List<BlockDisplayHandle> newRing = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    Location loc = center.clone().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.35f, 0.25f, 0.35f).glow(128, 0, 255).interpolation(2, 0);
                    newRing.add(h);
                    spawnedEntities.add(h.entity());
                }
                rings.add(newRing);
                ringRadii[ringIdx] = 1.5f;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
            }

            // Expand all rings outward
            if (ticksAlive % 2 == 0) {
                for (int r = 0; r < rings.size(); r++) {
                    ringRadii[r] += 0.4f;
                    List<BlockDisplayHandle> ring = rings.get(r);
                    for (int i = 0; i < ring.size(); i++) {
                        double a = (Math.PI * 2 / ring.size()) * i;
                        Location newLoc = center.clone().add(
                            Math.cos(a) * ringRadii[r], 0.1, Math.sin(a) * ringRadii[r]);
                        ring.get(i).entity().teleport(newLoc);
                    }
                    if (ticksAlive % 10 == 0) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(0, 0.2, 0), 4, ringRadii[r]);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeismicSurge(plugin); }
    }

    // ================================================================
    // 58. PRESSURE COLLAPSE — 12-block circle depresses and rebounds
    // ================================================================
    public static class PressureCollapse extends BossAttack {
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> collapseHandles = new ArrayList<>();
        private boolean collapsed = false;
        private boolean rebounded = false;

        public PressureCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pressure_collapse", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rim crack indicators at 12-block diameter edge
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(a) * 6, 0.05, Math.sin(a) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.1f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                crackHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph crack particles (0-40 ticks)
            if (ticksAlive < 40) {
                if (ticksAlive % 7 == 0) {
                    for (BlockDisplayHandle h : crackHandles) {
                        DisplayBuilder.purpleDust(h.entity().getLocation(), 2, 0.3);
                    }
                }
            }
            // Collapse depression (tick 40)
            else if (ticksAlive == 40 && !collapsed) {
                collapsed = true;
                for (int x = -5; x <= 5; x += 2) {
                    for (int z = -5; z <= 5; z += 2) {
                        if (x * x + z * z <= 36) {
                            Location loc = center.clone().add(x, -0.3, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                            h.scale(0.6f, 0.2f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                            collapseHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                DisplayBuilder.purpleDust(center, 30, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.5f);
            }
            // Rebound shockwave (tick 70)
            else if (ticksAlive == 70 && !rebounded) {
                rebounded = true;
                for (BlockDisplayHandle h : collapseHandles) h.entity().remove();
                collapseHandles.clear();
                // Rim shockwave outward 5 blocks
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    for (int r = 6; r <= 11; r++) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r), 3, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PressureCollapse(plugin); }
    }

    // ================================================================
    // 59. VOID FLARE — 6 hexagonal flare points erupt upward
    // ================================================================
    public static class VoidFlare extends BossAttack {
        private final List<BlockDisplayHandle> flareHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> emberHandles = new ArrayList<>();
        private boolean erupted = false;

        public VoidFlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_flare", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(480);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 hexagonal glow points at 8-block radius
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI / 3) * i;
                Location loc = center.clone().add(Math.cos(a) * 8, 0.1, Math.sin(a) * 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.15f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                flareHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph pulse (0-40 ticks)
            if (ticksAlive < 40 && !erupted) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle h : flareHandles) {
                        DisplayBuilder.crimsonDust(h.entity().getLocation(), 4, 0.8);
                    }
                }
            }
            // Eruption (tick 40)
            else if (ticksAlive == 40 && !erupted) {
                erupted = true;
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI / 3) * i;
                    Location base = center.clone().add(Math.cos(a) * 8, 0, Math.sin(a) * 8);
                    // Flare column going up 20 blocks
                    for (int y = 1; y <= 20; y++) {
                        BlockDisplayHandle flare = displayBuilder.spawnBlock(
                            base.clone().add(0, y, 0), Material.OBSIDIAN);
                        flare.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(2, 0);
                        flareHandles.add(flare);
                        spawnedEntities.add(flare.entity());
                    }
                    DisplayBuilder.crimsonDust(base, 15, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
            }
            // Rain embers down (40-140 ticks)
            else if (erupted && ticksAlive < 140 && ticksAlive % 10 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI / 3) * i;
                    Location base = center.clone().add(Math.cos(a) * 8, 0, Math.sin(a) * 8);
                    for (int em = 0; em < 3; em++) {
                        double ex = base.getX() + (Math.random() - 0.5) * 4;
                        double ez = base.getZ() + (Math.random() - 0.5) * 4;
                        double ey = 1 + Math.random() * 5;
                        BlockDisplayHandle ember = displayBuilder.spawnBlock(
                            new Location(center.getWorld(), ex, base.getY() + ey, ez),
                            Material.CRYING_OBSIDIAN);
                        ember.scale(0.2f, 0.2f, 0.2f).glow(200, 0, 50).interpolation(2, 0);
                        emberHandles.add(ember);
                        spawnedEntities.add(ember.entity());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFlare(plugin); }
    }

    // ================================================================
    // 60. BLACK HOLE PULSE — Miniature black hole then single massive ring
    // ================================================================
    public static class BlackHolePulse extends BossAttack {
        private final List<BlockDisplayHandle> holeHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> pulseRingHandles = new ArrayList<>();
        private boolean pulseFired = false;
        private float pulseRadius = 0;

        public BlackHolePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_hole_pulse", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(24.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Black hole sphere 10 blocks above center
            Location holeLoc = center.clone().add(0, 10, 0);
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location orbitLoc = holeLoc.clone().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(orbitLoc, Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                holeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Black hole formation (0-80 ticks): spin and pull particles inward
            if (ticksAlive < 80 && !pulseFired) {
                float angle = ticksAlive * 0.08f;
                Location holeLoc = center.clone().add(0, 10, 0);
                for (int i = 0; i < holeHandles.size(); i++) {
                    double a = angle + (Math.PI * 2 / holeHandles.size()) * i;
                    float scale = 0.5f + ticksAlive * 0.005f;
                    Location orbitLoc = holeLoc.clone().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
                    BlockDisplayHandle h = holeHandles.get(i);
                    h.entity().teleport(orbitLoc);
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(holeLoc, 10, 2.5);
                }
            }
            // Pulse fires at tick 80
            else if (ticksAlive == 80 && !pulseFired) {
                pulseFired = true;
                // Remove black hole
                for (BlockDisplayHandle h : holeHandles) h.entity().remove();
                holeHandles.clear();
                // Spawn massive ring at center
                pulseRadius = 1.0f;
                for (int i = 0; i < 32; i++) {
                    double a = (Math.PI * 2 / 32) * i;
                    Location ringLoc = center.clone().add(Math.cos(a), 1.5, Math.sin(a));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.OBSIDIAN);
                    h.scale(0.8f, 2.5f, 0.8f).glow(128, 0, 255).interpolation(1, 0);
                    pulseRingHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
            }
            // Ring expands very fast (80+ ticks)
            else if (pulseFired && pulseRadius < 30 && ticksAlive % 1 == 0) {
                pulseRadius += 1.5f;
                for (int i = 0; i < pulseRingHandles.size(); i++) {
                    double a = (Math.PI * 2 / pulseRingHandles.size()) * i;
                    Location newLoc = center.clone().add(
                        Math.cos(a) * pulseRadius, 1.5, Math.sin(a) * pulseRadius);
                    pulseRingHandles.get(i).entity().teleport(newLoc);
                }
                if ((int) pulseRadius % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 20, pulseRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackHolePulse(plugin); }
    }
}
