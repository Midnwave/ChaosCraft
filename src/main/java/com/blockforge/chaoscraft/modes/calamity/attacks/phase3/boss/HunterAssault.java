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
 * Phase 3 Boss (Dweller) — TIER 2 "THE HUNTER" Attacks #31-40
 * HP range: 80%-60%. Wall traversal, brimstone columns, prowling,
 * mark exploitation, and the Tier 2 signature diving tackle.
 * Damage range: 12-18 HP (6-9 hearts).
 * NO status effects — damage only.
 */
public final class HunterAssault {

    private HunterAssault() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WallRake(plugin));
        registry.register(new DreadSpike(plugin));
        registry.register(new BrimstoneColumn(plugin));
        registry.register(new Prowl(plugin));
        registry.register(new ClawSwipe(plugin));
        registry.register(new MarkSpikeField(plugin));
        registry.register(new DistantGrowl(plugin));
        registry.register(new BrimstoneBrand(plugin));
        registry.register(new VoidTear(plugin));
        registry.register(new HuntersClaim(plugin));
    }

    // ================================================================
    // 31. WALL RAKE — Lateral wall traversal with horizontal fire spray
    // ================================================================
    public static class WallRake extends BossAttack {
        private final List<BlockDisplayHandle> sprayHandles = new ArrayList<>();
        private boolean raking = false;
        private int rakeTick = 0;

        public WallRake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_wall_rake", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller on wall at mid-height
            BlockDisplayHandle wallBody = displayBuilder.spawnBlock(
                center.clone().add(14, 6, -8), Material.BLACKSTONE);
            wallBody.scale(0.5f, 2.5f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(wallBody.entity());
            DisplayBuilder.playSound(center.clone().add(14, 6, 0),
                Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall position telegraph (0-30 ticks)
            if (ticksAlive < 30 && !raking) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(14, 6, -8), 5, 0.5,
                        255, 100, 0, 0.8f);
                }
            }
            // RAKE begins (tick 30)
            else if (ticksAlive == 30 && !raking) {
                raking = true;
                rakeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
            }
            // Lateral traversal with fire spray (30-150 ticks = 6 seconds)
            else if (raking && rakeTick < 120) {
                rakeTick++;
                float progress = rakeTick / 120.0f;
                float wallZ = -8 + progress * 16;
                // Fire spray blocks extending from wall
                if (rakeTick % 8 == 0) {
                    for (int d = 1; d <= 4; d++) {
                        Location sprayLoc = center.clone().add(14 - d, 6, wallZ);
                        BlockDisplayHandle spray = displayBuilder.spawnBlock(sprayLoc, Material.NETHERRACK);
                        spray.scale(0.3f, 0.2f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                        sprayHandles.add(spray);
                        spawnedEntities.add(spray.entity());
                    }
                    DisplayBuilder.crimsonDust(center.clone().add(12, 6, wallZ), 6, 2.0);
                }
                // Corruption line at wall base
                if (rakeTick % 12 == 0) {
                    Location baseLoc = center.clone().add(13, 0.1, wallZ);
                    BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, Material.MAGMA_BLOCK);
                    base.scale(0.7f, 0.08f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(base.entity());
                }
                if (rakeTick % 20 == 0) {
                    DisplayBuilder.playSound(center.clone().add(14, 6, wallZ),
                        Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WallRake(plugin); }
    }

    // ================================================================
    // 32. DREAD SPIKE — Instant beam targeting highest-dread player
    // ================================================================
    public static class DreadSpike extends BossAttack {
        private boolean fired = false;

        public DreadSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_dread_spike", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm raising, finger extending
            BlockDisplayHandle arm = displayBuilder.spawnBlock(
                center.clone().add(0.6, 2.0, 0), Material.BASALT);
            arm.scale(0.2f, 1.2f, 0.2f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(arm.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fingertip condensing particles (0-30 ticks)
            if (ticksAlive < 30 && !fired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0.6, 3.2, 0), 5, 0.2,
                        0, 150, 255, 0.8f);
                }
            }
            // FIRE — instant beam (tick 30)
            else if (ticksAlive == 30 && !fired) {
                fired = true;
                // Beam line from finger to target
                for (int i = 0; i < 15; i++) {
                    Location beamLoc = center.clone().add(0.6, 3.0, -1 - i * 1.2);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(beamLoc, Material.ORANGE_STAINED_GLASS);
                    beam.scale(0.04f, 0.04f, 1.1f).glow(0, 150, 255).interpolation(1, 0);
                    spawnedEntities.add(beam.entity());
                }
                // Cyan burst at target
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, -18), 20, 1.5,
                    0, 150, 255, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
            }
            // Target outline fading (30-50 ticks)
            else if (fired && ticksAlive <= 50) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, -18), 5, 0.8,
                        0, 150, 255, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DreadSpike(plugin); }
    }

    // ================================================================
    // 33. BRIMSTONE COLUMN — Erupting pillar from corruption zone
    // ================================================================
    public static class BrimstoneColumn extends BossAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private boolean erupted = false;

        public BrimstoneColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_column", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Target corruption zone tile pulsing
            BlockDisplayHandle zone = displayBuilder.spawnBlock(
                center.clone().add(5, 0.1, -3), Material.MAGMA_BLOCK);
            zone.scale(1.0f, 0.12f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(zone.entity());
            DisplayBuilder.playSound(center.clone().add(5, 0, -3),
                Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location columnBase = center.clone().add(5, 0, -3);

            // Rapid pulsing telegraph (0-40 ticks)
            if (ticksAlive < 40 && !erupted) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(columnBase.clone().add(0, 0.3, 0), 4, 0.4);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(columnBase, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f,
                        0.8f + (ticksAlive / 40.0f) * 0.8f);
                }
            }
            // ERUPTION (tick 40)
            else if (ticksAlive == 40 && !erupted) {
                erupted = true;
                // 8-block tall column of fire
                for (int y = 0; y < 8; y++) {
                    Location colLoc = columnBase.clone().add(0, y, 0);
                    BlockDisplayHandle col = displayBuilder.spawnBlock(colLoc, Material.MAGMA_BLOCK);
                    col.scale(0.8f, 0.9f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                    columnHandles.add(col);
                    spawnedEntities.add(col.entity());
                }
                DisplayBuilder.crimsonDust(columnBase.clone().add(0, 4, 0), 30, 2.0);
                DisplayBuilder.dustParticles(columnBase.clone().add(0, 6, 0), 15, 1.5,
                    255, 100, 0, 1.5f);
                DisplayBuilder.playSound(columnBase, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.7f);
                DisplayBuilder.playSound(columnBase, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
                triggerImpactDamage(columnBase);
            }
            // Column persists then collapses (40-100 ticks)
            else if (erupted && ticksAlive > 80 && ticksAlive <= 100) {
                // Ash rain phase
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double ox = (Math.random() - 0.5) * 3;
                        double oz = (Math.random() - 0.5) * 3;
                        DisplayBuilder.darkPurpleDust(
                            columnBase.clone().add(ox, 6 - Math.random() * 4, oz), 3, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneColumn(plugin); }
    }

    // ================================================================
    // 34. PROWL — Semi-transparent circling, damage deflected
    // ================================================================
    public static class Prowl extends BossAttack {
        private float prowlAngle = 0;

        public Prowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_prowl", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Semi-transparent shimmer form
            BlockDisplayHandle shimmer = displayBuilder.spawnBlock(
                center.clone().add(6, 1, 0), Material.BLACKSTONE);
            shimmer.scale(0.8f, 2.2f, 0.8f).glow(200, 0, 50).interpolation(5, 0);
            spawnedEntities.add(shimmer.entity());
            // Tiny pinpoint eye glow
            BlockDisplayHandle eyes = displayBuilder.spawnBlock(
                center.clone().add(6, 2.3, -0.3), Material.ORANGE_STAINED_GLASS);
            eyes.scale(0.08f, 0.04f, 0.03f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(eyes.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wide circling at half speed
            prowlAngle += 0.025f;
            double x = Math.cos(prowlAngle) * 7;
            double z = Math.sin(prowlAngle) * 7;

            // Extremely faint portal drip particles
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.darkPurpleDust(
                    center.clone().add(x, 1.2, z), 2, 0.4);
            }
            // Ambient dread tick sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center.clone().add(x, 1, z),
                    Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Prowl(plugin); }
    }

    // ================================================================
    // 35. CLAW SWIPE — 180-degree melee arc, 4-block range
    // ================================================================
    public static class ClawSwipe extends BossAttack {
        private boolean swiped = false;

        public ClawSwipe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_claw_swipe", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller form lunging forward
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 2.8f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Arm pulling back
            BlockDisplayHandle arm = displayBuilder.spawnBlock(
                center.clone().add(1, 1.8, 0.5), Material.BASALT);
            arm.scale(0.3f, 0.8f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(arm.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arm raising and pulling back (0-30 ticks)
            if (ticksAlive < 30 && !swiped) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1, 2, 0.5), 4, 0.3);
                }
            }
            // SWIPE — 180-degree arc (tick 30)
            else if (ticksAlive == 30 && !swiped) {
                swiped = true;
                // Fan of crimson crit particles in 180-degree sweep
                for (int deg = -90; deg <= 90; deg += 15) {
                    double a = Math.toRadians(deg);
                    for (int r = 2; r <= 4; r++) {
                        Location arcLoc = center.clone().add(
                            Math.sin(a) * r, 1.5, -Math.cos(a) * r);
                        DisplayBuilder.crimsonDust(arcLoc, 3, 0.3);
                    }
                }
                // Sweep slash blocks along arc
                for (int deg = -90; deg <= 90; deg += 30) {
                    double a = Math.toRadians(deg);
                    Location slashLoc = center.clone().add(
                        Math.sin(a) * 3.5, 1.5, -Math.cos(a) * 3.5);
                    BlockDisplayHandle slash = displayBuilder.spawnBlock(slashLoc, Material.NETHERRACK);
                    slash.scale(0.2f, 0.6f, 0.2f)
                        .rotate((float) a, 0, 1, 0)
                        .glow(200, 0, 50).interpolation(1, 0);
                    spawnedEntities.add(slash.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 1.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
            }
            // Brimstone crack flare after swipe (tick 40)
            else if (ticksAlive == 40) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 10, 1.0,
                    255, 100, 0, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClawSwipe(plugin); }
    }

    // ================================================================
    // 36. MARK SPIKE FIELD — Brimstone spikes erupt around Marked player
    // ================================================================
    public static class MarkSpikeField extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean erupted = false;

        public MarkSpikeField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_spike_field", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Amber aura flickering at target position
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 12, 1.0,
                255, 180, 0, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Amber aura flicker telegraph (0-40 ticks)
            if (ticksAlive < 40 && !erupted) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 8, 2.0,
                        255, 180, 0, 1.0f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
                }
            }
            // SPIKE ERUPTION (tick 40)
            else if (ticksAlive == 40 && !erupted) {
                erupted = true;
                // Ring of brimstone spikes at 5-block radius and intermediate
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2 / 12) * i;
                    for (int r = 2; r <= 5; r += 3) {
                        Location spikeLoc = center.clone().add(
                            Math.cos(a) * r, 0.1, Math.sin(a) * r);
                        BlockDisplayHandle spike = displayBuilder.spawnBlock(spikeLoc, Material.BASALT);
                        spike.scale(0.3f, 1.2f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                        spikeHandles.add(spike);
                        spawnedEntities.add(spike.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 25, 5.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 3.0,
                    255, 100, 0, 1.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
            }
            // Spikes persist with sizzle particles (40-160 ticks = 6 seconds)
            else if (erupted && ticksAlive < 160) {
                if (ticksAlive % 15 == 0) {
                    for (int i = 0; i < spikeHandles.size(); i += 3) {
                        Location loc = spikeHandles.get(i).entity().getLocation();
                        DisplayBuilder.dustParticles(loc.clone().add(0, 0.8, 0), 2, 0.2,
                            255, 100, 0, 0.6f);
                    }
                }
            }
            // Retraction — spikes descend, corruption zones remain (tick 160)
            else if (ticksAlive == 160) {
                for (BlockDisplayHandle spike : spikeHandles) {
                    Location loc = spike.entity().getLocation();
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    zone.scale(0.6f, 0.08f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkSpikeField(plugin); }
    }

    // ================================================================
    // 37. DISTANT GROWL — Audio disorientation, positional fakeout
    // ================================================================
    public static class DistantGrowl extends BossAttack {

        public DistantGrowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_distant_growl", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at edge, turned away
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(-14, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Turned away posture (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-14, 1.5, 0), 3, 0.5);
                }
            }
            // Deep growl from all directions (tick 20)
            else if (ticksAlive == 20) {
                // Growl sound from multiple fake positions
                DisplayBuilder.playSound(center.clone().add(10, 0, 10),
                    Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.2f);
                DisplayBuilder.playSound(center.clone().add(-10, 0, -10),
                    Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.2f);
                DisplayBuilder.playSound(center.clone().add(10, 0, -10),
                    Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.2f);
            }
            // Teleport to opposite side (tick 40)
            else if (ticksAlive == 40) {
                DisplayBuilder.darkPurpleDust(center.clone().add(-14, 1.5, 0), 15, 1.0);
                // Reappear on opposite side
                BlockDisplayHandle newBody = displayBuilder.spawnBlock(
                    center.clone().add(14, 1.2, 0), Material.POLISHED_BLACKSTONE);
                newBody.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(newBody.entity());
                DisplayBuilder.darkPurpleDust(center.clone().add(14, 1.5, 0), 15, 1.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DistantGrowl(plugin); }
    }

    // ================================================================
    // 38. BRIMSTONE BRAND — Contact brand with delayed bonus damage
    // ================================================================
    public static class BrimstoneBrand extends BossAttack {
        private boolean branded = false;

        public BrimstoneBrand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_brand", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Teleport behind Marked player
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 2), 12, 0.8);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Appearance behind target (0-20 ticks — short, Tier 2)
            if (ticksAlive < 20 && !branded) {
                if (ticksAlive % 6 == 0) {
                    // Dweller solidifying behind
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 2), 6, 0.6);
                }
            }
            // BRAND (tick 20) — chest cracks pressed against back
            else if (ticksAlive == 20 && !branded) {
                branded = true;
                // Sizzle burst at contact point
                BlockDisplayHandle brandMark = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.2, 1), Material.MAGMA_BLOCK);
                brandMark.scale(0.5f, 0.5f, 0.1f).glow(255, 100, 0).interpolation(2, 0);
                spawnedEntities.add(brandMark.entity());
                DisplayBuilder.dustParticles(center.clone().add(0, 1.2, 1), 15, 0.5,
                    255, 100, 0, 1.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.6f);
            }
            // Brand glow persisting on target (20-60 ticks)
            else if (branded && ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    // Orange footprint particles around branded player
                    for (int i = 0; i < 4; i++) {
                        double a = (Math.PI * 2 / 4) * i + ticksAlive * 0.05;
                        DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6),
                            2, 0.1, 255, 100, 0, 0.6f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBrand(plugin); }
    }

    // ================================================================
    // 39. VOID TEAR — Ground rift that displaces players
    // ================================================================
    public static class VoidTear extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean torn = false;

        public VoidTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_tear", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller kneeling, driving hands to ground
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 0.6, 0), Material.BLACKSTONE);
            body.scale(1.3f, 1.5f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hands driving into ground telegraph (0-40 ticks)
            if (ticksAlive < 40 && !torn) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.3, -1), 6, 1.0);
                }
            }
            // TEAR (tick 40) — rift opens in ground
            else if (ticksAlive == 40 && !torn) {
                torn = true;
                // 3-block wide jagged rift
                for (int i = -1; i <= 1; i++) {
                    Location riftLoc = center.clone().add(i, 0.05, -2);
                    BlockDisplayHandle rift = displayBuilder.spawnBlock(riftLoc, Material.CRYING_OBSIDIAN);
                    rift.scale(0.9f, 0.05f, 1.5f).glow(200, 0, 50).interpolation(2, 0);
                    riftHandles.add(rift);
                    spawnedEntities.add(rift.entity());
                }
                // Portal particle curtain filling the rift
                for (int i = -1; i <= 1; i++) {
                    for (int y = 0; y < 3; y++) {
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add(i, 0.3 + y * 0.5, -2), 5, 0.3);
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);
            }
            // Rift persists with swirling particles (40-200 ticks = 8 seconds)
            else if (torn && ticksAlive < 200) {
                if (ticksAlive % 10 == 0) {
                    for (int i = -1; i <= 1; i++) {
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add(i, 0.5, -2), 3, 0.4);
                    }
                }
                if (ticksAlive % 30 == 0) {
                    DisplayBuilder.playSound(center.clone().add(0, 0, -2),
                        Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTear(plugin); }
    }

    // ================================================================
    // 40. HUNTER'S CLAIM — Signature Tier 2 finisher: sprint + wall-bounce + dive
    // ================================================================
    public static class HuntersClaim extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean sprinting = false;
        private boolean bounced = false;
        private boolean dived = false;
        private int sprintTick = 0;

        public HuntersClaim(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_hunters_claim", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at far edge, coiled to sprint
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(-16, 1, 0), Material.BLACKSTONE);
            body.scale(1.4f, 2.5f, 1.4f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.crimsonDust(center.clone().add(-16, 1.5, 0), 10, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sprint buildup (0-20 ticks)
            if (ticksAlive < 20 && !sprinting) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-16, 1.5, 0), 6, 0.8);
                    DisplayBuilder.dustParticles(center.clone().add(-16, 1, 0), 4, 0.5,
                        255, 100, 0, 1.0f);
                }
            }
            // SPRINT across arena (tick 20)
            else if (ticksAlive == 20 && !sprinting) {
                sprinting = true;
                sprintTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.8f);
            }
            // Sprint phase (20-45 ticks)
            else if (sprinting && !bounced && sprintTick < 25) {
                sprintTick++;
                float progress = sprintTick / 25.0f;
                float sprintX = -16 + progress * 30;
                if (sprintTick % 3 == 0) {
                    Location trailLoc = center.clone().add(sprintX - 2, 0.8, 0);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.NETHERRACK);
                    trail.scale(0.3f, 0.25f, 0.3f).glow(255, 100, 0).interpolation(1, 0);
                    trailHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.crimsonDust(trailLoc, 4, 0.5);
                }
            }
            // WALL BOUNCE (tick 45) — runs up wall 3 blocks and pushes off
            else if (sprinting && sprintTick == 25 && !bounced) {
                bounced = true;
                sprintTick = 0;
                DisplayBuilder.playSound(center.clone().add(14, 3, 0),
                    Sound.BLOCK_ANVIL_LAND, 1.5f, 1.0f);
                // Wall contact burst
                DisplayBuilder.crimsonDust(center.clone().add(14, 3, 0), 12, 1.0);
                DisplayBuilder.darkPurpleDust(center.clone().add(14, 4, 0), 8, 0.8);
            }
            // Dive arc from wall bounce to target (45-60 ticks)
            else if (bounced && !dived && sprintTick < 15) {
                sprintTick++;
                float diveProgress = sprintTick / 15.0f;
                float diveX = 14 - diveProgress * 14;
                float diveY = (float) Math.sin(diveProgress * Math.PI) * 8;
                if (sprintTick % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(diveX, diveY, 0), 5, 0.6);
                }
            }
            // LANDING IMPACT (tick 60)
            else if (bounced && sprintTick == 15 && !dived) {
                dived = true;
                Location landLoc = center.clone().add(0, 0.1, 0);
                // 4x4 corruption zone cluster
                for (int x = -2; x <= 1; x++) {
                    for (int z = -2; z <= 1; z++) {
                        Location zoneLoc = landLoc.clone().add(x, 0.1, z);
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                        zone.scale(0.9f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                    }
                }
                DisplayBuilder.crimsonDust(landLoc, 30, 4.0);
                DisplayBuilder.dustParticles(landLoc, 20, 2.0, 255, 100, 0, 1.5f);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                triggerImpactDamage(landLoc);
            }
            // Recovery roar — counter-attack window (tick 70)
            else if (dived && ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 15, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HuntersClaim(plugin); }
    }
}
