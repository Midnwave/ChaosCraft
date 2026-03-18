package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 Environmental -- GROUP 6: ISLAND CONSUMPTION EVENTS
 * 10 attacks (#51-60) where remaining end stone is devoured by brimstone.
 * These attacks accelerate the physical shrinking of the arena.
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Island consumption materials: MAGMA_BLOCK, CRIMSON_NYLIUM, NETHERRACK, BASALT, BLACKSTONE
 * - Late fight -- higher damage, shorter warnings
 */
public final class IslandConsumption {

    private IslandConsumption() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BrimstoneCreep(plugin));
        registry.register(new MoltenIngest(plugin));
        registry.register(new FissureMouth(plugin));
        registry.register(new RootCollapse(plugin));
        registry.register(new MagmaBloom(plugin));
        registry.register(new TideOfConsumption(plugin));
        registry.register(new ConsumptionSurge(plugin));
        registry.register(new VoidSeep(plugin));
        registry.register(new BrimstoneBurial(plugin));
        registry.register(new FinalStone(plugin));
    }

    // =========================================================================
    // 51. BRIMSTONE CREEP -- wave of brimstone expands outward, consuming end stone
    // =========================================================================
    public static class BrimstoneCreep extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private Location origin;
        private boolean waveStarted = false;

        public BrimstoneCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_creep", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            origin = center.clone();

            // Warning: basalt break x3
            DisplayBuilder.playSound(origin, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(origin, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f), 8L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(origin, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f), 16L);

            // Warning particles -- crimson spores spiraling outward
            DisplayBuilder.crimsonDust(origin, 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(origin, 12, 2.0, 180, 30, 30, 0.8f);
                }
                return;
            }

            // Begin wave expansion
            if (!waveStarted) {
                waveStarted = true;
                DisplayBuilder.playSound(origin, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            }

            int waveTick = ticksAlive - 40;
            double radius = waveTick * 0.3; // ~4 blocks per second expansion
            if (radius > 12) radius = 12;

            // Spawn wave front blocks at intervals
            if (waveTick % 5 == 0 && radius <= 12) {
                int segments = Math.max(6, (int) (radius * 2));
                for (int i = 0; i < segments; i++) {
                    double angle = (2 * Math.PI * i) / segments;
                    Location loc = origin.clone().add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                    Material mat = (i % 2 == 0) ? Material.MAGMA_BLOCK : Material.CRIMSON_NYLIUM;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(1.0f, 0.15f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    waveHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Lava fountain particles on converted blocks
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.3, 0), 3, 0.1, 0.5, 0.1, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc.clone().add(0, 0.5, 0), 2, 0.2, 0, 0.2, 0);
                }
            }

            // Damage players on converting tiles
            if (waveTick % 4 == 0) {
                double waveFront = radius;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(origin);
                    if (dist >= waveFront - 1.5 && dist <= waveFront + 1.0) {
                        p.damage(16.0); // 8 hearts
                        DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5, 255, 80, 0, 1.0f);
                    }
                }
            }

            // Ambient particles
            if (waveTick % 8 == 0) {
                DisplayBuilder.dustParticles(origin, 6, radius, 255, 80, 0, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCreep(plugin); }
    }

    // =========================================================================
    // 52. MOLTEN INGEST -- three edge columns sink into magma
    // =========================================================================
    public static class MoltenIngest extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private final Location[] columnLocations = new Location[3];
        private boolean sinkingStarted = false;

        public MoltenIngest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_ingest", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(440); // 22 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three edge columns at evenly spaced angles
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 + Math.random() * 0.5;
                columnLocations[i] = center.clone().add(Math.cos(angle) * 16, 0, Math.sin(angle) * 16);

                // Warning smoke
                w.spawnParticle(Particle.SMOKE, columnLocations[i].clone().add(0, 1, 0), 15, 0.5, 1, 0.5, 0.02);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 15 == 0) {
                    for (Location col : columnLocations) {
                        if (col == null) continue;
                        w.spawnParticle(Particle.SMOKE, col.clone().add(0, 2, 0), 8, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                return;
            }

            // Begin sinking
            if (!sinkingStarted) {
                sinkingStarted = true;
                for (Location col : columnLocations) {
                    if (col == null) continue;
                    DisplayBuilder.playSound(col, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);

                    // Spawn column block displays
                    for (int y = 0; y < 3; y++) {
                        Location loc = col.clone().add(0, y, 0);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                        h.scale(2.0f, 1.0f, 2.0f).glow(255, 100, 0).interpolation(5, 0);
                        columnHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Soul fire rings
                    DisplayBuilder.dustParticles(col, 15, 2.0, 0, 150, 255, 1.0f);
                }
            }

            int sinkTick = ticksAlive - 60;
            float sinkAmount = Math.min(sinkTick * 0.025f, 2.0f);

            // Animate sinking
            for (BlockDisplayHandle h : columnHandles) {
                BlockDisplay bd = h.entity();
                Location loc = bd.getLocation();
                loc.setY(loc.getY() - 0.025);
                bd.teleport(loc);
            }

            // Lava particles streaming upward
            if (sinkTick % 4 == 0) {
                for (Location col : columnLocations) {
                    if (col == null) continue;
                    w.spawnParticle(Particle.LAVA, col.clone().add(0, 1, 0), 10, 0.3, 2.0, 0.3, 0);
                    DisplayBuilder.dustParticles(col, 6, 1.5, 0, 150, 255, 0.8f);
                }
            }

            // Damage players on sinking columns
            if (sinkTick % 10 == 0) {
                for (Location col : columnLocations) {
                    if (col == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(col) <= 6.25) { // 2.5 block radius
                            p.damage(20.0); // 10 hearts contact
                            p.setVelocity(p.getVelocity().setY(0.6)); // teleport upward
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenIngest(plugin); }
    }

    // =========================================================================
    // 53. FISSURE MOUTH -- straight crack across island floor
    // =========================================================================
    public static class FissureMouth extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private Location crackStart;
        private double crackAngle;
        private boolean crackOpened = false;

        public FissureMouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fissure_mouth", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(10.0); // 5 hearts/sec lava fountain
            config.setDamageRadius(1.5);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(360); // 18 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Perpendicular to nearest player movement
            crackAngle = Math.random() * Math.PI;
            crackStart = center.clone();

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);

            // Telegraph crack line with crimson spore particles
            for (int i = -5; i <= 5; i++) {
                Location loc = center.clone().add(Math.cos(crackAngle) * i, 0.1, Math.sin(crackAngle) * i);
                DisplayBuilder.dustParticles(loc, 4, 0.3, 220, 40, 20, 0.7f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    for (int i = -5; i <= 5; i++) {
                        Location loc = crackStart.clone().add(Math.cos(crackAngle) * i, 0.1, Math.sin(crackAngle) * i);
                        DisplayBuilder.dustParticles(loc, 3, 0.2, 220, 40, 20, 0.6f);
                    }
                }
                return;
            }

            // Open crack
            if (!crackOpened) {
                crackOpened = true;
                DisplayBuilder.playSound(crackStart, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);

                // Spawn fissure blocks
                for (int i = -5; i <= 5; i++) {
                    Location loc = crackStart.clone().add(Math.cos(crackAngle) * i, 0.01, Math.sin(crackAngle) * i);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.8f, 0.05f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                    crackHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Damage + fling players on crack line
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check if player is near the crack line
                    Location pl = p.getLocation();
                    double dx = pl.getX() - crackStart.getX();
                    double dz = pl.getZ() - crackStart.getZ();
                    double perpDist = Math.abs(dx * Math.sin(crackAngle) - dz * Math.cos(crackAngle));
                    double parDist = Math.abs(dx * Math.cos(crackAngle) + dz * Math.sin(crackAngle));
                    if (perpDist <= 1.0 && parDist <= 6.0) {
                        p.damage(18.0); // 9 hearts
                        // Fling 3 blocks to nearest side
                        double flingDir = (dx * Math.sin(crackAngle) - dz * Math.cos(crackAngle)) >= 0 ? 1.0 : -1.0;
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(
                                Math.sin(crackAngle) * flingDir * 0.8, 0.3, -Math.cos(crackAngle) * flingDir * 0.8)));
                    }
                }
            }

            // Lava fountain particles from crack
            int crackTick = ticksAlive - 40;
            if (crackTick % 3 == 0) {
                for (int i = -5; i <= 5; i++) {
                    Location loc = crackStart.clone().add(Math.cos(crackAngle) * i, 0.5, Math.sin(crackAngle) * i);
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, Math.random() * 4, 0), 2, 0.1, 0.5, 0.1, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc, 1, 0.2, 0, 0.2, 0);
                }
                // Flame particles curling outward at ground level
                DisplayBuilder.dustParticles(crackStart, 5, 5.0, 255, 120, 0, 0.5f);
            }

            // Flanking terrain conversion particles
            if (crackTick % 15 == 0 && crackTick <= 60) {
                for (int i = -5; i <= 5; i++) {
                    for (int side = -1; side <= 1; side += 2) {
                        Location loc = crackStart.clone().add(
                                Math.cos(crackAngle) * i + Math.sin(crackAngle) * side * 1.5, 0.02,
                                Math.sin(crackAngle) * i - Math.cos(crackAngle) * side * 1.5);
                        DisplayBuilder.dustParticles(loc, 3, 0.3, 200, 0, 50, 0.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FissureMouth(plugin); }
    }

    // =========================================================================
    // 54. ROOT COLLAPSE -- scattered tiles darken and collapse
    // =========================================================================
    public static class RootCollapse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> tileHandles = new ArrayList<>();
        private final List<Location> tileLocations = new ArrayList<>();
        private boolean collapsed = false;

        public RootCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("root_collapse", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(320); // 16 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.4f);

            // 8 scattered non-adjacent tile positions
            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 10;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0.02, Math.sin(angle) * dist);
                tileLocations.add(loc);

                // Dark tile displays
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(1.0f, 0.1f, 1.0f).glow(50, 45, 40).interpolation(5, 0);
                tileHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks (2.5 seconds) -- tiles flicker dark/light
            if (ticksAlive <= 50) {
                if (ticksAlive % 6 == 0) {
                    boolean dark = (ticksAlive / 6) % 2 == 0;
                    for (BlockDisplayHandle h : tileHandles) {
                        h.glow(dark ? 30 : 80, dark ? 25 : 75, dark ? 20 : 70);
                    }
                    for (Location loc : tileLocations) {
                        DisplayBuilder.dustParticles(loc, 3, 0.3, 160, 155, 150, 0.5f);
                    }
                }
                return;
            }

            // Collapse
            if (!collapsed) {
                collapsed = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);

                // Tiles drop
                for (int i = 0; i < tileHandles.size(); i++) {
                    BlockDisplayHandle h = tileHandles.get(i);
                    Location loc = h.entity().getLocation();
                    loc.setY(loc.getY() - 4.0); // Drop to catch layer
                    h.entity().teleport(loc);

                    // Soul particles from below
                    DisplayBuilder.dustParticles(tileLocations.get(i), 8, 0.4, 10, 220, 255, 0.8f);
                }

                // Damage players on collapsing tiles
                for (Location tileLoc : tileLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tileLoc) <= 1.5) {
                            p.damage(4.0); // 2 hearts fall damage
                            p.setVelocity(p.getVelocity().setY(-0.4));
                        }
                    }
                }
            }

            int collapseTick = ticksAlive - 50;

            // Brimstone rises after 0.5 seconds (10 ticks)
            if (collapseTick == 10) {
                for (int i = 0; i < tileLocations.size(); i++) {
                    Location loc = tileLocations.get(i);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());

                    // Burn damage when brimstone rises
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 2.0) {
                            p.damage(16.0); // 8 hearts burn
                            w.spawnParticle(Particle.LAVA, p.getLocation(), 5, 0.3, 0.5, 0.3, 0);
                        }
                    }
                }
            }

            // Ash particles rising
            if (collapseTick % 6 == 0) {
                for (Location loc : tileLocations) {
                    DisplayBuilder.dustParticles(loc, 4, 0.4, 160, 155, 150, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RootCollapse(plugin); }
    }

    // =========================================================================
    // 55. MAGMA BLOOM -- flower-like magma eruptions at 4 locations
    // =========================================================================
    public static class MagmaBloom extends EnvironmentalAttack {

        private final Location[] bloomLocations = new Location[4];
        private boolean erupted = false;

        public MagmaBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_bloom", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); // 9 hearts
            config.setImpactRadius(3.0);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 bloom positions -- spread across arena
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i + Math.random() * 0.5;
                double dist = 5 + Math.random() * 8;
                bloomLocations[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            }

            // Warning: rapid basalt break
            for (int s = 0; s < 5; s++) {
                int delay = s * 4;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.7f), delay);
            }

            // Warning: dripping lava from above each bloom
            for (Location bl : bloomLocations) {
                if (bl == null) continue;
                w.spawnParticle(Particle.DRIPPING_LAVA, bl.clone().add(0, 3, 0), 10, 0.5, 0.5, 0.5, 0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    for (Location bl : bloomLocations) {
                        if (bl == null) continue;
                        w.spawnParticle(Particle.DRIPPING_LAVA, bl.clone().add(0, 3, 0), 6, 0.4, 0.3, 0.4, 0);
                    }
                }
                return;
            }

            // Eruption
            if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);

                for (Location bl : bloomLocations) {
                    if (bl == null) continue;

                    // Spawn flower-like magma cluster
                    BlockDisplayHandle coreH = displayBuilder.spawnBlock(bl, Material.MAGMA_BLOCK);
                    coreH.scale(1.5f, 0.3f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(coreH.entity());

                    // Petal blocks
                    for (int p = 0; p < 4; p++) {
                        double pAngle = (Math.PI / 2) * p;
                        Location petalLoc = bl.clone().add(Math.cos(pAngle) * 1.2, 0.1, Math.sin(pAngle) * 1.2);
                        BlockDisplayHandle ph = displayBuilder.spawnBlock(petalLoc, Material.CRIMSON_NYLIUM);
                        ph.scale(0.8f, 0.2f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                        spawnedEntities.add(ph.entity());
                    }

                    // Radial lava splash
                    w.spawnParticle(Particle.LAVA, bl.clone().add(0, 1, 0), 25, 3.0, 1.0, 3.0, 0);

                    // Impact damage
                    triggerImpactDamage(bl);
                }
            }

            int eruptTick = ticksAlive - 40;

            // Flame spirals from bloom centers
            if (eruptTick % 4 == 0) {
                for (Location bl : bloomLocations) {
                    if (bl == null) continue;
                    double spiralAngle = eruptTick * 0.2;
                    Location spiralLoc = bl.clone().add(Math.cos(spiralAngle) * 1.5, 0.5 + eruptTick * 0.05, Math.sin(spiralAngle) * 1.5);
                    DisplayBuilder.dustParticles(spiralLoc, 5, 0.3, 255, 90, 0, 1.0f);
                }
            }

            // Magma contact damage while standing on blooms
            if (eruptTick % 20 == 0 && eruptTick > 0) {
                for (Location bl : bloomLocations) {
                    if (bl == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(bl) <= 9.0) {
                            p.damage(8.0); // 4 hearts/sec
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaBloom(plugin); }
    }

    // =========================================================================
    // 56. TIDE OF CONSUMPTION -- pulsing brimstone burn across entire island
    // =========================================================================
    public static class TideOfConsumption extends EnvironmentalAttack {

        private int pulseCount = 0;

        public TideOfConsumption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tide_of_consumption", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);

            // Warning: smoke rising from all surfaces
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * 14;
                Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                w.spawnParticle(Particle.SMOKE, loc, 5, 0.3, 0.5, 0.3, 0.01);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.SMOKE, loc, 3, 0.2, 0.3, 0.2, 0.01);
                    }
                }
                return;
            }

            int pulseTick = ticksAlive - 40;

            // Three pulse peaks at ticks 0, 27, 54 (~1 second apart within 80-tick active window)
            boolean isPulsePeak = (pulseTick == 1 || pulseTick == 27 || pulseTick == 54);

            if (isPulsePeak) {
                pulseCount++;

                // Determine pulse color: deep red -> bright amber
                int r, g, b;
                if (pulseCount % 2 == 1) {
                    r = 180; g = 20; b = 0; // deep red
                } else {
                    r = 255; g = 160; b = 0; // bright amber
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.8f, 0.5f);

                // Lava fountains across entire brimstone surface
                for (int i = 0; i < 20; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 1, 0), 5, 0.2, 1.0, 0.2, 0);
                    DisplayBuilder.dustParticles(loc, 6, 0.5, r, g, b, 1.2f);
                }

                // Soul fire weaving at mid-height
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 12;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 1.5, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 4, 0.5, 0, 255, 200, 0.8f);
                }

                // Damage all players on brimstone (entire island is brimstone at this point)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    p.damage(16.0); // 8 hearts per pulse
                    DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5, r, g, b, 1.0f);
                }
            }

            // End stone flicker feint
            if (pulseTick % 10 == 5 && pulseTick < 60) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = 8 + Math.random() * 6;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 240, 235, 200, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TideOfConsumption(plugin); }
    }

    // =========================================================================
    // 57. CONSUMPTION SURGE -- sweeping beam converts terrain
    // =========================================================================
    public static class ConsumptionSurge extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private double sweepAngleStart;
        private boolean sweepStarted = false;

        public ConsumptionSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("consumption_surge", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(340); // 17 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepAngleStart = Math.random() * 2 * Math.PI;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.3f);

            // Warning: soul fire at beam origin
            DisplayBuilder.dustParticles(center, 15, 1.0, 0, 150, 255, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 0.8, 0, 150, 255, 1.0f);
                }
                return;
            }

            if (!sweepStarted) {
                sweepStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }

            int sweepTick = ticksAlive - 30;
            // 120 degrees over ~70 ticks = ~0.03 radians per tick
            double currentAngle = sweepAngleStart + sweepTick * 0.03;

            // Advance beam along sweep arc
            if (sweepTick % 2 == 0) {
                // Remove old beam displays
                for (BlockDisplayHandle h : beamHandles) {
                    h.entity().remove();
                }
                beamHandles.clear();

                // Create beam line from center outward at current angle
                for (int d = 1; d <= 15; d++) {
                    Location loc = center.clone().add(Math.cos(currentAngle) * d, 0.05, Math.sin(currentAngle) * d);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                    h.scale(0.6f, 0.1f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Trail particles
                    DisplayBuilder.dustParticles(loc, 3, 0.2, 200, 40, 30, 0.7f);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc, 1, 0.1, 0, 0.1, 0);
                }

                // Overhead crimson spore trail
                for (int d = 2; d <= 14; d += 3) {
                    Location loc = center.clone().add(Math.cos(currentAngle) * d, 1.5, Math.sin(currentAngle) * d);
                    DisplayBuilder.dustParticles(loc, 4, 0.3, 200, 40, 30, 0.6f);
                }
            }

            // Damage players in beam path
            if (sweepTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    // Check if player is in beam line
                    double dx = pl.getX() - center.getX();
                    double dz = pl.getZ() - center.getZ();
                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(playerAngle - currentAngle);
                    if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (angleDiff < 0.15 && dist > 1 && dist < 16) {
                        p.damage(20.0); // 10 hearts
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConsumptionSurge(plugin); }
    }

    // =========================================================================
    // 58. VOID SEEP -- dark patches spread from edges, slow + collision damage
    // =========================================================================
    public static class VoidSeep extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> seepHandles = new ArrayList<>();
        private final List<Location> seepOrigins = new ArrayList<>();
        private boolean seepVisible = false;

        public VoidSeep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_seep", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(380); // 19 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

            // 4 seep origins at island edges
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i + Math.random() * 0.3;
                seepOrigins.add(center.clone().add(Math.cos(angle) * 16, 0, Math.sin(angle) * 16));
            }

            // Soul particles from floor edges
            for (Location origin : seepOrigins) {
                DisplayBuilder.dustParticles(origin, 6, 1.0, 5, 255, 240, 0.6f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 10 == 0) {
                    for (Location origin : seepOrigins) {
                        DisplayBuilder.dustParticles(origin, 4, 1.0, 5, 255, 240, 0.5f);
                    }
                }
                return;
            }

            int seepTick = ticksAlive - 50;
            double seepRadius = Math.min(seepTick * 0.15, 8.0); // Spread inward at ~3 bps

            // Spawn seep visuals
            if (seepTick % 8 == 0 && !seepVisible) {
                seepVisible = true;
            }

            if (seepTick % 10 == 0 && seepRadius <= 8) {
                for (Location origin : seepOrigins) {
                    // Black patches spreading inward
                    double inwardAngle = Math.atan2(center.getZ() - origin.getZ(), center.getX() - origin.getX());
                    Location seepLoc = origin.clone().add(Math.cos(inwardAngle) * seepRadius, 0.01, Math.sin(inwardAngle) * seepRadius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(seepLoc, Material.BLACK_CONCRETE);
                    h.scale(1.5f, 0.02f, 1.5f).glow(0, 150, 255).interpolation(5, 0);
                    seepHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Teal ghost-glow particles
                    DisplayBuilder.dustParticles(seepLoc, 4, 0.5, 5, 255, 240, 0.6f);
                    DisplayBuilder.dustParticles(seepLoc, 3, 0.4, 0, 180, 150, 0.5f);
                }
            }

            // Slow players on void seep (velocity reduction, no status effects)
            if (seepTick % 4 == 0) {
                for (Location origin : seepOrigins) {
                    double inwardAngle = Math.atan2(center.getZ() - origin.getZ(), center.getX() - origin.getX());
                    for (double r = 0; r <= seepRadius; r += 2) {
                        Location seepZone = origin.clone().add(Math.cos(inwardAngle) * r, 0, Math.sin(inwardAngle) * r);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(seepZone) <= 2.25) {
                                // Slow via velocity reduction
                                org.bukkit.util.Vector vel = p.getVelocity();
                                p.setVelocity(vel.setX(vel.getX() * 0.6).setZ(vel.getZ() * 0.6));
                            }
                        }
                    }
                }
            }

            // Collision front damage (where seep meets brimstone)
            if (seepTick % 10 == 5) {
                for (Location origin : seepOrigins) {
                    double inwardAngle = Math.atan2(center.getZ() - origin.getZ(), center.getX() - origin.getX());
                    Location frontLoc = origin.clone().add(Math.cos(inwardAngle) * seepRadius, 0, Math.sin(inwardAngle) * seepRadius);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(frontLoc) <= 4.0) {
                            p.damage(18.0); // 9 hearts at collision front
                            w.spawnParticle(Particle.LAVA, p.getLocation(), 5, 0.3, 0.5, 0.3, 0);
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.4, 0, 150, 255, 1.0f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSeep(plugin); }
    }

    // =========================================================================
    // 59. BRIMSTONE BURIAL -- 5x5 bowl sinks and fills with lava
    // =========================================================================
    public static class BrimstoneBurial extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> bowlHandles = new ArrayList<>();
        private Location targetLoc;
        private boolean sunk = false;

        public BrimstoneBurial(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_burial", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(420); // 21 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target nearest player
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            targetLoc = (nearest != null) ? nearest.getLocation().clone() : center.clone();

            DisplayBuilder.playSound(targetLoc, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);
            DisplayBuilder.playSound(targetLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.6f);

            // Warning: crimson spore flash on 5x5 area
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    DisplayBuilder.dustParticles(targetLoc.clone().add(x, 0.1, z), 3, 0.2, 230, 50, 20, 0.6f);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            DisplayBuilder.dustParticles(targetLoc.clone().add(x, 0.1, z), 2, 0.2, 230, 50, 20, 0.5f);
                        }
                    }
                }
                return;
            }

            // Bowl sink
            if (!sunk) {
                sunk = true;
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);

                // Create bowl display blocks sinking
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location loc = targetLoc.clone().add(x, -0.5, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                        h.scale(0.95f, 0.1f, 0.95f).glow(255, 100, 0).interpolation(5, 0);
                        bowlHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Rim conversion blocks
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                            Location rimLoc = targetLoc.clone().add(x, 0.02, z);
                            BlockDisplayHandle rh = displayBuilder.spawnBlock(rimLoc, Material.CRIMSON_NYLIUM);
                            rh.scale(0.9f, 0.05f, 0.9f).glow(200, 0, 50).interpolation(5, 0);
                            spawnedEntities.add(rh.entity());
                        }
                    }
                }

                // Damage players in the bowl
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetLoc) <= 6.25) { // 2.5 block radius
                        p.damage(20.0); // 10 hearts
                        p.setVelocity(p.getVelocity().setY(-0.3)); // Pull down
                    }
                }
            }

            int sinkTick = ticksAlive - 40;

            // Lava + soul fire particles in the bowl
            if (sinkTick % 4 == 0) {
                w.spawnParticle(Particle.LAVA, targetLoc.clone().add(0, 0.3, 0), 8, 2.0, 0.5, 2.0, 0);
                w.spawnParticle(Particle.DRIPPING_LAVA, targetLoc.clone().add(0, 0.5, 0), 5, 2.0, 0, 2.0, 0);

                // Alternating flame and soul fire columns
                for (int c = 0; c < 4; c++) {
                    double colAngle = (Math.PI / 2) * c;
                    Location colLoc = targetLoc.clone().add(Math.cos(colAngle) * 1.5, 0.5 + Math.random() * 2, Math.sin(colAngle) * 1.5);
                    if (c % 2 == 0) {
                        DisplayBuilder.dustParticles(colLoc, 4, 0.3, 255, 100, 0, 0.8f);
                    } else {
                        DisplayBuilder.dustParticles(colLoc, 4, 0.3, 0, 150, 255, 0.8f);
                    }
                }
            }

            // Repeated lava damage in pit
            if (sinkTick % 20 == 0 && sinkTick > 10) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetLoc) <= 6.25) {
                        p.damage(6.0); // 3 hearts/sec
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBurial(plugin); }
    }

    // =========================================================================
    // 60. FINAL STONE -- last end stone patch converts to brimstone
    // =========================================================================
    public static class FinalStone extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> stoneHandles = new ArrayList<>();
        private final List<Location> tileLocations = new ArrayList<>();
        private int conversionIndex = 0;
        private boolean conversionStarted = false;

        public FinalStone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_stone", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(500); // 25 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Loudest sound so far
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);

            // Generate the "last patch" of tiles in a cluster
            Location patchCenter = center.clone().add(
                    (Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);

            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (x * x + z * z <= 12) { // Circular patch
                        Location loc = patchCenter.clone().add(x, 0.03, z);
                        tileLocations.add(loc);

                        // Show end stone tiles about to convert
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                        h.scale(0.95f, 0.08f, 0.95f).glow(200, 195, 180).interpolation(5, 0);
                        stoneHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 60 ticks (3 seconds) -- cracks appear
            if (ticksAlive <= 60) {
                if (ticksAlive % 6 == 0) {
                    for (Location loc : tileLocations) {
                        DisplayBuilder.dustParticles(loc, 2, 0.2, 140, 130, 120, 0.5f);
                    }
                    // Crimson spores seeping through cracks
                    if (ticksAlive % 12 == 0) {
                        for (Location loc : tileLocations) {
                            DisplayBuilder.dustParticles(loc.clone().add(0, 0.2, 0), 2, 0.1, 200, 30, 20, 0.4f);
                        }
                    }
                }
                return;
            }

            // Begin conversion
            if (!conversionStarted) {
                conversionStarted = true;
            }

            int convTick = ticksAlive - 60;

            // Convert tiles one by one from edges inward -- ~1 tile per 1-2 ticks
            if (convTick % 2 == 0 && conversionIndex < tileLocations.size()) {
                // Convert from outermost tiles first (reverse order since we added center-out)
                int idx = tileLocations.size() - 1 - conversionIndex;
                if (idx >= 0 && idx < tileLocations.size()) {
                    Location tileLoc = tileLocations.get(idx);

                    // Change display to magma
                    if (idx < stoneHandles.size()) {
                        stoneHandles.get(idx).glow(200, 0, 50);
                    }

                    // Lava burst particles
                    w.spawnParticle(Particle.LAVA, tileLoc.clone().add(0, 0.5, 0), 5, 0.3, 0.5, 0.3, 0);
                    DisplayBuilder.playSound(tileLoc, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);

                    // Damage players on converting tile
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tileLoc) <= 1.5) {
                            p.damage(16.0); // 8 hearts
                            DisplayBuilder.crimsonDust(p.getLocation(), 8, 0.4);
                        }
                    }

                    conversionIndex++;
                }
            }

            // Sky darkens
            if (convTick % 20 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 8, 0), 15, 8.0, 80, 30, 10, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalStone(plugin); }
    }
}
