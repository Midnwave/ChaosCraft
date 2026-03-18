package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

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

public final class ColossalMonumental {
    private ColossalMonumental() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ColossusSkull(plugin));
        registry.register(new VoidThrone(plugin));
        registry.register(new AbyssalCathedral(plugin));
        registry.register(new ColossalArm(plugin));
        registry.register(new SunkenTemple(plugin));
        registry.register(new AltarOfFirstVoid(plugin));
        registry.register(new LeviathanSpine(plugin));
        registry.register(new PrisonOfStars(plugin));
        registry.register(new VoidCathedralOrgan(plugin));
        registry.register(new VoidmawSigil(plugin));
    }

    // ================================================================
    // 91. COLOSSUS SKULL — giant cranial structure with animated jaw/eyes
    // ================================================================
    public static class ColossusSkull extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> dome = new ArrayList<>();
        private final List<BlockDisplayHandle> jaw = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeGlass = new ArrayList<>();
        private final List<BlockDisplayHandle> brow = new ArrayList<>();
        private final List<BlockDisplayHandle> cheeks = new ArrayList<>();
        private final List<BlockDisplayHandle> molars = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final List<Location> domeBase = new ArrayList<>();
        private final List<Location> jawBase = new ArrayList<>();
        private boolean jawOpen = false;
        private int jawPhaseStart = 0;

        public ColossusSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("colossus_skull", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location c) {
            this.center = c.clone();
            World w = c.getWorld();
            if (w == null) return;

            // Dome: 8 obsidian
            double[][] domeOff = {
                {0,4,0},{-1,3,-1},{1,3,-1},{-1,3,1},{1,3,1},
                {-2,2,0},{2,2,0},{0,2,-2}
            };
            for (double[] o : domeOff) {
                Location loc = c.clone().add(o[0], o[1], o[2]);
                domeBase.add(loc.clone());
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f,1.0f,1.0f).glow(80,0,120).interpolation(3,0);
                dome.add(h); all.add(h); spawnedEntities.add(h.entity());
            }

            // Brow: 3 black_concrete
            double[][] browOff = {{-1,3,-0.5},{0,3,-0.5},{1,3,-0.5}};
            for (double[] o : browOff) {
                Location loc = c.clone().add(o[0], o[1], o[2]);
                domeBase.add(loc.clone());
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(1.0f,0.5f,0.5f).glow(30,0,60).interpolation(3,0);
                brow.add(h); all.add(h); spawnedEntities.add(h.entity());
            }

            // Eyes: 2 dark_prismarine + 2 amethyst
            Location el = c.clone().add(-1.5,2.5,-1);
            Location er = c.clone().add(1.5,2.5,-1);
            BlockDisplayHandle ehl = displayBuilder.spawnBlock(el, Material.DARK_PRISMARINE);
            ehl.scale(0.8f,0.8f,0.8f).glow(0,200,255).interpolation(3,0);
            all.add(ehl); spawnedEntities.add(ehl.entity());
            BlockDisplayHandle ehr = displayBuilder.spawnBlock(er, Material.DARK_PRISMARINE);
            ehr.scale(0.8f,0.8f,0.8f).glow(0,200,255).interpolation(3,0);
            all.add(ehr); spawnedEntities.add(ehr.entity());

            Location al = c.clone().add(-1.5,2.5,-0.5);
            Location ar = c.clone().add(1.5,2.5,-0.5);
            BlockDisplayHandle ahl = displayBuilder.spawnBlock(al, Material.AMETHYST_BLOCK);
            ahl.scale(0.6f,0.6f,0.6f).glow(128,0,255).interpolation(2,0);
            eyes.add(ahl); all.add(ahl); spawnedEntities.add(ahl.entity());
            BlockDisplayHandle ahr = displayBuilder.spawnBlock(ar, Material.AMETHYST_BLOCK);
            ahr.scale(0.6f,0.6f,0.6f).glow(128,0,255).interpolation(2,0);
            eyes.add(ahr); all.add(ahr); spawnedEntities.add(ahr.entity());

            // Glass eyes: 2 magenta_stained_glass
            BlockDisplayHandle gl = displayBuilder.spawnBlock(c.clone().add(-1.5,2.5,-1.2), Material.MAGENTA_STAINED_GLASS);
            gl.scale(0.5f,0.5f,0.2f).glow(200,0,200).interpolation(3,0);
            eyeGlass.add(gl); all.add(gl); spawnedEntities.add(gl.entity());
            BlockDisplayHandle gr = displayBuilder.spawnBlock(c.clone().add(1.5,2.5,-1.2), Material.MAGENTA_STAINED_GLASS);
            gr.scale(0.5f,0.5f,0.2f).glow(200,0,200).interpolation(3,0);
            eyeGlass.add(gr); all.add(gr); spawnedEntities.add(gr.entity());

            // Jaw: 4 blackstone + 2 crying_obsidian
            double[][] jawOff = {{-1,0,0},{1,0,0},{0,0,-1},{0,0,1}};
            for (double[] o : jawOff) {
                Location loc = c.clone().add(o[0], o[1], o[2]);
                jawBase.add(loc.clone());
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(1.0f,1.0f,1.0f).glow(50,0,80).interpolation(3,0);
                jaw.add(h); all.add(h); spawnedEntities.add(h.entity());
            }
            Location cl1 = c.clone().add(-1,0.5,-0.5);
            Location cl2 = c.clone().add(1,0.5,-0.5);
            jawBase.add(cl1.clone()); jawBase.add(cl2.clone());
            BlockDisplayHandle cry1 = displayBuilder.spawnBlock(cl1, Material.CRYING_OBSIDIAN);
            cry1.scale(0.5f,0.8f,0.5f).glow(100,0,200).interpolation(3,0);
            jaw.add(cry1); all.add(cry1); spawnedEntities.add(cry1.entity());
            BlockDisplayHandle cry2 = displayBuilder.spawnBlock(cl2, Material.CRYING_OBSIDIAN);
            cry2.scale(0.5f,0.8f,0.5f).glow(100,0,200).interpolation(3,0);
            jaw.add(cry2); all.add(cry2); spawnedEntities.add(cry2.entity());

            // Cheeks: 2 netherite
            BlockDisplayHandle ck1 = displayBuilder.spawnBlock(c.clone().add(-2,2,0), Material.NETHERITE_BLOCK);
            ck1.scale(0.9f,0.9f,0.9f).glow(60,0,100).interpolation(3,0);
            cheeks.add(ck1); all.add(ck1); spawnedEntities.add(ck1.entity());
            BlockDisplayHandle ck2 = displayBuilder.spawnBlock(c.clone().add(2,2,0), Material.NETHERITE_BLOCK);
            ck2.scale(0.9f,0.9f,0.9f).glow(60,0,100).interpolation(3,0);
            cheeks.add(ck2); all.add(ck2); spawnedEntities.add(ck2.entity());

            // Molars: 2 polished_blackstone
            BlockDisplayHandle m1 = displayBuilder.spawnBlock(c.clone().add(-0.5,0.2,-1), Material.POLISHED_BLACKSTONE);
            m1.scale(0.5f,0.5f,0.5f).glow(40,0,60).interpolation(3,0);
            molars.add(m1); all.add(m1); spawnedEntities.add(m1.entity());
            BlockDisplayHandle m2 = displayBuilder.spawnBlock(c.clone().add(0.5,0.2,-1), Material.POLISHED_BLACKSTONE);
            m2.scale(0.5f,0.5f,0.5f).glow(40,0,60).interpolation(3,0);
            molars.add(m2); all.add(m2); spawnedEntities.add(m2.entity());

            // Nose: 1 black_glazed_terracotta
            BlockDisplayHandle nose = displayBuilder.spawnBlock(c.clone().add(0,1.5,-1), Material.BLACK_GLAZED_TERRACOTTA);
            nose.scale(0.6f,0.6f,0.4f).glow(20,0,40).interpolation(3,0);
            all.add(nose); spawnedEntities.add(nose.entity());
        }

        @Override
        protected void onTick(int t) {
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Skull rocks on Y axis (200-tick period)
            float yRock = (float) Math.sin(t * 0.031) * 5f;
            float yRad = (float) Math.toRadians(yRock);

            // Breathing scale (80-tick period)
            float breathe = 1.0f + 0.1f * (float) Math.sin(t * 0.079);

            // Animate all dome/brow/cheek/molar/nose blocks
            int domeIdx = 0;
            for (BlockDisplayHandle h : dome) {
                if (domeIdx < domeBase.size()) {
                    Location base = domeBase.get(domeIdx);
                    double rx = (base.getX() - center.getX()) * Math.cos(yRad) - (base.getZ() - center.getZ()) * Math.sin(yRad);
                    double rz = (base.getX() - center.getX()) * Math.sin(yRad) + (base.getZ() - center.getZ()) * Math.cos(yRad);
                    h.entity().teleport(center.clone().add(rx, base.getY() - center.getY(), rz));
                    h.scale(breathe, breathe, breathe);
                }
                domeIdx++;
            }

            // Jaw chew: every 120 ticks, animate open then close over 20 ticks
            int jawCycle = t % 120;
            float jawDrop = 0f;
            if (jawCycle < 20) {
                jawDrop = 0.8f * (jawCycle / 20.0f);
                if (!jawOpen && jawCycle == 0) {
                    w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
                    jawOpen = true;
                }
            } else {
                jawOpen = false;
            }
            for (int i = 0; i < jaw.size() && i < jawBase.size(); i++) {
                Location jb = jawBase.get(i);
                jaw.get(i).entity().teleport(center.clone().add(
                    jb.getX() - center.getX(),
                    jb.getY() - center.getY() - jawDrop,
                    jb.getZ() - center.getZ()
                ));
            }

            // Eye amethyst pulse (40-tick sine, scale 1.0-1.5)
            float eyeScale = 0.6f + 0.3f * (float)((Math.sin(t * 0.157) + 1.0) * 0.5);
            for (BlockDisplayHandle h : eyes) {
                h.scale(eyeScale, eyeScale, eyeScale).interpolation(2, 0);
            }

            // Particles
            if (t % 4 == 0) {
                // Dripping obsidian tear from crying_obsidian positions
                Location jawCenter = center.clone().add(0, 0.5, -0.5);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, jawCenter, 3, 0.5, 0.2, 0.5, 0);
            }
            if (t % 6 == 0) {
                // Purple dust from eye sockets
                for (BlockDisplayHandle h : eyes) {
                    DisplayBuilder.dustParticles(h.entity().getLocation(), 5, 0.3, 80, 0, 120, 1.0f);
                }
            }
            if (t % 8 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 0.3, 0), 4, 0.8, 0.2, 0.8, 0.01);
            }
            if (t % 10 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 4.5, 0), 6, 0.5, 0.3, 0.5, 0.05);
            }

            // Sounds
            if (t % 30 == 0) w.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.8f);
            if (t % 80 == 0) w.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.6f);
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ColossusSkull(plugin);
        }
    }

    // ================================================================
    // 92. VOID THRONE — rotating obsidian throne with dominance pulses
    // ================================================================
    public static class VoidThrone extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final List<Location> allBase = new ArrayList<>();
        private final List<BlockDisplayHandle> spires = new ArrayList<>();
        private final List<Location> spireBase = new ArrayList<>();
        private final List<BlockDisplayHandle> runes = new ArrayList<>();
        private float rotAngle = 0f;
        private int dominanceStart = -1;

        public VoidThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_throne", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(700);
        }

        private BlockDisplayHandle spawnAndTrack(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(sx, sy, sz).glow(r, g, b).interpolation(3, 0);
            all.add(h); allBase.add(loc.clone()); spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onSpawn(Location c) {
            this.center = c.clone();
            World w = c.getWorld();
            if (w == null) return;

            // Seat: 4 netherite
            double[][] seatOff = {{-1,0,-1},{-1,0,0},{1,0,-1},{1,0,0}};
            for (double[] o : seatOff) spawnAndTrack(c.clone().add(o[0],o[1],o[2]), Material.NETHERITE_BLOCK, 1,1,1, 60,0,100);

            // Throne back: 6 obsidian
            double[][] backOff = {{-1,1,1},{-1,2,1},{0,2,1},{0,3,1},{1,2,1},{1,1,1}};
            for (double[] o : backOff) spawnAndTrack(c.clone().add(o[0],o[1],o[2]), Material.OBSIDIAN, 1,1,1, 50,0,80);

            // Spires: 4 polished_blackstone
            double[][] spireOff = {{-1,3,1},{0,4,1},{1,3,1},{0,3.5,1}};
            for (double[] o : spireOff) {
                Location loc = c.clone().add(o[0],o[1],o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.7f,0.7f,0.7f).glow(80,0,160).interpolation(3,0);
                spires.add(h); spireBase.add(loc.clone());
                all.add(h); allBase.add(loc.clone()); spawnedEntities.add(h.entity());
            }

            // Spire tips: 4 amethyst
            double[][] tipOff = {{-1,3.8,1},{0,4.8,1},{1,3.8,1},{0,4.3,1}};
            for (double[] o : tipOff) spawnAndTrack(c.clone().add(o[0],o[1],o[2]), Material.AMETHYST_BLOCK, 0.5f,0.5f,0.5f, 128,0,255);

            // Armrests: 4 netherite
            double[][] armOff = {{-2,0.5,-1},{-2,0.5,0},{2,0.5,-1},{2,0.5,0}};
            for (double[] o : armOff) spawnAndTrack(c.clone().add(o[0],o[1],o[2]), Material.NETHERITE_BLOCK, 1,0.5f,1, 60,0,100);

            // Crying trim: 3 crying_obsidian
            double[][] cryOff = {{-1,0,-1.2},{0,0,-1.2},{1,0,-1.2}};
            for (double[] o : cryOff) spawnAndTrack(c.clone().add(o[0],o[1],o[2]), Material.CRYING_OBSIDIAN, 0.5f,0.3f,0.3f, 100,0,200);

            // Runes: 2 dark_prismarine
            Location rl = c.clone().add(-2,0.7,-0.5);
            Location rr = c.clone().add(2,0.7,-0.5);
            BlockDisplayHandle r1 = displayBuilder.spawnBlock(rl, Material.DARK_PRISMARINE);
            r1.scale(0.6f,0.6f,0.1f).glow(0,200,255).interpolation(2,0);
            runes.add(r1); all.add(r1); allBase.add(rl.clone()); spawnedEntities.add(r1.entity());
            BlockDisplayHandle r2 = displayBuilder.spawnBlock(rr, Material.DARK_PRISMARINE);
            r2.scale(0.6f,0.6f,0.1f).glow(0,200,255).interpolation(2,0);
            runes.add(r2); all.add(r2); allBase.add(rr.clone()); spawnedEntities.add(r2.entity());

            // Soul soil: 1
            spawnAndTrack(c.clone().add(0,-0.5,-1.5), Material.SOUL_SOIL, 1,0.3f,1, 60,80,120);

            // Glass windows: 2 purple_stained_glass
            spawnAndTrack(c.clone().add(-0.5,1.5,1.1), Material.PURPLE_STAINED_GLASS, 0.8f,1,0.1f, 100,0,200);
            spawnAndTrack(c.clone().add(0.5,1.5,1.1), Material.PURPLE_STAINED_GLASS, 0.8f,1,0.1f, 100,0,200);
        }

        @Override
        protected void onTick(int t) {
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Constant slow Y rotation: 0.2 deg/tick
            rotAngle += 0.2f;
            float rad = (float) Math.toRadians(rotAngle);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            // Dominance assertion: scale 1.0->1.4 over 30t then back, every 180t
            if (t % 180 == 0) dominanceStart = t;
            float domScale = 1.0f;
            if (dominanceStart >= 0) {
                int dp = t - dominanceStart;
                if (dp <= 30) domScale = 1.0f + 0.4f * (dp / 30.0f);
                else if (dp <= 60) domScale = 1.4f - 0.4f * ((dp - 30) / 30.0f);
                else dominanceStart = -1;
            }

            for (int i = 0; i < all.size() && i < allBase.size(); i++) {
                Location base = allBase.get(i);
                double bx = base.getX() - center.getX();
                double bz = base.getZ() - center.getZ();
                double rx = bx * cos - bz * sin;
                double rz = bx * sin + bz * cos;
                Location tgt = center.clone().add(rx, base.getY() - center.getY(), rz);
                all.get(i).entity().teleport(tgt);
                if (domScale != 1.0f) {
                    Transformation tr = all.get(i).entity().getTransformation();
                    Vector3f sc = tr.getScale();
                    all.get(i).entity().setTransformation(new Transformation(
                        tr.getTranslation(), tr.getLeftRotation(),
                        new Vector3f(sc.x * domScale, sc.y * domScale, sc.z * domScale),
                        tr.getRightRotation()
                    ));
                }
            }

            // Spire oscillation ±0.2 Y with 20-tick phase offsets
            for (int i = 0; i < spires.size() && i < spireBase.size(); i++) {
                double osc = 0.2 * Math.sin((t + i * 20) * 0.157);
                Location sb = spireBase.get(i);
                double bx = sb.getX() - center.getX();
                double bz = sb.getZ() - center.getZ();
                double rx = bx * cos - bz * sin;
                double rz = bx * sin + bz * cos;
                spires.get(i).entity().teleport(center.clone().add(rx, sb.getY() - center.getY() + osc, rz));
            }

            // Runes pulse scale (60-tick sine)
            float runeScale = 0.6f + 0.3f * (float)((Math.sin(t * 0.105) + 1) * 0.5);
            for (BlockDisplayHandle h : runes) h.scale(runeScale, runeScale, 0.1f).interpolation(2,0);

            // Particles
            if (t % 5 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0,-0.3,-1.5), 2, 0.2,0.1,0.2, 0.01);
            }
            if (t % 6 == 0) {
                for (BlockDisplayHandle h : runes) {
                    DisplayBuilder.dustParticles(h.entity().getLocation(), 3, 0.2, 150, 0, 200, 1.2f);
                }
            }
            if (t % 8 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0,1.5,1.2), 3, 0.3,0.5,0.1, 0.01);
            }
            if (t % 7 == 0) {
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, center.clone().add(0,0,-1.2), 2, 0.5,0.1,0.3, 0);
            }

            // Sounds
            if (t % 40 == 0) w.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.7f);
            if (t % 180 == 0) w.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidThrone(plugin);
        }
    }

    // ================================================================
    // 93. ABYSSAL CATHEDRAL — gothic two-tower cathedral structure
    // ================================================================
    public static class AbyssalCathedral extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> gargoyles = new ArrayList<>();
        private final List<Location> gargoyleBase = new ArrayList<>();
        private final List<BlockDisplayHandle> roseWindow = new ArrayList<>();
        private BlockDisplayHandle altar;
        private final List<BlockDisplayHandle> towerL = new ArrayList<>();
        private final List<BlockDisplayHandle> towerR = new ArrayList<>();
        private float roseAngle = 0f;

        public AbyssalCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_cathedral", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(800);
        }

        private BlockDisplayHandle sp(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(sx,sy,sz).glow(r,g,b).interpolation(3,0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onSpawn(Location c) {
            this.center = c.clone();
            World w = c.getWorld();
            if (w == null) return;

            // Left tower: 5 blackstone
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = sp(c.clone().add(-3,y,0), Material.BLACKSTONE, 1,1,1, 40,0,60);
                towerL.add(h);
            }
            // Right tower: 5 blackstone
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = sp(c.clone().add(3,y,0), Material.BLACKSTONE, 1,1,1, 40,0,60);
                towerR.add(h);
            }

            // Nave walls: 4 obsidian
            double[][] naveOff = {{-1.5,0,0},{-1.5,1,0},{1.5,0,0},{1.5,1,0}};
            for (double[] o : naveOff) sp(c.clone().add(o[0],o[1],o[2]), Material.OBSIDIAN, 1,1,0.5f, 50,0,80);

            // Buttresses: 4 cobbled_deepslate
            double[][] buttOff = {{-2.5,2,-1.5},{-2.5,2,1.5},{2.5,2,-1.5},{2.5,2,1.5}};
            for (double[] o : buttOff) sp(c.clone().add(o[0],o[1],o[2]), Material.COBBLED_DEEPSLATE, 0.8f,0.8f,0.8f, 30,0,50);

            // Rose window: 4 polished_blackstone + 2 purple glass
            double[][] roseOff = {{0,3.5,0},{-0.5,3,0},{0.5,3,0},{0,3,0}};
            for (double[] o : roseOff) {
                Location loc = c.clone().add(o[0],o[1],o[2]);
                BlockDisplayHandle h = sp(loc, Material.POLISHED_BLACKSTONE, 0.5f,0.5f,0.3f, 60,0,100);
                roseWindow.add(h);
            }
            sp(c.clone().add(-0.3,3.2,0), Material.PURPLE_STAINED_GLASS, 0.4f,0.4f,0.2f, 100,0,200);
            sp(c.clone().add(0.3,3.2,0), Material.PURPLE_STAINED_GLASS, 0.4f,0.4f,0.2f, 100,0,200);

            // Gargoyles: 4 crying_obsidian
            double[][] gargOff = {{-3,4.5,0},{3,4.5,0},{-2.5,2.5,-1.5},{2.5,2.5,1.5}};
            for (double[] o : gargOff) {
                Location loc = c.clone().add(o[0],o[1],o[2]);
                gargoyleBase.add(loc.clone());
                BlockDisplayHandle h = sp(loc, Material.CRYING_OBSIDIAN, 0.7f,0.7f,0.7f, 100,0,200);
                gargoyles.add(h);
            }

            // Door: 2 netherite
            sp(c.clone().add(0,0,0), Material.NETHERITE_BLOCK, 0.8f,1,0.5f, 60,0,100);
            sp(c.clone().add(0,1,0), Material.NETHERITE_BLOCK, 0.8f,1,0.5f, 60,0,100);

            // Altar
            altar = sp(c.clone().add(0,0.5,-1), Material.AMETHYST_BLOCK, 0.8f,0.5f,0.8f, 128,0,255);
            sp(c.clone().add(0,0,-1), Material.DARK_PRISMARINE, 1,0.3f,1, 0,200,255);
            sp(c.clone().add(0,1.5,-1), Material.BLACK_GLAZED_TERRACOTTA, 0.6f,0.6f,0.3f, 20,0,40);
        }

        @Override
        protected void onTick(int t) {
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Gargoyles lean outward ±10 deg X over 120-tick sine
            for (int i = 0; i < gargoyles.size() && i < gargoyleBase.size(); i++) {
                float leanDeg = 10f * (float) Math.sin(t * 0.0524); // ~120 tick period
                float leanRad = (float) Math.toRadians(leanDeg);
                gargoyles.get(i).entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f,-0.5f,-0.5f),
                    new AxisAngle4f(leanRad, 1, 0, 0),
                    new Vector3f(0.7f, 0.7f, 0.7f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Rose window rotates: 0.5 deg/tick — teleport 4 blocks in circle
            roseAngle += 0.5f;
            float rRad = (float) Math.toRadians(roseAngle);
            double[] roseAngles = {0, Math.PI/2, Math.PI, 3*Math.PI/2};
            for (int i = 0; i < roseWindow.size(); i++) {
                double a = roseAngles[i] + rRad;
                double rx = Math.cos(a) * 0.5;
                double ry = Math.sin(a) * 0.5 + 3.2;
                roseWindow.get(i).entity().teleport(center.clone().add(rx, ry, 0));
            }

            // Altar flickers scale 1.0-1.3 every 8 ticks
            if (t % 8 == 0 && altar != null) {
                float altScale = 0.8f + 0.25f * (float)((Math.sin(t * 0.785) + 1) * 0.5);
                altar.scale(altScale, altScale * 0.6f, altScale).interpolation(2, 0);
            }

            // Towers sway ±1 deg X, opposite phase, 300-tick period
            float swayL = (float) Math.toRadians(Math.sin(t * 0.021) * 1.0);
            float swayR = (float) Math.toRadians(Math.sin(t * 0.021 + Math.PI) * 1.0);
            for (int i = 0; i < towerL.size(); i++) {
                towerL.get(i).entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f,-0.5f,-0.5f),
                    new AxisAngle4f(swayL, 1, 0, 0),
                    new Vector3f(1,1,1),
                    new AxisAngle4f(0,0,1,0)
                ));
                towerR.get(i).entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f,-0.5f,-0.5f),
                    new AxisAngle4f(swayR, 1, 0, 0),
                    new Vector3f(1,1,1),
                    new AxisAngle4f(0,0,1,0)
                ));
            }

            // Particles
            if (t % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0,2,0), 4, 1.5, 30, 0, 50, 1.0f);
            }
            if (t % 8 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0,0.5,0), 3, 0.3,0.5,0.1, 0.01);
            }
            if (t % 5 == 0) {
                for (BlockDisplayHandle h : gargoyles) {
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, h.entity().getLocation(), 2, 0.2,0.3,0.2, 0);
                }
            }
            if (t % 10 == 0) {
                for (BlockDisplayHandle h : roseWindow) {
                    w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 1, 0.1,0.1,0.1, 0.02);
                }
            }
            if (t % 12 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(-3,5,0), 3, 0.3,0.3,0.3, 0.02);
                w.spawnParticle(Particle.ASH, center.clone().add(3,5,0), 3, 0.3,0.3,0.3, 0.02);
            }

            // Sounds
            if (t % 120 == 0) w.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.7f, 0.6f);
            if (t % 80 == 2) w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            if (t % 60 == 10) w.playSound(center, Sound.AMBIENT_CAVE, 0.3f, 0.7f);
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new AbyssalCathedral(plugin);
        }
    }

    // ================================================================
    // 94. COLOSSAL ARM — massive reaching arm with wave-flex fingers
    // ================================================================
    public static class ColossalArm extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> upperArm = new ArrayList<>();
        private final List<BlockDisplayHandle> foreArm = new ArrayList<>();
        private final List<BlockDisplayHandle> palm = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> fingertips = new ArrayList<>();
        private final List<BlockDisplayHandle> veins = new ArrayList<>();
        private BlockDisplayHandle palmGlass;
        private final List<Location> upperArmBase = new ArrayList<>();
        private final List<Location> foreArmBase = new ArrayList<>();
        private final List<Location> palmBase = new ArrayList<>();
        private final List<List<Location>> fingerBase = new ArrayList<>();
        private final List<Location> fingertipBase = new ArrayList<>();

        public ColossalArm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("colossal_arm", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(600);
        }

        private BlockDisplayHandle spawnTrack(Location loc, Material mat, float s, int r, int g, int b,
                                               List<BlockDisplayHandle> list, List<Location> bases) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(s,s,s).glow(r,g,b).interpolation(3,0);
            list.add(h); bases.add(loc.clone()); spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onSpawn(Location c) {
            this.center = c.clone();
            World w = c.getWorld();
            if (w == null) return;

            // Upper arm: 4 obsidian horizontal at shoulder height
            double[][] uaOff = {{0,3,-1},{0,3,0},{0,3,1},{0,3,2}};
            for (double[] o : uaOff) spawnTrack(c.clone().add(o[0],o[1],o[2]), Material.OBSIDIAN, 1.0f, 60,0,100, upperArm, upperArmBase);

            // Forearm (angled down): 4 netherite
            double[][] faOff = {{0,2.5,3},{0,2.3,4},{0,2.1,5},{0,1.9,6}};
            for (double[] o : faOff) spawnTrack(c.clone().add(o[0],o[1],o[2]), Material.NETHERITE_BLOCK, 0.9f, 50,0,80, foreArm, foreArmBase);

            // Palm: 4 blackstone
            double[][] palmOff = {{-1,1.5,7},{0,1.5,7},{1,1.5,7},{0,1.5,8}};
            for (double[] o : palmOff) spawnTrack(c.clone().add(o[0],o[1],o[2]), Material.BLACKSTONE, 1.0f, 40,0,70, palm, palmBase);

            // Fingers: 5 groups of 3 polished_blackstone
            double[] fxOff = {-2,-1,0,1,2};
            for (int fi = 0; fi < 5; fi++) {
                List<BlockDisplayHandle> fg = new ArrayList<>();
                List<Location> fb = new ArrayList<>();
                for (int fk = 0; fk < 3; fk++) {
                    Location loc = c.clone().add(fxOff[fi], 1.5, 9 + fk);
                    spawnTrack(loc, Material.POLISHED_BLACKSTONE, 0.8f, 35,0,60, fg, fb);
                }
                fingers.add(fg); fingerBase.add(fb);
            }

            // Fingertips: 5 crying_obsidian
            for (int fi = 0; fi < 5; fi++) {
                Location loc = c.clone().add(fxOff[fi], 1.5, 12);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f,0.6f,0.6f).glow(100,0,200).interpolation(2,0);
                fingertips.add(h); fingertipBase.add(loc.clone()); spawnedEntities.add(h.entity());
            }

            // Joints: 3 cobbled_deepslate
            Location j1 = c.clone().add(0,2.5,3.5);
            Location j2 = c.clone().add(0,2.0,5.5);
            Location j3 = c.clone().add(0,1.6,7.5);
            for (Location jl : new Location[]{j1,j2,j3}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(jl, Material.COBBLED_DEEPSLATE);
                h.scale(1.1f,1.1f,1.1f).glow(40,0,60).interpolation(3,0);
                spawnedEntities.add(h.entity());
            }

            // Veins: 2 dark_prismarine
            Location v1 = c.clone().add(0,1.0,4);
            Location v2 = c.clone().add(0,1.0,6);
            for (Location vl : new Location[]{v1,v2}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(vl, Material.DARK_PRISMARINE);
                h.scale(0.4f,0.3f,1.5f).glow(0,200,255).interpolation(2,0);
                veins.add(h); spawnedEntities.add(h.entity());
            }

            // Palm aperture glass
            Location pg = c.clone().add(0,1.5,7.5);
            palmGlass = displayBuilder.spawnBlock(pg, Material.MAGENTA_STAINED_GLASS);
            palmGlass.scale(0.5f,0.5f,0.1f).glow(200,0,200).interpolation(2,0);
            spawnedEntities.add(palmGlass.entity());

            // Knuckles: 2 amethyst
            Location k1 = c.clone().add(-1,1.8,8.5);
            Location k2 = c.clone().add(1,1.8,8.5);
            for (Location kl : new Location[]{k1,k2}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(kl, Material.AMETHYST_BLOCK);
                h.scale(0.5f,0.5f,0.5f).glow(128,0,255).interpolation(2,0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int t) {
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Arm sways ±8 deg Z — rotate all arm blocks around Z
            float swayDeg = (float)(Math.sin(t * 0.035) * 8.0);
            float swayRad = (float) Math.toRadians(swayDeg);
            float cos = (float) Math.cos(swayRad);
            float sin = (float) Math.sin(swayRad);

            // Helper: apply Z-rotation to a list of handles + bases
            rotateGroup(upperArm, upperArmBase, cos, sin);
            rotateGroup(foreArm, foreArmBase, cos, sin);
            rotateGroup(palm, palmBase, cos, sin);
            for (int fi = 0; fi < fingers.size(); fi++) rotateGroup(fingers.get(fi), fingerBase.get(fi), cos, sin);

            // Finger wave: each finger tip translates -0.4 Z with 10-tick phase offset, 60-tick period
            for (int fi = 0; fi < fingers.size() && fi < fingerBase.size(); fi++) {
                List<BlockDisplayHandle> fg = fingers.get(fi);
                List<Location> fb = fingerBase.get(fi);
                if (fg.size() < 3 || fb.size() < 3) continue;
                // Only move tip block (index 2)
                double flex = -0.4 * ((Math.sin((t + fi * 10) * 0.105) + 1) * 0.5);
                Location base = fb.get(2);
                double bx = base.getX() - center.getX();
                double by = base.getY() - center.getY();
                double bz = base.getZ() - center.getZ() + flex;
                double rx = bx * cos - by * sin;
                double ry = bx * sin + by * cos;
                fg.get(2).entity().teleport(center.clone().add(rx, ry, bz));
            }

            // Palm glass pulse scale 1.0-1.5, 50-tick sine
            if (palmGlass != null) {
                float pg = 0.5f + 0.5f * (float)((Math.sin(t * 0.126) + 1) * 0.5);
                palmGlass.scale(pg, pg, 0.1f).interpolation(2, 0);
            }

            // Veins brighten down arm every 40 ticks
            for (int i = 0; i < veins.size(); i++) {
                float vs = 0.4f + 0.4f * (float)((Math.sin((t - i * 20) * 0.157) + 1) * 0.5);
                veins.get(i).scale(vs * 0.4f / 0.4f, 0.3f, 1.5f).interpolation(2, 0);
                if (vs > 0.6f) {
                    DisplayBuilder.dustParticles(veins.get(i).entity().getLocation(), 2, 0.3, 0, 200, 255, 1.0f);
                }
            }

            // Particles
            if (t % 4 == 0) {
                for (BlockDisplayHandle h : fingertips) {
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, h.entity().getLocation(), 2, 0.1,0.2,0.1, 0);
                }
            }
            if (t % 6 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, palmGlass != null ? palmGlass.entity().getLocation() : center, 3, 0.2,0.1,0.2, 0.01);
            }
            if (t % 7 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0,2,3), 4, 1.5, 60, 0, 100, 1.0f);
            }
            // End rod from fingertips during flex (every 30t = ~flex peak)
            if (t % 30 == 0) {
                for (BlockDisplayHandle h : fingertips) {
                    w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 3, 0.1,0.3,0.1, 0.05);
                }
            }

            // Sounds
            if (t % 60 == 0) w.playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 0.7f, 0.6f);
            if (t % 20 == 0) w.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.3f, 1.0f);
            if (t % 180 == 0) w.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.6f);
        }

        private void rotateGroup(List<BlockDisplayHandle> group, List<Location> bases, float cos, float sin) {
            for (int i = 0; i < group.size() && i < bases.size(); i++) {
                Location base = bases.get(i);
                double bx = base.getX() - center.getX();
                double by = base.getY() - center.getY();
                double rx = bx * cos - by * sin;
                double ry = bx * sin + by * cos;
                group.get(i).entity().teleport(center.clone().add(rx, ry, base.getZ() - center.getZ()));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ColossalArm(plugin);
        }
    }

    // ================================================================
    // 95. SUNKEN TEMPLE — ancient pillared ruin slowly sinking
    // ================================================================
    public static class SunkenTemple extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<Location> allBase = new ArrayList<>();
        private final List<BlockDisplayHandle> floorRunes = new ArrayList<>();
        private final List<BlockDisplayHandle> pedAmethysts = new ArrayList<>();
        private final List<BlockDisplayHandle> columnCapitals = new ArrayList<>();
        private float pedOrbitAngle = 0f;
        private float sinkY = 0f;

        public SunkenTemple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sunken_temple", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(7.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(700);
        }

        private BlockDisplayHandle addBlock(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(sx,sy,sz).glow(r,g,b).interpolation(3,0);
            allBlocks.add(h); allBase.add(loc.clone()); spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onSpawn(Location c) {
            this.center = c.clone();
            World w = c.getWorld();
            if (w == null) return;

            // Platform: 9 cobbled_deepslate
            double[][] platOff = {
                {-2,0,-2},{-2,0,0},{-2,0,2},
                {0,0,-2},{0,0,0},{0,0,2},
                {2,0,-2},{2,0,0},{2,0,2}
            };
            for (double[] o : platOff) addBlock(c.clone().add(o[0],o[1],o[2]), Material.COBBLED_DEEPSLATE, 1,0.3f,1, 30,20,50);

            // 4 pillars (3 blocks each)
            double[][] pillarCorners = {{-2,-2},{-2,2},{2,-2},{2,2}};
            for (double[] pc : pillarCorners) {
                for (int y = 1; y <= 3; y++) {
                    addBlock(c.clone().add(pc[0],y,pc[1]), Material.BLACKSTONE, 0.8f,1,0.8f, 40,0,60);
                }
            }

            // Entablature: 4 obsidian
            addBlock(c.clone().add(-2,4,0), Material.OBSIDIAN, 1,0.5f,1, 50,0,80);
            addBlock(c.clone().add(2,4,0), Material.OBSIDIAN, 1,0.5f,1, 50,0,80);
            addBlock(c.clone().add(0,4,-2), Material.OBSIDIAN, 1,0.5f,1, 50,0,80);
            addBlock(c.clone().add(0,4,2), Material.OBSIDIAN, 1,0.5f,1, 50,0,80);

            // Pediment: 3 netherite (triangle)
            addBlock(c.clone().add(-1,5,0), Material.NETHERITE_BLOCK, 1,1,0.5f, 60,0,100);
            addBlock(c.clone().add(0,6,0), Material.NETHERITE_BLOCK, 1,1,0.5f, 60,0,100);
            addBlock(c.clone().add(1,5,0), Material.NETHERITE_BLOCK, 1,1,0.5f, 60,0,100);

            // Floor runes: 3 dark_prismarine
            Location fr0 = c.clone().add(0,0.1,0);
            Location fr1 = c.clone().add(-1,0.1,0);
            Location fr2 = c.clone().add(1,0.1,0);
            for (Location fl : new Location[]{fr0, fr1, fr2}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(fl, Material.DARK_PRISMARINE);
                h.scale(0.9f,0.1f,0.9f).glow(0,200,255).interpolation(2,0);
                floorRunes.add(h); allBlocks.add(h); allBase.add(fl.clone()); spawnedEntities.add(h.entity());
            }

            // Column capitals: 4 crying_obsidian at pillar tops (y=3)
            for (double[] pc : pillarCorners) {
                Location capLoc = c.clone().add(pc[0],3.5,pc[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(capLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.9f,0.5f,0.9f).glow(100,0,200).interpolation(3,0);
                columnCapitals.add(h); allBlocks.add(h); allBase.add(capLoc.clone()); spawnedEntities.add(h.entity());
            }

            // Friezes: 2 black_glazed_terracotta
            addBlock(c.clone().add(-2,4.3,0), Material.BLACK_GLAZED_TERRACOTTA, 0.5f,0.5f,0.5f, 20,0,40);
            addBlock(c.clone().add(2,4.3,0), Material.BLACK_GLAZED_TERRACOTTA, 0.5f,0.5f,0.5f, 20,0,40);

            // Pediment amethysts: 2
            Location pa0 = c.clone().add(-1,5.5,0);
            Location pa1 = c.clone().add(1,5.5,0);
            for (Location pl : new Location[]{pa0, pa1}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(pl, Material.AMETHYST_BLOCK);
                h.scale(0.5f,0.5f,0.5f).glow(128,0,255).interpolation(2,0);
                pedAmethysts.add(h); allBlocks.add(h); allBase.add(pl.clone()); spawnedEntities.add(h.entity());
            }

            // Soul hearth
            addBlock(c.clone().add(0,0.2,0), Material.SOUL_SOIL, 1,0.2f,1, 60,80,120);

            // Statue base
            addBlock(c.clone().add(0,0.3,-1), Material.POLISHED_BLACKSTONE, 0.8f,0.4f,0.8f, 40,0,60);
        }

        @Override
        protected void onTick(int t) {
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Temple sinks: translate Y by (t % 400) * 0.01, snap at 400
            int cycle = t % 400;
            sinkY = cycle * 0.01f;
            for (int i = 0; i < allBlocks.size() && i < allBase.size(); i++) {
                Location base = allBase.get(i);
                allBlocks.get(i).entity().teleport(center.clone().add(
                    base.getX() - center.getX(),
                    base.getY() - center.getY() - sinkY,
                    base.getZ() - center.getZ()
                ));
            }

            // Floor runes pulse in sequence: rune[(t/15)%3] scales 1.0->1.4 over 10 ticks
            int activeRune = (t / 15) % 3;
            int runePhase = t % 15;
            for (int i = 0; i < floorRunes.size(); i++) {
                float rs;
                if (i == activeRune && runePhase < 10) {
                    rs = 0.9f + 0.4f * (runePhase / 10.0f);
                    if (runePhase == 0) {
                        w.spawnParticle(Particle.REVERSE_PORTAL, floorRunes.get(i).entity().getLocation(), 5, 0.4,0.1,0.4, 0.01);
                        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.9f + i * 0.1f);
                    }
                } else {
                    rs = 0.9f;
                }
                floorRunes.get(i).scale(rs, 0.1f, rs).interpolation(2, 0);
            }

            // Pediment amethysts orbit each other at 0.2 deg/tick
            pedOrbitAngle += 0.2f;
            float oRad = (float) Math.toRadians(pedOrbitAngle);
            float ox = (float) Math.cos(oRad) * 1.0f;
            float oz = (float) Math.sin(oRad) * 0.3f;
            if (pedAmethysts.size() >= 2) {
                pedAmethysts.get(0).entity().teleport(center.clone().add(ox, 5.5 - sinkY, oz));
                pedAmethysts.get(1).entity().teleport(center.clone().add(-ox, 5.5 - sinkY, -oz));
            }

            // Column capitals oscillate ±5 deg X over 200-tick sine
            float capLean = (float) Math.toRadians(Math.sin(t * 0.0314) * 5.0);
            for (BlockDisplayHandle h : columnCapitals) {
                h.entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f,-0.5f,-0.5f),
                    new AxisAngle4f(capLean, 1, 0, 0),
                    new Vector3f(0.9f, 0.5f, 0.9f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Particles
            if (t % 8 == 0) {
                // Falling water from entablature edges
                w.spawnParticle(Particle.FALLING_WATER, center.clone().add(-2, 4.2 - sinkY, 0), 2, 0.2,0.1,0.2, 0.01);
                w.spawnParticle(Particle.FALLING_WATER, center.clone().add(2, 4.2 - sinkY, 0), 2, 0.2,0.1,0.2, 0.01);
            }
            if (t % 5 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3 - sinkY, 0), 2, 0.3,0.1,0.3, 0.01);
            }
            if (t % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 2 - sinkY, 0), 4, 2.0, 45, 40, 55, 1.0f);
            }

            // Sounds
            if (t % 40 == 0) w.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 0.5f, 0.8f);
            if (t % 20 == 0) w.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.3f, 1.0f);
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SunkenTemple(plugin);
        }
    }

    // ================================================================
    // 96. ALTAR OF FIRST VOID — Netherite altar with orbiting amethyst
    // ================================================================
    public static class AltarOfFirstVoid extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<BlockDisplayHandle> steps = new ArrayList<>();
        private final List<BlockDisplayHandle> border = new ArrayList<>();
        private final List<BlockDisplayHandle> runes = new ArrayList<>();
        private final List<BlockDisplayHandle> terracotta = new ArrayList<>();
        private BlockDisplayHandle altarFocus;
        private BlockDisplayHandle orbitAmethyst;
        private BlockDisplayHandle soulSoil;
        private BlockDisplayHandle reredos;
        private float orbitAngle = 0;

        public AltarOfFirstVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("altar_of_first_void", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 netherite base slabs
            double[][] baseOff = {{-1,0,0},{0,0,0},{1,0,0},{-1,0,1},{0,0,1},{1,0,1}};
            for (double[] o : baseOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(o[0], 0, o[2]), Material.NETHERITE_BLOCK);
                h.scale(1.0f, 0.3f, 1.0f).glow(40, 0, 80).interpolation(2, 0);
                base.add(h); spawnedEntities.add(h.entity());
            }
            // 4 obsidian side panels
            double[][] panelOff = {{-2,0.3,0.5},{2,0.3,0.5},{-0.5,0.3,-1},{-0.5,0.3,2}};
            for (double[] o : panelOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(o[0], o[1], o[2]), Material.OBSIDIAN);
                h.scale(0.3f, 1.5f, 1.0f).glow(60, 0, 120).interpolation(2, 0);
                panels.add(h); spawnedEntities.add(h.entity());
            }
            // 4 polished blackstone steps
            double[][] stepOff = {{-1,-0.3,2.2},{0,-0.3,2.2},{1,-0.3,2.2},{0,-0.6,2.8}};
            for (double[] o : stepOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(o[0], o[1], o[2]), Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.3f, 0.8f).glow(50, 0, 100).interpolation(2, 0);
                steps.add(h); spawnedEntities.add(h.entity());
            }
            // 3 crying obsidian border
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120);
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0.1, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.2f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                border.add(h); spawnedEntities.add(h.entity());
            }
            // 2 dark prismarine runes
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i == 0 ? -1.5 : 1.5, 0.35, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.4f, 0.1f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                runes.add(h); spawnedEntities.add(h.entity());
            }
            // 2 black glazed terracotta panels
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(i == 0 ? -2 : 2, 1.0, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.5f, 0.8f, 0.5f).glow(30, 0, 60).interpolation(2, 0);
                terracotta.add(h); spawnedEntities.add(h.entity());
            }
            // Amethyst altar focus
            altarFocus = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0.5), Material.AMETHYST_BLOCK);
            altarFocus.scale(0.5f, 0.6f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
            spawnedEntities.add(altarFocus.entity());
            // Orbiting amethyst
            orbitAmethyst = displayBuilder.spawnBlock(center.clone().add(1, 1.5, 0.5), Material.AMETHYST_BLOCK);
            orbitAmethyst.scale(0.3f, 0.3f, 0.3f).glow(200, 50, 255).interpolation(2, 0);
            spawnedEntities.add(orbitAmethyst.entity());
            // Soul soil step
            soulSoil = displayBuilder.spawnBlock(center.clone().add(0, -0.3, -1), Material.SOUL_SOIL);
            soulSoil.scale(1.0f, 0.3f, 0.8f).glow(60, 40, 20).interpolation(2, 0);
            spawnedEntities.add(soulSoil.entity());
            // Magenta stained glass reredos
            reredos = displayBuilder.spawnBlock(center.clone().add(0, 0.5, -0.8), Material.MAGENTA_STAINED_GLASS);
            reredos.scale(2.0f, 2.5f, 0.1f).glow(200, 0, 255).interpolation(2, 0);
            spawnedEntities.add(reredos.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(center, 40, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int t = ticksAlive;

            // Orbiting amethyst circles at 0.8 deg/tick, rising height
            orbitAngle += 0.8f;
            float oRad = (float) Math.toRadians(orbitAngle);
            float riseY = 1.5f + (float) Math.sin(t * 0.02) * 0.8f;
            orbitAmethyst.entity().teleport(c.clone().add(Math.cos(oRad) * 1.5, riseY, 0.5 + Math.sin(oRad) * 1.5));

            // Runes pulse alternating every 20 ticks
            int runeIdx = (t / 20) % 2;
            for (int i = 0; i < runes.size(); i++) {
                float s = (i == runeIdx) ? 0.4f + 0.2f * ((t % 20) / 20.0f) : 0.4f;
                runes.get(i).scale(s, 0.1f, s).interpolation(2, 0);
            }

            // Reredos scales pulse
            float rScale = 2.0f + 0.3f * (float) Math.sin(t * 0.05);
            reredos.scale(rScale, 2.5f, 0.1f).interpolation(3, 0);

            // Crying obsidian tear particles
            if (t % 6 == 0) {
                for (BlockDisplayHandle h : border) {
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, h.entity().getLocation().add(0, 0.3, 0), 2, 0.1, 0.2, 0.1, 0);
                }
            }
            // Soul fire from soul soil
            if (t % 8 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, soulSoil.entity().getLocation().add(0.5, 0.4, 0.4), 3, 0.3, 0.1, 0.3, 0.01);
            }
            // Reverse portal from reredos
            if (t % 5 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, reredos.entity().getLocation().add(1, 1.2, 0), 6, 0.5, 0.8, 0.05, 0.02);
            }
            if (t % 30 == 0) {
                w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AltarOfFirstVoid(plugin); }
    }

    // ================================================================
    // 97. LEVIATHAN SPINE — Skeletal vertebral column with undulation
    // ================================================================
    public static class LeviathanSpine extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> vertebrae = new ArrayList<>();
        private final List<BlockDisplayHandle> neuralSpines = new ArrayList<>();
        private final List<BlockDisplayHandle> ribStubs = new ArrayList<>();
        private final List<BlockDisplayHandle> discSpaces = new ArrayList<>();
        private final List<BlockDisplayHandle> marrow = new ArrayList<>();
        private final List<BlockDisplayHandle> atlas = new ArrayList<>();
        private BlockDisplayHandle tail;
        private final double[] baseY = new double[15];

        public LeviathanSpine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("leviathan_spine", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 vertebrae along X with S-curve Y
            for (int i = 0; i < 15; i++) {
                double x = (i - 7) * 0.8;
                double y = Math.sin(i * 0.45) * 1.2;
                baseY[i] = y;
                Material mat = (i >= 5 && i <= 9) ? Material.NETHERITE_BLOCK : Material.BLACK_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, y, 0), mat);
                h.scale(0.7f, 0.5f, 0.7f).glow(40, 0, 80).interpolation(2, 0);
                vertebrae.add(h); spawnedEntities.add(h.entity());
            }
            // 8 neural spines (obsidian) on top of every other vertebra
            for (int i = 0; i < 8; i++) {
                int vi = i * 2;
                if (vi >= 15) vi = 14;
                double x = (vi - 7) * 0.8;
                double y = baseY[vi] + 0.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, y, 0), Material.OBSIDIAN);
                h.scale(0.15f, 0.8f, 0.15f).glow(60, 0, 130).interpolation(2, 0);
                neuralSpines.add(h); spawnedEntities.add(h.entity());
            }
            // 4 rib stubs (blackstone) extending Z from vertebrae 3,6,9,12
            int[] ribVerts = {3, 6, 9, 12};
            for (int vi : ribVerts) {
                double x = (vi - 7) * 0.8;
                double y = baseY[vi];
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, y - 0.2, 0.6), Material.BLACKSTONE);
                h.scale(0.2f, 0.3f, 1.0f).glow(50, 0, 100).interpolation(2, 0);
                ribStubs.add(h); spawnedEntities.add(h.entity());
            }
            // 3 crying obsidian disc spaces
            int[] discVerts = {4, 7, 11};
            for (int vi : discVerts) {
                double x = (vi - 7) * 0.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, baseY[vi] - 0.1, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.15f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                discSpaces.add(h); spawnedEntities.add(h.entity());
            }
            // 3 dark prismarine marrow
            int[] marrowVerts = {2, 7, 12};
            for (int vi : marrowVerts) {
                double x = (vi - 7) * 0.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, baseY[vi], 0), Material.DARK_PRISMARINE);
                h.scale(0.25f, 0.25f, 0.25f).glow(0, 200, 255).interpolation(2, 0);
                marrow.add(h); spawnedEntities.add(h.entity());
            }
            // 2 amethyst at atlas (first two vertebrae)
            for (int i = 0; i < 2; i++) {
                double x = (i - 7) * 0.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, baseY[i] + 0.4, 0), Material.AMETHYST_BLOCK);
                h.scale(0.35f, 0.35f, 0.35f).glow(180, 0, 255).interpolation(2, 0);
                atlas.add(h); spawnedEntities.add(h.entity());
            }
            // Soul soil tail
            tail = displayBuilder.spawnBlock(center.clone().add((14 - 7) * 0.8 + 0.5, baseY[14] - 0.2, 0), Material.SOUL_SOIL);
            tail.scale(0.6f, 0.3f, 0.4f).glow(60, 40, 20).interpolation(2, 0);
            spawnedEntities.add(tail.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.4f);
            DisplayBuilder.purpleDust(center, 35, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int t = ticksAlive;

            // Undulating wave traveling along spine
            for (int i = 0; i < vertebrae.size(); i++) {
                double x = (i - 7) * 0.8;
                double waveY = baseY[i] + Math.sin((t - i * 4) * 0.1) * 0.4;
                vertebrae.get(i).entity().teleport(c.clone().add(x, waveY, 0));
            }
            // Neural spines wave with slight delay
            for (int i = 0; i < neuralSpines.size(); i++) {
                int vi = Math.min(i * 2, 14);
                double x = (vi - 7) * 0.8;
                double waveY = baseY[vi] + Math.sin((t - vi * 4) * 0.1) * 0.4 + 0.6;
                float lean = (float) Math.sin((t - vi * 4) * 0.1) * 0.3f;
                neuralSpines.get(i).entity().teleport(c.clone().add(x, waveY, lean * 0.2));
            }
            // Rib stubs flare outward periodically
            int[] ribVerts = {3, 6, 9, 12};
            for (int i = 0; i < ribStubs.size(); i++) {
                float flare = 1.0f + 0.4f * (float) Math.sin(t * 0.08 + i * 1.5);
                ribStubs.get(i).scale(0.2f, 0.3f, flare).interpolation(3, 0);
            }
            // Marrow blue dust particles
            if (t % 5 == 0) {
                for (BlockDisplayHandle h : marrow) {
                    DisplayBuilder.dustParticles(h.entity().getLocation(), 3, 0.3, 0, 180, 255, 1.0f);
                }
            }
            // Ash along spine
            if (t % 8 == 0) {
                double rx = (Math.random() * 15 - 7) * 0.8;
                w.spawnParticle(Particle.ASH, c.clone().add(rx, 1.5, 0), 4, 0.3, 0.3, 0.3, 0);
            }
            if (t % 40 == 0) {
                w.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LeviathanSpine(plugin); }
    }

    // ================================================================
    // 98. PRISON OF STARS — Rotating cage spheres with counter-rotation
    // ================================================================
    public static class PrisonOfStars extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerCage = new ArrayList<>();
        private final List<BlockDisplayHandle> hoops = new ArrayList<>();
        private final List<BlockDisplayHandle> innerPillars = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingObs = new ArrayList<>();
        private final List<BlockDisplayHandle> equatorial = new ArrayList<>();
        private final List<BlockDisplayHandle> poles = new ArrayList<>();
        private BlockDisplayHandle imprisonedCenter;
        private BlockDisplayHandle orbitGlass;
        private float outerAngle = 0;
        private float innerAngle = 0;
        private final float[] hoopAngles = new float[3];

        public PrisonOfStars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prison_of_stars", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(9.0);
            config.setDamageRadius(11.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 obsidian outer cage ribs (sphere at radius 4)
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30);
                double x = Math.cos(a) * 4.0;
                double z = Math.sin(a) * 4.0;
                double y = Math.sin(a * 2) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, y + 2, z), Material.OBSIDIAN);
                h.scale(0.5f, 1.8f, 0.5f).glow(60, 0, 130).interpolation(2, 0);
                outerCage.add(h); spawnedEntities.add(h.entity());
            }
            // 12 blackstone hoops (3 perpendicular rings × 4 blocks)
            double[][] axes = {{1,0,0},{0,1,0},{0,0,1}};
            for (int ring = 0; ring < 3; ring++) {
                for (int j = 0; j < 4; j++) {
                    double a = Math.toRadians(j * 90);
                    double r = 2.5;
                    double lx, ly, lz;
                    if (ring == 0) { lx = 0; ly = Math.cos(a) * r; lz = Math.sin(a) * r; }
                    else if (ring == 1) { lx = Math.cos(a) * r; ly = 0; lz = Math.sin(a) * r; }
                    else { lx = Math.cos(a) * r; ly = Math.sin(a) * r; lz = 0; }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(lx, ly + 2, lz), Material.BLACKSTONE);
                    h.scale(0.4f, 0.4f, 0.4f).glow(50, 0, 100).interpolation(2, 0);
                    hoops.add(h); spawnedEntities.add(h.entity());
                }
            }
            // 8 netherite inner pillars at radius 1.5
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                double x = Math.cos(a) * 1.5;
                double z = Math.sin(a) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 1, z), Material.NETHERITE_BLOCK);
                h.scale(0.3f, 2.0f, 0.3f).glow(40, 0, 80).interpolation(2, 0);
                innerPillars.add(h); spawnedEntities.add(h.entity());
            }
            // 4 crying obsidian
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(i * 90 + 45);
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * 2.2, 2, Math.sin(a) * 2.2), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                cryingObs.add(h); spawnedEntities.add(h.entity());
            }
            // 3 dark prismarine equatorial
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(i * 120);
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * 3.0, 2, Math.sin(a) * 3.0), Material.DARK_PRISMARINE);
                h.scale(0.5f, 0.3f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                equatorial.add(h); spawnedEntities.add(h.entity());
            }
            // 2 amethyst poles (top and bottom)
            for (int i = 0; i < 2; i++) {
                double y = i == 0 ? 5.0 : -1.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                poles.add(h); spawnedEntities.add(h.entity());
            }
            // Magenta stained glass imprisoned center
            imprisonedCenter = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.MAGENTA_STAINED_GLASS);
            imprisonedCenter.scale(0.8f, 0.8f, 0.8f).glow(200, 0, 255).interpolation(2, 0);
            spawnedEntities.add(imprisonedCenter.entity());
            // Purple stained glass orbiting
            orbitGlass = displayBuilder.spawnBlock(center.clone().add(2, 2, 0), Material.PURPLE_STAINED_GLASS);
            orbitGlass.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(orbitGlass.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(center, 50, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int t = ticksAlive;

            // Outer cage rotates Y 0.6 deg/tick
            outerAngle += 0.6f;
            float outerRad = (float) Math.toRadians(outerAngle);
            for (int i = 0; i < outerCage.size(); i++) {
                double baseA = Math.toRadians(i * 30) + outerRad;
                double x = Math.cos(baseA) * 4.0;
                double z = Math.sin(baseA) * 4.0;
                double y = Math.sin(baseA * 2) * 1.5 + 2;
                outerCage.get(i).entity().teleport(c.clone().add(x, y, z));
            }
            // Inner pillars counter-rotate 1 deg/tick
            innerAngle -= 1.0f;
            float innerRad = (float) Math.toRadians(innerAngle);
            for (int i = 0; i < innerPillars.size(); i++) {
                double baseA = Math.toRadians(i * 45) + innerRad;
                innerPillars.get(i).entity().teleport(c.clone().add(Math.cos(baseA) * 1.5, 1, Math.sin(baseA) * 1.5));
            }
            // 3 hoop rings rotate on independent axes
            for (int ring = 0; ring < 3; ring++) {
                hoopAngles[ring] += (ring + 1) * 0.4f;
                float hr = (float) Math.toRadians(hoopAngles[ring]);
                for (int j = 0; j < 4; j++) {
                    int idx = ring * 4 + j;
                    if (idx >= hoops.size()) break;
                    double a = Math.toRadians(j * 90) + hr;
                    double r = 2.5;
                    double lx, ly, lz;
                    if (ring == 0) { lx = 0; ly = Math.cos(a) * r; lz = Math.sin(a) * r; }
                    else if (ring == 1) { lx = Math.cos(a) * r; ly = 0; lz = Math.sin(a) * r; }
                    else { lx = Math.cos(a) * r; ly = Math.sin(a) * r; lz = 0; }
                    hoops.get(idx).entity().teleport(c.clone().add(lx, ly + 2, lz));
                }
            }
            // Imprisoned center pulses scale
            float cScale = 0.8f + 0.3f * (float) Math.sin(t * 0.08);
            imprisonedCenter.scale(cScale, cScale, cScale).interpolation(3, 0);

            // Amethyst poles pulse scale
            for (int i = 0; i < poles.size(); i++) {
                float pScale = 0.5f + 0.2f * (float) Math.sin(t * 0.06 + i * Math.PI);
                poles.get(i).scale(pScale, pScale, pScale).interpolation(3, 0);
            }
            // Orbiting glass
            float glassA = (float) Math.toRadians(t * 1.5);
            orbitGlass.entity().teleport(c.clone().add(Math.cos(glassA) * 3, 2 + Math.sin(t * 0.04) * 0.5, Math.sin(glassA) * 3));

            if (t % 6 == 0) {
                DisplayBuilder.purpleDust(c.clone().add(0, 2, 0), 5, 2.0);
            }
            if (t % 4 == 0) {
                for (BlockDisplayHandle h : cryingObs) {
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, h.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }
            if (t % 30 == 0) {
                w.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrisonOfStars(plugin); }
    }

    // ================================================================
    // 99. VOID CATHEDRAL ORGAN — Pipe organ with arpeggiated animation
    // ================================================================
    public static class VoidCathedralOrgan extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pipes = new ArrayList<>();
        private final List<BlockDisplayHandle> casework = new ArrayList<>();
        private final List<BlockDisplayHandle> bassPipes = new ArrayList<>();
        private final List<BlockDisplayHandle> mouths = new ArrayList<>();
        private final List<BlockDisplayHandle> windChests = new ArrayList<>();
        private final List<BlockDisplayHandle> pipeTops = new ArrayList<>();
        private final List<BlockDisplayHandle> keyboards = new ArrayList<>();
        private final List<BlockDisplayHandle> roseWindows = new ArrayList<>();
        private BlockDisplayHandle desk;
        private BlockDisplayHandle bench;
        private final float[] pipeBaseHeight = new float[10];

        public VoidCathedralOrgan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_cathedral_organ", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(5.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 blackstone pipes (2 banks × 5, varying heights)
            for (int bank = 0; bank < 2; bank++) {
                for (int i = 0; i < 5; i++) {
                    int idx = bank * 5 + i;
                    double x = (bank == 0 ? -2.5 : 2.5);
                    double z = (i - 2) * 0.7;
                    float h = 2.0f + i * 0.5f;
                    pipeBaseHeight[idx] = h;
                    BlockDisplayHandle bh = displayBuilder.spawnBlock(center.clone().add(x, 0, z), Material.BLACKSTONE);
                    bh.scale(0.25f, h, 0.25f).glow(50, 0, 100).interpolation(2, 0);
                    pipes.add(bh); spawnedEntities.add(bh.entity());
                }
            }
            // 10 obsidian casework flanking pipes
            for (int bank = 0; bank < 2; bank++) {
                for (int i = 0; i < 5; i++) {
                    double x = (bank == 0 ? -3.2 : 3.2);
                    double z = (i - 2) * 0.7;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 0, z), Material.OBSIDIAN);
                    h.scale(0.3f, 2.5f, 0.3f).glow(60, 0, 130).interpolation(2, 0);
                    casework.add(h); spawnedEntities.add(h.entity());
                }
            }
            // 4 netherite bass pipes (tall, center)
            for (int i = 0; i < 4; i++) {
                double x = (i - 1.5) * 0.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 0, -1.5), Material.NETHERITE_BLOCK);
                h.scale(0.35f, 4.5f, 0.35f).glow(40, 0, 80).interpolation(2, 0);
                bassPipes.add(h); spawnedEntities.add(h.entity());
            }
            // 3 polished blackstone mouths
            for (int i = 0; i < 3; i++) {
                double x = (i - 1) * 1.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 2.0, -1.0), Material.POLISHED_BLACKSTONE);
                h.scale(0.6f, 0.2f, 0.3f).glow(50, 0, 100).interpolation(2, 0);
                mouths.add(h); spawnedEntities.add(h.entity());
            }
            // 2 dark prismarine wind chests
            for (int i = 0; i < 2; i++) {
                double x = i == 0 ? -1.5 : 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, -0.3, 0), Material.DARK_PRISMARINE);
                h.scale(1.5f, 0.4f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
                windChests.add(h); spawnedEntities.add(h.entity());
            }
            // 3 crying obsidian pipe tops
            for (int i = 0; i < 3; i++) {
                double x = (i - 1) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 3.5, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                pipeTops.add(h); spawnedEntities.add(h.entity());
            }
            // 3 amethyst keyboards
            for (int i = 0; i < 3; i++) {
                double z = 1.5 + i * 0.4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 0.8 - i * 0.15, z), Material.AMETHYST_BLOCK);
                h.scale(2.0f, 0.1f, 0.35f).glow(180, 0, 255).interpolation(2, 0);
                keyboards.add(h); spawnedEntities.add(h.entity());
            }
            // 2 purple stained glass rose windows
            for (int i = 0; i < 2; i++) {
                double x = i == 0 ? -3.5 : 3.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 3.0, 0), Material.PURPLE_STAINED_GLASS);
                h.scale(1.0f, 1.0f, 0.1f).glow(128, 0, 255).interpolation(2, 0);
                roseWindows.add(h); spawnedEntities.add(h.entity());
            }
            // Black glazed terracotta desk
            desk = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 2.0), Material.BLACK_GLAZED_TERRACOTTA);
            desk.scale(2.5f, 0.8f, 0.6f).glow(30, 0, 60).interpolation(2, 0);
            spawnedEntities.add(desk.entity());
            // Soul soil bench
            bench = displayBuilder.spawnBlock(center.clone().add(0, 0, 2.8), Material.SOUL_SOIL);
            bench.scale(1.2f, 0.5f, 0.5f).glow(60, 40, 20).interpolation(2, 0);
            spawnedEntities.add(bench.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.3f);
            DisplayBuilder.purpleDust(center, 30, 2.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int t = ticksAlive;

            // Arpeggiated pipe wave: pipes extend sequentially
            for (int i = 0; i < pipes.size(); i++) {
                int phase = (t + i * 6) % 60;
                float extend = phase < 20 ? pipeBaseHeight[i] + (phase / 20.0f) * 1.0f :
                               phase < 40 ? pipeBaseHeight[i] + 1.0f - ((phase - 20) / 20.0f) * 1.0f :
                               pipeBaseHeight[i];
                pipes.get(i).scale(0.25f, extend, 0.25f).interpolation(3, 0);
            }
            // Keyboard press animation: sequential key dip
            int activeKey = (t / 8) % 3;
            for (int i = 0; i < keyboards.size(); i++) {
                float dip = (i == activeKey) ? -0.05f : 0f;
                Location kLoc = c.clone().add(0, 0.8 - i * 0.15 + dip, 1.5 + i * 0.4);
                keyboards.get(i).entity().teleport(kLoc);
            }
            // Note particles from pipe tops
            if (t % 8 == 0) {
                for (BlockDisplayHandle h : pipeTops) {
                    w.spawnParticle(Particle.NOTE, h.entity().getLocation().add(0, 0.5, 0), 2, 0.2, 0.3, 0.2, 0);
                }
            }
            // Amethyst resonate sounds at varying pitch
            if (t % 15 == 0) {
                int noteIdx = (t / 15) % 5;
                float pitch = 0.5f + noteIdx * 0.2f;
                w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
            }
            // Rose windows pulse glow
            if (t % 10 == 0) {
                DisplayBuilder.purpleDust(c.clone().add(0, 3, 0), 4, 1.5);
            }
            // Crying obsidian tears
            if (t % 6 == 0) {
                for (BlockDisplayHandle h : pipeTops) {
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCathedralOrgan(plugin); }
    }

    // ================================================================
    // 100. VOIDMAW SIGIL — Layered rotating ritual sigil with crescendo
    // ================================================================
    public static class VoidmawSigil extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> star = new ArrayList<>();
        private final List<BlockDisplayHandle> diamond = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        private final List<BlockDisplayHandle> resonanceNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> starPoints = new ArrayList<>();
        private BlockDisplayHandle purpleGlass1, purpleGlass2;
        private BlockDisplayHandle magentaGlass1, magentaGlass2;
        private BlockDisplayHandle eyePupil;
        private BlockDisplayHandle nucleus;
        private float outerAngle = 0;
        private float starAngle = 0;
        private float diamondAngle = 0;

        public VoidmawSigil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_sigil", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(16.0);
            config.setDurationTicks(1500);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 obsidian outer ring (radius 8)
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(i * 22.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * 8, 0.1, Math.sin(a) * 8), Material.OBSIDIAN);
                h.scale(0.8f, 0.3f, 0.8f).glow(60, 0, 130).interpolation(2, 0);
                outerRing.add(h); spawnedEntities.add(h.entity());
            }
            // 12 netherite 6-pointed star (radius 3.5-4.3)
            for (int i = 0; i < 12; i++) {
                double a = Math.toRadians(i * 30);
                double r = (i % 2 == 0) ? 4.3 : 3.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r), Material.NETHERITE_BLOCK);
                h.scale(0.5f, 0.2f, 0.5f).glow(40, 0, 80).interpolation(2, 0);
                star.add(h); spawnedEntities.add(h.entity());
            }
            // 8 crying obsidian inner diamond (radius ~2)
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45);
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * 2, 0.3, Math.sin(a) * 2), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.2f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                diamond.add(h); spawnedEntities.add(h.entity());
            }
            // 6 blackstone radial connectors
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(i * 60);
                double r = 5.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r), Material.BLACKSTONE);
                h.scale(0.3f, 0.15f, 1.5f).glow(50, 0, 100).interpolation(2, 0);
                connectors.add(h); spawnedEntities.add(h.entity());
            }
            // 4 dark prismarine resonance nodes (N, E, S, W)
            double[][] nodeOff = {{0,0,6},{6,0,0},{0,0,-6},{-6,0,0}};
            for (double[] o : nodeOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(o[0], 0.4, o[2]), Material.DARK_PRISMARINE);
                h.scale(0.6f, 0.3f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                resonanceNodes.add(h); spawnedEntities.add(h.entity());
            }
            // 3 amethyst star points
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(i * 120);
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(Math.cos(a) * 4.3, 0.5, Math.sin(a) * 4.3), Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.4f, 0.5f).glow(180, 0, 255).interpolation(2, 0);
                starPoints.add(h); spawnedEntities.add(h.entity());
            }
            // 2 purple + 2 magenta glass center
            purpleGlass1 = displayBuilder.spawnBlock(center.clone().add(-0.3, 0.4, -0.3), Material.PURPLE_STAINED_GLASS);
            purpleGlass1.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(purpleGlass1.entity());
            purpleGlass2 = displayBuilder.spawnBlock(center.clone().add(0.3, 0.4, 0.3), Material.PURPLE_STAINED_GLASS);
            purpleGlass2.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(purpleGlass2.entity());
            magentaGlass1 = displayBuilder.spawnBlock(center.clone().add(0.3, 0.4, -0.3), Material.MAGENTA_STAINED_GLASS);
            magentaGlass1.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 255).interpolation(2, 0);
            spawnedEntities.add(magentaGlass1.entity());
            magentaGlass2 = displayBuilder.spawnBlock(center.clone().add(-0.3, 0.4, 0.3), Material.MAGENTA_STAINED_GLASS);
            magentaGlass2.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 255).interpolation(2, 0);
            spawnedEntities.add(magentaGlass2.entity());
            // Polished blackstone eye pupil
            eyePupil = displayBuilder.spawnBlock(center.clone().add(0, 0.6, 0), Material.POLISHED_BLACKSTONE);
            eyePupil.scale(0.3f, 0.3f, 0.3f).glow(50, 0, 100).interpolation(2, 0);
            spawnedEntities.add(eyePupil.entity());
            // Netherite nucleus
            nucleus = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.NETHERITE_BLOCK);
            nucleus.scale(0.4f, 0.4f, 0.4f).glow(40, 0, 80).interpolation(2, 0);
            spawnedEntities.add(nucleus.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.4f);
            DisplayBuilder.purpleDust(center, 60, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int t = ticksAlive;
            int dur = config.getDurationTicks();

            // Outer ring rotates Y 0.3 deg/tick
            outerAngle += 0.3f;
            float oRad = (float) Math.toRadians(outerAngle);
            for (int i = 0; i < outerRing.size(); i++) {
                double baseA = Math.toRadians(i * 22.5) + oRad;
                outerRing.get(i).entity().teleport(c.clone().add(Math.cos(baseA) * 8, 0.1, Math.sin(baseA) * 8));
            }
            // Star counter-rotates 0.6 deg/tick
            starAngle -= 0.6f;
            float sRad = (float) Math.toRadians(starAngle);
            for (int i = 0; i < star.size(); i++) {
                double baseA = Math.toRadians(i * 30) + sRad;
                double r = (i % 2 == 0) ? 4.3 : 3.5;
                star.get(i).entity().teleport(c.clone().add(Math.cos(baseA) * r, 0.2, Math.sin(baseA) * r));
            }
            // Diamond rotates 1.2 deg/tick
            diamondAngle += 1.2f;
            float dRad = (float) Math.toRadians(diamondAngle);
            for (int i = 0; i < diamond.size(); i++) {
                double baseA = Math.toRadians(i * 45) + dRad;
                diamond.get(i).entity().teleport(c.clone().add(Math.cos(baseA) * 2, 0.3, Math.sin(baseA) * 2));
            }
            // Resonance nodes pulse N→E→S→W sequence
            int activeNode = (t / 20) % 4;
            for (int i = 0; i < resonanceNodes.size(); i++) {
                float ns = (i == activeNode) ? 0.6f + 0.3f * ((t % 20) / 20.0f) : 0.6f;
                resonanceNodes.get(i).scale(ns, 0.3f, ns).interpolation(2, 0);
                if (i == activeNode && t % 20 == 0) {
                    DisplayBuilder.dustParticles(resonanceNodes.get(i).entity().getLocation(), 5, 0.5, 0, 200, 255, 1.0f);
                }
            }
            // Nucleus scales 1→3 over final 300 ticks
            int remaining = dur - t;
            if (remaining <= 300 && remaining > 0) {
                float progress = 1.0f - (remaining / 300.0f);
                float nScale = 0.4f + progress * 2.6f;
                nucleus.scale(nScale, nScale, nScale).interpolation(3, 0);
                // Intensifying glow
                if (remaining % 10 == 0) {
                    DisplayBuilder.purpleDust(c.clone().add(0, 0.8, 0), (int)(3 + progress * 15), 1.0 + progress * 3.0);
                }
            }
            // Sonic boom on final tick
            if (remaining == 1) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.3f);
                DisplayBuilder.purpleDust(c, 80, 8.0);
                w.spawnParticle(Particle.SONIC_BOOM, c.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            }

            // Ambient particles
            if (t % 8 == 0) {
                for (BlockDisplayHandle h : diamond) {
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0);
                }
            }
            if (t % 5 == 0) {
                w.spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.6, 0), 4, 0.3, 0.2, 0.3, 0.01);
            }
            if (t % 35 == 0) {
                w.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawSigil(plugin); }
    }
}
