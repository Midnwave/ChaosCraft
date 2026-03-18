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
 * Phase 1 Boss Attacks — GROUP 7: ZONE CONTROL
 * 10 persistent danger zones that linger on the island, shrinking safe space.
 * Attacks 61-70 from boss1-voidmaw.md.
 * NO status effects — damage only.
 */
public final class ZoneControl {

    private ZoneControl() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidPool(plugin));
        registry.register(new DarkFogBank(plugin));
        registry.register(new VoidLattice(plugin));
        registry.register(new CorruptionSpread(plugin));
        registry.register(new DreadZone(plugin));
        registry.register(new BlackIce(plugin));
        registry.register(new VoidTar(plugin));
        registry.register(new TendrilsInGround(plugin));
        registry.register(new RingOfDarkness(plugin));
        registry.register(new FinalMosaic(plugin));
    }

    // ================================================================
    // 61. VOID POOL — Dark pool deals damage to players standing in it
    // ================================================================
    public static class VoidPool extends BossAttack {
        private final List<BlockDisplayHandle> poolHandles = new ArrayList<>();

        public VoidPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pool", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dark circular pool: concentric rings of flat blocks at ground level
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (x * x + z * z <= 16) {
                        Location loc = center.clone().add(x, 0.02, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(0.95f, 0.06f, 0.95f).glow(128, 0, 255).interpolation(3, 0);
                        poolHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Occasional particle glimmer
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.1, 0), 6, 3.5);
            }
            // Pool surface shimmer animation
            if (ticksAlive % 40 == 0 && !poolHandles.isEmpty()) {
                int idx = (int)(Math.random() * poolHandles.size());
                BlockDisplay bd = (BlockDisplay) poolHandles.get(idx).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.5f, 0, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.95f, 0.04f + (float)(Math.random() * 0.04), 0.95f),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(4);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPool(plugin); }
    }

    // ================================================================
    // 62. DARK FOG BANK — Thick fog rolls over 60% of the island
    // ================================================================
    public static class DarkFogBank extends BossAttack {
        private final List<BlockDisplayHandle> fogHandles = new ArrayList<>();
        private float fogProgress = 0;

        public DarkFogBank(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_fog_bank", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(2.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn a wall of fog blocks rolling from one edge
            for (int x = -15; x <= 5; x += 2) {
                for (int z = -15; z <= 15; z += 2) {
                    for (int y = 0; y <= 4; y += 2) {
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                        h.scale(1.8f, 1.8f, 1.8f).glow(80, 0, 160).interpolation(5, 0);
                        fogHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Roll fog in over 80 ticks
            if (ticksAlive <= 80) {
                fogProgress = ticksAlive / 80.0f;
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle h : fogHandles) {
                        Location loc = h.entity().getLocation();
                        h.entity().teleport(loc.clone().add(0.25, 0, 0));
                    }
                }
            }
            // Settle — periodic density pulses
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(-5, 2, 0), 10, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkFogBank(plugin); }
    }

    // ================================================================
    // 63. VOID LATTICE — Grid of damage lines materializes across island
    // ================================================================
    public static class VoidLattice extends BossAttack {
        private final List<BlockDisplayHandle> latticeHandles = new ArrayList<>();

        public VoidLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_lattice", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(0.8);
            config.setDurationTicks(500);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Grid lines: every 5 blocks, X and Z axes across 30-block span
            for (int offset = -15; offset <= 15; offset += 5) {
                // X-direction lines
                for (int x = -15; x <= 15; x++) {
                    Location loc = center.clone().add(x, 0.08, offset);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.95f, 0.1f, 0.2f).glow(128, 0, 255).interpolation(3, 0);
                    latticeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Z-direction lines
                for (int z = -15; z <= 15; z++) {
                    Location loc = center.clone().add(offset, 0.08, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.2f, 0.1f, 0.95f).glow(128, 0, 255).interpolation(3, 0);
                    latticeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Lattice pulse every second
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 8, 10.0);
            }
            // Subtle brightness flicker on lines
            if (ticksAlive % 40 == 0 && !latticeHandles.isEmpty()) {
                int idx = (int)(Math.random() * latticeHandles.size());
                BlockDisplay bd = (BlockDisplay) latticeHandles.get(idx).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.5f, 0, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.95f, 0.15f, 0.2f),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidLattice(plugin); }
    }

    // ================================================================
    // 64. CORRUPTION SPREAD — Slowly expanding dark corruption zone
    // ================================================================
    public static class CorruptionSpread extends BossAttack {
        private final List<BlockDisplayHandle> corruptedHandles = new ArrayList<>();
        private int spreadRadius = 1;
        private int lastSpreadTick = 0;

        public CorruptionSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_spread", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial corruption block
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.OBSIDIAN);
            h.scale(0.9f, 0.1f, 0.9f).glow(128, 0, 255).interpolation(2, 0);
            corruptedHandles.add(h);
            spawnedEntities.add(h.entity());
            DisplayBuilder.purpleDust(center, 8, 1.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spread every 60 ticks (3 seconds)
            if (ticksAlive - lastSpreadTick >= 60 && spreadRadius <= 8) {
                lastSpreadTick = ticksAlive;
                int r = spreadRadius;
                // Add new ring at current spread radius
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        int distSq = x * x + z * z;
                        if (distSq <= r * r && distSq > (r - 1) * (r - 1)) {
                            Location loc = center.clone().add(x, 0.05, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                            h.scale(0.9f, 0.08f, 0.9f).glow(128, 0, 255).interpolation(3, 0);
                            corruptedHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                spreadRadius++;
                DisplayBuilder.purpleDust(center, 10, spreadRadius);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);
            }

            // Corruption glow pulse
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.purpleDust(center, 5, spreadRadius - 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSpread(plugin); }
    }

    // ================================================================
    // 65. DREAD ZONE — Contracting ring from island edge inward
    // ================================================================
    public static class DreadZone extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private float ringRadius = 20.0f;

        public DreadZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dread_zone", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial large ring at island perimeter
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                Location loc = center.clone().add(Math.cos(a) * ringRadius, 0.1, Math.sin(a) * ringRadius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.5f, 1.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Contract ring inward over 500 ticks
            if (ringRadius > 2 && ticksAlive % 3 == 0) {
                ringRadius -= 0.12f;
                for (int i = 0; i < ringHandles.size(); i++) {
                    double a = (Math.PI * 2 / ringHandles.size()) * i;
                    Location newLoc = center.clone().add(
                        Math.cos(a) * ringRadius, 0.1, Math.sin(a) * ringRadius);
                    ringHandles.get(i).entity().teleport(newLoc);
                }
            }

            // Pulse crimson as ring contracts
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.crimsonDust(center, 6, ringRadius);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DreadZone(plugin); }
    }

    // ================================================================
    // 66. BLACK ICE — Slick void-ice zone causes uncontrolled sliding
    //     (No direct damage — forces players into other attacks;
    //      replaced with minor damage-over-time to comply with no-status rule)
    // ================================================================
    public static class BlackIce extends BossAttack {
        private final List<BlockDisplayHandle> iceHandles = new ArrayList<>();

        public BlackIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_ice", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(7.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Glossy black ice coating on 15-block diameter circle
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    if (x * x + z * z <= 56) {
                        Location loc = center.clone().add(x, 0.03, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        // Very flat, glossy appearance
                        h.scale(0.98f, 0.04f, 0.98f).glow(0, 200, 255).interpolation(4, 0);
                        iceHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.cyanDust(center, 15, 7.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Icy shimmer particles
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center, 5, 6.0);
            }
            // Occasional crack pulse
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackIce(plugin); }
    }

    // ================================================================
    // 67. VOID TAR — Thick black zone reduces movement and deals DoT
    // ================================================================
    public static class VoidTar extends BossAttack {
        private final List<BlockDisplayHandle> tarHandles = new ArrayList<>();

        public VoidTar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tar", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tar pool: dense layered blocks in 10-block diameter
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    if (x * x + z * z <= 25) {
                        // Ground level tar
                        Location loc0 = center.clone().add(x, 0.0, z);
                        BlockDisplayHandle h0 = displayBuilder.spawnBlock(loc0, Material.OBSIDIAN);
                        h0.scale(0.97f, 0.5f, 0.97f).glow(128, 0, 255).interpolation(3, 0);
                        tarHandles.add(h0);
                        spawnedEntities.add(h0.entity());
                        // Surface layer
                        Location loc1 = center.clone().add(x, 0.5, z);
                        BlockDisplayHandle h1 = displayBuilder.spawnBlock(loc1, Material.CRYING_OBSIDIAN);
                        h1.scale(0.97f, 0.15f, 0.97f).glow(80, 0, 160).interpolation(3, 0);
                        tarHandles.add(h1);
                        spawnedEntities.add(h1.entity());
                    }
                }
            }
            DisplayBuilder.purpleDust(center, 12, 5.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Bubbling tar surface
            if (ticksAlive % 12 == 0) {
                double bx = center.getX() + (Math.random() - 0.5) * 8;
                double bz = center.getZ() + (Math.random() - 0.5) * 8;
                DisplayBuilder.purpleDust(
                    new Location(center.getWorld(), bx, center.getY() + 0.6, bz), 3, 0.4);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTar(plugin); }
    }

    // ================================================================
    // 68. TENDRILS IN THE GROUND — Void tendrils emerge and grab players
    // ================================================================
    public static class TendrilsInGround extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private boolean fullyEmerged = false;

        public TendrilsInGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tendrils_in_ground", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(1.2);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Place ~30 tendrils pseudo-randomly across 70% of island
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 13;
                Location loc = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                // Tendrils start below ground
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc.clone().add(0, -1.5, 0), Material.OBSIDIAN);
                h.scale(0.15f, 2.0f, 0.15f).glow(128, 0, 255).interpolation(4, 0);
                tendrilHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendrils emerge over 60 ticks
            if (ticksAlive <= 60 && !fullyEmerged) {
                float rise = (ticksAlive / 60.0f) * 1.5f;
                for (BlockDisplayHandle h : tendrilHandles) {
                    Location base = h.entity().getLocation();
                    // Move upward toward surface
                    if (base.getY() < center.getY() + 0.5) {
                        h.entity().teleport(base.clone().add(0, 0.025, 0));
                    }
                }
                if (ticksAlive == 60) fullyEmerged = true;
            }
            // Sway tendrils
            if (fullyEmerged && ticksAlive % 10 == 0) {
                float sway = (float)(Math.sin(ticksAlive * 0.1) * 0.08);
                for (BlockDisplayHandle h : tendrilHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.075f, -1.0f, -0.075f),
                        new AxisAngle4f(sway, 0, 1, 0),
                        new Vector3f(0.15f, 2.0f, 0.15f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
            // Particle trail
            if (ticksAlive % 20 == 0 && fullyEmerged) {
                for (int i = 0; i < tendrilHandles.size(); i += 5) {
                    DisplayBuilder.purpleDust(tendrilHandles.get(i).entity().getLocation(), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TendrilsInGround(plugin); }
    }

    // ================================================================
    // 69. RING OF DARKNESS — Circular dark ring traps players inside
    // ================================================================
    public static class RingOfDarkness extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();

        public RingOfDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ring_of_darkness", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ring of opaque dark blocks at 5-block radius, 4 blocks tall
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                for (int y = 0; y <= 4; y++) {
                    Location loc = center.clone().add(
                        Math.cos(a) * 5, y, Math.sin(a) * 5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.9f, 0.9f, 0.9f).glow(128, 0, 255).interpolation(3, 0);
                    ringHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.purpleDust(center, 15, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Pulsing darkness within ring
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 8, 3.5);
            }
            // Occasional ring shimmer
            if (ticksAlive % 35 == 0) {
                int startIdx = (int)(Math.random() * (ringHandles.size() - 5));
                for (int i = startIdx; i < startIdx + 5 && i < ringHandles.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) ringHandles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.45f, 0, -0.45f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.95f, 1.0f, 0.95f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RingOfDarkness(plugin); }
    }

    // ================================================================
    // 70. FINAL MOSAIC — Alternating dark/light tiles shift every 4s
    // ================================================================
    public static class FinalMosaic extends BossAttack {
        private final List<BlockDisplayHandle> darkTileHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> lightTileHandles = new ArrayList<>();
        private boolean darksActive = true;

        public FinalMosaic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_mosaic", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(0.9);
            config.setDurationTicks(700);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Checkerboard pattern across 24x24 area
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    boolean isDark = (Math.abs(x) + Math.abs(z)) % 2 == 0;
                    Location loc = center.clone().add(x, 0.04, z);
                    if (isDark) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(0.92f, 0.07f, 0.92f).glow(128, 0, 255).interpolation(2, 0);
                        darkTileHandles.add(h);
                        spawnedEntities.add(h.entity());
                    } else {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.END_STONE);
                        h.scale(0.92f, 0.04f, 0.92f).glow(0, 200, 255).interpolation(2, 0);
                        lightTileHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shift tiles every 80 ticks (4 seconds)
            if (ticksAlive % 80 == 0 && ticksAlive > 0) {
                darksActive = !darksActive;
                // Flash announcement
                DisplayBuilder.purpleDust(center, 20, 12.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, darksActive ? 0.5f : 1.2f);

                // Swap visual emphasis — raise active danger tiles, lower safe tiles
                for (BlockDisplayHandle h : darkTileHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    float height = darksActive ? 0.12f : 0.04f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.46f, 0, -0.46f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.92f, height, 0.92f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                for (BlockDisplayHandle h : lightTileHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    float height = darksActive ? 0.04f : 0.12f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.46f, 0, -0.46f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.92f, height, 0.92f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
            // Steady particle on active dark tiles
            if (ticksAlive % 30 == 0 && darksActive) {
                DisplayBuilder.purpleDust(center, 8, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalMosaic(plugin); }
    }
}
