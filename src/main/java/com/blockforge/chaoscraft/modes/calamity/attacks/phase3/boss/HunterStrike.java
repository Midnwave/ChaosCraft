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
 * Phase 3 Boss (Dweller) — TIER 2 "THE HUNTER" Attacks #21-30
 * HP range: 80%-60%. The Dweller hunts actively. Shorter telegraphs,
 * wall ambush attacks, mark exploitation, vanish feints.
 * Damage range: 12-18 HP (6-9 hearts).
 * NO status effects — damage only.
 */
public final class HunterStrike {

    private HunterStrike() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WallAmbush(plugin));
        registry.register(new MarkTransferStrike(plugin));
        registry.register(new BrimstoneBarrage(plugin));
        registry.register(new VanishFeint(plugin));
        registry.register(new CeilingHang(plugin));
        registry.register(new Screech(plugin));
        registry.register(new ShadowGrab(plugin));
        registry.register(new CorruptionSurge(plugin));
        registry.register(new HuntRun(plugin));
        registry.register(new EyeDrain(plugin));
    }

    // ================================================================
    // 21. WALL AMBUSH — Drop from wall onto nearest player
    // ================================================================
    public static class WallAmbush extends BossAttack {
        private final List<BlockDisplayHandle> dustHandles = new ArrayList<>();
        private boolean dropped = false;

        public WallAmbush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_wall_ambush", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller climbing up wall — barely visible high on wall surface
            BlockDisplayHandle wallForm = displayBuilder.spawnBlock(
                center.clone().add(14, 12, 0), Material.BLACKSTONE);
            wallForm.scale(1.2f, 2.0f, 0.4f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(wallForm.entity());
            // Scraping sound from wall
            DisplayBuilder.playSound(center.clone().add(14, 10, 0),
                Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall scraping audio cue (0-30 ticks)
            if (ticksAlive < 30 && !dropped) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center.clone().add(14, 12, 0),
                        Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f);
                    // Falling obsidian dust from wall
                    for (int i = 0; i < 3; i++) {
                        Location dustLoc = center.clone().add(
                            13.5 + Math.random() * 1.0, 12 - Math.random() * 3, Math.random() - 0.5);
                        BlockDisplayHandle dust = displayBuilder.spawnBlock(dustLoc, Material.OBSIDIAN);
                        dust.scale(0.15f, 0.15f, 0.15f).glow(200, 0, 50).interpolation(2, 0);
                        dustHandles.add(dust);
                        spawnedEntities.add(dust.entity());
                    }
                }
            }
            // DROP (tick 30)
            else if (ticksAlive == 30 && !dropped) {
                dropped = true;
                Location landLoc = center.clone().add(10, 0.1, 0);
                // Streak of descent
                for (int y = 0; y < 6; y++) {
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(12, 2 + y * 2, 0), 4, 0.5);
                }
                // Landing impact — corruption ring
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2 / 12) * i;
                    Location ringLoc = landLoc.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(landLoc, 25, 3.0);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.4f);
                triggerImpactDamage(landLoc);
            }
            // Follow-up swipe (tick 40)
            else if (dropped && ticksAlive == 40) {
                Location swipeLoc = center.clone().add(10, 1.5, 0);
                for (int i = -3; i <= 3; i++) {
                    double a = Math.toRadians(i * 25);
                    DisplayBuilder.crimsonDust(
                        swipeLoc.clone().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5), 4, 0.4);
                }
                DisplayBuilder.playSound(swipeLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WallAmbush(plugin); }
    }

    // ================================================================
    // 22. MARK TRANSFER STRIKE — Heavy blow transfers the Mark
    // ================================================================
    public static class MarkTransferStrike extends BossAttack {
        private boolean struck = false;

        public MarkTransferStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_transfer_strike", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller staring at target — eyes shifting to violet
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 3.0f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 10, 0.3,
                128, 0, 200, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye color shift telegraph (0-40 ticks)
            if (ticksAlive < 40 && !struck) {
                if (ticksAlive % 8 == 0) {
                    float shift = ticksAlive / 40.0f;
                    int r = (int) (0 + shift * 128);
                    int b = (int) (150 + shift * 50);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 6, 0.3,
                        r, 0, b, 1.0f);
                }
            }
            // Double-handed blow (tick 40)
            else if (ticksAlive == 40 && !struck) {
                struck = true;
                // Impact at close range
                DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, -2), 20, 1.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }
            // Mark transfer ribbon — spectral magenta ribbon flying to new target (40-60)
            else if (struck && ticksAlive > 40 && ticksAlive <= 60) {
                float ribbon = (ticksAlive - 40) / 20.0f;
                Location ribbonLoc = center.clone().add(
                    Math.sin(ribbon * Math.PI * 4) * 3,
                    1.5 + ribbon * 2,
                    -2 - ribbon * 8);
                DisplayBuilder.dustParticles(ribbonLoc, 6, 0.4, 255, 0, 128, 1.2f);
            }
            // New mark ignition burst (tick 60)
            else if (ticksAlive == 60) {
                Location newMarkLoc = center.clone().add(0, 1.5, -10);
                DisplayBuilder.dustParticles(newMarkLoc, 20, 1.5, 255, 0, 128, 1.5f);
                DisplayBuilder.crimsonDust(newMarkLoc, 15, 1.0);
                DisplayBuilder.playSound(newMarkLoc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkTransferStrike(plugin); }
    }

    // ================================================================
    // 23. BRIMSTONE BARRAGE — 8-10 projectile rain in 120-degree arc
    // ================================================================
    public static class BrimstoneBarrage extends BossAttack {
        private final List<BlockDisplayHandle> projectiles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;
        private final int projectileCount = 9;

        public BrimstoneBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_barrage", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head tilt back — throat building
            BlockDisplayHandle throat = displayBuilder.spawnBlock(
                center.clone().add(0, 2.8, 0), Material.MAGMA_BLOCK);
            throat.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(throat.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Throat buildup (0-40 ticks)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.8, 0), 6, 0.4,
                        255, 100, 0, 1.0f);
                }
            }
            // Fire volley (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = 0;
                // Spawn all projectiles arcing upward
                for (int i = 0; i < projectileCount; i++) {
                    Location spawnLoc = center.clone().add(0, 3 + i * 0.3, 0);
                    BlockDisplayHandle proj = displayBuilder.spawnBlock(spawnLoc, Material.MAGMA_BLOCK);
                    proj.scale(0.4f, 0.4f, 0.4f).glow(255, 100, 0).interpolation(1, 0);
                    projectiles.add(proj);
                    spawnedEntities.add(proj.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.8f);
            }
            // Projectiles arc and scatter (40-100 ticks)
            else if (fired && fireTick < 60) {
                fireTick++;
                float progress = fireTick / 60.0f;
                for (int i = 0; i < projectiles.size(); i++) {
                    // 120-degree spread arc
                    double spreadAngle = Math.toRadians(-60 + (120.0 / (projectileCount - 1)) * i);
                    float dist = progress * 14;
                    double px = Math.sin(spreadAngle) * dist;
                    double pz = -Math.cos(spreadAngle) * dist;
                    // Parabolic arc — up then down
                    double py = Math.sin(progress * Math.PI) * 8;
                    if (progress > 0.7) py = Math.max(0.1, py);
                    projectiles.get(i).entity().teleport(
                        center.clone().add(px, 3 + py, pz));
                    // Flame trail
                    if (fireTick % 6 == 0) {
                        DisplayBuilder.dustParticles(
                            center.clone().add(px, 3 + py, pz), 3, 0.3, 255, 100, 0, 0.8f);
                    }
                }
            }
            // Impacts on ground (tick 100)
            else if (fired && fireTick == 60) {
                fireTick++;
                for (int i = 0; i < projectileCount; i++) {
                    double spreadAngle = Math.toRadians(-60 + (120.0 / (projectileCount - 1)) * i);
                    Location impactLoc = center.clone().add(
                        Math.sin(spreadAngle) * 14, 0.1, -Math.cos(spreadAngle) * 14);
                    // Corruption zone at each impact
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(impactLoc, Material.MAGMA_BLOCK);
                    zone.scale(0.6f, 0.1f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                    DisplayBuilder.crimsonDust(impactLoc, 8, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                triggerImpactDamage(center.clone().add(0, 0.1, -10));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBarrage(plugin); }
    }

    // ================================================================
    // 24. VANISH FEINT — Fake vanish that punishes panic movement
    // ================================================================
    public static class VanishFeint extends BossAttack {
        private boolean feintDone = false;

        public VanishFeint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_vanish_feint", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller form present
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Triple flicker (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !feintDone) {
                // Three rapid pulses at ticks 0, 6, 12
                if (ticksAlive == 0 || ticksAlive == 6 || ticksAlive == 12) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 15, 1.5);
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
                }
            }
            // Snap back to full visibility (tick 20)
            else if (ticksAlive == 20 && !feintDone) {
                feintDone = true;
                // Satisfaction gleam in eyes
                DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 8, 0.2,
                    0, 150, 255, 1.0f);
            }
            // Immediate charge at most-moved player (tick 25)
            else if (feintDone && ticksAlive == 25) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.0f);
            }
            // Charge travel (25-40 ticks)
            else if (feintDone && ticksAlive > 25 && ticksAlive <= 40) {
                float progress = (ticksAlive - 25) / 15.0f;
                float chargeZ = -progress * 14;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 1, chargeZ), 6, 0.8);
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 0.5, chargeZ + 1), 4, 0.4,
                        255, 100, 0, 1.0f);
                }
            }
            // Impact
            else if (ticksAlive == 40) {
                Location impactLoc = center.clone().add(0, 0.5, -14);
                DisplayBuilder.crimsonDust(impactLoc, 20, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VanishFeint(plugin); }
    }

    // ================================================================
    // 25. CEILING HANG — Inverted hang with dripping lava damage
    // ================================================================
    public static class CeilingHang extends BossAttack {
        private final List<BlockDisplayHandle> dripHandles = new ArrayList<>();
        private boolean hanging = false;
        private boolean dropped = false;

        public CeilingHang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ceiling_hang", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller climbing upward
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Climbing phase (0-40 ticks)
            if (ticksAlive < 40 && !hanging) {
                if (ticksAlive % 8 == 0) {
                    float height = (ticksAlive / 40.0f) * 15;
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(0, height, 0), 4, 0.5);
                    DisplayBuilder.playSound(center.clone().add(0, height, 0),
                        Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
                }
            }
            // Hanging inverted (tick 40)
            else if (ticksAlive == 40 && !hanging) {
                hanging = true;
                // Inverted Dweller form at height
                BlockDisplayHandle hangBody = displayBuilder.spawnBlock(
                    center.clone().add(0, 15, 0), Material.BLACKSTONE);
                hangBody.scale(1.2f, 2.5f, 1.2f)
                    .rotate((float) Math.PI, 1, 0, 0)
                    .glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(hangBody.entity());
                // Downward-facing cyan eyes
                DisplayBuilder.dustParticles(center.clone().add(0, 14, 0), 8, 0.3,
                    0, 150, 255, 1.0f);
            }
            // Dripping lava damage zone below (40-140 ticks)
            else if (hanging && !dropped && ticksAlive < 140) {
                if (ticksAlive % 12 == 0) {
                    // Drip particles falling from height
                    for (int i = 0; i < 3; i++) {
                        double ox = (Math.random() - 0.5) * 4;
                        double oz = (Math.random() - 0.5) * 4;
                        Location dripLoc = center.clone().add(ox, 0.2, oz);
                        BlockDisplayHandle drip = displayBuilder.spawnBlock(dripLoc, Material.MAGMA_BLOCK);
                        drip.scale(0.2f, 0.05f, 0.2f).glow(255, 100, 0).interpolation(2, 0);
                        dripHandles.add(drip);
                        spawnedEntities.add(drip.entity());
                        DisplayBuilder.dustParticles(dripLoc, 2, 0.2, 255, 100, 0, 0.6f);
                    }
                }
            }
            // DROP (tick 140)
            else if (ticksAlive == 140 && !dropped) {
                dropped = true;
                Location landLoc = center.clone().add(0, 0.1, 0);
                // Corruption ring on landing
                for (int i = 0; i < 14; i++) {
                    double a = (Math.PI * 2 / 14) * i;
                    Location ringLoc = landLoc.clone().add(Math.cos(a) * 4, 0.1, Math.sin(a) * 4);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(landLoc, 30, 4.0);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                triggerImpactDamage(landLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CeilingHang(plugin); }
    }

    // ================================================================
    // 26. SCREECH — Jaw-unhinging audio shockwave
    // ================================================================
    public static class Screech extends BossAttack {
        private boolean screeched = false;

        public Screech(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_screech", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller form with jaw starting to open
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 3.0f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Lower jaw block separating
            BlockDisplayHandle jaw = displayBuilder.spawnBlock(
                center.clone().add(0, 2.0, -0.5), Material.BLACKSTONE);
            jaw.scale(0.6f, 0.2f, 0.4f).glow(200, 0, 50).interpolation(5, 0);
            spawnedEntities.add(jaw.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaw opening wider (0-30 ticks)
            if (ticksAlive < 30 && !screeched) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2.2, -0.5), 4, 0.3);
                }
            }
            // SCREECH (tick 30)
            else if (ticksAlive == 30 && !screeched) {
                screeched = true;
                // Visible pressure shockwave ring at ground level
                for (int ring = 2; ring <= 14; ring += 3) {
                    for (int i = 0; i < ring * 2; i++) {
                        double a = (Math.PI * 2 / (ring * 2)) * i;
                        Location ringLoc = center.clone().add(
                            Math.cos(a) * ring, 0.3, Math.sin(a) * ring);
                        DisplayBuilder.crimsonDust(ringLoc, 2, 0.3);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
            }
            // Shockwave expanding (30-50 ticks)
            else if (screeched && ticksAlive <= 50) {
                float expand = (ticksAlive - 30) / 20.0f;
                if (ticksAlive % 4 == 0) {
                    float r = expand * 14;
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2 / 8) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 3, 0.4);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Screech(plugin); }
    }

    // ================================================================
    // 27. SHADOW GRAB — Teleport-grab with hold damage
    // ================================================================
    public static class ShadowGrab extends BossAttack {
        private boolean grabbed = false;
        private int holdTick = 0;

        public ShadowGrab(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_shadow_grab", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Quick teleport flash
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1, 0), 12, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brief telegraph (0-20 ticks — short, Tier 2 speed)
            if (ticksAlive < 20 && !grabbed) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1, 0), 6, 0.8);
                }
            }
            // GRAB — Dweller reappears with arms wrapping (tick 20)
            else if (ticksAlive == 20 && !grabbed) {
                grabbed = true;
                holdTick = 0;
                // Grab form — two arm blocks squeezing inward
                BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                    center.clone().add(-0.8, 1.2, 0), Material.BLACKSTONE);
                leftArm.scale(0.3f, 1.0f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(leftArm.entity());
                BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                    center.clone().add(0.8, 1.2, 0), Material.BLACKSTONE);
                rightArm.scale(0.3f, 1.0f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(rightArm.entity());
                DisplayBuilder.crimsonDust(center.clone().add(0, 1.2, 0), 10, 0.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.8f);
            }
            // Hold damage ticks (20-50 ticks = 1.5 seconds hold)
            else if (grabbed && holdTick < 30) {
                holdTick++;
                if (holdTick % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.2, 0), 6, 0.4);
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.6f);
                }
            }
            // Release — stagger back (tick 50)
            else if (holdTick == 30) {
                holdTick++;
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 2), 10, 1.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowGrab(plugin); }
    }

    // ================================================================
    // 28. CORRUPTION SURGE — Field-wide eruption, zones expand + heal
    // ================================================================
    public static class CorruptionSurge extends BossAttack {
        private final List<BlockDisplayHandle> zoneHandles = new ArrayList<>();
        private boolean surged = false;

        public CorruptionSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_surge", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at center, arms wide, conductor stance
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.8f, 3.0f, 0.6f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Corruption zone tiles across arena
            for (int i = 0; i < 10; i++) {
                double a = (Math.PI * 2 / 10) * i;
                double r = 5 + (i % 3) * 2;
                Location zoneLoc = center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                zone.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                zoneHandles.add(zone);
                spawnedEntities.add(zone.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flickering buildup on all zones (0-40 ticks)
            if (ticksAlive < 40 && !surged) {
                if (ticksAlive % 6 == 0) {
                    for (BlockDisplayHandle zone : zoneHandles) {
                        Location loc = zone.entity().getLocation();
                        DisplayBuilder.crimsonDust(loc.clone().add(0, 0.3, 0), 3, 0.3);
                    }
                }
            }
            // SURGE (tick 40)
            else if (ticksAlive == 40 && !surged) {
                surged = true;
                // All zones erupt 3 blocks high
                for (BlockDisplayHandle zone : zoneHandles) {
                    Location loc = zone.entity().getLocation();
                    DisplayBuilder.crimsonDust(loc.clone().add(0, 2, 0), 15, 1.0);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 1.5, 0), 8, 0.8,
                        255, 100, 0, 1.2f);
                    // Expand in all cardinals
                    for (double[] dir : new double[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        Location expandLoc = loc.clone().add(dir[0], 0, dir[1]);
                        BlockDisplayHandle expand = displayBuilder.spawnBlock(expandLoc, Material.MAGMA_BLOCK);
                        expand.scale(0.8f, 0.08f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(expand.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.3f);
                // Dweller heals — orange pulse through body
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 1.0,
                    255, 100, 0, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSurge(plugin); }
    }

    // ================================================================
    // 29. HUNT RUN — Full sprint at Marked player, unstoppable
    // ================================================================
    public static class HuntRun extends BossAttack {
        private final List<BlockDisplayHandle> cometTrail = new ArrayList<>();
        private boolean sprinting = false;
        private int sprintTick = 0;

        public HuntRun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_hunt_run", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes lock to magenta — Mark-color focus
            DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 10, 0.3,
                255, 0, 128, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye lock telegraph (0-20 ticks — short, Tier 2)
            if (ticksAlive < 20 && !sprinting) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 6, 0.2,
                        255, 0, 128, 1.0f);
                }
            }
            // SPRINT (tick 20)
            else if (ticksAlive == 20 && !sprinting) {
                sprinting = true;
                sprintTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.2f);
            }
            // Full speed sprint (20-50 ticks)
            else if (sprinting && sprintTick < 30) {
                sprintTick++;
                float progress = sprintTick / 30.0f;
                float sprintZ = -progress * 20;
                // Comet trail of soul flame
                if (sprintTick % 3 == 0) {
                    Location trailLoc = center.clone().add(0, 0.8, sprintZ + 2);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.NETHERRACK);
                    trail.scale(0.3f, 0.25f, 0.3f).glow(200, 0, 50).interpolation(1, 0);
                    cometTrail.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.crimsonDust(trailLoc, 5, 0.6);
                }
                // Corruption trail on ground
                if (sprintTick % 5 == 0) {
                    Location corruptLoc = center.clone().add(0, 0.05, sprintZ + 1);
                    BlockDisplayHandle corrupt = displayBuilder.spawnBlock(corruptLoc, Material.MAGMA_BLOCK);
                    corrupt.scale(0.6f, 0.05f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(corrupt.entity());
                }
            }
            // Contact impact
            else if (sprinting && sprintTick == 30) {
                sprintTick++;
                Location impactLoc = center.clone().add(0, 0.5, -20);
                DisplayBuilder.crimsonDust(impactLoc, 20, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HuntRun(plugin); }
    }

    // ================================================================
    // 30. EYE DRAIN — Life-draining gaze beam, Dweller heals
    // ================================================================
    public static class EyeDrain extends BossAttack {
        private final List<BlockDisplayHandle> drainBeam = new ArrayList<>();
        private boolean draining = false;
        private int drainTick = 0;

        public EyeDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_eye_drain", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller form with eyes shifting to hungry red
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye color shift to deep red (0-40 ticks)
            if (ticksAlive < 40 && !draining) {
                if (ticksAlive % 8 == 0) {
                    float shift = ticksAlive / 40.0f;
                    int red = (int) (0 + shift * 200);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 6, 0.3,
                        red, 0, (int) (150 * (1 - shift)), 1.0f);
                }
            }
            // Drain begins (tick 40)
            else if (ticksAlive == 40 && !draining) {
                draining = true;
                drainTick = 0;
                // Drain beam from target back to Dweller
                for (int i = 0; i < 10; i++) {
                    Location beamLoc = center.clone().add(0, 2.7, -1 - i * 1.5);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(beamLoc, Material.RED_STAINED_GLASS);
                    beam.scale(0.05f, 0.05f, 1.3f).glow(200, 0, 50).interpolation(2, 0);
                    drainBeam.add(beam);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.5f);
            }
            // Active drain — particles flowing from target to Dweller (40-120 ticks)
            else if (draining && drainTick < 80) {
                drainTick++;
                if (drainTick % 6 == 0) {
                    // Flow particles toward Dweller
                    float flowProgress = (drainTick % 20) / 20.0f;
                    float flowZ = -15 + flowProgress * 15;
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 2.5, flowZ), 4, 0.3);
                }
                // Dweller healing pulse every 20 ticks
                if (drainTick % 20 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 8, 0.8,
                        255, 100, 0, 1.0f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeDrain(plugin); }
    }
}
