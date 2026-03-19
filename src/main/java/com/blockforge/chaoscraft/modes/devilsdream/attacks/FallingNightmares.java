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
 * Devil's Dream — FALLING NIGHTMARE ATTACKS
 * 13 falling/raining BlockDisplay attacks featuring skull bombardments,
 * blood rain, demonic debris, and collapsing nightmare structures.
 * All falling attacks TRACK the player and deal impact damage on landing.
 */
public final class FallingNightmares {

    private FallingNightmares() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SkullBombardment(plugin));
        registry.register(new NightmareAnvil(plugin));
        registry.register(new BloodRain(plugin));
        registry.register(new FallingEyes(plugin));
        registry.register(new DemonicDebris(plugin));
        registry.register(new SoulDrop(plugin));
        registry.register(new NightmareHail(plugin));
        registry.register(new CollapsingCeiling(plugin));
        registry.register(new MeteorSwarm(plugin));
        registry.register(new FallingCage(plugin));
        registry.register(new NightmareChandelier(plugin));
        registry.register(new BoneAvalanche(plugin));
        registry.register(new PillarCollapse(plugin));
    }

    // ================================================================
    // 1. SKULL BOMBARDMENT — 10 bone block "skulls" rain down from
    //    above, each tracking the player. Staggered spawns. Impact
    //    creates bone fragment particles.
    // ================================================================
    public static class SkullBombardment extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> skulls = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> fallSpeed = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();
        private static final int COUNT = 10;

        public SkullBombardment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("skull_bombardment", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(70.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 5.0;
                xOff.add(Math.cos(angle) * dist);
                zOff.add(Math.sin(angle) * dist);
                yPos.add(18.0f + (float)(Math.random() * 5));
                fallSpeed.add(0.2f + (float)(Math.random() * 0.15));
                delays.add(i * 6);
                landed.add(false);
                skulls.add(null);
            }
            DisplayBuilder.playSound(center.clone().add(0, 18, 0), Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < COUNT; i++) {
                if (ticksAlive < delays.get(i) || landed.get(i)) continue;

                if (skulls.get(i) == null) {
                    Location spawnLoc = c.clone().add(xOff.get(i), yPos.get(i), zOff.get(i));
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(spawnLoc, Material.BONE_BLOCK);
                    skull.scale(1.5f, 1.2f, 1.5f).glow(200, 180, 150).interpolation(1, 0);
                    skulls.set(i, skull);
                    spawnedEntities.add(skull.entity());
                    DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_SKELETON_HURT, 0.5f, 0.4f);
                }

                float y = yPos.get(i) - fallSpeed.get(i);
                fallSpeed.set(i, fallSpeed.get(i) + 0.01f); // Accelerate
                yPos.set(i, y);
                skulls.get(i).entity().teleport(c.clone().add(xOff.get(i), y, zOff.get(i)));
                skulls.get(i).rotate(ticksAlive * 0.1f, 1, 0, 1);

                if (ticksAlive % 4 == 0) {
                    Location trail = c.clone().add(xOff.get(i), y + 0.5, zOff.get(i));
                    DisplayBuilder.dustParticles(trail, 2, 0.2, 200, 180, 150, 1.0f);
                }

                if (y <= 0) {
                    landed.set(i, true);
                    Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                    triggerImpactDamage(impLoc);
                    c.getWorld().spawnParticle(Particle.BLOCK, impLoc, 15, 0.5, 0.3, 0.5, 0.1,
                            Material.BONE_BLOCK.createBlockData());
                    DisplayBuilder.playSound(impLoc, Sound.ENTITY_SKELETON_DEATH, 0.7f, 0.4f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SkullBombardment(plugin); }
    }

    // ================================================================
    // 2. NIGHTMARE ANVIL — Giant anvil (12 blocks forming anvil shape)
    //    hovers above player, then slams down with massive impact.
    // ================================================================
    public static class NightmareAnvil extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float anvilY = 15;
        private boolean falling = false;
        private boolean impacted = false;

        public NightmareAnvil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_anvil", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(100.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, anvilY, 0);

            // Base (wide, 4 blocks)
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z <= 1; z++) {
                    parts.add(spawnP(pos, x * 1.2, 0, z * 1.2 - 0.6, Material.BLACKSTONE, 1.2f, 0.8f, 1.2f));
                }
            }
            // Waist (narrow, 2)
            parts.add(spawnP(pos, 0, 1.0, 0, Material.POLISHED_BLACKSTONE, 1.0f, 1.2f, 1.0f));
            parts.add(spawnP(pos, 0, 2.2, 0, Material.POLISHED_BLACKSTONE, 0.8f, 1.0f, 0.8f));
            // Top surface (wide, 4)
            for (int x = -1; x <= 0; x++) {
                for (int z = -1; z <= 0; z++) {
                    parts.add(spawnP(pos, x * 1.5 + 0.75, 3.2, z * 1.5 + 0.75, Material.BLACKSTONE, 1.5f, 0.6f, 1.5f));
                }
            }
            // Horn
            parts.add(spawnP(pos, 1.8, 3.2, 0, Material.NETHERRACK, 0.6f, 0.8f, 0.6f));

            DisplayBuilder.playSound(pos, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(60, 50, 60).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            if (ticksAlive == 50) {
                falling = true;
                DisplayBuilder.playSound(c.clone().add(0, anvilY, 0), Sound.ENTITY_TNT_PRIMED, 1.0f, 0.5f);
            }

            if (falling) {
                anvilY -= 0.7f;
                if (anvilY <= 0.5f) {
                    anvilY = 0.5f;
                    impacted = true;
                    triggerImpactDamage(c);
                    c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 2, 1, 1, 1, 0);
                    DisplayBuilder.particleRing(c, 7.0, Particle.SMOKE, 30, null);
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                }
            } else {
                anvilY = 15 + (float) Math.sin(ticksAlive * 0.05) * 0.3f;
            }

            // Shadow on ground
            if (!impacted && ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), 3.0, Particle.SMOKE, 12, null);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareAnvil(plugin); }
    }

    // ================================================================
    // 3. BLOOD RAIN — 15 small red concrete blocks rain down rapidly,
    //    tracking the player. Leaves red dust on impact.
    // ================================================================
    public static class BloodRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> drops = new ArrayList<>();
        private final List<Float> yPositions = new ArrayList<>();
        private final List<Float> speeds = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public BloodRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_rain", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(35.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(250);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                xOff.add(Math.random() * 10 - 5);
                zOff.add(Math.random() * 10 - 5);
                yPositions.add(15.0f + (float)(Math.random() * 5));
                speeds.add(0.25f + (float)(Math.random() * 0.2));
                delays.add(i * 3);
                landed.add(false);
                drops.add(null);
            }
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 15; i++) {
                if (ticksAlive < delays.get(i) || landed.get(i)) continue;

                if (drops.get(i) == null) {
                    Location loc = c.clone().add(xOff.get(i), yPositions.get(i), zOff.get(i));
                    BlockDisplayHandle drop = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                    drop.scale(0.4f, 0.6f, 0.4f).glow(180, 20, 20).interpolation(1, 0);
                    drops.set(i, drop);
                    spawnedEntities.add(drop.entity());
                }

                float y = yPositions.get(i) - speeds.get(i);
                yPositions.set(i, y);
                drops.get(i).entity().teleport(c.clone().add(xOff.get(i), y, zOff.get(i)));

                if (y <= 0) {
                    landed.set(i, true);
                    Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                    triggerImpactDamage(impLoc);
                    DisplayBuilder.dustParticles(impLoc, 5, 0.5, 180, 20, 20, 1.5f);
                    DisplayBuilder.playSound(impLoc, Sound.ENTITY_SLIME_SQUISH, 0.3f, 0.5f);
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BloodRain(plugin); }
    }

    // ================================================================
    // 4. FALLING EYES — 10 sea lantern "eyes" fall from sky,
    //    watching the player. They slow before impact then drop fast.
    // ================================================================
    public static class FallingEyes extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> speeds = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public FallingEyes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_eyes", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 6;
                xOff.add(Math.cos(angle) * dist);
                zOff.add(Math.sin(angle) * dist);
                yPos.add(16.0f + (float)(Math.random() * 4));
                speeds.add(0.15f);
                delays.add(i * 8);
                landed.add(false);
                eyes.add(null);
            }
            DisplayBuilder.playSound(center.clone().add(0, 16, 0), Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 10; i++) {
                if (ticksAlive < delays.get(i) || landed.get(i)) continue;

                if (eyes.get(i) == null) {
                    Location loc = c.clone().add(xOff.get(i), yPos.get(i), zOff.get(i));
                    BlockDisplayHandle eye = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                    eye.scale(1.2f, 1.2f, 1.2f).glow(220, 220, 180).interpolation(2, 0);
                    eyes.set(i, eye);
                    spawnedEntities.add(eye.entity());
                }

                float y = yPos.get(i);
                float speed = speeds.get(i);

                // Slow down between 5-3 blocks, then speed up
                if (y > 3 && y < 5) {
                    speed = Math.max(0.03f, speed * 0.95f);
                } else if (y <= 3) {
                    speed = Math.min(0.8f, speed + 0.05f);
                }
                speeds.set(i, speed);

                y -= speed;
                yPos.set(i, y);
                eyes.get(i).entity().teleport(c.clone().add(xOff.get(i), y, zOff.get(i)));

                // Staring particle
                if (ticksAlive % 6 == 0) {
                    Location eyeLoc = c.clone().add(xOff.get(i), y, zOff.get(i));
                    c.getWorld().spawnParticle(Particle.END_ROD, eyeLoc, 1, 0.2, 0.2, 0.2, 0.01);
                }

                if (y <= 0) {
                    landed.set(i, true);
                    Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                    triggerImpactDamage(impLoc);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impLoc, 10, 0.5, 0.3, 0.5, 0.05);
                    DisplayBuilder.playSound(impLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.5f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FallingEyes(plugin); }
    }

    // ================================================================
    // 5. DEMONIC DEBRIS — 12 mixed nether blocks tumble from above,
    //    spinning as they fall. Heavy blocks fall faster.
    // ================================================================
    public static class DemonicDebris extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> speeds = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Float> rotSpeeds = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public DemonicDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_debris", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(65.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Material[] mats = {Material.NETHERRACK, Material.BLACKSTONE, Material.NETHER_BRICKS,
                    Material.SOUL_SAND, Material.MAGMA_BLOCK, Material.BASALT};
            for (int i = 0; i < 12; i++) {
                xOff.add(Math.random() * 8 - 4);
                zOff.add(Math.random() * 8 - 4);
                yPos.add(16.0f + (float)(Math.random() * 6));
                speeds.add(0.15f + (float)(Math.random() * 0.2));
                rotSpeeds.add((float)(Math.random() * 0.2 - 0.1));
                delays.add((int)(Math.random() * 40));
                landed.add(false);

                BlockDisplayHandle d = displayBuilder.spawnBlock(
                        center.clone().add(xOff.get(i), yPos.get(i), zOff.get(i)), mats[i % mats.length]);
                float s = 0.8f + (float)(Math.random() * 1.0);
                d.scale(s, s, s).glow(120, 60, 30).interpolation(1, 0);
                debris.add(d);
                spawnedEntities.add(d.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            for (int i = 0; i < debris.size(); i++) {
                if (ticksAlive < delays.get(i) || landed.get(i)) continue;
                float y = yPos.get(i) - speeds.get(i);
                speeds.set(i, speeds.get(i) + 0.008f);
                yPos.set(i, y);
                debris.get(i).entity().teleport(c.clone().add(xOff.get(i), Math.max(0, y), zOff.get(i)));
                debris.get(i).rotate(ticksAlive * rotSpeeds.get(i), 1, 1, 0);

                if (y <= 0) {
                    landed.set(i, true);
                    Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                    triggerImpactDamage(impLoc);
                    c.getWorld().spawnParticle(Particle.SMOKE, impLoc, 8, 0.5, 0.3, 0.5, 0.05);
                    DisplayBuilder.playSound(impLoc, Sound.BLOCK_BASALT_BREAK, 0.5f, 0.5f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonicDebris(plugin); }
    }

    // ================================================================
    // 6. SOUL DROP — Single large soul sand sphere (12 blocks) falls
    //    slowly, tracking player. On impact, releases soul particles.
    // ================================================================
    public static class SoulDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphere = new ArrayList<>();
        private float dropY = 15;
        private boolean impacted = false;

        public SoulDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drop", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(80.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, dropY, 0);
            List<BlockDisplayHandle> s = displayBuilder.spawnSphere(pos, Material.SOUL_SAND, 2.0, 12);
            for (BlockDisplayHandle b : s) {
                b.scale(1.0f, 1.0f, 1.0f).glow(80, 120, 140);
                sphere.add(b);
                spawnedEntities.add(b.entity());
            }
            DisplayBuilder.playSound(pos, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            dropY -= 0.12f;
            if (dropY <= 1.5f) {
                dropY = 1.5f;
                impacted = true;
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SOUL, c, 30, 3, 1, 3, 0.05);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c, 20, 2, 0.5, 2, 0.03);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.4f);
            }

            // Move sphere
            Location pos = c.clone().add(0, dropY, 0);
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < sphere.size(); i++) {
                double yi = 1 - (2.0 * i / (sphere.size() - 1));
                double rAtY = Math.sqrt(1 - yi * yi);
                double theta = goldenAngle * i + ticksAlive * 0.01;
                sphere.get(i).entity().teleport(pos.clone().add(
                        Math.cos(theta) * rAtY * 2, yi * 2, Math.sin(theta) * rAtY * 2));
            }

            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL, pos, 3, 1.5, 1.5, 1.5, 0.02);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(pos, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulDrop(plugin); }
    }

    // ================================================================
    // 7. NIGHTMARE HAIL — 18 tiny blackstone blocks rain down fast
    //    and dense. Machine-gun style rapid bombardment.
    // ================================================================
    public static class NightmareHail extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hail = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> speeds = new ArrayList<>();
        private final List<Double> xOff = new ArrayList<>();
        private final List<Double> zOff = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public NightmareHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_hail", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 18; i++) {
                xOff.add(Math.random() * 12 - 6);
                zOff.add(Math.random() * 12 - 6);
                yPos.add(14.0f + (float)(Math.random() * 4));
                speeds.add(0.4f + (float)(Math.random() * 0.3));
                delays.add(i * 2);
                landed.add(false);
                hail.add(null);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            for (int i = 0; i < 18; i++) {
                if (ticksAlive < delays.get(i) || landed.get(i)) continue;
                if (hail.get(i) == null) {
                    Location loc = c.clone().add(xOff.get(i), yPos.get(i), zOff.get(i));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.4f, 0.4f, 0.4f).glow(50, 40, 60).interpolation(1, 0);
                    hail.set(i, h);
                    spawnedEntities.add(h.entity());
                }
                float y = yPos.get(i) - speeds.get(i);
                yPos.set(i, y);
                hail.get(i).entity().teleport(c.clone().add(xOff.get(i), Math.max(0, y), zOff.get(i)));
                hail.get(i).rotate(ticksAlive * 0.15f, 1, 0, 1);

                if (y <= 0) {
                    landed.set(i, true);
                    Location impLoc = c.clone().add(xOff.get(i), 0, zOff.get(i));
                    triggerImpactDamage(impLoc);
                    c.getWorld().spawnParticle(Particle.SMOKE, impLoc, 3, 0.2, 0.1, 0.2, 0.02);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareHail(plugin); }
    }

    // ================================================================
    // 8. COLLAPSING CEILING — 16 blocks form a flat ceiling that
    //    cracks and falls in sections. Sections fall at different times.
    // ================================================================
    public static class CollapsingCeiling extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tiles = new ArrayList<>();
        private final List<Float> tileY = new ArrayList<>();
        private final List<Boolean> falling = new ArrayList<>();
        private final List<Boolean> landed = new ArrayList<>();

        public CollapsingCeiling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsing_ceiling", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double x = (col - 1.5) * 2.5;
                    double z = (row - 1.5) * 2.5;
                    Material mat = ((row + col) % 2 == 0) ? Material.DEEPSLATE_BRICKS : Material.POLISHED_DEEPSLATE;
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(
                            center.clone().add(x, 10, z), mat);
                    tile.scale(2.5f, 0.5f, 2.5f).glow(70, 60, 80).interpolation(3, 0);
                    tiles.add(tile);
                    spawnedEntities.add(tile.entity());
                    tileY.add(10.0f);
                    falling.add(false);
                    landed.add(false);
                }
            }
            DisplayBuilder.playSound(center.clone().add(0, 10, 0), Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Start falling in random order
            if (ticksAlive > 30 && ticksAlive % 8 == 0) {
                for (int i = 0; i < tiles.size(); i++) {
                    if (!falling.get(i) && Math.random() < 0.3) {
                        falling.set(i, true);
                        // Crack sound
                        DisplayBuilder.playSound(tiles.get(i).entity().getLocation(),
                                Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
                        break;
                    }
                }
            }

            for (int i = 0; i < tiles.size(); i++) {
                if (landed.get(i)) continue;
                if (falling.get(i)) {
                    float y = tileY.get(i) - 0.4f;
                    tileY.set(i, y);
                    int row = i / 4, col = i % 4;
                    tiles.get(i).entity().teleport(c.clone().add((col - 1.5) * 2.5, Math.max(0, y), (row - 1.5) * 2.5));
                    // Wobble as it falls
                    tiles.get(i).rotate((float)(Math.random() * 0.1 - 0.05), 1, 0, 1);

                    if (y <= 0) {
                        landed.set(i, true);
                        Location impLoc = c.clone().add((col - 1.5) * 2.5, 0, (row - 1.5) * 2.5);
                        triggerImpactDamage(impLoc);
                        c.getWorld().spawnParticle(Particle.BLOCK, impLoc, 10, 0.5, 0.2, 0.5, 0.05,
                                Material.DEEPSLATE.createBlockData());
                        DisplayBuilder.playSound(impLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
                    }
                } else {
                    // Subtle shake before falling
                    if (ticksAlive > 20) {
                        float shake = (float)(Math.random() * 0.02 - 0.01);
                        tileY.set(i, 10.0f + shake);
                    }
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CollapsingCeiling(plugin); }
    }

    // ================================================================
    // 9. METEOR SWARM — 5 large sphere clusters (3 blocks each = 15)
    //    fall in sequence, tracking the player between each.
    // ================================================================
    public static class MeteorSwarm extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> meteors = new ArrayList<>();
        private final List<Float> meteorY = new ArrayList<>();
        private final List<Boolean> launched = new ArrayList<>();
        private final List<Boolean> impacted = new ArrayList<>();

        public MeteorSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteor_swarm", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(85.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int m = 0; m < 5; m++) {
                List<BlockDisplayHandle> cluster = new ArrayList<>();
                Location mPos = center.clone().add((m - 2) * 3, 18, 0);
                cluster.add(spawnP(mPos, 0, 0, 0, Material.MAGMA_BLOCK, 2.0f, 2.0f, 2.0f));
                cluster.add(spawnP(mPos, 1.0, 0.5, 0.5, Material.NETHERRACK, 1.2f, 1.2f, 1.2f));
                cluster.add(spawnP(mPos, -0.5, -0.5, 0.8, Material.BLACKSTONE, 1.0f, 1.0f, 1.0f));
                meteors.add(cluster);
                meteorY.add(18.0f);
                launched.add(false);
                impacted.add(false);
            }
            DisplayBuilder.playSound(center.clone().add(0, 18, 0), Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.5f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(255, 120, 30).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int m = 0; m < 5; m++) {
                int launchTick = 30 + m * 30;
                if (ticksAlive >= launchTick && !launched.get(m)) {
                    launched.set(m, true);
                    DisplayBuilder.playSound(c.clone().add(0, 18, 0), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 0.4f);
                }
                if (!launched.get(m) || impacted.get(m)) continue;

                float y = meteorY.get(m) - 0.6f;
                meteorY.set(m, y);

                Location mPos = c.clone().add((m - 2) * 3, y, 0);
                meteors.get(m).get(0).entity().teleport(mPos);
                meteors.get(m).get(1).entity().teleport(mPos.clone().add(1, 0.5, 0.5));
                meteors.get(m).get(2).entity().teleport(mPos.clone().add(-0.5, -0.5, 0.8));

                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.FLAME, mPos, 5, 0.5, 0.5, 0.5, 0.03);
                }

                if (y <= 1) {
                    impacted.set(m, true);
                    Location impLoc = c.clone().add((m - 2) * 3, 0, 0);
                    triggerImpactDamage(impLoc);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, impLoc, 2, 0.5, 0.5, 0.5, 0);
                    c.getWorld().spawnParticle(Particle.LAVA, impLoc, 10, 1, 0.5, 1, 0);
                    DisplayBuilder.playSound(impLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MeteorSwarm(plugin); }
    }

    // ================================================================
    // 10. FALLING CAGE — Cage structure (12 bars) drops from above
    //     and traps the player inside. Bars form around them.
    // ================================================================
    public static class FallingCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private float cageY = 14;
        private boolean landed = false;

        public FallingCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_cage", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, cageY, 0);

            // 8 vertical bars + 4 top bars
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                BlockDisplayHandle bar = displayBuilder.spawnBlock(
                        pos.clone().add(x, 0, z), Material.BLACKSTONE);
                bar.scale(0.4f, 4.0f, 0.4f).glow(50, 40, 60).interpolation(2, 0);
                bars.add(bar);
                spawnedEntities.add(bar.entity());
            }
            // Top cross bars
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                BlockDisplayHandle topBar = displayBuilder.spawnBlock(
                        pos.clone().add(0, 4, 0), Material.POLISHED_BLACKSTONE);
                topBar.scale(5.0f, 0.4f, 0.4f).glow(50, 40, 60).interpolation(2, 0);
                topBar.rotate((float) angle, 0, 1, 0);
                bars.add(topBar);
                spawnedEntities.add(topBar.entity());
            }

            DisplayBuilder.playSound(pos, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!landed) {
                cageY -= 0.25f;
                if (cageY <= 0) {
                    cageY = 0;
                    landed = true;
                    triggerImpactDamage(c);
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
                    c.getWorld().spawnParticle(Particle.SMOKE, c, 15, 2, 0.5, 2, 0.05);
                }
            }

            // Update bar positions
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                bars.get(i).entity().teleport(c.clone().add(
                        Math.cos(angle) * 2.5, cageY, Math.sin(angle) * 2.5));
            }
            for (int i = 0; i < 4; i++) {
                bars.get(8 + i).entity().teleport(c.clone().add(0, cageY + 4, 0));
            }

            // Warning shadow
            if (!landed && ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), 2.5, Particle.SMOKE, 10, null);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FallingCage(plugin); }
    }

    // ================================================================
    // 11. NIGHTMARE CHANDELIER — Ornate chandelier (14 blocks) drops.
    //     Ring of lights + center post + arms. Shatters on impact.
    // ================================================================
    public static class NightmareChandelier extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float chanY = 14;
        private boolean impacted = false;

        public NightmareChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_chandelier", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, chanY, 0);

            // Center post
            parts.add(spawnP(pos, 0, 0, 0, Material.CHAIN, 0.3f, 3.0f, 0.3f));
            parts.add(spawnP(pos, 0, 3, 0, Material.GOLD_BLOCK, 0.8f, 0.5f, 0.8f));

            // 6 arms radiating out
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                parts.add(spawnP(pos, x * 0.5, 1.5, z * 0.5, Material.GOLD_BLOCK, 0.2f, 0.2f, 2.0f));
                parts.add(spawnP(pos, x, 1.0, z, Material.SEA_LANTERN, 0.5f, 0.5f, 0.5f));
            }

            DisplayBuilder.playSound(pos, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 180, 100).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (impacted) return;

            if (ticksAlive > 40) chanY -= 0.5f;
            else chanY = 14 + (float) Math.sin(ticksAlive * 0.1) * 0.2f;

            if (chanY <= 1) {
                chanY = 1;
                impacted = true;
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 20, 1.5, 0.5, 1.5, 0.1,
                        Material.GOLD_BLOCK.createBlockData());
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 15, 1, 0.5, 1, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.3f);
            }

            if (ticksAlive > 30 && !impacted && ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE,
                        c.clone().add(0, chanY + 3, 0), 2, 0.2, 0.2, 0.2, 0.01);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareChandelier(plugin); }
    }

    // ================================================================
    // 12. BONE AVALANCHE — 14 bone blocks tumble down in a wave from
    //     one side, bouncing as they go. Diagonal fall pattern.
    // ================================================================
    public static class BoneAvalanche extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bones = new ArrayList<>();
        private final List<Float> xPos = new ArrayList<>();
        private final List<Float> yPos = new ArrayList<>();
        private final List<Float> xSpeed = new ArrayList<>();
        private final List<Float> ySpeed = new ArrayList<>();
        private final List<Integer> delays = new ArrayList<>();
        private final List<Boolean> active = new ArrayList<>();

        public BoneAvalanche(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_avalanche", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(45.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(55.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 14; i++) {
                xPos.add(-10.0f);
                yPos.add(12.0f + (float)(Math.random() * 4));
                xSpeed.add(0.3f + (float)(Math.random() * 0.15));
                ySpeed.add(0.0f);
                delays.add(i * 4);
                active.add(false);

                BlockDisplayHandle bone = displayBuilder.spawnBlock(
                        center.clone().add(-10, 12, (i - 7) * 0.8), Material.BONE_BLOCK);
                float s = 0.6f + (float)(Math.random() * 0.6);
                bone.scale(s, s, s).glow(200, 190, 170).interpolation(1, 0);
                bones.add(bone);
                spawnedEntities.add(bone.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < bones.size(); i++) {
                if (ticksAlive < delays.get(i)) continue;
                if (!active.get(i)) active.set(i, true);

                float x = xPos.get(i) + xSpeed.get(i);
                float vy = ySpeed.get(i) - 0.04f; // Gravity
                float y = yPos.get(i) + vy;

                // Bounce off ground
                if (y <= 0.5f) {
                    y = 0.5f;
                    vy = Math.abs(vy) * 0.5f;
                    Location impLoc = c.clone().add(x, 0, (i - 7) * 0.8);
                    triggerImpactDamage(impLoc);
                    DisplayBuilder.playSound(impLoc, Sound.ENTITY_SKELETON_STEP, 0.3f, 0.6f);
                }

                xPos.set(i, x);
                yPos.set(i, y);
                ySpeed.set(i, vy);

                bones.get(i).entity().teleport(c.clone().add(x, y, (i - 7) * 0.8));
                bones.get(i).rotate(ticksAlive * 0.1f + i, 1, 0.5f, 0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BoneAvalanche(plugin); }
    }

    // ================================================================
    // 13. PILLAR COLLAPSE — 4 tall pillars (3 each = 12) stand around
    //     player, then topple inward. Impact from falling pillars.
    // ================================================================
    public static class PillarCollapse extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();
        private final List<Float> tiltAngles = new ArrayList<>();
        private final List<Boolean> collapsing = new ArrayList<>();

        public PillarCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_collapse", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(80.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double dist = 5.0;
            double[][] positions = {{dist, 0}, {-dist, 0}, {0, dist}, {0, -dist}};

            for (double[] pos : positions) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(
                            center.clone().add(pos[0], y * 2.5, pos[1]), Material.DEEPSLATE_BRICKS);
                    seg.scale(1.5f, 2.5f, 1.5f).glow(70, 60, 80).interpolation(3, 0);
                    pillar.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                pillars.add(pillar);
                tiltAngles.add(0.0f);
                collapsing.add(false);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double dist = 5.0;
            double[][] positions = {{dist, 0}, {-dist, 0}, {0, dist}, {0, -dist}};
            float[][] tiltAxes = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

            for (int p = 0; p < 4; p++) {
                int collapseStart = 40 + p * 25;
                if (ticksAlive >= collapseStart && !collapsing.get(p)) {
                    collapsing.set(p, true);
                    DisplayBuilder.playSound(c.clone().add(positions[p][0], 0, positions[p][1]),
                            Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
                }

                if (collapsing.get(p)) {
                    float tilt = tiltAngles.get(p) + 0.02f;
                    if (tilt > (float)(Math.PI / 2)) tilt = (float)(Math.PI / 2);
                    tiltAngles.set(p, tilt);

                    if (tilt >= (float)(Math.PI / 2) - 0.05f && tilt < (float)(Math.PI / 2)) {
                        triggerImpactDamage(c);
                        c.getWorld().spawnParticle(Particle.BLOCK, c, 15, 1, 0.5, 1, 0.1,
                                Material.DEEPSLATE.createBlockData());
                        DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.6f);
                    }
                }

                // Update pillar segments
                for (int y = 0; y < 3; y++) {
                    float tilt = tiltAngles.get(p);
                    float segDist = (float)(dist - Math.sin(tilt) * y * 2.5);
                    float segY = (float)(Math.cos(tilt) * y * 2.5);
                    pillars.get(p).get(y).entity().teleport(c.clone().add(
                            positions[p][0] * segDist / dist, segY, positions[p][1] * segDist / dist));
                    pillars.get(p).get(y).rotate(tilt, tiltAxes[p][0], tiltAxes[p][1], tiltAxes[p][2]);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PillarCollapse(plugin); }
    }
}
