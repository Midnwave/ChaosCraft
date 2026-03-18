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
 * Phase 4D Boss — THE VOID EMPEROR (Dragon)
 * Phase 1: "The Awakening" — HP 100% to 65%
 * Attacks #11-20
 * NO status effects — damage only.
 */
public final class DragonPhase1B {

    private DragonPhase1B() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new StarfallArray(plugin));
        registry.register(new RiftClawSwipe(plugin));
        registry.register(new VoidTide(plugin));
        registry.register(new BladePincer(plugin));
        registry.register(new EchoingDread(plugin));
        registry.register(new CrystalLance(plugin));
        registry.register(new VoidPillar(plugin));
        registry.register(new FeatherfallDenial(plugin));
        registry.register(new OrbitalStrikeAlpha(plugin));
        registry.register(new SkyStreamRedirect(plugin));
    }

    // ================================================================
    // 11. STARFALL ARRAY — 6 void bolts raining from maximum height
    // ================================================================
    public static class StarfallArray extends BossAttack {
        private final List<BlockDisplayHandle> starHandles = new ArrayList<>();
        private boolean boltsFalling = false;
        private int fallTick = 0;

        public StarfallArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_starfall_array", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 landing zone markers — 2-block radius circles
            for (int i = 0; i < 6; i++) {
                double ox = (Math.random() - 0.5) * 24;
                double oz = (Math.random() - 0.5) * 24;
                Location markerLoc = center.clone().add(ox, 0.05, oz);
                BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.WHITE_STAINED_GLASS);
                marker.scale(4.0f, 0.04f, 4.0f).glow(255, 255, 255).interpolation(3, 0);
                starHandles.add(marker);
                spawnedEntities.add(marker.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — landing zones visible with END_ROD rain (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !boltsFalling) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle marker : starHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation().add(0, 5, 0), 6, 2.0);
                    }
                }
            }
            // Bolts start falling (tick 30) — staggered 4-tick delay between each
            else if (ticksAlive == 30 && !boltsFalling) {
                boltsFalling = true;
                fallTick = ticksAlive;
                // Spawn bolt column displays at max height
                for (int i = 0; i < 6; i++) {
                    Location markerLoc = starHandles.get(i).entity().getLocation();
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(
                        markerLoc.clone().add(0, 50, 0), Material.END_STONE);
                    bolt.scale(0.5f, 3.0f, 0.5f).glow(255, 255, 255).interpolation(1, 0);
                    starHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                }
            }
            // Bolts descend (30-70 ticks = 2 seconds staggered fall)
            else if (boltsFalling && ticksAlive - fallTick < 40) {
                for (int i = 0; i < 6; i++) {
                    int boltStartOffset = i * 4; // 4-tick stagger per bolt
                    int boltTicks = (ticksAlive - fallTick) - boltStartOffset;
                    if (boltTicks >= 0 && boltTicks < 30 && i + 6 < starHandles.size()) {
                        float height = 50 * (1.0f - (boltTicks / 30.0f));
                        Location markerLoc = starHandles.get(i).entity().getLocation();
                        starHandles.get(i + 6).entity().teleport(
                            markerLoc.clone().add(0, height, 0));
                    }
                    // Impact when bolt reaches ground
                    if (boltTicks == 30 && i < starHandles.size()) {
                        Location impactLoc = starHandles.get(i).entity().getLocation();
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.cyanDust(impactLoc, 15, 3.0);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 1.2f, 0.5f);
                    }
                }
            }
            // Impact site afterglow (70-150 ticks = 4 seconds)
            else if (boltsFalling && ticksAlive - fallTick >= 40 && ticksAlive - fallTick < 120) {
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < 6 && i < starHandles.size(); i++) {
                        DisplayBuilder.cyanDust(starHandles.get(i).entity().getLocation(), 4, 3.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarfallArray(plugin); }
    }

    // ================================================================
    // 12. RIFT CLAW SWIPE — 90-degree arc claw sweep
    // ================================================================
    public static class RiftClawSwipe extends BossAttack {
        private final List<BlockDisplayHandle> clawHandles = new ArrayList<>();
        private boolean swipeActive = false;
        private int swipeTick = 0;

        public RiftClawSwipe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_rift_claw_swipe", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Claw extends and glows with PORTAL violet
            BlockDisplayHandle claw = displayBuilder.spawnBlock(
                center.clone().add(3, 4, 0), Material.OBSIDIAN);
            claw.scale(0.5f, 0.5f, 2.0f).glow(160, 0, 255).interpolation(3, 0);
            clawHandles.add(claw);
            spawnedEntities.add(claw.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Claw glow telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !swipeActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(3, 4, 0), 6, 1.0);
                }
            }
            // Swipe executes (tick 40) — 90-degree arc from right to left
            else if (ticksAlive == 40 && !swipeActive) {
                swipeActive = true;
                swipeTick = ticksAlive;
                // Arc segments representing the sweep path
                for (int arc = 0; arc < 6; arc++) {
                    double sweepAngle = (Math.PI / 2) - (Math.PI / 2 / 6) * arc;
                    Location arcPt = center.clone().add(
                        Math.cos(sweepAngle) * 8, 4, Math.sin(sweepAngle) * 8);
                    BlockDisplayHandle arcBlock = displayBuilder.spawnBlock(arcPt, Material.OBSIDIAN);
                    arcBlock.scale(1.5f, 0.5f, 1.5f).glow(160, 0, 255).interpolation(1, 0);
                    clawHandles.add(arcBlock);
                    spawnedEntities.add(arcBlock.entity());
                }
                triggerImpactDamage(center.clone().add(0, 4, 5));
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 20, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
            }
            // Arc trail visible — glowing scar fades (40-55 ticks = 15 ticks)
            else if (swipeActive && ticksAlive - swipeTick < 15) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 8, 8.0);
                }
            }
            // Residual arc hangs in air (55-115 ticks = 3 seconds)
            else if (swipeActive && ticksAlive - swipeTick >= 15 && ticksAlive - swipeTick < 75) {
                if (ticksAlive % 10 == 0) {
                    for (int arc = 0; arc < 6; arc++) {
                        double sweepAngle = (Math.PI / 2) - (Math.PI / 2 / 6) * arc;
                        Location arcPt = center.clone().add(
                            Math.cos(sweepAngle) * 8, 4, Math.sin(sweepAngle) * 8);
                        DisplayBuilder.cyanDust(arcPt, 3, 1.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftClawSwipe(plugin); }
    }

    // ================================================================
    // 13. VOID TIDE — Wave of void energy sweeping across island
    // ================================================================
    public static class VoidTide extends BossAttack {
        private final List<BlockDisplayHandle> tideHandles = new ArrayList<>();
        private boolean tideActive = false;
        private int tideTick = 0;

        public VoidTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_tide", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Perimeter ring of blue-white flame rising inward
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location perimPt = center.clone().add(Math.cos(angle) * 25, 0.1, Math.sin(angle) * 25);
                BlockDisplayHandle perimSeg = displayBuilder.spawnBlock(perimPt, Material.SOUL_LANTERN);
                perimSeg.scale(1.5f, 0.5f, 1.5f).glow(100, 180, 255).interpolation(3, 0);
                tideHandles.add(perimSeg);
                spawnedEntities.add(perimSeg.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Perimeter ring moves inward (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !tideActive) {
                float radius = 25.0f - (ticksAlive / 40.0f) * 5.0f;
                for (int i = 0; i < 16 && i < tideHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    tideHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 10, radius);
                }
            }
            // Tide launches (tick 40) — wall sweeps from -Z to +Z
            else if (ticksAlive == 40 && !tideActive) {
                tideActive = true;
                tideTick = ticksAlive;
                // Tide wall segments — 40-block wide, 4 blocks tall
                for (int x = -20; x <= 20; x += 4) {
                    Location wallPt = center.clone().add(x, 2, -25);
                    BlockDisplayHandle wallSeg = displayBuilder.spawnBlock(wallPt, Material.CYAN_STAINED_GLASS);
                    wallSeg.scale(4.0f, 4.0f, 2.0f).glow(0, 120, 110).interpolation(2, 0);
                    tideHandles.add(wallSeg);
                    spawnedEntities.add(wallSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.3f);
            }
            // Tide sweeps across island (40-160 ticks = 6 seconds at 8 blocks/second)
            else if (tideActive && ticksAlive - tideTick < 120) {
                float progress = (ticksAlive - tideTick) / 120.0f;
                float zPos = -25 + progress * 50;
                for (int i = 16; i < tideHandles.size(); i++) {
                    Location basePt = tideHandles.get(i).entity().getLocation();
                    tideHandles.get(i).entity().teleport(
                        center.clone().add(basePt.getX() - center.getX(), 2, zPos));
                }
                if (ticksAlive % 8 == 0) {
                    triggerImpactDamage(center.clone().add(0, 2, zPos));
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, zPos), 12, 20.0);
                }
            }
            // Tide residue on ground (160-280 ticks = 6 seconds fade)
            else if (tideActive && ticksAlive - tideTick >= 120 && ticksAlive - tideTick < 240) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 20.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTide(plugin); }
    }

    // ================================================================
    // 14. BLADE PINCER — Two blades converge on single player from opposites
    // ================================================================
    public static class BladePincer extends BossAttack {
        private final List<BlockDisplayHandle> pincerHandles = new ArrayList<>();
        private boolean bladesFired = false;
        private int fireTick = 0;

        public BladePincer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_blade_pincer", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Two blades halt on opposite sides, angled toward player
            BlockDisplayHandle bladeA = displayBuilder.spawnBlock(
                center.clone().add(-12, 4, 0), Material.IRON_BLOCK);
            bladeA.scale(0.15f, 0.15f, 1.2f).glow(255, 240, 100).interpolation(2, 0);
            pincerHandles.add(bladeA);
            spawnedEntities.add(bladeA.entity());

            BlockDisplayHandle bladeB = displayBuilder.spawnBlock(
                center.clone().add(12, 4, 0), Material.IRON_BLOCK);
            bladeB.scale(0.15f, 0.15f, 1.2f).glow(255, 240, 100).interpolation(2, 0);
            pincerHandles.add(bladeB);
            spawnedEntities.add(bladeB.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pointing telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !bladesFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-12, 4, 0), 4, 0.5);
                    DisplayBuilder.cyanDust(center.clone().add(12, 4, 0), 4, 0.5);
                }
            }
            // Blades fire simultaneously (tick 30)
            else if (ticksAlive == 30 && !bladesFired) {
                bladesFired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.6f);
            }
            // Blades converge toward center (30-55 ticks = 1.25 seconds at 0.8x speed)
            else if (bladesFired && ticksAlive - fireTick < 25) {
                float progress = (ticksAlive - fireTick) / 25.0f;
                pincerHandles.get(0).entity().teleport(
                    center.clone().add(-12 * (1 - progress), 4, 0));
                pincerHandles.get(1).entity().teleport(
                    center.clone().add(12 * (1 - progress), 4, 0));
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(pincerHandles.get(0).entity().getLocation(), 3, 0.5);
                    DisplayBuilder.cyanDust(pincerHandles.get(1).entity().getLocation(), 3, 0.5);
                }
            }
            // Convergence burst at center (tick 55)
            else if (bladesFired && ticksAlive - fireTick == 25) {
                triggerImpactDamage(center.clone().add(0, 4, 0));
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 20, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.4f);
            }
            // Blades return to orbit (55-115 ticks = 3 seconds)
            else if (bladesFired && ticksAlive - fireTick > 25 && ticksAlive - fireTick < 85) {
                float progress = (ticksAlive - fireTick - 25) / 60.0f;
                pincerHandles.get(0).entity().teleport(
                    center.clone().add(-12 * progress, 4 + progress, 0));
                pincerHandles.get(1).entity().teleport(
                    center.clone().add(12 * progress, 4 + progress, 0));
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 4, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladePincer(plugin); }
    }

    // ================================================================
    // 15. ECHOING DREAD — Pure debuff psychic pulse (no direct damage)
    // ================================================================
    public static class EchoingDread extends BossAttack {
        private final List<BlockDisplayHandle> dreadHandles = new ArrayList<>();
        private boolean pulseFired = false;

        public EchoingDread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_echoing_dread", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon goes completely still — unnatural silence
            // Sky streams intensify — large glow marker
            BlockDisplayHandle dreadCore = displayBuilder.spawnBlock(
                center.clone().add(0, 8, 0), Material.BLACK_STAINED_GLASS);
            dreadCore.scale(4.0f, 4.0f, 4.0f).glow(20, 0, 40).interpolation(5, 0);
            dreadHandles.add(dreadCore);
            spawnedEntities.add(dreadCore.entity());
            // 1 second of silence — then barely audible rumble
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silence period (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !pulseFired) {
                // Nothing — pure silence is the telegraph
            }
            // Sky streams intensify (20-50 ticks = 1.5 seconds)
            else if (ticksAlive >= 20 && ticksAlive < 50 && !pulseFired) {
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 0.1f);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 15, 10.0);
                }
            }
            // Psychic pulse fires (tick 50) — radial soul fire flame burst
            else if (ticksAlive == 50 && !pulseFired) {
                pulseFired = true;
                // Blue-white radial burst
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    for (int r = 2; r <= 20; r += 4) {
                        Location burstPt = center.clone().add(
                            Math.cos(angle) * r, 4, Math.sin(angle) * r);
                        BlockDisplayHandle burstSeg = displayBuilder.spawnBlock(burstPt, Material.SOUL_LANTERN);
                        burstSeg.scale(1.0f, 1.0f, 1.0f).glow(100, 180, 255).interpolation(2, 0);
                        dreadHandles.add(burstSeg);
                        spawnedEntities.add(burstSeg.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            }
            // Darkness aftermath (50-170 ticks = 6 seconds of debuff window)
            else if (pulseFired && ticksAlive < 170) {
                // Nothing visible — silence IS the effect
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoingDread(plugin); }
    }

    // ================================================================
    // 16. CRYSTAL LANCE — Single concentrated high-speed projectile
    // ================================================================
    public static class CrystalLance extends BossAttack {
        private final List<BlockDisplayHandle> lanceHandles = new ArrayList<>();
        private boolean lanceFired = false;
        private int fireTick = 0;

        public CrystalLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystal_lance", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Specific crystal structure begins glowing intensely
            BlockDisplayHandle crystal = displayBuilder.spawnBlock(
                center.clone().add(10, 1.5, 0), Material.AMETHYST_BLOCK);
            crystal.scale(1.2f, 2.0f, 1.2f).glow(255, 200, 255).interpolation(3, 0);
            lanceHandles.add(crystal);
            spawnedEntities.add(crystal.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crystal charges — radiating expanding particles (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !lanceFired) {
                float burstRadius = (ticksAlive / 30.0f) * 4.0f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(10, 1.5, 0), 8, burstRadius);
                }
            }
            // Lance fires (tick 30) — fast narrow projectile
            else if (ticksAlive == 30 && !lanceFired) {
                lanceFired = true;
                fireTick = ticksAlive;
                // Lance beam segments
                for (int seg = 0; seg < 20; seg++) {
                    Location segLoc = center.clone().add(10 - seg * 2, 2.5, 0);
                    BlockDisplayHandle lanceSeg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    lanceSeg.scale(0.3f, 0.3f, 0.3f).glow(255, 200, 255).interpolation(1, 0);
                    lanceHandles.add(lanceSeg);
                    spawnedEntities.add(lanceSeg.entity());
                }
                triggerImpactDamage(center.clone().add(-30, 2.5, 0));
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.5f, 1.8f);
            }
            // Lance trail visible (30-70 ticks = 2 seconds trajectory afterimage)
            else if (lanceFired && ticksAlive - fireTick < 40) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 8, 20.0);
                }
            }
            // Crystal goes dark — cooldown glow (70-970 ticks = 45 seconds)
            else if (lanceFired && ticksAlive - fireTick >= 40 && ticksAlive - fireTick < 940) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(10, 2.5, 0), 3, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalLance(plugin); }
    }

    // ================================================================
    // 17. VOID PILLAR — Erupting void energy column 30 blocks tall
    // ================================================================
    public static class VoidPillar extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private boolean pillarActive = false;
        private int pillarTick = 0;

        public VoidPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_pillar", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground vortex appears beneath a player — 2-block expanding circle
            BlockDisplayHandle vortex = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 0), Material.PURPLE_STAINED_GLASS);
            vortex.scale(0.5f, 0.04f, 0.5f).glow(60, 0, 120).interpolation(4, 0);
            pillarHandles.add(vortex);
            spawnedEntities.add(vortex.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vortex expanding (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pillarActive) {
                float scale = 0.5f + (ticksAlive / 40.0f) * 5.5f;
                pillarHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 0, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, 0.04f, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, scale);
                }
            }
            // Pillar erupts upward (tick 40) — 3 blocks wide, 30 blocks tall
            else if (ticksAlive == 40 && !pillarActive) {
                pillarActive = true;
                pillarTick = ticksAlive;
                for (int h = 0; h < 10; h++) {
                    Location pillarPt = center.clone().add(0, h * 3, 0);
                    BlockDisplayHandle pillarSeg = displayBuilder.spawnBlock(pillarPt, Material.PURPLE_STAINED_GLASS);
                    pillarSeg.scale(3.0f, 3.0f, 3.0f).glow(40, 0, 90).interpolation(2, 0);
                    pillarHandles.add(pillarSeg);
                    spawnedEntities.add(pillarSeg.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 30, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 2.0f, 0.3f);
            }
            // Pillar active — helix particles (40-140 ticks = 5 seconds)
            else if (pillarActive && ticksAlive - pillarTick < 100) {
                if (ticksAlive % 6 == 0) {
                    double helixAngle = ticksAlive * 0.3;
                    float helixHeight = (ticksAlive % 20) * 1.5f;
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(helixAngle) * 1.5, helixHeight, Math.sin(helixAngle) * 1.5), 6, 1.0);
                }
            }
            // Pillar dissipates (tick 140)
            else if (pillarActive && ticksAlive - pillarTick == 100) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.8f);
            }
            // Aftermath column — faded (140-220 ticks = 4 seconds)
            else if (pillarActive && ticksAlive - pillarTick > 100 && ticksAlive - pillarTick < 180) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPillar(plugin); }
    }

    // ================================================================
    // 18. FEATHERFALL DENIAL — Gravity field intensifying fall damage
    // ================================================================
    public static class FeatherfallDenial extends BossAttack {
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private boolean fieldActive = false;
        private int fieldTick = 0;

        public FeatherfallDenial(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_featherfall_denial", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sky darkens — all streams double density visual
            BlockDisplayHandle skyDarken = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.BLACK_STAINED_GLASS);
            skyDarken.scale(40.0f, 0.3f, 40.0f).glow(30, 0, 50).interpolation(5, 0);
            fieldHandles.add(skyDarken);
            spawnedEntities.add(skyDarken.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sky darkens — double density streams (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fieldActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 15, 20.0);
                }
            }
            // Gravity field activates (tick 40) — gold-white rain pattern
            else if (ticksAlive == 40 && !fieldActive) {
                fieldActive = true;
                fieldTick = ticksAlive;
                // Rain pillars across the island
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * 40;
                    double oz = (Math.random() - 0.5) * 40;
                    Location rainPt = center.clone().add(ox, 20, oz);
                    BlockDisplayHandle rain = displayBuilder.spawnBlock(rainPt, Material.YELLOW_STAINED_GLASS);
                    rain.scale(0.3f, 20.0f, 0.3f).glow(255, 245, 180).interpolation(3, 0);
                    fieldHandles.add(rain);
                    spawnedEntities.add(rain.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.3f);
            }
            // Gravity field sustained (40-280 ticks = 12 seconds)
            else if (fieldActive && ticksAlive - fieldTick < 240) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, 20.0);
                }
            }
            // Field ends (tick 280) — fade upward
            else if (fieldActive && ticksAlive - fieldTick == 240) {
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 20, 15.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FeatherfallDenial(plugin); }
    }

    // ================================================================
    // 19. ORBITAL STRIKE PATTERN ALPHA — Cardinal blade convergence
    // ================================================================
    public static class OrbitalStrikeAlpha extends BossAttack {
        private final List<BlockDisplayHandle> strikeHandles = new ArrayList<>();
        private boolean strikeActive = false;
        private int strikeTick = 0;

        public OrbitalStrikeAlpha(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_orbital_strike_alpha", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 blades align to cardinal directions on orbit ring
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i; // N, E, S, W
                Location bladeLoc = center.clone().add(Math.cos(angle) * 20, 5, Math.sin(angle) * 20);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.2f).glow(255, 255, 255).interpolation(2, 0);
                strikeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            // Cardinal cross spark trails
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                for (int r = 5; r <= 20; r += 5) {
                    Location trailPt = center.clone().add(Math.cos(angle) * r, 5, Math.sin(angle) * r);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailPt, Material.WHITE_STAINED_GLASS);
                    trail.scale(0.3f, 0.1f, 0.3f).glow(255, 255, 255).interpolation(2, 0);
                    strikeHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Alignment telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !strikeActive) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI / 2) * i;
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(angle) * 12, 5, Math.sin(angle) * 12), 4, 1.0);
                    }
                }
            }
            // Blades fire inward simultaneously (tick 30) — converge on center
            else if (ticksAlive == 30 && !strikeActive) {
                strikeActive = true;
                strikeTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.7f);
            }
            // Blades converge (30-40 ticks = 0.5 seconds high speed)
            else if (strikeActive && ticksAlive - strikeTick < 10) {
                float progress = (ticksAlive - strikeTick) / 10.0f;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    float dist = 20 * (1.0f - progress);
                    strikeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 5, Math.sin(angle) * dist));
                }
            }
            // Convergence impact at center (tick 40)
            else if (strikeActive && ticksAlive - strikeTick == 10) {
                triggerImpactDamage(center.clone().add(0, 5, 0));
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 25, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.0f);
            }
            // Blades hover at center (40-60 ticks = 1 second)
            else if (strikeActive && ticksAlive - strikeTick > 10 && ticksAlive - strikeTick < 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 6, 1.0);
                }
            }
            // Blades return to orbit (60-100 ticks)
            else if (strikeActive && ticksAlive - strikeTick >= 30 && ticksAlive - strikeTick < 70) {
                float progress = (ticksAlive - strikeTick - 30) / 40.0f;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    float dist = progress * 20;
                    strikeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 5, Math.sin(angle) * dist));
                }
            }
            // Afterglow ring at center (100-160 ticks = 3 seconds)
            else if (strikeActive && ticksAlive - strikeTick >= 70 && ticksAlive - strikeTick < 130) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalStrikeAlpha(plugin); }
    }

    // ================================================================
    // 20. SKY STREAM REDIRECT — Sky streams redirected downward at 45 degrees
    // ================================================================
    public static class SkyStreamRedirect extends BossAttack {
        private final List<BlockDisplayHandle> streamHandles = new ArrayList<>();
        private boolean streamsActive = false;
        private int streamTick = 0;

        public SkyStreamRedirect(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_sky_stream_redirect", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon barrel roll — helix of bright particles
            BlockDisplayHandle rollCore = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.YELLOW_STAINED_GLASS);
            rollCore.scale(2.0f, 2.0f, 2.0f).glow(255, 255, 100).interpolation(4, 0);
            streamHandles.add(rollCore);
            spawnedEntities.add(rollCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Barrel roll telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !streamsActive) {
                double helixAngle = ticksAlive * 0.5;
                float helixRadius = 3.0f;
                streamHandles.get(0).entity().teleport(
                    center.clone().add(
                        Math.cos(helixAngle) * helixRadius, 30,
                        Math.sin(helixAngle) * helixRadius));
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 8, 4.0);
                }
            }
            // Streams redirect downward (tick 40)
            else if (ticksAlive == 40 && !streamsActive) {
                streamsActive = true;
                streamTick = ticksAlive;
                // 3 angled stream columns from sky to island
                for (int s = 0; s < 3; s++) {
                    double streamAngle = (Math.PI * 2 / 3) * s;
                    for (int seg = 0; seg < 8; seg++) {
                        float height = 30 - seg * 3.5f;
                        float hDist = seg * 2.5f;
                        Location segLoc = center.clone().add(
                            Math.cos(streamAngle) * hDist, height, Math.sin(streamAngle) * hDist);
                        BlockDisplayHandle streamSeg = displayBuilder.spawnBlock(segLoc, Material.WHITE_STAINED_GLASS);
                        streamSeg.scale(1.0f, 1.0f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                        streamHandles.add(streamSeg);
                        spawnedEntities.add(streamSeg.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 1.4f);
            }
            // Streams sweep downward for 10 seconds (40-240 ticks)
            else if (streamsActive && ticksAlive - streamTick < 200) {
                // Rotate stream directions slowly
                if (ticksAlive % 10 == 0) {
                    double rotOffset = (ticksAlive - streamTick) * 0.02;
                    int idx = 1; // skip the roll core
                    for (int s = 0; s < 3; s++) {
                        double streamAngle = (Math.PI * 2 / 3) * s + rotOffset;
                        for (int seg = 0; seg < 8; seg++) {
                            float height = 30 - seg * 3.5f;
                            float hDist = seg * 2.5f;
                            if (idx < streamHandles.size()) {
                                streamHandles.get(idx).entity().teleport(
                                    center.clone().add(
                                        Math.cos(streamAngle) * hDist, height,
                                        Math.sin(streamAngle) * hDist));
                                idx++;
                            }
                        }
                    }
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 10, 15.0);
                }
            }
            // Streams revert to horizontal (tick 240) — settling rain
            else if (streamsActive && ticksAlive - streamTick == 200) {
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 20, 20.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.8f);
            }
            // Settling aftermath (240-300 ticks = 3 seconds)
            else if (streamsActive && ticksAlive - streamTick > 200 && ticksAlive - streamTick < 260) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 6, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyStreamRedirect(plugin); }
    }
}
