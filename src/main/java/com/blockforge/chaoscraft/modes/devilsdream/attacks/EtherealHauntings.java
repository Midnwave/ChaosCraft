package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — ETHEREAL HAUNTING ATTACKS
 * 13 ghostly/spectral BlockDisplay attacks featuring phantom bridges,
 * soul lanterns, ghostly ships, and nightmare thrones.
 *
 * Materials: tinted_glass, white/light_blue/purple stained glass,
 * soul_lantern, crying_obsidian, amethyst_block, sea_lantern
 */
public final class EtherealHauntings {

    private EtherealHauntings() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PhantomBridge(plugin));
        registry.register(new SoulLanterns(plugin));
        registry.register(new GhostlyPortal(plugin));
        registry.register(new SpectralCage(plugin));
        registry.register(new HauntedMirror(plugin));
        registry.register(new SoulChain(plugin));
        registry.register(new GhostlyShip(plugin));
        registry.register(new SpectralStairway(plugin));
        registry.register(new PhantomClock(plugin));
        registry.register(new SoulWhirlpool(plugin));
        registry.register(new GhostlyArmy(plugin));
        registry.register(new EtherealOrb(plugin));
        registry.register(new NightmareThrone(plugin));
    }

    // ================================================================
    // 1. PHANTOM BRIDGE — 12 translucent glass blocks form a bridge
    //    that extends from one side across the player. Blocks fade
    //    in one by one. Bridge slowly descends toward player level.
    // ================================================================
    public static class PhantomBridge extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> planks = new ArrayList<>();
        private float bridgeY = 5.0f;

        public PhantomBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                double z = (i - 5.5) * 1.5;
                Material mat = (i % 3 == 0) ? Material.WHITE_STAINED_GLASS
                        : (i % 3 == 1) ? Material.LIGHT_BLUE_STAINED_GLASS
                        : Material.TINTED_GLASS;
                BlockDisplayHandle plank = displayBuilder.spawnBlock(
                        center.clone().add(0, bridgeY, z), mat);
                plank.scale(0.01f, 0.3f, 1.5f).glow(150, 180, 220).interpolation(8, 0);
                planks.add(plank);
                spawnedEntities.add(plank.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            bridgeY = 5.0f - ticksAlive * 0.008f;
            if (bridgeY < 1.5f) bridgeY = 1.5f;

            for (int i = 0; i < planks.size(); i++) {
                int revealTick = i * 5;
                double z = (i - 5.5) * 1.5;
                planks.get(i).entity().teleport(c.clone().add(0, bridgeY, z));

                if (ticksAlive >= revealTick) {
                    planks.get(i).animateTo(
                            new Vector3f(-1.5f, -0.15f, -0.75f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(3.0f, 0.3f, 1.5f), 10);
                }
            }

            if (ticksAlive % 6 == 0) {
                double z = Math.random() * 18 - 9;
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(Math.random() * 3 - 1.5, bridgeY + 0.5, z),
                        1, 0.3, 0.2, 0.3, 0.01);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.4f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PhantomBridge(plugin); }
    }

    // ================================================================
    // 2. SOUL LANTERNS — 10 soul lantern blocks float around the
    //    player at varying heights, bobbing gently. Emit soul flame.
    // ================================================================
    public static class SoulLanterns extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> lanterns = new ArrayList<>();
        private final List<Double> orbitAngles = new ArrayList<>();
        private final List<Double> orbitRadii = new ArrayList<>();
        private final List<Float> baseHeights = new ArrayList<>();

        public SoulLanterns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_lanterns", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(45.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                orbitAngles.add(Math.random() * Math.PI * 2);
                orbitRadii.add(2.0 + Math.random() * 5.0);
                baseHeights.add(1.5f + (float)(Math.random() * 5.0));

                BlockDisplayHandle lantern = displayBuilder.spawnBlock(center, Material.SOUL_LANTERN);
                lantern.scale(0.6f, 0.8f, 0.6f).glow(80, 180, 200).interpolation(3, 0);
                lanterns.add(lantern);
                spawnedEntities.add(lantern.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LANTERN_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < lanterns.size(); i++) {
                double angle = orbitAngles.get(i) + ticksAlive * 0.015;
                double r = orbitRadii.get(i);
                float bob = (float) Math.sin(ticksAlive * 0.04 + i * 0.8) * 0.5f;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                float y = baseHeights.get(i) + bob;

                lanterns.get(i).entity().teleport(c.clone().add(x, y, z));

                // Soul flame from each lantern
                if (ticksAlive % 8 == i % 8) {
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            c.clone().add(x, y + 0.5, z), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LANTERN_STEP, 0.3f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulLanterns(plugin); }
    }

    // ================================================================
    // 3. GHOSTLY PORTAL — Ring of 10 glass blocks forming a portal
    //    frame. Smoke/soul particles swirl inside. Slowly rotates.
    // ================================================================
    public static class GhostlyPortal extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();

        public GhostlyPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghostly_portal", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location portalCenter = center.clone().add(0, 3, 0);
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 3.0;
                double y = Math.sin(angle) * 3.0;
                Material mat = (i % 2 == 0) ? Material.PURPLE_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        portalCenter.clone().add(x, y, 0), mat);
                b.scale(0.8f, 0.8f, 0.4f).glow(120, 80, 200).interpolation(2, 0);
                ring.add(b);
                spawnedEntities.add(b.entity());
            }
            DisplayBuilder.playSound(portalCenter, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location portalCenter = c.clone().add(0, 3, 0);
            double rotOffset = ticksAlive * 0.02;

            for (int i = 0; i < ring.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 10;
                double angle = baseAngle + rotOffset;
                double x = Math.cos(angle) * 3.0;
                double y = Math.sin(angle) * 3.0;
                ring.get(i).entity().teleport(portalCenter.clone().add(x, y, 0));
            }

            // Swirling particles inside portal
            if (ticksAlive % 3 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 2.5;
                Location pLoc = portalCenter.clone().add(Math.cos(angle) * r, Math.sin(angle) * r, 0);
                c.getWorld().spawnParticle(Particle.SOUL, pLoc, 2, 0.3, 0.3, 0.1, 0.02);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, pLoc, 3, 0.5, 0.5, 0.1, 0.05);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(portalCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GhostlyPortal(plugin); }
    }

    // ================================================================
    // 4. SPECTRAL CAGE — 10 glass bars form a cage that materializes
    //    around the player. Semi-transparent, ethereal.
    // ================================================================
    public static class SpectralCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private float opacity = 0; // Simulated via scale

        public SpectralCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_cage", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                BlockDisplayHandle bar = displayBuilder.spawnBlock(
                        center.clone().add(x, 2, z), Material.LIGHT_BLUE_STAINED_GLASS);
                bar.scale(0.01f, 0.01f, 0.01f).glow(100, 150, 200).interpolation(10, 0);
                bars.add(bar);
                spawnedEntities.add(bar.entity());
            }
            // Top crossbars
            bars.add(spawnP(center, 0, 4.5, 0, Material.WHITE_STAINED_GLASS, 0.01f, 0.01f, 0.01f));
            bars.add(spawnP(center, 0, 4.5, 0, Material.WHITE_STAINED_GLASS, 0.01f, 0.01f, 0.01f));

            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.6f, 0.5f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(100, 150, 200).interpolation(10, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Fade in
            if (ticksAlive == 5) {
                for (int i = 0; i < 8; i++) {
                    bars.get(i).animateTo(
                            new Vector3f(-0.15f, -2.0f, -0.15f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.3f, 5.0f, 0.3f), 15);
                }
                bars.get(8).animateTo(
                        new Vector3f(-3.0f, -0.15f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(6.0f, 0.3f, 0.3f), 15);
                bars.get(9).animateTo(
                        new Vector3f(-0.15f, -0.15f, -3.0f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 6.0f), 15);
            }

            // Ghostly flicker
            if (ticksAlive % 30 == 0) {
                int glowR = 80 + (int)(Math.random() * 40);
                int glowG = 130 + (int)(Math.random() * 40);
                for (BlockDisplayHandle bar : bars) bar.glow(glowR, glowG, 200);
            }

            if (ticksAlive % 6 == 0) {
                double angle = Math.random() * Math.PI * 2;
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(Math.cos(angle) * 3, Math.random() * 4, Math.sin(angle) * 3),
                        1, 0.1, 0.1, 0.1, 0.01);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SpectralCage(plugin); }
    }

    // ================================================================
    // 5. HAUNTED MIRROR — Large mirror frame (8) + glass surface (2)
    //    = 10. Floats in front of player. Surface shimmers.
    // ================================================================
    public static class HauntedMirror extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private BlockDisplayHandle glass1, glass2;

        public HauntedMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("haunted_mirror", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 3, -3);

            // Frame (dark wood border)
            frame.add(spawnP(pos, -2, 0, 0, Material.DARK_OAK_PLANKS, 0.4f, 4.0f, 0.4f));
            frame.add(spawnP(pos, 2, 0, 0, Material.DARK_OAK_PLANKS, 0.4f, 4.0f, 0.4f));
            frame.add(spawnP(pos, 0, 2, 0, Material.DARK_OAK_PLANKS, 4.0f, 0.4f, 0.4f));
            frame.add(spawnP(pos, 0, -2, 0, Material.DARK_OAK_PLANKS, 4.0f, 0.4f, 0.4f));
            // Corner accents
            frame.add(spawnP(pos, -2, 2, 0, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.5f));
            frame.add(spawnP(pos, 2, 2, 0, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.5f));
            frame.add(spawnP(pos, -2, -2, 0, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.5f));
            frame.add(spawnP(pos, 2, -2, 0, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.5f));

            // Glass surface
            glass1 = spawnP(pos, 0, 0, 0, Material.TINTED_GLASS, 3.6f, 3.6f, 0.15f);
            glass2 = spawnP(pos, 0, 0, 0.1, Material.WHITE_STAINED_GLASS, 3.2f, 3.2f, 0.1f);

            DisplayBuilder.playSound(pos, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(120, 100, 160).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float bob = (float) Math.sin(ticksAlive * 0.03) * 0.3f;
            // Shimmer effect on glass
            int glowPulse = 100 + (int)(Math.sin(ticksAlive * 0.08) * 50);
            glass1.glow(glowPulse, glowPulse - 20, glowPulse + 40);

            // Reflection particles
            if (ticksAlive % 5 == 0) {
                Location mirrorLoc = c.clone().add(Math.random() * 3 - 1.5, 3 + bob + Math.random() * 3 - 1.5, -3);
                c.getWorld().spawnParticle(Particle.END_ROD, mirrorLoc, 1, 0.1, 0.1, 0.05, 0.01);
            }

            // Eerie whisper
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, -3), Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HauntedMirror(plugin); }
    }

    // ================================================================
    // 6. SOUL CHAIN — 14 soul lantern/glass links form a spectral
    //    chain that wraps around the player area, constricting.
    // ================================================================
    public static class SoulChain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> links = new ArrayList<>();
        private double chainRadius = 6.0;

        public SoulChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_chain", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 14; i++) {
                Material mat = (i % 3 == 0) ? Material.SOUL_LANTERN
                        : (i % 3 == 1) ? Material.LIGHT_BLUE_STAINED_GLASS
                        : Material.WHITE_STAINED_GLASS;
                BlockDisplayHandle link = displayBuilder.spawnBlock(center, mat);
                link.scale(0.5f, 0.5f, 0.5f).glow(80, 160, 180).interpolation(2, 0);
                links.add(link);
                spawnedEntities.add(link.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (chainRadius > 2.0) chainRadius -= 0.015;
            double spinOffset = ticksAlive * 0.03;

            for (int i = 0; i < links.size(); i++) {
                double angle = (2 * Math.PI * i) / links.size() + spinOffset;
                double x = Math.cos(angle) * chainRadius;
                double z = Math.sin(angle) * chainRadius;
                double y = 2.0 + Math.sin(angle * 2 + ticksAlive * 0.05) * 1.0;

                links.get(i).entity().teleport(c.clone().add(x, y, z));
                links.get(i).rotate((float) angle, 0, 1, 0);
            }

            // Spectral particles between links
            if (ticksAlive % 4 == 0) {
                double angle = Math.random() * Math.PI * 2;
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(Math.cos(angle) * chainRadius, 2, Math.sin(angle) * chainRadius),
                        1, 0.2, 0.5, 0.2, 0.01);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulChain(plugin); }
    }

    // ================================================================
    // 7. GHOSTLY SHIP — Ship hull (8), mast (2), sail (2) = 12 blocks
    //    floating through the air. Translucent ghost ship sailing past.
    // ================================================================
    public static class GhostlyShip extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hull = new ArrayList<>();
        private BlockDisplayHandle mast1, mast2, sail1, sail2;
        private float shipX = -12;

        public GhostlyShip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghostly_ship", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(shipX, 5, 0);

            // Hull bottom (4 blocks — boat shape)
            for (int i = 0; i < 4; i++) {
                float width = (i == 0 || i == 3) ? 1.0f : 1.8f;
                hull.add(spawnP(pos, (i - 1.5) * 2.0, 0, 0,
                        Material.WHITE_STAINED_GLASS, width, 0.8f, 2.5f));
            }
            // Hull sides (4)
            hull.add(spawnP(pos, 0, 0.8, 1.2, Material.LIGHT_BLUE_STAINED_GLASS, 6.0f, 0.8f, 0.3f));
            hull.add(spawnP(pos, 0, 0.8, -1.2, Material.LIGHT_BLUE_STAINED_GLASS, 6.0f, 0.8f, 0.3f));
            hull.add(spawnP(pos, 3.5, 0.5, 0, Material.WHITE_STAINED_GLASS, 0.4f, 1.0f, 1.5f)); // Bow
            hull.add(spawnP(pos, -3.5, 0.5, 0, Material.WHITE_STAINED_GLASS, 0.4f, 0.8f, 1.0f)); // Stern

            // Masts
            mast1 = spawnP(pos, 0, 2.5, 0, Material.TINTED_GLASS, 0.2f, 3.0f, 0.2f);
            mast2 = spawnP(pos, -1.5, 2.0, 0, Material.TINTED_GLASS, 0.2f, 2.0f, 0.2f);

            // Sails
            sail1 = spawnP(pos, 0, 3.5, 0, Material.WHITE_STAINED_GLASS, 0.1f, 2.0f, 2.0f);
            sail2 = spawnP(pos, -1.5, 2.5, 0, Material.WHITE_STAINED_GLASS, 0.1f, 1.5f, 1.5f);

            DisplayBuilder.playSound(pos, Sound.ENTITY_BOAT_PADDLE_WATER, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(140, 160, 200).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            shipX += 0.08f;
            float bob = (float) Math.sin(ticksAlive * 0.04) * 0.3f;

            // Mist trail behind ship
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                        c.clone().add(shipX - 4, 5 + bob, 0), 2, 0.5, 0.3, 0.5, 0.005);
                c.getWorld().spawnParticle(Particle.SOUL,
                        c.clone().add(shipX, 5.5 + bob, 0), 1, 1.5, 0.5, 1.0, 0.01);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(shipX, 5, 0),
                        Sound.ENTITY_BOAT_PADDLE_WATER, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GhostlyShip(plugin); }
    }

    // ================================================================
    // 8. SPECTRAL STAIRWAY — 10 glass steps ascending in a spiral,
    //    slowly fading in from bottom to top. Leads to nowhere.
    // ================================================================
    public static class SpectralStairway extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> steps = new ArrayList<>();

        public SpectralStairway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_stairway", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(45.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double angle = i * 0.6;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                float y = i * 0.8f;
                Material mat = (i % 2 == 0) ? Material.WHITE_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle step = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                step.scale(0.01f, 0.01f, 0.01f).glow(140, 160, 220).interpolation(10, 0);
                steps.add(step);
                spawnedEntities.add(step.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < steps.size(); i++) {
                int revealTick = i * 8;
                if (ticksAlive == revealTick) {
                    steps.get(i).animateTo(
                            new Vector3f(-0.75f, -0.15f, -0.75f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.5f, 0.3f, 1.5f), 12);
                    DisplayBuilder.playSound(steps.get(i).entity().getLocation(),
                            Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.3f, 0.5f + i * 0.05f);
                }
            }

            // Ethereal particles on steps
            if (ticksAlive % 8 == 0) {
                int stepIdx = (ticksAlive / 8) % steps.size();
                Location stepLoc = steps.get(stepIdx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.END_ROD, stepLoc, 2, 0.3, 0.2, 0.3, 0.01);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SpectralStairway(plugin); }
    }

    // ================================================================
    // 9. PHANTOM CLOCK — Ghostly clock (12 blocks): face ring (8),
    //    2 hands, center, pendulum. All translucent glass.
    // ================================================================
    public static class PhantomClock extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> faceRing = new ArrayList<>();
        private BlockDisplayHandle minuteHand, hourHand, centerHub;

        public PhantomClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_clock", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location clockPos = center.clone().add(0, 4, 0);

            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 3.0;
                double y = Math.sin(angle) * 3.0;
                BlockDisplayHandle marker = displayBuilder.spawnBlock(
                        clockPos.clone().add(x, y, 0), Material.WHITE_STAINED_GLASS);
                marker.scale(0.6f, 0.6f, 0.3f).glow(150, 170, 220).interpolation(2, 0);
                faceRing.add(marker);
                spawnedEntities.add(marker.entity());
            }

            minuteHand = spawnP(clockPos, 0, 0, 0.1, Material.LIGHT_BLUE_STAINED_GLASS, 0.2f, 2.5f, 0.15f);
            hourHand = spawnP(clockPos, 0, 0, 0.1, Material.PURPLE_STAINED_GLASS, 0.3f, 1.8f, 0.15f);
            centerHub = spawnP(clockPos, 0, 0, 0.1, Material.SEA_LANTERN, 0.5f, 0.5f, 0.3f);

            DisplayBuilder.playSound(clockPos, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 0.5f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(150, 170, 220).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Erratic hand movement (ghostly, time is broken)
            double minuteAngle = ticksAlive * 0.04 + Math.sin(ticksAlive * 0.1) * 0.5;
            double hourAngle = ticksAlive * 0.015 + Math.cos(ticksAlive * 0.07) * 0.3;

            minuteHand.rotate((float) minuteAngle, 0, 0, 1);
            hourHand.rotate((float) hourAngle, 0, 0, 1);

            // Spectral particles
            if (ticksAlive % 6 == 0) {
                Location clockPos = c.clone().add(0, 4, 0);
                c.getWorld().spawnParticle(Particle.SOUL, clockPos, 2, 2, 2, 0.2, 0.01);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 4, 0), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f,
                        0.4f + (float)(Math.random() * 0.4));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PhantomClock(plugin); }
    }

    // ================================================================
    // 10. SOUL WHIRLPOOL — 12 blocks spiral downward into a central
    //     point, like souls being drained. Funnel shape.
    // ================================================================
    public static class SoulWhirlpool extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> souls = new ArrayList<>();

        public SoulWhirlpool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_whirlpool", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Material mat = (i % 3 == 0) ? Material.SOUL_LANTERN
                        : (i % 3 == 1) ? Material.LIGHT_BLUE_STAINED_GLASS
                        : Material.WHITE_STAINED_GLASS;
                BlockDisplayHandle soul = displayBuilder.spawnBlock(center, mat);
                soul.scale(0.6f, 0.6f, 0.6f).glow(80, 160, 200).interpolation(2, 0);
                souls.add(soul);
                spawnedEntities.add(soul.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < souls.size(); i++) {
                double t = (double) i / souls.size();
                double spiralAngle = t * Math.PI * 4 + ticksAlive * 0.06;
                double radius = (1.0 - t) * 5.0 + 0.5;
                double height = t * 6.0;
                double x = Math.cos(spiralAngle) * radius;
                double z = Math.sin(spiralAngle) * radius;

                souls.get(i).entity().teleport(c.clone().add(x, height, z));
                souls.get(i).rotate(ticksAlive * 0.05f + i, 0, 1, 0);
            }

            // Soul drain particles at center
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL, c.clone().add(0, 0.5, 0),
                        3, 0.3, 0.2, 0.3, 0.03);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulWhirlpool(plugin); }
    }

    // ================================================================
    // 11. GHOSTLY ARMY — 10 "soldier" silhouettes (1 block each with
    //     tall thin proportions) marching in formation toward player.
    // ================================================================
    public static class GhostlyArmy extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> soldiers = new ArrayList<>();
        private float marchZ = -10;

        public GhostlyArmy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghostly_army", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 2 rows of 5
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    double x = (col - 2) * 2.0;
                    double z = -10 - row * 2;
                    BlockDisplayHandle soldier = displayBuilder.spawnBlock(
                            center.clone().add(x, 1, z), Material.TINTED_GLASS);
                    soldier.scale(0.8f, 2.5f, 0.4f).glow(100, 120, 150).interpolation(2, 0);
                    soldiers.add(soldier);
                    spawnedEntities.add(soldier.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ZOMBIE_STEP, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            marchZ += 0.06f;

            for (int i = 0; i < soldiers.size(); i++) {
                int row = i / 5;
                int col = i % 5;
                double x = (col - 2) * 2.0;
                double z = marchZ - row * 2;
                // Marching bob
                float bob = (float) Math.abs(Math.sin(ticksAlive * 0.08 + i * 0.5)) * 0.2f;
                soldiers.get(i).entity().teleport(c.clone().add(x, 1 + bob, z));
            }

            // Marching sounds
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 0, marchZ), Sound.ENTITY_ZOMBIE_STEP, 0.4f, 0.4f);
            }

            // Ghost trail
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                        c.clone().add(Math.random() * 8 - 4, 0.5, marchZ - 2),
                        1, 0.5, 0.2, 0.5, 0.005);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GhostlyArmy(plugin); }
    }

    // ================================================================
    // 12. ETHEREAL ORB — 12 tiny glass blocks form a floating orb
    //     that pulses with energy, orbiting the player. Emits light.
    // ================================================================
    public static class EtherealOrb extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> orbParts = new ArrayList<>();
        private double orbitAngle = 0;

        public EtherealOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ethereal_orb", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(50.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            List<BlockDisplayHandle> sphere = displayBuilder.spawnSphere(
                    center.clone().add(4, 3, 0), Material.SEA_LANTERN, 1.2, 12);
            for (BlockDisplayHandle b : sphere) {
                b.scale(0.5f, 0.5f, 0.5f).glow(150, 200, 255).interpolation(2, 0);
                orbParts.add(b);
                spawnedEntities.add(b.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            orbitAngle += 0.05;
            double orbX = Math.cos(orbitAngle) * 4.0;
            double orbZ = Math.sin(orbitAngle) * 4.0;
            float bob = (float) Math.sin(ticksAlive * 0.06) * 0.5f;
            Location orbCenter = c.clone().add(orbX, 3 + bob, orbZ);

            // Pulse scale
            float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.1) * 0.3f;

            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < orbParts.size(); i++) {
                double yi = 1 - (2.0 * i / (orbParts.size() - 1));
                double rAtY = Math.sqrt(1 - yi * yi);
                double theta = goldenAngle * i + ticksAlive * 0.03;
                orbParts.get(i).entity().teleport(orbCenter.clone().add(
                        Math.cos(theta) * rAtY * 1.2 * pulse,
                        yi * 1.2 * pulse,
                        Math.sin(theta) * rAtY * 1.2 * pulse));
            }

            // Light particles
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, orbCenter, 2, 0.5, 0.5, 0.5, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(orbCenter, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EtherealOrb(plugin); }
    }

    // ================================================================
    // 13. NIGHTMARE THRONE — Ornate throne (14 blocks): seat, back,
    //     armrests, crown/top, base steps. Materializes piece by piece.
    // ================================================================
    public static class NightmareThrone extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();

        public NightmareThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_throne", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 0, -3);

            // Base steps (3)
            parts.add(spawnP(pos, 0, 0, 0, Material.DEEPSLATE_BRICKS, 4.0f, 0.4f, 3.0f));
            parts.add(spawnP(pos, 0, 0.4, -0.3, Material.POLISHED_DEEPSLATE, 3.5f, 0.4f, 2.5f));
            parts.add(spawnP(pos, 0, 0.8, -0.6, Material.DEEPSLATE_TILES, 3.0f, 0.4f, 2.0f));

            // Seat (1)
            parts.add(spawnP(pos, 0, 1.2, -0.8, Material.CRYING_OBSIDIAN, 2.5f, 0.5f, 1.5f));

            // Back (3 — tall)
            parts.add(spawnP(pos, 0, 2.5, -1.5, Material.POLISHED_BLACKSTONE, 2.5f, 2.0f, 0.5f));
            parts.add(spawnP(pos, 0, 4.5, -1.5, Material.DEEPSLATE_BRICKS, 2.0f, 1.5f, 0.4f));
            parts.add(spawnP(pos, 0, 6.0, -1.5, Material.CRYING_OBSIDIAN, 1.5f, 1.0f, 0.4f));

            // Armrests (2)
            parts.add(spawnP(pos, -1.5, 1.8, -0.5, Material.POLISHED_BLACKSTONE, 0.5f, 1.0f, 1.5f));
            parts.add(spawnP(pos, 1.5, 1.8, -0.5, Material.POLISHED_BLACKSTONE, 0.5f, 1.0f, 1.5f));

            // Armrest tops
            parts.add(spawnP(pos, -1.5, 2.5, -0.2, Material.AMETHYST_BLOCK, 0.6f, 0.4f, 0.6f));
            parts.add(spawnP(pos, 1.5, 2.5, -0.2, Material.AMETHYST_BLOCK, 0.6f, 0.4f, 0.6f));

            // Crown/top accent (3)
            parts.add(spawnP(pos, 0, 7.0, -1.5, Material.AMETHYST_BLOCK, 0.8f, 0.8f, 0.5f));
            parts.add(spawnP(pos, -0.8, 6.5, -1.5, Material.BUDDING_AMETHYST, 0.4f, 0.6f, 0.3f));
            parts.add(spawnP(pos, 0.8, 6.5, -1.5, Material.BUDDING_AMETHYST, 0.4f, 0.6f, 0.3f));

            DisplayBuilder.playSound(pos, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(0.01f, 0.01f, 0.01f).glow(120, 80, 180).interpolation(8, 0);
            spawnedEntities.add(h.entity());
            // Store target scales for reveal
            parts.add(h);
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Reveal parts one by one every 6 ticks
            int revealIdx = ticksAlive / 6;
            if (revealIdx < parts.size() && ticksAlive % 6 == 0) {
                // Since we can't store target scales, just set to a visible size
                parts.get(revealIdx).scale(2.0f, 1.0f, 2.0f);
                DisplayBuilder.playSound(parts.get(revealIdx).entity().getLocation(),
                        Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.5f + revealIdx * 0.03f);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        parts.get(revealIdx).entity().getLocation(), 5, 0.3, 0.3, 0.3, 0.05);
            }

            // Ambient dark energy
            if (ticksAlive % 8 == 0 && ticksAlive > parts.size() * 6) {
                Location throneCenter = c.clone().add(0, 3, -3);
                DisplayBuilder.dustParticles(throneCenter, 3, 1.5, 120, 80, 180, 1.2f);
                c.getWorld().spawnParticle(Particle.SOUL, throneCenter, 1, 0.5, 1, 0.5, 0.02);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 3, -3), Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareThrone(plugin); }
    }
}
