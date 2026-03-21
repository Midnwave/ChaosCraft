package com.blockforge.chaoscraft.modes.seer.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Seer Mode — VOID FORMATIONS
 * 13 void-themed block display attacks.
 * Palette: Deep purple (80,0,160), Void black (20,0,40), Psychic magenta (200,0,180)
 */
public final class VoidFormations {

    private VoidFormations() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidTendril(plugin));
        registry.register(new RiftTear(plugin));
        registry.register(new DarkPortal(plugin));
        registry.register(new AbyssPillar(plugin));
        registry.register(new VoidSphere(plugin));
        registry.register(new ShadowHand(plugin));
        registry.register(new NullCube(plugin));
        registry.register(new EntropyRing(plugin));
        registry.register(new VoidFountain(plugin));
        registry.register(new DarkSpire(plugin));
        registry.register(new RiftStorm(plugin));
        registry.register(new AbyssalMaw(plugin));
        registry.register(new VoidAnchor(plugin));
    }

    // ================================================================
    // 1. VOID TENDRIL — Crying obsidian tentacle curving up, sways
    // ================================================================
    public static class VoidTendril extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tendrilBlocks = new ArrayList<>();

        public VoidTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendril", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                double y = i * 0.6;
                double curve = Math.sin(i * 0.3) * 1.5;
                Location loc = center.clone().add(curve, y, curve * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                float s = 0.5f - (i * 0.02f);
                h.scale(s, 0.6f, s).glow(80, 0, 160).interpolation(3, 0);
                tendrilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double sway = Math.sin(ticksAlive * 0.05) * 0.8;
            for (int i = 0; i < tendrilBlocks.size(); i++) {
                double y = i * 0.6;
                double curve = Math.sin(i * 0.3 + ticksAlive * 0.03) * 1.5 + sway * (i / 15.0);
                tendrilBlocks.get(i).entity().teleport(c.clone().add(curve, y, curve * 0.5));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 4, 2.0, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendril(plugin); }
    }

    // ================================================================
    // 2. RIFT TEAR — Obsidian+purple glass vertical crack with pull effect
    // ================================================================
    public static class RiftTear extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> riftBlocks = new ArrayList<>();

        public RiftTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_tear", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 14; i++) {
                Material mat = i % 2 == 0 ? Material.OBSIDIAN : Material.PURPLE_STAINED_GLASS;
                double y = i * 0.5;
                double ox = (Math.random() - 0.5) * 0.6;
                Location loc = center.clone().add(ox, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.5f, 0.2f).glow(20, 0, 40).interpolation(3, 0);
                riftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Shimmer effect
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < riftBlocks.size(); i++) {
                    double y = i * 0.5;
                    double ox = Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.4;
                    riftBlocks.get(i).entity().teleport(c.clone().add(ox, y, 0));
                }
            }
            // Pull effect
            if (ticksAlive % 4 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 16) {
                        Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.12);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 1.0, 80, 0, 160, 1.5f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftTear(plugin); }
    }

    // ================================================================
    // 3. DARK PORTAL — Crying obsidian + blackstone archway with purple particles
    // ================================================================
    public static class DarkPortal extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> portalBlocks = new ArrayList<>();

        public DarkPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_portal", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Left pillar
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-2, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.8f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                portalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pillar
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(2, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.8f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                portalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Arch top
            for (int i = 0; i < 6; i++) {
                double x = (i - 2.5) * 0.9;
                Location loc = center.clone().add(x, 4.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.7f, 0.6f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                portalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 8, 1.5, 200, 0, 180, 1.2f);
            }
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(0, 2, 0), 5, 1.0, 1.5, 0.5, 0.1);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkPortal(plugin); }
    }

    // ================================================================
    // 4. ABYSS PILLAR — Blackstone + sculk pillar erupting from ground
    // ================================================================
    public static class AbyssPillar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();

        public AbyssPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Material mat = i % 2 == 0 ? Material.BLACKSTONE : Material.SCULK;
                Location loc = center.clone().add(0, -5 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.8f, 0.7f).glow(20, 0, 40).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double rise = Math.min(5.0, ticksAlive * 0.25);
            for (int i = 0; i < pillarBlocks.size(); i++) {
                pillarBlocks.get(i).entity().teleport(c.clone().add(0, -5 + rise + i * 0.8, 0));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, 2.0, 20, 0, 40, 1.5f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssPillar(plugin); }
    }

    // ================================================================
    // 5. VOID SPHERE — Obsidian sphere that implodes then explodes
    // ================================================================
    public static class VoidSphere extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphereBlocks = new ArrayList<>();
        private boolean exploded = false;

        public VoidSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_sphere", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 16; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double r = 4.0;
                Location loc = center.clone().add(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 3, Math.sin(theta) * Math.sin(phi) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(20, 0, 40).interpolation(3, 0);
                sphereBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive < 25) {
                // Implode: contract from r=4 to r=0
                double r = 4.0 * (1.0 - ticksAlive / 25.0);
                for (int i = 0; i < sphereBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    sphereBlocks.get(i).entity().teleport(c.clone().add(
                            Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 3, Math.sin(theta) * Math.sin(phi) * r));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, (float) r, 80, 0, 160, 1.5f);
                }
            }
            if (!exploded && ticksAlive >= 25) {
                exploded = true;
                double r = 6.0;
                for (int i = 0; i < sphereBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    sphereBlocks.get(i).entity().teleport(c.clone().add(
                            Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 3, Math.sin(theta) * Math.sin(phi) * r));
                }
                triggerImpactDamage(c.clone().add(0, 3, 0));
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 30, 5.0, 200, 0, 50, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSphere(plugin); }
    }

    // ================================================================
    // 6. SHADOW HAND — Blackstone hand shape reaching up
    // ================================================================
    public static class ShadowHand extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> handBlocks = new ArrayList<>();

        public ShadowHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_hand", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Palm: 4 blocks
            for (int i = 0; i < 4; i++) {
                double ox = (i % 2 - 0.5) * 0.8;
                double oz = (i / 2 - 0.5) * 0.8;
                Location loc = center.clone().add(ox, 0, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.7f, 0.5f, 0.7f).glow(20, 0, 40).interpolation(3, 0);
                handBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Fingers: 5 x 2 segments each
            double[] fingerX = {-1.5, -0.75, 0, 0.75, 1.5};
            for (int f = 0; f < 5; f++) {
                for (int seg = 0; seg < 2; seg++) {
                    Location loc = center.clone().add(fingerX[f], 0.5 + seg * 0.8, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.4f, 0.7f, 0.4f).glow(20, 0, 40).interpolation(3, 0);
                    handBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double rise = Math.min(3.0, ticksAlive * 0.15);
            // Curl fingers over time
            double curl = Math.min(1.0, ticksAlive / 100.0);
            for (int i = 0; i < handBlocks.size(); i++) {
                Entity e = handBlocks.get(i).entity();
                Location current = e.getLocation();
                e.teleport(current.clone().add(0, rise > current.getY() - c.getY() ? 0.05 : 0, 0));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, rise, 0), 6, 2.0, 20, 0, 40, 1.2f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowHand(plugin); }
    }

    // ================================================================
    // 7. NULL CUBE — Perfect obsidian cube rotating on all axes
    // ================================================================
    public static class NullCube extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cubeBlocks = new ArrayList<>();

        public NullCube(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_cube", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 10 blocks forming cube edges
            double[][] offsets = {{-1,-1,-1},{1,-1,-1},{-1,1,-1},{1,1,-1},{-1,-1,1},{1,-1,1},{-1,1,1},{1,1,1},{0,0,-1},{0,0,1}};
            for (double[] off : offsets) {
                Location loc = center.clone().add(off[0], off[1] + 3, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                cubeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double[][] offsets = {{-1,-1,-1},{1,-1,-1},{-1,1,-1},{1,1,-1},{-1,-1,1},{1,-1,1},{-1,1,1},{1,1,1},{0,0,-1},{0,0,1}};
            double angleY = ticksAlive * 0.05;
            double angleX = ticksAlive * 0.03;
            for (int i = 0; i < cubeBlocks.size(); i++) {
                double x = offsets[i][0], y = offsets[i][1], z = offsets[i][2];
                // Rotate Y
                double rx = x * Math.cos(angleY) - z * Math.sin(angleY);
                double rz = x * Math.sin(angleY) + z * Math.cos(angleY);
                // Rotate X
                double ry = y * Math.cos(angleX) - rz * Math.sin(angleX);
                double rz2 = y * Math.sin(angleX) + rz * Math.cos(angleX);
                cubeBlocks.get(i).entity().teleport(c.clone().add(rx, ry + 3, rz2));
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 2.0, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullCube(plugin); }
    }

    // ================================================================
    // 8. ENTROPY RING — Crying obsidian ring descending from Y+10
    // ================================================================
    public static class EntropyRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();

        public EntropyRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_ring", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 4.0, 10, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.4f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double y = 10 - (ticksAlive / (double) config.getDurationTicks()) * 10;
            y = Math.max(0.2, y);
            double rot = ticksAlive * 0.03;
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 16 + rot;
                ringBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * 4.0, y, Math.sin(angle) * 4.0));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, y, 0), 8, 4.0, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EntropyRing(plugin); }
    }

    // ================================================================
    // 9. VOID FOUNTAIN — Blackstone + purple glass fountain spraying upward
    // ================================================================
    public static class VoidFountain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fountainBlocks = new ArrayList<>();

        public VoidFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Base: 6 blackstone
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.6f, 0.5f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                fountainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Spray: 8 purple glass rising
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double r = 0.5 + (i % 3) * 0.3;
                Location loc = center.clone().add(Math.cos(angle) * r, 1 + i * 0.4, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.3f, 0.4f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                fountainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Animate spray blocks up and down
            for (int i = 6; i < fountainBlocks.size(); i++) {
                int idx = i - 6;
                double angle = (2 * Math.PI * idx) / 8;
                double r = 0.5 + (idx % 3) * 0.3;
                double y = 1 + Math.abs(Math.sin(ticksAlive * 0.08 + idx * 0.5)) * 4;
                fountainBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 1.5, 80, 0, 160, 1.2f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFountain(plugin); }
    }

    // ================================================================
    // 10. DARK SPIRE — Sculk + obsidian twisted spire
    // ================================================================
    public static class DarkSpire extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spireBlocks = new ArrayList<>();

        public DarkSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_spire", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 18; i++) {
                Material mat = i % 2 == 0 ? Material.SCULK : Material.OBSIDIAN;
                double twist = i * 0.3;
                double r = 1.5 - (i / 18.0) * 1.0;
                Location loc = center.clone().add(Math.cos(twist) * r, i * 0.5, Math.sin(twist) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.6f - (i * 0.02f);
                h.scale(s, 0.5f, s).glow(20, 0, 40).interpolation(3, 0);
                spireBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double twistOffset = ticksAlive * 0.02;
            for (int i = 0; i < spireBlocks.size(); i++) {
                double twist = i * 0.3 + twistOffset;
                double r = 1.5 - (i / 18.0) * 1.0;
                spireBlocks.get(i).entity().teleport(c.clone().add(Math.cos(twist) * r, i * 0.5, Math.sin(twist) * r));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4, 0), 4, 1.5, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSpire(plugin); }
    }

    // ================================================================
    // 11. RIFT STORM — Crying obsidian fragments orbiting chaotically
    // ================================================================
    public static class RiftStorm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stormBlocks = new ArrayList<>();
        private static final Random RNG = new Random();
        private final double[] phaseOffsets = new double[12];

        public RiftStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_storm", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                phaseOffsets[i] = RNG.nextDouble() * Math.PI * 2;
                Location loc = center.clone().add(RNG.nextDouble() * 6 - 3, 1 + RNG.nextDouble() * 4, RNG.nextDouble() * 6 - 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                stormBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            for (int i = 0; i < stormBlocks.size(); i++) {
                double speed = 0.1 + (i % 3) * 0.05;
                double r = 2.0 + (i % 4);
                double angle = ticksAlive * speed + phaseOffsets[i];
                double y = 2 + Math.sin(ticksAlive * 0.08 + i) * 2;
                stormBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 8, 4.0, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftStorm(plugin); }
    }

    // ================================================================
    // 12. ABYSSAL MAW — Blackstone jaw/mouth opening downward, impact when closes
    // ================================================================
    public static class AbyssalMaw extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> upperJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerJaw = new ArrayList<>();
        private boolean impacted = false;

        public AbyssalMaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_maw", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double x = (i - 3.5) * 0.6;
                Location upper = center.clone().add(x, 6, 0);
                BlockDisplayHandle hu = displayBuilder.spawnBlock(upper, Material.BLACKSTONE);
                hu.scale(0.6f, 0.5f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                upperJaw.add(hu);
                spawnedEntities.add(hu.entity());
                Location lower = center.clone().add(x, 0, 0);
                BlockDisplayHandle hl = displayBuilder.spawnBlock(lower, Material.BLACKSTONE);
                hl.scale(0.6f, 0.5f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                lowerJaw.add(hl);
                spawnedEntities.add(hl.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double closeProgress = Math.min(1.0, ticksAlive / 30.0);
            double upperY = 6 - closeProgress * 3;
            double lowerY = 0 + closeProgress * 3;
            for (int i = 0; i < 8; i++) {
                double x = (i - 3.5) * 0.6;
                upperJaw.get(i).entity().teleport(c.clone().add(x, upperY, 0));
                lowerJaw.get(i).entity().teleport(c.clone().add(x, lowerY, 0));
            }
            if (!impacted && ticksAlive >= 30) {
                impacted = true;
                triggerImpactDamage(c.clone().add(0, 3, 0));
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 20, 3.0, 200, 0, 50, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.3f);
            }
            if (ticksAlive % 4 == 0 && !impacted) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, 2.0, 20, 0, 40, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssalMaw(plugin); }
    }

    // ================================================================
    // 13. VOID ANCHOR — Obsidian + iron heavy shape falling slowly then slam
    // ================================================================
    public static class VoidAnchor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> anchorBlocks = new ArrayList<>();
        private boolean impacted = false;

        public VoidAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_anchor", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Heavy anchor shape at Y+15
            double[][] offsets = {{0,0,0},{0,1,0},{0,2,0},{-1,0,0},{1,0,0},{-1,-1,0},{1,-1,0},{0,-1,-1},{0,-1,1},
                    {0,3,0},{-0.5,3,0},{0.5,3,0},{0,-1,0},{0,-2,0}};
            for (int i = 0; i < offsets.length; i++) {
                Material mat = i < 7 ? Material.OBSIDIAN : Material.IRON_BLOCK;
                Location loc = center.clone().add(offsets[i][0] * 0.5, 15 + offsets[i][1] * 0.5, offsets[i][2] * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.5f, 0.5f).glow(20, 0, 40).interpolation(3, 0);
                anchorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double[][] offsets = {{0,0,0},{0,1,0},{0,2,0},{-1,0,0},{1,0,0},{-1,-1,0},{1,-1,0},{0,-1,-1},{0,-1,1},
                    {0,3,0},{-0.5,3,0},{0.5,3,0},{0,-1,0},{0,-2,0}};
            // Slow descent, accelerating
            double speed = ticksAlive * 0.008;
            double dropY = 15 - (speed * ticksAlive);
            dropY = Math.max(0, dropY);
            for (int i = 0; i < anchorBlocks.size(); i++) {
                anchorBlocks.get(i).entity().teleport(c.clone().add(
                        offsets[i][0] * 0.5, dropY + offsets[i][1] * 0.5, offsets[i][2] * 0.5));
            }
            if (!impacted && dropY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 25, 3.0, 20, 0, 40, 2.5f);
                DisplayBuilder.dustParticles(c, 15, 2.0, 80, 0, 160, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            }
            if (!impacted && ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, dropY, 0), 4, 1.0, 80, 0, 160, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidAnchor(plugin); }
    }
}
