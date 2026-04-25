package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * DevilsDream Mode — BLOCK DISPLAY ATTACKS (set 2, entries 11–20).
 * Nightmare/demonic/hellfire-themed structural attacks.
 * Each attack spawns >=25 displays, varies geometry (rings, spirals, curves,
 * tapered shapes, helixes), and has spawn / active / dissipate phases.
 * No potion effects — damage only.
 */
public final class DDBlockDisplay2 {
    private DDBlockDisplay2() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ObsidianFuneralPyre(plugin));
        registry.register(new WarpedSpineDragon(plugin));
        registry.register(new ExecutionersBlock(plugin));
        registry.register(new NetherGale(plugin));
        registry.register(new UnholyCrucifix(plugin));
        registry.register(new FracturedHalo(plugin));
        registry.register(new DreamersGuillotine(plugin));
        registry.register(new SoulFurnace(plugin));
        registry.register(new TheGuillotined(plugin));
        registry.register(new ObsidianMoth(plugin));
    }

    // ================================================================
    // 11. OBSIDIAN FUNERAL PYRE
    //     Stacked criss-cross pyre that ignites, expands, collapses,
    //     then erupts upward in a final pyre-burst.
    //     Impact-only damage on collapse + upward eruption.
    //     Display count: 4 layers x 6 logs + 4 magma + 4 altar legs +
    //                    4 bone offerings = 36 displays.
    // ================================================================
    public static class ObsidianFuneralPyre extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> logs = new ArrayList<>();
        private final List<BlockDisplayHandle> magma = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> bones = new ArrayList<>();
        private int phase = 0;

        public ObsidianFuneralPyre(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_funeral_pyre", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.4f, 0.5f);
            // 4 altar leg pillars (nether bricks) at base corners
            double L = 2.4;
            double[][] corners = {{L,L},{-L,L},{L,-L},{-L,-L}};
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0], 0, c[1]), Material.NETHER_BRICKS);
                h.scale(0.9f, 1.6f, 0.9f).interpolation(10, 0).glow(140, 0, 30);
                legs.add(h);
            }
            // 4 magma blocks at base between the legs
            double[][] mPos = {{0,L},{0,-L},{L,0},{-L,0}};
            for (double[] m : mPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(m[0], 0.1, m[1]), Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.5f, 1.0f).glow(255, 120, 30).interpolation(10, 0);
                magma.add(h);
            }
            // 4 layers of 6 criss-cross logs (alternating axes)
            for (int layer = 0; layer < 4; layer++) {
                boolean alongX = (layer % 2 == 0);
                double y = 0.6 + layer * 0.95;
                for (int i = -1; i <= 1; i++) {
                    for (int s = 0; s < 2; s++) {
                        double off = i * 0.95;
                        double bias = (s == 0) ? -1.5 : 1.5;
                        Location loc = alongX
                                ? center.clone().add(bias, y, off)
                                : center.clone().add(off, y, bias);
                        BlockDisplayHandle log = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                        if (alongX) log.scale(3.0f, 0.7f, 0.7f);
                        else log.scale(0.7f, 0.7f, 3.0f);
                        log.glow(60, 30, 30).interpolation(8, 0);
                        logs.add(log);
                    }
                }
            }
            // 4 bone offerings on top
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location loc = center.clone().add(Math.cos(a) * 0.8, 4.6, Math.sin(a) * 0.8);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                b.scale(0.6f, 0.6f, 0.6f).glow(240, 230, 200).interpolation(10, 0);
                bones.add(b);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            // Phase progression
            if (tick < dur * 0.35) phase = 0;          // ignite + heat-expand
            else if (tick < dur * 0.55) phase = 1;     // collapse inward
            else if (tick < dur * 0.85) phase = 2;     // eruption upward
            else phase = 3;                             // dissipate

            // Constant fire / smoke / ash particles after ignite
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 6, 1.5, 1.0, 1.5, 0.02);
                w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 4, 0), 4, 1.0, 0.8, 1.0, 0.02);
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 0.6, 0), 1, 1.0, 0.2, 1.0, 0);
            }

            switch (phase) {
                case 0: { // heat expand + wobble
                    if (tick % 5 == 0) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_WOOD_STEP, 0.7f, 0.5f);
                        for (int i = 0; i < logs.size(); i++) {
                            BlockDisplayHandle log = logs.get(i);
                            float wobble = (float) Math.sin(tick * 0.18 + i) * 0.12f;
                            float outward = (float) (Math.sin(tick * 0.05) * 0.15);
                            log.animateTo(
                                    new Vector3f(outward, wobble, outward),
                                    new AxisAngle4f(wobble, 0, 1, 0),
                                    new Vector3f(((i / 6) % 2 == 0) ? 3.0f : 0.7f, 0.7f,
                                            ((i / 6) % 2 == 0) ? 0.7f : 3.0f), 5);
                        }
                    }
                    break;
                }
                case 1: { // collapse inward and downward
                    if (tick == (int)(dur * 0.35) + 1) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                        triggerImpactDamage(center);
                        for (int i = 0; i < logs.size(); i++) {
                            BlockDisplayHandle log = logs.get(i);
                            log.animateTo(
                                    new Vector3f((float) (Math.random() - 0.5) * 0.5f,
                                            -0.6f - (float) Math.random() * 0.4f,
                                            (float) (Math.random() - 0.5) * 0.5f),
                                    new AxisAngle4f((float) (Math.random() * Math.PI * 0.6 - 0.3), 1, 0, 1),
                                    new Vector3f(2.0f, 0.5f, 2.0f), 14);
                        }
                        for (BlockDisplayHandle b : bones) {
                            b.animateTo(new Vector3f(0, -3.5f, 0),
                                    new AxisAngle4f((float) Math.random(), 1, 0, 1),
                                    new Vector3f(0.5f, 0.5f, 0.5f), 14);
                        }
                    }
                    break;
                }
                case 2: { // eruption upward
                    if (tick == (int)(dur * 0.55) + 1) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.4f);
                        triggerImpactDamage(center);
                        for (int i = 0; i < logs.size(); i++) {
                            BlockDisplayHandle log = logs.get(i);
                            double a = Math.random() * Math.PI * 2;
                            float fx = (float) (Math.cos(a) * (2 + Math.random() * 4));
                            float fz = (float) (Math.sin(a) * (2 + Math.random() * 4));
                            float fy = 6f + (float) Math.random() * 4f;
                            log.animateTo(new Vector3f(fx, fy, fz),
                                    new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                                    new Vector3f(1.2f, 0.5f, 1.2f), 24);
                        }
                        for (BlockDisplayHandle b : bones) {
                            double a = Math.random() * Math.PI * 2;
                            b.animateTo(new Vector3f((float) Math.cos(a) * 4f, 7f, (float) Math.sin(a) * 4f),
                                    new AxisAngle4f((float) Math.random() * 6, 0, 1, 0),
                                    new Vector3f(0.4f, 0.4f, 0.4f), 24);
                        }
                    }
                    if (tick % 3 == 0) {
                        for (int i = 0; i < 12; i++) {
                            double a = Math.random() * Math.PI * 2;
                            double r = Math.random() * 3;
                            w.spawnParticle(Particle.LAVA,
                                    center.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 4, Math.sin(a) * r),
                                    1, 0.2, 0.4, 0.2, 0.2);
                        }
                    }
                    break;
                }
                case 3: { // dissipate scale-down
                    if (tick % 6 == 0) {
                        for (BlockDisplayHandle log : logs) {
                            log.animateTo(new Vector3f(0, 8f, 0), new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.05f, 0.05f, 0.05f), 6);
                        }
                        for (BlockDisplayHandle leg : legs) {
                            leg.animateTo(new Vector3f(0, -1f, 0), new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.05f, 0.05f, 0.05f), 6);
                        }
                        for (BlockDisplayHandle m : magma) {
                            m.animateTo(new Vector3f(0, -1f, 0), new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.05f, 0.05f, 0.05f), 6);
                        }
                    }
                    break;
                }
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.55f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ObsidianFuneralPyre(plugin); }
    }

    // ================================================================
    // 12. WARPED SPINE DRAGON
    //     Skull + neck + spine + 4 wing membranes — undulating S-curve,
    //     wing flex, head-tracking, periodic coil-and-whip.
    //     Display count: 6 skull + 4 neck + 8 spine + 4*4 wings = 34.
    // ================================================================
    public static class WarpedSpineDragon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> skull = new ArrayList<>();
        private final List<BlockDisplayHandle> neck = new ArrayList<>();
        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private final List<BlockDisplayHandle> wingsL = new ArrayList<>();
        private final List<BlockDisplayHandle> wingsR = new ArrayList<>();
        private int phase = 0;

        public WarpedSpineDragon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("warped_spine_dragon", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(22);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.6f);
            // 8 spine vertebrae (curve from -8 to +0 along Z)
            for (int i = 0; i < 8; i++) {
                double t = i / 7.0;
                double z = -8 + i * 1.0;
                double y = 4 + Math.sin(t * Math.PI) * 0.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, z), Material.WARPED_STEM);
                h.scale(0.9f, 0.9f, 0.9f).glow(50, 220, 200).interpolation(8, 0);
                spine.add(h);
            }
            // 4 neck vertebrae from spine head into skull
            for (int i = 0; i < 4; i++) {
                double z = 0.5 + i * 0.85;
                double y = 4 + i * 0.25;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, z), Material.WARPED_STEM);
                h.scale(0.85f, 0.85f, 0.85f).glow(50, 220, 200).interpolation(8, 0);
                neck.add(h);
            }
            // 6 skull pieces (deepslate tile cluster + sculk eyes)
            double sx = 0, sy = 5.4, sz = 4.5;
            double[][] skullOff = {
                {0, 0, 0}, {0.7, 0, 0}, {-0.7, 0, 0},
                {0.4, 0.6, -0.3}, {-0.4, 0.6, -0.3}, {0, -0.4, 0.6}
            };
            for (int i = 0; i < skullOff.length; i++) {
                double[] o = skullOff[i];
                Material m = (i >= 3 && i <= 4) ? Material.SCULK : Material.DEEPSLATE_TILES;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(sx + o[0], sy + o[1], sz + o[2]), m);
                h.scale(0.85f, 0.85f, 0.85f).interpolation(8, 0);
                if (m == Material.SCULK) h.glow(50, 220, 200);
                else h.glow(60, 60, 80);
                skull.add(h);
            }
            // 4 left + 4 right wing membrane segments
            for (int i = 0; i < 4; i++) {
                double zPos = -5 + i * 1.5;
                Location l = center.clone().add(-1.5 - i * 0.4, 4 + i * 0.3, zPos);
                Location r = center.clone().add(1.5 + i * 0.4, 4 + i * 0.3, zPos);
                BlockDisplayHandle wl = displayBuilder.spawnBlock(l, Material.WARPED_PLANKS);
                BlockDisplayHandle wr = displayBuilder.spawnBlock(r, Material.WARPED_PLANKS);
                wl.scale(1.6f, 1.0f, 1.0f).glow(50, 220, 200).interpolation(8, 0);
                wr.scale(1.6f, 1.0f, 1.0f).glow(50, 220, 200).interpolation(8, 0);
                wingsL.add(wl);
                wingsR.add(wr);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();
            phase = (tick < dur * 0.15) ? 0
                    : (tick % 140 < 100 ? 1 : 2);

            // Spine S-curve undulation
            if (tick % 4 == 0) {
                for (int i = 0; i < spine.size(); i++) {
                    double phaseShift = i * 0.4;
                    float xOff = (float) (Math.sin(tick * 0.12 - phaseShift) * 1.3);
                    float yLevitate = (float) Math.sin(tick * 0.05) * 0.5f;
                    spine.get(i).animateTo(
                            new Vector3f(xOff, yLevitate, 0),
                            new AxisAngle4f((float) (Math.sin(tick * 0.1 - phaseShift) * 0.3), 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 4);
                }
            }
            // Wing flex (left/right mirrored)
            if (tick % 3 == 0) {
                float flex = (float) Math.sin(tick * 0.1);
                for (int i = 0; i < 4; i++) {
                    wingsL.get(i).animateTo(
                            new Vector3f(-flex * 0.6f * (i + 1), flex * 0.4f, 0),
                            new AxisAngle4f(flex * 0.5f, 0, 0, 1),
                            new Vector3f(1.6f, 1.0f, 1.0f), 3);
                    wingsR.get(i).animateTo(
                            new Vector3f(flex * 0.6f * (i + 1), -flex * 0.4f, 0),
                            new AxisAngle4f(-flex * 0.5f, 0, 0, 1),
                            new Vector3f(1.6f, 1.0f, 1.0f), 3);
                }
            }
            // Head tracking
            if (tick % 8 == 0) {
                Player p = getTargetPlayer();
                float trackAng = 0;
                if (p != null && p.getWorld().equals(center.getWorld())) {
                    Vector dv = p.getLocation().toVector().subtract(center.toVector());
                    trackAng = (float) Math.atan2(dv.getX(), dv.getZ() + 4);
                }
                for (BlockDisplayHandle h : skull) {
                    h.animateTo(new Vector3f((float) Math.sin(trackAng) * 0.5f, 0, 0),
                            new AxisAngle4f(trackAng, 0, 1, 0),
                            new Vector3f(0.85f, 0.85f, 0.85f), 8);
                }
            }

            // Particles
            if (tick % 4 == 0) {
                for (BlockDisplayHandle wing : wingsL) {
                    Location loc = wing.entity().getLocation();
                    w.spawnParticle(Particle.WARPED_SPORE, loc, 3, 0.5, 0.3, 0.5, 0.01);
                }
                for (BlockDisplayHandle wing : wingsR) {
                    Location loc = wing.entity().getLocation();
                    w.spawnParticle(Particle.WARPED_SPORE, loc, 3, 0.5, 0.3, 0.5, 0.01);
                }
                if (skull.size() >= 5) {
                    w.spawnParticle(Particle.SCULK_SOUL, skull.get(3).entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                    w.spawnParticle(Particle.SCULK_SOUL, skull.get(4).entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.01);
                }
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 4.5, -3), 4, 1.0, 0.5, 2.5, 0.02);
            }

            // Whip-coil periodic (phase 2)
            if (phase == 2 && tick % 140 == 100) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 1.4f, 0.7f);
                for (int i = 0; i < spine.size(); i++) {
                    float a = (float) (i * 0.9 + tick * 0.1);
                    spine.get(i).animateTo(
                            new Vector3f((float) Math.cos(a) * 3.0f, (float) Math.sin(a) * 1.5f, (float) Math.sin(a) * 3.0f),
                            new AxisAngle4f(a, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 18);
                }
            }

            if (tick % 60 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 0.6f);

            // Dissipate
            if (tick > dur - 25 && tick % 5 == 0) {
                for (BlockDisplayHandle h : spine) h.animateTo(new Vector3f(0, 6, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.1f, 0.1f, 0.1f), 5);
                for (BlockDisplayHandle h : neck) h.animateTo(new Vector3f(0, 6, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.1f, 0.1f, 0.1f), 5);
                for (BlockDisplayHandle h : skull) h.animateTo(new Vector3f(0, 6, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.1f, 0.1f, 0.1f), 5);
                for (BlockDisplayHandle h : wingsL) h.animateTo(new Vector3f(0, 6, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.1f, 0.1f, 0.1f), 5);
                for (BlockDisplayHandle h : wingsR) h.animateTo(new Vector3f(0, 6, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.1f, 0.1f, 0.1f), 5);
            }
        }

        @Override public AbstractAttack newInstance() { return new WarpedSpineDragon(plugin); }
    }

    // ================================================================
    // 13. THE EXECUTIONER'S BLOCK
    //     Wide chopping block + suspended axe; axe descends + slams,
    //     block splits, reassembles. Impact-only damage on slam.
    //     Display count: 24 block body + 4 blood channels + 8 axe head
    //     + 5 axe handle + 2 axe top = 43 displays.
    // ================================================================
    public static class ExecutionersBlock extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyLeft = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyRight = new ArrayList<>();
        private final List<BlockDisplayHandle> bloodChannel = new ArrayList<>();
        private final List<BlockDisplayHandle> axeHead = new ArrayList<>();
        private final List<BlockDisplayHandle> axeHandle = new ArrayList<>();
        private int phase = 0;

        public ExecutionersBlock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("executioners_block", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(6.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.4f, 0.5f);
            // Block body 4x3x2.5 split into left/right halves for splitting animation
            for (int x = -2; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = 0; y <= 2; y++) {
                        Material m = (y == 2) ? Material.DEEPSLATE_TILES : Material.POLISHED_BLACKSTONE_BRICKS;
                        Location loc = center.clone().add(x + 0.5, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, m);
                        h.scale(1.0f, 1.0f, 1.0f).interpolation(8, 0);
                        if (x < 0) bodyLeft.add(h); else bodyRight.add(h);
                    }
                }
            }
            // 4 blood channels carved into top
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-1.5 + i * 0.9, 2.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_STAINED_GLASS);
                h.scale(0.7f, 0.15f, 1.7f).glow(180, 0, 0).interpolation(8, 0);
                bloodChannel.add(h);
            }
            // Axe head — 8 blackstone slabs in blade fan shape, suspended above
            for (int i = 0; i < 8; i++) {
                double a = -0.6 + i * 0.15;
                Location loc = center.clone().add(0, 9 + a * 0.4, -0.5 + Math.sin(a) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.4f, 0.4f, 0.5f).rotate(0.4f, 0, 0, 1).glow(80, 80, 90).interpolation(10, 0);
                axeHead.add(h);
            }
            // Axe handle — 5 bone block segments
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 9.4 + i * 0.7, 0.6 + i * 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BONE_BLOCK);
                h.scale(0.4f, 0.7f, 0.4f).glow(240, 230, 200).interpolation(10, 0);
                axeHandle.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            if (tick < dur * 0.30) phase = 0;        // slow descent
            else if (tick < dur * 0.45) phase = 1;   // slam + split
            else if (tick < dur * 0.80) phase = 2;   // hold split
            else phase = 3;                           // reassemble

            if (phase == 0) {
                if (tick % 6 == 0) {
                    float drop = (float) (tick * 0.015);
                    for (BlockDisplayHandle h : axeHead) {
                        h.animateTo(new Vector3f(0, -drop, 0), new AxisAngle4f(0.4f, 0, 0, 1),
                                new Vector3f(1.4f, 0.4f, 0.5f), 6);
                    }
                    for (BlockDisplayHandle h : axeHandle) {
                        h.animateTo(new Vector3f(0, -drop, 0), new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.4f, 0.7f, 0.4f), 6);
                    }
                }
            } else if (phase == 1 && tick == (int)(dur * 0.30) + 1) {
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.8f, 0.5f);
                triggerImpactDamage(center.clone().add(0, 2, 0));
                // Axe slam — instant translate down to block top
                for (BlockDisplayHandle h : axeHead) {
                    h.animateTo(new Vector3f(0, -6.5f, 0), new AxisAngle4f(0.4f, 0, 0, 1),
                            new Vector3f(1.4f, 0.4f, 0.5f), 3);
                }
                for (BlockDisplayHandle h : axeHandle) {
                    h.animateTo(new Vector3f(0, -6.5f, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 0.7f, 0.4f), 3);
                }
                // Block splits outward
                for (BlockDisplayHandle h : bodyLeft) {
                    h.animateTo(new Vector3f(-1.8f, 0, 0), new AxisAngle4f(0.1f, 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f), 8);
                }
                for (BlockDisplayHandle h : bodyRight) {
                    h.animateTo(new Vector3f(1.8f, 0, 0), new AxisAngle4f(-0.1f, 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f), 8);
                }
                for (BlockDisplayHandle h : bloodChannel) {
                    h.animateTo(new Vector3f(0, 0.2f, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.15f, 1.7f), 8);
                }
                for (int i = 0; i < 30; i++) {
                    w.spawnParticle(Particle.DUST, center.clone().add(0, 2.6, 0),
                            1, 1.5, 0.3, 1.5, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.6f));
                }
                w.spawnParticle(Particle.CRIT, center.clone().add(0, 2.6, 0), 30, 2, 0.5, 2, 0.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.4f, 0.4f);
            } else if (phase == 3 && tick % 8 == 0) {
                // Reassemble
                for (BlockDisplayHandle h : bodyLeft) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1.0f, 1.0f, 1.0f), 8);
                for (BlockDisplayHandle h : bodyRight) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1.0f, 1.0f, 1.0f), 8);
                for (BlockDisplayHandle h : axeHead) h.animateTo(new Vector3f(0, 4f, 0), new AxisAngle4f(0.4f, 0, 0, 1), new Vector3f(1.4f, 0.4f, 0.5f), 8);
                for (BlockDisplayHandle h : axeHandle) h.animateTo(new Vector3f(0, 4f, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.4f, 0.7f, 0.4f), 8);
            }

            // Constant ambient
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.DUST, center.clone().add(0, 2.5, 0), 2, 1.0, 0.3, 1.0,
                        new Particle.DustOptions(Color.fromRGB(140, 0, 30), 1.0f));
            }
        }

        @Override public AbstractAttack newInstance() { return new ExecutionersBlock(plugin); }
    }

    // ================================================================
    // 14. NETHER GALE
    //     Tornado: ground debris (basalt) + spiraling nether wart helix +
    //     fast orbiting blackstone crown. Drifts in arc, kicks debris.
    //     Display count: 5 base debris + 16 helix + 8 top crown = 29.
    // ================================================================
    public static class NetherGale extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private final List<BlockDisplayHandle> helix = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();

        public NetherGale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_gale", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.2f, 1.8f);
            // 5 ground debris basalt rocks scattered
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                Location loc = center.clone().add(Math.cos(a) * 2.6, 0.2, Math.sin(a) * 2.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(1.2f, 0.7f, 1.2f).interpolation(8, 0);
                debris.add(h);
            }
            // 16 helix nether wart blocks (mid section)
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double a = t * Math.PI * 4;
                double r = 2.2 + t * 0.6;
                double y = 1.5 + t * 6;
                Location loc = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(140, 0, 30).interpolation(8, 0);
                helix.add(h);
            }
            // 8 crown blackstone chunks orbiting at top
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location loc = center.clone().add(Math.cos(a) * 2.0, 8.5, Math.sin(a) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.65f, 0.65f, 0.65f).interpolation(4, 0);
                crown.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            // Bottom slow rotation
            if (tick % 6 == 0) {
                for (int i = 0; i < debris.size(); i++) {
                    double a = tick * 0.03 + Math.PI * 2 * i / debris.size();
                    debris.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * 2.6 - Math.cos(Math.PI * 2 * i / 5) * 2.6),
                                    0,
                                    (float) (Math.sin(a) * 2.6 - Math.sin(Math.PI * 2 * i / 5) * 2.6)),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(1.2f, 0.7f, 1.2f), 6);
                }
            }
            // Mid medium rotation
            if (tick % 3 == 0) {
                for (int i = 0; i < helix.size(); i++) {
                    double t = i / 15.0;
                    double a = t * Math.PI * 4 + tick * 0.10;
                    double r = 2.2 + t * 0.6;
                    double oa = t * Math.PI * 4;
                    double or = 2.2 + t * 0.6;
                    helix.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * r - Math.cos(oa) * or),
                                    (float) Math.sin(tick * 0.08 + i) * 0.3f,
                                    (float) (Math.sin(a) * r - Math.sin(oa) * or)),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 3);
                }
            }
            // Top fast rotation (blur)
            if (tick % 2 == 0) {
                for (int i = 0; i < crown.size(); i++) {
                    double a = tick * 0.30 + Math.PI * 2 * i / 8;
                    crown.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * 2.0 - Math.cos(Math.PI * 2 * i / 8) * 2.0),
                                    0,
                                    (float) (Math.sin(a) * 2.0 - Math.sin(Math.PI * 2 * i / 8) * 2.0)),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.65f, 0.65f, 0.65f), 2);
                }
            }
            // Particles
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.5;
                    double y = Math.random() * 9;
                    Location loc = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    if (y < 3) w.spawnParticle(Particle.ASH, loc, 2, 0.3, 0.3, 0.3, 0.05);
                    else if (y < 7) w.spawnParticle(Particle.WARPED_SPORE, loc, 2, 0.3, 0.3, 0.3, 0.05);
                    else w.spawnParticle(Particle.FLAME, loc, 1, 0.2, 0.2, 0.2, 0.02);
                }
            }
            // Debris kick
            if (tick % 35 == 0 && tick > 30) {
                int idx = (int) (Math.random() * debris.size());
                BlockDisplayHandle d = debris.get(idx);
                double a = Math.random() * Math.PI * 2;
                d.animateTo(new Vector3f((float) Math.cos(a) * 4f, 1.5f, (float) Math.sin(a) * 4f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(1.2f, 0.7f, 1.2f), 8);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_FALL, 1.2f, 0.7f);
            }

            if (tick % 25 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 0.8f, 1.7f);

            // Dissipate
            if (tick > dur - 30 && tick % 5 == 0) {
                for (BlockDisplayHandle h : debris) h.animateTo(new Vector3f(0, 8, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : helix) h.animateTo(new Vector3f(0, 8, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : crown) h.animateTo(new Vector3f(0, 12, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
            }
        }

        @Override public AbstractAttack newInstance() { return new NetherGale(plugin); }
    }

    // ================================================================
    // 15. UNHOLY CRUCIFIX
    //     Inverted cross descends, rotates, face panels open, flips,
    //     stakes downward. Impact-only on stake slam.
    //     Display count: 20 vertical beam + 14 crossbar + 4 shroomlight
    //     joints + 4 carved face panels + 4 crying obsidian arm tips = 46.
    // ================================================================
    public static class UnholyCrucifix extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> beam = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbar = new ArrayList<>();
        private final List<BlockDisplayHandle> joints = new ArrayList<>();
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private final List<BlockDisplayHandle> tips = new ArrayList<>();
        private int phase = 0;

        public UnholyCrucifix(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("unholy_crucifix", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(19.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.7f);
            // Vertical beam — 20 displays (10 long, 2 wide)
            for (int i = 0; i < 10; i++) {
                for (int s = 0; s < 2; s++) {
                    double y = 12 - i * 1.0;
                    double xOff = (s == 0) ? -0.5 : 0.5;
                    Location loc = center.clone().add(xOff, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(1.0f, 1.0f, 1.0f).interpolation(10, 0);
                    beam.add(h);
                }
            }
            // Crossbar — 14 displays (7 wide, 2 deep)
            for (int i = 0; i < 7; i++) {
                for (int s = 0; s < 2; s++) {
                    double x = -3 + i * 1.0;
                    double zOff = (s == 0) ? -0.5 : 0.5;
                    Location loc = center.clone().add(x, 9.5, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(1.0f, 1.0f, 1.0f).interpolation(10, 0);
                    crossbar.add(h);
                }
            }
            // 4 shroomlight joints at intersection corners
            double[][] jPos = {{-0.5, 9.5, -0.5}, {0.5, 9.5, -0.5}, {-0.5, 9.5, 0.5}, {0.5, 9.5, 0.5}};
            for (double[] p : jPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0], p[1], p[2]), Material.SHROOMLIGHT);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 60).interpolation(10, 0);
                joints.add(h);
            }
            // 4 carved face panels at arm ends (top, bottom, left, right)
            double[][] fPos = {{0, 12.6, 0}, {0, 6.5, 0}, {-3.6, 9.5, 0}, {3.6, 9.5, 0}};
            for (double[] p : fPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0], p[1], p[2]), Material.CHISELED_POLISHED_BLACKSTONE);
                h.scale(1.1f, 1.1f, 1.1f).glow(140, 0, 30).interpolation(10, 0);
                faces.add(h);
            }
            // 4 crying obsidian drip tips
            for (double[] p : fPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0], p[1] - 0.5, p[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(90, 0, 130).interpolation(10, 0);
                tips.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            if (tick < dur * 0.30) phase = 0;       // descend + rotate
            else if (tick < dur * 0.55) phase = 1;  // faces open
            else if (tick < dur * 0.75) phase = 2;  // flip + stake
            else phase = 3;                          // dissipate

            // Slow Y rotation always
            if (tick % 6 == 0 && phase < 2) {
                float rotY = (float) (tick * 0.05);
                for (BlockDisplayHandle h : beam) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(rotY, 0, 1, 0), new Vector3f(1, 1, 1), 6);
                for (BlockDisplayHandle h : crossbar) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(rotY, 0, 1, 0), new Vector3f(1, 1, 1), 6);
                for (BlockDisplayHandle h : joints) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(rotY, 0, 1, 0), new Vector3f(0.6f, 0.6f, 0.6f), 6);
                for (BlockDisplayHandle h : faces) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(rotY, 0, 1, 0), new Vector3f(1.1f, 1.1f, 1.1f), 6);
                for (BlockDisplayHandle h : tips) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(rotY, 0, 1, 0), new Vector3f(0.4f, 0.4f, 0.4f), 6);
            }

            if (phase == 1 && tick % 12 == 0) {
                // faces "open" — scale up briefly
                for (int i = 0; i < faces.size(); i++) {
                    float s = 1.5f + (float) Math.sin(tick * 0.15 + i) * 0.3f;
                    faces.get(i).animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(s, s, s), 12);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.4f);
                // 4-direction face arm-tip burst damage hint via particles
                for (BlockDisplayHandle f : faces) {
                    Location l = f.entity().getLocation();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, l, 12, 0.5, 0.5, 0.5, 0.1);
                }
            }
            if (phase == 2 && tick == (int)(dur * 0.55) + 1) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.6f, 0.6f);
                triggerImpactDamage(center);
                // Flip + slam
                for (BlockDisplayHandle h : beam) h.animateTo(new Vector3f(0, -10f, 0), new AxisAngle4f((float)Math.PI, 0, 0, 1), new Vector3f(1, 1, 1), 12);
                for (BlockDisplayHandle h : crossbar) h.animateTo(new Vector3f(0, -10f, 0), new AxisAngle4f((float)Math.PI, 0, 0, 1), new Vector3f(1, 1, 1), 12);
                for (BlockDisplayHandle h : joints) h.animateTo(new Vector3f(0, -10f, 0), new AxisAngle4f((float)Math.PI, 0, 0, 1), new Vector3f(0.6f, 0.6f, 0.6f), 12);
                for (BlockDisplayHandle h : faces) h.animateTo(new Vector3f(0, -10f, 0), new AxisAngle4f((float)Math.PI, 0, 0, 1), new Vector3f(1.1f, 1.1f, 1.1f), 12);
                for (BlockDisplayHandle h : tips) h.animateTo(new Vector3f(0, -10f, 0), new AxisAngle4f((float)Math.PI, 0, 0, 1), new Vector3f(0.4f, 0.4f, 0.4f), 12);
            }

            // Particles
            if (tick % 4 == 0) {
                for (BlockDisplayHandle t : tips) {
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, t.entity().getLocation(), 2, 0.2, 0.2, 0.2, 0);
                }
                for (BlockDisplayHandle j : joints) {
                    w.spawnParticle(Particle.GLOW, j.entity().getLocation(), 2, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new UnholyCrucifix(plugin); }
    }

    // ================================================================
    // 16. FRACTURED HALO
    //     12-segment ring (alternating amethyst + calcite) with crying
    //     obsidian cracks. Drifts apart, reassembles in implosion.
    //     Display count: 12 ring segments + 12 cracks + 8 inner glow
    //     accents = 32.
    // ================================================================
    public static class FracturedHalo extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private final List<BlockDisplayHandle> innerAccents = new ArrayList<>();
        private final double tilt = Math.toRadians(20);

        public FracturedHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_halo", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
            double R = 5.0;
            double y = 6;
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                double xRaw = Math.cos(a) * R;
                double zRaw = Math.sin(a) * R;
                double yTilt = Math.sin(a) * Math.sin(tilt) * R;
                Location loc = center.clone().add(xRaw, y + yTilt, zRaw * Math.cos(tilt));
                Material m = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.CALCITE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, m);
                h.scale(0.95f, 0.6f, 1.4f).rotate((float) a, 0, 1, 0).interpolation(10, 0);
                if (m == Material.AMETHYST_BLOCK) h.glow(180, 100, 220);
                else h.glow(240, 240, 240);
                segments.add(h);
            }
            // Cracks between segments
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * (i + 0.5) / 12;
                double xRaw = Math.cos(a) * R;
                double zRaw = Math.sin(a) * R;
                double yTilt = Math.sin(a) * Math.sin(tilt) * R;
                Location loc = center.clone().add(xRaw, y + yTilt, zRaw * Math.cos(tilt));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.5f, 0.4f).rotate((float) a, 0, 1, 0).glow(90, 0, 130).interpolation(10, 0);
                cracks.add(h);
            }
            // 8 inner accent glows
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double r2 = 2.2;
                Location loc = center.clone().add(Math.cos(a) * r2, y + 0.2, Math.sin(a) * r2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.35f, 0.35f, 0.35f).glow(200, 140, 240).interpolation(10, 0);
                innerAccents.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();
            int cycle = dur / 3;
            int local = tick % cycle;
            int phase = (local < cycle * 0.5) ? 0 : (local < cycle * 0.85 ? 1 : 2);

            // Y rotation always
            float rotY = tick * 0.04f;
            // separation factor
            float sep = (phase == 0) ? local / (float) (cycle * 0.5) : (phase == 1 ? 1.0f : 0f);

            if (tick % 4 == 0) {
                for (int i = 0; i < segments.size(); i++) {
                    double a = Math.PI * 2 * i / 12;
                    float ox = (float) Math.cos(a) * sep * 1.0f;
                    float oz = (float) Math.sin(a) * sep * 1.0f;
                    float oy = sep * 0.4f * (i % 3 - 1);
                    segments.get(i).animateTo(
                            new Vector3f(ox, oy, oz),
                            new AxisAngle4f((float) a + rotY, 0, 1, 0),
                            new Vector3f(0.95f, 0.6f, 1.4f), 4);
                }
                for (int i = 0; i < cracks.size(); i++) {
                    double a = Math.PI * 2 * (i + 0.5) / 12;
                    float widen = 1.0f + sep * 1.4f;
                    cracks.get(i).animateTo(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f((float) a + rotY, 0, 1, 0),
                            new Vector3f(0.4f * widen, 0.5f, 0.4f * widen), 4);
                }
                for (int i = 0; i < innerAccents.size(); i++) {
                    double a = Math.PI * 2 * i / 8 + rotY * 0.5;
                    innerAccents.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 2.2 - Math.cos(Math.PI * 2 * i / 8) * 2.2),
                                    (float) Math.sin(tick * 0.1 + i) * 0.4f,
                                    (float)(Math.sin(a) * 2.2 - Math.sin(Math.PI * 2 * i / 8) * 2.2)),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.35f), 4);
                }
            }

            if (phase == 2 && local == (int)(cycle * 0.85)) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.6f, 0.5f);
                w.spawnParticle(Particle.FLASH, center.clone().add(0, 6, 0), 1, 0, 0, 0, 0);
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 5;
                    Location l = center.clone().add(Math.cos(a) * r, 6 + (Math.random() - 0.5), Math.sin(a) * r);
                    w.spawnParticle(Particle.DUST, l, 1, 0.1, 0.1, 0.1,
                            new Particle.DustOptions(Color.fromRGB(180, 100, 220), 1.6f));
                }
            }

            // Particles
            if (tick % 4 == 0) {
                for (int i = 0; i < cracks.size(); i++) {
                    Location l = cracks.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DUST, l, 2, 0.2, 0.2, 0.2,
                            new Particle.DustOptions(Color.fromRGB(180, 100, 220), 1.0f));
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, l, 1, 0.1, 0.1, 0.1, 0);
                }
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.4f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FracturedHalo(plugin); }
    }

    // ================================================================
    // 17. DREAMER'S GUILLOTINE
    //     Two posts, crossbar, suspended blade. Blade drops repeatedly,
    //     winches up. Impact-only damage on each drop.
    //     Display count: 2 posts x 8 = 16 + crossbar 5 + 4 guide rails
    //     + 6 blade displays = 31.
    // ================================================================
    public static class DreamersGuillotine extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> postsL = new ArrayList<>();
        private final List<BlockDisplayHandle> postsR = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbar = new ArrayList<>();
        private final List<BlockDisplayHandle> rails = new ArrayList<>();
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private float bladeY = 0;

        public DreamersGuillotine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dreamers_guillotine", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(440);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(17.0);
            config.setImpactRadius(5.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_STEP, 1.2f, 0.6f);
            // Posts (each 8 tall, x=±2)
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle l = displayBuilder.spawnBlock(center.clone().add(-2, i, 0), Material.POLISHED_BLACKSTONE_BRICKS);
                BlockDisplayHandle r = displayBuilder.spawnBlock(center.clone().add(2, i, 0), Material.POLISHED_BLACKSTONE_BRICKS);
                l.scale(0.8f, 1.0f, 0.8f).interpolation(10, 0);
                r.scale(0.8f, 1.0f, 0.8f).interpolation(10, 0);
                postsL.add(l);
                postsR.add(r);
            }
            // Crossbar (5 wide at top)
            for (int i = -2; i <= 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(i, 8.2, 0), Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(1.0f, 0.5f, 0.8f).interpolation(10, 0);
                crossbar.add(h);
            }
            // Guide rails (4 deepslate tile strips on inside of posts)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle r1 = displayBuilder.spawnBlock(center.clone().add(-1.6, 1 + i * 3.5, 0), Material.DEEPSLATE_TILES);
                BlockDisplayHandle r2 = displayBuilder.spawnBlock(center.clone().add(1.6, 1 + i * 3.5, 0), Material.DEEPSLATE_TILES);
                r1.scale(0.2f, 3.4f, 0.6f).interpolation(10, 0);
                r2.scale(0.2f, 3.4f, 0.6f).interpolation(10, 0);
                rails.add(r1);
                rails.add(r2);
            }
            // Blade (3 polished blackstone + 3 red glass accent)
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(-1.2 + i * 1.2, 7.5, 0), Material.POLISHED_BLACKSTONE);
                b.scale(1.2f, 0.5f, 0.5f).interpolation(2, 0).glow(200, 200, 220);
                blade.add(b);
            }
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(-1.2 + i * 1.2, 7.0, 0), Material.RED_STAINED_GLASS);
                b.scale(1.2f, 0.3f, 0.4f).glow(200, 0, 0).interpolation(2, 0);
                blade.add(b);
            }
            bladeY = 0;
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();
            int cycle = 80;
            int local = tick % cycle;

            if (tick < 25) return; // initial spawn pause

            if (local == 0) {
                // Drop
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.7f);
                for (BlockDisplayHandle b : blade) {
                    b.animateTo(new Vector3f(0, -7f, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.2f, 0.5f, 0.5f), 4);
                }
                bladeY = -7;
            } else if (local == 6) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.5f);
                for (int i = 0; i < 20; i++) {
                    w.spawnParticle(Particle.DUST, center.clone().add(0, 0.6, 0), 1, 1.5, 0.2, 0.4,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.4f));
                }
            } else if (local == 14) {
                // Bounce overshoot occasionally
                if (Math.random() < 0.4) {
                    for (BlockDisplayHandle b : blade) {
                        b.animateTo(new Vector3f(0, -5f, 0), new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.2f, 0.5f, 0.5f), 4);
                    }
                    bladeY = -5;
                }
            } else if (local == 24) {
                // Winch back up slowly
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.6f);
                for (BlockDisplayHandle b : blade) {
                    b.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.2f, 0.5f, 0.5f), 50);
                }
                bladeY = 0;
            }

            // Air slash particles during drop (local 0–6)
            if (local < 6) {
                for (int i = 0; i < 12; i++) {
                    double y = 6 - local + Math.random() * 1.5;
                    w.spawnParticle(Particle.SWEEP_ATTACK,
                            center.clone().add((Math.random() - 0.5) * 2, y, 0), 1, 0, 0, 0, 0);
                }
            }
            if (tick % 3 == 0) {
                w.spawnParticle(Particle.DUST,
                        center.clone().add((Math.random() - 0.5) * 2, 1 + Math.random() * 2, 0),
                        1, 0.2, 0.5, 0.2,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.0f));
            }

            // Dissipate
            if (tick > dur - 30 && tick % 5 == 0) {
                for (BlockDisplayHandle h : postsL) h.animateTo(new Vector3f(-3, -1, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : postsR) h.animateTo(new Vector3f(3, -1, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : crossbar) h.animateTo(new Vector3f(0, 4, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : rails) h.animateTo(new Vector3f(0, -2, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : blade) h.animateTo(new Vector3f(0, -2, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
            }
        }

        @Override public AbstractAttack newInstance() { return new DreamersGuillotine(plugin); }
    }

    // ================================================================
    // 18. SOUL FURNACE
    //     Boxy 4x4x4 furnace with carved face, mouth opens to vent fire,
    //     walks (rocks side to side). Continuous damage in front cone.
    //     Display count: 4-walls hollow box (~40) + face panel + mouth
    //     halves (2) + magma core (1) + 2 shroomlight cracks = far over 25.
    //     Simplified: 24 wall blocks + 4 face + 2 mouth halves + 2 magma
    //     + 2 shroomlight = 34 displays.
    // ================================================================
    public static class SoulFurnace extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> face = new ArrayList<>();
        private final List<BlockDisplayHandle> mouth = new ArrayList<>();
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private int phase = 0;

        public SoulFurnace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_furnace", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.5);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.4f, 0.5f);
            // Box body — 4 walls (skip front face area), 4 high. Use 6x4 = 24 wall blocks.
            // Sides (left/right): each 2 deep x 4 tall
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle wL = displayBuilder.spawnBlock(center.clone().add(-2, y, z), Material.POLISHED_BLACKSTONE_BRICKS);
                    BlockDisplayHandle wR = displayBuilder.spawnBlock(center.clone().add(2, y, z), Material.POLISHED_BLACKSTONE_BRICKS);
                    wL.scale(1.0f, 1.0f, 1.0f).interpolation(8, 0);
                    wR.scale(1.0f, 1.0f, 1.0f).interpolation(8, 0);
                    walls.add(wL);
                    walls.add(wR);
                }
            }
            // Back wall: 4 wide x 3 tall (skip top center for stove pipe)
            for (int x = -1; x <= 2; x++) {
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(x, y, -1.5), Material.POLISHED_BLACKSTONE_BRICKS);
                    b.scale(1.0f, 1.0f, 1.0f).interpolation(8, 0);
                    walls.add(b);
                }
            }
            // Face panel (4 carved blackstone)
            double[][] fOff = {{-0.5, 1.5, 1.5}, {0.5, 1.5, 1.5}, {-0.5, 2.5, 1.5}, {0.5, 2.5, 1.5}};
            for (double[] o : fOff) {
                BlockDisplayHandle f = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.CHISELED_POLISHED_BLACKSTONE);
                f.scale(1.0f, 1.0f, 0.3f).interpolation(8, 0).glow(140, 0, 30);
                face.add(f);
            }
            // Mouth halves (2 magma blocks)
            BlockDisplayHandle mL = displayBuilder.spawnBlock(center.clone().add(-0.5, 0.6, 1.6), Material.MAGMA_BLOCK);
            BlockDisplayHandle mR = displayBuilder.spawnBlock(center.clone().add(0.5, 0.6, 1.6), Material.MAGMA_BLOCK);
            mL.scale(1.0f, 0.8f, 0.3f).glow(255, 120, 30).interpolation(6, 0);
            mR.scale(1.0f, 0.8f, 0.3f).glow(255, 120, 30).interpolation(6, 0);
            mouth.add(mL);
            mouth.add(mR);
            // Magma core glow inside (2)
            BlockDisplayHandle c1 = displayBuilder.spawnBlock(center.clone().add(0, 0.6, 0), Material.MAGMA_BLOCK);
            BlockDisplayHandle c2 = displayBuilder.spawnBlock(center.clone().add(0, 1.6, 0), Material.MAGMA_BLOCK);
            c1.scale(1.5f, 1.0f, 1.5f).glow(255, 120, 30).interpolation(6, 0);
            c2.scale(1.5f, 1.0f, 1.5f).glow(255, 120, 30).interpolation(6, 0);
            core.add(c1);
            core.add(c2);
            // 2 shroomlight cracks
            BlockDisplayHandle s1 = displayBuilder.spawnBlock(center.clone().add(-2.05, 1.5, 0), Material.SHROOMLIGHT);
            BlockDisplayHandle s2 = displayBuilder.spawnBlock(center.clone().add(2.05, 2.5, 0), Material.SHROOMLIGHT);
            s1.scale(0.15f, 0.6f, 0.6f).glow(255, 200, 60).interpolation(6, 0);
            s2.scale(0.15f, 0.6f, 0.6f).glow(255, 200, 60).interpolation(6, 0);
            cracks.add(s1);
            cracks.add(s2);
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            int local = tick % 100;
            if (local < 20) phase = 0;       // mouth opens
            else if (local < 60) phase = 1;  // fire eruption
            else if (local < 70) phase = 2;  // mouth slams
            else phase = 3;                   // walking rock

            if (phase == 0 && local == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.5f);
                mouth.get(0).animateTo(new Vector3f(-0.6f, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 0.8f, 0.3f), 18);
                mouth.get(1).animateTo(new Vector3f(0.6f, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 0.8f, 0.3f), 18);
            }
            if (phase == 2 && local == 60) {
                DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_BRICKS_STEP, 1.3f, 0.5f);
                mouth.get(0).animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 0.8f, 0.3f), 8);
                mouth.get(1).animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1f, 0.8f, 0.3f), 8);
            }

            // Fire column from mouth during phase 1
            if (phase == 1 && tick % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double t = i / 12.0;
                    Location l = center.clone().add(0, 1.0, 1.6 + t * 7);
                    w.spawnParticle(Particle.FLAME, l, 4, 0.5, 0.5, 0.2, 0.05);
                    w.spawnParticle(Particle.LAVA, l, 1, 0.4, 0.4, 0.2, 0);
                }
                if (tick % 8 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.2f, 0.7f);
            }
            // Walking rock (small wobble)
            if (tick % 4 == 0) {
                float lean = (float) Math.sin(tick * 0.1) * 0.08f;
                for (BlockDisplayHandle wall : walls) {
                    wall.animateTo(new Vector3f(0, Math.abs(lean) * 0.3f, 0),
                            new AxisAngle4f(lean, 0, 0, 1),
                            new Vector3f(1, 1, 1), 4);
                }
                for (BlockDisplayHandle f : face) {
                    f.animateTo(new Vector3f(0, Math.abs(lean) * 0.3f, 0),
                            new AxisAngle4f(lean, 0, 0, 1),
                            new Vector3f(1f, 1f, 0.3f), 4);
                }
                if (tick % 32 == 0) DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_BRICKS_STEP, 0.8f, 0.5f);
            }
            // Heat shimmer
            if (tick % 5 == 0) {
                for (BlockDisplayHandle s : cracks) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, s.entity().getLocation(), 2, 0.1, 0.3, 0.1, 0.01);
                }
                w.spawnParticle(Particle.ASH, center.clone().add(0, 0.2, 0), 6, 1.5, 0.1, 1.5, 0.02);
            }

            // Dissipate
            if (tick > dur - 25 && tick % 5 == 0) {
                for (BlockDisplayHandle h : walls) h.animateTo(new Vector3f(0, -2, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : face) h.animateTo(new Vector3f(0, 0, 2), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : mouth) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : core) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
                for (BlockDisplayHandle h : cracks) h.animateTo(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f, 0.05f, 0.05f), 5);
            }
        }

        @Override public AbstractAttack newInstance() { return new SoulFurnace(plugin); }
    }

    // ================================================================
    // 19. THE GUILLOTINED
    //     Headless seated figure: torso + arms + neck stump + blood spray.
    //     Stands, stumbles, then collapses outward in pieces.
    //     Display count: 4 torso + 4 arms (2 segs x 2) + 1 neck + 6 spray
    //     + 4 platform + 4 hip/leg bone = 23. Add 6 chest/shoulder
    //     overlays = 29 displays.
    // ================================================================
    public static class TheGuillotined extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> neckCap = new ArrayList<>();
        private final List<BlockDisplayHandle> spray = new ArrayList<>();
        private final List<BlockDisplayHandle> platform = new ArrayList<>();
        private final List<BlockDisplayHandle> overlays = new ArrayList<>();
        private int phase = 0;
        private float walkOffset = 0;

        public TheGuillotined(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_guillotined", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.5);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(22);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 0.9f, 1.4f);
            // Platform — 4 deepslate tile blocks
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(-0.5 + x, 0, -0.5 + z), Material.DEEPSLATE_TILES);
                    h.scale(1, 0.4f, 1).interpolation(10, 0);
                    platform.add(h);
                }
            }
            // Torso — 4 bone blocks (shoulders + chest)
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(-0.5 + x, 1.4 + y, 0), Material.BONE_BLOCK);
                    h.scale(1, 1, 0.8f).interpolation(8, 0);
                    torso.add(h);
                }
            }
            // 6 chest/shoulder overlay details
            double[][] over = {{-0.5, 1.4, 0.5}, {0.5, 1.4, 0.5}, {-0.5, 2.4, 0.5}, {0.5, 2.4, 0.5},
                    {0, 2.0, 0.8}, {0, 3.1, 0.0}};
            for (double[] p : over) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0], p[1], p[2]), Material.BONE_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).interpolation(8, 0);
                overlays.add(h);
            }
            // Arms — 2 segments per side
            for (int s = 0; s < 2; s++) {
                double sx = (s == 0) ? -1.2 : 1.2;
                BlockDisplayHandle a1 = displayBuilder.spawnBlock(center.clone().add(sx, 1.8, 0), Material.BONE_BLOCK);
                BlockDisplayHandle a2 = displayBuilder.spawnBlock(center.clone().add(sx, 1.0, 0), Material.BONE_BLOCK);
                a1.scale(0.5f, 1.0f, 0.5f).interpolation(8, 0);
                a2.scale(0.5f, 1.0f, 0.5f).interpolation(8, 0);
                arms.add(a1);
                arms.add(a2);
            }
            // Neck stump cap (1 blackstone)
            BlockDisplayHandle ncap = displayBuilder.spawnBlock(center.clone().add(0, 3.4, 0), Material.BLACKSTONE);
            ncap.scale(0.7f, 0.4f, 0.7f).glow(40, 40, 40).interpolation(8, 0);
            neckCap.add(ncap);
            // 6 red glass blood spray arc
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double a = Math.PI * t;
                Location loc = center.clone().add(Math.cos(a) * 0.9, 3.6 + Math.sin(a) * 1.4, Math.sin(a) * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_STAINED_GLASS);
                h.scale(0.3f, 0.3f, 0.3f).rotate((float) a, 0, 0, 1).glow(180, 0, 0).interpolation(8, 0);
                spray.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();

            if (tick < 40) phase = 0;                     // seated
            else if (tick < 90) phase = 1;                // standing
            else if (tick < dur * 0.7) phase = 2;         // stumbling
            else phase = 3;                                // collapse

            if (phase == 1 && tick % 6 == 0) {
                float rise = Math.min(1.0f, (tick - 40) / 50f);
                for (BlockDisplayHandle h : torso) h.animateTo(new Vector3f(0, rise * 1.4f, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(1, 1, 0.8f), 6);
                for (BlockDisplayHandle h : overlays) h.animateTo(new Vector3f(0, rise * 1.4f, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.4f, 0.4f, 0.4f), 6);
                for (BlockDisplayHandle h : neckCap) h.animateTo(new Vector3f(0, rise * 1.4f, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.7f, 0.4f, 0.7f), 6);
                for (BlockDisplayHandle h : spray) h.animateTo(new Vector3f(0, rise * 1.4f, 0), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.3f, 0.3f, 0.3f), 6);
                // arms rotate down to sides
                for (int i = 0; i < arms.size(); i++) {
                    arms.get(i).animateTo(new Vector3f(0, rise * 1.4f, 0),
                            new AxisAngle4f(rise * 0.4f, 0, 0, 1),
                            new Vector3f(0.5f, 1.0f, 0.5f), 6);
                }
            }
            if (phase == 2 && tick % 8 == 0) {
                walkOffset += 0.18f;
                float wobble = (float) Math.sin(tick * 0.2) * 0.15f;
                for (BlockDisplayHandle h : torso) h.animateTo(new Vector3f(0, 1.4f + wobble, walkOffset), new AxisAngle4f(wobble * 0.3f, 0, 0, 1), new Vector3f(1, 1, 0.8f), 8);
                for (BlockDisplayHandle h : overlays) h.animateTo(new Vector3f(0, 1.4f + wobble, walkOffset), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.4f, 0.4f, 0.4f), 8);
                for (BlockDisplayHandle h : neckCap) h.animateTo(new Vector3f(0, 1.4f + wobble, walkOffset), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.7f, 0.4f, 0.7f), 8);
                for (BlockDisplayHandle h : spray) h.animateTo(new Vector3f(0, 1.4f + wobble, walkOffset), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.3f, 0.3f, 0.3f), 8);
                for (BlockDisplayHandle h : arms) h.animateTo(new Vector3f(0, 1.4f + wobble, walkOffset),
                        new AxisAngle4f((float) Math.sin(tick * 0.25) * 0.4f, 1, 0, 0),
                        new Vector3f(0.5f, 1.0f, 0.5f), 8);
                if (tick % 16 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_STEP, 1.0f, 0.6f);
            }
            if (phase == 3 && tick == (int)(dur * 0.7) + 1) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.6f, 0.5f);
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(torso); all.addAll(arms); all.addAll(neckCap);
                all.addAll(spray); all.addAll(overlays);
                for (BlockDisplayHandle h : all) {
                    double a = Math.random() * Math.PI * 2;
                    float fx = (float) Math.cos(a) * (1.5f + (float) Math.random() * 2.5f);
                    float fz = (float) Math.sin(a) * (1.5f + (float) Math.random() * 2.5f);
                    float fy = -1.5f + (float) Math.random() * 1.5f;
                    h.animateTo(new Vector3f(fx, fy + walkOffset, fz + walkOffset),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 0, 1),
                            new Vector3f(0.6f, 0.6f, 0.6f), 18);
                }
            }

            // Particles — neck stump always sprays
            if (tick % 2 == 0) {
                Location l = center.clone().add(0, 3.4 + (phase >= 1 ? 1.4 : 0) + walkOffset, walkOffset);
                w.spawnParticle(Particle.DUST, l, 4, 0.4, 0.6, 0.4,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.4f));
            }
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 3, 0.6, 1, 0.6,
                        new Particle.DustOptions(Color.fromRGB(240, 230, 200), 0.8f));
            }
            if (tick % 70 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 0.6f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new TheGuillotined(plugin); }
    }

    // ================================================================
    // 20. THE OBSIDIAN MOTH
    //     Two large upper wings, two smaller lower wings, tapered body.
    //     Wings flap. Periodic close-and-blast. Impact-only on blast.
    //     Display count: upper wings 2 x 12 (4x6 trimmed by spots) ≈ 24,
    //     lower wings 2 x 6 = 12, body taper 5, end stone spots 6 = 47.
    //     We keep slim layout: upper 2x10, lower 2x5, body 5, spots 6 = 31.
    // ================================================================
    public static class ObsidianMoth extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> upperL = new ArrayList<>();
        private final List<BlockDisplayHandle> upperR = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerL = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerR = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> spots = new ArrayList<>();
        private int phase = 0;
        private float drift = 0;

        public ObsidianMoth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_moth", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.7f);
            double y = 5;
            // Body taper — 5 blackstone segments
            for (int i = 0; i < 5; i++) {
                double t = i / 4.0;
                double yPos = y + 1.5 - t * 3.0;
                float scale = 0.7f - (float) t * 0.3f;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, yPos, 0), Material.BLACKSTONE);
                h.scale(scale, 0.6f, scale * 1.2f).interpolation(8, 0);
                body.add(h);
            }
            // Upper wings — left and right, 10 each (rough panel + crying obsidian veins)
            for (int s = 0; s < 2; s++) {
                double sign = (s == 0) ? -1 : 1;
                List<BlockDisplayHandle> dest = (s == 0) ? upperL : upperR;
                for (int i = 0; i < 10; i++) {
                    double col = i % 5;
                    double row = i / 5;
                    double xOff = sign * (1.0 + col * 0.7);
                    double yOff = 0.5 + row * 0.9;
                    Material m = ((i + s) % 4 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    Location loc = center.clone().add(xOff, y + yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, m);
                    h.scale(0.7f, 0.9f, 0.3f).interpolation(8, 0);
                    if (m == Material.CRYING_OBSIDIAN) h.glow(90, 0, 130);
                    else h.glow(40, 0, 60);
                    dest.add(h);
                }
            }
            // Lower wings — 5 each
            for (int s = 0; s < 2; s++) {
                double sign = (s == 0) ? -1 : 1;
                List<BlockDisplayHandle> dest = (s == 0) ? lowerL : lowerR;
                for (int i = 0; i < 5; i++) {
                    double xOff = sign * (1.0 + (i % 3) * 0.7);
                    double yOff = -0.5 - (i / 3) * 0.7;
                    Material m = ((i + s) % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    Location loc = center.clone().add(xOff, y + yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, m);
                    h.scale(0.6f, 0.7f, 0.3f).interpolation(8, 0);
                    if (m == Material.CRYING_OBSIDIAN) h.glow(90, 0, 130);
                    else h.glow(40, 0, 60);
                    dest.add(h);
                }
            }
            // 6 end-stone wing spots
            double[][] sPos = {
                    {-2.5, y + 1.5, 0}, {-3.4, y + 0.6, 0}, {-2.5, y - 0.5, 0},
                    {2.5, y + 1.5, 0}, {3.4, y + 0.6, 0}, {2.5, y - 0.5, 0}
            };
            for (double[] p : sPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0], p[1], p[2]), Material.END_STONE_BRICKS);
                h.scale(0.5f, 0.5f, 0.4f).glow(240, 230, 200).interpolation(8, 0);
                spots.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int dur = config.getDurationTicks();
            int cycle = 100;
            int local = tick % cycle;

            if (local < 70) phase = 0;        // flap + drift
            else if (local < 80) phase = 1;   // close wings
            else if (local < 90) phase = 2;   // blast open
            else phase = 3;                    // recover

            // Drift
            if (tick % 6 == 0) {
                drift = (float) Math.sin(tick * 0.04) * 1.2f;
                for (BlockDisplayHandle b : body) {
                    float by = (float) Math.sin(tick * 0.08) * 0.2f;
                    b.animateTo(new Vector3f(drift, by, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.6f, 0.7f), 6);
                }
            }

            // Wing flapping
            if (phase == 0 && tick % 3 == 0) {
                float flap = (float) Math.sin(tick * 0.15);
                applyWingTransform(upperL, drift, 0, flap * 0.5f, 1, 1.4f);
                applyWingTransform(upperR, drift, 0, -flap * 0.5f, 1, 1.4f);
                applyWingTransform(lowerL, drift, 0, -flap * 0.4f, 1, 0.8f);
                applyWingTransform(lowerR, drift, 0, flap * 0.4f, 1, 0.8f);
                if (tick % 15 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.9f, 0.7f);
            }
            if (phase == 1 && local == 70) {
                applyWingTransform(upperL, drift, 0, -1.4f, 1, 1.4f);
                applyWingTransform(upperR, drift, 0, 1.4f, 1, 1.4f);
                applyWingTransform(lowerL, drift, 0, 1.2f, 1, 0.8f);
                applyWingTransform(lowerR, drift, 0, -1.2f, 1, 0.8f);
            }
            if (phase == 2 && local == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6f, 0.6f);
                triggerImpactDamage(center.clone().add(drift, 5.5, 0));
                applyWingTransform(upperL, drift, 0, 0.8f, 1, 1.6f);
                applyWingTransform(upperR, drift, 0, -0.8f, 1, 1.6f);
                applyWingTransform(lowerL, drift, 0, -0.6f, 1, 1.0f);
                applyWingTransform(lowerR, drift, 0, 0.6f, 1, 1.0f);
                for (BlockDisplayHandle s : spots) {
                    s.animateTo(new Vector3f(drift, 0, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f), 5);
                }
                w.spawnParticle(Particle.SONIC_BOOM, center.clone().add(drift, 5.5, 0), 1, 0, 0, 0, 0);
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6;
                    Location l = center.clone().add(drift + Math.cos(a) * r, 5 + (Math.random() - 0.5) * 2, Math.sin(a) * r);
                    w.spawnParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0.05);
                }
            }

            // Particles
            if (tick % 3 == 0) {
                for (BlockDisplayHandle s : spots) {
                    Location l = s.entity().getLocation();
                    w.spawnParticle(Particle.PORTAL, l, 4, 0.2, 0.2, 0.2, 0.05);
                }
                for (BlockDisplayHandle l : upperL) {
                    if (Math.random() < 0.15)
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, l.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
                for (BlockDisplayHandle r : upperR) {
                    if (Math.random() < 0.15)
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, r.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
                w.spawnParticle(Particle.SQUID_INK, center.clone().add(drift, 4.5, 0), 2, 0.5, 0.5, 0.5, 0.02);
            }

            if (tick % 60 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.6f);

            if (tick > dur - 30 && tick % 5 == 0) {
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(upperL); all.addAll(upperR); all.addAll(lowerL);
                all.addAll(lowerR); all.addAll(body); all.addAll(spots);
                for (BlockDisplayHandle h : all) {
                    h.animateTo(new Vector3f(drift, 8, 0), new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.05f, 0.05f, 0.05f), 5);
                }
            }
        }

        private void applyWingTransform(List<BlockDisplayHandle> wing, float driftX, float driftY,
                                        float angleZ, int axisDir, float scaleX) {
            for (int i = 0; i < wing.size(); i++) {
                wing.get(i).animateTo(
                        new Vector3f(driftX, driftY, 0),
                        new AxisAngle4f(angleZ, 0, 0, 1),
                        new Vector3f(scaleX * 0.5f, 0.9f, 0.3f), 3);
            }
        }

        @Override public AbstractAttack newInstance() { return new ObsidianMoth(plugin); }
    }
}
