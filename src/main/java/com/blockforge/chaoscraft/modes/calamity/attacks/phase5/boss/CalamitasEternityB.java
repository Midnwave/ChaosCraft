package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

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
 * Supreme Calamitas — Phase 4E Boss (Boss 5, Final Boss)
 * Phase 2: "The Weight of Eternity" — HP 74% to 50%
 * Attacks #61-70 — Lofted Tridents, Ground Phase, Brother Summon
 *
 * Ground phase mechanics force close-range. Brothers emerge at 60% HP.
 * NO status effects — damage only.
 */
public final class CalamitasEternityB {

    private CalamitasEternityB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GravityTridentArc(plugin));
        registry.register(new BrimstoneCoatedBurst(plugin));
        registry.register(new DescentStrike(plugin));
        registry.register(new SoulFireDrain(plugin));
        registry.register(new CorruptionPulse(plugin));
        registry.register(new TheReach(plugin));
        registry.register(new GroundTridentSlam(plugin));
        registry.register(new AscendingSlash(plugin));
        registry.register(new BrimstoneFootprintTrail(plugin));
        registry.register(new TheHandsDivide(plugin));
    }

    // ================================================================
    // 61. GRAVITY TRIDENT ARC — 4 lofted parabolic tridents
    // ================================================================
    public static class GravityTridentArc extends BossAttack {
        private final List<BlockDisplayHandle> arcHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public GravityTridentArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_gravity_trident_arc", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle t = displayBuilder.spawnBlock(
                    center.clone().add((i - 1.5) * 2, 25, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 1.0f).glow(200, 0, 200).interpolation(2, 0);
                arcHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 8, 3.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
            } else if (fired && ticksAlive - fireTick < 50) {
                float t = (ticksAlive - fireTick) / 50.0f;
                for (int i = 0; i < 4 && i < arcHandles.size(); i++) {
                    double x = (i - 1.5) * 2 + (i - 1.5) * t * 3;
                    double y = 25 + 10 * t - 35 * t * t; // parabolic
                    double z = t * 40;
                    arcHandles.get(i).entity().teleport(center.clone().add(x, Math.max(0.1, y), z));
                }
                if (ticksAlive % 6 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 15, 20 * t), 8, 3.0);
            } else if (fired && ticksAlive - fireTick == 50) {
                triggerImpactDamage(center.clone().add(0, 0.5, 40));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 40), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new GravityTridentArc(plugin); }
    }

    // ================================================================
    // 62. BRIMSTONE-COATED TRIDENT BURST — 5 fire-trail tridents
    // ================================================================
    public static class BrimstoneCoatedBurst extends BossAttack {
        private final List<BlockDisplayHandle> burstHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public BrimstoneCoatedBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_coated_burst", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(460);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(-40 + 20 * i);
                BlockDisplayHandle t = displayBuilder.spawnBlock(
                    center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2), Material.SOUL_SAND);
                t.scale(0.15f, 0.15f, 1.0f).glow(100, 180, 255).interpolation(2, 0);
                burstHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 2), 8, 2.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.9f);
            } else if (fired && ticksAlive - fireTick < 50) {
                float dist = (ticksAlive - fireTick) * 1.5f;
                for (int i = 0; i < 5 && i < burstHandles.size(); i++) {
                    double angle = Math.toRadians(-40 + 20 * i);
                    burstHandles.get(i).entity().teleport(
                        center.clone().add(Math.sin(angle) * (2 + dist), 20 - dist * 0.15, Math.cos(angle) * (2 + dist)));
                }
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 18, dist), 8, 2.0);
            } else if (fired && ticksAlive - fireTick == 50) {
                // Fire zones at impact
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians(-40 + 20 * i);
                    Location impLoc = center.clone().add(Math.sin(angle) * 77, 0.05, Math.cos(angle) * 77);
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(impLoc, Material.MAGMA_BLOCK);
                    zone.scale(5.0f, 0.08f, 5.0f).glow(255, 80, 0).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 70));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 70), 20, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new BrimstoneCoatedBurst(plugin); }
    }

    // ================================================================
    // 63. DESCENT STRIKE — Ground phase opener, shockwave on landing
    // ================================================================
    public static class DescentStrike extends BossAttack {
        private final List<BlockDisplayHandle> strikeHandles = new ArrayList<>();
        private boolean landed = false;

        public DescentStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_descent_strike", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle landing = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 0), Material.MAGMA_BLOCK);
            landing.scale(10.0f, 0.06f, 10.0f).glow(255, 60, 0).interpolation(3, 0);
            strikeHandles.add(landing);
            spawnedEntities.add(landing.entity());
            BlockDisplayHandle model = displayBuilder.spawnBlock(
                center.clone().add(0, 25, 0), Material.RED_STAINED_GLASS);
            model.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
            strikeHandles.add(model);
            spawnedEntities.add(model.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !landed) {
                float progress = ticksAlive / 20.0f;
                if (strikeHandles.size() > 1)
                    strikeHandles.get(1).entity().teleport(center.clone().add(0, 25 - progress * 24, 0));
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 25 - progress * 20, 0), 10, 2.0);
            } else if (ticksAlive == 20 && !landed) {
                landed = true;
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 8, 0.1, Math.sin(angle) * 8);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.ORANGE_STAINED_GLASS);
                    ring.scale(2.0f, 0.5f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center.clone().add(0, 1, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_STEP, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new DescentStrike(plugin); }
    }

    // ================================================================
    // 64. SOUL FIRE DRAIN — 7-block radius channeled AoE, 3 seconds
    // ================================================================
    public static class SoulFireDrain extends BossAttack {
        private final List<BlockDisplayHandle> drainHandles = new ArrayList<>();
        private boolean channeling = false;
        private int channelStart = 0;

        public SoulFireDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_soul_fire_drain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle hand = displayBuilder.spawnBlock(
                center.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
            hand.scale(1.0f, 0.3f, 1.0f).glow(255, 80, 0).interpolation(3, 0);
            drainHandles.add(hand);
            spawnedEntities.add(hand.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.4f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !channeling) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 10, 4.0);
            } else if (ticksAlive == 20 && !channeling) {
                channeling = true;
                channelStart = ticksAlive;
                for (int ring = 0; ring < 3; ring++) {
                    double radius = 2 + ring * 2.5;
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        Location ringPt = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(ringPt, Material.ORANGE_STAINED_GLASS);
                        seg.scale(1.5f, 0.5f, 1.5f).glow(255, 60, 0).interpolation(2, 0);
                        spawnedEntities.add(seg.entity());
                    }
                }
            } else if (channeling && ticksAlive - channelStart < 60) {
                if ((ticksAlive - channelStart) % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 15, 7.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.7f);
                }
            } else if (channeling && ticksAlive - channelStart == 60) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new SoulFireDrain(plugin); }
    }

    // ================================================================
    // 65. CORRUPTION PULSE — Instant 8-block radius ground stomp
    // ================================================================
    public static class CorruptionPulse extends BossAttack {
        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();
        private boolean stomped = false;

        public CorruptionPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_corruption_pulse", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location veinPt = center.clone().add(Math.cos(angle) * 4, 0.05, Math.sin(angle) * 4);
                BlockDisplayHandle vein = displayBuilder.spawnBlock(veinPt, Material.PURPLE_STAINED_GLASS);
                vein.scale(0.3f, 0.04f, 4.0f).glow(120, 0, 120).interpolation(3, 0);
                pulseHandles.add(vein);
                spawnedEntities.add(vein.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !stomped) {
                float expand = ticksAlive / 20.0f;
                for (int i = 0; i < 8 && i < pulseHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    pulseHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 4 * expand, 0.05, Math.sin(angle) * 4 * expand));
                }
            } else if (ticksAlive == 20 && !stomped) {
                stomped = true;
                for (int i = 0; i < 10; i++) {
                    double angle = (Math.PI * 2 / 10) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 8, 0.1, Math.sin(angle) * 8);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                    ring.scale(2.0f, 1.0f, 2.0f).glow(120, 0, 120).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new CorruptionPulse(plugin); }
    }

    // ================================================================
    // 66. THE REACH — Ground phase grab, 20 hearts on connection
    // ================================================================
    public static class TheReach extends BossAttack {
        private final List<BlockDisplayHandle> reachHandles = new ArrayList<>();
        private boolean lunged = false;

        public TheReach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_reach", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(150);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle arm = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 1), Material.RED_STAINED_GLASS);
            arm.scale(0.4f, 0.4f, 3.0f).glow(180, 0, 0).interpolation(2, 0);
            reachHandles.add(arm);
            spawnedEntities.add(arm.entity());
            BlockDisplayHandle target = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 5), Material.MAGMA_BLOCK);
            target.scale(2.0f, 0.06f, 2.0f).glow(220, 0, 0).interpolation(2, 0);
            reachHandles.add(target);
            spawnedEntities.add(target.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !lunged) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 2, 3), 8, 1.5);
            } else if (ticksAlive == 20 && !lunged) {
                lunged = true;
                if (!reachHandles.isEmpty()) {
                    reachHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-0.2f, 1.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.4f, 6.0f),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                triggerImpactDamage(center.clone().add(0, 2, 5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 5), 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.3f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TheReach(plugin); }
    }

    // ================================================================
    // 67. GROUND TRIDENT SLAM — Melee overhead slam + 6-block shockwave
    // ================================================================
    public static class GroundTridentSlam extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private boolean slammed = false;

        public GroundTridentSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ground_trident_slam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle trident = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.IRON_BLOCK);
            trident.scale(0.3f, 4.0f, 0.3f).glow(180, 0, 0).interpolation(2, 0);
            slamHandles.add(trident);
            spawnedEntities.add(trident.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !slammed) {
                float vibrate = (float) Math.sin(ticksAlive * 1.5) * 0.2f;
                if (!slamHandles.isEmpty())
                    slamHandles.get(0).entity().teleport(center.clone().add(vibrate, 5, vibrate));
            } else if (ticksAlive == 20 && !slammed) {
                slammed = true;
                if (!slamHandles.isEmpty())
                    slamHandles.get(0).entity().teleport(center.clone().add(0, 0.1, 0));
                for (int i = 0; i < 10; i++) {
                    double angle = (Math.PI * 2 / 10) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 6, 0.1, Math.sin(angle) * 6);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.ORANGE_STAINED_GLASS);
                    ring.scale(1.5f, 0.5f, 1.5f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 25, 6.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new GroundTridentSlam(plugin); }
    }

    // ================================================================
    // 68. ASCENDING SLASH — Ground phase exit, vertical cut column
    // ================================================================
    public static class AscendingSlash extends BossAttack {
        private final List<BlockDisplayHandle> slashHandles = new ArrayList<>();
        private boolean ascending = false;

        public AscendingSlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ascending_slash", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle model = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.RED_STAINED_GLASS);
            model.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
            slashHandles.add(model);
            spawnedEntities.add(model.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.4f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !ascending) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 10, 2.0);
            } else if (ticksAlive == 20 && !ascending) {
                ascending = true;
                // Vertical cut column
                for (int y = 0; y < 15; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                    col.scale(4.0f, 1.0f, 0.3f).glow(255, 80, 0).interpolation(1, 0);
                    spawnedEntities.add(col.entity());
                }
                triggerImpactDamage(center.clone().add(0, 5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 20, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            } else if (ascending && ticksAlive < 50) {
                float progress = (ticksAlive - 20) / 30.0f;
                if (!slashHandles.isEmpty())
                    slashHandles.get(0).entity().teleport(center.clone().add(0, 1 + progress * 14, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new AscendingSlash(plugin); }
    }

    // ================================================================
    // 69. BRIMSTONE FOOTPRINT TRAIL — Passive ground fire during ground phase
    // ================================================================
    public static class BrimstoneFootprintTrail extends BossAttack {
        private int lastTrailTick = 0;

        public BrimstoneFootprintTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_footprint_trail", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Every 10 ticks, place a footprint
            if (ticksAlive - lastTrailTick >= 10) {
                lastTrailTick = ticksAlive;
                double patrolAngle = ticksAlive * 0.02;
                double px = Math.sin(patrolAngle) * 6;
                double pz = Math.cos(patrolAngle) * 6;
                Location trailPt = center.clone().add(px, 0.05, pz);
                BlockDisplayHandle footprint = displayBuilder.spawnBlock(trailPt, Material.MAGMA_BLOCK);
                footprint.scale(3.0f, 0.04f, 3.0f).glow(255, 60, 0).interpolation(2, 0);
                spawnedEntities.add(footprint.entity());
                DisplayBuilder.crimsonDust(trailPt, 5, 1.5);
                DisplayBuilder.playSound(trailPt, Sound.BLOCK_SOUL_SAND_STEP, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new BrimstoneFootprintTrail(plugin); }
    }

    // ================================================================
    // 70. THE HANDS DIVIDE — Brother summon cinematic at 60% HP
    // ================================================================
    public static class TheHandsDivide extends BossAttack {
        private final List<BlockDisplayHandle> summonHandles = new ArrayList<>();
        private int beatPhase = 0;

        public TheHandsDivide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_hands_divide", AttackType.BOSS, 5), "calamitas");
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(160);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.3f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Beat 1 (0-10): She stops. Sky dims.
            if (ticksAlive < 10 && beatPhase == 0) {
                // Silence, stillness
            }
            // Beat 2 (10-30): Arms cross, crimson dust orbits
            else if (ticksAlive == 10 && beatPhase == 0) {
                beatPhase = 1;
                BlockDisplayHandle cross = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 0), Material.RED_STAINED_GLASS);
                cross.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
                summonHandles.add(cross);
                spawnedEntities.add(cross.entity());
            }
            else if (beatPhase == 1 && ticksAlive < 30) {
                if (ticksAlive % 4 == 0)
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 12, 2.0);
            }
            // Beat 3 (30-40): Two ground zones pulse
            else if (ticksAlive == 30 && beatPhase == 1) {
                beatPhase = 2;
                // Left: brimstone zone
                BlockDisplayHandle leftZone = displayBuilder.spawnBlock(
                    center.clone().add(-8, 0.05, 0), Material.MAGMA_BLOCK);
                leftZone.scale(6.0f, 0.1f, 6.0f).glow(255, 80, 0).interpolation(2, 0);
                summonHandles.add(leftZone);
                spawnedEntities.add(leftZone.entity());
                // Right: trident zone
                BlockDisplayHandle rightZone = displayBuilder.spawnBlock(
                    center.clone().add(8, 0.05, 0), Material.END_ROD);
                rightZone.scale(6.0f, 0.1f, 6.0f).glow(220, 220, 255).interpolation(2, 0);
                summonHandles.add(rightZone);
                spawnedEntities.add(rightZone.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            }
            // Beat 4 (40-60): Dimensional tears
            else if (ticksAlive == 40 && beatPhase == 2) {
                beatPhase = 3;
                // Left tear column
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle tear = displayBuilder.spawnBlock(
                        center.clone().add(-8, y, 0), Material.PURPLE_STAINED_GLASS);
                    tear.scale(3.0f, 1.0f, 0.3f).glow(128, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(tear.entity());
                }
                // Right tear column
                for (int y = 0; y < 8; y++) {
                    BlockDisplayHandle tear = displayBuilder.spawnBlock(
                        center.clone().add(8, y, 0), Material.PURPLE_STAINED_GLASS);
                    tear.scale(3.0f, 1.0f, 0.3f).glow(128, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(tear.entity());
                }
                for (int i = 0; i < 8; i++) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                }
            }
            // Beat 5 (60-80): Brothers emerge
            else if (ticksAlive == 60 && beatPhase == 3) {
                beatPhase = 4;
                // Left Hand — brimstone melee
                BlockDisplayHandle leftHand = displayBuilder.spawnBlock(
                    center.clone().add(-8, 1, 0), Material.SOUL_SAND);
                leftHand.scale(2.0f, 3.0f, 2.0f).glow(100, 180, 255).interpolation(2, 0);
                spawnedEntities.add(leftHand.entity());
                // Right Hand — trident ranged
                BlockDisplayHandle rightHand = displayBuilder.spawnBlock(
                    center.clone().add(8, 1, 0), Material.IRON_BLOCK);
                rightHand.scale(1.5f, 3.0f, 1.5f).glow(255, 0, 0).interpolation(2, 0);
                spawnedEntities.add(rightHand.entity());
                DisplayBuilder.crimsonDust(center.clone().add(-8, 2, 0), 20, 3.0);
                DisplayBuilder.crimsonDust(center.clone().add(8, 2, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.5f);
            }
            // Beat 6 (80-100): Tears seal, she resumes
            else if (ticksAlive == 80 && beatPhase == 4) {
                beatPhase = 5;
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 15, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TheHandsDivide(plugin); }
    }
}
