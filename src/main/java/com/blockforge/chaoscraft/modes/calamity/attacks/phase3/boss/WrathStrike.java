package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.boss;

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
 * Phase 3 Boss (Dweller) — TIER 3 "THE WRATH" Attacks #41-50
 * HP range: 60%-40%. Open hostility. Attacks chain into each other.
 * Corruption covers significant arena ground. Cluster punishment.
 * Damage range: 14-22 HP (7-11 hearts).
 * NO status effects — damage only.
 */
public final class WrathStrike {

    private WrathStrike() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InfernalStomp(plugin));
        registry.register(new ChainGrab(plugin));
        registry.register(new TwinLunge(plugin));
        registry.register(new CorruptionCrawl(plugin));
        registry.register(new ClusterPunish(plugin));
        registry.register(new MarkDetonation(plugin));
        registry.register(new VoidTendrils(plugin));
        registry.register(new HungerAscent(plugin));
        registry.register(new AshCloud(plugin));
        registry.register(new GroundBurn(plugin));
    }

    // ================================================================
    // 41. INFERNAL STOMP — Full-body slam, 6-block corruption ring
    // ================================================================
    public static class InfernalStomp extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean stomped = false;

        public InfernalStomp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_infernal_stomp", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller rises to full height, arms spread, all cracks blazing
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.BLACKSTONE);
            body.scale(1.6f, 3.5f, 1.6f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Rising on toes stretch upward
            DisplayBuilder.dustParticles(center.clone().add(0, 4, 0), 10, 0.8,
                255, 100, 0, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rise telegraph (0-30 ticks)
            if (ticksAlive < 30 && !stomped) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 4.5, 0), 6, 0.5,
                        255, 100, 0, 1.2f);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 4, 1.0);
                }
            }
            // STOMP (tick 30)
            else if (ticksAlive == 30 && !stomped) {
                stomped = true;
                // 6-block radius corruption ring
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 6, 0.1, Math.sin(a) * 6);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.8f, 0.12f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    ringHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }
                // Inner lethal zone
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2 / 6) * i;
                    Location innerLoc = center.clone().add(Math.cos(a) * 1.5, 0.15, Math.sin(a) * 1.5);
                    BlockDisplayHandle inner = displayBuilder.spawnBlock(innerLoc, Material.NETHERRACK);
                    inner.scale(1.0f, 0.2f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(inner.entity());
                }
                // Massive shockwave particles
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 40, 6.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 25, 4.0,
                    255, 100, 0, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                triggerImpactDamage(center);
            }
            // Dweller immediately walks forward 5 blocks (tick 35)
            else if (stomped && ticksAlive == 35) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, -5), 8, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalStomp(plugin); }
    }

    // ================================================================
    // 42. CHAIN GRAB — Grab nearest player and throw toward corruption
    // ================================================================
    public static class ChainGrab extends BossAttack {
        private boolean grabbed = false;
        private boolean thrown = false;

        public ChainGrab(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_chain_grab", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.5f);
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 10, 0.8);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Short telegraph (0-20 ticks — Tier 3 speed)
            if (ticksAlive < 20 && !grabbed) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.2, 0), 6, 0.6);
                }
            }
            // GRAB (tick 20)
            else if (ticksAlive == 20 && !grabbed) {
                grabbed = true;
                // Grab clamp blocks
                BlockDisplayHandle leftClamp = displayBuilder.spawnBlock(
                    center.clone().add(-0.7, 1.2, 0), Material.BLACKSTONE);
                leftClamp.scale(0.3f, 0.8f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(leftClamp.entity());
                BlockDisplayHandle rightClamp = displayBuilder.spawnBlock(
                    center.clone().add(0.7, 1.2, 0), Material.BLACKSTONE);
                rightClamp.scale(0.3f, 0.8f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(rightClamp.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
            }
            // Hold for 1 second (20-40 ticks)
            else if (grabbed && !thrown && ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.2, 0), 4, 0.3);
                }
            }
            // THROW toward corruption zone (tick 40)
            else if (ticksAlive == 40 && !thrown) {
                thrown = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
            }
            // Throw arc (40-55 ticks)
            else if (thrown && ticksAlive > 40 && ticksAlive <= 55) {
                float t = (ticksAlive - 40) / 15.0f;
                float throwZ = -t * 12;
                float throwY = (float) Math.sin(t * Math.PI) * 4;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 1 + throwY, throwZ), 5, 0.5);
                }
            }
            // Landing in corruption zone (tick 55)
            else if (thrown && ticksAlive == 55) {
                Location landLoc = center.clone().add(0, 0.1, -12);
                // Corruption zone eruption at landing
                BlockDisplayHandle zone = displayBuilder.spawnBlock(landLoc, Material.MAGMA_BLOCK);
                zone.scale(1.5f, 0.15f, 1.5f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(zone.entity());
                DisplayBuilder.crimsonDust(landLoc, 15, 1.5);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGrab(plugin); }
    }

    // ================================================================
    // 43. TWIN LUNGE — Real + shadow clone double lunge
    // ================================================================
    public static class TwinLunge extends BossAttack {
        private boolean lunged = false;

        public TwinLunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_twin_lunge", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Main Dweller crouching
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 0.5, 6), Material.BLACKSTONE);
            body.scale(1.3f, 1.5f, 1.3f).glow(255, 100, 0).interpolation(2, 0);
            spawnedEntities.add(body.entity());
            // Shadow clone on opposite side
            BlockDisplayHandle shadow = displayBuilder.spawnBlock(
                center.clone().add(0, 0.5, -6), Material.BLACKSTONE);
            shadow.scale(1.1f, 1.3f, 1.1f).glow(200, 0, 50).interpolation(2, 0);
            spawnedEntities.add(shadow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crouch telegraph + shadow echo appearing (0-30 ticks)
            if (ticksAlive < 30 && !lunged) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.8, 6), 4, 0.6);
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.8, -6), 4, 0.6);
                }
                // Shadow echo faint silhouette at tick 20
                if (ticksAlive == 20) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1, -6), 10, 0.8);
                }
            }
            // DUAL LUNGE (tick 30) — both converge on center target
            else if (ticksAlive == 30 && !lunged) {
                lunged = true;
                // Real lunge trail
                for (int i = 0; i < 5; i++) {
                    float z = 6 - i * 1.2f;
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.8, z), 5, 0.4);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, z), 3, 0.3,
                        255, 100, 0, 0.8f);
                }
                // Shadow lunge trail (opposite direction)
                for (int i = 0; i < 5; i++) {
                    float z = -6 + i * 1.2f;
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.8, z), 5, 0.4);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
            }
            // Shadow dissolves on contact (tick 44 — 0.7s after first)
            else if (lunged && ticksAlive == 44) {
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 20, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            }
            // Recovery window (tick 50)
            else if (lunged && ticksAlive == 50) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 6, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinLunge(plugin); }
    }

    // ================================================================
    // 44. CORRUPTION CRAWL — All-fours speed crawl, massive corruption path
    // ================================================================
    public static class CorruptionCrawl extends BossAttack {
        private final List<BlockDisplayHandle> crawlPath = new ArrayList<>();
        private boolean crawling = false;
        private int crawlTick = 0;

        public CorruptionCrawl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_crawl", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller dropping low
            BlockDisplayHandle crawlForm = displayBuilder.spawnBlock(
                center.clone().add(-10, 0.3, -8), Material.BLACKSTONE);
            crawlForm.scale(1.8f, 0.5f, 1.4f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(crawlForm.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crouch-to-crawl telegraph (0-30 ticks)
            if (ticksAlive < 30 && !crawling) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-10, 0.4, -8), 4, 0.6);
                }
            }
            // CRAWL begins (tick 30)
            else if (ticksAlive == 30 && !crawling) {
                crawling = true;
                crawlTick = 0;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.2f, 0.5f);
            }
            // Semicircular crawl with corruption trail (30-130 ticks = 5 seconds)
            else if (crawling && crawlTick < 100) {
                crawlTick++;
                float progress = crawlTick / 100.0f;
                // Semicircle path from -10,-8 around to 10,-8
                double arcAngle = progress * Math.PI;
                double pathX = Math.cos(arcAngle) * 10;
                double pathZ = -8 + Math.sin(arcAngle) * 8;

                // Corruption tiles along path
                if (crawlTick % 5 == 0) {
                    Location tileLoc = center.clone().add(pathX, 0.05, pathZ);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                    tile.scale(0.8f, 0.08f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    crawlPath.add(tile);
                    spawnedEntities.add(tile.entity());
                    DisplayBuilder.crimsonDust(tileLoc.clone().add(0, 0.2, 0), 3, 0.3);
                }
                if (crawlTick % 15 == 0) {
                    DisplayBuilder.playSound(center.clone().add(pathX, 0, pathZ),
                        Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionCrawl(plugin); }
    }

    // ================================================================
    // 45. CLUSTER PUNISH — Shockwave targeting clustered players
    // ================================================================
    public static class ClusterPunish extends BossAttack {
        private boolean punished = false;

        public ClusterPunish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_cluster_punish", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller snaps head toward cluster — eyes shift gold
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 3.0f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Gold eye shift
            DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 10, 0.3,
                255, 200, 0, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gold eye + arms overhead telegraph (0-30 ticks)
            if (ticksAlive < 30 && !punished) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 3.5, 0), 6, 0.4,
                        255, 200, 0, 1.0f);
                }
            }
            // SHOCKWAVE at cluster midpoint (tick 30)
            else if (ticksAlive == 30 && !punished) {
                punished = true;
                Location targetLoc = center.clone().add(0, 0, -6);
                // Concentric shockwave rings
                for (int r = 1; r <= 5; r++) {
                    for (int i = 0; i < r * 4; i++) {
                        double a = (Math.PI * 2 / (r * 4)) * i;
                        Location ringLoc = targetLoc.clone().add(
                            Math.cos(a) * r, 0.2, Math.sin(a) * r);
                        DisplayBuilder.dustParticles(ringLoc, 3, 0.3, 255, 200, 0, 1.0f);
                    }
                }
                // Central impact block
                BlockDisplayHandle impact = displayBuilder.spawnBlock(
                    targetLoc.clone().add(0, 0.2, 0), Material.MAGMA_BLOCK);
                impact.scale(2.0f, 0.3f, 2.0f).glow(255, 200, 0).interpolation(2, 0);
                spawnedEntities.add(impact.entity());
                DisplayBuilder.crimsonDust(targetLoc, 30, 5.0);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
                triggerImpactDamage(targetLoc);
            }
            // Corruption ring at edge of shockwave (tick 35)
            else if (punished && ticksAlive == 35) {
                Location targetLoc = center.clone().add(0, 0, -6);
                for (int i = 0; i < 10; i++) {
                    double a = (Math.PI * 2 / 10) * i;
                    Location zoneLoc = targetLoc.clone().add(Math.cos(a) * 5, 0.1, Math.sin(a) * 5);
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                    zone.scale(0.7f, 0.08f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClusterPunish(plugin); }
    }

    // ================================================================
    // 46. MARK DETONATION — Mark explodes outward from Marked player
    // ================================================================
    public static class MarkDetonation extends BossAttack {
        private boolean detonated = false;

        public MarkDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_detonation", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Marked player aura pulsing rapidly
            DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 15, 0.8,
                255, 0, 128, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rapid magenta pulse telegraph (0-30 ticks)
            if (ticksAlive < 30 && !detonated) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 8, 0.6,
                        255, 0, 128, 1.2f);
                }
                // Three flash pulses at 10, 18, 24
                if (ticksAlive == 10 || ticksAlive == 18 || ticksAlive == 24) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 1.0,
                        255, 0, 128, 1.5f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                }
            }
            // DETONATION (tick 30)
            else if (ticksAlive == 30 && !detonated) {
                detonated = true;
                // Magenta-orange explosion outward
                for (int r = 1; r <= 4; r++) {
                    for (int i = 0; i < r * 6; i++) {
                        double a = (Math.PI * 2 / (r * 6)) * i;
                        DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(a) * r, 1, Math.sin(a) * r),
                            4, 0.3, 255, 0, 128, 1.2f);
                    }
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 30, 4.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 20, 2.0,
                    255, 100, 0, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                triggerImpactDamage(center);
            }
            // Mark transfer ribbon to new target (tick 40)
            else if (detonated && ticksAlive == 40) {
                for (int i = 0; i < 8; i++) {
                    float t = i / 8.0f;
                    DisplayBuilder.dustParticles(
                        center.clone().add(Math.sin(t * Math.PI * 4) * 3, 1.5 + t, -t * 10),
                        4, 0.3, 255, 0, 128, 1.0f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkDetonation(plugin); }
    }

    // ================================================================
    // 47. VOID TENDRILS — Four tendrils targeting individual players
    // ================================================================
    public static class VoidTendrils extends BossAttack {
        private final List<List<BlockDisplayHandle>> tendrilPaths = new ArrayList<>();
        private boolean launched = false;
        private int travelTick = 0;

        public VoidTendrils(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_tendrils", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 4; i++) {
                tendrilPaths.add(new ArrayList<>());
            }
            // Tendrils writhing on Dweller's back
            for (int i = 0; i < 4; i++) {
                Location tendrilBase = center.clone().add(
                    (i - 1.5) * 0.5, 2 + i * 0.3, 0.3);
                BlockDisplayHandle base = displayBuilder.spawnBlock(tendrilBase, Material.SOUL_SOIL);
                base.scale(0.15f, 0.8f, 0.15f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(base.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendrils writhing upward telegraph (0-30 ticks)
            if (ticksAlive < 30 && !launched) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 4; i++) {
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add((i - 1.5) * 0.5, 2.5 + i * 0.3, 0.3),
                            3, 0.3);
                    }
                }
            }
            // LAUNCH (tick 30)
            else if (ticksAlive == 30 && !launched) {
                launched = true;
                travelTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.6f);
            }
            // Tendrils racing along ground to 4 different targets (30-70 ticks)
            else if (launched && travelTick < 40) {
                travelTick++;
                float progress = travelTick / 40.0f;
                // 4 tendrils in different directions
                double[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int t = 0; t < 4; t++) {
                    float dist = progress * 14;
                    double tx = directions[t][0] * dist;
                    double tz = directions[t][1] * dist;
                    if (travelTick % 4 == 0) {
                        Location segLoc = center.clone().add(tx, 0.12, tz);
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.SOUL_SOIL);
                        seg.scale(0.25f, 0.08f, 0.25f).glow(200, 0, 50).interpolation(1, 0);
                        tendrilPaths.get(t).add(seg);
                        spawnedEntities.add(seg.entity());
                        DisplayBuilder.darkPurpleDust(segLoc.clone().add(0, 0.15, 0), 3, 0.2);
                    }
                }
            }
            // Impact — root burst at endpoints (tick 70)
            else if (launched && travelTick == 40) {
                travelTick++;
                double[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int t = 0; t < 4; t++) {
                    Location endLoc = center.clone().add(
                        directions[t][0] * 14, 0.3, directions[t][1] * 14);
                    DisplayBuilder.crimsonDust(endLoc, 10, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendrils(plugin); }
    }

    // ================================================================
    // 48. HUNGER ASCENT — Full-speed wall-run, apex dive from height
    // ================================================================
    public static class HungerAscent extends BossAttack {
        private boolean ascending = false;
        private boolean diving = false;
        private int phaseTick = 0;

        public HungerAscent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_hunger_ascent", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sprint toward wall
            DisplayBuilder.crimsonDust(center.clone().add(-14, 1, 0), 8, 0.8);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sprint to wall (0-20 ticks)
            if (ticksAlive < 20 && !ascending) {
                if (ticksAlive % 5 == 0) {
                    float sprintX = -14 + (ticksAlive / 20.0f) * 28;
                    DisplayBuilder.crimsonDust(
                        center.clone().add(sprintX, 0.8, 0), 4, 0.5);
                }
            }
            // Wall-run ascent (tick 20)
            else if (ticksAlive == 20 && !ascending) {
                ascending = true;
                phaseTick = 0;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.2f, 0.5f);
            }
            // Climbing at full speed (20-45 ticks)
            else if (ascending && !diving && phaseTick < 25) {
                phaseTick++;
                float height = (phaseTick / 25.0f) * 15;
                if (phaseTick % 5 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(14, height, 0), 5, 0.5);
                    DisplayBuilder.playSound(center.clone().add(14, height, 0),
                        Sound.BLOCK_STONE_PLACE, 0.5f, 0.6f);
                }
            }
            // Apex — arms spread, silhouetted (tick 45)
            else if (ascending && phaseTick == 25 && !diving) {
                diving = true;
                phaseTick = 0;
                // Apex silhouette with arms spread
                BlockDisplayHandle apex = displayBuilder.spawnBlock(
                    center.clone().add(14, 15, 0), Material.BLACKSTONE);
                apex.scale(2.5f, 3.0f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(apex.entity());
                DisplayBuilder.crimsonDust(center.clone().add(14, 15, 0), 12, 1.5);
            }
            // Wide-arc dive (45-65 ticks)
            else if (diving && phaseTick < 20) {
                phaseTick++;
                float diveProgress = phaseTick / 20.0f;
                float diveX = 14 - diveProgress * 14;
                float diveY = 15 * (1 - diveProgress);
                if (phaseTick % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(diveX, diveY, 0), 6, 0.8);
                }
            }
            // LANDING IMPACT (tick 65)
            else if (diving && phaseTick == 20) {
                phaseTick++;
                Location landLoc = center.clone().add(0, 0.1, 0);
                // 5-block corruption ring
                for (int i = 0; i < 18; i++) {
                    double a = (Math.PI * 2 / 18) * i;
                    Location ringLoc = landLoc.clone().add(Math.cos(a) * 5, 0.1, Math.sin(a) * 5);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(landLoc, 35, 5.0);
                DisplayBuilder.dustParticles(landLoc, 20, 3.0, 255, 100, 0, 1.5f);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                triggerImpactDamage(landLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HungerAscent(plugin); }
    }

    // ================================================================
    // 49. ASH CLOUD — Massive obscuring cloud with hidden melee strikes
    // ================================================================
    public static class AshCloud extends BossAttack {
        private final List<BlockDisplayHandle> cloudHandles = new ArrayList<>();
        private boolean exhaled = false;
        private int cloudTick = 0;

        public AshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ash_cloud", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller pulling arms inward, chest glowing
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.0f, 2.8f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 10, 0.5,
                255, 100, 0, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Chest cavity building (0-30 ticks)
            if (ticksAlive < 30 && !exhaled) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 6, 0.4,
                        255, 100, 0, 1.0f + ticksAlive / 60.0f);
                }
            }
            // EXHALE — cloud erupts (tick 30)
            else if (ticksAlive == 30 && !exhaled) {
                exhaled = true;
                cloudTick = 0;
                // Dense cloud of ash/smoke blocks filling 10-block radius
                for (int i = 0; i < 30; i++) {
                    double rx = (Math.random() - 0.5) * 20;
                    double ry = Math.random() * 5;
                    double rz = (Math.random() - 0.5) * 20;
                    // Only within 10-block sphere
                    if (rx * rx + ry * ry + rz * rz > 100) continue;
                    Location cloudLoc = center.clone().add(rx, ry + 0.5, rz);
                    BlockDisplayHandle cloud = displayBuilder.spawnBlock(cloudLoc, Material.BLACK_CONCRETE);
                    cloud.scale(1.5f, 1.5f, 1.5f).glow(200, 0, 50).interpolation(3, 0);
                    cloudHandles.add(cloud);
                    spawnedEntities.add(cloud.entity());
                }
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 3, 0), 30, 8.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 20, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.3f);
            }
            // Three hidden strikes during cloud (at 1s, 2.5s, 4.5s)
            else if (exhaled && cloudTick < 100) {
                cloudTick++;
                // Strike 1 at tick 20 (1 second in)
                if (cloudTick == 20) {
                    DisplayBuilder.crimsonDust(center.clone().add(2, 1.5, -2), 10, 1.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                }
                // Strike 2 at tick 50 (2.5 seconds in)
                if (cloudTick == 50) {
                    DisplayBuilder.crimsonDust(center.clone().add(-3, 1.5, 1), 10, 1.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.7f);
                }
                // Strike 3 at tick 90 (4.5 seconds in)
                if (cloudTick == 90) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, 3), 10, 1.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
                }
                // Swirling ash particles within cloud
                if (cloudTick % 10 == 0) {
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(
                            (Math.random() - 0.5) * 10, 2,
                            (Math.random() - 0.5) * 10),
                        4, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AshCloud(plugin); }
    }

    // ================================================================
    // 50. GROUND BURN — Perimeter walk eliminating safe ground
    // ================================================================
    public static class GroundBurn extends BossAttack {
        private final List<BlockDisplayHandle> burnPath = new ArrayList<>();
        private boolean burning = false;
        private int burnTick = 0;

        public GroundBurn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ground_burn", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller turns to face safe zone
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(6, 1.2, 6), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center.clone().add(6, 0, 6),
                Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pause facing safe zone (0-30 ticks)
            if (ticksAlive < 30 && !burning) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(6, 1.5, 6), 4, 0.5);
                }
            }
            // Begin perimeter walk (tick 30)
            else if (ticksAlive == 30 && !burning) {
                burning = true;
                burnTick = 0;
            }
            // Walk rectangle perimeter dragging hand (30-170 ticks)
            else if (burning && burnTick < 140) {
                burnTick++;
                // Rectangle path: 8x8 block safe zone perimeter
                float progress = burnTick / 140.0f;
                double perimeterLength = 32.0; // 4 * 8 blocks
                double dist = progress * perimeterLength;
                double px, pz;
                if (dist < 8) {
                    px = 2 + dist;
                    pz = 2;
                } else if (dist < 16) {
                    px = 10;
                    pz = 2 + (dist - 8);
                } else if (dist < 24) {
                    px = 10 - (dist - 16);
                    pz = 10;
                } else {
                    px = 2;
                    pz = 10 - (dist - 24);
                }

                // Corruption tile at each step
                if (burnTick % 4 == 0) {
                    Location tileLoc = center.clone().add(px, 0.05, pz);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                    tile.scale(0.7f, 0.08f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    burnPath.add(tile);
                    spawnedEntities.add(tile.entity());
                    DisplayBuilder.crimsonDust(tileLoc.clone().add(0, 0.2, 0), 3, 0.3);
                    DisplayBuilder.dustParticles(tileLoc.clone().add(0, 0.3, 0), 2, 0.2,
                        255, 100, 0, 0.6f);
                }
                if (burnTick % 20 == 0) {
                    DisplayBuilder.playSound(center.clone().add(px, 0, pz),
                        Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
                }
            }
            // Teleport inside perimeter (tick 170)
            else if (burning && burnTick == 140) {
                burnTick++;
                Location insideLoc = center.clone().add(6, 1, 6);
                DisplayBuilder.darkPurpleDust(insideLoc.clone().add(0, 1.5, 0), 12, 1.0);
                DisplayBuilder.playSound(insideLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundBurn(plugin); }
    }
}
