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
 * Phase 4D Boss — THE VOID EMPEROR (Ender Dragon)
 * Phase 2A: Attacks #43-55
 * "The Corruption Spreads" — HP 64%-30%
 * Absorbed boss essences bleed through: Voidmaw (PORTAL), DoG (crystal), Dweller (brimstone).
 * Telegraphs collapse to 1 second. Arena terrain degrades progressively.
 * Phase 2 damage range: 14-24 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase2A {

    private DragonPhase2A() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DimensionalRiftSurge(plugin));
        registry.register(new VoidWalkMimic(plugin));
        registry.register(new RiftLattice(plugin));
        registry.register(new DoGSpikeEruption(plugin));
        registry.register(new PrismaticBarrage(plugin));
        registry.register(new CrystalFortress(plugin));
        registry.register(new HellfireColumn(plugin));
        registry.register(new BrimstoneWyrmTrace(plugin));
        registry.register(new MagmaVeil(plugin));
        registry.register(new BladeHurricane(plugin));
        registry.register(new VoidEyeBeam(plugin));
        registry.register(new GravityFieldInversion(plugin));
        registry.register(new DragonSplitIllusion(plugin));
    }

    // ================================================================
    // #43 — DIMENSIONAL RIFT SURGE — Voidmaw absorption, 3 void rifts
    // ================================================================
    public static class DimensionalRiftSurge extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean riftsOpened = false;
        private int openTick = 0;

        public DimensionalRiftSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_dimensional_rift_surge", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 ground telegraph markers — spiraling PORTAL vortex positions
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * 24;
                double oz = (Math.random() - 0.5) * 24;
                Location riftLoc = center.clone().add(ox, 0.05, oz);
                BlockDisplayHandle marker = displayBuilder.spawnBlock(riftLoc, Material.CRYING_OBSIDIAN);
                marker.scale(2.0f, 0.08f, 2.0f).glow(80, 0, 160).interpolation(3, 0);
                riftHandles.add(marker);
                spawnedEntities.add(marker.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph pulse (0-20 ticks)
            if (ticksAlive < 20 && !riftsOpened) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle marker : riftHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation(), 6, 2.0);
                    }
                }
            }
            // Rifts open — vertical tears erupt (tick 20)
            else if (ticksAlive == 20 && !riftsOpened) {
                riftsOpened = true;
                openTick = ticksAlive;
                for (BlockDisplayHandle marker : riftHandles) {
                    Location baseLoc = marker.entity().getLocation();
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle tear = displayBuilder.spawnBlock(
                            baseLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                        tear.scale(0.3f, 1.0f, 0.3f).glow(120, 0, 200).interpolation(2, 0);
                        spawnedEntities.add(tear.entity());
                    }
                    DisplayBuilder.cyanDust(baseLoc, 15, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
            }
            // Rifts active — ambient particles (20-100 ticks = 4 seconds)
            else if (riftsOpened && ticksAlive - openTick < 80) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle marker : riftHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation().add(0, 2, 0), 4, 1.0);
                    }
                }
            }
            // Rifts collapse (tick 100)
            else if (riftsOpened && ticksAlive - openTick == 80) {
                for (BlockDisplayHandle marker : riftHandles) {
                    Location collapseLoc = marker.entity().getLocation();
                    triggerImpactDamage(collapseLoc);
                    DisplayBuilder.cyanDust(collapseLoc, 20, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.6f, 0.5f);
            }
            // Aftermath — soul sand scorched patches (100-180 ticks)
            else if (riftsOpened && ticksAlive - openTick < 160) {
                if (ticksAlive % 12 == 0) {
                    for (BlockDisplayHandle marker : riftHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation(), 3, 2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalRiftSurge(plugin); }
    }

    // ================================================================
    // #44 — VOID WALK MIMIC — Dragon teleports using Voidmaw's dimensional walk
    // ================================================================
    public static class VoidWalkMimic extends BossAttack {
        private final List<BlockDisplayHandle> walkHandles = new ArrayList<>();
        private boolean teleported = false;

        public VoidWalkMimic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_walk_mimic", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Departure vortex cloud at current position
            BlockDisplayHandle vortex = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.PURPLE_STAINED_GLASS);
            vortex.scale(3.0f, 3.0f, 3.0f).glow(100, 0, 200).interpolation(4, 0);
            walkHandles.add(vortex);
            spawnedEntities.add(vortex.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Departure swirl (0-20 ticks)
            if (ticksAlive < 20 && !teleported) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, 3.0);
                }
            }
            // Teleport — arrival burst (tick 20)
            else if (ticksAlive == 20 && !teleported) {
                teleported = true;
                double arrivalX = (Math.random() - 0.5) * 30;
                double arrivalZ = (Math.random() - 0.5) * 30;
                Location arrivalLoc = center.clone().add(arrivalX, 12, arrivalZ);
                BlockDisplayHandle arrivalBurst = displayBuilder.spawnBlock(
                    arrivalLoc, Material.PURPLE_STAINED_GLASS);
                arrivalBurst.scale(5.0f, 5.0f, 5.0f).glow(140, 0, 220).interpolation(3, 0);
                walkHandles.add(arrivalBurst);
                spawnedEntities.add(arrivalBurst.entity());
                triggerImpactDamage(arrivalLoc);
                DisplayBuilder.cyanDust(arrivalLoc, 25, 5.0);
                DisplayBuilder.playSound(arrivalLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
                DisplayBuilder.playSound(arrivalLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.9f);
            }
            // Residual portal pools (20-80 ticks)
            else if (teleported && ticksAlive < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWalkMimic(plugin); }
    }

    // ================================================================
    // #45 — RIFT LATTICE — Hexagonal void beam grid that rotates
    // ================================================================
    public static class RiftLattice extends BossAttack {
        private final List<BlockDisplayHandle> latticeHandles = new ArrayList<>();
        private boolean latticeActive = false;
        private int activeTick = 0;

        public RiftLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_rift_lattice", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 hexagonal anchor points at radius 15
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location anchorLoc = center.clone().add(Math.cos(angle) * 15, 0.1, Math.sin(angle) * 15);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(anchorLoc, Material.CRYING_OBSIDIAN);
                anchor.scale(0.8f, 4.0f, 0.8f).glow(100, 0, 200).interpolation(3, 0);
                latticeHandles.add(anchor);
                spawnedEntities.add(anchor.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Anchor telegraph (0-20 ticks)
            if (ticksAlive < 20 && !latticeActive) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle anchor : latticeHandles) {
                        DisplayBuilder.cyanDust(anchor.entity().getLocation(), 4, 1.0);
                    }
                }
            }
            // Lattice activates — beams connect anchors (tick 20)
            else if (ticksAlive == 20 && !latticeActive) {
                latticeActive = true;
                activeTick = ticksAlive;
                // Create beam segments between adjacent anchors
                for (int i = 0; i < 6; i++) {
                    int next = (i + 1) % 6;
                    Location from = latticeHandles.get(i).entity().getLocation();
                    Location to = latticeHandles.get(next).entity().getLocation();
                    Location mid = from.clone().add(to).multiply(0.5);
                    mid.setY(1.0);
                    double beamLen = from.distance(to);
                    double beamAngle = Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX());
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(mid, Material.PURPLE_STAINED_GLASS);
                    beam.scale((float) beamLen, 2.0f, 0.3f).glow(120, 0, 200).interpolation(2, 0);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.5f);
            }
            // Lattice rotates (20-140 ticks = 6 seconds)
            else if (latticeActive && ticksAlive - activeTick < 120) {
                float rotAngle = ((ticksAlive - activeTick) / 40.0f) * 30.0f;
                float radians = (float) Math.toRadians(rotAngle);
                for (int i = 0; i < latticeHandles.size(); i++) {
                    double baseAngle = (Math.PI * 2 / 6) * i + radians;
                    latticeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(baseAngle) * 15, 0.1, Math.sin(baseAngle) * 15));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 6, 8.0);
                }
            }
            // Lattice collapse inward (tick 140)
            else if (latticeActive && ticksAlive - activeTick == 120) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 30, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftLattice(plugin); }
    }

    // ================================================================
    // #46 — DoG SPIKE ERUPTION — Crystal spikes from absorbed DoG essence
    // ================================================================
    public static class DoGSpikeEruption extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean spikesErupted = false;
        private int eruptTick = 0;

        public DoGSpikeEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_dog_spike_eruption", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 12 ground target markers — 2 concentric rings of 6
            for (int ring = 0; ring < 2; ring++) {
                double radius = ring == 0 ? 12 : 6;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i + (ring * Math.PI / 6);
                    Location markerLoc = center.clone().add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                    BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.AMETHYST_BLOCK);
                    marker.scale(1.0f, 0.05f, 1.0f).glow(180, 230, 255).interpolation(3, 0);
                    spikeHandles.add(marker);
                    spawnedEntities.add(marker.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph glow (0-20 ticks)
            if (ticksAlive < 20 && !spikesErupted) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle marker : spikeHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation(), 4, 1.0);
                    }
                }
            }
            // Outer ring erupts (tick 20), inner ring (tick 28)
            else if (ticksAlive >= 20 && ticksAlive <= 28 && !spikesErupted) {
                int eruptIdx = ticksAlive == 20 ? 0 : (ticksAlive == 28 ? 6 : -1);
                if (eruptIdx >= 0) {
                    if (eruptIdx == 6) spikesErupted = true;
                    eruptTick = ticksAlive;
                    for (int i = eruptIdx; i < eruptIdx + 6 && i < spikeHandles.size(); i++) {
                        Location spikeLoc = spikeHandles.get(i).entity().getLocation();
                        for (int y = 0; y < 3; y++) {
                            BlockDisplayHandle spike = displayBuilder.spawnBlock(
                                spikeLoc.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                            spike.scale(0.5f, 1.0f, 0.5f).glow(180, 230, 255).interpolation(1, 0);
                            spawnedEntities.add(spike.entity());
                        }
                        triggerImpactDamage(spikeLoc);
                        DisplayBuilder.cyanDust(spikeLoc, 10, 1.5);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 0.8f);
                }
            }
            // Spikes persist then shatter (28-88 ticks = 3 seconds)
            else if (spikesErupted && ticksAlive - eruptTick < 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 4, 6.0);
                }
            }
            // Spikes shatter — shards fly outward (tick 88)
            else if (spikesErupted && ticksAlive - eruptTick == 60) {
                for (BlockDisplayHandle marker : spikeHandles) {
                    Location shatterLoc = marker.entity().getLocation();
                    DisplayBuilder.cyanDust(shatterLoc, 8, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGSpikeEruption(plugin); }
    }

    // ================================================================
    // #47 — PRISMATIC BARRAGE — 18 crystal bolt fan from Dragon's maw
    // ================================================================
    public static class PrismaticBarrage extends BossAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private boolean boltsFired = false;
        private int fireStartTick = 0;

        public PrismaticBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_prismatic_barrage", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Coalescing energy in Dragon's maw
            BlockDisplayHandle chargeGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 8, 0), Material.SEA_LANTERN);
            chargeGlow.scale(1.5f, 1.5f, 1.5f).glow(180, 230, 255).interpolation(3, 0);
            boltHandles.add(chargeGlow);
            spawnedEntities.add(chargeGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge telegraph (0-20 ticks)
            if (ticksAlive < 20 && !boltsFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 8, 5.0);
                }
            }
            // Bolts fire in 3 waves of 6 (tick 20, 26, 32)
            else if (ticksAlive >= 20 && ticksAlive <= 32 && !boltsFired) {
                if (ticksAlive == 20 || ticksAlive == 26 || ticksAlive == 32) {
                    if (ticksAlive == 32) boltsFired = true;
                    fireStartTick = ticksAlive;
                    for (int i = 0; i < 6; i++) {
                        double spreadAngle = Math.toRadians(-60 + i * 20);
                        Location boltLoc = center.clone().add(Math.cos(spreadAngle) * 3, 6, Math.sin(spreadAngle) * 3);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.AMETHYST_BLOCK);
                        bolt.scale(0.2f, 0.2f, 0.6f).glow(180, 230, 255).interpolation(1, 0);
                        boltHandles.add(bolt);
                        spawnedEntities.add(bolt.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.4f, 1.4f);
                }
            }
            // Bolts travel outward (32-62 ticks)
            else if (boltsFired && ticksAlive - fireStartTick < 30) {
                float dist = (ticksAlive - fireStartTick) * 0.7f;
                int boltIdx = 1;
                for (int wave = 0; wave < 3; wave++) {
                    for (int i = 0; i < 6; i++) {
                        if (boltIdx < boltHandles.size()) {
                            double spreadAngle = Math.toRadians(-60 + i * 20);
                            float wDist = dist - wave * 3;
                            if (wDist > 0) {
                                boltHandles.get(boltIdx).entity().teleport(
                                    center.clone().add(Math.cos(spreadAngle) * (3 + wDist), 6 - wDist * 0.2, Math.sin(spreadAngle) * (3 + wDist)));
                            }
                            boltIdx++;
                        }
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 4, 8.0);
                }
            }
            // Ground impacts — damage patches (tick 62)
            else if (boltsFired && ticksAlive - fireStartTick == 30) {
                for (int i = 0; i < 6; i++) {
                    double spreadAngle = Math.toRadians(-60 + i * 20);
                    Location impactLoc = center.clone().add(Math.cos(spreadAngle) * 24, 0.1, Math.sin(spreadAngle) * 24);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.cyanDust(impactLoc, 8, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrismaticBarrage(plugin); }
    }

    // ================================================================
    // #48 — CRYSTAL FORTRESS — DoG cage trap on highest-health player
    // ================================================================
    public static class CrystalFortress extends BossAttack {
        private final List<BlockDisplayHandle> cageHandles = new ArrayList<>();
        private boolean cageActive = false;
        private int cageTick = 0;

        public CrystalFortress(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystal_fortress", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hexagonal ground outline telegraph
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location hexPt = center.clone().add(Math.cos(angle) * 6, 0.05, Math.sin(angle) * 6);
                BlockDisplayHandle hexMarker = displayBuilder.spawnBlock(hexPt, Material.AMETHYST_BLOCK);
                hexMarker.scale(1.0f, 0.05f, 1.0f).glow(160, 220, 255).interpolation(3, 0);
                cageHandles.add(hexMarker);
                spawnedEntities.add(hexMarker.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph pulse (0-20 ticks)
            if (ticksAlive < 20 && !cageActive) {
                if (ticksAlive % 7 == 0) {
                    for (BlockDisplayHandle hex : cageHandles) {
                        DisplayBuilder.cyanDust(hex.entity().getLocation(), 4, 1.0);
                    }
                }
            }
            // Cage walls erupt (tick 20)
            else if (ticksAlive == 20 && !cageActive) {
                cageActive = true;
                cageTick = ticksAlive;
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    for (int y = 0; y < 4; y++) {
                        Location wallLoc = center.clone().add(Math.cos(angle) * 6, y, Math.sin(angle) * 6);
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(wallLoc, Material.AMETHYST_BLOCK);
                        wall.scale(2.0f, 1.0f, 0.5f).glow(160, 220, 255).interpolation(2, 0);
                        spawnedEntities.add(wall.entity());
                    }
                }
                // Ceiling seal
                BlockDisplayHandle ceiling = displayBuilder.spawnBlock(
                    center.clone().add(0, 4, 0), Material.AMETHYST_BLOCK);
                ceiling.scale(12.0f, 0.3f, 12.0f).glow(160, 220, 255).interpolation(2, 0);
                spawnedEntities.add(ceiling.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0f, 0.7f);
            }
            // Cage active — interior damage (20-120 ticks = 5 seconds)
            else if (cageActive && ticksAlive - cageTick < 100) {
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 4.0);
                }
            }
            // Cage shatters (tick 120)
            else if (cageActive && ticksAlive - cageTick == 100) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalFortress(plugin); }
    }

    // ================================================================
    // #49 — HELLFIRE COLUMN — Dweller brimstone memory, 3 fire columns
    // ================================================================
    public static class HellfireColumn extends BossAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private boolean columnsErupted = false;

        public HellfireColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_hellfire_column", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 ground fire circles beneath players
            for (int i = 0; i < 3; i++) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                Location fireLoc = center.clone().add(ox, 0.05, oz);
                BlockDisplayHandle fireCircle = displayBuilder.spawnBlock(fireLoc, Material.MAGMA_BLOCK);
                fireCircle.scale(3.0f, 0.08f, 3.0f).glow(255, 100, 0).interpolation(3, 0);
                columnHandles.add(fireCircle);
                spawnedEntities.add(fireCircle.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire circle telegraph (0-20 ticks)
            if (ticksAlive < 20 && !columnsErupted) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle circle : columnHandles) {
                        DisplayBuilder.cyanDust(circle.entity().getLocation(), 6, 1.5);
                    }
                }
            }
            // Columns erupt (tick 20)
            else if (ticksAlive == 20 && !columnsErupted) {
                columnsErupted = true;
                for (BlockDisplayHandle circle : columnHandles) {
                    Location baseLoc = circle.entity().getLocation();
                    for (int y = 0; y < 8; y++) {
                        BlockDisplayHandle col = displayBuilder.spawnBlock(
                            baseLoc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        col.scale(2.0f, 1.0f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                        spawnedEntities.add(col.entity());
                    }
                    triggerImpactDamage(baseLoc);
                    DisplayBuilder.cyanDust(baseLoc, 15, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
            }
            // Columns persist with sway (20-120 ticks = 5 seconds)
            else if (columnsErupted && ticksAlive < 120) {
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle circle : columnHandles) {
                        DisplayBuilder.cyanDust(circle.entity().getLocation().add(0, 4, 0), 4, 2.0);
                    }
                }
            }
            // Aftermath — magma ground patches (120-520 ticks = 20 seconds)
            else if (columnsErupted && ticksAlive < 520) {
                if (ticksAlive % 20 == 0) {
                    for (BlockDisplayHandle circle : columnHandles) {
                        DisplayBuilder.cyanDust(circle.entity().getLocation(), 3, 2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireColumn(plugin); }
    }

    // ================================================================
    // #50 — BRIMSTONE WYRM TRACE — Pursuing fire trail spiral
    // ================================================================
    public static class BrimstoneWyrmTrace extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean traceActive = false;
        private int traceTick = 0;

        public BrimstoneWyrmTrace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_wyrm_trace", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon dives low — wingtip fire trail wisps
            BlockDisplayHandle wingtrail = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.MAGMA_BLOCK);
            wingtrail.scale(0.5f, 0.2f, 2.0f).glow(255, 120, 0).interpolation(2, 0);
            trailHandles.add(wingtrail);
            spawnedEntities.add(wingtrail.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dive telegraph (0-20 ticks)
            if (ticksAlive < 20 && !traceActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 4, 2.0);
                }
            }
            // Trace begins — spiral fire trail (tick 20)
            else if (ticksAlive == 20 && !traceActive) {
                traceActive = true;
                traceTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.9f);
            }
            // Spiral orbit depositing fire trail (20-140 ticks = 6 seconds, 2 orbits)
            else if (traceActive && ticksAlive - traceTick < 120) {
                int elapsed = ticksAlive - traceTick;
                double orbitAngle = elapsed * 0.05;
                double radius = 12 - (elapsed / 120.0) * 5; // 12 -> 7 block radius
                Location trailPt = center.clone().add(
                    Math.cos(orbitAngle) * radius, 0.2, Math.sin(orbitAngle) * radius);

                if (elapsed % 4 == 0) {
                    BlockDisplayHandle firePatch = displayBuilder.spawnBlock(trailPt, Material.MAGMA_BLOCK);
                    firePatch.scale(2.0f, 0.1f, 2.0f).glow(200, 80, 0).interpolation(2, 0);
                    trailHandles.add(firePatch);
                    spawnedEntities.add(firePatch.entity());
                }
                if (elapsed % 8 == 0) {
                    DisplayBuilder.cyanDust(trailPt, 4, 1.0);
                }
            }
            // Final strafing breath (tick 140)
            else if (traceActive && ticksAlive - traceTick == 120) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneWyrmTrace(plugin); }
    }

    // ================================================================
    // #51 — MAGMA VEIL — Descending fire rain, 20-block circle
    // ================================================================
    public static class MagmaVeil extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private boolean rainActive = false;
        private int rainTick = 0;

        public MagmaVeil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_magma_veil", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sky darkens — descending LAVA/FLAME cloud at Y+30
            BlockDisplayHandle cloud = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.MAGMA_BLOCK);
            cloud.scale(20.0f, 2.0f, 20.0f).glow(255, 100, 0).interpolation(4, 0);
            rainHandles.add(cloud);
            spawnedEntities.add(cloud.entity());
            DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_MOOD, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Cloud telegraph (0-20 ticks)
            if (ticksAlive < 20 && !rainActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 10, 10.0);
                }
            }
            // Ember rain begins (tick 20)
            else if (ticksAlive == 20 && !rainActive) {
                rainActive = true;
                rainTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
            }
            // Ember strikes (20-100 ticks = 4 seconds, 1 per 5 ticks)
            else if (rainActive && ticksAlive - rainTick < 80) {
                if (ticksAlive % 5 == 0) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    Location strikeLoc = center.clone().add(ox, 0.1, oz);
                    BlockDisplayHandle ember = displayBuilder.spawnBlock(strikeLoc, Material.MAGMA_BLOCK);
                    ember.scale(1.0f, 0.3f, 1.0f).glow(255, 60, 0).interpolation(1, 0);
                    rainHandles.add(ember);
                    spawnedEntities.add(ember.entity());
                    triggerImpactDamage(strikeLoc);
                    DisplayBuilder.cyanDust(strikeLoc, 5, 1.0);
                    DisplayBuilder.playSound(strikeLoc, Sound.BLOCK_LAVA_POP, 0.5f, 1.1f);
                }
            }
            // Aftermath — scattered LAVA patches (100-400 ticks = 15 seconds)
            else if (rainActive && ticksAlive - rainTick < 380) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 4, 10.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaVeil(plugin); }
    }

    // ================================================================
    // #52 — BLADE HURRICANE — All orbital blades released in expanding spiral
    // ================================================================
    public static class BladeHurricane extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private boolean bladesReleased = false;
        private int releaseTick = 0;

        public BladeHurricane(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_blade_hurricane", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 orbiting blades accelerate — crimson dust trails
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 4, 8, Math.sin(angle) * 4);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.2f, 1.5f, 0.4f).glow(220, 30, 30).interpolation(2, 0);
                bladeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Blades orbit faster (0-20 ticks)
            if (ticksAlive < 20 && !bladesReleased) {
                float speed = 0.1f + ticksAlive * 0.02f;
                for (int i = 0; i < bladeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * speed;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 4, 8, Math.sin(angle) * 4));
                }
            }
            // Blades release — expanding spiral (tick 20)
            else if (ticksAlive == 20 && !bladesReleased) {
                bladesReleased = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.6f);
            }
            // Blades spiral outward (20-70 ticks)
            else if (bladesReleased && ticksAlive - releaseTick < 50) {
                float dist = (ticksAlive - releaseTick) * 0.4f;
                for (int i = 0; i < bladeHandles.size(); i++) {
                    double baseAngle = (Math.PI * 2 / 8) * i;
                    double spiralAngle = baseAngle + (ticksAlive - releaseTick) * 0.08;
                    float speed = (i < 4) ? dist : dist * 0.6f;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(spiralAngle) * (4 + speed), 8 - speed * 0.1, Math.sin(spiralAngle) * (4 + speed)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 4, 8.0);
                }
            }
            // Blades shatter at arena wall — shrapnel (tick 70)
            else if (bladesReleased && ticksAlive - releaseTick == 50) {
                for (BlockDisplayHandle blade : bladeHandles) {
                    Location shatterLoc = blade.entity().getLocation();
                    triggerImpactDamage(shatterLoc);
                    DisplayBuilder.cyanDust(shatterLoc, 8, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeHurricane(plugin); }
    }

    // ================================================================
    // #53 — VOID EYE BEAM — Sustained sweeping beam from Dragon's eyes
    // ================================================================
    public static class VoidEyeBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int beamTick = 0;

        public VoidEyeBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_eye_beam", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(0.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(2);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye glow telegraph — twin beams forming
            BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                center.clone().add(-1, 15, 0), Material.PURPLE_STAINED_GLASS);
            leftEye.scale(0.3f, 0.3f, 10.0f).glow(200, 100, 255).interpolation(3, 0);
            beamHandles.add(leftEye);
            spawnedEntities.add(leftEye.entity());
            BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                center.clone().add(1, 15, 0), Material.PURPLE_STAINED_GLASS);
            rightEye.scale(0.3f, 0.3f, 10.0f).glow(200, 100, 255).interpolation(3, 0);
            beamHandles.add(rightEye);
            spawnedEntities.add(rightEye.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph flash (0-20 ticks)
            if (ticksAlive < 20 && !beamActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 6, 2.0);
                }
            }
            // Beam activates and sweeps (tick 20)
            else if (ticksAlive == 20 && !beamActive) {
                beamActive = true;
                beamTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.9f);
            }
            // Beam sweeps left-to-right across arena (20-160 ticks = 7 seconds)
            else if (beamActive && ticksAlive - beamTick < 140) {
                float sweepAngle = ((ticksAlive - beamTick) / 140.0f) * (float) Math.PI;
                for (int b = 0; b < beamHandles.size(); b++) {
                    float offset = b == 0 ? -1 : 1;
                    double bx = Math.cos(sweepAngle) * 15;
                    double bz = Math.sin(sweepAngle) * 15;
                    beamHandles.get(b).entity().teleport(
                        center.clone().add(offset + bx * 0.1, 15 - Math.abs(bx) * 0.1, bz));
                }
                if (ticksAlive % 6 == 0) {
                    double sweepX = Math.cos(sweepAngle) * 15;
                    double sweepZ = Math.sin(sweepAngle) * 15;
                    Location sweepLoc = center.clone().add(sweepX, 0.5, sweepZ);
                    triggerImpactDamage(sweepLoc);
                    DisplayBuilder.cyanDust(sweepLoc, 4, 1.0);
                }
            }
            // Beam-lock super-burst if sustained contact (tick 160)
            else if (beamActive && ticksAlive - beamTick == 140) {
                DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 15, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEyeBeam(plugin); }
    }

    // ================================================================
    // #54 — GRAVITY FIELD: ARENA INVERSION — Gravity reversal
    // ================================================================
    public static class GravityFieldInversion extends BossAttack {
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private boolean inverted = false;
        private int invertTick = 0;

        public GravityFieldInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_gravity_field_inversion", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Expanding ring at ground level
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.END_ROD);
                ring.scale(1.0f, 0.2f, 1.0f).glow(255, 255, 255).interpolation(3, 0);
                fieldHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ring expands outward (0-20 ticks)
            if (ticksAlive < 20 && !inverted) {
                float radius = 3 + ticksAlive * 1.0f;
                for (int i = 0; i < fieldHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    fieldHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 10, radius);
                }
            }
            // Gravity inverts (tick 20)
            else if (ticksAlive == 20 && !inverted) {
                inverted = true;
                invertTick = ticksAlive;
                // Ceiling lattice visual
                BlockDisplayHandle ceiling = displayBuilder.spawnBlock(
                    center.clone().add(0, 40, 0), Material.WHITE_STAINED_GLASS);
                ceiling.scale(30.0f, 0.2f, 30.0f).glow(200, 200, 255).interpolation(3, 0);
                fieldHandles.add(ceiling);
                spawnedEntities.add(ceiling.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            }
            // Inversion active (20-100 ticks = 4 seconds)
            else if (inverted && ticksAlive - invertTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 8, 15.0);
                }
            }
            // Gravity snap-back (tick 100)
            else if (inverted && ticksAlive - invertTick == 80) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 25, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.8f);
            }
            // Residual reduced gravity (100-300 ticks = 10 seconds)
            else if (inverted && ticksAlive - invertTick < 280) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 4, 10.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityFieldInversion(plugin); }
    }

    // ================================================================
    // #55 — DRAGON SPLIT ILLUSION — Dragon shatters into 4 (1 real + 3 fake)
    // ================================================================
    public static class DragonSplitIllusion extends BossAttack {
        private final List<BlockDisplayHandle> illusionHandles = new ArrayList<>();
        private boolean splitActive = false;
        private int splitTick = 0;

        public DragonSplitIllusion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_split_illusion", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(760);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Fragmentation burst — afterimage silhouettes
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 / 3) * i;
                Location afterLoc = center.clone().add(Math.cos(angle) * 3, 10, Math.sin(angle) * 3);
                BlockDisplayHandle afterimage = displayBuilder.spawnBlock(afterLoc, Material.PURPLE_STAINED_GLASS);
                afterimage.scale(2.0f, 3.0f, 2.0f).glow(100, 0, 150).interpolation(3, 0);
                illusionHandles.add(afterimage);
                spawnedEntities.add(afterimage.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Split telegraph (0-20 ticks)
            if (ticksAlive < 20 && !splitActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 8, 5.0);
                }
            }
            // Split — 4 dragons approach from cardinal directions (tick 20)
            else if (ticksAlive == 20 && !splitActive) {
                splitActive = true;
                splitTick = ticksAlive;
                // Real dragon marker (denser particle trail)
                BlockDisplayHandle realDragon = displayBuilder.spawnBlock(
                    center.clone().add(20, 10, 0), Material.OBSIDIAN);
                realDragon.scale(3.0f, 4.0f, 3.0f).glow(80, 0, 120).interpolation(2, 0);
                illusionHandles.add(realDragon);
                spawnedEntities.add(realDragon.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.1f);
            }
            // Dragons converge on target (20-60 ticks)
            else if (splitActive && ticksAlive - splitTick < 40) {
                float progress = (ticksAlive - splitTick) / 40.0f;
                for (int i = 0; i < illusionHandles.size(); i++) {
                    double angle = (Math.PI * 2 / illusionHandles.size()) * i;
                    double dist = 20 * (1 - progress);
                    illusionHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 10 - progress * 8, Math.sin(angle) * dist));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 6, 8.0);
                }
            }
            // Real dragon pass — damage (tick 60)
            else if (splitActive && ticksAlive - splitTick == 40) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.7f);
            }
            // Illusion shatter pools (60-160 ticks = 5 seconds)
            else if (splitActive && ticksAlive - splitTick < 140) {
                if (ticksAlive % 12 == 0) {
                    for (BlockDisplayHandle illusion : illusionHandles) {
                        DisplayBuilder.cyanDust(illusion.entity().getLocation(), 3, 2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonSplitIllusion(plugin); }
    }
}
