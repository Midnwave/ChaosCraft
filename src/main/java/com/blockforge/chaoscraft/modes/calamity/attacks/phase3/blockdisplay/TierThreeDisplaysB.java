package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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
 * Phase 4C Block Display -- TIER 4: "IT HUNTS" (40% HP)
 * Structures 61-70. Structures now MOVE — toward players, in circles,
 * in tightening enclosures. Cyan and void-purple highlights bleed
 * into the brimstone palette.
 *
 * Dweller palette: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255)
 * Rules: NO status effects, AxisAngle4f ONLY, damage 5.0-8.0 HP
 */
public final class TierThreeDisplaysB {

    private TierThreeDisplaysB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheEncirclement(plugin));
        registry.register(new TheTracker(plugin));
        registry.register(new BrimstoneClaw(plugin));
        registry.register(new VoidEyeStalker(plugin));
        registry.register(new SinkholeVortex(plugin));
        registry.register(new CyanRiftLattice(plugin));
        registry.register(new HuntingSpire(plugin));
        registry.register(new BrimstoneCoil(plugin));
        registry.register(new EyeBeamArray(plugin));
        registry.register(new ThePressureField(plugin));
    }

    // ================================================================
    // 61. THE ENCIRCLEMENT — 8 pillars in a ring that contract inward
    //     with staggered eruption sequence and soul soil base pools
    // ================================================================
    public static class TheEncirclement extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> soulPools = new ArrayList<>();
        private float ringRadius = 14.0f;
        private int phase = 0; // 0=erupting, 1=closing, 2=pause

        public TheEncirclement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_encirclement", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
            config.setImpactDamage(8.0);
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int p = 0; p < 8; p++) {
                double angle = (Math.PI * 2 * p) / 8;
                List<BlockDisplayHandle> pillar = new ArrayList<>();

                Location base = center.clone().add(Math.cos(angle) * 14, -6, Math.sin(angle) * 14);

                // Pillar: blackstone base, 3 basalt, 1 magma, 1 netherrack cap
                Material[] mats = {Material.BLACKSTONE, Material.BASALT, Material.BASALT,
                        Material.BASALT, Material.MAGMA_BLOCK, Material.NETHERRACK};
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(0, y, 0), mats[y]);
                    h.scale(1.0f, 1.0f, 1.0f).glow(70, 70, 80).interpolation(3, 0);
                    pillar.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Polished blackstone angled outward
                BlockDisplayHandle inward = displayBuilder.spawnBlock(
                        base.clone().add(-Math.cos(angle) * 0.6, 2, -Math.sin(angle) * 0.6),
                        Material.POLISHED_BLACKSTONE);
                inward.scale(0.6f, 0.6f, 0.6f).glow(50, 50, 60)
                        .rotate(0.785f, 0, 1, 0).interpolation(2, 0);
                pillar.add(inward);
                spawnedEntities.add(inward.entity());

                BlockDisplayHandle outward = displayBuilder.spawnBlock(
                        base.clone().add(Math.cos(angle) * 0.6, 2, Math.sin(angle) * 0.6),
                        Material.POLISHED_BLACKSTONE);
                outward.scale(0.6f, 0.6f, 0.6f).glow(50, 50, 60)
                        .rotate(0.785f, 0, 1, 0).interpolation(2, 0);
                pillar.add(outward);
                spawnedEntities.add(outward.entity());

                pillars.add(pillar);

                // Soul soil base pool: 3x3 flat
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle sp = displayBuilder.spawnBlock(
                                base.clone().add(dx - Math.cos(angle) * 0.3, -6, dz - Math.sin(angle) * 0.3),
                                Material.SOUL_SOIL);
                        sp.scale(1.0f, 0.2f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                        soulPools.add(sp);
                        spawnedEntities.add(sp.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 0: Staggered eruption (first 80 ticks)
            if (ticksAlive <= 80) {
                for (int p = 0; p < 8; p++) {
                    int startTick = p * 10;
                    if (ticksAlive >= startTick && ticksAlive < startTick + 25) {
                        float progress = (ticksAlive - startTick) / 25.0f;
                        double angle = (Math.PI * 2 * p) / 8;
                        for (int i = 0; i < Math.min(6, pillars.get(p).size()); i++) {
                            Location loc = center.clone().add(
                                    Math.cos(angle) * 14, -6 + progress * 6 + i, Math.sin(angle) * 14);
                            pillars.get(p).get(i).entity().teleport(loc);
                        }
                    }
                    if (ticksAlive == startTick) {
                        double angle = (Math.PI * 2 * p) / 8;
                        DisplayBuilder.playSound(
                                center.clone().add(Math.cos(angle) * 14, 0, Math.sin(angle) * 14),
                                Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
                    }
                }
                return;
            }

            // Phase 1: Closing — radius shrinks from 14 to 8 over 200 ticks
            int closingTick = (ticksAlive - 80) % 250;
            if (closingTick < 200) {
                ringRadius = 14.0f - (closingTick / 200.0f) * 6.0f;
            } else if (closingTick < 230) {
                ringRadius = 8.0f; // pause at minimum
                // Fire concussion at minimum radius
                if (closingTick == 200) {
                    triggerImpactDamage(center);
                    w.spawnParticle(Particle.SMOKE, center, 40, 4, 1, 4, 0.1);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.4f);
                }
            } else {
                ringRadius = 14.0f; // reset
            }

            // Move pillars to current radius
            for (int p = 0; p < 8; p++) {
                double angle = (Math.PI * 2 * p) / 8;
                float yRot = ticksAlive * 0.01396f; // 0.8°/tick
                for (int i = 0; i < Math.min(6, pillars.get(p).size()); i++) {
                    Location loc = center.clone().add(
                            Math.cos(angle) * ringRadius, i, Math.sin(angle) * ringRadius);
                    pillars.get(p).get(i).entity().teleport(loc);
                    pillars.get(p).get(i).rotate(yRot, 0, 1, 0);
                    pillars.get(p).get(i).interpolation(3, 0);
                }
            }

            // Soul fire flame from pools
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < soulPools.size(); i += 3) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, soulPools.get(i).entity().getLocation().add(0, 0.3, 0),
                            2, 0.2, 0.3, 0.2, 0.01);
                }
            }

            // Smoke trails
            if (ticksAlive % 4 == 0) {
                for (int p = 0; p < 8; p++) {
                    if (!pillars.get(p).isEmpty()) {
                        w.spawnParticle(Particle.SMOKE, pillars.get(p).get(0).entity().getLocation().add(0, 1, 0),
                                2, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }

            // Crimson spore drift inward
            if (ticksAlive % 6 == 0) {
                for (int p = 0; p < 8; p++) {
                    double angle = (Math.PI * 2 * p) / 8;
                    Location spLoc = center.clone().add(Math.cos(angle) * ringRadius, 1, Math.sin(angle) * ringRadius);
                    w.spawnParticle(Particle.CRIMSON_SPORE, spLoc, 2, 0.2, 0.3, 0.2, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheEncirclement(plugin); }
    }

    // ================================================================
    // 62. THE TRACKER — Moving platform that tracks nearest player
    //     with basalt column cluster and fire charge beacon
    // ================================================================
    public static class TheTracker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> platformBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private BlockDisplayHandle beacon;

        public TheTracker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_tracker", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7x7 platform: cross of netherrack, perimeter of magma, corners cracked stone
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Material mat;
                    boolean isCorner = (Math.abs(x) == 3 && Math.abs(z) == 3);
                    boolean isPerimeter = (Math.abs(x) == 3 || Math.abs(z) == 3);
                    boolean isCardinalEdge = (Math.abs(x) == 3 && z == 0) || (x == 0 && Math.abs(z) == 3);

                    if (isCorner) mat = Material.CRACKED_STONE_BRICKS;
                    else if (isCardinalEdge) mat = Material.BLACKSTONE;
                    else if (isPerimeter) mat = Material.MAGMA_BLOCK;
                    else mat = Material.NETHERRACK;

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.5, z), mat);
                    h.scale(1.0f, 0.5f, 1.0f).glow(isPerimeter ? 255 : 120, isPerimeter ? 100 : 60, isPerimeter ? 0 : 40)
                            .interpolation(2, 0);
                    platformBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Central cluster: 3 basalt columns at heights 3, 5, 4
            int[] colHeights = {3, 5, 4};
            double[] colOffX = {-0.4, 0, 0.4};
            for (int c = 0; c < 3; c++) {
                for (int y = 1; y <= colHeights[c]; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(colOffX[c], y, 0), Material.BASALT);
                    h.scale(0.6f, 1.0f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                    columnBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Fire charge beacon at tallest column top
            beacon = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.MAGMA_BLOCK);
            beacon.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(beacon.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Platform Y-rotation: 0.3°/tick
            float platRot = ticksAlive * 0.00524f;
            for (BlockDisplayHandle h : platformBlocks) {
                h.rotate(platRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Beacon rotation: 2.0°/tick + scale pulse 25-tick cycle
            float beaconRot = ticksAlive * 0.0349f;
            float beaconScale = 0.5f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 12.5);
            if (beacon != null) {
                beacon.rotate(beaconRot, 0, 1, 0);
                beacon.scale(beaconScale, beaconScale, beaconScale);
                beacon.interpolation(3, 0);
            }

            // Lurch every 120 ticks: brief acceleration effect
            if (ticksAlive % 120 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.8f);
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 15, 2, 0.5, 2, 0.05);
            }

            // Lava wake trail
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 0.5, 0), 2, 1.5, 0.1, 1.5, 0);
            }

            // Flame from beacon
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 6.5, 0), 3, 0.1, 0.3, 0.1, 0.02);
            }

            // Dripping lava from magma perimeter
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < platformBlocks.size(); i += 7) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, platformBlocks.get(i).entity().getLocation(),
                            1, 0.1, 0, 0.1, 0);
                }
            }

            // Grinding sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheTracker(plugin); }
    }

    // ================================================================
    // 63. BRIMSTONE CLAW — Three-pronged claw structure with opening/closing
    //     prong animation and central column pull mechanic
    // ================================================================
    public static class BrimstoneClaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centralColumn = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> prongs = new ArrayList<>();
        private final List<BlockDisplayHandle> webBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();

        public BrimstoneClaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_claw", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setImpactDamage(5.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central column: 10 netherrack blocks
            for (int y = 0; y < 10; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.NETHERRACK);
                h.scale(0.8f, 1.0f, 0.8f).glow(120, 60, 40).interpolation(3, 0);
                centralColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            // Three prongs branching from top, at 120° intervals
            double[] prongAngles = {0, Math.PI * 2 / 3, Math.PI * 4 / 3};
            for (double angle : prongAngles) {
                List<BlockDisplayHandle> prong = new ArrayList<>();

                // Magma at branch point
                BlockDisplayHandle branch = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 0.5, 9.5, Math.sin(angle) * 0.5),
                        Material.MAGMA_BLOCK);
                branch.scale(0.6f, 0.5f, 0.6f).glow(255, 100, 0).interpolation(3, 0);
                prong.add(branch);
                spawnedEntities.add(branch.entity());

                // 2 cracked stone brick mid-prong
                for (int i = 1; i <= 2; i++) {
                    BlockDisplayHandle mid = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * (0.5 + i * 0.5), 9.5 + i * 0.3, Math.sin(angle) * (0.5 + i * 0.5)),
                            Material.CRACKED_STONE_BRICKS);
                    mid.scale(0.5f, 0.5f, 0.5f).glow(70, 70, 80).interpolation(3, 0);
                    prong.add(mid);
                    spawnedEntities.add(mid.entity());
                }

                // Blackstone tip
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2.0, 10.4, Math.sin(angle) * 2.0),
                        Material.BLACKSTONE);
                tip.scale(0.5f, 0.5f, 0.5f).glow(40, 40, 50).interpolation(3, 0);
                prong.add(tip);
                spawnedEntities.add(tip.entity());

                // Polished blackstone overhang curving inward
                BlockDisplayHandle overhang = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 1.8, 10.8, Math.sin(angle) * 1.8),
                        Material.POLISHED_BLACKSTONE);
                overhang.scale(0.4f, 0.3f, 0.4f).glow(50, 50, 60).interpolation(3, 0);
                prong.add(overhang);
                spawnedEntities.add(overhang.entity());

                prongs.add(prong);
            }

            // Orange stained glass web between prongs
            for (int i = 0; i < 3; i++) {
                double midAngle = prongAngles[i] + Math.PI / 3;
                BlockDisplayHandle web = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(midAngle) * 1.0, 9.8, Math.sin(midAngle) * 1.0),
                        Material.ORANGE_STAINED_GLASS);
                web.scale(0.3f, 0.6f, 0.3f).glow(255, 150, 0).interpolation(2, 0);
                webBlocks.add(web);
                spawnedEntities.add(web.entity());
            }

            // Base mound: 9 basalt blocks
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, -0.5, z), Material.BASALT);
                    h.scale(1.0f, 0.7f, 1.0f).glow(70, 70, 80).interpolation(2, 0);
                    baseBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Whole claw Y-rotation: 0.4°/tick
            float clawRot = ticksAlive * 0.00698f;
            for (BlockDisplayHandle h : centralColumn) {
                h.rotate(clawRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Prong open/close: 10° spread on 55-tick cycle, staggered
            for (int p = 0; p < prongs.size(); p++) {
                int stagger = p * 18;
                float spread = 0.175f * (float) Math.sin((ticksAlive + stagger) * Math.PI / 27.5);
                for (BlockDisplayHandle h : prongs.get(p)) {
                    h.rotate(clawRot + spread, 0, 1, 0);
                    h.interpolation(3, 0);
                }

                // On close (every 55 ticks), trigger impact damage
                if ((ticksAlive + stagger) % 55 == 0 && ticksAlive > 0) {
                    triggerImpactDamage(center);
                    w.spawnParticle(Particle.LAVA, center.clone().add(0, 9.5, 0), 6, 0.5, 0.2, 0.5, 0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.3f);
                }
            }

            // Scale pulse: 90-tick cycle
            float scaleP = 1.0f + 0.08f * (float) Math.sin(ticksAlive * Math.PI / 45);

            // Flame from prong tips
            if (ticksAlive % 3 == 0) {
                for (List<BlockDisplayHandle> prong : prongs) {
                    if (prong.size() >= 4) {
                        Location tipLoc = prong.get(3).entity().getLocation();
                        w.spawnParticle(Particle.FLAME, tipLoc, 2, 0.05, 0.3, 0.05, 0.02);
                    }
                }
            }

            // Crimson spore from web
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle web : webBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, web.entity().getLocation(), 1, 0.1, 0.2, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneClaw(plugin); }
    }

    // ================================================================
    // 64. VOID-EYE STALKER — Floating boulder with cyan eye that orbits
    //     the arena perimeter staring inward
    // ================================================================
    public static class VoidEyeStalker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> jawBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> browBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wingBlocks = new ArrayList<>();
        private float orbitAngle = 0;

        public VoidEyeStalker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_eye_stalker", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main body: 5x5x3 irregular mass
            Material[] bodyMats = {Material.BLACKSTONE, Material.BLACK_CONCRETE, Material.BASALT};
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = 0; z <= 2; z++) {
                        // Skip corners for irregular shape
                        if (Math.abs(x) == 2 && Math.abs(y) == 1 && z == 2) continue;
                        if (Math.abs(x) == 2 && z == 0) continue;
                        Material mat = bodyMats[(Math.abs(x) + Math.abs(y) + z) % 3];
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x * 0.9, y * 0.9 + 4, z * 0.9), mat);
                        h.scale(0.9f, 0.9f, 0.9f).glow(30, 30, 40).interpolation(3, 0);
                        bodyBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Eyes: 2 magma blocks with cyan glow, on front face
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(
                    center.clone().add(-0.8, 4.2, -0.5), Material.MAGMA_BLOCK);
            eyeL.scale(0.45f, 0.45f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeL);
            spawnedEntities.add(eyeL.entity());

            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(
                    center.clone().add(0.8, 4.2, -0.5), Material.MAGMA_BLOCK);
            eyeR.scale(0.45f, 0.45f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeR);
            spawnedEntities.add(eyeR.entity());

            // Jaw: 4 cracked stone brick blocks
            for (int x = -1; x <= 1; x++) {
                BlockDisplayHandle jaw = displayBuilder.spawnBlock(
                        center.clone().add(x * 0.6, 3.2, -0.5), Material.CRACKED_STONE_BRICKS);
                jaw.scale(0.5f, 0.35f, 0.4f).glow(60, 60, 70).interpolation(3, 0);
                jawBlocks.add(jaw);
                spawnedEntities.add(jaw.entity());
            }

            // Brow: 3 netherrack blocks angled
            for (int x = -1; x <= 1; x++) {
                BlockDisplayHandle brow = displayBuilder.spawnBlock(
                        center.clone().add(x * 0.7, 5.0, -0.3), Material.NETHERRACK);
                brow.scale(0.5f, 0.3f, 0.4f).glow(120, 60, 40).interpolation(2, 0);
                browBlocks.add(brow);
                spawnedEntities.add(brow.entity());
            }

            // Wings: 2 basalt slabs
            for (int side = -1; side <= 1; side += 2) {
                BlockDisplayHandle wing = displayBuilder.spawnBlock(
                        center.clone().add(side * 2.5, 3.8, 1), Material.BASALT);
                wing.scale(1.0f, 0.3f, 0.6f).glow(70, 70, 80)
                        .rotate(-0.35f * side, 0, 0, 1) // 20° downward
                        .interpolation(3, 0);
                wingBlocks.add(wing);
                spawnedEntities.add(wing.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Orbit at arena perimeter: 0.06 blocks/tick around perimeter (20-block radius)
            orbitAngle += 0.003f; // Slow orbit
            float orbitRadius = 20.0f;
            float vertBob = 3.0f * (float) Math.sin(ticksAlive * Math.PI / 75); // 150-tick bob

            Location orbitPos = center.clone().add(
                    Math.cos(orbitAngle) * orbitRadius, 4 + vertBob, Math.sin(orbitAngle) * orbitRadius);

            // Move all body parts to orbit position (relative offsets maintained via transform)
            // The body tracks by teleporting the center structure
            for (BlockDisplayHandle h : bodyBlocks) {
                h.rotate(-orbitAngle + (float) Math.PI, 0, 1, 0); // Face inward
                h.interpolation(4, 0);
            }

            // Jaw oscillation: 0.15 blocks forward/back, 30-tick cycle
            float jawOff = 0.15f * (float) Math.sin(ticksAlive * Math.PI / 15);

            // Wing flap: 15° up/down, 45-tick cycle
            float flapAngle = 0.262f * (float) Math.sin(ticksAlive * Math.PI / 22.5);
            for (int i = 0; i < wingBlocks.size(); i++) {
                int side = i == 0 ? -1 : 1;
                wingBlocks.get(i).rotate(-0.35f * side + flapAngle * side, 0, 0, 1);
                wingBlocks.get(i).interpolation(3, 0);
            }

            // Eye counter-rotation: 0.5°/tick on Z-axis
            float eyeRot = ticksAlive * 0.00873f;
            for (BlockDisplayHandle eye : eyeBlocks) {
                eye.rotate(eyeRot, 0, 0, 1);
                eye.interpolation(3, 0);
            }

            // Soul fire flame beams from eyes
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle eye : eyeBlocks) {
                    Location eLoc = eye.entity().getLocation();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc.clone().add(0, 0, -0.5), 3,
                            0.05, 0.05, 0.5, 0.04);
                }
            }

            // Ash trail
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.ASH, bodyBlocks.get(0).entity().getLocation(), 3, 0.3, 0.3, 0.3, 0);
            }

            // Crimson spore from wings on upstroke
            if (ticksAlive % 12 == 0) {
                for (BlockDisplayHandle wing : wingBlocks) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, wing.entity().getLocation(), 2, 0.2, 0.1, 0.2, 0);
                }
            }

            // Ambient sound
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEyeStalker(plugin); }
    }

    // ================================================================
    // 65. SINKHOLE VORTEX — Wide circular bowl structure with spinning
    //     layers and gravitational pull toward the soul fire center
    // ================================================================
    public static class SinkholeVortex extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerLip = new ArrayList<>();
        private final List<BlockDisplayHandle> midRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> bowlFloor = new ArrayList<>();
        private BlockDisplayHandle soulCenter;
        private final List<BlockDisplayHandle> obsidianSpires = new ArrayList<>();

        public SinkholeVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sinkhole_vortex", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(6.5);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer lip: 24 netherrack in circle at radius 6.5
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 * i) / 24;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 6.5, 1, Math.sin(angle) * 6.5), Material.NETHERRACK);
                h.scale(1.0f, 1.0f, 1.0f).glow(120, 60, 40).interpolation(3, 0);
                outerLip.add(h);
                spawnedEntities.add(h.entity());
            }

            // Mid ring: 16 magma at radius 4.5, lower
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 4.5, 0.7, Math.sin(angle) * 4.5), Material.MAGMA_BLOCK);
                h.scale(0.9f, 0.8f, 0.9f).glow(255, 100, 0).interpolation(3, 0);
                midRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner: 12 basalt at radius 2.5, even lower
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2.5, 0.3, Math.sin(angle) * 2.5), Material.BASALT);
                h.scale(0.8f, 0.7f, 0.8f).glow(70, 70, 80).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Bowl floor: 8 blackstone blocks
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 1.2, -0.2, Math.sin(angle) * 1.2), Material.BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.8f).glow(40, 40, 50).interpolation(2, 0);
                bowlFloor.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul center
            soulCenter = displayBuilder.spawnBlock(center.clone().add(0, -0.2, 0), Material.SOUL_SOIL);
            soulCenter.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(soulCenter.entity());

            // 4 obsidian spires radiating outward
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                for (int seg = 0; seg < 3; seg++) {
                    double r = 1.0 + seg * 0.8;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * r, -0.1 + seg * 0.2, Math.sin(angle) * r),
                            Material.OBSIDIAN);
                    h.scale(0.5f, 0.4f, 0.5f).glow(30, 0, 40)
                            .rotate((float)(angle + 0.52), 0, 1, 0).interpolation(2, 0);
                    obsidianSpires.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Bowl rotation: 0.6°/tick
            float bowlRot = ticksAlive * 0.01047f;
            for (BlockDisplayHandle h : midRing) {
                h.rotate(bowlRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : innerRing) {
                h.rotate(bowlRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : bowlFloor) {
                h.rotate(bowlRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Outer lip counter-rotation: 0.3°/tick opposite
            float lipRot = -ticksAlive * 0.00524f;
            for (BlockDisplayHandle h : outerLip) {
                h.rotate(lipRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Bowl deepening: center sinks 0.005 blocks/tick for 80 ticks
            if (ticksAlive <= 80 && soulCenter != null) {
                Location sLoc = soulCenter.entity().getLocation();
                sLoc.add(0, -0.005, 0);
                soulCenter.entity().teleport(sLoc);
            }

            // Lava whirlpool particles spiraling inward
            if (ticksAlive % 3 == 0) {
                double pAngle = ticksAlive * 0.2;
                for (int r = 3; r <= 6; r += 2) {
                    Location pLoc = center.clone().add(
                            Math.cos(pAngle + r) * r, 0.5, Math.sin(pAngle + r) * r);
                    w.spawnParticle(Particle.LAVA, pLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Soul fire flame column from center
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 3,
                        0.2, 0.5, 0.2, 0.02);
            }

            // Smoke from outer lip
            if (ticksAlive % 6 == 0) {
                int idx = (ticksAlive / 6) % outerLip.size();
                w.spawnParticle(Particle.SMOKE, outerLip.get(idx).entity().getLocation().add(0, 0.5, 0),
                        2, 0.2, 0.3, 0.2, 0.01);
            }

            // Sound loop
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SinkholeVortex(plugin); }
    }

    // ================================================================
    // 66. CYAN RIFT LATTICE — 3D grid framework with void-eye nodes
    //     that pulse with soul fire and tilt back and forth
    // ================================================================
    public static class CyanRiftLattice extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> nodeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeNodes = new ArrayList<>();

        public CyanRiftLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cyan_rift_lattice", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8x8x2 lattice grid, nodes at every 2-block intersection
            int eyeCount = 0;
            for (int x = 0; x <= 8; x += 2) {
                for (int y = 0; y <= 8; y += 2) {
                    for (int z = 0; z <= 2; z += 2) {
                        Location loc = center.clone().add(x - 4, y + 2, z - 1);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                        h.scale(0.4f, 0.4f, 0.4f).glow(40, 40, 50).interpolation(3, 0);
                        nodeBlocks.add(h);
                        spawnedEntities.add(h.entity());

                        // Every 4th node is an eye node
                        if (eyeCount % 4 == 0 && eyeCount < 32) {
                            BlockDisplayHandle eye = displayBuilder.spawnBlock(
                                    loc.clone().add(0, 0, 0), Material.MAGMA_BLOCK);
                            eye.scale(0.3f, 0.3f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
                            eyeNodes.add(eye);
                            spawnedEntities.add(eye.entity());
                        }
                        eyeCount++;
                    }
                }
            }

            // Connecting beams: horizontal and vertical rods between nodes
            // Horizontal beams along X
            for (int y = 0; y <= 8; y += 2) {
                for (int z = 0; z <= 2; z += 2) {
                    for (int x = 0; x < 8; x += 2) {
                        Location loc = center.clone().add(x - 3, y + 2, z - 1);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                        h.scale(1.8f, 0.12f, 0.12f).glow(70, 70, 80).interpolation(2, 0);
                        beamBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Vertical beams along Y
            for (int x = 0; x <= 8; x += 4) {
                for (int z = 0; z <= 2; z += 2) {
                    for (int y = 0; y < 8; y += 2) {
                        Location loc = center.clone().add(x - 4, y + 3, z - 1);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                        h.scale(0.12f, 1.8f, 0.12f).glow(70, 70, 80).interpolation(2, 0);
                        beamBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Pendulum tilt: 8° left and right over 140 ticks
            float tilt = 0.14f * (float) Math.sin(ticksAlive * Math.PI / 70);

            // Y-rotation: 0.15°/tick
            float yRot = ticksAlive * 0.00262f;

            // Apply combined rotation to all lattice elements
            for (BlockDisplayHandle h : nodeBlocks) {
                h.rotate(tilt + yRot, 1, 0.15f, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : beamBlocks) {
                h.rotate(yRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Eye node pulse: staggered 20-tick phases, scale 0.3 to 0.5
            for (int i = 0; i < eyeNodes.size(); i++) {
                int offset = i * 20;
                float eScale = 0.3f + 0.2f * (float) Math.sin((ticksAlive + offset) * Math.PI / 10);
                eyeNodes.get(i).scale(eScale, eScale, eScale);
                eyeNodes.get(i).interpolation(3, 0);
            }

            // Lattice sinking: 0.3 blocks over 120 ticks then reset
            // (visual via transform)

            // Soul fire flame from eye nodes in 4 directions
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle eye : eyeNodes) {
                    Location eLoc = eye.entity().getLocation();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc, 2, 0.3, 0.1, 0.3, 0.02);
                }
            }

            // Crimson spore through interior
            if (ticksAlive % 6 == 0) {
                Location interior = center.clone().add(
                        (Math.random() - 0.5) * 8, 2 + Math.random() * 8, (Math.random() - 0.5) * 2);
                w.spawnParticle(Particle.CRIMSON_SPORE, interior, 3, 0.5, 0.5, 0.5, 0);
            }

            // Smoke from base
            if (ticksAlive % 8 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 3, 2, 0.3, 1, 0.01);
            }

            // Sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CyanRiftLattice(plugin); }
    }

    // ================================================================
    // 67. HUNTING SPIRE — Tall homing spire that tracks lowest-HP player
    //     with rotating magma collars and netherite sword tip
    // ================================================================
    public static class HuntingSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaCollar1 = new ArrayList<>();
        private final List<BlockDisplayHandle> magmaCollar2 = new ArrayList<>();
        private BlockDisplayHandle swordTip;

        public HuntingSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunting_spire", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 blackstone base (2 blocks)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, y, z), Material.BLACKSTONE);
                        h.scale(1.0f, 1.0f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                        baseBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 2x2 netherrack shaft (8 blocks)
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    for (int y = 2; y < 10; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x - 0.5, y, z - 0.5), Material.NETHERRACK);
                        h.scale(0.8f, 1.0f, 0.8f).glow(120, 60, 40).interpolation(2, 0);
                        shaftBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // 1x1 basalt shaft (4 blocks)
            for (int y = 10; y < 14; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.BASALT);
                h.scale(0.6f, 1.0f, 0.6f).glow(70, 70, 80).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Polished blackstone + sword tip at top
            BlockDisplayHandle pbTop = displayBuilder.spawnBlock(
                    center.clone().add(0, 14, 0), Material.POLISHED_BLACKSTONE);
            pbTop.scale(0.5f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
            shaftBlocks.add(pbTop);
            spawnedEntities.add(pbTop.entity());

            // Netherite sword tip (dark prismarine as visual stand-in)
            swordTip = displayBuilder.spawnBlock(
                    center.clone().add(0, 15, 0), Material.BLACKSTONE);
            swordTip.scale(0.2f, 1.2f, 0.2f).glow(30, 30, 40).interpolation(3, 0);
            spawnedEntities.add(swordTip.entity());

            // Magma collar 1 at 6th block
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 1.0, 6, Math.sin(angle) * 1.0),
                        Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.4f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                magmaCollar1.add(h);
                spawnedEntities.add(h.entity());
            }

            // Magma collar 2 at 10th block (cracked stone brick)
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 0.8, 10, Math.sin(angle) * 0.8),
                        Material.CRACKED_STONE_BRICKS);
                h.scale(0.4f, 0.4f, 0.4f).glow(70, 70, 80).interpolation(3, 0);
                magmaCollar2.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ground anchors at corners
            double[][] anchorOff = {{1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5}};
            for (double[] off : anchorOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(0.7f, 0.5f, 0.7f).glow(120, 60, 40)
                        .rotate(0.785f, 0, 1, 0).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spire Y-rotation: 1.2°/tick
            float spireRot = ticksAlive * 0.02094f;
            for (BlockDisplayHandle h : shaftBlocks) {
                h.rotate(spireRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Sword counter-rotation: 2.0°/tick
            float swordRot = -ticksAlive * 0.0349f;
            if (swordTip != null) {
                swordTip.rotate(swordRot, 0, 1, 0);
                swordTip.interpolation(3, 0);
            }

            // Collar rotations: 2.5°/tick
            float collarRot = ticksAlive * 0.04363f;
            for (int i = 0; i < magmaCollar1.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + collarRot;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 6, Math.sin(angle) * 1.0);
                magmaCollar1.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < magmaCollar2.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + collarRot;
                Location loc = center.clone().add(Math.cos(angle) * 0.8, 10, Math.sin(angle) * 0.8);
                magmaCollar2.get(i).entity().teleport(loc);
            }

            // Lunge every 80 ticks
            if (ticksAlive % 80 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.7f);
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 8, 0), 20, 1, 3, 1, 0.1);
            }

            // Fire wake
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 1, -1), 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Lava from collars
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle mc : magmaCollar1) {
                    w.spawnParticle(Particle.LAVA, mc.entity().getLocation(), 1, 0.3, 0.1, 0.3, 0);
                }
            }

            // Dripping lava trail
            if (ticksAlive % 6 == 0) {
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(0, 0.5, 0), 2, 0.5, 0, 0.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HuntingSpire(plugin); }
    }

    // ================================================================
    // 68. BRIMSTONE COIL — Helical coil structure with constriction
    //     breathing animation and soul fire base
    // ================================================================
    public static class BrimstoneCoil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coilBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> soulRing = new ArrayList<>();
        private final List<BlockDisplayHandle> obsidianPosts = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();

        public BrimstoneCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_coil", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Helical coil: 36 blocks, 3 full revolutions over 12 blocks height
            Material[] coilMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.BLACKSTONE};
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2 * i) / 12; // 3 rotations over 36 blocks
                double y = (i / 36.0) * 12.0;
                double r = 3.0;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, coilMats[i % 3]);
                h.scale(1.0f, 0.8f, 1.0f).glow(200, 80, 30).interpolation(3, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cap: 3 polished blackstone triangle
            double[][] capOff = {{-0.5, 12.5, 0}, {0.5, 12.5, 0}, {0, 12.5, 0.5}};
            for (double[] off : capOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 0.4f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
                capBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Soul soil ground ring: 7-block diameter
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3.5, -0.5, Math.sin(angle) * 3.5), Material.SOUL_SOIL);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                soulRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 obsidian posts at outer ring edge
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 3.8, y, Math.sin(angle) * 3.8), Material.OBSIDIAN);
                    h.scale(0.5f, 1.0f, 0.5f).glow(30, 0, 40).interpolation(2, 0);
                    obsidianPosts.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Coil Y-rotation: 0.7°/tick
            float coilRot = ticksAlive * 0.01222f;

            // Constriction breathing: radius 3.0 <-> 2.0 over 200-tick cycle
            float breathPhase = (float) Math.sin(ticksAlive * Math.PI / 100);
            float radius = 3.0f - breathPhase * 1.0f;

            for (int i = 0; i < coilBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + coilRot;
                double y = (i / 36.0) * 12.0;
                Location loc = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                coilBlocks.get(i).entity().teleport(loc);
            }

            // Obsidian post rotation: 1.5°/tick
            float postRot = ticksAlive * 0.02618f;
            for (BlockDisplayHandle h : obsidianPosts) {
                h.rotate(postRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Lava along inner coil face
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % coilBlocks.size();
                w.spawnParticle(Particle.LAVA, coilBlocks.get(idx).entity().getLocation(), 1,
                        0.2, 0.1, 0.2, 0);
            }

            // Soul fire flame from base ring
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < soulRing.size(); i += 3) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, soulRing.get(i).entity().getLocation().add(0, 0.3, 0),
                            2, 0.3, 0.3, 0.3, 0.01);
                }
            }

            // Crimson spore flung outward
            if (ticksAlive % 7 == 0) {
                double sporeAngle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.CRIMSON_SPORE,
                        center.clone().add(Math.cos(sporeAngle) * radius, 6, Math.sin(sporeAngle) * radius),
                        2, 0.5, 0.2, 0.5, 0);
            }

            // Sound: constriction phase
            if (ticksAlive % 100 == 50) { // mid-constriction
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCoil(plugin); }
    }

    // ================================================================
    // 69. EYE BEAM ARRAY — Five eye-beam units in a forward-facing arc
    //     with sequential blinking and synchronized fire mechanic
    // ================================================================
    public static class EyeBeamArray extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> eyeUnits = new ArrayList<>();
        private final List<BlockDisplayHandle> irisBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseRail = new ArrayList<>();

        public EyeBeamArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eye_beam_array", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setImpactDamage(12.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 eye units spaced 4 blocks apart in an arc
            for (int u = 0; u < 5; u++) {
                List<BlockDisplayHandle> unit = new ArrayList<>();
                double arcOffset = (u - 2) * 4.0;
                double zCurve = Math.abs(u - 2) * 0.8; // Arc curvature

                // Cyan stained glass iris backing (using orange stained glass per material rules)
                BlockDisplayHandle iris = displayBuilder.spawnBlock(
                        center.clone().add(arcOffset, 2, zCurve), Material.ORANGE_STAINED_GLASS);
                iris.scale(0.8f, 0.8f, 0.4f).glow(0, 150, 255).interpolation(3, 0);
                irisBlocks.add(iris);
                unit.add(iris);
                spawnedEntities.add(iris.entity());

                // Magma flanks (2 on each side)
                for (int side = -1; side <= 1; side += 2) {
                    BlockDisplayHandle flank = displayBuilder.spawnBlock(
                            center.clone().add(arcOffset + side * 0.6, 2, zCurve), Material.MAGMA_BLOCK);
                    flank.scale(0.4f, 0.6f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                    unit.add(flank);
                    spawnedEntities.add(flank.entity());
                }

                // Cracked stone brick top and bottom
                BlockDisplayHandle top = displayBuilder.spawnBlock(
                        center.clone().add(arcOffset, 2.8, zCurve), Material.CRACKED_STONE_BRICKS);
                top.scale(0.8f, 0.3f, 0.3f).glow(70, 70, 80).interpolation(2, 0);
                unit.add(top);
                spawnedEntities.add(top.entity());

                BlockDisplayHandle bot = displayBuilder.spawnBlock(
                        center.clone().add(arcOffset, 1.2, zCurve), Material.NETHERRACK);
                bot.scale(0.8f, 0.3f, 0.3f).glow(120, 60, 40).interpolation(2, 0);
                unit.add(bot);
                spawnedEntities.add(bot.entity());

                // Black concrete backing
                BlockDisplayHandle backing = displayBuilder.spawnBlock(
                        center.clone().add(arcOffset, 2, zCurve + 0.5), Material.BLACK_CONCRETE);
                backing.scale(0.8f, 1.0f, 0.3f).glow(20, 20, 25).interpolation(2, 0);
                unit.add(backing);
                spawnedEntities.add(backing.entity());

                eyeUnits.add(unit);
            }

            // Base rail: 20 blackstone blocks
            for (int x = -10; x < 10; x++) {
                double z = Math.abs(x / 5.0) * 0.8; // Arc
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                baseRail.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sequential iris blink: staggered 15-tick phases left to right
            for (int i = 0; i < irisBlocks.size(); i++) {
                int offset = i * 15;
                float iScale = 0.8f + 0.3f * (float) Math.sin((ticksAlive + offset) * Math.PI / 7.5);
                irisBlocks.get(i).scale(iScale, iScale, 0.4f);
                irisBlocks.get(i).interpolation(3, 0);
            }

            // Synchronized fire every 100 ticks (5 seconds)
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                // All 5 fire simultaneously
                for (BlockDisplayHandle iris : irisBlocks) {
                    Location iLoc = iris.entity().getLocation();
                    // Flame laser burst forward
                    for (int d = 0; d < 12; d++) {
                        w.spawnParticle(Particle.FLAME, iLoc.clone().add(0, 0, -d), 5,
                                0.1, 0.1, 0.1, 0.02);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.4f);
            }

            // Continuous soul fire tracking beams
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle iris : irisBlocks) {
                    Location iLoc = iris.entity().getLocation();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, iLoc.clone().add(0, 0, -1), 2,
                            0.05, 0.05, 0.5, 0.03);
                }
            }

            // Sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeBeamArray(plugin); }
    }

    // ================================================================
    // 70. THE PRESSURE FIELD — Large flat disc that descends and slams
    //     with hanging stalactite blaze rods and rotating edge teeth
    // ================================================================
    public static class ThePressureField extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();
        private BlockDisplayHandle magmaCenter;
        private final List<BlockDisplayHandle> teeth = new ArrayList<>();
        private final List<BlockDisplayHandle> pendulums = new ArrayList<>();
        private float discHeight = 4.0f;
        private boolean slamming = false;

        public ThePressureField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_pressure_field", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Disc: concentric rings, 9-block diameter
            // Center: magma
            magmaCenter = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.MAGMA_BLOCK);
            magmaCenter.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
            discBlocks.add(magmaCenter);
            spawnedEntities.add(magmaCenter.entity());

            // Inner ring: 8 netherrack
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 1.5, 4, Math.sin(angle) * 1.5), Material.NETHERRACK);
                h.scale(1.0f, 0.5f, 1.0f).glow(120, 60, 40).interpolation(2, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 12 cracked stone brick
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3.0, 4, Math.sin(angle) * 3.0),
                        Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f, 0.5f, 1.0f).glow(70, 70, 80).interpolation(2, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer ring: 16 blackstone
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 4.5, 4, Math.sin(angle) * 4.5), Material.BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Edge teeth: 4 obsidian at cardinal positions
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 5.0, 4, Math.sin(angle) * 5.0), Material.OBSIDIAN);
                h.scale(0.6f, 0.5f, 0.6f).glow(30, 0, 40).interpolation(2, 0);
                teeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Hanging pendulums: 8 blaze-rod-like structures
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 2.5, 3, Math.sin(angle) * 2.5), Material.NETHERRACK);
                h.scale(0.15f, 1.5f, 0.15f).glow(255, 100, 0).interpolation(3, 0);
                pendulums.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Disc Y-rotation: 0.5°/tick
            float discRot = ticksAlive * 0.00873f;

            // Descent: 0.01 blocks/tick from 4 down to 1, then slam
            int cyclePos = ticksAlive % 440;
            if (cyclePos < 400) {
                discHeight = 4.0f - (cyclePos * 0.01f);
                if (discHeight < 1.0f) discHeight = 1.0f;
                slamming = false;
            } else if (cyclePos == 400) {
                // Slam
                discHeight = 0;
                slamming = true;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                w.spawnParticle(Particle.LAVA, center, 30, 4, 0.5, 4, 0.1);
            } else if (cyclePos < 420) {
                discHeight = 0; // Hold
            } else {
                discHeight = 4.0f; // Reset
            }

            // Move disc to current height
            for (BlockDisplayHandle h : discBlocks) {
                h.rotate(discRot, 0, 1, 0);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : teeth) {
                h.rotate(discRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Center magma pulse: 20-tick cycle
            float centerScale = 1.0f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 10);
            if (magmaCenter != null) {
                magmaCenter.scale(centerScale, 0.5f, centerScale);
                magmaCenter.interpolation(3, 0);
            }

            // Pendulum Z-rotation: 1.0°/tick
            float pendRot = ticksAlive * 0.01745f;
            for (BlockDisplayHandle h : pendulums) {
                h.rotate(pendRot, 0, 0, 1);
                h.interpolation(3, 0);
            }

            // Lava drips from disc underside
            if (ticksAlive % 3 == 0) {
                double dAngle = Math.random() * Math.PI * 2;
                double dRadius = Math.random() * 4.5;
                Location dripLoc = center.clone().add(
                        Math.cos(dAngle) * dRadius, discHeight - 0.5, Math.sin(dAngle) * dRadius);
                w.spawnParticle(Particle.LAVA, dripLoc, 1, 0.1, 0.1, 0.1, 0);
            }

            // Flame from center through disc
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, discHeight + 0.5, 0),
                        2, 0.1, 0.3, 0.1, 0.01);
            }

            // Smoke from edge
            if (ticksAlive % 5 == 0) {
                double sAngle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.SMOKE,
                        center.clone().add(Math.cos(sAngle) * 4.5, discHeight, Math.sin(sAngle) * 4.5),
                        2, 0.2, 0.2, 0.2, 0.01);
            }

            // Sound during descent
            if (ticksAlive % 80 == 0 && cyclePos < 400) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThePressureField(plugin); }
    }
}
