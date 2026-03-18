package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Environmental -- GROUP 9: SANCTUM'S WRATH (Sanctum Finale Events)
 * 10 attacks (#81-90) where the Fractured Sanctum's architecture physically dies.
 * Gold spires fall, amethyst arrays detonate, floor sinks, central pillar detonates.
 *
 * Design notes:
 * - No status effects
 * - Void Emperor palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Sanctum materials: GOLD_BLOCK, AMETHYST_BLOCK, OBSIDIAN, DEEPSLATE
 * - Structural collapse -- permanent visual damage, shockwave mechanics
 * - Late fight -- damage range 8.0-18.0 HP
 */
public final class SanctumWrath {

    private SanctumWrath() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NorthSpireCollapse(plugin));
        registry.register(new SouthSpireCollapse(plugin));
        registry.register(new EastAmethystDetonation(plugin));
        registry.register(new WestAmethystDetonation(plugin));
        registry.register(new GoldArchwayFracture(plugin));
        registry.register(new FloorSinkZoneA(plugin));
        registry.register(new FloorSinkZoneB(plugin));
        registry.register(new CentralPillarBurst(plugin));
        registry.register(new VoidWallRush(plugin));
        registry.register(new FinalArchitectureCollapse(plugin));
    }

    // =========================================================================
    // 81. (Doc #83) NORTH SPIRE COLLAPSE -- gold spire falls with ground shockwave
    // =========================================================================
    public static class NorthSpireCollapse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rubbleHandles = new ArrayList<>();
        private Location spireBase;
        private boolean collapsed = false;

        public NorthSpireCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_spire_collapse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 30s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // North spire position
            spireBase = center.clone().add(0, 0, -16);

            // Warning: cracking particles
            w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, 8, 0), 30, 1, 4, 1, 0,
                    Material.GOLD_BLOCK.createBlockData());
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, 10, 0), 20, 0.5, 1, 0.5, 0.05);
            w.spawnParticle(Particle.CRIT, spireBase.clone().add(0, 12, 0), 15, 0.5, 0.5, 0.5, 0.2);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || spireBase == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks (4 seconds)
            if (ticksAlive <= 80) {
                if (ticksAlive % 10 == 0) {
                    double progress = ticksAlive / 80.0;
                    int crackY = (int) (12 * progress);
                    w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, crackY, 0), 15, 0.5, 0.5, 0.5, 0,
                            Material.GOLD_BLOCK.createBlockData());
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, crackY, 0), 10, 0.5, 0.5, 0.5, 0.03);
                }
                return;
            }

            // Collapse at tick 81
            if (!collapsed) {
                collapsed = true;

                // Cascading collapse sounds
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(spireBase, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 0.4f), idx * 5L);
                }

                // Particle cascade: top to bottom over 2 seconds (40 ticks)
                for (int y = 12; y >= 0; y--) {
                    final int yLevel = y;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, yLevel, 0), 20, 1, 0.5, 1, 0,
                                Material.GOLD_BLOCK.createBlockData());
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, yLevel, 0), 10, 1, 0.5, 1, 0.05);
                        w.spawnParticle(Particle.CRIT, spireBase.clone().add(0, yLevel, 0), 8, 0.5, 0.3, 0.5, 0.1);
                    }, (long) ((12 - yLevel) * 3));
                }

                // Ground shockwave at collapse completion (delayed 40 ticks)
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    DisplayBuilder.playSound(spireBase, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.5f);

                    // Shockwave particles
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase, 80, 5, 0.5, 5, 0.1);
                    w.spawnParticle(Particle.BLOCK, spireBase, 60, 5, 0.5, 5, 0,
                            Material.GOLD_BLOCK.createBlockData());

                    // Shockwave damage: 20 within 10 blocks, knockback 4 blocks
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(spireBase) <= 100.0) {
                            p.damage(20.0); // 10 hearts

                            Vector kb = p.getLocation().toVector().subtract(spireBase.toVector()).normalize().multiply(1.5);
                            kb.setY(0.3);
                            p.setVelocity(kb);
                        }
                    }

                    // Rubble displays
                    for (int i = 0; i < 6; i++) {
                        double a = (2 * Math.PI * i) / 6;
                        Location rubbleLoc = spireBase.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(rubbleLoc, Material.GOLD_BLOCK);
                        h.scale(1.2f, 0.3f, 1.2f).glow(255, 200, 0).interpolation(5, 0);
                        rubbleHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }, 40L);
            }

            // Aftermath: rubble zone particles
            if (ticksAlive > 120 && ticksAlive % 10 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, 0.5, 0), 5, 2, 0.3, 2, 0.02);
                w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, 0.3, 0), 3, 2, 0.2, 2, 0,
                        Material.GOLD_BLOCK.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthSpireCollapse(plugin); }
    }

    // =========================================================================
    // 82. (Doc #84) SOUTH SPIRE COLLAPSE -- same as north + gold shard projectiles
    // =========================================================================
    public static class SouthSpireCollapse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rubbleHandles = new ArrayList<>();
        private Location spireBase;
        private boolean collapsed = false;

        public SouthSpireCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("south_spire_collapse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 60s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            spireBase = center.clone().add(0, 0, 16);

            w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, 8, 0), 30, 1, 4, 1, 0,
                    Material.GOLD_BLOCK.createBlockData());
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, 10, 0), 20, 0.5, 1, 0.5, 0.05);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || spireBase == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks (4 seconds) -- same as north
            if (ticksAlive <= 80) {
                if (ticksAlive % 10 == 0) {
                    double progress = ticksAlive / 80.0;
                    int crackY = (int) (12 * progress);
                    w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, crackY, 0), 15, 0.5, 0.5, 0.5, 0,
                            Material.GOLD_BLOCK.createBlockData());
                }
                return;
            }

            // Collapse
            if (!collapsed) {
                collapsed = true;

                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(spireBase, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 0.4f), idx * 5L);
                }

                // Cascade particles
                for (int y = 12; y >= 0; y--) {
                    final int yLevel = y;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        w.spawnParticle(Particle.BLOCK, spireBase.clone().add(0, yLevel, 0), 20, 1, 0.5, 1, 0,
                                Material.GOLD_BLOCK.createBlockData());
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, yLevel, 0), 10, 1, 0.5, 1, 0.05);
                    }, (long) ((12 - yLevel) * 3));
                }

                // Ground shockwave + gold shard projectiles
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    DisplayBuilder.playSound(spireBase, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.5f);

                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase, 80, 5, 0.5, 5, 0.1);
                    w.spawnParticle(Particle.BLOCK, spireBase, 60, 5, 0.5, 5, 0,
                            Material.GOLD_BLOCK.createBlockData());

                    // Base shockwave: 20 damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(spireBase) <= 100.0) {
                            p.damage(20.0);
                            Vector kb = p.getLocation().toVector().subtract(spireBase.toVector()).normalize().multiply(1.5);
                            kb.setY(0.3);
                            p.setVelocity(kb);
                        }
                    }

                    // 3 gold shard projectiles toward random player positions
                    List<Player> targets = new ArrayList<>(w.getPlayers().stream()
                            .filter(p -> !isExempt(p))
                            .filter(p -> p.getLocation().distanceSquared(spireBase) <= 2500)
                            .toList());
                    for (int shard = 0; shard < Math.min(3, targets.size()); shard++) {
                        Player target = targets.get(shard % targets.size());
                        Location shardTarget = target.getLocation().clone();
                        final int shardIdx = shard;

                        // Animate shard arc
                        for (int step = 0; step < 15; step++) {
                            final int s = step;
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                double t = (double) s / 15;
                                Location arcLoc = spireBase.clone().add(
                                        (shardTarget.getX() - spireBase.getX()) * t,
                                        6 * Math.sin(t * Math.PI), // Arc
                                        (shardTarget.getZ() - spireBase.getZ()) * t
                                );
                                w.spawnParticle(Particle.TOTEM_OF_UNDYING, arcLoc, 8, 0.2, 0.2, 0.2, 0.02);

                                // Impact on last step
                                if (s == 14) {
                                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, shardTarget, 20, 1, 0.5, 1, 0.05);
                                    for (Player p : w.getPlayers()) {
                                        if (isExempt(p)) continue;
                                        if (p.getLocation().distanceSquared(shardTarget) <= 4.0) {
                                            p.damage(8.0); // 4 hearts per shard
                                        }
                                    }
                                }
                            }, shardIdx * 8L + s * 2L);
                        }
                    }

                    // Rubble displays
                    for (int i = 0; i < 6; i++) {
                        double a = (2 * Math.PI * i) / 6;
                        Location rubbleLoc = spireBase.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(rubbleLoc, Material.GOLD_BLOCK);
                        h.scale(1.2f, 0.3f, 1.2f).glow(255, 200, 0).interpolation(5, 0);
                        rubbleHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }, 40L);
            }

            // Aftermath
            if (ticksAlive > 120 && ticksAlive % 10 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, spireBase.clone().add(0, 0.5, 0), 5, 2, 0.3, 2, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthSpireCollapse(plugin); }
    }

    // =========================================================================
    // 83. (Doc #85) EAST AMETHYST ARRAY DETONATION -- explosive shatter + shard scatter
    // =========================================================================
    public static class EastAmethystDetonation extends EnvironmentalAttack {

        private Location arrayPos;
        private boolean detonated = false;

        public EastAmethystDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("east_amethyst_detonation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 45s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            arrayPos = center.clone().add(16, 0, 0);

            // Warning glow
            w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 4, 0), 20, 1, 2, 1, 0.03);
            w.spawnParticle(Particle.BLOCK, arrayPos.clone().add(0, 3, 0), 15, 1, 1, 1, 0,
                    Material.AMETHYST_BLOCK.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || arrayPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 3, 0), 12, 1, 2, 1, 0.02);
                    w.spawnParticle(Particle.BLOCK, arrayPos.clone().add(0, 2, 0), 8, 1, 1, 1, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;

                // Sounds
                DisplayBuilder.playSound(arrayPos, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(arrayPos, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f), idx * 3L);
                }
                DisplayBuilder.playSound(arrayPos, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 0.8f);

                // Explosive burst
                w.spawnParticle(Particle.BLOCK, arrayPos, 150, 4, 4, 4, 0,
                        Material.AMETHYST_BLOCK.createBlockData());
                w.spawnParticle(Particle.END_ROD, arrayPos, 100, 4, 4, 4, 0.1);

                // Tiered damage: 25 within 8, 14 within 15
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(arrayPos);
                    if (dist <= 8) p.damage(25.0);
                    else if (dist <= 15) p.damage(14.0);
                }

                // 12 amethyst shard projectiles traveling 20 blocks
                for (int dir = 0; dir < 12; dir++) {
                    double shardAngle = (2 * Math.PI * dir) / 12;
                    final double sa = shardAngle;

                    for (int step = 0; step < 20; step++) {
                        final int s = step;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            double dist = s + 1;
                            Location shardPos = arrayPos.clone().add(
                                    Math.cos(sa) * dist, 1.0, Math.sin(sa) * dist);
                            w.spawnParticle(Particle.BLOCK, shardPos, 4, 0.2, 0.2, 0.2, 0,
                                    Material.AMETHYST_BLOCK.createBlockData());
                            w.spawnParticle(Particle.END_ROD, shardPos, 2, 0.1, 0.1, 0.1, 0.01);

                            // Shard damage: 10 in 2-block radius
                            for (Player p : w.getPlayers()) {
                                if (isExempt(p)) continue;
                                if (p.getLocation().distanceSquared(shardPos) <= 4.0) {
                                    p.damage(10.0);
                                }
                            }
                        }, step * 1L);
                    }
                }
            }

            // Aftermath: END_ROD smoke
            int afterTick = ticksAlive - 61;
            if (afterTick > 0 && afterTick <= 120 && afterTick % 10 == 0) {
                w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 1, 0), 8, 2, 1, 2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastAmethystDetonation(plugin); }
    }

    // =========================================================================
    // 84. (Doc #86) WEST AMETHYST ARRAY DETONATION -- same as east + wind push
    // =========================================================================
    public static class WestAmethystDetonation extends EnvironmentalAttack {

        private Location arrayPos;
        private boolean detonated = false;

        public WestAmethystDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("west_amethyst_detonation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 75s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            arrayPos = center.clone().add(-16, 0, 0);

            w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 4, 0), 20, 1, 2, 1, 0.03);
            w.spawnParticle(Particle.BLOCK, arrayPos.clone().add(0, 3, 0), 15, 1, 1, 1, 0,
                    Material.AMETHYST_BLOCK.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || arrayPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 3, 0), 12, 1, 2, 1, 0.02);
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;

                DisplayBuilder.playSound(arrayPos, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
                for (int i = 0; i < 4; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(arrayPos, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f), idx * 3L);
                }

                // Explosion
                w.spawnParticle(Particle.BLOCK, arrayPos, 150, 4, 4, 4, 0,
                        Material.AMETHYST_BLOCK.createBlockData());
                w.spawnParticle(Particle.END_ROD, arrayPos, 100, 4, 4, 4, 0.1);

                // Tiered damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(arrayPos);
                    if (dist <= 8) p.damage(25.0);
                    else if (dist <= 15) p.damage(14.0);
                }

                // Shard scatter
                for (int dir = 0; dir < 12; dir++) {
                    double shardAngle = (2 * Math.PI * dir) / 12;
                    final double sa = shardAngle;
                    for (int step = 0; step < 20; step++) {
                        final int s = step;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            Location shardPos = arrayPos.clone().add(
                                    Math.cos(sa) * (s + 1), 1.0, Math.sin(sa) * (s + 1));
                            w.spawnParticle(Particle.BLOCK, shardPos, 4, 0.2, 0.2, 0.2, 0,
                                    Material.AMETHYST_BLOCK.createBlockData());
                            for (Player p : w.getPlayers()) {
                                if (isExempt(p)) continue;
                                if (p.getLocation().distanceSquared(shardPos) <= 4.0) {
                                    p.damage(10.0);
                                }
                            }
                        }, step * 1L);
                    }
                }

                // West-specific: wind push 1 second after detonation
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // CLOUD particles rushing west to east
                    for (int i = 0; i < 20; i++) {
                        double z = center.getZ() + (Math.random() - 0.5) * 30;
                        double y = center.getY() + 1 + Math.random() * 8;
                        for (double x = arrayPos.getX(); x <= arrayPos.getX() + 32; x += 2) {
                            w.spawnParticle(Particle.CLOUD, new Location(w, x, y, z), 2, 0.3, 0.3, 0.3, 0.02);
                        }
                    }

                    // Push all players east
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 900) {
                            p.setVelocity(new Vector(1.2, 0.2, 0)); // Push east (+X)
                        }
                    }
                }, 20L);
            }

            // Aftermath
            int afterTick = ticksAlive - 61;
            if (afterTick > 0 && afterTick <= 120 && afterTick % 10 == 0) {
                w.spawnParticle(Particle.END_ROD, arrayPos.clone().add(0, 1, 0), 8, 2, 1, 2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WestAmethystDetonation(plugin); }
    }

    // =========================================================================
    // 85. (Doc #87) GOLD ARCHWAY FRACTURE -- chunk falls from archway
    // =========================================================================
    public static class GoldArchwayFracture extends EnvironmentalAttack {

        private Location archwayPos;
        private Location landingPos;
        private boolean fractured = false;

        public GoldArchwayFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gold_archway_fracture", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(500); // 25 seconds, up to 3 times
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random archway position at arena edge
            double angle = Math.random() * 2 * Math.PI;
            archwayPos = center.clone().add(Math.cos(angle) * 18, 8, Math.sin(angle) * 18);

            // Landing position within 15 blocks
            double landAngle = angle + (Math.random() - 0.5) * 1.5;
            double landDist = 5 + Math.random() * 10;
            landingPos = center.clone().add(Math.cos(landAngle) * landDist, 0, Math.sin(landAngle) * landDist);

            // Warning: crackling sparks
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, archwayPos, 15, 0.5, 0.5, 0.5, 0.05);
            w.spawnParticle(Particle.CRIT, archwayPos, 10, 0.3, 0.3, 0.3, 0.1);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || archwayPos == null || landingPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 8 == 0) {
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, archwayPos, 10, 0.5, 0.5, 0.5, 0.03);
                    w.spawnParticle(Particle.CRIT, archwayPos, 8, 0.3, 0.3, 0.3, 0.08);
                }
                return;
            }

            // Falling chunk: tick 51-90 (2 seconds of fall)
            if (!fractured) {
                fractured = true;
                DisplayBuilder.playSound(archwayPos, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.5f);
            }

            int fallTick = ticksAlive - 50;
            if (fallTick > 0 && fallTick <= 40) {
                // Arc trajectory from archway to landing
                double t = fallTick / 40.0;
                double chunkX = archwayPos.getX() + (landingPos.getX() - archwayPos.getX()) * t;
                double chunkZ = archwayPos.getZ() + (landingPos.getZ() - archwayPos.getZ()) * t;
                double chunkY = archwayPos.getY() + (landingPos.getY() - archwayPos.getY()) * t
                        + 4 * Math.sin(t * Math.PI); // Parabolic arc

                Location chunkLoc = new Location(w, chunkX, chunkY, chunkZ);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, chunkLoc, 12, 0.3, 0.3, 0.3, 0.02);
                w.spawnParticle(Particle.CRIT, chunkLoc, 6, 0.2, 0.2, 0.2, 0.05);

                // Landing impact
                if (fallTick == 40) {
                    DisplayBuilder.playSound(landingPos, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.6f);

                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, landingPos, 100, 3, 1, 3, 0.08);
                    w.spawnParticle(Particle.CRIT, landingPos, 50, 2, 0.5, 2, 0.15);

                    // 18 damage in 6-block radius + knockback
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landingPos) <= 36.0) {
                            p.damage(18.0); // 9 hearts

                            Vector kb = p.getLocation().toVector().subtract(landingPos.toVector()).normalize().multiply(1.0);
                            kb.setY(0.3);
                            p.setVelocity(kb);
                        }
                    }
                }
            }

            // Aftermath: debris at landing
            if (fallTick > 40 && fallTick <= 80 && fallTick % 10 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, landingPos.clone().add(0, 0.3, 0), 5, 1.5, 0.3, 1.5, 0.02);
                w.spawnParticle(Particle.CRIT, landingPos.clone().add(0, 0.5, 0), 3, 1, 0.3, 1, 0.05);
            }

            // Source archway crack particles
            if (fallTick > 0 && fallTick <= 60 && fallTick % 15 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, archwayPos, 5, 0.5, 0.5, 0.5, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldArchwayFracture(plugin); }
    }

    // =========================================================================
    // 86. (Doc #88) FLOOR SINK: ZONE A -- permanent sinking hazard zone
    // =========================================================================
    public static class FloorSinkZoneA extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> sinkHandles = new ArrayList<>();
        private Location sinkCenter;
        private boolean sunk = false;

        public FloorSinkZoneA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_sink_zone_a", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0); // 5 hearts per second
            config.setDamageRadius(8.0);
            config.setDurationTicks(6000); // Persists
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 90s
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Zone A: offset toward northeast
            sinkCenter = center.clone().add(6, 0, -6);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || sinkCenter == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 70 ticks (3.5 seconds)
            if (ticksAlive <= 70) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 12; i++) {
                        Location crackLoc = sinkCenter.clone().add(
                                (Math.random() - 0.5) * 12, 0.1, (Math.random() - 0.5) * 12);
                        w.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.3, 0.1, 0.3, 0,
                                Material.STONE.createBlockData());
                    }
                    // Smoke rising
                    w.spawnParticle(Particle.SMOKE, sinkCenter.clone().add(0, 0.5, 0), 10, 3, 1, 3, 0.02);
                }
                return;
            }

            // Sink established
            if (!sunk) {
                sunk = true;

                // Cascade sounds
                for (int i = 0; i < 6; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(sinkCenter, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.4f), idx * 3L);
                }
                DisplayBuilder.playSound(sinkCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.4f);

                w.spawnParticle(Particle.BLOCK, sinkCenter, 150, 6, 0.5, 6, 0,
                        Material.STONE.createBlockData());

                // Sunken floor displays
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location sinkLoc = sinkCenter.clone().add(Math.cos(a) * 4, -0.3, Math.sin(a) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(sinkLoc, Material.DEEPSLATE);
                    h.scale(2.0f, 0.15f, 2.0f).glow(80, 0, 160).interpolation(5, 0);
                    sinkHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Persistent zone particles
            if (sunk && ticksAlive % 6 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location hazLoc = sinkCenter.clone().add(
                            (Math.random() - 0.5) * 12, 0.3, (Math.random() - 0.5) * 12);
                    w.spawnParticle(Particle.DRAGON_BREATH, hazLoc, 3, 0.3, 0.2, 0.3, 0.01);
                    w.spawnParticle(Particle.SMOKE, hazLoc.clone().add(0, 0.3, 0), 2, 0.3, 0.3, 0.3, 0.01);
                }
            }

            // Damage + slow: players in zone
            if (sunk && ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - sinkCenter.getX());
                    double dz = Math.abs(p.getLocation().getZ() - sinkCenter.getZ());
                    if (dx <= 6 && dz <= 6) {
                        p.damage(10.0); // 5 hearts per second

                        // Movement disruption via knockback (simulating slow)
                        Vector backward = p.getVelocity().multiply(-0.3);
                        backward.setY(0);
                        p.setVelocity(p.getVelocity().add(backward));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorSinkZoneA(plugin); }
    }

    // =========================================================================
    // 87. (Doc #89) FLOOR SINK: ZONE B -- second sinking zone
    // =========================================================================
    public static class FloorSinkZoneB extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> sinkHandles = new ArrayList<>();
        private Location sinkCenter;
        private boolean sunk = false;

        public FloorSinkZoneB(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_sink_zone_b", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 120s
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Zone B: offset toward southwest
            sinkCenter = center.clone().add(-6, 0, 6);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || sinkCenter == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 70 ticks
            if (ticksAlive <= 70) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 12; i++) {
                        Location crackLoc = sinkCenter.clone().add(
                                (Math.random() - 0.5) * 12, 0.1, (Math.random() - 0.5) * 12);
                        w.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.3, 0.1, 0.3, 0,
                                Material.STONE.createBlockData());
                    }
                    w.spawnParticle(Particle.SMOKE, sinkCenter.clone().add(0, 0.5, 0), 10, 3, 1, 3, 0.02);
                }
                return;
            }

            if (!sunk) {
                sunk = true;

                for (int i = 0; i < 6; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(sinkCenter, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.4f), idx * 3L);
                }
                DisplayBuilder.playSound(sinkCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.4f);

                w.spawnParticle(Particle.BLOCK, sinkCenter, 150, 6, 0.5, 6, 0,
                        Material.STONE.createBlockData());

                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location sinkLoc = sinkCenter.clone().add(Math.cos(a) * 4, -0.3, Math.sin(a) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(sinkLoc, Material.DEEPSLATE);
                    h.scale(2.0f, 0.15f, 2.0f).glow(80, 0, 160).interpolation(5, 0);
                    sinkHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            if (sunk && ticksAlive % 6 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location hazLoc = sinkCenter.clone().add(
                            (Math.random() - 0.5) * 12, 0.3, (Math.random() - 0.5) * 12);
                    w.spawnParticle(Particle.DRAGON_BREATH, hazLoc, 3, 0.3, 0.2, 0.3, 0.01);
                    w.spawnParticle(Particle.SMOKE, hazLoc.clone().add(0, 0.3, 0), 2, 0.3, 0.3, 0.3, 0.01);
                }
            }

            if (sunk && ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - sinkCenter.getX());
                    double dz = Math.abs(p.getLocation().getZ() - sinkCenter.getZ());
                    if (dx <= 6 && dz <= 6) {
                        p.damage(10.0);
                        Vector backward = p.getVelocity().multiply(-0.3);
                        backward.setY(0);
                        p.setVelocity(p.getVelocity().add(backward));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorSinkZoneB(plugin); }
    }

    // =========================================================================
    // 88. (Doc #90) CENTRAL SANCTUM PILLAR BURST -- massive central detonation at 7% HP
    // =========================================================================
    public static class CentralPillarBurst extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> voidHandles = new ArrayList<>();
        private boolean detonated = false;
        private int fogStartTick = -1;

        public CentralPillarBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("central_pillar_burst", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(99999); // Scripted once at 7% HP
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: spiral cracking from base to top
            DisplayBuilder.playSound(center, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks (5 seconds)
            if (ticksAlive <= 100) {
                if (ticksAlive % 5 == 0) {
                    double progress = ticksAlive / 100.0;
                    double spiralY = progress * 20;
                    double spiralAngle = progress * 6 * Math.PI;

                    // Spiral crack particles
                    Location spiralLoc = center.clone().add(
                            Math.cos(spiralAngle) * 1.5, spiralY, Math.sin(spiralAngle) * 1.5);
                    w.spawnParticle(Particle.BLOCK, spiralLoc, 10, 0.3, 0.3, 0.3, 0,
                            Material.STONE.createBlockData());

                    // Top: TOTEM + CRIT burst
                    if (progress > 0.8) {
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 18, 0), 10, 0.5, 1, 0.5, 0.05);
                        w.spawnParticle(Particle.CRIT, center.clone().add(0, 19, 0), 8, 0.5, 0.5, 0.5, 0.1);
                    }

                    // Base: DRAGON_BREATH eruption
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 0.5, 0), 8, 1, 0.5, 1, 0.02);
                }
                return;
            }

            // Full detonation
            if (!detonated) {
                detonated = true;
                fogStartTick = ticksAlive;

                // ALL ambient sounds simultaneously
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.4f);

                // Massive burst: ~1150 particles
                w.spawnParticle(Particle.WITCH, center, 100, 5, 8, 5, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 5, 8, 5, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 75, 5, 8, 5, 0.05);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 150, 5, 8, 5, 0.05);
                w.spawnParticle(Particle.CRIT, center, 50, 4, 6, 4, 0.2);
                w.spawnParticle(Particle.BLOCK, center, 100, 4, 4, 4, 0,
                        Material.STONE.createBlockData());

                // Tiered damage: 35/22/10
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 5) p.damage(35.0);
                    else if (dist <= 12) p.damage(22.0);
                    else if (dist <= 20) p.damage(10.0);
                }

                // Void cloud displays
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location vLoc = center.clone().add(Math.cos(a) * 4, 0.1, Math.sin(a) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(vLoc, Material.OBSIDIAN);
                    h.scale(2.0f, 0.3f, 2.0f).glow(128, 0, 255).interpolation(5, 0);
                    voidHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Lingering cloud: 6 seconds
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 120) {
                if ((ticksAlive - fogStartTick) % 3 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1.5, 0), 15, 4, 2, 4, 0.01);
                }
            }

            // Rubble particles
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 180 && ticksAlive % 15 == 0) {
                w.spawnParticle(Particle.BLOCK, center.clone().add(0, 0.5, 0), 8, 3, 0.5, 3, 0,
                        Material.STONE.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralPillarBurst(plugin); }
    }

    // =========================================================================
    // 89. (Doc #91) VOID WALL RUSH -- void energy from east and west walls converging
    // =========================================================================
    public static class VoidWallRush extends EnvironmentalAttack {

        private boolean rushed = false;

        public VoidWallRush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_wall_rush", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(99999); // Scripted at Phase 3 + 150s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: east and west walls glowing
            Location eastWall = center.clone().add(20, 3, 0);
            Location westWall = center.clone().add(-20, 3, 0);

            w.spawnParticle(Particle.DRAGON_BREATH, eastWall, 30, 0.5, 5, 8, 0.02);
            w.spawnParticle(Particle.DRAGON_BREATH, westWall, 30, 0.5, 5, 8, 0.02);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    Location eastWall = center.clone().add(20, 3, 0);
                    Location westWall = center.clone().add(-20, 3, 0);
                    w.spawnParticle(Particle.DRAGON_BREATH, eastWall, 15, 0.5, 5, 8, 0.02);
                    w.spawnParticle(Particle.DRAGON_BREATH, westWall, 15, 0.5, 5, 8, 0.02);
                }
                return;
            }

            // Rush: two walls sweeping inward over 2.5 seconds (50 ticks)
            if (!rushed) {
                rushed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.8f, 0.4f);
            }

            int rushTick = ticksAlive - 40;
            if (rushTick > 0 && rushTick <= 50) {
                double progress = rushTick / 50.0;
                double eastX = center.getX() + 20 - 20 * progress; // Moving west
                double westX = center.getX() - 20 + 20 * progress; // Moving east

                // Wall particles
                for (int i = 0; i < 12; i++) {
                    double z = center.getZ() + (Math.random() - 0.5) * 30;
                    double y = center.getY() + Math.random() * 15;
                    w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, eastX, y, z), 3, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, westX, y, z), 3, 0.3, 0.5, 0.3, 0.01);
                }

                // Collision at center
                if (rushTick >= 48) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.7f);

                    w.spawnParticle(Particle.DRAGON_BREATH, center, 150, 2, 5, 8, 0.03);

                    // Damage central strip: 24 damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double px = Math.abs(p.getLocation().getX() - center.getX());
                        if (px <= 5) { // Central strip
                            p.damage(24.0); // 12 hearts
                        }
                    }
                }
            }

            // Aftermath: dispersal at center
            if (rushTick > 50 && rushTick <= 90 && rushTick % 8 == 0) {
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 2, 0), 10, 3, 2, 3, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWallRush(plugin); }
    }

    // =========================================================================
    // 90. (Doc #92) FINAL ARCHITECTURE COLLAPSE -- everything collapses at 4% HP
    // =========================================================================
    public static class FinalArchitectureCollapse extends EnvironmentalAttack {

        private boolean collapseStarted = false;

        public FinalArchitectureCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_architecture_collapse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(4.0); // 2 hearts per second ambient
            config.setDamageRadius(30.0); // Full arena
            config.setDurationTicks(180); // 9 seconds (6s collapse + 3s aftermath)
            config.setCooldownTicks(99999); // Scripted once at 4% HP
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Everything cracking simultaneously
            for (int i = 0; i < 50; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * 20;
                double y = Math.random() * 12;
                Location crackLoc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                w.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.5, 0.5, 0.5, 0,
                        Material.STONE.createBlockData());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks (3 seconds) -- everything cracking
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 20;
                        double y = Math.random() * 15;
                        Location crackLoc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.BLOCK, crackLoc, 8, 0.5, 0.5, 0.5, 0,
                                Material.STONE.createBlockData());
                    }
                }
                return;
            }

            // Sustained collapse: 120 ticks (6 seconds)
            if (!collapseStarted) {
                collapseStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.3f);
            }

            int collapseTick = ticksAlive - 60;
            if (collapseTick > 0 && collapseTick <= 120) {
                // Continuous structural debris
                if (collapseTick % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 20;
                        double y = Math.random() * 15;
                        Location debrisLoc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.BLOCK, debrisLoc, 5, 0.5, 0.5, 0.5, 0,
                                Material.STONE.createBlockData());
                    }
                }

                // Gold element sparks
                if (collapseTick % 10 == 0) {
                    for (int s = 0; s < 4; s++) {
                        double a = (2 * Math.PI * s) / 4;
                        Location goldLoc = center.clone().add(Math.cos(a) * 16, 5, Math.sin(a) * 16);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, goldLoc, 10, 1, 2, 1, 0.05);
                        w.spawnParticle(Particle.CRIT, goldLoc, 8, 0.5, 1, 0.5, 0.1);
                    }
                }

                // Amethyst remnant END_ROD sparks
                if (collapseTick % 15 == 0) {
                    for (int s = 0; s < 6; s++) {
                        double a = (2 * Math.PI * s) / 6;
                        Location amLoc = center.clone().add(Math.cos(a) * 14, 3, Math.sin(a) * 14);
                        w.spawnParticle(Particle.END_ROD, amLoc, 8, 0.5, 1, 0.5, 0.02);
                        w.spawnParticle(Particle.BLOCK, amLoc, 5, 0.5, 0.5, 0.5, 0,
                                Material.AMETHYST_BLOCK.createBlockData());
                    }
                }

                // Ambient sounds overlapping
                if (collapseTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.6f, 0.3f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.5f, 0.4f);
                }

                // Ambient damage: 2/sec handled by base config
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalArchitectureCollapse(plugin); }
    }
}
