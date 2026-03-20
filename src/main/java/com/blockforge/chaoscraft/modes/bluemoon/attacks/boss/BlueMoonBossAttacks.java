package com.blockforge.chaoscraft.modes.bluemoon.attacks.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Boss Attacks — 11 boss-specific block display attacks.
 * These extend BlockDisplayAttack for visual displays but classify as AttackType.BOSS.
 * Blue Moon palette: pale blue (180,210,255), silver (200,200,220), frost cyan (150,230,255).
 * NO status effects. Min 10 BlockDisplays per attack.
 */
public final class BlueMoonBossAttacks {

    private BlueMoonBossAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MoonbeamSmite(plugin));
        registry.register(new CrescentSlash(plugin));
        registry.register(new CraterRain(plugin));
        registry.register(new BossTidalPull(plugin));
        registry.register(new BossLunarEclipse(plugin));
        registry.register(new FrostNova(plugin));
        registry.register(new StarfallVolley(plugin));
        registry.register(new BossGravityInversion(plugin));
        registry.register(new MoonstoneCage(plugin));
        registry.register(new HowlingResonance(plugin));
        registry.register(new LunarSuperLaserDisplay(plugin));
    }

    // ================================================================
    // 1. MOONBEAM SMITE — Vertical beam of 14 SEA_LANTERN from Y+25
    //    Warning particle circle 20 ticks before. High impact.
    // ================================================================
    public static class MoonbeamSmite extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();
        private boolean activated = false;
        private static final int WARNING_TICKS = 20;

        public MoonbeamSmite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonbeam_smite", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(14.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(WARNING_TICKS);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 SEA_LANTERN blocks forming vertical beam from Y+25 down to ground
            for (int i = 0; i < 14; i++) {
                double y = 25.0 - i * 1.8;
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                block.scale(0.1f, 0.1f, 0.1f).glow(180, 210, 255).interpolation(3, 0)
                     .brightness(15, 15);
                beamBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Warning phase: pulsing circle on ground
            if (ticksAlive < WARNING_TICKS) {
                if (ticksAlive % 2 == 0) {
                    double pulseRadius = 3.0 + Math.sin(ticksAlive * 0.5) * 1.0;
                    DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), pulseRadius, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(180, 210, 255), 1.2f));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.6f + ticksAlive * 0.04f);
                }
                return;
            }

            // Activate beam
            if (!activated) {
                activated = true;
                for (int i = 0; i < beamBlocks.size(); i++) {
                    beamBlocks.get(i).scale(1.2f, 1.8f, 1.2f).interpolation(4, i);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.2f);
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);

                // Ground impact particles
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 30, 4.0, 150, 230, 255, 2.0f);
            }

            // Beam pulse animation
            float pulse = (float) (0.8 + Math.sin(ticksAlive * 0.15) * 0.4);
            for (BlockDisplayHandle block : beamBlocks) {
                block.scale(1.2f * pulse, 1.8f, 1.2f * pulse);
            }

            // Vertical light particles streaming down
            if (ticksAlive % 2 == 0) {
                double py = 25.0 - ((ticksAlive * 1.5) % 25.0);
                DisplayBuilder.dustParticles(c.clone().add(0, py, 0), 8, 1.0, 240, 240, 255, 1.5f);
                // Side glow particles
                double angle = ticksAlive * 0.2;
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(angle) * 2, py * 0.5, Math.sin(angle) * 2),
                        4, 0.5, 200, 200, 220, 1.0f);
            }

            // Ground frost ring
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), 4.0, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(150, 230, 255), 0.8f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonbeamSmite(plugin); }
    }

    // ================================================================
    // 2. CRESCENT SLASH — 16 BLUE_ICE arc sweeping horizontally
    //    Moves at 0.5 blocks/tick. Continuous damage.
    // ================================================================
    public static class CrescentSlash extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arcBlocks = new ArrayList<>();
        private double sweepAngle = 0;

        public CrescentSlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crescent_slash", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 BLUE_ICE blocks forming crescent arc at Y+8
            for (int i = 0; i < 16; i++) {
                double arcAngle = (Math.PI * 0.8) * ((double) i / 15) - (Math.PI * 0.4);
                double radius = 6.0;
                double x = Math.cos(arcAngle) * radius;
                double z = Math.sin(arcAngle) * radius;
                double y = 8.0 + Math.sin(arcAngle * 2) * 1.5;

                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float scale = 0.6f + (float) Math.abs(Math.sin(arcAngle)) * 0.5f;
                block.scale(scale, 0.4f, scale).glow(150, 230, 255).interpolation(2, 0)
                     .brightness(15, 15);
                arcBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep: rotate entire arc around center at 0.5 blocks/tick angular rate
            sweepAngle += 0.08; // ~0.5 blocks/tick at 6 radius
            for (int i = 0; i < arcBlocks.size(); i++) {
                double arcAngle = (Math.PI * 0.8) * ((double) i / 15) - (Math.PI * 0.4) + sweepAngle;
                double radius = 6.0;
                double x = Math.cos(arcAngle) * radius;
                double z = Math.sin(arcAngle) * radius;
                double y = 8.0 + Math.sin(arcAngle * 2) * 1.5 - (ticksAlive * 0.05); // slowly descend

                Location newLoc = c.clone().add(x, y, z);
                arcBlocks.get(i).entity().teleport(newLoc);
            }

            // Trail particles along the arc
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < arcBlocks.size(); i += 3) {
                    Entity e = arcBlocks.get(i).entity();
                    if (!e.isDead()) {
                        DisplayBuilder.dustParticles(e.getLocation(), 4, 0.5, 150, 230, 255, 1.2f);
                    }
                }
            }

            // Whoosh sound every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.0f + (float)(sweepAngle * 0.1));
            }

            // Frost particles on ground beneath arc
            if (ticksAlive % 3 == 0) {
                double groundAngle = sweepAngle + Math.PI * 0.2;
                DisplayBuilder.dustParticles(
                        c.clone().add(Math.cos(groundAngle) * 6, 0.2, Math.sin(groundAngle) * 6),
                        6, 1.5, 200, 200, 220, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrescentSlash(plugin); }
    }

    // ================================================================
    // 3. CRATER RAIN — 15 END_STONE+DEEPSLATE chunks (5 groups of 3)
    //    falling from Y+30, staggered. Impact per group.
    // ================================================================
    public static class CraterRain extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> chunkGroups = new ArrayList<>();
        private final double[] groupFallY = new double[5];
        private final boolean[] groupImpacted = new boolean[5];
        private final Location[] groupTargets = new Location[5];

        public CraterRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crater_rain", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Random rng = new Random();
            Material[] mats = {Material.END_STONE, Material.DEEPSLATE, Material.END_STONE};

            for (int g = 0; g < 5; g++) {
                List<BlockDisplayHandle> group = new ArrayList<>();
                // Each group targets a random offset near center
                double ox = (rng.nextDouble() - 0.5) * 12;
                double oz = (rng.nextDouble() - 0.5) * 12;
                groupTargets[g] = center.clone().add(ox, 0, oz);
                groupFallY[g] = 30.0;
                groupImpacted[g] = false;

                for (int b = 0; b < 3; b++) {
                    Location loc = groupTargets[g].clone().add(
                            (rng.nextDouble() - 0.5) * 2, 30.0 + b * 0.8, (rng.nextDouble() - 0.5) * 2);
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[b]);
                    float s = 0.8f + rng.nextFloat() * 0.6f;
                    block.scale(s, s, s).glow(200, 200, 220).interpolation(1, 0);
                    group.add(block);
                    spawnedEntities.add(block.entity());
                }
                chunkGroups.add(group);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int g = 0; g < 5; g++) {
                // Stagger: group g starts falling at tick g*12
                int startTick = g * 12;
                if (ticksAlive < startTick || groupImpacted[g]) continue;

                int fallTick = ticksAlive - startTick;
                // Accelerating fall: y = 30 - 0.5*t^2 * 0.04 (reaching ground in ~38 ticks)
                groupFallY[g] = 30.0 - (fallTick * fallTick * 0.02);

                if (groupFallY[g] <= 0.5) {
                    // Impact!
                    groupImpacted[g] = true;
                    Location impact = groupTargets[g];

                    // Remove chunk blocks
                    for (BlockDisplayHandle block : chunkGroups.get(g)) {
                        block.entity().remove();
                    }

                    // Impact particles
                    DisplayBuilder.dustParticles(impact.clone().add(0, 0.5, 0), 25, 3.0, 200, 200, 220, 2.0f);
                    DisplayBuilder.dustParticles(impact.clone().add(0, 1.0, 0), 15, 2.0, 150, 230, 255, 1.5f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                    // Crater ring on ground
                    DisplayBuilder.particleRing(impact.clone().add(0, 0.1, 0), 3.0, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(180, 210, 255), 1.0f));
                } else {
                    // Update falling position
                    for (int b = 0; b < chunkGroups.get(g).size(); b++) {
                        BlockDisplayHandle block = chunkGroups.get(g).get(b);
                        Location target = groupTargets[g].clone().add(
                                (b - 1) * 0.8, groupFallY[g] + b * 0.8, 0);
                        block.entity().teleport(target);
                    }

                    // Falling trail particles
                    if (fallTick % 3 == 0) {
                        DisplayBuilder.dustParticles(
                                groupTargets[g].clone().add(0, groupFallY[g], 0),
                                6, 1.0, 180, 210, 255, 1.0f);
                    }
                }
            }

            // Warning shadows on ground for upcoming groups
            for (int g = 0; g < 5; g++) {
                int startTick = g * 12;
                if (ticksAlive >= startTick - 10 && ticksAlive < startTick && !groupImpacted[g]) {
                    DisplayBuilder.particleRing(groupTargets[g].clone().add(0, 0.05, 0), 2.0, Particle.DUST, 10,
                            new Particle.DustOptions(Color.fromRGB(100, 100, 120), 0.6f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CraterRain(plugin); }
    }

    // ================================================================
    // 4. BOSS TIDAL PULL — 12 PRISMARINE blocks forming pull vortex.
    //    All players pulled toward center. Frost damage.
    // ================================================================
    public static class BossTidalPull extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> vortexBlocks = new ArrayList<>();
        private double vortexAngle = 0;

        public BossTidalPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("boss_tidal_pull", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(15);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 PRISMARINE blocks in spiral vortex pattern
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                double radius = 6.0 + (i % 3) * 1.5;
                double y = 0.5 + i * 0.4;
                Location loc = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                block.scale(0.7f, 0.5f, 0.7f).glow(150, 230, 255).interpolation(2, 0)
                     .brightness(12, 12);
                vortexBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            vortexAngle += 0.12;
            double contractRate = Math.max(0.5, 6.0 - ticksAlive * 0.05);

            // Spin and contract vortex blocks
            for (int i = 0; i < vortexBlocks.size(); i++) {
                double angle = (Math.PI * 2 / 12) * i + vortexAngle;
                double radius = contractRate + (i % 3) * 0.8;
                double y = 0.5 + i * 0.4 + Math.sin(ticksAlive * 0.1 + i) * 0.3;
                Location newLoc = c.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                vortexBlocks.get(i).entity().teleport(newLoc);
            }

            // Pull all nearby players toward center
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                            || p.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        double dist = p.getLocation().distance(c);
                        if (dist <= 8.0 && dist > 1.0) {
                            Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.35);
                            p.setVelocity(p.getVelocity().add(pull));
                        }
                    }
                }
            }

            // Vortex particles spiraling inward
            if (ticksAlive % 2 == 0) {
                for (int s = 0; s < 4; s++) {
                    double spiralAngle = vortexAngle * 2 + (Math.PI * 2 / 4) * s;
                    double spiralRadius = contractRate + 2;
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(spiralAngle) * spiralRadius, 1.5, Math.sin(spiralAngle) * spiralRadius),
                            4, 0.4, 150, 230, 255, 1.0f);
                }
            }

            // Water-like ambient sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.7f, 0.8f);
            }

            // Frost floor particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0), 10, contractRate + 1, 200, 200, 220, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BossTidalPull(plugin); }
    }

    // ================================================================
    // 5. BOSS LUNAR ECLIPSE — 18 DEEPSLATE+BLACK_CONCRETE disc descends
    //    from Y+20. Shadow damage beneath. Continuous.
    // ================================================================
    public static class BossLunarEclipse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();
        private double discHeight = 20.0;

        public BossLunarEclipse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("boss_lunar_eclipse", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(15);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.DEEPSLATE, Material.BLACK_CONCRETE};
            Random rng = new Random();

            // 18 blocks forming a flat disc at Y+20
            for (int i = 0; i < 18; i++) {
                double angle = (Math.PI * 2 / 18) * i;
                double ringRadius = (i < 6) ? 1.5 : (i < 12) ? 3.5 : 5.5;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;

                Location loc = center.clone().add(x, 20.0, z);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[rng.nextInt(mats.length)]);
                float s = (ringRadius < 2) ? 1.5f : (ringRadius < 4) ? 1.2f : 0.8f;
                block.scale(s, 0.3f, s).glow(40, 40, 60).interpolation(3, 0);
                discBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly descend the disc
            discHeight = Math.max(5.0, 20.0 - ticksAlive * 0.15);

            for (int i = 0; i < discBlocks.size(); i++) {
                double angle = (Math.PI * 2 / 18) * i;
                double ringRadius = (i < 6) ? 1.5 : (i < 12) ? 3.5 : 5.5;
                // Slight wobble
                double wobble = Math.sin(ticksAlive * 0.05 + i * 0.3) * 0.2;
                Location newLoc = c.clone().add(
                        Math.cos(angle) * ringRadius, discHeight + wobble, Math.sin(angle) * ringRadius);
                discBlocks.get(i).entity().teleport(newLoc);
            }

            // Shadow particles beneath the disc
            if (ticksAlive % 3 == 0) {
                Random rng = new Random();
                for (int s = 0; s < 8; s++) {
                    double sx = (rng.nextDouble() - 0.5) * 10;
                    double sz = (rng.nextDouble() - 0.5) * 10;
                    DisplayBuilder.dustParticles(c.clone().add(sx, 0.3, sz), 3, 0.5, 30, 30, 50, 1.5f);
                }
            }

            // Edge glow on the disc
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, discHeight, 0), 5.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.8f));
            }

            // Darkness tendrils reaching down
            if (ticksAlive % 6 == 0) {
                double tendrilAngle = ticksAlive * 0.15;
                for (double h = discHeight; h > 0; h -= 2.0) {
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(tendrilAngle) * 2, h, Math.sin(tendrilAngle) * 2),
                            3, 0.3, 40, 40, 60, 1.0f);
                }
            }

            // Ambient sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BossLunarEclipse(plugin); }
    }

    // ================================================================
    // 6. FROST NOVA — 16 BLUE_ICE blocks in expanding ring from center
    //    outward (radius grows 0.3/tick). Must jump over or outrun.
    // ================================================================
    public static class FrostNova extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private double ringRadius = 1.0;

        public FrostNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_nova", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 BLUE_ICE blocks forming tight ring at center
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 0.3, Math.sin(angle) * 1.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                block.scale(0.6f, 0.8f, 0.6f).glow(150, 230, 255).interpolation(2, 0)
                     .brightness(14, 14);
                ringBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Center burst
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 20, 1.5, 180, 210, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expand ring at 0.3 blocks/tick
            ringRadius = 1.0 + ticksAlive * 0.3;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location newLoc = c.clone().add(Math.cos(angle) * ringRadius, 0.3, Math.sin(angle) * ringRadius);
                ringBlocks.get(i).entity().teleport(newLoc);

                // Scale shrinks as ring expands (thins out)
                float s = Math.max(0.3f, 0.6f - (float)(ticksAlive * 0.003));
                ringBlocks.get(i).scale(s, 0.8f, s);
            }

            // Frost trail on inner edge
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 8; p++) {
                    double trailAngle = (Math.PI * 2 / 8) * p + ticksAlive * 0.1;
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(trailAngle) * (ringRadius - 0.5), 0.2, Math.sin(trailAngle) * (ringRadius - 0.5)),
                            3, 0.3, 150, 230, 255, 0.8f);
                }
            }

            // Outer edge particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), ringRadius + 0.3, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.0f));
            }

            // Crackling ice sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.5f + (float)(ringRadius * 0.02));
            }

            // Ground frost fill behind the ring
            if (ticksAlive % 5 == 0) {
                Random rng = new Random();
                for (int f = 0; f < 5; f++) {
                    double fAngle = rng.nextDouble() * Math.PI * 2;
                    double fRadius = rng.nextDouble() * ringRadius * 0.8;
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(fAngle) * fRadius, 0.1, Math.sin(fAngle) * fRadius),
                            2, 0.3, 200, 200, 220, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrostNova(plugin); }
    }

    // ================================================================
    // 7. STARFALL VOLLEY — 12 SEA_LANTERN small projectiles fire outward
    //    radially from center, each leaving frost particle trail.
    // ================================================================
    public static class StarfallVolley extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private final double[] starRadii = new double[12];
        private final double[] starAngles = new double[12];
        private final boolean[] starLanded = new boolean[12];

        public StarfallVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("starfall_volley", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(250);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 small SEA_LANTERN projectiles at center, Y+5
            for (int i = 0; i < 12; i++) {
                starAngles[i] = (Math.PI * 2 / 12) * i;
                starRadii[i] = 0.5;
                starLanded[i] = false;

                Location loc = center.clone().add(0, 5.0, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                block.scale(0.35f, 0.35f, 0.35f).glow(240, 240, 255).interpolation(1, 0)
                     .brightness(15, 15);
                starBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < 12; i++) {
                if (starLanded[i]) continue;

                // Projectiles fly outward and arc downward
                starRadii[i] += 0.6;
                double height = 5.0 - (starRadii[i] * starRadii[i] * 0.015); // parabolic arc

                if (height <= 0.3) {
                    // Impact
                    starLanded[i] = true;
                    Location impactLoc = c.clone().add(
                            Math.cos(starAngles[i]) * starRadii[i], 0.3,
                            Math.sin(starAngles[i]) * starRadii[i]);

                    starBlocks.get(i).entity().remove();
                    DisplayBuilder.dustParticles(impactLoc, 15, 2.0, 150, 230, 255, 1.5f);
                    DisplayBuilder.dustParticles(impactLoc, 8, 1.0, 240, 240, 255, 1.0f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);

                    // Frost splash ring
                    DisplayBuilder.particleRing(impactLoc, 2.0, Particle.DUST, 10,
                            new Particle.DustOptions(Color.fromRGB(200, 200, 220), 0.8f));
                } else {
                    // Move projectile
                    Location newLoc = c.clone().add(
                            Math.cos(starAngles[i]) * starRadii[i], height,
                            Math.sin(starAngles[i]) * starRadii[i]);
                    starBlocks.get(i).entity().teleport(newLoc);

                    // Frost trail particles
                    if (ticksAlive % 2 == 0) {
                        DisplayBuilder.dustParticles(newLoc, 3, 0.3, 150, 230, 255, 0.8f);
                    }
                }
            }

            // Central glow while stars are flying
            boolean anyFlying = false;
            for (boolean landed : starLanded) if (!landed) { anyFlying = true; break; }
            if (anyFlying && ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 6, 1.0, 180, 210, 255, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarfallVolley(plugin); }
    }

    // ================================================================
    // 8. BOSS GRAVITY INVERSION — 10 CRYING_OBSIDIAN blocks forming
    //    upward arrows. Launch players up (velocity Y=1.2). Fall damage.
    // ================================================================
    public static class BossGravityInversion extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arrowBlocks = new ArrayList<>();
        private boolean launched = false;
        private static final int CHARGE_TICKS = 25;

        public BossGravityInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("boss_gravity_inversion", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(CHARGE_TICKS);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 CRYING_OBSIDIAN blocks forming upward arrow pattern
            // Arrow shaft (6 blocks vertical)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 0.5 + i * 0.8, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                block.scale(0.5f, 0.8f, 0.5f).glow(180, 100, 255).interpolation(2, 0);
                arrowBlocks.add(block);
                spawnedEntities.add(block.entity());
            }
            // Arrow head (4 blocks forming V shape at top)
            double[][] headOffsets = {{-1.0, 4.0, 0}, {1.0, 4.0, 0}, {-0.5, 5.0, 0}, {0.5, 5.0, 0}};
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                block.scale(0.6f, 0.6f, 0.6f).glow(200, 130, 255).interpolation(2, 0)
                     .brightness(12, 12);
                arrowBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Charge phase: blocks pulse and grow
            if (ticksAlive < CHARGE_TICKS) {
                float pulse = (float) (0.5 + Math.sin(ticksAlive * 0.3) * 0.2);
                for (BlockDisplayHandle block : arrowBlocks) {
                    block.scale(pulse, 0.8f, pulse);
                }

                // Warning particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 10, 3.0, 180, 100, 255, 1.0f);
                    DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), 6.0, Particle.DUST, 14,
                            new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.8f));
                }
                return;
            }

            // Launch!
            if (!launched) {
                launched = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);

                // Launch all nearby survival players
                for (Player p : w.getPlayers()) {
                    if ((p.getGameMode() == org.bukkit.GameMode.SURVIVAL
                            || p.getGameMode() == org.bukkit.GameMode.ADVENTURE)
                            && p.getLocation().distance(c) <= 6.0) {
                        p.setVelocity(new Vector(0, 1.2, 0));
                    }
                }

                // Arrow blocks shoot upward
                for (BlockDisplayHandle block : arrowBlocks) {
                    Location up = block.entity().getLocation().add(0, 15, 0);
                    block.entity().teleport(up);
                }

                // Blast particles
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 30, 6.0, 180, 210, 255, 2.0f);
            }

            // Post-launch: upward particle streams
            if (ticksAlive % 2 == 0) {
                double streamY = ((ticksAlive - CHARGE_TICKS) * 1.5) % 20.0;
                for (int s = 0; s < 4; s++) {
                    double angle = (Math.PI * 2 / 4) * s + ticksAlive * 0.1;
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(angle) * 2, streamY, Math.sin(angle) * 2),
                            3, 0.4, 150, 230, 255, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BossGravityInversion(plugin); }
    }

    // ================================================================
    // 9. MOONSTONE CAGE — 16 QUARTZ_BLOCK+CALCITE ring around player.
    //    Contracts over 50 ticks. Must escape.
    // ================================================================
    public static class MoonstoneCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cageBlocks = new ArrayList<>();
        private double cageRadius = 6.0;

        public MoonstoneCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonstone_cage", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(50); // Damage starts after contraction
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.QUARTZ_BLOCK, Material.CALCITE};

            // 16 alternating QUARTZ_BLOCK/CALCITE forming ring wall + pillars
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Material mat = mats[i % 2];

                // Base block
                Location baseLoc = center.clone().add(Math.cos(angle) * 6.0, 0.2, Math.sin(angle) * 6.0);
                BlockDisplayHandle base = displayBuilder.spawnBlock(baseLoc, mat);
                base.scale(0.6f, 2.0f, 0.6f).glow(200, 200, 220).interpolation(3, 0)
                    .brightness(13, 13);
                cageBlocks.add(base);
                spawnedEntities.add(base.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 0.6f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract cage over first 50 ticks
            if (ticksAlive < 50) {
                cageRadius = 6.0 - (ticksAlive * 0.1); // 6.0 -> 1.0 over 50 ticks
            } else {
                cageRadius = 1.0;
            }

            // Update cage block positions
            for (int i = 0; i < cageBlocks.size(); i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location newLoc = c.clone().add(
                        Math.cos(angle) * cageRadius, 0.2, Math.sin(angle) * cageRadius);
                cageBlocks.get(i).entity().teleport(newLoc);

                // Scale grows taller as cage contracts
                float heightScale = 2.0f + (float)((6.0 - cageRadius) * 0.3);
                cageBlocks.get(i).scale(0.6f, heightScale, 0.6f);
            }

            // Warning particles on inner edge
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 1.0, 0), cageRadius - 0.3, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.8f));
            }

            // Grinding stone sound during contraction
            if (ticksAlive < 50 && ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.8f + ticksAlive * 0.01f);
            }

            // After contraction: crushing particles
            if (ticksAlive >= 50 && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 12, 1.0, 200, 200, 220, 1.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 8, 0.8, 150, 230, 255, 1.0f);
            }

            // Top rim glow
            if (ticksAlive % 4 == 0) {
                float h = 2.0f + (float)((6.0 - cageRadius) * 0.3);
                DisplayBuilder.particleRing(c.clone().add(0, h, 0), cageRadius, Particle.DUST, 10,
                        new Particle.DustOptions(Color.fromRGB(240, 240, 255), 0.6f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonstoneCage(plugin); }
    }

    // ================================================================
    // 10. HOWLING RESONANCE — 14 WHITE_CONCRETE blocks forming shockwave
    //     ring. Expands rapidly. Visual buff indicator for all attacks.
    // ================================================================
    public static class HowlingResonance extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private double waveRadius = 1.0;

        public HowlingResonance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("howling_resonance", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 WHITE_CONCRETE blocks in tight initial ring
            for (int i = 0; i < 14; i++) {
                double angle = (Math.PI * 2 / 14) * i;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 0.5, Math.sin(angle) * 1.0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                block.scale(0.8f, 0.4f, 0.8f).glow(240, 240, 255).interpolation(2, 0)
                     .brightness(15, 15);
                waveBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Initial howl
            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rapid expansion: 0.5 blocks/tick
            waveRadius = 1.0 + ticksAlive * 0.5;

            for (int i = 0; i < waveBlocks.size(); i++) {
                double angle = (Math.PI * 2 / 14) * i;
                Location newLoc = c.clone().add(
                        Math.cos(angle) * waveRadius, 0.5 + Math.sin(ticksAlive * 0.2 + i) * 0.3,
                        Math.sin(angle) * waveRadius);
                waveBlocks.get(i).entity().teleport(newLoc);

                // Fade scale as wave expands
                float s = Math.max(0.3f, 0.8f - (float)(ticksAlive * 0.01));
                waveBlocks.get(i).scale(s, 0.4f, s);
            }

            // Shockwave particle ring at leading edge
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), waveRadius, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(240, 240, 255), 1.5f));
                // Inner silver trail
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), waveRadius - 1.0, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(200, 200, 220), 0.8f));
            }

            // Resonance echoes at intervals
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 0.4f, 0.8f + ticksAlive * 0.03f);
            }

            // Buff visual: silver sparkle particles at center (indicates all attacks buffed)
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3.0, 0), 10, 2.0, 200, 200, 220, 1.2f);
                // Vertical silver beams
                for (int b = 0; b < 3; b++) {
                    double beamAngle = (Math.PI * 2 / 3) * b + ticksAlive * 0.05;
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(beamAngle) * 3, 5.0, Math.sin(beamAngle) * 3),
                            4, 0.5, 240, 240, 255, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HowlingResonance(plugin); }
    }

    // ================================================================
    // 11. LUNAR SUPER LASER DISPLAY — 20 SEA_LANTERN+DIAMOND_BLOCK
    //     thick vertical beam column. Sweeps in arc. VISUAL ONLY
    //     (damage handled by BlueMoonBossManager).
    // ================================================================
    public static class LunarSuperLaserDisplay extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();
        private double sweepAngle = 0;
        private double sweepDirection = 1;

        public LunarSuperLaserDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_super_laser_display", AttackType.BOSS, 1, "modes/bluemoon/attacks"));
            // No damage — damage handled by BlueMoonBossManager
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(100);
        }

        @Override
        public AttackType getType() { return AttackType.BOSS; }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] mats = {Material.SEA_LANTERN, Material.DIAMOND_BLOCK};

            // 20 blocks forming thick vertical beam column from Y+25 down to ground
            for (int i = 0; i < 20; i++) {
                double y = 25.0 - i * 1.3;
                // Core column (alternating materials)
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mats[i % 2]);
                float coreScale = 1.5f + (float) Math.sin(i * 0.3) * 0.3f;
                block.scale(coreScale, 1.3f, coreScale).glow(180, 210, 255).interpolation(3, 0)
                     .brightness(15, 15);
                beamBlocks.add(block);
                spawnedEntities.add(block.entity());
            }

            // Massive startup sounds
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.8f);

            // Initial burst
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 40, 5.0, 150, 230, 255, 3.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep arc: oscillate back and forth
            sweepAngle += 0.04 * sweepDirection;
            if (Math.abs(sweepAngle) > Math.PI * 0.3) {
                sweepDirection = -sweepDirection;
            }

            double sweepOffsetX = Math.sin(sweepAngle) * 8.0;
            double sweepOffsetZ = Math.cos(sweepAngle) * 4.0;

            // Update beam blocks with sweep offset (top stays fixed, bottom sweeps)
            for (int i = 0; i < beamBlocks.size(); i++) {
                double t = (double) i / (beamBlocks.size() - 1); // 0 at top, 1 at bottom
                double y = 25.0 - i * 1.3;
                double ox = sweepOffsetX * t * t; // quadratic: more offset at bottom
                double oz = sweepOffsetZ * t * t;

                Location newLoc = c.clone().add(ox, y, oz);
                beamBlocks.get(i).entity().teleport(newLoc);

                // Pulsating scale
                float pulse = (float) (1.5 + Math.sin(ticksAlive * 0.15 + i * 0.2) * 0.4);
                beamBlocks.get(i).scale(pulse, 1.3f, pulse);
            }

            // Dense white/cyan dust particles spiraling around beam
            if (ticksAlive % 1 == 0) { // every tick
                for (int s = 0; s < 6; s++) {
                    double spiralAngle = ticksAlive * 0.3 + (Math.PI * 2 / 6) * s;
                    double spiralY = (ticksAlive * 0.8 + s * 4) % 25.0;
                    double t = spiralY / 25.0;
                    double spiralRadius = 2.0 + Math.sin(spiralAngle) * 0.5;
                    double ox = sweepOffsetX * t * t;
                    double oz = sweepOffsetZ * t * t;

                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(spiralAngle) * spiralRadius + ox,
                                    25.0 - spiralY, Math.sin(spiralAngle) * spiralRadius + oz),
                            4, 0.3, 240, 240, 255, 1.5f);
                    // Cyan accent
                    DisplayBuilder.dustParticles(
                            c.clone().add(Math.cos(spiralAngle + 0.5) * (spiralRadius + 1) + ox,
                                    25.0 - spiralY, Math.sin(spiralAngle + 0.5) * (spiralRadius + 1) + oz),
                            3, 0.3, 150, 230, 255, 1.2f);
                }
            }

            // Frost trail on ground where beam base touches
            if (ticksAlive % 2 == 0) {
                double baseX = sweepOffsetX;
                double baseZ = sweepOffsetZ;
                Location groundLoc = c.clone().add(baseX, 0.1, baseZ);

                // Wide frost patch
                DisplayBuilder.dustParticles(groundLoc, 15, 3.0, 200, 200, 220, 1.0f);
                DisplayBuilder.dustParticles(groundLoc, 8, 1.5, 150, 230, 255, 1.5f);

                // Frost ring at beam base
                DisplayBuilder.particleRing(groundLoc, 2.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 1.2f));
            }

            // Beam hum sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            }

            // Periodic thunder crack
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
            }

            // Sky glow at beam origin
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 25, 0), 12, 3.0, 180, 210, 255, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarSuperLaserDisplay(plugin); }
    }
}
