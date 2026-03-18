package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

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
 * Phase 2 Boss Attacks - GROUP 6: RIFT CHARGE (#51-60)
 * Charges through dimensional rifts, teleport-dash combos.
 * DoG is no longer moving continuously through physical space.
 * NO status effects - damage only.
 */
public final class RiftCharge {

    private RiftCharge() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RiftDash(plugin));
        registry.register(new CrossRiftAmbush(plugin));
        registry.register(new RiftSpiralEmergence(plugin));
        registry.register(new ChainRiftBlitz(plugin));
        registry.register(new PhantomFlanks(plugin));
        registry.register(new RiftClamp(plugin));
        registry.register(new RiftWallCharge(plugin));
        registry.register(new VerticalRiftPlunge(plugin));
        registry.register(new BlinkSweep(plugin));
        registry.register(new RiftAmbushLoop(plugin));
    }

    // ================================================================
    // 51. RIFT DASH - DoG vanishes into a rift, erupts from another
    // 3 sequential dashes, each aimed at player position on re-emergence
    // ================================================================
    public static class RiftDash extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private int dashCount = 0;
        private int lastDashTick = -60;

        public RiftDash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_dash", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Entry rift - vertical tear in space
            for (int y = 0; y < 6; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 1.0f, 0.1f).glow(0, 200, 255).interpolation(2, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: rift pulses for 16 ticks (0.8 sec) before entry
            if (ticksAlive < 16) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 10, 1.5);
                }
            }
            // Dash sequence: 3 dashes, 40 ticks apart
            else if (dashCount < 3 && ticksAlive - lastDashTick >= 40) {
                dashCount++;
                lastDashTick = ticksAlive;

                // Clear previous rift visuals
                for (BlockDisplayHandle h : riftHandles) h.entity().remove();
                riftHandles.clear();

                // Exit rift at random offset
                double angle = Math.random() * Math.PI * 2;
                double dist = 8 + Math.random() * 6;
                Location exitLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Spawn exit rift
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        exitLoc.clone().add(0, y, 0), Material.CYAN_STAINED_GLASS);
                    h.scale(0.4f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(1, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Charge trail from exit rift toward center
                for (int t = 0; t < 8; t++) {
                    double frac = t / 8.0;
                    Location trailLoc = exitLoc.clone().add(
                        (center.getX() - exitLoc.getX()) * frac, 1.5,
                        (center.getZ() - exitLoc.getZ()) * frac);
                    DisplayBuilder.cyanDust(trailLoc, 6, 0.8);
                }

                DisplayBuilder.playSound(exitLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
                DisplayBuilder.playSound(exitLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
            }
            // Lingering rift particles
            else if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : riftHandles) {
                    DisplayBuilder.cyanDust(h.entity().getLocation(), 3, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftDash(plugin); }
    }

    // ================================================================
    // 52. CROSS-RIFT AMBUSH - Two rifts, one real one phantom
    // Only the real rift deals full damage; phantom is a visual decoy
    // ================================================================
    public static class CrossRiftAmbush extends BossAttack {
        private final List<BlockDisplayHandle> riftAHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> riftBHandles = new ArrayList<>();
        private boolean charged = false;
        private boolean realIsA;

        public CrossRiftAmbush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cross_rift_ambush", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            realIsA = Math.random() > 0.5;

            // Rift A on the north side
            Location riftALoc = center.clone().add(0, 0, -12);
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    riftALoc.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                h.scale(0.4f, 1.0f, 0.15f).glow(realIsA ? 0 : 128, 200, 255).interpolation(2, 0);
                riftAHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Rift B on the south side
            Location riftBLoc = center.clone().add(0, 0, 12);
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    riftBLoc.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                h.scale(0.4f, 1.0f, 0.15f).glow(!realIsA ? 0 : 128, 200, 255).interpolation(2, 0);
                riftBHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: both rifts pulse (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !charged) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle h : riftAHandles) {
                        DisplayBuilder.cyanDust(h.entity().getLocation(), 4, 0.6);
                    }
                    for (BlockDisplayHandle h : riftBHandles) {
                        DisplayBuilder.cyanDust(h.entity().getLocation(), 4, 0.6);
                    }
                }
            }
            // Charge: both exit simultaneously (tick 30)
            else if (ticksAlive == 30 && !charged) {
                charged = true;
                // Create charge trails from both rifts toward center
                Location riftALoc = center.clone().add(0, 1.5, -12);
                Location riftBLoc = center.clone().add(0, 1.5, 12);

                for (int t = 0; t < 12; t++) {
                    double frac = t / 12.0;
                    DisplayBuilder.cyanDust(riftALoc.clone().add(0, 0, frac * 12), 8, 0.6);
                    DisplayBuilder.cyanDust(riftBLoc.clone().add(0, 0, -frac * 12), 8, 0.6);
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            }
            // Aftermath particles
            else if (charged && ticksAlive % 10 == 0 && ticksAlive < 60) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 8, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrossRiftAmbush(plugin); }
    }

    // ================================================================
    // 53. RIFT SPIRAL EMERGENCE - DoG spirals out of overhead rift
    // Corkscrew descent with ground-marked landing zone
    // ================================================================
    public static class RiftSpiralEmergence extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean descended = false;

        public RiftSpiralEmergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_spiral_emergence", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Overhead rift - wide horizontal tear at y+15
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 / 12) * i;
                Location loc = center.clone().add(Math.cos(a) * 3, 15, Math.sin(a) * 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.6f, 0.15f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Ground crosshair marking landing zone
            for (int arm = 0; arm < 4; arm++) {
                double a = (Math.PI / 2) * arm;
                for (int r = 1; r <= 4; r++) {
                    Location markLoc = center.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(markLoc, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.05f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: rift particles rain down (0-30 ticks)
            if (ticksAlive < 30 && !descended) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 3;
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(a) * r, 15 - Math.random() * 5, Math.sin(a) * r), 3, 0.5);
                    }
                    // Ground crosshair pulses
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 4.0);
                }
            }
            // Spiral descent (tick 30): spawn spiral body segments
            else if (ticksAlive == 30 && !descended) {
                descended = true;
                for (int seg = 0; seg < 20; seg++) {
                    double spiralAngle = (Math.PI * 2 / 20) * seg * 3;
                    double radius = 3.0 - (seg / 20.0) * 2.5;
                    double y = 15 - (seg / 20.0) * 15;
                    Location segLoc = center.clone().add(Math.cos(spiralAngle) * radius, y, Math.sin(spiralAngle) * radius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                    spiralHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Impact shockwave ring at ground
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i;
                    for (int r = 1; r <= 6; r++) {
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 4, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 1.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
            }
            // Lingering spiral particles
            else if (descended && ticksAlive % 10 == 0 && ticksAlive < 80) {
                for (int i = 0; i < spiralHandles.size(); i += 4) {
                    DisplayBuilder.cyanDust(spiralHandles.get(i).entity().getLocation(), 3, 0.6);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftSpiralEmergence(plugin); }
    }

    // ================================================================
    // 54. CHAIN RIFT BLITZ - 8 rapid micro-rifts across the arena
    // Each transition aimed at a player position, 0.3 sec apart
    // ================================================================
    public static class ChainRiftBlitz extends BossAttack {
        private final List<BlockDisplayHandle> microRiftHandles = new ArrayList<>();
        private int riftsSpawned = 0;

        public ChainRiftBlitz(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_rift_blitz", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 8 micro-rifts, each 6 ticks apart (0.3 sec)
            int riftIdx = ticksAlive / 6;
            if (riftIdx >= 0 && riftIdx < 8 && riftsSpawned <= riftIdx && ticksAlive % 6 == 0) {
                riftsSpawned = riftIdx + 1;

                // Random position around center
                double angle = Math.random() * Math.PI * 2;
                double dist = 4 + Math.random() * 10;
                Location riftLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Spawn micro-rift (small, quick)
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftLoc.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                    h.scale(0.3f, 0.8f, 0.1f).glow(0, 200, 255).interpolation(1, 0);
                    microRiftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Partial body segments emerging
                for (int seg = 0; seg < 4; seg++) {
                    Location segLoc = riftLoc.clone().add(seg * 0.6 - 0.9, 1.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(1, 0);
                    microRiftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.cyanDust(riftLoc.clone().add(0, 1, 0), 12, 1.5);
                DisplayBuilder.playSound(riftLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f + riftIdx * 0.1f);
            }
            // Fade particles on micro-rifts
            else if (ticksAlive > 48 && ticksAlive % 10 == 0) {
                for (int i = 0; i < microRiftHandles.size(); i += 7) {
                    DisplayBuilder.cyanDust(microRiftHandles.get(i).entity().getLocation(), 2, 0.4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainRiftBlitz(plugin); }
    }

    // ================================================================
    // 55. PHANTOM FLANKS - Two copies emerge from separate rifts
    // Both have full visuals; one real, one phantom with reduced damage
    // ================================================================
    public static class PhantomFlanks extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> phantomAHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> phantomBHandles = new ArrayList<>();
        private boolean emerged = false;

        public PhantomFlanks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_flanks", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Two exit rifts on opposite sides
            Location riftA = center.clone().add(14, 0, 0);
            Location riftB = center.clone().add(-14, 0, 0);

            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle hA = displayBuilder.spawnBlock(riftA.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                hA.scale(0.4f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftHandles.add(hA);
                spawnedEntities.add(hA.entity());

                BlockDisplayHandle hB = displayBuilder.spawnBlock(riftB.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                hB.scale(0.4f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftHandles.add(hB);
                spawnedEntities.add(hB.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: rifts pulse (0-30 ticks)
            if (ticksAlive < 30 && !emerged) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(14, 2.5, 0), 8, 1.5);
                    DisplayBuilder.cyanDust(center.clone().add(-14, 2.5, 0), 8, 1.5);
                }
            }
            // Emergence (tick 30): both copies charge toward center
            else if (ticksAlive == 30 && !emerged) {
                emerged = true;
                Location riftA = center.clone().add(14, 0, 0);
                Location riftB = center.clone().add(-14, 0, 0);

                // Phantom A body (5 segments)
                for (int seg = 0; seg < 5; seg++) {
                    Location segLoc = riftA.clone().add(-seg * 2, 1.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.2f, 1.2f, 1.2f).glow(0, 200, 255).interpolation(1, 0);
                    phantomAHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Phantom B body (5 segments)
                for (int seg = 0; seg < 5; seg++) {
                    Location segLoc = riftB.clone().add(seg * 2, 1.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.2f, 1.2f, 1.2f).glow(128, 0, 255).interpolation(1, 0);
                    phantomBHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
            }
            // Sweep both phantoms toward center (30-50 ticks)
            else if (emerged && ticksAlive > 30 && ticksAlive < 50) {
                float progress = (ticksAlive - 30) / 20.0f;
                for (int seg = 0; seg < phantomAHandles.size(); seg++) {
                    double startX = 14 - seg * 2;
                    double curX = startX - startX * progress;
                    phantomAHandles.get(seg).entity().teleport(
                        center.clone().add(curX, 1.5, 0));
                }
                for (int seg = 0; seg < phantomBHandles.size(); seg++) {
                    double startX = -14 + seg * 2;
                    double curX = startX - startX * progress;
                    phantomBHandles.get(seg).entity().teleport(
                        center.clone().add(curX, 1.5, 0));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 10, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomFlanks(plugin); }
    }

    // ================================================================
    // 56. RIFT CLAMP - Two rifts front and back of player
    // DoG threads through the player's exact position
    // ================================================================
    public static class RiftClamp extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> threadHandles = new ArrayList<>();
        private boolean clamped = false;

        public RiftClamp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_clamp", AttackType.BOSS, 2), "dog");
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rear rift (5 blocks behind)
            Location rearLoc = center.clone().add(0, 0, -5);
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(rearLoc.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Front rift (10 blocks ahead)
            Location frontLoc = center.clone().add(0, 0, 10);
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(frontLoc.clone().add(0, y, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: both rifts glow (0-30 ticks)
            if (ticksAlive < 30 && !clamped) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, -5), 6, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 10), 6, 1.0);
                    // Line connecting the two rifts through player
                    for (int z = -5; z <= 10; z += 3) {
                        DisplayBuilder.cyanDust(center.clone().add(0, 0.2, z), 2, 0.3);
                    }
                }
            }
            // Clamp: DoG threads through (tick 30)
            else if (ticksAlive == 30 && !clamped) {
                clamped = true;
                // Thread body segments from rear to front
                for (int seg = 0; seg < 10; seg++) {
                    double z = -5 + (seg / 10.0) * 15;
                    Location segLoc = center.clone().add(0, 1.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.5f).glow(0, 200, 255).interpolation(1, 0);
                    threadHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.4f);
            }
            // Thread moves through (30-50 ticks)
            else if (clamped && ticksAlive > 30 && ticksAlive < 50) {
                float progress = (ticksAlive - 30) / 20.0f;
                for (int seg = 0; seg < threadHandles.size(); seg++) {
                    double baseZ = -5 + (seg / 10.0) * 15;
                    double moveZ = baseZ + progress * 12;
                    threadHandles.get(seg).entity().teleport(
                        center.clone().add(0, 1.5, moveZ));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftClamp(plugin); }
    }

    // ================================================================
    // 57. RIFT WALL CHARGE - 4 rifts in a line across arena
    // DoG enters first, exits last at accelerated speed
    // ================================================================
    public static class RiftWallCharge extends BossAttack {
        private final List<BlockDisplayHandle> wallRiftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private int riftsFormed = 0;
        private boolean chargeFired = false;

        public RiftWallCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_wall_charge", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 4 rifts form one by one, 8 ticks apart (0.4 sec each)
            int riftIdx = ticksAlive / 8;
            if (riftIdx >= 0 && riftIdx < 4 && riftsFormed <= riftIdx && ticksAlive % 8 == 0) {
                riftsFormed = riftIdx + 1;
                double x = -12 + riftIdx * 8;
                Location riftLoc = center.clone().add(x, 0, 0);

                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftLoc.clone().add(0, y, 0), Material.CYAN_STAINED_GLASS);
                    h.scale(0.15f, 1.0f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
                    wallRiftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(riftLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f + riftIdx * 0.2f);
            }
            // Charge fires after all 4 rifts formed (tick 40)
            else if (ticksAlive == 40 && !chargeFired) {
                chargeFired = true;
                // Body segments burst from last rift, sweeping across
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(12, 2, seg - 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.5f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    chargeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.6f);
            }
            // Sweep charge across arena (40-60 ticks)
            else if (chargeFired && ticksAlive > 40 && ticksAlive < 60) {
                float progress = (ticksAlive - 40) / 20.0f;
                for (int seg = 0; seg < chargeHandles.size(); seg++) {
                    double x = 12 - progress * 30;
                    chargeHandles.get(seg).entity().teleport(
                        center.clone().add(x, 2, seg - 4));
                }
                if (ticksAlive % 3 == 0) {
                    float x = 12 - progress * 30;
                    DisplayBuilder.cyanDust(center.clone().add(x, 2, 0), 12, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftWallCharge(plugin); }
    }

    // ================================================================
    // 58. VERTICAL RIFT PLUNGE - Rift beneath player, DoG erupts upward
    // Ground fissure then vertical burst through the island surface
    // ================================================================
    public static class VerticalRiftPlunge extends BossAttack {
        private final List<BlockDisplayHandle> fissureHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> burstHandles = new ArrayList<>();
        private boolean erupted = false;

        public VerticalRiftPlunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vertical_rift_plunge", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground fissure crack
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 1.5, 0.05, Math.sin(a) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.08f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                fissureHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fissure pulses and expands (0-30 ticks)
            if (ticksAlive < 30 && !erupted) {
                if (ticksAlive % 10 == 0) {
                    // Pulse expansion
                    float scale = 0.5f + (ticksAlive / 30.0f) * 0.5f;
                    for (BlockDisplayHandle h : fissureHandles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scale / 2, -0.04f, -scale / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, 0.08f, scale),
                            new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(5);
                    }
                    DisplayBuilder.cyanDust(center, 10, 2.0);
                }
            }
            // Eruption (tick 30): DoG bursts upward
            else if (ticksAlive == 30 && !erupted) {
                erupted = true;
                // Remove fissure
                for (BlockDisplayHandle h : fissureHandles) h.entity().remove();
                fissureHandles.clear();

                // Vertical burst column of body segments
                for (int y = 0; y < 12; y++) {
                    Location burstLoc = center.clone().add(0, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(burstLoc, Material.AMETHYST_BLOCK);
                    float s = 1.2f - y * 0.06f;
                    h.scale(s, 1.0f, s).glow(0, 200, 255).interpolation(1, 0);
                    burstHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Ground fracture shockwave
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i;
                    for (int r = 1; r <= 3; r++) {
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 4, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 1.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
            }
            // Burst moves upward (30-60 ticks)
            else if (erupted && ticksAlive > 30 && ticksAlive < 60) {
                float lift = (ticksAlive - 30) * 0.5f;
                for (int i = 0; i < burstHandles.size(); i++) {
                    burstHandles.get(i).entity().teleport(
                        center.clone().add(0, i + lift, 0));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, lift, 0), 8, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VerticalRiftPlunge(plugin); }
    }

    // ================================================================
    // 59. BLINK SWEEP - 12 rapid micro-teleports across arena
    // Each blink re-aims at nearest player, 0.2 sec flashes
    // ================================================================
    public static class BlinkSweep extends BossAttack {
        private final List<BlockDisplayHandle> blinkHandles = new ArrayList<>();
        private int blinksExecuted = 0;

        public BlinkSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blink_sweep", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(4);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial flicker
            DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 15, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 10 ticks pre-telegraph
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 6, 2.5);
                }
                return;
            }

            // 12 blinks over ~80 ticks (every ~7 ticks)
            int blinkIdx = (ticksAlive - 10) / 7;
            if (blinkIdx >= 0 && blinkIdx < 12 && blinksExecuted <= blinkIdx && (ticksAlive - 10) % 7 == 0) {
                blinksExecuted = blinkIdx + 1;

                // Random blink position 5-8 blocks away
                double angle = Math.random() * Math.PI * 2;
                double dist = 5 + Math.random() * 3;
                Location blinkLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Flash at departure point
                DisplayBuilder.cyanDust(blinkLoc, 8, 1.0);

                // Body flash at blink position
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    blinkLoc.clone().add(0, 1, 0), Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                blinkHandles.add(h);
                spawnedEntities.add(h.entity());

                DisplayBuilder.playSound(blinkLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.5f + blinkIdx * 0.05f);
            }
            // Fade old blink echoes
            if (blinkHandles.size() > 3 && ticksAlive % 7 == 0) {
                BlockDisplayHandle oldest = blinkHandles.get(0);
                oldest.entity().remove();
                blinkHandles.remove(0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlinkSweep(plugin); }
    }

    // ================================================================
    // 60. RIFT AMBUSH LOOP - DoG loops through same rift pair 5 times
    // Fixed axis, repeating passage through same space
    // ================================================================
    public static class RiftAmbushLoop extends BossAttack {
        private final List<BlockDisplayHandle> riftPairHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean loopActive = false;
        private int loopsCompleted = 0;

        public RiftAmbushLoop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_ambush_loop", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rift pair connected by END_ROD particle line
            Location riftEntry = center.clone().add(-8, 0, 0);
            Location riftExit = center.clone().add(8, 0, 0);

            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle hA = displayBuilder.spawnBlock(riftEntry.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                hA.scale(0.4f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftPairHandles.add(hA);
                spawnedEntities.add(hA.entity());

                BlockDisplayHandle hB = displayBuilder.spawnBlock(riftExit.clone().add(0, y, 0), Material.DARK_PRISMARINE);
                hB.scale(0.4f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                riftPairHandles.add(hB);
                spawnedEntities.add(hB.entity());
            }

            // Connecting line between rifts (END_ROD-like blocks)
            for (int x = -6; x <= 6; x += 3) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                    center.clone().add(x, 2.5, 0), Material.END_ROD);
                h.scale(0.1f, 0.1f, 0.1f).glow(240, 240, 255).interpolation(2, 0);
                riftPairHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph: rift pair glows (0-20 ticks)
            if (ticksAlive < 20 && !loopActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-8, 2.5, 0), 6, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(8, 2.5, 0), 6, 1.0);
                }
            }
            // Loop begins (tick 20): 5 passes, 12 ticks each
            else if (ticksAlive >= 20 && loopsCompleted < 5) {
                if (!loopActive) {
                    loopActive = true;
                }
                int loopTick = (ticksAlive - 20) % 12;
                int currentLoop = (ticksAlive - 20) / 12;

                if (currentLoop < 5 && currentLoop >= loopsCompleted) {
                    loopsCompleted = currentLoop + 1;
                    float progress = loopTick / 12.0f;

                    // Clear old body
                    for (BlockDisplayHandle h : bodyHandles) h.entity().remove();
                    bodyHandles.clear();

                    // Body traversing from entry to exit
                    double x = -8 + progress * 16;
                    for (int seg = 0; seg < 4; seg++) {
                        Location segLoc = center.clone().add(x - seg * 1.5, 1.5, 0);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                        h.scale(0.9f, 0.9f, 0.9f).glow(0, 200, 255).interpolation(1, 0);
                        bodyHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    DisplayBuilder.cyanDust(center.clone().add(x, 1.5, 0), 10, 1.5);
                    if (loopTick == 0) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f + currentLoop * 0.15f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftAmbushLoop(plugin); }
    }
}
