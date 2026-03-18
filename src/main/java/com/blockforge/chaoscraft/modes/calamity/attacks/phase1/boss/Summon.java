package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

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
 * GROUP 4: SUMMON
 * The Voidmaw calls forth creatures from the abyss to assist.
 * Smaller, terrible things that serve their master.
 * Attacks 31-40 from the Voidmaw boss design document.
 */
public final class Summon {

    private Summon() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidSpawnEruption(plugin));
        registry.register(new TendrilWall(plugin));
        registry.register(new TheHatching(plugin));
        registry.register(new VoidWraithAscension(plugin));
        registry.register(new EyeballGarden(plugin));
        registry.register(new TheChorus(plugin));
        registry.register(new LeviathanWhelps(plugin));
        registry.register(new VoidRiftSentinels(plugin));
        registry.register(new BroodRelease(plugin));
        registry.register(new TheHerald(plugin));
    }

    // -------------------------------------------------------------------------
    // 31. Void Spawn Eruption
    // Six geysers of dark void energy erupt from cracks, each birthing a
    // Voidspawn creature — small, fast, composed of dark energy and teeth.
    // -------------------------------------------------------------------------
    public static class VoidSpawnEruption extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int SPAWN_COUNT = 6;
        private boolean erupted = false;

        public VoidSpawnEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_spawn_eruption", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(3.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Six geyser crack locations
            double[] gx = {-6, 4, -3, 8, 0, -9};
            double[] gz = {3, -5, 8, 0, -7, -2};

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.5f);

            for (int i = 0; i < SPAWN_COUNT; i++) {
                Location crackLoc = center.clone().add(gx[i], 0, gz[i]);

                // Crack indicator on surface
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc.clone().add(0, -0.1, 0), Material.OBSIDIAN);
                crack.scale(0.8f, 0.05f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(crack);
                spawnedEntities.add(crack.entity());

                // Geyser column (hidden until eruption)
                for (int h = 0; h < 4; h++) {
                    Location geyserLoc = crackLoc.clone().add(0, h * 0.8 - 3, 0);
                    BlockDisplayHandle geyser = displayBuilder.spawnBlock(geyserLoc, Material.PURPLE_STAINED_GLASS);
                    geyser.scale(0.5f, 0.7f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                    handles.add(geyser);
                    spawnedEntities.add(geyser.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            double[] gx = {-6, 4, -3, 8, 0, -9};
            double[] gz = {3, -5, 8, 0, -7, -2};

            // Phase 1 (0-30): crack forming telegraph
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 5.0);
                    for (int i = 0; i < SPAWN_COUNT; i++) {
                        Location crackLoc = center.clone().add(gx[i], 0.3, gz[i]);
                        DisplayBuilder.purpleDust(crackLoc, 4, 0.8);
                    }
                }
                return;
            }

            // Phase 2 (30): eruption
            if (!erupted && ticksAlive == 30) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.7f);

                int nodeStride = 1 + 4; // 1 crack + 4 geyser segments
                for (int i = 0; i < SPAWN_COUNT; i++) {
                    Location crackLoc = center.clone().add(gx[i], 0, gz[i]);
                    DisplayBuilder.purpleDust(crackLoc.clone().add(0, 1, 0), 20, 2.0);

                    // Raise geyser segments
                    int base = i * nodeStride + 1;
                    for (int h = 0; h < 4; h++) {
                        int idx = base + h;
                        if (idx >= handles.size()) break;
                        handles.get(idx).entity().teleport(crackLoc.clone().add(0, h * 0.8 + 0.3, 0));
                    }
                    triggerImpactDamage(crackLoc.clone().add(0, 0.5, 0));
                }
            }

            // Phase 3 (30-100): geysers pulse, creatures active
            if (erupted && ticksAlive > 30) {
                int nodeStride = 5;
                float age = (ticksAlive - 30) / 70f;

                for (int i = 0; i < SPAWN_COUNT; i++) {
                    Location crackLoc = center.clone().add(gx[i], 0, gz[i]);

                    int base = i * nodeStride + 1;
                    for (int h = 0; h < 4; h++) {
                        int idx = base + h;
                        if (idx >= handles.size()) break;
                        float pulse = (float)Math.sin(ticksAlive * 0.3 + h * 0.5) * 0.15f;
                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float scaleY = 0.7f * (1f - age * 0.6f);
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.25f - pulse, -scaleY * 0.5f, -0.25f - pulse),
                            new AxisAngle4f(ticksAlive * 0.04f + h, 0, 1, 0),
                            new Vector3f(0.5f + pulse * 2, scaleY, 0.5f + pulse * 2),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(2);
                    }

                    if (ticksAlive % 20 == 0) {
                        triggerImpactDamage(crackLoc.clone().add(0, 0.5, 0));
                        DisplayBuilder.purpleDust(crackLoc.clone().add(0, 1.5, 0), 6, 1.5);
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
            return new VoidSpawnEruption(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 32. Tendril Wall
    // A row of 8 thick void tendrils erupts from one island edge, forming a
    // 24-block wide wall with narrow passable gaps.
    // -------------------------------------------------------------------------
    public static class TendrilWall extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean wallRaised = false;

        public TendrilWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tendril_wall", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(5.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall rises from the north edge
            Location wallBase = center.clone().add(0, 0, -18);

            DisplayBuilder.playSound(wallBase, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
            DisplayBuilder.purpleDust(wallBase, 25, 5.0);

            // 8 tendrils spanning 24 blocks along edge, each 12 blocks tall
            for (int tendril = 0; tendril < 8; tendril++) {
                double xOffset = -10.5 + tendril * 3.0;
                for (int h = 0; h < 6; h++) {
                    Location tendrilLoc = wallBase.clone().add(xOffset, -4 + h * 0.5, 0);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(tendrilLoc, Material.OBSIDIAN);
                    seg.scale(1.5f, 2.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                    handles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
            }

            // Seeping fluid pools at base
            for (int i = 0; i < 16; i++) {
                double xOff = (i - 7.5) * 1.5;
                Location poolLoc = wallBase.clone().add(xOff, -0.1, 0);
                BlockDisplayHandle pool = displayBuilder.spawnBlock(poolLoc, Material.PURPLE_STAINED_GLASS);
                pool.scale(1.3f, 0.1f, 0.8f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(pool);
                spawnedEntities.add(pool.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            Location wallBase = center.clone().add(0, 0, -18);

            // Phase 1 (0-40): tendrils rise from below
            if (ticksAlive <= 40) {
                float riseT = ticksAlive / 40f;

                for (int tendril = 0; tendril < 8; tendril++) {
                    double xOffset = -10.5 + tendril * 3.0;
                    for (int h = 0; h < 6; h++) {
                        int idx = tendril * 6 + h;
                        if (idx >= handles.size()) break;
                        float yTarget = h * 2.0f * riseT;
                        handles.get(idx).entity().teleport(wallBase.clone().add(xOffset, yTarget, 0));

                        BlockDisplay bd = (BlockDisplay) handles.get(idx).entity();
                        float scaleY = 2.0f * riseT;
                        if (scaleY < 0.1f) scaleY = 0.1f;
                        bd.setTransformation(new Transformation(
                            new Vector3f(-0.75f, -scaleY * 0.5f, -0.5f),
                            new AxisAngle4f((float)Math.sin(ticksAlive * 0.1 + h) * 0.05f, 0, 0, 1),
                            new Vector3f(1.5f, scaleY, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(wallBase.clone().add(0, 3 * riseT, 0), 15, 6.0);
                }
                return;
            }

            // Phase 2 (40-400): wall active — sway and contact damage
            if (!wallRaised) {
                wallRaised = true;
                DisplayBuilder.playSound(wallBase, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
            }

            // Slow sway animation
            for (int tendril = 0; tendril < 8; tendril++) {
                double xOffset = -10.5 + tendril * 3.0;
                float sway = (float)Math.sin(ticksAlive * 0.04 + tendril * 0.8) * 0.3f;
                for (int h = 0; h < 6; h++) {
                    int idx = tendril * 6 + h;
                    if (idx >= handles.size()) break;
                    float yPos = h * 2.0f;
                    handles.get(idx).entity().teleport(wallBase.clone().add(xOffset + sway * h, yPos, sway * 0.5));
                }
            }

            // Ichor drip particles
            if (ticksAlive % 6 == 0) {
                for (int tendril = 0; tendril < 8; tendril++) {
                    Location drip = wallBase.clone().add(-10.5 + tendril * 3.0, 10, 0);
                    DisplayBuilder.purpleDust(drip, 4, 0.5);
                }
            }

            // Contact damage zones along wall
            if (ticksAlive % 20 == 0 && ticksAlive > 40) {
                for (int tendril = 0; tendril < 8; tendril++) {
                    Location tendrilDmg = wallBase.clone().add(-10.5 + tendril * 3.0, 3, 0);
                    triggerImpactDamage(tendrilDmg);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TendrilWall(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 33. The Hatching
    // Three large void eggs rise from below the island. Players must destroy
    // them before they hatch into Voidhounds.
    // -------------------------------------------------------------------------
    public static class TheHatching extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int EGG_COUNT = 3;
        private boolean hatched = false;

        public TheHatching(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_hatching", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(3.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1000);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(3.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three egg positions — spread out
            double[] ex = {-6, 5, 0};
            double[] ez = {-4, 3, 8};

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.0f, 0.4f);

            for (int i = 0; i < EGG_COUNT; i++) {
                Location eggLoc = center.clone().add(ex[i], -3, ez[i]);

                // Main egg body
                BlockDisplayHandle egg = displayBuilder.spawnBlock(eggLoc, Material.PURPLE_CONCRETE);
                egg.scale(1.5f, 2.0f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(egg);
                spawnedEntities.add(egg.entity());

                // Veins on the egg
                for (int v = 0; v < 4; v++) {
                    double angle = Math.toRadians(v * 90);
                    Location veinLoc = eggLoc.clone().add(Math.cos(angle) * 0.7, v * 0.3, Math.sin(angle) * 0.7);
                    BlockDisplayHandle vein = displayBuilder.spawnBlock(veinLoc, Material.OBSIDIAN);
                    vein.scale(0.2f, 0.8f, 0.2f).glow(80, 0, 160).interpolation(2, 0);
                    handles.add(vein);
                    spawnedEntities.add(vein.entity());
                }

                // Crack indicator on surface
                BlockDisplayHandle surfaceCrack = displayBuilder.spawnBlock(center.clone().add(ex[i], -0.05, ez[i]),
                        Material.OBSIDIAN);
                surfaceCrack.scale(1.2f, 0.05f, 1.2f).glow(80, 0, 160).interpolation(3, 0);
                handles.add(surfaceCrack);
                spawnedEntities.add(surfaceCrack.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            double[] ex = {-6, 5, 0};
            double[] ez = {-4, 3, 8};
            int perEgg = 1 + 4 + 1; // 1 egg body + 4 veins + 1 surface crack

            // Phase 1 (0-20): eggs rise from cracks
            if (ticksAlive <= 20) {
                float riseT = ticksAlive / 20f;
                for (int i = 0; i < EGG_COUNT; i++) {
                    int base = i * perEgg;
                    if (base >= handles.size()) break;
                    float yPos = -3.0f + riseT * 3.5f;
                    handles.get(base).entity().teleport(center.clone().add(ex[i], yPos, ez[i]));
                    DisplayBuilder.purpleDust(center.clone().add(ex[i], yPos + 0.5, ez[i]), 5, 1.0);
                }
                return;
            }

            // Phase 2 (20-160): eggs rest and breathe
            if (ticksAlive <= 160) {
                float breathAge = (ticksAlive - 20) / 140f;

                for (int i = 0; i < EGG_COUNT; i++) {
                    int base = i * perEgg;
                    if (base >= handles.size()) break;

                    // Breathing pulse
                    float breathe = (float)Math.sin(ticksAlive * 0.15 + i) * 0.1f;
                    BlockDisplay egg = (BlockDisplay) handles.get(base).entity();
                    float scaleXZ = 1.5f + breathe;
                    float scaleY = 2.0f + breathe * 1.5f;
                    egg.setTransformation(new Transformation(
                        new Vector3f(-scaleXZ * 0.5f, -scaleY * 0.5f, -scaleXZ * 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scaleXZ, scaleY, scaleXZ),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    egg.setInterpolationDelay(0);
                    egg.setInterpolationDuration(4);

                    // Veins pulse with growing urgency
                    for (int v = 0; v < 4; v++) {
                        int vIdx = base + 1 + v;
                        if (vIdx >= handles.size()) break;
                        BlockDisplay vein = (BlockDisplay) handles.get(vIdx).entity();
                        float veinScale = 0.2f + breathAge * 0.2f + breathe * 0.2f;
                        vein.setTransformation(new Transformation(
                            new Vector3f(-veinScale * 0.5f, -0.4f, -veinScale * 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(veinScale, 0.8f, veinScale),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        vein.setInterpolationDelay(0);
                        vein.setInterpolationDuration(3);
                    }

                    // Warming pulse
                    if (ticksAlive % 15 == 0) {
                        DisplayBuilder.crimsonDust(center.clone().add(ex[i], 0.5, ez[i]), 6, 1.5);
                    }
                }
            }

            // Phase 3 (160): hatch — violent crack open
            if (!hatched && ticksAlive == 160) {
                hatched = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.2f);

                for (int i = 0; i < EGG_COUNT; i++) {
                    Location eggLoc = center.clone().add(ex[i], 0.5, ez[i]);
                    DisplayBuilder.purpleDust(eggLoc, 25, 3.0);
                    DisplayBuilder.crimsonDust(eggLoc, 15, 3.0);
                    triggerImpactDamage(eggLoc);

                    // Scatter egg fragments
                    int base = i * perEgg;
                    for (int v = 0; v < 4; v++) {
                        int vIdx = base + 1 + v;
                        if (vIdx >= handles.size()) break;
                        double angle = Math.toRadians(v * 90 + Math.random() * 45);
                        handles.get(vIdx).entity().teleport(
                            eggLoc.clone().add(Math.cos(angle) * (3 + Math.random() * 3), 1.5, Math.sin(angle) * (3 + Math.random() * 3))
                        );
                    }
                }
            }

            // Phase 4 (160-300): Voidhound presence visualized — roaming constructs
            if (hatched && ticksAlive > 160) {
                float houndAge = (ticksAlive - 160) / 140f;
                for (int i = 0; i < EGG_COUNT; i++) {
                    int base = i * perEgg;
                    if (base >= handles.size()) break;

                    // Egg body "stalks" the island
                    double angle = Math.toRadians(ticksAlive * 1.5 + i * 120);
                    double r = 4.0 + Math.sin(ticksAlive * 0.05 + i) * 3.0;
                    Location houndLoc = center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                    handles.get(base).entity().teleport(houndLoc);

                    BlockDisplay bd = (BlockDisplay) handles.get(base).entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.75f, -1.0f, -0.75f),
                        new AxisAngle4f((float)angle, 0, 1, 0),
                        new Vector3f(1.5f, 2.0f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);

                    if (ticksAlive % 15 == 0) {
                        triggerImpactDamage(houndLoc);
                        DisplayBuilder.crimsonDust(houndLoc, 5, 1.5);
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
            return new TheHatching(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 34. Void Wraith Ascension
    // Two translucent ghostly entities phase up through the island — elongated
    // humanoid silhouettes filled with swirling void energy.
    // -------------------------------------------------------------------------
    public static class VoidWraithAscension extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int WRAITH_COUNT = 2;

        public VoidWraithAscension(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_wraith_ascension", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(900);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[] wx = {-5, 6};
            double[] wz = {-3, 5};

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

            for (int i = 0; i < WRAITH_COUNT; i++) {
                Location wraithLoc = center.clone().add(wx[i], -5, wz[i]);

                // Telegraph: surface glow
                Location surfaceLoc = center.clone().add(wx[i], -0.05, wz[i]);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(surfaceLoc, Material.PURPLE_STAINED_GLASS);
                glow.scale(1.5f, 0.05f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(glow);
                spawnedEntities.add(glow.entity());

                // Main wraith body — elongated silhouette
                BlockDisplayHandle body = displayBuilder.spawnBlock(wraithLoc, Material.PURPLE_STAINED_GLASS);
                body.scale(0.6f, 3.0f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(body);
                spawnedEntities.add(body.entity());

                // Wispy outer form
                BlockDisplayHandle wisp1 = displayBuilder.spawnBlock(wraithLoc.clone().add(0.3, 0, 0),
                        Material.BLACK_STAINED_GLASS);
                wisp1.scale(0.4f, 2.5f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(wisp1);
                spawnedEntities.add(wisp1.entity());

                // Eye points
                for (int eye = 0; eye < 2; eye++) {
                    Location eyeLoc = wraithLoc.clone().add(-0.15 + eye * 0.3, 1.5, -0.2);
                    BlockDisplayHandle eyePoint = displayBuilder.spawnBlock(eyeLoc, Material.WHITE_CONCRETE);
                    eyePoint.scale(0.15f, 0.15f, 0.1f).glow(200, 200, 255).interpolation(2, 0);
                    handles.add(eyePoint);
                    spawnedEntities.add(eyePoint.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            double[] wx = {-5, 6};
            double[] wz = {-3, 5};
            int perWraith = 1 + 1 + 1 + 2; // glow + body + wisp + 2 eyes = 5

            // Phase 1 (0-20): phase through floor
            if (ticksAlive <= 20) {
                float t = ticksAlive / 20f;
                for (int i = 0; i < WRAITH_COUNT; i++) {
                    int base = i * perWraith;
                    if (base + 1 >= handles.size()) break;
                    float yPos = -5.0f + t * 8.0f;
                    handles.get(base + 1).entity().teleport(center.clone().add(wx[i], yPos, wz[i]));
                    handles.get(base + 2).entity().teleport(center.clone().add(wx[i] + 0.3, yPos, wz[i]));
                    // Eyes
                    handles.get(base + 3).entity().teleport(center.clone().add(wx[i] - 0.15, yPos + 1.5, wz[i] - 0.2));
                    handles.get(base + 4).entity().teleport(center.clone().add(wx[i] + 0.15, yPos + 1.5, wz[i] - 0.2));
                }
                return;
            }

            // Phase 2 (20-300): wraiths patrol — float 3 blocks above surface
            float floatAge = (ticksAlive - 20) / 280f;
            for (int i = 0; i < WRAITH_COUNT; i++) {
                int base = i * perWraith;
                if (base + 4 >= handles.size()) break;

                // Patrol path — figure-8 movement
                double t = (ticksAlive - 20) / 80.0 + i * Math.PI;
                double px = wx[i] + Math.sin(t) * 6.0;
                double pz = wz[i] + Math.sin(t * 2) * 4.0;
                double py = 3.0 + Math.sin(ticksAlive * 0.05 + i) * 0.5;

                handles.get(base + 1).entity().teleport(center.clone().add(px, py, pz));
                handles.get(base + 2).entity().teleport(center.clone().add(px + 0.3, py, pz));
                handles.get(base + 3).entity().teleport(center.clone().add(px - 0.15, py + 1.5, pz - 0.2));
                handles.get(base + 4).entity().teleport(center.clone().add(px + 0.15, py + 1.5, pz - 0.2));

                // Animate wraith flicker
                BlockDisplay body = (BlockDisplay) handles.get(base + 1).entity();
                float flicker = (float)Math.sin(ticksAlive * 0.4 + i * 1.5) * 0.1f;
                body.setTransformation(new Transformation(
                    new Vector3f(-0.3f + flicker, -1.5f, -0.2f),
                    new AxisAngle4f((float)Math.sin(ticksAlive * 0.03 + i) * 0.1f, 0, 1, 0),
                    new Vector3f(0.6f + flicker, 3.0f, 0.4f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                body.setInterpolationDelay(0);
                body.setInterpolationDuration(3);

                // Damage on approach
                if (ticksAlive % 20 == 0) {
                    Location dmgLoc = center.clone().add(px, py - 1, pz);
                    triggerImpactDamage(dmgLoc);
                    DisplayBuilder.purpleDust(dmgLoc, 6, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidWraithAscension(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 35. Eyeball Garden
    // 16 pulsating eyeball stalks rise across the island, each tracking the
    // nearest player and firing void beams when not destroyed in time.
    // -------------------------------------------------------------------------
    public static class EyeballGarden extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int STALK_COUNT = 16;
        private boolean beamsFired = false;

        public EyeballGarden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eyeball_garden", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(2.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(700);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_OPEN, 1.0f, 0.5f);

            // Scatter 16 stalks across the island
            for (int i = 0; i < STALK_COUNT; i++) {
                double angle = Math.toRadians(i * (360.0 / STALK_COUNT));
                double r = 2.0 + Math.random() * 10.0;
                Location stalkLoc = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);

                // Stalk stem
                BlockDisplayHandle stem = displayBuilder.spawnBlock(stalkLoc.clone().add(0, -0.5, 0), Material.OBSIDIAN);
                stem.scale(0.2f, 0.8f, 0.2f).glow(80, 0, 160).interpolation(3, 0);
                handles.add(stem);
                spawnedEntities.add(stem.entity());

                // Eye bulb — initially closed
                BlockDisplayHandle eyeBulb = displayBuilder.spawnBlock(stalkLoc.clone().add(0, 0.5, 0),
                        Material.CRIMSON_NYLIUM);
                eyeBulb.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(eyeBulb);
                spawnedEntities.add(eyeBulb.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < STALK_COUNT * 2) return;

            // Phase 1 (0-20): stalks emerge
            if (ticksAlive <= 20) {
                float t = ticksAlive / 20f;
                for (int i = 0; i < STALK_COUNT; i++) {
                    double angle = Math.toRadians(i * (360.0 / STALK_COUNT));
                    double r = 2.0 + (i % 5) * 2.2;
                    Location stalkBase = center.clone().add(Math.cos(angle) * r, -0.5 + t * 0.5, Math.sin(angle) * r);
                    handles.get(i * 2).entity().teleport(stalkBase);
                    handles.get(i * 2 + 1).entity().teleport(stalkBase.clone().add(0, 1.0, 0));
                }
                return;
            }

            // Phase 2 (20-300): eyes track and rotate toward center
            for (int i = 0; i < STALK_COUNT; i++) {
                double angle = Math.toRadians(i * (360.0 / STALK_COUNT));
                double r = 2.0 + (i % 5) * 2.2;
                Location stalkBase = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);

                // Stem sways slightly
                handles.get(i * 2).entity().teleport(
                    stalkBase.clone().add(Math.sin(ticksAlive * 0.05 + i) * 0.1, -0.2, Math.cos(ticksAlive * 0.05 + i) * 0.1)
                );

                // Eye rotates to track center
                float eyeRot = (float)(Math.atan2(center.getZ() - stalkBase.getZ(), center.getX() - stalkBase.getX()));
                BlockDisplay eyeBulb = (BlockDisplay) handles.get(i * 2 + 1).entity();
                float pulse = 0.4f + (float)Math.sin(ticksAlive * 0.2 + i) * 0.1f;
                eyeBulb.setTransformation(new Transformation(
                    new Vector3f(-pulse * 0.5f, -pulse * 0.5f, -pulse * 0.5f),
                    new AxisAngle4f(eyeRot, 0, 1, 0),
                    new Vector3f(pulse, pulse, pulse),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                eyeBulb.setInterpolationDelay(0);
                eyeBulb.setInterpolationDuration(3);

                // Damage pulses
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(stalkBase.clone().add(0, 0.5, 0));
                }
            }

            // Phase 3 (300 - 1 tick before end): coordinated beam fire
            if (!beamsFired && ticksAlive == 280) {
                beamsFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
                DisplayBuilder.purpleDust(center, 40, 10.0);

                for (int i = 0; i < STALK_COUNT; i++) {
                    double angle = Math.toRadians(i * (360.0 / STALK_COUNT));
                    double r = 2.0 + (i % 5) * 2.2;
                    Location stalkBase = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                    triggerImpactDamage(stalkBase);
                    DisplayBuilder.crimsonDust(stalkBase, 10, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EyeballGarden(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 36. The Chorus
    // Eight floating dark crystal skulls materialize around the island
    // perimeter, humming in resonant harmony that amplifies the next attack.
    // -------------------------------------------------------------------------
    public static class TheChorus extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int CHORUS_COUNT = 8;

        public TheChorus(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_chorus", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(0.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);

            // Spawn 8 entities evenly around the island perimeter
            for (int i = 0; i < CHORUS_COUNT; i++) {
                double angle = Math.toRadians(i * 45);
                double r = 16.0;
                Location chorusLoc = center.clone().add(Math.cos(angle) * r, 3.0, Math.sin(angle) * r);

                // Crystal skull main body
                BlockDisplayHandle skull = displayBuilder.spawnBlock(chorusLoc, Material.AMETHYST_BLOCK);
                skull.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                handles.add(skull);
                spawnedEntities.add(skull.entity());

                // Inner dark core
                BlockDisplayHandle core = displayBuilder.spawnBlock(chorusLoc.clone().add(0, 0, 0),
                        Material.OBSIDIAN);
                core.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                handles.add(core);
                spawnedEntities.add(core.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.size() < CHORUS_COUNT * 2) return;

            // Phase 1 (0-100): entities hum in orbit
            for (int i = 0; i < CHORUS_COUNT; i++) {
                double baseAngle = Math.toRadians(i * 45);
                double rotAngle = baseAngle + ticksAlive * 0.015;
                double r = 16.0;
                double y = 3.0 + Math.sin(ticksAlive * 0.05 + i) * 0.5;
                Location chorusLoc = center.clone().add(Math.cos(rotAngle) * r, y, Math.sin(rotAngle) * r);

                handles.get(i * 2).entity().teleport(chorusLoc);
                handles.get(i * 2 + 1).entity().teleport(chorusLoc);

                // Resonance pulse animation
                float resonance = (float)Math.sin(ticksAlive * 0.2 + i * 0.785) * 0.2f;
                BlockDisplay skull = (BlockDisplay) handles.get(i * 2).entity();
                float scale = 0.8f + resonance;
                skull.setTransformation(new Transformation(
                    new Vector3f(-scale * 0.5f, -scale * 0.5f, -scale * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                skull.setInterpolationDelay(0);
                skull.setInterpolationDuration(3);
            }

            // Resonance sound every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f + ticksAlive * 0.005f);
                DisplayBuilder.purpleDust(center, 8, 8.0);
            }

            // Nausea-inducing visual — connecting beams between chorus members
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < CHORUS_COUNT; i++) {
                    double angle = Math.toRadians(i * 45 + ticksAlive * 0.9);
                    double r = 16.0;
                    Location chorusLoc = center.clone().add(Math.cos(angle) * r, 3.0, Math.sin(angle) * r);
                    DisplayBuilder.purpleDust(chorusLoc, 3, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheChorus(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 37. Leviathan Whelps
    // Two juvenile leviathan creatures breach the island edge and slither
    // across the surface hunting by vibration.
    // -------------------------------------------------------------------------
    public static class LeviathanWhelps extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int WHELP_COUNT = 2;
        private static final int SEGMENTS_PER_WHELP = 5;

        public LeviathanWhelps(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("leviathan_whelps", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1100);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);

            // Two whelps from opposite edges
            double[] startX = {-20, 20};
            double[] startZ = {-5, 5};

            for (int whelp = 0; whelp < WHELP_COUNT; whelp++) {
                // Spawn whelp as a chain of segments
                for (int seg = 0; seg < SEGMENTS_PER_WHELP; seg++) {
                    Location segLoc = center.clone().add(startX[whelp] + seg * 1.0, 0.3, startZ[whelp]);
                    Material mat = seg == 0 ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                    BlockDisplayHandle segment = displayBuilder.spawnBlock(segLoc, mat);
                    float scale = seg == 0 ? 1.2f : 0.9f - seg * 0.1f;
                    segment.scale(scale, scale * 0.7f, scale).glow(128, 0, 255).interpolation(2, 0);
                    handles.add(segment);
                    spawnedEntities.add(segment.entity());
                }

                // Mouth glow
                Location mouthLoc = center.clone().add(startX[whelp], 0.3, startZ[whelp]);
                BlockDisplayHandle mouth = displayBuilder.spawnBlock(mouthLoc.clone().add(0, 0, -0.6),
                        Material.CRIMSON_NYLIUM);
                mouth.scale(0.8f, 0.5f, 0.3f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(mouth);
                spawnedEntities.add(mouth.entity());

                // Fluid eruption at entry point
                DisplayBuilder.purpleDust(mouthLoc, 20, 3.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int perWhelp = SEGMENTS_PER_WHELP + 1; // segments + mouth

            for (int whelp = 0; whelp < WHELP_COUNT; whelp++) {
                int base = whelp * perWhelp;
                if (base >= handles.size()) break;

                // Serpentine path across island
                double timeOffset = ticksAlive * 0.025 + whelp * Math.PI;
                double headX = Math.sin(timeOffset) * 12.0;
                double headZ = Math.cos(timeOffset * 0.7 + whelp) * 10.0;
                double headY = 0.3;

                Location headLoc = center.clone().add(headX, headY, headZ);
                handles.get(base).entity().teleport(headLoc);

                // Snake body follows with delay
                for (int seg = 1; seg < SEGMENTS_PER_WHELP; seg++) {
                    double segTimeOffset = (ticksAlive - seg * 3) * 0.025 + whelp * Math.PI;
                    double segX = Math.sin(segTimeOffset) * 12.0;
                    double segZ = Math.cos(segTimeOffset * 0.7 + whelp) * 10.0;
                    handles.get(base + seg).entity().teleport(center.clone().add(segX, headY, segZ));
                }

                // Mouth position at head
                handles.get(base + SEGMENTS_PER_WHELP).entity().teleport(headLoc.clone().add(0, 0, -0.7));

                // Head animation
                BlockDisplay head = (BlockDisplay) handles.get(base).entity();
                float bite = (float)Math.sin(ticksAlive * 0.2 + whelp) * 0.1f;
                head.setTransformation(new Transformation(
                    new Vector3f(-0.6f + bite, -0.42f, -0.6f + bite),
                    new AxisAngle4f((float)timeOffset, 0, 1, 0),
                    new Vector3f(1.2f - bite, 0.84f, 1.2f - bite),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                head.setInterpolationDelay(0);
                head.setInterpolationDuration(2);

                // Damage zone around head
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(headLoc);
                    DisplayBuilder.purpleDust(headLoc.clone().add(0, 0.5, 0), 8, 2.0);
                }
            }

            // Ambient particle trail
            if (ticksAlive % 4 == 0) {
                for (int whelp = 0; whelp < WHELP_COUNT; whelp++) {
                    int base = whelp * perWhelp;
                    if (base >= handles.size()) break;
                    Location headLoc = handles.get(base).entity().getLocation();
                    DisplayBuilder.purpleDust(headLoc, 3, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new LeviathanWhelps(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 38. Void Rift Sentinels
    // Four vertical rifts tear open, each spawning a geometric black shard
    // creature that guards positions and attacks on approach.
    // -------------------------------------------------------------------------
    public static class VoidRiftSentinels extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private static final int RIFT_COUNT = 4;

        public VoidRiftSentinels(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rift_sentinels", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(1000);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);

            // Four rifts around the island at intermediate positions
            double[][] riftPositions = {{-8, 0}, {8, 0}, {0, -8}, {0, 8}};

            for (int rift = 0; rift < RIFT_COUNT; rift++) {
                Location riftLoc = center.clone().add(riftPositions[rift][0], 2, riftPositions[rift][1]);

                // Rift tear — vertical slit
                BlockDisplayHandle riftFrame = displayBuilder.spawnBlock(riftLoc, Material.BLACK_STAINED_GLASS);
                riftFrame.scale(0.1f, 4.0f, 0.1f).glow(200, 200, 255).interpolation(3, 0);
                handles.add(riftFrame);
                spawnedEntities.add(riftFrame.entity());

                // Rift edge glows
                for (int edge = 0; edge < 2; edge++) {
                    Location edgeLoc = riftLoc.clone().add(edge == 0 ? -0.1 : 0.1, 0, 0);
                    BlockDisplayHandle edgeGlow = displayBuilder.spawnBlock(edgeLoc, Material.WHITE_CONCRETE);
                    edgeGlow.scale(0.05f, 4.0f, 0.05f).glow(200, 200, 255).interpolation(2, 0);
                    handles.add(edgeGlow);
                    spawnedEntities.add(edgeGlow.entity());
                }

                // Sentinel — geometric blade shard
                Location sentinelLoc = riftLoc.clone().add(0, 0, 0);
                BlockDisplayHandle sentinel = displayBuilder.spawnBlock(sentinelLoc, Material.OBSIDIAN);
                sentinel.scale(0.2f, 2.5f, 1.5f).glow(80, 0, 160).interpolation(3, 0);
                handles.add(sentinel);
                spawnedEntities.add(sentinel.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            int perRift = 4; // riftFrame + 2 edgeGlows + sentinel
            double[][] riftPositions = {{-8, 0}, {8, 0}, {0, -8}, {0, 8}};

            for (int rift = 0; rift < RIFT_COUNT; rift++) {
                int base = rift * perRift;
                if (base + 3 >= handles.size()) break;

                Location riftLoc = center.clone().add(riftPositions[rift][0], 2, riftPositions[rift][1]);

                // Rift pulses
                BlockDisplay riftFrame = (BlockDisplay) handles.get(base).entity();
                float riftPulse = 0.1f + (float)Math.sin(ticksAlive * 0.1 + rift) * 0.03f;
                riftFrame.setTransformation(new Transformation(
                    new Vector3f(-riftPulse * 0.5f, -2.0f, -riftPulse * 0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(riftPulse, 4.0f, riftPulse),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                riftFrame.setInterpolationDelay(0);
                riftFrame.setInterpolationDuration(3);

                // Sentinel patrols nearby
                double sentinelAngle = ticksAlive * 0.03 + rift * (Math.PI / 2);
                double sx = riftPositions[rift][0] + Math.cos(sentinelAngle) * 3.0;
                double sz = riftPositions[rift][1] + Math.sin(sentinelAngle) * 3.0;
                Location sentinelLoc = center.clone().add(sx, 2, sz);
                handles.get(base + 3).entity().teleport(sentinelLoc);

                // Sentinel rotation
                BlockDisplay sentinel = (BlockDisplay) handles.get(base + 3).entity();
                sentinel.setTransformation(new Transformation(
                    new Vector3f(-0.1f, -1.25f, -0.75f),
                    new AxisAngle4f((float)sentinelAngle + ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(0.2f, 2.5f, 1.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                sentinel.setInterpolationDelay(0);
                sentinel.setInterpolationDuration(2);

                // Damage zone around sentinel
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(sentinelLoc);
                    DisplayBuilder.purpleDust(sentinelLoc, 6, 2.0);
                }

                // Rift damage zone
                if (ticksAlive % 30 == 0) {
                    triggerImpactDamage(riftLoc);
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.purpleDust(center, 5, 6.0);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new VoidRiftSentinels(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 39. Brood Release
    // A spore cloud settles across the island and hatches simultaneously into
    // a massive swarm of Broodings targeting a single player.
    // -------------------------------------------------------------------------
    public static class BroodRelease extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean hatched = false;

        public BroodRelease(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brood_release", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(1.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(900);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);

            // Spore cloud settling — many tiny particles across island
            for (int i = 0; i < 35; i++) {
                double angle = Math.toRadians(i * (360.0 / 35));
                double r = 1.0 + Math.random() * 14.0;
                Location sporeLoc = center.clone().add(Math.cos(angle) * r, 0.5 + Math.random() * 3.0, Math.sin(angle) * r);
                BlockDisplayHandle spore = displayBuilder.spawnBlock(sporeLoc, Material.PURPLE_STAINED_GLASS);
                spore.scale(0.15f, 0.15f, 0.15f).glow(128, 0, 255).interpolation(2, 0);
                handles.add(spore);
                spawnedEntities.add(spore.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-80): spores float downward and settle
            if (ticksAlive <= 80) {
                float settleT = ticksAlive / 80f;
                for (int i = 0; i < handles.size(); i++) {
                    double angle = Math.toRadians(i * (360.0 / 35));
                    double r = 1.0 + (i % 5) * 3.0;
                    double targetY = 0.1;
                    double currentY = 0.5 + (1.0 - settleT) * 3.0;
                    handles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * r, currentY, Math.sin(angle) * r)
                    );

                    if (ticksAlive % 5 == 0) {
                        DisplayBuilder.purpleDust(handles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
                return;
            }

            // Phase 2 (80): mass hatch
            if (!hatched && ticksAlive == 80) {
                hatched = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 2.0f);
                DisplayBuilder.purpleDust(center, 50, 8.0);
            }

            // Phase 3 (80-200): swarm visualized as rapidly darting particles
            if (hatched && ticksAlive > 80) {
                float swarmAge = (ticksAlive - 80) / 120f;

                for (int i = 0; i < handles.size(); i++) {
                    // Swarm behavior — converge on a single point then scatter
                    double baseAngle = Math.toRadians(i * (360.0 / 35));
                    double swarmAngle = baseAngle + ticksAlive * 0.25;
                    double r = (1.0 + (i % 5) * 2.5) * (1f - swarmAge * 0.4f);
                    double yBob = 0.3 + Math.sin(ticksAlive * 0.3 + i) * 0.2;
                    handles.get(i).entity().teleport(
                        center.clone().add(Math.cos(swarmAngle) * r, yBob, Math.sin(swarmAngle) * r)
                    );

                    // Frantic animation
                    BlockDisplay bd = (BlockDisplay) handles.get(i).entity();
                    float sz = 0.15f + (float)Math.sin(ticksAlive * 0.8 + i * 0.5) * 0.05f;
                    bd.setTransformation(new Transformation(
                        new Vector3f(-sz * 0.5f, -sz * 0.5f, -sz * 0.5f),
                        new AxisAngle4f(ticksAlive * 0.3f + i, 1, 1, 0),
                        new Vector3f(sz, sz, sz),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(1);
                }

                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new BroodRelease(plugin);
        }
    }

    // -------------------------------------------------------------------------
    // 40. The Herald
    // A single massive void creature — the Herald — phases up through the
    // island center and marks a player for tripled boss attack rate.
    // -------------------------------------------------------------------------
    public static class TheHerald extends BossAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean emerged = false;

        public TheHerald(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_herald", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1200);
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center glow telegraph
            BlockDisplayHandle glow = displayBuilder.spawnBlock(center.clone().add(0, -0.1, 0),
                    Material.OBSIDIAN);
            glow.scale(3f, 0.1f, 3f).glow(128, 0, 255).interpolation(4, 0);
            handles.add(glow);
            spawnedEntities.add(glow.entity());

            // Herald body — massive obsidian pillar with a circular mouth
            Location heraldStart = center.clone().add(0, -8, 0);
            BlockDisplayHandle body = displayBuilder.spawnBlock(heraldStart, Material.OBSIDIAN);
            body.scale(1.5f, 4.0f, 1.5f).glow(80, 0, 160).interpolation(4, 0);
            handles.add(body);
            spawnedEntities.add(body.entity());

            // Circular mouth
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(i * 30);
                Location mouthLoc = heraldStart.clone().add(Math.cos(angle) * 0.7, 2.0, Math.sin(angle) * 0.7);
                BlockDisplayHandle mouthSeg = displayBuilder.spawnBlock(mouthLoc, Material.CRYING_OBSIDIAN);
                mouthSeg.scale(0.3f, 0.6f, 0.3f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(mouthSeg);
                spawnedEntities.add(mouthSeg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.purpleDust(center, 25, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (handles.isEmpty()) return;

            // Phase 1 (0-60): center glows, Herald phases through floor
            if (ticksAlive <= 60) {
                float riseT = ticksAlive / 60f;
                float yPos = -8.0f + riseT * 11.0f;

                handles.get(1).entity().teleport(center.clone().add(0, yPos, 0));

                // Mouth segments rise with body
                for (int i = 0; i < 12; i++) {
                    int idx = 2 + i;
                    if (idx >= handles.size()) break;
                    double angle = Math.toRadians(i * 30);
                    double mouthY = yPos + 2.0;
                    handles.get(idx).entity().teleport(center.clone().add(
                        Math.cos(angle) * 0.7, mouthY, Math.sin(angle) * 0.7
                    ));
                }

                // Center glow intensifies
                BlockDisplay glowBd = (BlockDisplay) handles.get(0).entity();
                float glowScale = 3f * riseT;
                glowBd.setTransformation(new Transformation(
                    new Vector3f(-glowScale * 0.5f, -0.05f, -glowScale * 0.5f),
                    new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                    new Vector3f(glowScale, 0.1f, glowScale),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                glowBd.setInterpolationDelay(0);
                glowBd.setInterpolationDuration(4);

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, yPos + 1, 0), 12, 2.5);
                }
                return;
            }

            // Phase 2 (60): fully emerged
            if (!emerged) {
                emerged = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
                DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), 40, 5.0);
            }

            // Phase 3 (60-300): Herald patrols and marks
            float patrolAge = (ticksAlive - 60) / 240f;
            double patrolAngle = ticksAlive * 0.02;
            double patrolR = 5.0 + Math.sin(ticksAlive * 0.04) * 3.0;
            double px = Math.cos(patrolAngle) * patrolR;
            double pz = Math.sin(patrolAngle) * patrolR;

            handles.get(1).entity().teleport(center.clone().add(px, 0.3, pz));

            // Mouth rotates
            for (int i = 0; i < 12; i++) {
                int idx = 2 + i;
                if (idx >= handles.size()) break;
                double mouthAngle = Math.toRadians(i * 30 + ticksAlive * 3.0);
                handles.get(idx).entity().teleport(center.clone().add(
                    px + Math.cos(mouthAngle) * 0.7, 0.3 + 2.0, pz + Math.sin(mouthAngle) * 0.7
                ));
            }

            // Mark aura visual — crimson ring around Herald
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.crimsonDust(center.clone().add(px, 1, pz), 8, 2.5);
            }

            // Damage from contact
            if (ticksAlive % 20 == 0) {
                triggerImpactDamage(center.clone().add(px, 1, pz));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TheHerald(plugin);
        }
    }
}
