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
 * Phase 3 Environmental — GROUP 5: BRIMSTONE SKY EVENTS
 * 10 attacks (41-50) themed around aerial bombardment, meteors, fire rain, sky hazards.
 * Calamity Dweller (Boss 3) — boss3-dweller.md
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson glow (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Materials: NETHERRACK, MAGMA_BLOCK, BASALT, SMOOTH_BASALT, OBSIDIAN
 * - Damage in HP (4.0 - 12.0 range)
 * - Impact = triggerImpactDamage()
 */
public final class BrimstoneSky {

    private BrimstoneSky() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EmberDrop(plugin));
        registry.register(new FireRainBurst(plugin));
        registry.register(new BrimstoneMeteor(plugin));
        registry.register(new HellfireColumn(plugin));
        registry.register(new RainingBasaltShards(plugin));
        registry.register(new SkyCrack(plugin));
        registry.register(new AshCloudDescent(plugin));
        registry.register(new BrimstoneHail(plugin));
        registry.register(new SkyBomb(plugin));
        registry.register(new Hellstorm(plugin));
    }

    // =========================================================================
    // ATTACK 41 — Ember Drop
    // 8 fireballs descend from sky in staggered pairs. Landing zones marked.
    // 5 hearts per fireball impact, 2-block radius.
    // =========================================================================
    public static class EmberDrop extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> landingZones = new ArrayList<>();
        private final List<Integer> dropTicks = new ArrayList<>();
        private int dropsLanded = 0;

        public EmberDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_drop", AttackType.ENVIRONMENTAL, 3));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(2.0);
            config.setDurationTicks(140); // 4s release + 1.5s fall per pair
            config.setCooldownTicks(360); // 18s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 8 random landing zones across the arena
            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 2.0 + Math.random() * 8.0;
                landingZones.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
                dropTicks.add(20 + (i / 2) * 20); // Pairs every 1s
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.2f);
            // Sky flame density
            DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 15, 5.0, 240, 120, 40, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Process fireball drops
            for (int i = 0; i < landingZones.size(); i++) {
                int scheduledTick = dropTicks.get(i);

                // Landing indicator: 30 ticks (1.5s) before impact
                if (ticksAlive >= scheduledTick - 30 && ticksAlive < scheduledTick) {
                    if (ticksAlive % 6 == 0) {
                        Location landing = landingZones.get(i);
                        DisplayBuilder.dustParticles(landing.clone().add(0, 0.1, 0), 4, 1.0, 220, 80, 10, 0.6f);
                    }
                }

                // Fireball descent visual
                if (ticksAlive >= scheduledTick - 20 && ticksAlive < scheduledTick) {
                    int fallTick = ticksAlive - (scheduledTick - 20);
                    double fallY = 25.0 - (fallTick * 1.25); // Descend from 25 blocks
                    Location landing = landingZones.get(i);
                    Location fireballPos = landing.clone().add(0, fallY, 0);
                    DisplayBuilder.dustParticles(fireballPos, 6, 0.5, 230, 95, 18, 1.0f);
                    DisplayBuilder.dustParticles(fireballPos.clone().add(0, 1, 0), 3, 0.3, 85, 75, 65, 0.6f);
                }

                // Impact
                if (ticksAlive == scheduledTick) {
                    Location landing = landingZones.get(i);
                    DisplayBuilder.playSound(landing, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.0f);
                    DisplayBuilder.dustParticles(landing, 15, 1.5, 255, 140, 0, 1.2f);

                    // Fire patch display
                    BlockDisplayHandle h = displayBuilder.spawnBlock(landing, Material.MAGMA_BLOCK);
                    h.scale(1.5f, 0.1f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());

                    // Impact damage
                    triggerImpactDamage(landing);
                    dropsLanded++;
                }
            }

            // Ambient sky fire during event
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(
                        (Math.random() - 0.5) * 16, 15 + Math.random() * 10, (Math.random() - 0.5) * 16),
                        3, 1.0, 240, 120, 40, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EmberDrop(plugin); }
    }

    // =========================================================================
    // ATTACK 42 — Fire Rain Burst
    // Dense fire particle rain with 20 real projectiles hidden in visual noise.
    // 4 hearts per real projectile hit.
    // =========================================================================
    public static class FireRainBurst extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> realProjectiles = new ArrayList<>();
        private final List<Integer> projectileTicks = new ArrayList<>();

        public FireRainBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_rain_burst", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 1s warn + 4s rain
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 20 real projectile positions and timings
            for (int i = 0; i < 20; i++) {
                double ox = (Math.random() - 0.5) * 18.0;
                double oz = (Math.random() - 0.5) * 18.0;
                realProjectiles.add(center.clone().add(ox, 0, oz));
                projectileTicks.add(20 + (int)(Math.random() * 80)); // Spread over 4s
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1s)
            if (ticksAlive < 20) {
                // Sky saturation begins
                DisplayBuilder.dustParticles(center.clone().add(0, 18, 0), 20, 8.0, 240, 120, 40, 0.8f);
                return;
            }

            // Dense cosmetic fire rain (visual noise)
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double rx = center.getX() + (Math.random() - 0.5) * 20;
                    double rz = center.getZ() + (Math.random() - 0.5) * 20;
                    double ry = center.getY() + 10 + Math.random() * 10;
                    DisplayBuilder.dustParticles(new Location(w, rx, ry, rz), 2, 0.5, 240, 120, 40, 0.4f);
                }
            }

            // Real projectile impacts
            for (int i = 0; i < realProjectiles.size(); i++) {
                if (ticksAlive == projectileTicks.get(i)) {
                    Location landing = realProjectiles.get(i);
                    // Lava burst on impact
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.3, 0), 8, 0.5, 230, 95, 18, 0.8f);
                    DisplayBuilder.playSound(landing, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 1.2f);

                    // Damage in 1.5-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 2.25) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }

            // Maximum volume ambient fire
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireRainBurst(plugin); }
    }

    // =========================================================================
    // ATTACK 43 — Brimstone Meteor
    // Single massive 2x2 meteor descends over 3s. Huge impact explosion.
    // 9 hearts direct (2-block), 5 hearts splash (6-block), 2 hearts/s lava crater.
    // =========================================================================
    public static class BrimstoneMeteor extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location targetLoc;
        private boolean impacted = false;

        public BrimstoneMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_meteor", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(460); // 3s descent + 5s debris + 15s crater
            config.setCooldownTicks(700); // 35s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Target nearest player's position at fire time
            Player nearest = null;
            double closestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < closestDist) {
                    closestDist = dist;
                    nearest = p;
                }
            }
            targetLoc = nearest != null ? nearest.getLocation().clone() : center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: 60 ticks (3s)
            if (ticksAlive < 60) {
                double fallY = 30.0 - (ticksAlive * 0.5);
                Location meteorPos = targetLoc.clone().add(0, fallY, 0);

                // Meteor display
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(meteorPos, 10, 1.0, 235, 105, 22, 1.5f);
                    DisplayBuilder.dustParticles(meteorPos.clone().add(0, 2, 0), 5, 0.5, 255, 140, 30, 1.0f);
                    // Smoke trail
                    DisplayBuilder.dustParticles(meteorPos.clone().add(0, 3, 0), 4, 0.8, 85, 75, 65, 0.8f);
                }

                // Growing landing circle
                if (ticksAlive % 8 == 0) {
                    double circleSize = (ticksAlive / 60.0) * 3.0;
                    DisplayBuilder.particleRing(targetLoc, circleSize, Particle.DUST, 10,
                            new Particle.DustOptions(Color.fromRGB(220, 80, 10), 0.8f));
                }

                // Spawn meteor block displays
                if (ticksAlive == 10) {
                    for (int x = 0; x <= 1; x++) {
                        for (int z = 0; z <= 1; z++) {
                            Location mLoc = targetLoc.clone().add(x, 30, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(mLoc, Material.NETHERRACK);
                            h.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(1, 0);
                            handles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                        BlockDisplayHandle mh = displayBuilder.spawnBlock(targetLoc.clone().add(0, 30.5, 0), Material.MAGMA_BLOCK);
                        mh.scale(1.5f, 1.5f, 1.5f).glow(255, 100, 0).interpolation(1, 0);
                        handles.add(mh);
                        spawnedEntities.add(mh.entity());
                    }
                }

                // Move meteor blocks downward
                if (ticksAlive % 2 == 0) {
                    for (BlockDisplayHandle h : handles) {
                        Location loc = h.entity().getLocation();
                        loc.setY(loc.getY() - 1.0);
                        h.entity().teleport(loc);
                    }
                }
                return;
            }

            // Impact
            if (!impacted) {
                impacted = true;
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                DisplayBuilder.dustParticles(targetLoc, 40, 3.0, 230, 95, 18, 2.0f);
                DisplayBuilder.dustParticles(targetLoc, 25, 3.0, 255, 140, 30, 1.5f);
                DisplayBuilder.dustParticles(targetLoc, 15, 2.0, 85, 75, 65, 1.0f);

                // Direct hit damage (2-block radius)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(targetLoc);
                    if (distSq <= 4.0) {
                        p.damage(12.0); // ~6 hearts (scaled from 9)
                    } else if (distSq <= 36.0) {
                        p.damage(10.0); // 5 hearts splash
                    }
                }

                // Spawn crater
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x * x + z * z > 1) continue;
                        BlockDisplayHandle ch = displayBuilder.spawnBlock(
                                targetLoc.clone().add(x, -0.2, z), Material.MAGMA_BLOCK);
                        ch.scale(1.0f, 0.2f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                        handles.add(ch);
                        spawnedEntities.add(ch.entity());
                    }
                }
            }

            // Debris cloud damage (5s)
            int impactTick = ticksAlive - 60;
            if (impactTick > 0 && impactTick <= 100 && impactTick % 15 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetLoc) <= 36.0) {
                        p.damage(4.0); // Contact damage in debris
                    }
                }
                DisplayBuilder.dustParticles(targetLoc.clone().add(0, 2, 0), 8, 3.0, 85, 75, 65, 0.6f);
            }

            // Lava crater damage (15s after debris clears)
            if (impactTick > 100 && impactTick <= 400 && impactTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetLoc) <= 4.0) {
                        p.damage(4.0); // 2 hearts/s
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneMeteor(plugin); }
    }

    // =========================================================================
    // ATTACK 44 — Hellfire Column
    // 3-block-wide fire column descends from sky onto player. No floor indicator.
    // 6 hearts/s in column. Fires 3 times with 3s gaps.
    // =========================================================================
    public static class HellfireColumn extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int columnsFired = 0;
        private Location currentTarget;

        public HellfireColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_column", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260); // 3 columns with 3s gaps + 4s persistence each
            config.setCooldownTicks(500); // 25s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No visual spawn — audio-only warning
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fire columns at tick 10, 70, 130 (3s gaps)
            int[] fireTicks = {10, 70, 130};
            for (int fireTick : fireTicks) {
                // Audio warning 0.5s before
                if (ticksAlive == fireTick - 10) {
                    // Target nearest player
                    Player nearest = null;
                    double closestDist = Double.MAX_VALUE;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist < closestDist) {
                            closestDist = dist;
                            nearest = p;
                        }
                    }
                    currentTarget = nearest != null ? nearest.getLocation().clone() : center.clone();
                    DisplayBuilder.playSound(currentTarget, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
                }

                // Column appears
                if (ticksAlive == fireTick && currentTarget != null) {
                    // 20-block tall, 3 blocks wide fire column
                    for (int y = 0; y < 20; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                currentTarget.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        h.scale(3.0f, 2.0f, 3.0f).glow(255, 100, 0).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.playSound(currentTarget, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                    DisplayBuilder.dustParticles(currentTarget.clone().add(0, 10, 0), 30, 5.0, 255, 140, 0, 1.5f);
                    columnsFired++;
                }

                // Column active damage zone
                if (ticksAlive >= fireTick && ticksAlive < fireTick + 80 && ticksAlive % 5 == 0 && currentTarget != null) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - currentTarget.getX();
                        double dz = p.getLocation().getZ() - currentTarget.getZ();
                        if (dx * dx + dz * dz <= 2.25) { // 1.5-block radius
                            p.damage(6.0); // ~6 hearts/s at 5-tick interval
                        }
                    }
                    // Column particles
                    if (ticksAlive % 8 == 0) {
                        int yRand = (int)(Math.random() * 20);
                        DisplayBuilder.dustParticles(currentTarget.clone().add(0, yRand, 0),
                                4, 1.0, 240, 120, 40, 0.8f);
                        // Ash particles falling within column
                        DisplayBuilder.dustParticles(currentTarget.clone().add(0, yRand, 0),
                                2, 0.5, 190, 175, 155, 0.5f);
                    }
                }

                // Floor persistence after column fades
                if (ticksAlive == fireTick + 60 && currentTarget != null) {
                    BlockDisplayHandle floorH = displayBuilder.spawnBlock(currentTarget, Material.MAGMA_BLOCK);
                    floorH.scale(3.0f, 0.15f, 3.0f).glow(255, 100, 0).interpolation(3, 0);
                    handles.add(floorH);
                    spawnedEntities.add(floorH.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HellfireColumn(plugin); }
    }

    // =========================================================================
    // ATTACK 45 — Raining Basalt Shards
    // 15 angled basalt shards rain from NW to SE over 6 seconds.
    // 5 hearts per shard impact, 1-block landing zone.
    // =========================================================================
    public static class RainingBasaltShards extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> shardLandings = new ArrayList<>();
        private final List<Integer> shardTicks = new ArrayList<>();

        public RainingBasaltShards(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("raining_basalt_shards", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(420); // 6s rain + 15s debris
            config.setCooldownTicks(480); // 24s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 15 shard landings (biased toward west/northwest)
            for (int i = 0; i < 15; i++) {
                double ox = (Math.random() - 0.3) * 16.0; // Biased eastward landing
                double oz = (Math.random() - 0.3) * 16.0; // Biased southward landing
                shardLandings.add(center.clone().add(ox, 0, oz));
                shardTicks.add(20 + (int)(Math.random() * 120)); // Over 6s
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < shardLandings.size(); i++) {
                int scheduledTick = shardTicks.get(i);

                // ASH trail visible 1s before landing
                if (ticksAlive >= scheduledTick - 20 && ticksAlive < scheduledTick) {
                    int trailTick = ticksAlive - (scheduledTick - 20);
                    double trailY = 15.0 - trailTick * 0.75;
                    // NW origin angle offset
                    double trailOffsetX = (15.0 - trailTick * 0.75) * (-0.3);
                    double trailOffsetZ = (15.0 - trailTick * 0.75) * (-0.3);
                    Location trailLoc = shardLandings.get(i).clone().add(trailOffsetX, trailY, trailOffsetZ);
                    DisplayBuilder.dustParticles(trailLoc, 3, 0.3, 190, 175, 155, 0.6f);
                    DisplayBuilder.dustParticles(trailLoc, 2, 0.2, 80, 70, 60, 0.4f);
                }

                // Impact
                if (ticksAlive == scheduledTick) {
                    Location landing = shardLandings.get(i);
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.1, 0), 4, 0.3, 220, 80, 10, 0.5f);
                    DisplayBuilder.playSound(landing, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.6f);

                    // Spawn debris block
                    BlockDisplayHandle h = displayBuilder.spawnBlock(landing, Material.BASALT);
                    h.scale(0.6f, 0.8f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());

                    // Damage at landing
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 1.0) {
                            p.damage(10.0); // 5 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RainingBasaltShards(plugin); }
    }

    // =========================================================================
    // ATTACK 46 — Sky Crack
    // Visible crack in sky, dripping lava curtain with 10 hidden projectiles.
    // 5 hearts per real projectile.
    // =========================================================================
    public static class SkyCrack extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> projectiles = new ArrayList<>();
        private final List<Integer> projectileTicks = new ArrayList<>();
        private double crackAngle;
        private boolean crackFormed = false;

        public SkyCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_crack", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220); // 3s form + 6s active + 2s closing
            config.setCooldownTicks(560); // 28s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            crackAngle = Math.random() * Math.PI;
            // Pre-calculate 10 real projectiles under the crack
            for (int i = 0; i < 10; i++) {
                double offset = (i - 5) * 2.0;
                double perpOffset = (Math.random() - 0.5) * 3.0;
                double perpAngle = crackAngle + Math.PI / 2;
                projectiles.add(center.clone().add(
                        Math.cos(crackAngle) * offset + Math.cos(perpAngle) * perpOffset, 0,
                        Math.sin(crackAngle) * offset + Math.sin(perpAngle) * perpOffset));
                projectileTicks.add(80 + (int)(Math.random() * 100)); // During active phase
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crack formation: 60 ticks (3s) — jagged lava line in sky
            if (ticksAlive < 60) {
                int segments = (int)((ticksAlive / 60.0) * 10);
                for (int s = 0; s <= segments; s++) {
                    double offset = (s - 5) * 2.0;
                    Location crackLoc = center.clone().add(
                            Math.cos(crackAngle) * offset, 18 + Math.random() * 2,
                            Math.sin(crackAngle) * offset);
                    if (ticksAlive % 6 == 0) {
                        DisplayBuilder.dustParticles(crackLoc, 3, 0.5, 230, 95, 18, 0.8f);
                    }
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.2f);
                }
                return;
            }

            // Crack formed — spawn display blocks along crack
            if (!crackFormed) {
                crackFormed = true;
                for (int s = -5; s <= 5; s++) {
                    double offset = s * 2.0;
                    Location crackLoc = center.clone().add(
                            Math.cos(crackAngle) * offset, 18,
                            Math.sin(crackAngle) * offset);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(crackLoc, Material.MAGMA_BLOCK);
                    h.scale(1.5f, 0.3f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);
            }

            // Active phase: dripping lava curtain + hidden real projectiles
            if (ticksAlive >= 60 && ticksAlive < 180) {
                // Visual lava drips (cosmetic)
                if (ticksAlive % 3 == 0) {
                    double offset = (Math.random() - 0.5) * 20;
                    Location dripLoc = center.clone().add(
                            Math.cos(crackAngle) * offset, 15 - Math.random() * 5,
                            Math.sin(crackAngle) * offset);
                    DisplayBuilder.dustParticles(dripLoc, 2, 0.3, 220, 80, 10, 0.4f);
                }

                // Real projectile impacts
                for (int i = 0; i < projectiles.size(); i++) {
                    if (ticksAlive == projectileTicks.get(i)) {
                        Location landing = projectiles.get(i);
                        DisplayBuilder.dustParticles(landing.clone().add(0, 0.2, 0), 6, 0.4, 230, 95, 18, 0.8f);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(landing) <= 2.25) {
                                p.damage(10.0); // 5 hearts
                            }
                        }
                    }
                }
            }

            // Crack closing: 180-220
            if (ticksAlive >= 180) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 18, 0), 6, 3.0, 85, 75, 65, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyCrack(plugin); }
    }

    // =========================================================================
    // ATTACK 47 — Ash Cloud Descent
    // Dense ash cloud descends to 4 blocks above floor, reducing visibility.
    // No direct damage — visibility hazard. 10s coverage.
    // =========================================================================
    public static class AshCloudDescent extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double cloudHeight = 20;

        public AshCloudDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_cloud_descent", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(360); // 5s descent + 10s coverage + 3s ascent
            config.setCooldownTicks(600); // 30s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Cloud visible at altitude
            DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 30, 8.0, 195, 178, 158, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: 100 ticks (5s) — from 20 to 4
            if (ticksAlive < 100) {
                cloudHeight = 20.0 - (ticksAlive / 100.0) * 16.0;
            }
            // Coverage: ticks 100-300 (10s) — at 4 blocks
            else if (ticksAlive < 300) {
                cloudHeight = 4.0;
            }
            // Ascent: ticks 300-360 (3s) — from 4 to 20
            else {
                cloudHeight = 4.0 + ((ticksAlive - 300) / 60.0) * 16.0;
            }

            // Cloud visual: dense ash particles at cloud height
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double rx = (Math.random() - 0.5) * 20;
                    double rz = (Math.random() - 0.5) * 20;
                    DisplayBuilder.dustParticles(
                            center.clone().add(rx, cloudHeight + Math.random() * 2, rz),
                            3, 1.0, 195, 178, 158, 0.8f);
                    DisplayBuilder.dustParticles(
                            center.clone().add(rx, cloudHeight + Math.random() * 2, rz),
                            2, 0.8, 80, 70, 60, 0.6f);
                }
            }

            // Flame flickers within cloud during coverage
            if (cloudHeight <= 6 && ticksAlive % 8 == 0) {
                double rx = (Math.random() - 0.5) * 16;
                double rz = (Math.random() - 0.5) * 16;
                DisplayBuilder.dustParticles(
                        center.clone().add(rx, cloudHeight - 1 + Math.random() * 2, rz),
                        2, 0.3, 240, 120, 40, 0.5f);
            }

            // Random directional ambient sounds during coverage
            if (cloudHeight <= 6 && ticksAlive % 20 == 0) {
                double rx = (Math.random() - 0.5) * 12;
                double rz = (Math.random() - 0.5) * 12;
                DisplayBuilder.playSound(center.clone().add(rx, 2, rz),
                        Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.6f + (float)(Math.random() * 0.4));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AshCloudDescent(plugin); }
    }

    // =========================================================================
    // ATTACK 48 — Brimstone Hail
    // 30 micro-projectiles blanket the arena over 5 seconds.
    // 3 hearts per micro-projectile. Mobile players dodge better.
    // =========================================================================
    public static class BrimstoneHail extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> hailLandings = new ArrayList<>();
        private final List<Integer> hailTicks = new ArrayList<>();

        public BrimstoneHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_hail", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(130); // 1.5s warn + 5s hail
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 30 micro-projectile landings
            for (int i = 0; i < 30; i++) {
                double ox = (Math.random() - 0.5) * 18.0;
                double oz = (Math.random() - 0.5) * 18.0;
                hailLandings.add(center.clone().add(ox, 0, oz));
                hailTicks.add(30 + (int)(Math.random() * 100)); // Over 5s
            }
            // Floor flicker warning
            DisplayBuilder.dustParticles(center, 20, 8.0, 220, 80, 10, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 8.0, 220, 80, 10, 0.4f);
                }
                return;
            }

            // Ambient sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
            }

            // Process hail impacts
            for (int i = 0; i < hailLandings.size(); i++) {
                if (ticksAlive == hailTicks.get(i)) {
                    Location landing = hailLandings.get(i);
                    // Small particle cluster on impact
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.2, 0), 3, 0.3, 225, 90, 15, 0.5f);
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.2, 0), 2, 0.2, 245, 125, 35, 0.4f);

                    // Damage in 1-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 1.0) {
                            p.damage(6.0); // 3 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneHail(plugin); }
    }

    // =========================================================================
    // ATTACK 49 — Sky Bomb
    // Massive lava sphere forms 30 blocks up, targeting beam sweeps, then detonates.
    // 10 hearts center (2-block), 7 hearts splash (6-block), 4 hearts outer (12-block).
    // =========================================================================
    public static class SkyBomb extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean detonated = false;
        private double beamAngle = 0;
        private Location detonationCenter;

        public SkyBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_bomb", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 4s formation + 3s beam + instant detonation
            config.setCooldownTicks(900); // 45s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location skyPos = center.clone().add(0, 30, 0);

            // Formation: 80 ticks (4s) — growing sphere
            if (ticksAlive < 80) {
                float size = ticksAlive / 80.0f * 4.0f;
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(skyPos, (int)(size * 4), size * 0.5, 230, 95, 18, 1.2f);
                    DisplayBuilder.dustParticles(skyPos, (int)(size * 2), size * 0.3, 255, 140, 30, 0.8f);
                }

                // Spawn sphere display blocks at intervals
                if (ticksAlive == 20 || ticksAlive == 40 || ticksAlive == 60) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(skyPos, Material.MAGMA_BLOCK);
                    h.scale(size, size, size).glow(255, 100, 0).interpolation(10, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Building sound
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(skyPos, Sound.ENTITY_RAVAGER_ROAR,
                            0.3f + size * 0.2f, 0.3f + ticksAlive * 0.005f);
                }
                return;
            }

            // Targeting beam: 60 ticks (3s) — sweeps slowly across arena
            if (ticksAlive < 140) {
                beamAngle += 0.04; // Slow sweep
                double beamX = Math.cos(beamAngle) * 8;
                double beamZ = Math.sin(beamAngle) * 8;
                detonationCenter = center.clone().add(beamX, 0, beamZ);

                // Beam visual — particle line from sky to floor
                if (ticksAlive % 2 == 0) {
                    for (int y = 0; y < 30; y += 2) {
                        double t = y / 30.0;
                        Location beamLoc = center.clone().add(beamX * t, 30 - y, beamZ * t);
                        DisplayBuilder.dustParticles(beamLoc, 2, 0.3, 255, 140, 30, 0.8f);
                    }
                    // Floor target point
                    DisplayBuilder.dustParticles(detonationCenter.clone().add(0, 0.2, 0),
                            5, 0.5, 255, 140, 30, 1.0f);
                }

                // Building scream
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(skyPos, Sound.ENTITY_RAVAGER_ROAR,
                            0.8f + (ticksAlive - 80) * 0.01f, 0.5f);
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;
                if (detonationCenter == null) detonationCenter = center.clone();

                DisplayBuilder.playSound(detonationCenter, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.dustParticles(detonationCenter, 50, 6.0, 230, 95, 18, 2.0f);
                DisplayBuilder.dustParticles(detonationCenter, 30, 4.0, 255, 140, 30, 1.5f);
                DisplayBuilder.dustParticles(detonationCenter, 20, 3.0, 85, 75, 65, 1.2f);

                // Tiered damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(detonationCenter);
                    if (distSq <= 4.0) {
                        p.damage(12.0); // ~10 hearts scaled
                    } else if (distSq <= 36.0) {
                        p.damage(10.0); // ~7 hearts splash
                    } else if (distSq <= 144.0) {
                        p.damage(8.0); // 4 hearts outer
                    }
                }

                // Remove sky sphere displays
                for (BlockDisplayHandle h : handles) {
                    h.entity().remove();
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyBomb(plugin); }
    }

    // =========================================================================
    // ATTACK 50 — Hellstorm
    // Culmination event: all sky effects simultaneously. 6 seconds of chaos.
    // Fire rain + 3 Ember Drop volleys + Ash Cloud. Once per fight at 52% HP.
    // Variable damage: 10-25 hearts without mitigation.
    // =========================================================================
    public static class Hellstorm extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> realProjectiles = new ArrayList<>();
        private final List<Integer> projectileTicks = new ArrayList<>();
        private final List<Location> emberLandings = new ArrayList<>();
        private final List<Integer> emberTicks = new ArrayList<>();
        private boolean ashCloudActive = false;

        public Hellstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellstorm", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180); // 3s ramp + 6s storm
            config.setCooldownTicks(1200); // 60s — once per fight
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-calculate 15 fire rain projectiles
            for (int i = 0; i < 15; i++) {
                double ox = (Math.random() - 0.5) * 18.0;
                double oz = (Math.random() - 0.5) * 18.0;
                realProjectiles.add(center.clone().add(ox, 0, oz));
                projectileTicks.add(60 + (int)(Math.random() * 120));
            }

            // Pre-calculate 24 ember drop fireballs (3 volleys of 8)
            for (int volley = 0; volley < 3; volley++) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = 2.0 + Math.random() * 8.0;
                    emberLandings.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
                    emberTicks.add(60 + volley * 40 + (int)(Math.random() * 20));
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ramp-up: 60 ticks (3s) — sky saturates
            if (ticksAlive < 60) {
                float intensity = ticksAlive / 60.0f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(
                            center.clone().add(0, 20, 0),
                            (int)(10 * intensity), 8.0 * intensity, 240, 120, 40, 0.6f);
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT,
                            0.5f + intensity * 0.5f, 0.5f);
                }
                return;
            }

            // STORM PHASE: everything fires simultaneously

            // Dense cosmetic fire particles (maximum visual chaos)
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double rx = center.getX() + (Math.random() - 0.5) * 22;
                    double rz = center.getZ() + (Math.random() - 0.5) * 22;
                    double ry = center.getY() + Math.random() * 25;
                    int particleType = (int)(Math.random() * 4);
                    switch (particleType) {
                        case 0: DisplayBuilder.dustParticles(new Location(w, rx, ry, rz), 2, 0.5, 195, 178, 158, 0.5f); break;
                        case 1: DisplayBuilder.dustParticles(new Location(w, rx, ry, rz), 2, 0.5, 255, 135, 30, 0.6f); break;
                        case 2: DisplayBuilder.dustParticles(new Location(w, rx, ry, rz), 2, 0.5, 235, 105, 22, 0.5f); break;
                        default: DisplayBuilder.dustParticles(new Location(w, rx, ry, rz), 2, 0.5, 85, 75, 65, 0.4f); break;
                    }
                }
            }

            // Fire rain projectile impacts
            for (int i = 0; i < realProjectiles.size(); i++) {
                if (ticksAlive == projectileTicks.get(i)) {
                    Location landing = realProjectiles.get(i);
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.3, 0), 5, 0.4, 230, 95, 18, 0.7f);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 2.25) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }

            // Ember drop impacts
            for (int i = 0; i < emberLandings.size(); i++) {
                if (ticksAlive == emberTicks.get(i)) {
                    Location landing = emberLandings.get(i);
                    DisplayBuilder.dustParticles(landing, 10, 1.0, 255, 140, 0, 1.0f);
                    DisplayBuilder.playSound(landing, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.0f);

                    BlockDisplayHandle h = displayBuilder.spawnBlock(landing, Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.1f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 4.0) {
                            p.damage(10.0); // 5 hearts
                        }
                    }
                }
            }

            // Ash cloud in middle 4 seconds (ticks 80-160)
            if (ticksAlive >= 80 && ticksAlive < 160) {
                if (!ashCloudActive) {
                    ashCloudActive = true;
                }
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double rx = (Math.random() - 0.5) * 20;
                        double rz = (Math.random() - 0.5) * 20;
                        DisplayBuilder.dustParticles(
                                center.clone().add(rx, 3 + Math.random() * 2, rz),
                                3, 1.0, 195, 178, 158, 0.8f);
                    }
                }
            }

            // Wall of sound
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
            }

            // Abrupt end at tick 180 — 1 second silence handled by cleanup
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Hellstorm(plugin); }
    }
}
