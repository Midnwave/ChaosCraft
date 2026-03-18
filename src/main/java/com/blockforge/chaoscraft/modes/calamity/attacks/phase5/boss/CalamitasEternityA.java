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
 * Attacks #51-60 — Trident Escalation
 *
 * Telegraph windows collapse to 1 second. Pressure doubles.
 * 12-fan spreads. Walls. Homing clusters. Spiral storms.
 * NO status effects — damage only.
 */
public final class CalamitasEternityA {

    private CalamitasEternityA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TwelveTridentFan(plugin));
        registry.register(new TridentWall(plugin));
        registry.register(new HomingTridentCluster(plugin));
        registry.register(new TridentSpiralStorm(plugin));
        registry.register(new TridentRainPhase2(plugin));
        registry.register(new ReturningTridentRing(plugin));
        registry.register(new TridentPiercerVolley(plugin));
        registry.register(new DetonatingTridentSpread(plugin));
        registry.register(new VelocityTrident(plugin));
        registry.register(new TridentFormationLace(plugin));
    }

    // ================================================================
    // 51. TWELVE-TRIDENT FAN — 180-degree arc of 12 tridents
    // ================================================================
    public static class TwelveTridentFan extends BossAttack {
        private final List<BlockDisplayHandle> fanHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TwelveTridentFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_twelve_trident_fan", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(-90 + (180.0 / 11) * i);
                Location pt = center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2);
                BlockDisplayHandle ind = displayBuilder.spawnBlock(pt, Material.IRON_BLOCK);
                ind.scale(0.15f, 0.15f, 0.15f).glow(180, 0, 0).interpolation(2, 0);
                fanHandles.add(ind);
                spawnedEntities.add(ind.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 2), 10, 3.0);
                }
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(-90 + (180.0 / 11) * i);
                    Location spawnLoc = center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    float velVariation = 0.95f + (float)(Math.random() * 0.1);
                    t.scale(0.1f, 0.1f, 0.9f).glow(220, 220, 255).interpolation(1, 0);
                    fanHandles.add(t);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.9f);
            } else if (fired && ticksAlive - fireTick < 50) {
                float dist = (ticksAlive - fireTick) * 1.8f;
                for (int i = 0; i < 12; i++) {
                    int idx = 12 + i;
                    if (idx < fanHandles.size()) {
                        double angle = Math.toRadians(-90 + (180.0 / 11) * i);
                        fanHandles.get(idx).entity().teleport(
                            center.clone().add(Math.sin(angle) * (2 + dist), 20, Math.cos(angle) * (2 + dist)));
                    }
                }
            } else if (fired && ticksAlive - fireTick == 50) {
                triggerImpactDamage(center.clone().add(0, 0.5, 80));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 80), 20, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TwelveTridentFan(plugin); }
    }

    // ================================================================
    // 52. TRIDENT WALL — 6 parallel tridents forming a horizontal barrier
    // ================================================================
    public static class TridentWall extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TridentWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_wall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 6; i++) {
                double x = -5 + i * 2;
                BlockDisplayHandle ghost = displayBuilder.spawnBlock(
                    center.clone().add(x, 20, 8), Material.END_ROD);
                ghost.scale(0.1f, 0.1f, 0.6f).glow(255, 255, 230).interpolation(2, 0);
                wallHandles.add(ghost);
                spawnedEntities.add(ghost.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.85f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 8), 12, 5.0);
                }
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 6; i++) {
                    double x = -5 + i * 2;
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(x, 20, 8), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    wallHandles.add(t);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.85f);
            } else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 2.5f;
                for (int i = 0; i < 6; i++) {
                    int idx = 6 + i;
                    if (idx < wallHandles.size()) {
                        double x = -5 + i * 2;
                        wallHandles.get(idx).entity().teleport(
                            center.clone().add(x, 20, 8 + dist));
                    }
                }
            } else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 100));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 100), 15, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.7f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TridentWall(plugin); }
    }

    // ================================================================
    // 53. HOMING TRIDENT CLUSTER — 5 homing tridents with turning radius
    // ================================================================
    public static class HomingTridentCluster extends BossAttack {
        private final List<BlockDisplayHandle> homeHandles = new ArrayList<>();
        private boolean released = false;
        private int releaseTick = 0;

        public HomingTridentCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_homing_trident_cluster", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 / 5) * i;
                Location pt = center.clone().add(Math.cos(angle) * 3, 20, Math.sin(angle) * 3);
                BlockDisplayHandle t = displayBuilder.spawnBlock(pt, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 1.0f).glow(200, 0, 200).interpolation(2, 0);
                homeHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !released) {
                double rot = ticksAlive * 0.2;
                for (int i = 0; i < 5 && i < homeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 5) * i + rot;
                    homeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 20, Math.sin(angle) * 3));
                }
            } else if (ticksAlive == 20 && !released) {
                released = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.1f);
            } else if (released && ticksAlive - releaseTick < 160) {
                int hTick = ticksAlive - releaseTick;
                float speed = 1.2f + (hTick > 80 ? 0.24f : 0);
                for (int i = 0; i < 5 && i < homeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 5) * i + hTick * 0.08;
                    float dist = hTick * speed * 0.15f;
                    homeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (3 + dist), 20 - dist * 0.08,
                            Math.sin(angle) * (3 + dist)));
                }
                if (hTick % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 18, 0), 6, 5.0);
                }
            } else if (released && ticksAlive - releaseTick == 160) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 15, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new HomingTridentCluster(plugin); }
    }

    // ================================================================
    // 54. TRIDENT SPIRAL STORM — 32 tridents in clockwise expanding spiral
    // ================================================================
    public static class TridentSpiralStorm extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean spinning = false;
        private int spinStartTick = 0;

        public TridentSpiralStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_spiral_storm", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.RED_STAINED_GLASS);
            core.scale(1.5f, 1.5f, 1.5f).glow(180, 0, 0).interpolation(2, 0);
            spiralHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !spinning) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, 2.0);
                }
            } else if (ticksAlive == 20 && !spinning) {
                spinning = true;
                spinStartTick = ticksAlive;
            } else if (spinning && ticksAlive - spinStartTick < 80) {
                int sTick = ticksAlive - spinStartTick;
                // Fire 4 tridents every 10 ticks = 32 total over 80 ticks
                if (sTick % 10 == 0) {
                    int volley = sTick / 10;
                    double baseAngle = Math.toRadians(45 * volley);
                    for (int spoke = 0; spoke < 4; spoke++) {
                        double angle = baseAngle + Math.toRadians(90 * spoke);
                        Location spawnLoc = center.clone().add(Math.cos(angle) * 2, 20, Math.sin(angle) * 2);
                        BlockDisplayHandle t = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                        t.scale(0.1f, 0.1f, 0.8f).glow(220, 220, 255).interpolation(1, 0);
                        spiralHandles.add(t);
                        spawnedEntities.add(t.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.0f);
                }
                // Move all fired tridents outward
                for (int i = 1; i < spiralHandles.size(); i++) {
                    int tIdx = (i - 1);
                    int volley = tIdx / 4;
                    int spoke = tIdx % 4;
                    int tFireTick = volley * 10;
                    if (sTick > tFireTick) {
                        float dist = (sTick - tFireTick) * 1.2f;
                        double angle = Math.toRadians(45 * volley + 90 * spoke);
                        spiralHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * (2 + dist), 20, Math.sin(angle) * (2 + dist)));
                    }
                }
            } else if (spinning && ticksAlive - spinStartTick == 80) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TridentSpiralStorm(plugin); }
    }

    // ================================================================
    // 55. TRIDENT RAIN PHASE 2 — 15 concentrated drops on player position
    // ================================================================
    public static class TridentRainPhase2 extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private boolean raining = false;
        private int rainStartTick = 0;

        public TridentRainPhase2(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_rain_p2", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 15 shadow points at Y+35
            for (int i = 0; i < 15; i++) {
                double offX = (Math.random() - 0.5) * 12;
                double offZ = (Math.random() - 0.5) * 12;
                Location pt = center.clone().add(offX, 35, offZ);
                BlockDisplayHandle shadow = displayBuilder.spawnBlock(pt, Material.BLACK_STAINED_GLASS);
                shadow.scale(0.3f, 0.3f, 0.3f).glow(60, 0, 0).interpolation(2, 0);
                rainHandles.add(shadow);
                spawnedEntities.add(shadow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !raining) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 12, 6.0);
                }
            } else if (ticksAlive == 20 && !raining) {
                raining = true;
                rainStartTick = ticksAlive;
            } else if (raining && ticksAlive - rainStartTick < 60) {
                int rTick = ticksAlive - rainStartTick;
                // Stagger drops 0.15s apart = 3 ticks
                for (int i = 0; i < 15 && i < rainHandles.size(); i++) {
                    int delay = i * 3;
                    if (rTick > delay) {
                        float drop = (rTick - delay) * 1.0f;
                        double y = 35 - drop;
                        if (y < 0.1) y = 0.1;
                        Location curLoc = rainHandles.get(i).entity().getLocation();
                        rainHandles.get(i).entity().teleport(
                            new Location(curLoc.getWorld(), curLoc.getX(), y, curLoc.getZ()));
                    }
                }
                if (rTick % 3 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f + (float)(Math.random() * 0.4));
                }
            } else if (raining && ticksAlive - rainStartTick == 60) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 25, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TridentRainPhase2(plugin); }
    }

    // ================================================================
    // 56. RETURNING TRIDENT RING — 8 outward then reverse inward
    // ================================================================
    public static class ReturningTridentRing extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean fired = false;
        private boolean returning = false;
        private int fireTick = 0;

        public ReturningTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_returning_trident_ring", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location pt = center.clone().add(Math.cos(angle) * 2, 20, Math.sin(angle) * 2);
                BlockDisplayHandle t = displayBuilder.spawnBlock(pt, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 1.0f).glow(180, 0, 0).interpolation(2, 0);
                ringHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.95f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 8, 2.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.95f);
            } else if (fired && !returning && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 1.5f;
                for (int i = 0; i < 8 && i < ringHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    ringHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 20, Math.sin(angle) * (2 + dist)));
                }
            } else if (fired && !returning && ticksAlive - fireTick == 40) {
                returning = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.3f, 0.7f);
            } else if (returning && ticksAlive - fireTick < 80) {
                float returnProgress = (ticksAlive - fireTick - 40) / 40.0f;
                float dist = 62 * (1 - returnProgress);
                for (int i = 0; i < 8 && i < ringHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + returnProgress * 0.3;
                    ringHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 20, Math.sin(angle) * (2 + dist)));
                }
            } else if (returning && ticksAlive - fireTick == 80) {
                triggerImpactDamage(center.clone().add(0, 20, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 15, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new ReturningTridentRing(plugin); }
    }

    // ================================================================
    // 57. TRIDENT PIERCER VOLLEY — 3 piercing tridents, pass through terrain
    // ================================================================
    public static class TridentPiercerVolley extends BossAttack {
        private final List<BlockDisplayHandle> piercerHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TridentPiercerVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_piercer_volley", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle t = displayBuilder.spawnBlock(
                    center.clone().add(i * 0.5, 20, 2), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 1.2f).glow(255, 255, 200).interpolation(2, 0);
                piercerHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 20, 2), 6, 0.5, 255, 255, 200, 1.0f);
                }
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.1f, 1.2f);
            } else if (fired && ticksAlive - fireTick < 30) {
                float dist = (ticksAlive - fireTick) * 3.5f;
                for (int i = -1; i <= 1; i++) {
                    int idx = i + 1;
                    if (idx < piercerHandles.size()) {
                        piercerHandles.get(idx).entity().teleport(
                            center.clone().add(i * 0.5, 20, 2 + dist));
                    }
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 20, 2 + dist), 6, 0.5, 255, 255, 230, 1.0f);
                }
            } else if (fired && ticksAlive - fireTick == 30) {
                triggerImpactDamage(center.clone().add(0, 20, 100));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TridentPiercerVolley(plugin); }
    }

    // ================================================================
    // 58. DETONATING TRIDENT SPREAD — 6 tridents that explode on impact
    // ================================================================
    public static class DetonatingTridentSpread extends BossAttack {
        private final List<BlockDisplayHandle> detHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public DetonatingTridentSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_detonating_trident_spread", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(420);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(-60 + (120.0 / 5) * i);
                BlockDisplayHandle t = displayBuilder.spawnBlock(
                    center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2),
                    Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 0.9f).glow(255, 100, 0).interpolation(2, 0);
                detHandles.add(t);
                spawnedEntities.add(t.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 2), 8, 3.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.9f);
            } else if (fired && ticksAlive - fireTick < 50) {
                float dist = (ticksAlive - fireTick) * 1.5f;
                for (int i = 0; i < 6 && i < detHandles.size(); i++) {
                    double angle = Math.toRadians(-60 + (120.0 / 5) * i);
                    detHandles.get(i).entity().teleport(
                        center.clone().add(Math.sin(angle) * (2 + dist), 20 - dist * 0.15, Math.cos(angle) * (2 + dist)));
                }
            } else if (fired && ticksAlive - fireTick == 50) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(-60 + (120.0 / 5) * i);
                    Location impLoc = center.clone().add(Math.sin(angle) * 77, 0.1, Math.cos(angle) * 77);
                    BlockDisplayHandle blast = displayBuilder.spawnBlock(impLoc, Material.ORANGE_STAINED_GLASS);
                    blast.scale(3.0f, 3.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(blast.entity());
                    DisplayBuilder.crimsonDust(impLoc, 10, 3.0);
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 70));
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.1f);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new DetonatingTridentSpread(plugin); }
    }

    // ================================================================
    // 59. VELOCITY TRIDENT — Single ultra-fast trident, 0.5s telegraph
    // ================================================================
    public static class VelocityTrident extends BossAttack {
        private final List<BlockDisplayHandle> velHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public VelocityTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_velocity_trident", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle glow = displayBuilder.spawnBlock(
                center.clone().add(1, 20, 0), Material.GLOWSTONE);
            glow.scale(0.3f, 0.3f, 0.3f).glow(255, 255, 200).interpolation(1, 0);
            velHandles.add(glow);
            spawnedEntities.add(glow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ultra-short telegraph: 10 ticks = 0.5 seconds
            if (ticksAlive < 10 && !fired) {
                DisplayBuilder.dustParticles(center.clone().add(1, 20, 0), 8, 0.3, 255, 255, 200, 1.5f);
            } else if (ticksAlive == 10 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                BlockDisplayHandle t = displayBuilder.spawnBlock(
                    center.clone().add(1, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                t.scale(0.12f, 0.12f, 1.5f).glow(255, 255, 255).interpolation(0, 0);
                velHandles.add(t);
                spawnedEntities.add(t.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.8f);
            } else if (fired && ticksAlive - fireTick < 10) {
                float dist = (ticksAlive - fireTick) * 10.0f;
                if (velHandles.size() > 1) {
                    velHandles.get(1).entity().teleport(center.clone().add(1, 20, dist));
                }
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(1, 20, dist), 4, 0.3, 255, 255, 230, 1.0f);
                }
            } else if (fired && ticksAlive - fireTick == 10) {
                triggerImpactDamage(center.clone().add(1, 0.5, 100));
                DisplayBuilder.crimsonDust(center.clone().add(1, 0.5, 100), 8, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new VelocityTrident(plugin); }
    }

    // ================================================================
    // 60. TRIDENT FORMATION LACE — 2 rows of 5, head+waist height
    // ================================================================
    public static class TridentFormationLace extends BossAttack {
        private final List<BlockDisplayHandle> laceHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TridentFormationLace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_formation_lace", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 2 rows of 5 indicators
            for (int row = 0; row < 2; row++) {
                double y = (row == 0) ? 21.5 : 18.5;
                for (int col = 0; col < 5; col++) {
                    double x = -4 + col * 2;
                    BlockDisplayHandle ind = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 5), Material.IRON_BLOCK);
                    ind.scale(0.1f, 0.1f, 0.6f).glow(180, 0, 0).interpolation(2, 0);
                    laceHandles.add(ind);
                    spawnedEntities.add(ind.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.85f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 5), 10, 4.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int row = 0; row < 2; row++) {
                    double y = (row == 0) ? 21.5 : 18.5;
                    for (int col = 0; col < 5; col++) {
                        double x = -4 + col * 2;
                        double spread = Math.toRadians(5 * (col - 2));
                        BlockDisplayHandle t = displayBuilder.spawnBlock(
                            center.clone().add(x, y, 5), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                        t.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                        laceHandles.add(t);
                        spawnedEntities.add(t.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.85f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.95f);
            } else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 2.0f;
                int idx = 10;
                for (int row = 0; row < 2; row++) {
                    double y = (row == 0) ? 21.5 : 18.5;
                    for (int col = 0; col < 5; col++) {
                        if (idx < laceHandles.size()) {
                            double x = -4 + col * 2;
                            double spread = Math.toRadians(5 * (col - 2));
                            laceHandles.get(idx).entity().teleport(
                                center.clone().add(x + Math.sin(spread) * dist, y, 5 + Math.cos(spread) * dist));
                        }
                        idx++;
                    }
                }
            } else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 80));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 80), 15, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }
        @Override
        public AbstractAttack newInstance() { return new TridentFormationLace(plugin); }
    }
}
