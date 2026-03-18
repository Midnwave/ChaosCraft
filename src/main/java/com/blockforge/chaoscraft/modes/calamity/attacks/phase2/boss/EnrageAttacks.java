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
 * Phase 2 Boss Attacks - GROUP 10: ENRAGE (#91-100)
 * Fires only below 25% HP. DoG has shed all limits.
 * Multi-mechanic, visually overwhelming, unforgiving.
 * NO status effects - damage only.
 */
public final class EnrageAttacks {

    private EnrageAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CosmicAnnihilation(plugin));
        registry.register(new GodWrathCharge(plugin));
        registry.register(new RiftCollapse(plugin));
        registry.register(new RealityConsumingLaser(plugin));
        registry.register(new TheDevouring(plugin));
        registry.register(new StarDeath(plugin));
        registry.register(new RiftWebImprison(plugin));
        registry.register(new GodForm(plugin));
        registry.register(new Singularity(plugin));
        registry.register(new EndOfAllThings(plugin));
    }

    // ================================================================
    // 91. COSMIC ANNIHILATION - Radial burst of 40+ beams from corona
    // DoG rises, spine fans out, 360-degree beam sphere for 3 sec
    // ================================================================
    public static class CosmicAnnihilation extends BossAttack {
        private final List<BlockDisplayHandle> coronaHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamsFired = false;

        public CosmicAnnihilation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_annihilation", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG rises to center, spine segments fanning out
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(a) * 2, 10, Math.sin(a) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(4, 0);
                coronaHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Corona formation (0-40 ticks = 2 sec): segments fan outward
            if (ticksAlive < 40 && !beamsFired) {
                float fanOut = ticksAlive / 40.0f;
                for (int i = 0; i < coronaHandles.size(); i++) {
                    double a = (Math.PI * 2 / coronaHandles.size()) * i;
                    double r = 2 + fanOut * 6;
                    coronaHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 10, Math.sin(a) * r));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 15, 4.0 + fanOut * 4);
                }
            }
            // Beams fire (tick 40): 40 beams in all directions
            else if (ticksAlive == 40 && !beamsFired) {
                beamsFired = true;
                Location beamOrigin = center.clone().add(0, 10, 0);
                for (int beam = 0; beam < 40; beam++) {
                    double yaw = (Math.PI * 2 / 40) * beam;
                    double pitch = (beam % 5) * (Math.PI / 10) - Math.PI / 5;
                    // Each beam extends 20 blocks
                    for (int seg = 1; seg <= 20; seg++) {
                        Location segLoc = beamOrigin.clone().add(
                            Math.cos(yaw) * Math.cos(pitch) * seg,
                            Math.sin(pitch) * seg,
                            Math.sin(yaw) * Math.cos(pitch) * seg);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                        h.scale(0.2f, 0.2f, 1.0f).glow(240, 240, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.cyanDust(beamOrigin, 60, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
            }
            // Beams sustained (40-100 ticks = 3 sec)
            else if (beamsFired && ticksAlive < 100) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 20, 6.0);
                    for (int i = 0; i < beamHandles.size(); i += 40) {
                        DisplayBuilder.cyanDust(beamHandles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
            }
            // Beams fade (tick 100)
            else if (ticksAlive == 100) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
                for (BlockDisplayHandle h : coronaHandles) h.entity().remove();
                coronaHandles.clear();
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicAnnihilation(plugin); }
    }

    // ================================================================
    // 92. GOD WRATH CHARGE - 5x speed for 5 sec
    // Visually incomprehensible speed, smearing contrail across arena
    // ================================================================
    public static class GodWrathCharge extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean chargeActive = false;

        public GodWrathCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_wrath_charge", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Deep sound cue only - no visual telegraph
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.15f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sound-only telegraph (0-20 ticks = 1 sec)
            if (ticksAlive < 20 && !chargeActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 2, 0), 4, 3.0);
                }
            }
            // Speed burst begins (tick 20)
            else if (ticksAlive == 20 && !chargeActive) {
                chargeActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
            // 5x speed charge (20-120 ticks = 5 sec): dense smearing trail
            else if (chargeActive && ticksAlive < 120) {
                if (ticksAlive % 2 == 0) {
                    // Rapid position changes - DoG covers entire arena
                    double angle = (ticksAlive - 20) * 0.3;
                    double radius = 5 + Math.sin((ticksAlive - 20) * 0.15) * 10;
                    Location bodyLoc = center.clone().add(
                        Math.cos(angle) * radius, 2, Math.sin(angle) * radius);

                    // Contrail segment
                    BlockDisplayHandle h = displayBuilder.spawnBlock(bodyLoc, Material.SEA_LANTERN);
                    h.scale(0.5f, 0.5f, 2.0f).glow(240, 240, 255).interpolation(1, 0);
                    trailHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Dense particle streak
                    DisplayBuilder.cyanDust(bodyLoc, 8, 1.5);

                    // Limit trail segments (budget: 150 max)
                    while (trailHandles.size() > 100) {
                        trailHandles.get(0).entity().remove();
                        trailHandles.remove(0);
                    }
                }
            }
            // Charge ends (tick 120)
            else if (ticksAlive == 120) {
                for (BlockDisplayHandle h : trailHandles) h.entity().remove();
                trailHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodWrathCharge(plugin); }
    }

    // ================================================================
    // 93. RIFT COLLAPSE - 8 rifts simultaneously implode then explode
    // Pull force toward each rift, then 12-block radius explosions
    // ================================================================
    public static class RiftCollapse extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final double[][] riftPositions = new double[8][2];
        private boolean riftsFormed = false;
        private boolean collapsed = false;
        private boolean exploded = false;

        public RiftCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_collapse", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 rift spawn positions marked
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                double dist = 8 + Math.random() * 6;
                riftPositions[i][0] = Math.cos(angle) * dist;
                riftPositions[i][1] = Math.sin(angle) * dist;

                // Column markers
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(riftPositions[i][0], y, riftPositions[i][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.3f, 1.0f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rift columns visible (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !riftsFormed) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(riftPositions[i][0], 2, riftPositions[i][1]), 6, 1.0);
                    }
                }
            }
            // Rifts form fully (tick 30)
            else if (ticksAlive == 30 && !riftsFormed) {
                riftsFormed = true;
                // Expand each rift visually
                for (int i = 0; i < 8; i++) {
                    Location riftLoc = center.clone().add(riftPositions[i][0], 0, riftPositions[i][1]);
                    for (int r = 0; r < 6; r++) {
                        double a = (Math.PI * 2 / 6) * r;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            riftLoc.clone().add(Math.cos(a) * 2, 1.5, Math.sin(a) * 2), Material.DARK_PRISMARINE);
                        h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                        riftHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            // Collapse: pull inward (40-70 ticks = 1.5 sec pull phase)
            else if (riftsFormed && ticksAlive >= 40 && ticksAlive < 70 && !collapsed) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        // Pull particles converging on rift center
                        Location riftCenter = center.clone().add(riftPositions[i][0], 1.5, riftPositions[i][1]);
                        for (int p = 0; p < 4; p++) {
                            double pa = Math.random() * Math.PI * 2;
                            double pr = 6 + Math.random() * 2;
                            DisplayBuilder.cyanDust(
                                riftCenter.clone().add(Math.cos(pa) * pr, 0, Math.sin(pa) * pr), 3, 0.5);
                        }
                    }
                }
            }
            // Explosion (tick 70): all 8 rifts explode simultaneously
            else if (ticksAlive == 70 && !exploded) {
                collapsed = true;
                exploded = true;
                // Remove all rift blocks
                for (BlockDisplayHandle h : riftHandles) h.entity().remove();
                riftHandles.clear();

                // Massive explosion at each rift
                for (int i = 0; i < 8; i++) {
                    Location riftCenter = center.clone().add(riftPositions[i][0], 1.5, riftPositions[i][1]);
                    DisplayBuilder.cyanDust(riftCenter, 40, 12.0);
                    DisplayBuilder.purpleDust(riftCenter, 30, 10.0);

                    // Explosion debris ring
                    for (int d = 0; d < 8; d++) {
                        double da = (Math.PI * 2 / 8) * d;
                        for (int r = 2; r <= 10; r += 3) {
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                riftCenter.clone().add(Math.cos(da) * r, 0.5, Math.sin(da) * r),
                                Material.CRYING_OBSIDIAN);
                            h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(1, 0);
                            riftHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.2f);
            }
            // Explosion aftermath lingers (70-120)
            else if (exploded && ticksAlive < 120) {
                if (ticksAlive % 15 == 0) {
                    for (int i = 0; i < 8; i++) {
                        DisplayBuilder.purpleDust(
                            center.clone().add(riftPositions[i][0], 1.5, riftPositions[i][1]), 5, 5.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftCollapse(plugin); }
    }

    // ================================================================
    // 94. REALITY CONSUMING LASER - 12-block-wide beam, 180-degree sweep
    // Widest beam in encounter, 6-sec sweep, terrain dissolves visually
    // ================================================================
    public static class RealityConsumingLaser extends BossAttack {
        private final List<BlockDisplayHandle> jawHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private double beamBaseAngle = 0;

        public RealityConsumingLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_consuming_laser", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            beamBaseAngle = Math.random() * Math.PI * 2;
            // Jaw opening animation
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 / 6) * i;
                Location loc = center.clone().add(Math.cos(a) * 2, 4, Math.sin(a) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(2.0f, 2.0f, 2.0f).glow(0, 200, 255).interpolation(4, 0);
                jawHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.15f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaw opening (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !beamActive) {
                float openFrac = ticksAlive / 40.0f;
                for (int i = 0; i < jawHandles.size(); i++) {
                    double a = (Math.PI * 2 / jawHandles.size()) * i;
                    double r = 2 + openFrac * 5;
                    jawHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 4, Math.sin(a) * r));
                }
                if (ticksAlive % 5 == 0) {
                    double precursorWidth = 2 + openFrac * 10;
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(beamBaseAngle) * 4, 3, Math.sin(beamBaseAngle) * 4),
                        (int)(precursorWidth * 2), precursorWidth * 0.5);
                }
            }
            // Beam fires (tick 40)
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                for (BlockDisplayHandle h : jawHandles) h.entity().remove();
                jawHandles.clear();
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
            }
            // Beam sweeps 180 degrees (40-160 ticks = 6 sec)
            else if (beamActive && ticksAlive >= 40 && ticksAlive < 160) {
                float sweepFrac = (ticksAlive - 40) / 120.0f;
                double currentAngle = beamBaseAngle + sweepFrac * Math.PI;

                // Rebuild beam at current angle
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();

                for (int seg = 0; seg < 18; seg++) {
                    Location beamLoc = center.clone().add(
                        Math.cos(currentAngle) * seg * 1.5, 2, Math.sin(currentAngle) * seg * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.SEA_LANTERN);
                    h.scale(12.0f, 4.0f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Terrain dissolution particles where beam hits ground
                if (ticksAlive % 4 == 0) {
                    for (int r = 5; r <= 20; r += 5) {
                        DisplayBuilder.cyanDust(center.clone().add(
                            Math.cos(currentAngle) * r, 0.5, Math.sin(currentAngle) * r), 10, 6.0);
                        DisplayBuilder.purpleDust(center.clone().add(
                            Math.cos(currentAngle) * r, 0.5, Math.sin(currentAngle) * r), 8, 4.0);
                    }
                }
            }
            // Beam ends (tick 160)
            else if (ticksAlive == 160) {
                for (BlockDisplayHandle h : beamHandles) h.entity().remove();
                beamHandles.clear();
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityConsumingLaser(plugin); }
    }

    // ================================================================
    // 95. THE DEVOURING - 8-block-wide maw with gravitational pull
    // 15-block pull radius, 3-block consumption trigger, 6 sec duration
    // ================================================================
    public static class TheDevouring extends BossAttack {
        private final List<BlockDisplayHandle> mawHandles = new ArrayList<>();
        private boolean mawOpen = false;

        public TheDevouring(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_devouring", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Jaw yawning open
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 / 12) * i;
                Location loc = center.clone().add(Math.cos(a) * 1, 3, Math.sin(a) * 1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(4, 0);
                mawHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.15f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Maw expansion (0-50 ticks = 2.5 sec)
            if (ticksAlive < 50 && !mawOpen) {
                float openFrac = ticksAlive / 50.0f;
                for (int i = 0; i < mawHandles.size(); i++) {
                    double a = (Math.PI * 2 / mawHandles.size()) * i;
                    double r = 1 + openFrac * 3;
                    float scale = 0.8f + openFrac * 1.2f;
                    mawHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r));
                    BlockDisplay bd = (BlockDisplay) mawHandles.get(i).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(ticksAlive * 0.03f, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 10, 2.0 + openFrac * 3);
                }
            }
            // Maw fully open (tick 50)
            else if (ticksAlive == 50 && !mawOpen) {
                mawOpen = true;
                // Accretion disk ring
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 5, 3, Math.sin(a) * 5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    mawHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.2f);
            }
            // Maw active with pull effect (50-170 ticks = 6 sec)
            else if (mawOpen && ticksAlive < 170) {
                // Rotate accretion disk
                float rot = ticksAlive * 0.05f;
                for (int i = 12; i < mawHandles.size(); i++) {
                    int diskIdx = i - 12;
                    double a = rot + (Math.PI * 2 / 20) * diskIdx;
                    mawHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * 5, 3, Math.sin(a) * 5));
                }
                // Pull particles converging inward
                if (ticksAlive % 6 == 0) {
                    for (int p = 0; p < 8; p++) {
                        double pa = Math.random() * Math.PI * 2;
                        double pr = 10 + Math.random() * 5;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(pa) * pr, 3, Math.sin(pa) * pr), 4, 0.8);
                    }
                    // Center void
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 3, 0), 8, 2.0);
                }
            }
            // Maw closes (tick 170)
            else if (ticksAlive == 170) {
                for (BlockDisplayHandle h : mawHandles) h.entity().remove();
                mawHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDevouring(plugin); }
    }

    // ================================================================
    // 96. STAR DEATH - DoG coils into sphere, pulses, then supernova
    // 40-block radius, distance-scaled damage, massive detonation
    // ================================================================
    public static class StarDeath extends BossAttack {
        private final List<BlockDisplayHandle> coilHandles = new ArrayList<>();
        private boolean detonated = false;

        public StarDeath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_death", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG segments coiling inward to sphere
            for (int i = 0; i < 20; i++) {
                double a = (Math.PI * 2 / 20) * i;
                double pitch = (i % 5) * (Math.PI / 5) - Math.PI / 2.5;
                Location loc = center.clone().add(
                    Math.cos(a) * Math.cos(pitch) * 6,
                    5 + Math.sin(pitch) * 4,
                    Math.sin(a) * Math.cos(pitch) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(4, 0);
                coilHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.15f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Coiling phase (0-60 ticks = 3 sec): segments compress inward
            if (ticksAlive < 60 && !detonated) {
                float compress = ticksAlive / 60.0f;
                float pulseFreq = 1 + compress * 4; // Increasing pulse frequency
                boolean pulse = (ticksAlive % Math.max(1, (int)(10 / pulseFreq))) == 0;

                for (int i = 0; i < coilHandles.size(); i++) {
                    double a = (Math.PI * 2 / coilHandles.size()) * i;
                    double pitch = (i % 5) * (Math.PI / 5) - Math.PI / 2.5;
                    double r = 6 * (1 - compress * 0.8);
                    coilHandles.get(i).entity().teleport(
                        center.clone().add(
                            Math.cos(a) * Math.cos(pitch) * r,
                            5 + Math.sin(pitch) * 4 * (1 - compress * 0.6),
                            Math.sin(a) * Math.cos(pitch) * r));
                }

                if (pulse) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 15, 4.0 - compress * 2);
                    DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 10, 3.0 - compress * 1.5);
                }
            }
            // DETONATION (tick 60): supernova burst
            else if (ticksAlive == 60 && !detonated) {
                detonated = true;
                // Remove coil
                for (BlockDisplayHandle h : coilHandles) h.entity().remove();
                coilHandles.clear();

                // Massive expanding explosion sphere
                Location blastCenter = center.clone().add(0, 5, 0);
                for (int ring = 0; ring < 8; ring++) {
                    double radius = ring * 5;
                    for (int i = 0; i < 16; i++) {
                        double a = (Math.PI * 2 / 16) * i;
                        DisplayBuilder.cyanDust(
                            blastCenter.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 8, 3.0);
                        DisplayBuilder.purpleDust(
                            blastCenter.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 6, 2.0);
                    }
                }

                // Visual shockwave ring expanding outward
                for (int i = 0; i < 32; i++) {
                    double a = (Math.PI * 2 / 32) * i;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        blastCenter.clone().add(Math.cos(a) * 3, 0, Math.sin(a) * 3), Material.SEA_LANTERN);
                    h.scale(2.0f, 3.0f, 2.0f).glow(240, 240, 255).interpolation(1, 0);
                    coilHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.15f);
            }
            // Shockwave expands (60-80 ticks)
            else if (detonated && ticksAlive > 60 && ticksAlive < 80) {
                float expandFrac = (ticksAlive - 60) / 20.0f;
                float shockRadius = 3 + expandFrac * 37;
                for (int i = 0; i < coilHandles.size(); i++) {
                    double a = (Math.PI * 2 / coilHandles.size()) * i;
                    coilHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * shockRadius, 5, Math.sin(a) * shockRadius));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 20, shockRadius);
                }
            }
            // Aftermath (tick 80)
            else if (ticksAlive == 80) {
                for (BlockDisplayHandle h : coilHandles) h.entity().remove();
                coilHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarDeath(plugin); }
    }

    // ================================================================
    // 97. RIFT WEB IMPRISON - Rift web at every player's feet
    // 5-block web per player, pulses damage, follows player
    // ================================================================
    public static class RiftWebImprison extends BossAttack {
        private final List<BlockDisplayHandle> webHandles = new ArrayList<>();
        private boolean webActive = false;
        private int pulsesCompleted = 0;

        public RiftWebImprison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_web_imprison", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Web buildup at feet
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 1, 0.05, Math.sin(a) * 1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.08f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                webHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Web formation (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !webActive) {
                float growth = ticksAlive / 30.0f;
                if (ticksAlive % 5 == 0) {
                    // Add new rift nodes as web spreads
                    for (int n = 0; n < 3; n++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = growth * 5;
                        Location nodeLoc = center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(nodeLoc, Material.DARK_PRISMARINE);
                        h.scale(0.25f, 0.1f, 0.25f).glow(0, 200, 255).interpolation(2, 0);
                        webHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Connecting lattice lines
                    for (int l = 0; l < 2; l++) {
                        double la = Math.random() * Math.PI * 2;
                        double lr = growth * 4;
                        Location lineLoc = center.clone().add(Math.cos(la) * lr, 0.05, Math.sin(la) * lr);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(lineLoc, Material.SEA_LANTERN);
                        h.scale(0.08f, 0.05f, 2.0f).glow(240, 240, 255).interpolation(2, 0);
                        webHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, growth * 5);
            }
            // Web snaps into full form (tick 30)
            else if (ticksAlive == 30 && !webActive) {
                webActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.6f);
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 20, 5.0);
            }
            // Web active with damage pulses (30-150 ticks = 6 sec, pulse every 30 ticks)
            else if (webActive && ticksAlive < 150) {
                if ((ticksAlive - 30) % 30 == 0 && pulsesCompleted < 4) {
                    pulsesCompleted++;
                    // Pulse visual: all nodes flash
                    for (int i = 0; i < webHandles.size(); i += 3) {
                        DisplayBuilder.cyanDust(webHandles.get(i).entity().getLocation(), 5, 0.8);
                    }
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 15, 5.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f + pulsesCompleted * 0.3f);
                }
            }
            // Web dissolves (tick 150)
            else if (ticksAlive == 150) {
                for (BlockDisplayHandle h : webHandles) h.entity().remove();
                webHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 15, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftWebImprison(plugin); }
    }

    // ================================================================
    // 98. GOD FORM - DoG fills sky, attacks from above for 10 sec
    // Segment slams, overhead lasers, cosmic flame bombardment
    // ================================================================
    public static class GodForm extends BossAttack {
        private final List<BlockDisplayHandle> skyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> attackHandles = new ArrayList<>();
        private boolean ascended = false;
        private boolean bombardmentActive = false;

        public GodForm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_form", AttackType.BOSS, 2), "dog");
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ascending body segments
            for (int seg = 0; seg < 12; seg++) {
                Location segLoc = center.clone().add(seg * 3 - 16, 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                h.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(4, 0);
                skyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ascent (0-80 ticks = 4 sec): segments rise and expand
            if (ticksAlive < 80 && !ascended) {
                float ascentFrac = ticksAlive / 80.0f;
                for (int seg = 0; seg < skyHandles.size(); seg++) {
                    float y = 3 + ascentFrac * 17;
                    float scale = 1.5f + ascentFrac * 2.5f;
                    skyHandles.get(seg).entity().teleport(
                        center.clone().add(seg * 3 - 16, y, 0));
                    BlockDisplay bd = (BlockDisplay) skyHandles.get(seg).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3 + ascentFrac * 15, 0), 12, 8.0);
                }
            }
            // God Form active (tick 80)
            else if (ticksAlive == 80 && !ascended) {
                ascended = true;
                bombardmentActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.2f);
            }
            // Bombardment (80-280 ticks = 10 sec)
            else if (bombardmentActive && ticksAlive < 280) {
                // Segment slam shockwaves (every 20 ticks)
                if (ticksAlive % 20 == 0) {
                    double slamX = (Math.random() - 0.5) * 20;
                    double slamZ = (Math.random() - 0.5) * 20;
                    Location slamLoc = center.clone().add(slamX, 0, slamZ);
                    // Shockwave ring
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2 / 8) * i;
                        for (int r = 1; r <= 5; r++) {
                            DisplayBuilder.cyanDust(
                                slamLoc.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 4, 0.5);
                        }
                    }
                    // Impact block
                    BlockDisplayHandle impact = displayBuilder.spawnBlock(slamLoc, Material.DARK_PRISMARINE);
                    impact.scale(2.0f, 0.3f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
                    attackHandles.add(impact);
                    spawnedEntities.add(impact.entity());
                    DisplayBuilder.playSound(slamLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
                }
                // Overhead laser beams (every 30 ticks)
                if (ticksAlive % 30 == 15) {
                    double laserAngle = Math.random() * Math.PI * 2;
                    for (int seg = 0; seg < 12; seg++) {
                        Location beamLoc = center.clone().add(
                            Math.cos(laserAngle) * seg, 18 - seg * 1.2, Math.sin(laserAngle) * seg);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                        h.scale(0.3f, 0.3f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                        attackHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Cosmic flame impacts (every 10 ticks)
                if (ticksAlive % 10 == 5) {
                    double flameX = (Math.random() - 0.5) * 18;
                    double flameZ = (Math.random() - 0.5) * 18;
                    Location flameLoc = center.clone().add(flameX, 0.2, flameZ);
                    BlockDisplayHandle flame = displayBuilder.spawnBlock(flameLoc, Material.AMETHYST_CLUSTER);
                    flame.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(1, 0);
                    attackHandles.add(flame);
                    spawnedEntities.add(flame.entity());
                    DisplayBuilder.purpleDust(flameLoc, 6, 3.0);
                }

                // Budget control: remove old attack displays
                while (attackHandles.size() > 120) {
                    attackHandles.get(0).entity().remove();
                    attackHandles.remove(0);
                }
            }
            // God Form ends (tick 280): descend
            else if (ticksAlive == 280) {
                bombardmentActive = false;
                for (BlockDisplayHandle h : attackHandles) h.entity().remove();
                attackHandles.clear();
                for (BlockDisplayHandle h : skyHandles) h.entity().remove();
                skyHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 40, 15.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodForm(plugin); }
    }

    // ================================================================
    // 99. SINGULARITY - Point of absolute blackness at arena center
    // Grows from 1 to 5 blocks, 15-block pull radius, 12 sec duration
    // ================================================================
    public static class Singularity extends BossAttack {
        private final List<BlockDisplayHandle> singularityHandles = new ArrayList<>();
        private float singularityRadius = 0.5f;
        private boolean singularityActive = false;

        public Singularity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("singularity", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial void point
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.POLISHED_BLACKSTONE);
            core.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(4, 0);
            singularityHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.15f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Growth phase (0-80 ticks = 4 sec): 1 to 5 block radius
            if (ticksAlive < 80 && !singularityActive) {
                singularityRadius = 0.5f + (ticksAlive / 80.0f) * 4.5f;

                // Scale core
                BlockDisplay core = (BlockDisplay) singularityHandles.get(0).entity();
                core.setTransformation(new Transformation(
                    new Vector3f(-singularityRadius, -singularityRadius, -singularityRadius),
                    new AxisAngle4f(ticksAlive * 0.02f, 0, 1, 0),
                    new Vector3f(singularityRadius * 2, singularityRadius * 2, singularityRadius * 2),
                    new AxisAngle4f(0, 0, 1, 0)));
                core.setInterpolationDelay(0);
                core.setInterpolationDuration(5);

                // Light-eating particles drawn inward
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = singularityRadius + 3 + Math.random() * 3;
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add(Math.cos(a) * r, 3, Math.sin(a) * r), 3, 0.5);
                    }
                }
            }
            // Singularity active (tick 80): begins pulling
            else if (ticksAlive == 80 && !singularityActive) {
                singularityActive = true;
                singularityRadius = 5.0f;

                // Outer event horizon ring
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 6, 3, Math.sin(a) * 6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.DARK_PRISMARINE);
                    h.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    singularityHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.2f);
            }
            // Active singularity (80-320 ticks = 12 sec): pull particles, event horizon spins
            else if (singularityActive && ticksAlive < 320) {
                // Rotate event horizon
                float rot = ticksAlive * 0.04f;
                for (int i = 1; i < singularityHandles.size(); i++) {
                    int ringIdx = i - 1;
                    double a = rot + (Math.PI * 2 / 16) * ringIdx;
                    singularityHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * 6, 3, Math.sin(a) * 6));
                }

                // Pull streaks converging
                if (ticksAlive % 6 == 0) {
                    for (int p = 0; p < 8; p++) {
                        double pa = Math.random() * Math.PI * 2;
                        double pr = 8 + Math.random() * 7;
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add(Math.cos(pa) * pr, 3 + (Math.random() - 0.5) * 3,
                                Math.sin(pa) * pr), 4, 0.8);
                    }
                    DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 6, 3.0);
                }

                // Core pulses
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 3, 0), 12, 5.0);
                }
            }
            // Singularity collapses (tick 320)
            else if (ticksAlive == 320) {
                for (BlockDisplayHandle h : singularityHandles) h.entity().remove();
                singularityHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 40, 12.0);
                DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Singularity(plugin); }
    }

    // ================================================================
    // 100. END OF ALL THINGS - Complete stillness then every attack at once
    // 2 sec silence, sky fracture, then simultaneous 6-sec combo
    // The ultimate attack in the DoG encounter
    // ================================================================
    public static class EndOfAllThings extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> comboHandles = new ArrayList<>();
        private boolean stillnessEnded = false;
        private boolean skyFractured = false;
        private boolean comboFired = false;

        public EndOfAllThings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_of_all_things", AttackType.BOSS, 2), "dog");
            config.setDamage(24.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            // Complete silence. Nothing spawns. This IS the telegraph.
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // TOTAL SILENCE (0-40 ticks = 2 sec): nothing happens
            // This is the only attack that telegraphs through absence

            // Sky fracture visual (tick 40)
            if (ticksAlive == 40 && !stillnessEnded) {
                stillnessEnded = true;
                // Massive sky crack
                for (int crack = 0; crack < 8; crack++) {
                    double angle = (Math.PI * 2 / 8) * crack;
                    for (int seg = 0; seg < 10; seg++) {
                        double r = seg * 4;
                        Location crackLoc = center.clone().add(
                            Math.cos(angle) * r, 20 + Math.sin(seg * 0.5) * 3, Math.sin(angle) * r);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(crackLoc, Material.SEA_LANTERN);
                        h.scale(0.3f, 0.3f, 4.0f).glow(240, 240, 255).interpolation(2, 0);
                        fractureHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Cosmic form suggestion (impossibly vast segment visible)
                for (int i = 0; i < 6; i++) {
                    Location vastLoc = center.clone().add(i * 8 - 20, 22, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(vastLoc, Material.AMETHYST_BLOCK);
                    h.scale(8.0f, 8.0f, 8.0f).glow(0, 200, 255).interpolation(3, 0);
                    fractureHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.15f);
            }
            // Sky fracture visible (40-60 ticks)
            else if (stillnessEnded && ticksAlive < 60 && !skyFractured) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 30, 20.0);
                }
            }
            // Fracture closes, combo begins (tick 60)
            else if (ticksAlive == 60 && !skyFractured) {
                skyFractured = true;
                // Remove sky visuals
                for (BlockDisplayHandle h : fractureHandles) h.entity().remove();
                fractureHandles.clear();
            }
            // ALL ATTACKS FIRE SIMULTANEOUSLY (tick 80): 6-second window
            else if (ticksAlive == 80 && !comboFired) {
                comboFired = true;

                // 1. Cosmic Annihilation radial beams (40 beams)
                Location beamOrigin = center.clone().add(0, 8, 0);
                for (int beam = 0; beam < 40; beam++) {
                    double yaw = (Math.PI * 2 / 40) * beam;
                    for (int seg = 2; seg <= 18; seg += 4) {
                        Location segLoc = beamOrigin.clone().add(
                            Math.cos(yaw) * seg, -seg * 0.3, Math.sin(yaw) * seg);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                        h.scale(0.15f, 0.15f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                        comboHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // 2. Three rift dash charge bodies
                for (int dash = 0; dash < 3; dash++) {
                    double dashAngle = (Math.PI * 2 / 3) * dash;
                    for (int seg = 0; seg < 4; seg++) {
                        Location segLoc = center.clone().add(
                            Math.cos(dashAngle) * (8 + seg * 2), 2, Math.sin(dashAngle) * (8 + seg * 2));
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                        h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                        comboHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // 3. Laser wall crossing
                for (int z = -12; z <= 12; z += 3) {
                    for (double y : new double[]{0, 1.5, 3, 4.5}) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(-18, y, z), Material.SEA_LANTERN);
                        h.scale(0.3f, 0.3f, 3.0f).glow(240, 240, 255).interpolation(1, 0);
                        comboHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // 4. Void field (ground corruption)
                for (int x = -8; x <= 8; x += 4) {
                    for (int z = -8; z <= 8; z += 4) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.1, z), Material.POLISHED_BLACKSTONE);
                        h.scale(4.0f, 0.2f, 4.0f).glow(128, 0, 255).interpolation(2, 0);
                        comboHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // 5. Pressure wave ring
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * 3, 1.5, Math.sin(a) * 3), Material.DARK_PRISMARINE);
                    h.scale(0.5f, 2.0f, 0.5f).glow(0, 200, 255).interpolation(1, 0);
                    comboHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 60, 15.0);
                DisplayBuilder.purpleDust(center.clone().add(0, 4, 0), 50, 12.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 4, 0), 40, 10.0);

                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.15f);
            }
            // Combo sweeps across arena (80-200 ticks = 6 sec)
            else if (comboFired && ticksAlive > 80 && ticksAlive < 200) {
                float progress = (ticksAlive - 80) / 120.0f;

                // Move rift dash bodies inward
                int dashStart = 200; // After beams (40 * 5 segs)
                for (int dash = 0; dash < 3; dash++) {
                    double dashAngle = (Math.PI * 2 / 3) * dash;
                    for (int seg = 0; seg < 4; seg++) {
                        int idx = dashStart + dash * 4 + seg;
                        if (idx < comboHandles.size()) {
                            double dist = (8 + seg * 2) * (1 - progress);
                            comboHandles.get(idx).entity().teleport(
                                center.clone().add(Math.cos(dashAngle) * dist, 2, Math.sin(dashAngle) * dist));
                        }
                    }
                }

                // Move laser wall across
                int laserStart = dashStart + 12;
                float wallX = -18 + progress * 36;
                for (int i = laserStart; i < laserStart + 36 && i < comboHandles.size(); i++) {
                    Location loc = comboHandles.get(i).entity().getLocation();
                    comboHandles.get(i).entity().teleport(
                        center.clone().add(wallX, loc.getY() - center.getY(), loc.getZ() - center.getZ()));
                }

                // Expand pressure wave ring
                int waveStart = comboHandles.size() - 24;
                float waveR = 3 + progress * 20;
                for (int i = 0; i < 24; i++) {
                    int idx = waveStart + i;
                    if (idx >= 0 && idx < comboHandles.size()) {
                        double a = (Math.PI * 2 / 24) * i;
                        comboHandles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(a) * waveR, 1.5, Math.sin(a) * waveR));
                    }
                }

                // Continuous particle chaos
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 15, 10.0);
                    DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), 10, 8.0);
                }
            }
            // End of All Things concludes (tick 200)
            else if (ticksAlive == 200) {
                for (BlockDisplayHandle h : comboHandles) h.entity().remove();
                comboHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 50, 20.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndOfAllThings(plugin); }
    }
}
