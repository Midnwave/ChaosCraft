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
 * Phase 2 Boss Attacks — GROUP 5: AOE / FIELD ATTACKS (#41-50)
 * Crystal scar expansion, void field drops, area denial.
 * Persistent zone-control hazards with crystalline aesthetics.
 * NO status effects — damage only.
 */
public final class AOEField {

    private AOEField() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidFieldDrop(plugin));
        registry.register(new CrystalScarExpansion(plugin));
        registry.register(new SegmentSlamChain(plugin));
        registry.register(new CosmicRain(plugin));
        registry.register(new ElectricFloorField(plugin));
        registry.register(new RiftCraterDeployment(plugin));
        registry.register(new ShockwavePulse(plugin));
        registry.register(new ZoneOfSilence(plugin));
        registry.register(new BoneField(plugin));
        registry.register(new DimensionalPressureWave(plugin));
    }

    // ================================================================
    // 41. VOID FIELD DROP — Descending void zone, persistent damage area
    // ================================================================
    public static class VoidFieldDrop extends BossAttack {
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private boolean fieldLanded = false;
        private int landTick = 0;

        public VoidFieldDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_void_field_drop", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Descending disk of void energy from above
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location diskLoc = center.clone().add(Math.cos(angle) * 8, 20, Math.sin(angle) * 8);
                BlockDisplayHandle disk = displayBuilder.spawnBlock(diskLoc, Material.CRYING_OBSIDIAN);
                disk.scale(1.5f, 0.2f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                fieldHandles.add(disk);
                spawnedEntities.add(disk.entity());
            }
            // Fill disk interior
            for (int x = -6; x <= 6; x += 3) {
                for (int z = -6; z <= 6; z += 3) {
                    if (x * x + z * z <= 36) {
                        Location fillLoc = center.clone().add(x, 20, z);
                        BlockDisplayHandle fill = displayBuilder.spawnBlock(fillLoc, Material.POLISHED_BLACKSTONE);
                        fill.scale(1.8f, 0.15f, 1.8f).glow(128, 0, 255).interpolation(3, 0);
                        fieldHandles.add(fill);
                        spawnedEntities.add(fill.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Disk descends over 40 ticks (2 seconds)
            if (ticksAlive < 40 && !fieldLanded) {
                float dropY = 20 - (ticksAlive / 40.0f) * 19.5f;
                for (BlockDisplayHandle h : fieldHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(new Location(loc.getWorld(), loc.getX(), center.getY() + dropY,
                        loc.getZ(), loc.getYaw(), loc.getPitch()));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, dropY, 0), 10, 8.0);
                }
            }
            // Field lands (tick 40) — persists for 200 ticks (10 seconds)
            else if (ticksAlive == 40 && !fieldLanded) {
                fieldLanded = true;
                landTick = ticksAlive;
                // Set all field elements to ground level
                for (BlockDisplayHandle h : fieldHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(new Location(loc.getWorld(), loc.getX(), center.getY() + 0.15,
                        loc.getZ(), loc.getYaw(), loc.getPitch()));
                }
                DisplayBuilder.cyanDust(center, 30, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.3f);
            }
            // Persistent field pulses with damage zone
            else if (fieldLanded) {
                int fieldTick = ticksAlive - landTick;
                if (fieldTick % 20 == 0 && fieldTick <= 200) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 8, 8.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFieldDrop(plugin); }
    }

    // ================================================================
    // 42. CRYSTAL SCAR EXPANSION — Outward-expanding damage ring
    // ================================================================
    public static class CrystalScarExpansion extends BossAttack {
        private final List<BlockDisplayHandle> scarHandles = new ArrayList<>();
        private boolean expanding = false;
        private float currentRadius = 0;

        public CrystalScarExpansion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_crystal_scar_expansion", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Origin point flash
            BlockDisplayHandle origin = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0),
                Material.SEA_LANTERN);
            origin.scale(1.0f, 0.15f, 1.0f).glow(0, 200, 255).interpolation(4, 0);
            scarHandles.add(origin);
            spawnedEntities.add(origin.entity());
            DisplayBuilder.cyanDust(center, 15, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Origin pulsing (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !expanding) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center, 8, 1.5);
                }
            }
            // Expansion starts (tick 30) — 3 blocks/sec for 100 ticks (5 seconds)
            else if (ticksAlive >= 30) {
                if (!expanding) {
                    expanding = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
                }
                int expandTick = ticksAlive - 30;
                if (expandTick <= 100) {
                    currentRadius = expandTick * 0.15f; // 3 blocks/sec
                    if (currentRadius > 15) currentRadius = 15;

                    // Add expanding edge ring of crystal scar
                    if (expandTick % 4 == 0) {
                        int segments = Math.max(8, (int) (currentRadius * 3));
                        for (int i = 0; i < segments; i++) {
                            double angle = (Math.PI * 2 / segments) * i;
                            Location edgeLoc = center.clone().add(
                                Math.cos(angle) * currentRadius, 0.08, Math.sin(angle) * currentRadius);
                            BlockDisplayHandle edge = displayBuilder.spawnBlock(edgeLoc,
                                Material.AMETHYST_CLUSTER);
                            edge.scale(0.3f, 0.2f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                            scarHandles.add(edge);
                            spawnedEntities.add(edge.entity());
                        }
                        DisplayBuilder.cyanDust(center.clone().add(currentRadius, 0.3, 0), 6, 1.0);
                    }
                    if (expandTick % 10 == 0) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f,
                            0.5f + currentRadius * 0.05f);
                    }
                }
                // Scar persists after expansion stops (100-340 ticks = 12 seconds total)
                else if (expandTick % 30 == 0 && expandTick <= 240) {
                    DisplayBuilder.cyanDust(center, 5, currentRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalScarExpansion(plugin); }
    }

    // ================================================================
    // 43. SEGMENT SLAM CHAIN — 5 rapid sequential ground shockwaves
    // ================================================================
    public static class SegmentSlamChain extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private int slamsTriggered = 0;
        private int lastSlamTick = 0;

        public SegmentSlamChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_segment_slam_chain", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 body segments glowing in sequence
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 / 5) * i;
                double r = 6 + Math.random() * 6;
                Location segLoc = center.clone().add(Math.cos(angle) * r, 10, Math.sin(angle) * r);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                seg.scale(1.2f, 1.0f, 1.2f).glow(0, 200, 255).interpolation(3, 0);
                slamHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Running glow telegraph (0-24 ticks)
            if (ticksAlive < 24) {
                int litSeg = (ticksAlive / 5) % 5;
                if (litSeg < slamHandles.size()) {
                    DisplayBuilder.cyanDust(slamHandles.get(litSeg).entity().getLocation(), 6, 1.5);
                }
            }
            // Sequential slams: one every 8 ticks (0.4 seconds apart)
            else if (slamsTriggered < 5 && ticksAlive - lastSlamTick >= 8) {
                lastSlamTick = ticksAlive;
                Location slamLoc = slamHandles.get(slamsTriggered).entity().getLocation();

                // Slam segment down to ground level
                slamHandles.get(slamsTriggered).entity().teleport(
                    new Location(slamLoc.getWorld(), slamLoc.getX(), center.getY() + 0.5,
                        slamLoc.getZ(), slamLoc.getYaw(), slamLoc.getPitch()));

                // Ground shockwave ring at impact
                for (int ring = 0; ring < 12; ring++) {
                    double angle = (Math.PI * 2 / 12) * ring;
                    Location ringLoc = new Location(slamLoc.getWorld(),
                        slamLoc.getX() + Math.cos(angle) * 4, center.getY() + 0.2,
                        slamLoc.getZ() + Math.sin(angle) * 4);
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(ringLoc, Material.POLISHED_BLACKSTONE);
                    wave.scale(0.6f, 0.15f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                    slamHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }

                DisplayBuilder.cyanDust(new Location(slamLoc.getWorld(), slamLoc.getX(), center.getY() + 0.5,
                    slamLoc.getZ()), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
                slamsTriggered++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SegmentSlamChain(plugin); }
    }

    // ================================================================
    // 44. COSMIC RAIN — 20 projectiles arcing up then raining down
    // ================================================================
    public static class CosmicRain extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private final double[][] rainPositions = new double[20][2];
        private boolean rainFired = false;
        private boolean raining = false;

        public CosmicRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_cosmic_rain", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG head aimed skyward with energy gathering
            BlockDisplayHandle headGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 12, 0), Material.SEA_LANTERN);
            headGlow.scale(2.0f, 2.0f, 2.0f).glow(0, 200, 255).interpolation(4, 0);
            rainHandles.add(headGlow);
            spawnedEntities.add(headGlow.entity());
            // Pre-generate random landing positions
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 15;
                rainPositions[i][0] = Math.cos(angle) * r;
                rainPositions[i][1] = Math.sin(angle) * r;
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 15, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up (0-30 ticks)
            if (ticksAlive < 30 && !rainFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 10, 3.0);
                }
            }
            // Fire barrage upward (tick 30)
            else if (ticksAlive == 30 && !rainFired) {
                rainFired = true;
                // 20 projectiles arc upward
                for (int i = 0; i < 20; i++) {
                    Location projLoc = center.clone().add(0, 12, 0);
                    BlockDisplayHandle proj = displayBuilder.spawnBlock(projLoc, Material.AMETHYST_CLUSTER);
                    proj.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(1, 0);
                    rainHandles.add(proj);
                    spawnedEntities.add(proj.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.0f);
            }
            // Projectiles arc upward (30-60 ticks = 1.5 seconds rising)
            else if (rainFired && !raining && ticksAlive < 60) {
                int riseTick = ticksAlive - 30;
                for (int i = 1; i <= 20 && i < rainHandles.size(); i++) {
                    double spreadAngle = (Math.PI * 2 / 20) * (i - 1);
                    double riseHeight = 12 + riseTick * 0.8;
                    double spread = riseTick * 0.3;
                    rainHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(spreadAngle) * spread, riseHeight, Math.sin(spreadAngle) * spread));
                }
                if (riseTick % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12 + riseTick * 0.8, 0), 8, 5.0);
                }
            }
            // Projectiles rain down to pre-determined positions (tick 60+)
            else if (rainFired && ticksAlive >= 60) {
                if (!raining) {
                    raining = true;
                    // Show landing markers
                    for (int i = 0; i < 20; i++) {
                        Location markerLoc = center.clone().add(rainPositions[i][0], 0.1, rainPositions[i][1]);
                        BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.END_ROD);
                        marker.scale(0.3f, 0.05f, 0.3f).glow(0, 200, 255).interpolation(3, 0);
                        rainHandles.add(marker);
                        spawnedEntities.add(marker.entity());
                    }
                }

                int fallTick = ticksAlive - 60;
                if (fallTick <= 30) {
                    float fallProgress = fallTick / 30.0f;
                    for (int i = 1; i <= 20 && i < rainHandles.size(); i++) {
                        double targetX = rainPositions[i - 1][0];
                        double targetZ = rainPositions[i - 1][1];
                        double currentY = 36 * (1 - fallProgress);
                        double currentX = targetX * fallProgress;
                        double currentZ = targetZ * fallProgress;
                        rainHandles.get(i).entity().teleport(
                            center.clone().add(currentX, currentY, currentZ));
                    }
                    if (fallTick % 5 == 0) {
                        DisplayBuilder.cyanDust(center.clone().add(0, 36 * (1 - fallProgress), 0), 6, 5.0);
                    }
                }
                // Impact effects
                if (fallTick == 30) {
                    for (int i = 0; i < 20; i++) {
                        Location impactLoc = center.clone().add(rainPositions[i][0], 0.3, rainPositions[i][1]);
                        DisplayBuilder.cyanDust(impactLoc, 6, 2.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicRain(plugin); }
    }

    // ================================================================
    // 45. ELECTRIC FLOOR FIELD — Full-island surface burst
    // ================================================================
    public static class ElectricFloorField extends BossAttack {
        private final List<BlockDisplayHandle> floorHandles = new ArrayList<>();
        private boolean peakFired = false;

        public ElectricFloorField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_electric_floor_field", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sparse ground-level crackling starting
            for (int i = 0; i < 16; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 16;
                Location sparkLoc = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                BlockDisplayHandle spark = displayBuilder.spawnBlock(sparkLoc, Material.SEA_LANTERN);
                spark.scale(0.2f, 0.05f, 0.2f).glow(0, 200, 255).interpolation(3, 0);
                floorHandles.add(spark);
                spawnedEntities.add(spark.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Density ramps up (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !peakFired) {
                // Add more spark displays progressively
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = Math.random() * 16;
                        Location sparkLoc = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                        BlockDisplayHandle spark = displayBuilder.spawnBlock(sparkLoc, Material.SEA_LANTERN);
                        float intensity = ticksAlive / 30.0f;
                        spark.scale(0.2f + intensity * 0.3f, 0.05f + intensity * 0.05f, 0.2f + intensity * 0.3f)
                            .glow(0, 200, 255).interpolation(2, 0);
                        floorHandles.add(spark);
                        spawnedEntities.add(spark.entity());
                    }
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 10, 16.0);
                }
            }
            // Peak burst (tick 30) — 0.5-second damage window
            else if (ticksAlive == 30 && !peakFired) {
                peakFired = true;
                // Full-island electric flash — dense ground coverage
                for (int x = -14; x <= 14; x += 3) {
                    for (int z = -14; z <= 14; z += 3) {
                        Location flashLoc = center.clone().add(x, 0.15, z);
                        BlockDisplayHandle flash = displayBuilder.spawnBlock(flashLoc, Material.SEA_LANTERN);
                        flash.scale(1.8f, 0.1f, 1.8f).glow(0, 200, 255).interpolation(1, 0);
                        floorHandles.add(flash);
                        spawnedEntities.add(flash.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 50, 16.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
            }
            // Flash dissipates quickly (30-40 ticks)
            else if (peakFired && ticksAlive == 40) {
                for (int i = floorHandles.size() - 1; i >= 16; i--) {
                    floorHandles.get(i).entity().remove();
                    floorHandles.remove(i);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ElectricFloorField(plugin); }
    }

    // ================================================================
    // 46. RIFT CRATER DEPLOYMENT — 3 persistent hazard craters
    // ================================================================
    public static class RiftCraterDeployment extends BossAttack {
        private final List<BlockDisplayHandle> craterHandles = new ArrayList<>();
        private boolean cratersFormed = false;

        public RiftCraterDeployment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_rift_crater_deployment", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 descending portal columns marking crater locations
            double[][] craterPositions = {{-8, -6}, {8, -4}, {0, 8}};
            for (double[] pos : craterPositions) {
                for (int y = 0; y <= 10; y += 2) {
                    Location colLoc = center.clone().add(pos[0], y, pos[1]);
                    BlockDisplayHandle col = displayBuilder.spawnBlock(colLoc, Material.CRYING_OBSIDIAN);
                    col.scale(0.4f, 1.5f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                    craterHandles.add(col);
                    spawnedEntities.add(col.entity());
                }
            }
            DisplayBuilder.cyanDust(center, 15, 10.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Columns descend (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !cratersFormed) {
                if (ticksAlive % 8 == 0) {
                    double[][] craterPositions = {{-8, -6}, {8, -4}, {0, 8}};
                    for (double[] pos : craterPositions) {
                        DisplayBuilder.cyanDust(center.clone().add(pos[0], 5, pos[1]), 5, 1.5);
                    }
                }
            }
            // Craters form (tick 40)
            else if (ticksAlive == 40 && !cratersFormed) {
                cratersFormed = true;
                // Remove column indicators
                for (BlockDisplayHandle h : craterHandles) h.entity().remove();
                craterHandles.clear();

                // Create crater rings at each position
                double[][] craterPositions = {{-8, -6}, {8, -4}, {0, 8}};
                for (double[] pos : craterPositions) {
                    Location craterCenter = center.clone().add(pos[0], 0, pos[1]);
                    // Crater rim ring
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        Location rimLoc = craterCenter.clone().add(Math.cos(angle) * 5, 0.1, Math.sin(angle) * 5);
                        BlockDisplayHandle rim = displayBuilder.spawnBlock(rimLoc, Material.CRYING_OBSIDIAN);
                        rim.scale(1.0f, 0.2f, 1.0f).glow(128, 0, 255).interpolation(2, 0);
                        craterHandles.add(rim);
                        spawnedEntities.add(rim.entity());
                    }
                    // Crater interior darkness
                    for (int x = -3; x <= 3; x += 2) {
                        for (int z = -3; z <= 3; z += 2) {
                            if (x * x + z * z <= 12) {
                                Location interiorLoc = craterCenter.clone().add(x, -0.5, z);
                                BlockDisplayHandle interior = displayBuilder.spawnBlock(interiorLoc,
                                    Material.POLISHED_BLACKSTONE);
                                interior.scale(1.2f, 0.3f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
                                craterHandles.add(interior);
                                spawnedEntities.add(interior.entity());
                            }
                        }
                    }
                    DisplayBuilder.cyanDust(craterCenter, 15, 5.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }
            // Craters pulse (persists for 300 ticks = 15 seconds)
            else if (cratersFormed && ticksAlive % 30 == 0 && ticksAlive < 340) {
                double[][] craterPositions = {{-8, -6}, {8, -4}, {0, 8}};
                for (double[] pos : craterPositions) {
                    DisplayBuilder.cyanDust(center.clone().add(pos[0], 0.3, pos[1]), 5, 5.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftCraterDeployment(plugin); }
    }

    // ================================================================
    // 47. SHOCKWAVE PULSE — Full-body radial expanding shockwave
    // ================================================================
    public static class ShockwavePulse extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean pulseFired = false;
        private float waveRadius = 0;

        public ShockwavePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_shockwave_pulse", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Full-body flare — entire worm lights up
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location segLoc = center.clone().add(Math.cos(angle) * 3, 5, Math.sin(angle) * 3);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.SEA_LANTERN);
                seg.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                waveHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 25, 4.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Body-wide flare (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !pulseFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 15, 4.0);
                }
            }
            // Pulse fires (tick 20)
            else if (ticksAlive == 20 && !pulseFired) {
                pulseFired = true;
                waveRadius = 1;
                // Create shockwave ring
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 / 24) * i;
                    Location waveLoc = center.clone().add(Math.cos(angle), 1, Math.sin(angle));
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(waveLoc, Material.AMETHYST_BLOCK);
                    wave.scale(0.8f, 1.5f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                    waveHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);
            }
            // Wave expands at 20 blocks/sec (20+ ticks)
            else if (pulseFired && waveRadius < 28) {
                waveRadius += 1.0f; // 20 blocks/sec
                // Update wave ring positions
                for (int i = 12; i < waveHandles.size(); i++) {
                    int idx = i - 12;
                    double angle = (Math.PI * 2 / 24) * idx;
                    waveHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * waveRadius, 1, Math.sin(angle) * waveRadius));
                }
                if ((int) waveRadius % 4 == 0) {
                    DisplayBuilder.cyanDust(center, 8, waveRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShockwavePulse(plugin); }
    }

    // ================================================================
    // 48. ZONE OF SILENCE — Dormant zone that detonates after 3 seconds
    // ================================================================
    public static class ZoneOfSilence extends BossAttack {
        private final List<BlockDisplayHandle> zoneHandles = new ArrayList<>();
        private boolean zoneActive = false;
        private boolean detonated = false;
        private int zoneStartTick = 0;

        public ZoneOfSilence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_zone_of_silence", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Zone boundary forming — pale blue ring
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 / 24) * i;
                Location ringLoc = center.clone().add(Math.cos(angle) * 12, 0.2, Math.sin(angle) * 12);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.END_ROD);
                ring.scale(0.8f, 0.3f, 0.8f).glow(0, 200, 255).interpolation(4, 0);
                zoneHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Zone forming (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !zoneActive) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center, 6, 12.0);
                }
            }
            // Zone activates — dormant silence period (tick 30)
            else if (ticksAlive == 30 && !zoneActive) {
                zoneActive = true;
                zoneStartTick = ticksAlive;
                // Interior calms — visual distortion markers
                for (int x = -8; x <= 8; x += 4) {
                    for (int z = -8; z <= 8; z += 4) {
                        if (x * x + z * z <= 100) {
                            Location interiorLoc = center.clone().add(x, 0.5, z);
                            BlockDisplayHandle interior = displayBuilder.spawnBlock(interiorLoc,
                                Material.AMETHYST_BLOCK);
                            interior.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(5, 0);
                            zoneHandles.add(interior);
                            spawnedEntities.add(interior.entity());
                        }
                    }
                }
                // No sound — this is the silence
            }
            // Dormancy period — 60 ticks (3 seconds) of silence
            else if (zoneActive && !detonated) {
                int dormantTick = ticksAlive - zoneStartTick;
                // Very subtle interior particle slow-drift
                if (dormantTick % 15 == 0 && dormantTick <= 60) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 3, 6.0);
                }
                // DETONATION at 3 seconds
                if (dormantTick >= 60 && !detonated) {
                    detonated = true;
                    // Massive outward blast of suppressed energy
                    for (int ring = 0; ring < 20; ring++) {
                        double angle = (Math.PI * 2 / 20) * ring;
                        for (int r = 2; r <= 12; r += 2) {
                            Location blastLoc = center.clone().add(
                                Math.cos(angle) * r, 0.5 + r * 0.1, Math.sin(angle) * r);
                            BlockDisplayHandle blast = displayBuilder.spawnBlock(blastLoc, Material.SEA_LANTERN);
                            blast.scale(0.8f, 0.5f, 0.8f).glow(0, 200, 255).interpolation(1, 0);
                            zoneHandles.add(blast);
                            spawnedEntities.add(blast.entity());
                        }
                    }
                    DisplayBuilder.cyanDust(center, 60, 12.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ZoneOfSilence(plugin); }
    }

    // ================================================================
    // 49. BONE FIELD — Crystalline spike trail along DoG's path
    // ================================================================
    public static class BoneField extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean shedding = false;
        private int shedStartTick = 0;
        private int spikesPlaced = 0;
        private float shedAngle = 0;

        public BoneField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_bone_field", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rear segments brightening with running light
            for (int i = 0; i < 6; i++) {
                Location segLoc = center.clone().add(6 + i * 2, 6, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                seg.scale(0.8f, 0.6f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                spikeHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Running light telegraph (0-20 ticks)
            if (ticksAlive < 20 && !shedding) {
                int litSeg = (ticksAlive / 4) % 6;
                if (litSeg < spikeHandles.size()) {
                    DisplayBuilder.cyanDust(spikeHandles.get(litSeg).entity().getLocation(), 4, 1.0);
                }
            }
            // Start shedding (tick 20) — continuous for 120 ticks (6 seconds)
            else if (ticksAlive >= 20) {
                if (!shedding) {
                    shedding = true;
                    shedStartTick = ticksAlive;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.0f);
                }
                int shedTick = ticksAlive - shedStartTick;
                shedAngle += 0.06f; // DoG's movement path

                // Drop a spike every 6 ticks (~3 blocks apart along path)
                if (shedTick % 6 == 0 && spikesPlaced < 20 && shedTick <= 120) {
                    double pathR = 10;
                    double spikeX = Math.cos(shedAngle) * pathR;
                    double spikeZ = Math.sin(shedAngle) * pathR;
                    Location spikeLoc = center.clone().add(spikeX, 0, spikeZ);

                    // Crystal bone spike — 3 blocks tall
                    for (int y = 0; y <= 3; y++) {
                        BlockDisplayHandle spike = displayBuilder.spawnBlock(
                            spikeLoc.clone().add(0, y, 0), Material.AMETHYST_CLUSTER);
                        float scale = 0.5f - y * 0.1f;
                        spike.scale(scale, 0.8f, scale).glow(128, 0, 255).interpolation(2, 0);
                        spikeHandles.add(spike);
                        spawnedEntities.add(spike.entity());
                    }
                    spikesPlaced++;
                    DisplayBuilder.cyanDust(spikeLoc, 6, 1.0);
                    if (spikesPlaced % 3 == 0) {
                        DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BoneField(plugin); }
    }

    // ================================================================
    // 50. DIMENSIONAL PRESSURE WAVE — Unavoidable expanding distortion ring
    // ================================================================
    public static class DimensionalPressureWave extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean waveFired = false;
        private float waveRadius = 0;

        public DimensionalPressureWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_dimensional_pressure_wave", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Deep violet aura — distinct purple telegraph
            BlockDisplayHandle headAura = displayBuilder.spawnBlock(
                center.clone().add(0, 8, 0), Material.CRYING_OBSIDIAN);
            headAura.scale(2.5f, 2.5f, 2.5f).glow(128, 0, 255).interpolation(4, 0);
            waveHandles.add(headAura);
            spawnedEntities.add(headAura.entity());
            // Distinct purple particles — different from all other attacks
            DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 20, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Purple aura building (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !waveFired) {
                float scale = 2.5f + (ticksAlive / 40.0f) * 2.0f;
                BlockDisplay bd = (BlockDisplay) waveHandles.get(0).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 10, 3.0);
                }
            }
            // Wave fires (tick 40) — expands at 12 blocks/sec, passes through everything
            else if (ticksAlive == 40 && !waveFired) {
                waveFired = true;
                waveRadius = 1;
                // Create pressure wave ring — thick, unavoidable, passes through terrain
                for (int layer = 0; layer < 3; layer++) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (Math.PI * 2 / 20) * i;
                        Location waveLoc = center.clone().add(Math.cos(angle), 2 + layer * 2, Math.sin(angle));
                        BlockDisplayHandle wave = displayBuilder.spawnBlock(waveLoc, Material.CRYING_OBSIDIAN);
                        wave.scale(0.8f, 1.5f, 0.8f).glow(128, 0, 255).interpolation(1, 0);
                        waveHandles.add(wave);
                        spawnedEntities.add(wave.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.3f);
            }
            // Wave expands at 12 blocks/sec — covers full arena in ~2.5 seconds
            else if (waveFired && waveRadius < 30) {
                waveRadius += 0.6f; // 12 blocks/sec
                // Update all wave ring segments
                for (int layer = 0; layer < 3; layer++) {
                    for (int i = 0; i < 20; i++) {
                        int idx = 1 + layer * 20 + i;
                        if (idx < waveHandles.size()) {
                            double angle = (Math.PI * 2 / 20) * i;
                            waveHandles.get(idx).entity().teleport(
                                center.clone().add(
                                    Math.cos(angle) * waveRadius, 2 + layer * 2, Math.sin(angle) * waveRadius));
                        }
                    }
                }
                if ((int) waveRadius % 3 == 0) {
                    DisplayBuilder.cyanDust(center, 10, waveRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalPressureWave(plugin); }
    }
}
