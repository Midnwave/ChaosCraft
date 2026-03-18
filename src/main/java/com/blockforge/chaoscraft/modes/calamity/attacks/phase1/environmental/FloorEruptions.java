package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

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
import java.util.Random;

/**
 * GROUP 2: FLOOR ERUPTIONS
 * Ten attacks that come up from the ground.
 */
public final class FloorEruptions {

    private FloorEruptions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidGeyser(plugin));
        registry.register(new SpineBurst(plugin));
        registry.register(new BlackBloom(plugin));
        registry.register(new TarVent(plugin));
        registry.register(new RiftCrack(plugin));
        registry.register(new VoidTendrilEruption(plugin));
        registry.register(new FloorImplosion(plugin));
        registry.register(new CrystalShardEruption(plugin));
        registry.register(new PillarRise(plugin));
        registry.register(new TheMawOpens(plugin));
    }

    // =========================================================================
    // ATTACK 11 — Void Geyser
    // Column of void energy erupts from floor, shoots 15 blocks straight up.
    // Players on the tile are launched and damaged.
    // =========================================================================
    public static class VoidGeyser extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean erupted = false;
        private int eruption = 0;

        public VoidGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_geyser", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(2.0);
            config.setDurationTicks(200); // ~10s
            config.setCooldownTicks(400); // 20s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: crack particles + hiss
            DisplayBuilder.purpleDust(center.clone().add(0, 0.2, 0), 8, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase (0-40 ticks)
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.1, 0), 4, 1.0);
                }
                return;
            }

            // Erupt: build column upward over 15 ticks
            if (!erupted && eruption < 15) {
                eruption++;
                Location colLoc = center.clone().add(0, eruption - 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc, Material.PURPLE_CONCRETE);
                h.scale(0.7f, 1.0f, 0.7f).glow(128, 0, 255).interpolation(1, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                if (eruption == 3) {
                    // Trigger damage once base rises
                    triggerImpactDamage(center);
                    // Launch players upward
                    World w = center.getWorld();
                    if (w != null) {
                        for (org.bukkit.entity.Player player : w.getPlayers()) {
                            if (isExempt(player)) continue;
                            if (player.getLocation().distanceSquared(center) <= 9) {
                                player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 1.5, 0)));
                            }
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
                }

                if (eruption == 15) {
                    erupted = true;
                    // Apex burst
                    DisplayBuilder.purpleDust(center.clone().add(0, 15, 0), 20, 3.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 10, 2.0);
                }
                return;
            }

            // Column lingering — pulse particles
            if (ticksAlive % 6 == 0) {
                int yRand = (int)(Math.random() * 15);
                DisplayBuilder.purpleDust(center.clone().add(0, yRand, 0), 4, 0.8);
            }

            // Re-damage players in column
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        double dx = ploc.getX() - center.getX();
                        double dz = ploc.getZ() - center.getZ();
                        double dy = ploc.getY() - center.getY();
                        if (dx * dx + dz * dz <= 1.5 * 1.5 && dy >= 0 && dy <= 15) {
                            player.damage(4.0); // 2 hearts in column
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidGeyser(plugin);
        }
    }

    // =========================================================================
    // ATTACK 12 — Spine Burst
    // 3-5 obsidian spines erupt in a line sequentially, each 3-5 blocks tall.
    // =========================================================================
    public static class SpineBurst extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int spinesSpawned = 0;
        private static final int SPINE_COUNT = 4;
        private double lineAngle;

        public SpineBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spine_burst", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts on eruption hit
            config.setImpactRadius(1.5);
            config.setDurationTicks(400); // ~20s (12s persistence)
            config.setCooldownTicks(600); // 30s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            lineAngle = RNG.nextDouble() * Math.PI;
            // Warning: ground fracture particles tracing the line
            for (int i = -6; i <= 6; i++) {
                double px = Math.cos(lineAngle) * i;
                double pz = Math.sin(lineAngle) * i;
                DisplayBuilder.purpleDust(center.clone().add(px, 0.2, pz), 2, 0.4);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Erupt spines one by one every 4 ticks, starting at tick 40
            if (ticksAlive >= 40 && spinesSpawned < SPINE_COUNT && (ticksAlive - 40) % 4 == 0) {
                double offset = (spinesSpawned - SPINE_COUNT / 2.0) * 3.0;
                double px = Math.cos(lineAngle) * offset;
                double pz = Math.sin(lineAngle) * offset;
                Location spineBase = center.clone().add(px, 0, pz);

                int height = 3 + RNG.nextInt(3);
                for (int y = 0; y < height; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(spineBase.clone().add(0, y, 0), Material.OBSIDIAN);
                    float tapering = 1.0f - y * 0.15f;
                    h.scale(tapering, 1.0f, tapering).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }

                triggerImpactDamage(spineBase);
                DisplayBuilder.purpleDust(spineBase, 10, 1.5);
                DisplayBuilder.playSound(spineBase, Sound.BLOCK_STONE_PLACE, 1.2f, 0.5f);
                spinesSpawned++;
            }

            // Crackle particles along spines while persisting
            if (ticksAlive >= 60 && ticksAlive % 12 == 0) {
                for (int s = 0; s < spinesSpawned; s++) {
                    double offset = (s - SPINE_COUNT / 2.0) * 3.0;
                    double px = Math.cos(lineAngle) * offset;
                    double pz = Math.sin(lineAngle) * offset;
                    DisplayBuilder.purpleDust(center.clone().add(px, 1, pz), 4, 0.6);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SpineBurst(plugin);
        }
    }

    // =========================================================================
    // ATTACK 13 — Black Bloom
    // Flower-like corrupted growth erupts, petals spreading outward over 4s.
    // =========================================================================
    public static class BlackBloom extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int petalStage = 0;
        private static final int MAX_STAGES = 6;

        public BlackBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_bloom", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0); // 2 hearts per petal
            config.setImpactRadius(2.5);
            config.setDurationTicks(600); // ~30s
            config.setCooldownTicks(1100); // 55s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Concentric ring warning
            for (int r = 0; r < 20; r++) {
                double angle = r / 20.0 * 2 * Math.PI;
                DisplayBuilder.purpleDust(center.clone().add(Math.cos(angle) * 3, 0.2, Math.sin(angle) * 3), 2, 0.3);
            }
            // Stem/center core
            for (int y = 0; y < 2; y++) {
                BlockDisplayHandle stem = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BLACKSTONE);
                stem.scale(0.6f, 1.0f, 0.6f).glow(80, 0, 150).interpolation(2, 10);
                handles.add(stem);
                spawnedEntities.add(stem.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Bloom-open phase: one petal stage every ~13 ticks (80 ticks / 6 stages)
            if (ticksAlive >= 40 && petalStage < MAX_STAGES && (ticksAlive - 40) % 13 == 0) {
                double radius = (petalStage + 1) * 1.2;
                int petalsPerStage = 6 + petalStage;
                for (int i = 0; i < petalsPerStage; i++) {
                    double angle = (i / (double) petalsPerStage) * 2 * Math.PI;
                    double px = Math.cos(angle) * radius;
                    double pz = Math.sin(angle) * radius;
                    Location petalLoc = center.clone().add(px, 0.5, pz);

                    Material mat = petalStage % 2 == 0 ? Material.BLACKSTONE : Material.PURPLE_CONCRETE;
                    BlockDisplayHandle petal = displayBuilder.spawnBlock(petalLoc, mat);
                    petal.scale(0.9f, 0.7f, 0.9f).glow(100, 0, 200).interpolation(2, 8);
                    handles.add(petal);
                    spawnedEntities.add(petal.entity());
                }

                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 12, (petalStage + 1) * 1.2);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.5f + petalStage * 0.1f);
                petalStage++;
            }

            // Pulse particles during persistence
            if (ticksAlive > 120 && ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 10, 4.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 5, 3.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new BlackBloom(plugin);
        }
    }

    // =========================================================================
    // ATTACK 14 — Tar Vent
    // Pool of black void-tar bursts upward, coating a 5-block radius.
    // Leaves a slick that applies Slowness to walkers.
    // =========================================================================
    public static class TarVent extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean vented = false;

        public TarVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tar_vent", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0); // 2 hearts on eruption splash
            config.setImpactRadius(5.0);
            config.setDurationTicks(600); // ~30s (20s slick persistence)
            config.setCooldownTicks(900); // 45s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Bubbling warning: black wisps rising
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 6;
                double oz = (Math.random() - 0.5) * 6;
                DisplayBuilder.purpleDust(center.clone().add(ox, 0.3, oz), 3, 0.5);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 60) {
                // Bubbling intensifies
                if (ticksAlive % 6 == 0) {
                    double ox = (Math.random() - 0.5) * 8;
                    double oz = (Math.random() - 0.5) * 8;
                    DisplayBuilder.purpleDust(center.clone().add(ox, 0.5, oz), 4, 0.7);
                }
                return;
            }

            if (!vented) {
                vented = true;
                // Spawn tar slick: dark blocks covering the floor radius
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        if (x * x + z * z <= 25) {
                            Location tarLoc = center.clone().add(x, 0.05, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(tarLoc, Material.BLACK_CONCRETE);
                            h.scale(0.98f, 0.08f, 0.98f).glow(50, 0, 100).interpolation(2, 10);
                            handles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                triggerImpactDamage(center);
                // Upward burst particles
                DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 30, 4.0);
                DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.4f);
                return;
            }

            // Slick pulses
            if (ticksAlive % 20 == 0) {
                for (int r = 0; r < 14; r++) {
                    double angle = r / 14.0 * 2 * Math.PI;
                    DisplayBuilder.purpleDust(center.clone().add(Math.cos(angle) * 4, 0.15, Math.sin(angle) * 4), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TarVent(plugin);
        }
    }

    // =========================================================================
    // ATTACK 15 — Rift Crack
    // Jagged fissure tears open across 15 blocks; players on it take fall damage.
    // =========================================================================
    public static class RiftCrack extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double riftAngle;
        private boolean riftOpen = false;

        public RiftCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_crack", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(1.5);
            config.setDurationTicks(600); // ~30s
            config.setCooldownTicks(1500); // 75s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            riftAngle = RNG.nextDouble() * Math.PI;
            // Screen-shake proxy: rapid particle burst along line
            for (int i = -7; i <= 7; i++) {
                double px = Math.cos(riftAngle) * i;
                double pz = Math.sin(riftAngle) * i;
                DisplayBuilder.purpleDust(center.clone().add(px, 0.1, pz), 3, 0.3);
                DisplayBuilder.crimsonDust(center.clone().add(px, 0.1, pz), 2, 0.3);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40) {
                // Fracture warning — cracks spreading
                if (ticksAlive % 4 == 0) {
                    for (int i = -7; i <= 7; i++) {
                        double px = Math.cos(riftAngle) * i;
                        double pz = Math.sin(riftAngle) * i;
                        DisplayBuilder.crimsonDust(center.clone().add(px, 0.2, pz), 2, 0.4);
                    }
                }
                return;
            }

            if (!riftOpen) {
                riftOpen = true;
                // Render the rift as a void-colored trench of display blocks
                for (int i = -7; i <= 7; i++) {
                    double px = Math.cos(riftAngle) * i;
                    double pz = Math.sin(riftAngle) * i;
                    // Two parallel 1-block-wide sides
                    for (int side : new int[]{-1, 1}) {
                        double ex = Math.sin(riftAngle) * side * 0.5;
                        double ez = Math.cos(riftAngle) * side * 0.5;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(px + ex, -0.5, pz + ez), Material.OBSIDIAN);
                        h.scale(0.4f, 0.6f, 0.4f).glow(30, 0, 80).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    // Void glow in trench
                    BlockDisplayHandle voidBlock = displayBuilder.spawnBlock(
                            center.clone().add(px, -1.0, pz), Material.END_STONE);
                    voidBlock.scale(0.9f, 0.1f, 0.9f).glow(180, 150, 255).interpolation(2, 0);
                    handles.add(voidBlock);
                    spawnedEntities.add(voidBlock.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
            }

            // Pulse from rift
            if (ticksAlive % 10 == 0) {
                int ri = RNG.nextInt(14) - 7;
                double px = Math.cos(riftAngle) * ri;
                double pz = Math.sin(riftAngle) * ri;
                DisplayBuilder.purpleDust(center.clone().add(px, 0.3, pz), 6, 0.8);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RiftCrack(plugin);
        }
    }

    // =========================================================================
    // ATTACK 16 — Void Tendril Eruption
    // A writhing tentacle bursts from the floor, lashes in a 90-degree arc.
    // =========================================================================
    public static class VoidTendrilEruption extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double lashAngle;
        private int lashPhase = 0; // 0=warning, 1=extend, 2=retract

        public VoidTendrilEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendril_eruption", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts per strike
            config.setImpactRadius(4.0);
            config.setDurationTicks(300); // ~15s
            config.setCooldownTicks(700); // 35s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            lashAngle = RNG.nextDouble() * 2 * Math.PI;
            DisplayBuilder.purpleDust(center.clone().add(0, 0.2, 0), 6, 1.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), 5, 0.8);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.3, 0), 2, 0.5);
                }
                return;
            }

            // Extend phase: build tendril outward over 30 ticks
            if (ticksAlive >= 40 && ticksAlive < 70) {
                int segIdx = ticksAlive - 40;
                // Tendril curves slightly each segment
                double curveShift = Math.sin(segIdx * 0.3) * 0.5;
                double segAngle = lashAngle + curveShift * 0.1;
                double dist = segIdx * 0.28;
                double height = 1.0 + Math.sin(segIdx * 0.2) * 1.5;
                double px = Math.cos(segAngle) * dist;
                double pz = Math.sin(segAngle) * dist;
                Location segLoc = center.clone().add(px, height, pz);

                Material segMat = segIdx % 3 == 0 ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, segMat);
                h.scale(0.5f + (float)(segIdx * 0.01), 0.5f, 0.5f + (float)(segIdx * 0.01))
                        .glow(120, 0, 200).interpolation(1, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                if (ticksAlive == 55) {
                    // Strike: tip arrives at reach
                    triggerImpactDamage(segLoc);
                    DisplayBuilder.crimsonDust(segLoc, 12, 2.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.7f);
                }
                return;
            }

            // Retract phase: hide segments over 20 ticks
            if (ticksAlive >= 70 && ticksAlive < 90) {
                int retractIdx = ticksAlive - 70;
                if (retractIdx < handles.size()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(retractIdx).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.25f, -0.25f, -0.25f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                // Second strike at retract tip
                if (ticksAlive == 80) {
                    Location retractTip = center.clone().add(
                            Math.cos(lashAngle) * 5, 2.0, Math.sin(lashAngle) * 5);
                    triggerImpactDamage(retractTip);
                    DisplayBuilder.purpleDust(retractTip, 8, 2.0);
                }
            }

            // Ichor marks: pulsing trail
            if (ticksAlive > 70 && ticksAlive % 15 == 0) {
                double dist = 3 + Math.random() * 4;
                DisplayBuilder.crimsonDust(center.clone().add(
                        Math.cos(lashAngle) * dist, 0.2, Math.sin(lashAngle) * dist), 6, 1.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidTendrilEruption(plugin);
        }
    }

    // =========================================================================
    // ATTACK 17 — Floor Implosion
    // A 5x5 zone silently implodes then snaps back, expelling players upward.
    // =========================================================================
    public static class FloorImplosion extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean imploded = false;
        private boolean snapped = false;

        public FloorImplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_implosion", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts from expulsion
            config.setImpactRadius(3.5);
            config.setDurationTicks(520); // ~26s
            config.setCooldownTicks(1200); // 60s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: faint dark glow on 5x5 zone
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.05, z), Material.BLACK_CONCRETE);
                    h.scale(0.95f, 0.08f, 0.95f).glow(30, 0, 60).interpolation(3, 20);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Deep tone warning (silence proxy — use deep ambient)
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase: zone gets darker
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center, 4, 2.0);
                }
                return;
            }

            // Implode: sink the zone blocks down rapidly (2 ticks)
            if (!imploded && ticksAlive == 40) {
                imploded = true;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.475f, -2.0f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.08f, 0.95f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
                DisplayBuilder.purpleDust(center, 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.2f);
            }

            // Snap back: expel upward
            if (!snapped && ticksAlive == 55) {
                snapped = true;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.475f, 0.05f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.1f, 0.95f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }

                triggerImpactDamage(center);
                // Launch players upward
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(center) <= 12.25) {
                            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 1.8, 0)));
                        }
                    }
                }

                DisplayBuilder.purpleDust(center, 30, 3.0);
                DisplayBuilder.cyanDust(center, 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.4f);
            }

            // Fractured tile effect persisting
            if (ticksAlive > 70 && ticksAlive % 20 == 0) {
                DisplayBuilder.crimsonDust(center, 5, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FloorImplosion(plugin);
        }
    }

    // =========================================================================
    // ATTACK 18 — Crystal Shard Eruption
    // Dozens of amethyst-black void crystals burst across a 10-block scatter zone.
    // =========================================================================
    public static class CrystalShardEruption extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean erupted = false;

        public CrystalShardEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_shard_eruption", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts on eruption
            config.setImpactRadius(5.0);
            config.setDurationTicks(400); // ~20s (10s field + warning)
            config.setCooldownTicks(800); // 40s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tiny crystal tips poking through ground
            for (int i = 0; i < 8; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 10;
                double oz = (RNG.nextDouble() - 0.5) * 10;
                DisplayBuilder.cyanDust(center.clone().add(ox, 0.3, oz), 3, 0.4);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40) {
                // Vibration warning
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double ox = (RNG.nextDouble() - 0.5) * 10;
                        double oz = (RNG.nextDouble() - 0.5) * 10;
                        DisplayBuilder.cyanDust(center.clone().add(ox, 0.2, oz), 2, 0.3);
                    }
                }
                return;
            }

            if (!erupted) {
                erupted = true;
                // Erupt 30+ crystal shards
                for (int i = 0; i < 32; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    int height = 1 + RNG.nextInt(2);
                    for (int y = 0; y < height; y++) {
                        Location shardLoc = center.clone().add(ox, y, oz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                        float scale = 0.3f + RNG.nextFloat() * 0.4f;
                        h.scale(scale, scale * 2, scale).glow(180, 50, 255).interpolation(2, 5);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center, 40, 6.0);
                DisplayBuilder.purpleDust(center, 25, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.0f);
                return;
            }

            // Shard field: cold light pulses
            if (ticksAlive % 8 == 0) {
                int idx = RNG.nextInt(Math.max(1, handles.size()));
                if (idx < handles.size() && handles.get(idx).entity().isValid()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                    float s = 0.3f + RNG.nextFloat() * 0.35f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-s / 2, -s, -s / 2),
                            new AxisAngle4f(ticksAlive * 0.1f, 0, 1, 0),
                            new Vector3f(s, s * 2, s),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
                DisplayBuilder.cyanDust(center, 5, 5.0);
            }
        }

        @Override
        protected void onCleanup() {
            handles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CrystalShardEruption(plugin);
        }
    }

    // =========================================================================
    // ATTACK 19 — Pillar Rise
    // Enormous 10-block obsidian pillar erupts from 3x3 zone, persists 20s.
    // =========================================================================
    public static class PillarRise extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean risen = false;
        private double pillarY;
        private double groundY;

        public PillarRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_rise", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts if standing on it
            config.setImpactRadius(2.5);
            config.setDurationTicks(800); // ~40s (20s warning + rise + 20s persist)
            config.setCooldownTicks(1400); // 70s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            groundY = center.getY();

            // Intense 3x3 glow warning
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle glow = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.05, z), Material.PURPLE_CONCRETE);
                    glow.scale(0.95f, 0.08f, 0.95f).glow(200, 50, 255).interpolation(2, 15);
                    handles.add(glow);
                    spawnedEntities.add(glow.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 60) {
                // Rumbling glow
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 2.0);
                    // Pulse glow blocks brightness
                    for (BlockDisplayHandle h : handles) {
                        if (!h.entity().isValid()) continue;
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        float p = 0.08f + (float)(Math.sin(ticksAlive * 0.4) * 0.04 + 0.04);
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.475f, -p / 2, -0.475f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.95f, p, 0.95f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                }
                return;
            }

            if (!risen) {
                risen = true;
                pillarY = groundY;
                // Remove glow plates and spawn the full pillar
                for (BlockDisplayHandle h : handles) {
                    h.entity().remove();
                }
                handles.clear();

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int y = 0; y < 10; y++) {
                            Location pillarBlock = center.clone().add(x, y, z);
                            Material mat = (y == 9 || x == 0 && z == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                            BlockDisplayHandle h = displayBuilder.spawnBlock(pillarBlock, mat);
                            h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
                            handles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }

                triggerImpactDamage(center);
                // Launch players on the 3x3 base
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        double dx = Math.abs(ploc.getX() - center.getX());
                        double dz = Math.abs(ploc.getZ() - center.getZ());
                        if (dx <= 1.5 && dz <= 1.5) {
                            player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 2.0, 0)));
                        }
                    }
                }

                DisplayBuilder.purpleDust(center.clone().add(0, 10, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.4f);
            }

            // Void arc crackles along pillar surface
            if (ticksAlive % 10 == 0) {
                int yArc = (int)(Math.random() * 10);
                DisplayBuilder.purpleDust(center.clone().add(
                        (Math.random() - 0.5) * 3, yArc, (Math.random() - 0.5) * 3), 5, 0.8);
            }

            // Slow sink near cleanup
            if (ticksAlive > 700) {
                double sinkAmount = (ticksAlive - 700) * 0.05;
                for (BlockDisplayHandle h : handles) {
                    if (!h.entity().isValid()) continue;
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, (float)(-sinkAmount), -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new PillarRise(plugin);
        }
    }

    // =========================================================================
    // ATTACK 20 — The Maw Opens
    // Floor cracks into a circular "mouth" with end-stone teeth; players fall in.
    // =========================================================================
    public static class TheMawOpens extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int mawPhase = 0; // 0=warning rings, 1=opening, 2=open, 3=sealing

        public TheMawOpens(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_maw_opens", AttackType.ENVIRONMENTAL, 1));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts knock
            config.setImpactRadius(4.0);
            config.setDurationTicks(700); // ~35s
            config.setCooldownTicks(1800); // 90s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Concentric ring crack warnings
            for (int ring = 1; ring <= 4; ring++) {
                for (int r = 0; r < 12; r++) {
                    double angle = r / 12.0 * 2 * Math.PI;
                    DisplayBuilder.crimsonDust(center.clone().add(
                            Math.cos(angle) * ring, 0.1, Math.sin(angle) * ring), 2, 0.4);
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning (0-60): expanding rings
            if (ticksAlive < 60) {
                if (ticksAlive % 6 == 0) {
                    double ringR = 1.0 + (ticksAlive / 60.0) * 3.0;
                    for (int r = 0; r < 16; r++) {
                        double angle = r / 16.0 * 2 * Math.PI;
                        DisplayBuilder.purpleDust(center.clone().add(
                                Math.cos(angle) * ringR, 0.2, Math.sin(angle) * ringR), 3, 0.4);
                    }
                }
                return;
            }

            // Opening phase (60-100): spawn teeth + void interior
            if (ticksAlive == 60) {
                mawPhase = 1;
                // Void center (dark pool)
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        if (x * x + z * z <= 9) {
                            BlockDisplayHandle vp = displayBuilder.spawnBlock(
                                    center.clone().add(x, -0.5, z), Material.OBSIDIAN);
                            vp.scale(0.95f, 0.1f, 0.95f).glow(20, 0, 50).interpolation(2, 8);
                            handles.add(vp);
                            spawnedEntities.add(vp.entity());
                        }
                    }
                }
                // Teeth around perimeter
                int numTeeth = 12;
                for (int t = 0; t < numTeeth; t++) {
                    double angle = t / (double) numTeeth * 2 * Math.PI;
                    double tx = Math.cos(angle) * 4.2;
                    double tz = Math.sin(angle) * 4.2;
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle tooth = displayBuilder.spawnBlock(
                                center.clone().add(tx, y, tz), Material.END_STONE);
                        float scale = 0.7f - y * 0.2f;
                        tooth.scale(scale, 1.0f, scale).glow(180, 150, 255).interpolation(2, 5);
                        handles.add(tooth);
                        spawnedEntities.add(tooth.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 30, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.8f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.3f);
            }

            // Open: knock players toward center
            if (ticksAlive >= 60 && ticksAlive <= 100 && ticksAlive % 5 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        Location ploc = player.getLocation();
                        double dist = ploc.distance(center);
                        if (dist > 1 && dist <= 6) {
                            org.bukkit.util.Vector pullDir = center.toVector().subtract(ploc.toVector()).normalize().multiply(0.35);
                            player.setVelocity(player.getVelocity().add(pullDir));
                        }
                        // Players in center: teleport effect (simulate cage)
                        if (dist <= 1.5) {
                            player.damage(6.0); // 3 hearts cage
                            Location safeReturn = center.clone().add(8, 0, 0);
                            player.teleport(safeReturn);
                        }
                    }
                }
            }

            // Maw glow
            if (ticksAlive >= 60 && ticksAlive % 12 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.3, 0), 12, 3.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.3, 0), 6, 2.0);
            }

            // Sealing (tick 300+): shrink teeth
            if (ticksAlive > 300 && ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center, 6, 2.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheMawOpens(plugin);
        }
    }
}
