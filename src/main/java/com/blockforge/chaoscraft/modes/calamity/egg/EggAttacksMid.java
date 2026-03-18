package com.blockforge.chaoscraft.modes.calamity.egg;

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
 * EGG ATTACKS 11-20 (MID)
 * Between Boss 2 and Boss 3 — faster, more complex compound patterns.
 * The egg's void energy intensifies. Multiple mechanics per attack, tighter
 * dodge windows, overlapping hazard zones.
 * Colors: deep purple (80,0,160), violet (128,0,255), dark crimson (120,0,30).
 * Damage range: 6.0 - 12.0 HP.
 * NO status effects. All attacks originate from the egg outward.
 */
public final class EggAttacksMid {

    private EggAttacksMid() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DoublePulseRings(plugin));
        registry.register(new CardinalBoltVolley(plugin));
        registry.register(new SpiralVoidTendril(plugin));
        registry.register(new EggCloneIllusions(plugin));
        registry.register(new PulsingDamageAura(plugin));
        registry.register(new VoidGeysers(plugin));
        registry.register(new ObsidianCageSnap(plugin));
        registry.register(new AmethystStormSpiral(plugin));
        registry.register(new TwinVoidLances(plugin));
        registry.register(new CrimsonNovaWave(plugin));
    }

    // =========================================================================
    // ATTACK 11 — Double Pulse Rings
    // Two expanding rings at different heights (ground level and head height).
    // The lower ring expands first, then the upper ring 1 second later.
    // Players must time jumps between both waves.
    // =========================================================================
    public static class DoublePulseRings extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> lowerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> upperRing = new ArrayList<>();
        private double lowerRadius = 1.0;
        private double upperRadius = 1.0;
        private boolean lowerSpawned = false;
        private boolean upperSpawned = false;

        public DoublePulseRings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_double_pulse_rings", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.3, 0), 12, 1.5);
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 2.5, 0), 12, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.3, 0), 6, 1.0);
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 2.5, 0), 6, 1.0);
                }
                return;
            }

            // Spawn lower ring (tick 20)
            if (ticksAlive == 20 && !lowerSpawned) {
                lowerSpawned = true;
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20;
                    Location loc = center.clone().add(Math.cos(angle), 0.1, Math.sin(angle));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 0.3f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                    lowerRing.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            }

            // Spawn upper ring (tick 40 — 1 second delay)
            if (ticksAlive == 40 && !upperSpawned) {
                upperSpawned = true;
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI * i) / 20;
                    Location loc = center.clone().add(Math.cos(angle), 2.5, Math.sin(angle));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.5f, 0.4f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    upperRing.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.6f);
            }

            // Expand lower ring — 0.35 blocks/tick
            if (lowerSpawned) {
                lowerRadius += 0.35;
                for (int i = 0; i < lowerRing.size(); i++) {
                    if (!lowerRing.get(i).entity().isValid()) continue;
                    double angle = (2 * Math.PI * i) / lowerRing.size();
                    lowerRing.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * lowerRadius, 0.1, Math.sin(angle) * lowerRadius));
                }
            }

            // Expand upper ring — 0.35 blocks/tick (starts 20 ticks later)
            if (upperSpawned) {
                upperRadius += 0.35;
                for (int i = 0; i < upperRing.size(); i++) {
                    if (!upperRing.get(i).entity().isValid()) continue;
                    double angle = (2 * Math.PI * i) / upperRing.size();
                    upperRing.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * upperRadius, 2.5, Math.sin(angle) * upperRadius));
                }
            }

            // Ring particles
            if (ticksAlive % 4 == 0) {
                if (lowerSpawned) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), lowerRadius,
                        Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.0f));
                }
                if (upperSpawned) {
                    DisplayBuilder.particleRing(center.clone().add(0, 2.7, 0), upperRadius,
                        Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
                }
            }

            // Damage players hit by either ring
            if (ticksAlive % 4 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double pDist = player.getLocation().toVector().setY(0).distance(center.toVector().setY(0));
                    double py = player.getLocation().getY();
                    // Lower ring hits grounded players
                    if (lowerSpawned && Math.abs(pDist - lowerRadius) < 2.0 && py < center.getY() + 1.5) {
                        triggerImpactDamage(player.getLocation());
                    }
                    // Upper ring hits standing/jumping players
                    if (upperSpawned && Math.abs(pDist - upperRadius) < 2.0 && py >= center.getY() + 1.0 && py < center.getY() + 4.0) {
                        triggerImpactDamage(player.getLocation());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            lowerRing.clear();
            upperRing.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new DoublePulseRings(plugin); }
    }

    // =========================================================================
    // ATTACK 12 — Cardinal Bolt Volley
    // 4 crying obsidian bolts fire simultaneously in N/S/E/W directions from
    // the egg. Each bolt is faster than the single bolt, and a second volley
    // fires at 45-degree offsets 1 second later.
    // =========================================================================
    public static class CardinalBoltVolley extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private final List<double[]> boltDirs = new ArrayList<>();
        private boolean firstVolley = false;
        private boolean secondVolley = false;
        private int secondVolleyTick = 0;

        public CardinalBoltVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_cardinal_bolt_volley", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flash 4 cardinal directions
            double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] dir : dirs) {
                DisplayBuilder.dustParticles(center.clone().add(dir[0] * 2, 1.5, dir[1] * 2),
                    5, 0.3, 120, 0, 30, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-15 ticks)
            if (ticksAlive < 15) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 8, 0.5);
                }
                return;
            }

            // First volley — 4 cardinal bolts (tick 15)
            if (ticksAlive == 15 && !firstVolley) {
                firstVolley = true;
                double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (double[] dir : dirs) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(dir[0] * 1.5, 1.5, dir[1] * 1.5), Material.CRYING_OBSIDIAN);
                    h.scale(0.35f, 0.35f, 0.7f).glow(80, 0, 160).interpolation(1, 0);
                    boltHandles.add(h);
                    spawnedEntities.add(h.entity());
                    boltDirs.add(dir);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.2f, 0.6f);
            }

            // Second volley — 4 diagonal bolts (tick 35)
            if (ticksAlive == 35 && !secondVolley) {
                secondVolley = true;
                secondVolleyTick = ticksAlive;
                double diag = 0.707; // cos(45)
                double[][] dirs = {{diag, diag}, {diag, -diag}, {-diag, diag}, {-diag, -diag}};
                for (double[] dir : dirs) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(dir[0] * 1.5, 1.5, dir[1] * 1.5), Material.OBSIDIAN);
                    h.scale(0.3f, 0.3f, 0.6f).glow(128, 0, 255).interpolation(1, 0);
                    boltHandles.add(h);
                    spawnedEntities.add(h.entity());
                    boltDirs.add(dir);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.2f, 0.9f);

                // Diagonal telegraph flash
                for (double[] dir : dirs) {
                    DisplayBuilder.dustParticles(center.clone().add(dir[0] * 3, 1.5, dir[1] * 3),
                        4, 0.3, 128, 0, 255, 1.0f);
                }
            }

            // Move all bolts — 0.7 blocks/tick
            for (int i = 0; i < boltHandles.size(); i++) {
                BlockDisplayHandle h = boltHandles.get(i);
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                double[] dir = boltDirs.get(i);
                h.entity().teleport(loc.clone().add(dir[0] * 0.7, 0, dir[1] * 0.7));

                // Trail particles every 3 ticks
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.darkPurpleDust(h.entity().getLocation(), 2, 0.2);
                }
            }

            // Damage check
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < boltHandles.size(); i++) {
                    if (!boltHandles.get(i).entity().isValid()) continue;
                    Location boltLoc = boltHandles.get(i).entity().getLocation();
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(boltLoc) < 6.25) { // 2.5^2
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            boltHandles.clear();
            boltDirs.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new CardinalBoltVolley(plugin); }
    }

    // =========================================================================
    // ATTACK 13 — Spiral Void Tendril
    // A single arm of obsidian blocks spirals outward from the egg in a tight
    // spiral pattern, sweeping the ground. Faster rotation than the early arms,
    // with the spiral getting wider over time. 2 full rotations.
    // =========================================================================
    public static class SpiralVoidTendril extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private double spiralAngle = 0;
        private double spiralRadius = 1.5;
        private boolean tendrilActive = false;

        public SpiralVoidTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_spiral_void_tendril", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(7.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning spiral particle preview
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double r = 1.5 + i * 0.3;
                DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r),
                    2, 0.2, 128, 0, 255, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 6, 1.0);
                }
                return;
            }

            // Spawn tendril (tick 20)
            if (ticksAlive == 20 && !tendrilActive) {
                tendrilActive = true;
                // 12-segment tendril
                for (int i = 0; i < 12; i++) {
                    double angle = spiralAngle + (i * 0.15);
                    double r = spiralRadius + i * 0.8;
                    Location loc = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                    Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.4f, 0.6f, 0.4f).glow(80, 0, 160).interpolation(1, 0);
                    tendrilHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
            }

            if (!tendrilActive) return;

            // Rotate and expand spiral — 0.08 radians/tick, radius grows slowly
            spiralAngle += 0.08;
            spiralRadius += 0.04;

            for (int i = 0; i < tendrilHandles.size(); i++) {
                if (!tendrilHandles.get(i).entity().isValid()) continue;
                double segAngle = spiralAngle + (i * 0.15);
                double r = spiralRadius + i * 0.8;
                Location loc = center.clone().add(Math.cos(segAngle) * r, 0.5, Math.sin(segAngle) * r);
                tendrilHandles.get(i).entity().teleport(loc);

                // Segment rotation
                BlockDisplay bd = tendrilHandles.get(i).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.2f, -0.3f, -0.2f),
                    new AxisAngle4f((float)(spiralAngle * 2 + i), 0, 1, 0),
                    new Vector3f(0.4f, 0.6f, 0.4f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDuration(2);
                bd.setInterpolationDelay(0);
            }

            // Trail particles on tip
            if (ticksAlive % 3 == 0 && !tendrilHandles.isEmpty()) {
                BlockDisplayHandle tip = tendrilHandles.get(tendrilHandles.size() - 1);
                if (tip.entity().isValid()) {
                    DisplayBuilder.purpleDust(tip.entity().getLocation(), 4, 0.4);
                }
            }
        }

        @Override
        protected void onCleanup() {
            tendrilHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new SpiralVoidTendril(plugin); }
    }

    // =========================================================================
    // ATTACK 14 — Egg Clone Illusions
    // 3 fake egg BlockDisplays materialize around the arena, each a smaller
    // dark copy of the real egg. After 2.5 seconds they explode in a burst
    // of void particles and obsidian shrapnel dealing damage.
    // =========================================================================
    public static class EggCloneIllusions extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cloneHandles = new ArrayList<>();
        private final List<Location> cloneLocations = new ArrayList<>();
        private boolean clonesSpawned = false;
        private boolean clonesExploded = false;

        public EggCloneIllusions(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_clone_illusions", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 random positions 8-14 blocks from egg
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 / 3) * i + Math.random() * 0.5;
                double dist = 8 + Math.random() * 6;
                cloneLocations.add(center.clone().add(
                    Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist));
            }
            // Warning shimmer at clone positions
            for (Location loc : cloneLocations) {
                DisplayBuilder.dustParticles(loc, 6, 1.0, 128, 0, 255, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn clones (tick 15)
            if (ticksAlive == 15 && !clonesSpawned) {
                clonesSpawned = true;
                for (Location loc : cloneLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DRAGON_EGG);
                    h.scale(0.6f, 0.6f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                    cloneHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
            }

            // Clones vibrate and pulse (15-65 ticks = 2.5 seconds)
            if (clonesSpawned && !clonesExploded && ticksAlive > 15 && ticksAlive < 65) {
                float intensity = (ticksAlive - 15) / 50.0f; // 0 to 1
                for (int i = 0; i < cloneHandles.size(); i++) {
                    if (!cloneHandles.get(i).entity().isValid()) continue;
                    Location base = cloneLocations.get(i);
                    float shake = intensity * 0.15f;
                    float sx = (float)(Math.sin(ticksAlive * 0.5 + i) * shake);
                    float sz = (float)(Math.cos(ticksAlive * 0.7 + i) * shake);
                    cloneHandles.get(i).entity().teleport(base.clone().add(sx, 0, sz));

                    // Pulsing scale
                    float scale = 0.6f + (float)(Math.sin(ticksAlive * 0.2) * 0.1 * intensity);
                    float rot = (float)(ticksAlive * 0.05);
                    cloneHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    cloneHandles.get(i).entity().setInterpolationDuration(2);
                    cloneHandles.get(i).entity().setInterpolationDelay(0);
                }

                // Warning particles intensify
                if (ticksAlive % (int) Math.max(2, 8 - intensity * 6) == 0) {
                    for (Location loc : cloneLocations) {
                        DisplayBuilder.dustParticles(loc, (int)(4 + intensity * 8), 1.5,
                            128, 0, 255, 1.0f + intensity);
                    }
                }

                // Warning sounds get faster
                if (ticksAlive % (int) Math.max(4, 15 - intensity * 10) == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f,
                        0.5f + intensity * 1.0f);
                }
            }

            // Clones explode (tick 65)
            if (ticksAlive == 65 && !clonesExploded) {
                clonesExploded = true;
                for (Location loc : cloneLocations) {
                    triggerImpactDamage(loc);
                    DisplayBuilder.dustParticles(loc, 30, 3.0, 80, 0, 160, 2.0f);
                    DisplayBuilder.dustParticles(loc, 20, 2.0, 120, 0, 30, 1.5f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() {
            cloneHandles.clear();
            cloneLocations.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new EggCloneIllusions(plugin); }
    }

    // =========================================================================
    // ATTACK 15 — Pulsing Damage Aura
    // A dome of purple stained glass grows outward from the egg, starting small
    // and expanding to 12-block radius over 5 seconds. Players inside take
    // ticking damage. The dome pulses visually (scale oscillates). Must flee.
    // =========================================================================
    public static class PulsingDamageAura extends BlockDisplayAttack {
        private BlockDisplayHandle domeHandle;
        private final List<BlockDisplayHandle> edgeHandles = new ArrayList<>();
        private float auraRadius = 1.0f;
        private boolean domeActive = false;

        public PulsingDamageAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_pulsing_damage_aura", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(6.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.darkPurpleDust(center, 15, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center, 8, 1.5);
                }
                return;
            }

            // Spawn dome core (tick 20)
            if (ticksAlive == 20 && !domeActive) {
                domeActive = true;
                domeHandle = displayBuilder.spawnBlock(center, Material.PURPLE_STAINED_GLASS);
                domeHandle.scale(2.0f, 2.0f, 2.0f).glow(128, 0, 255).interpolation(4, 0);
                spawnedEntities.add(domeHandle.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
            }

            if (!domeActive || domeHandle == null) return;

            // Grow aura radius — 0.1 per tick, max 12
            auraRadius = Math.min(12.0f, auraRadius + 0.1f);

            // Pulse oscillation
            float pulse = (float)(Math.sin(ticksAlive * 0.15) * 0.5);
            float displayScale = auraRadius + pulse;
            float halfScale = displayScale / 2;

            domeHandle.entity().setTransformation(new Transformation(
                new Vector3f(-halfScale, -halfScale, -halfScale),
                new AxisAngle4f((float)(ticksAlive * 0.02), 0, 1, 0),
                new Vector3f(displayScale, displayScale, displayScale),
                new AxisAngle4f(0, 0, 1, 0)
            ));
            domeHandle.entity().setInterpolationDuration(3);
            domeHandle.entity().setInterpolationDelay(0);

            // Edge ring particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), auraRadius,
                    Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                DisplayBuilder.particleRing(center.clone().add(0, 2.0, 0), auraRadius * 0.8,
                    Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
            }

            // Sound pulse
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f,
                    0.4f + auraRadius * 0.05f);
            }

            // Damage players inside radius
            if (ticksAlive % 10 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) < auraRadius * auraRadius) {
                        triggerImpactDamage(player.getLocation());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            domeHandle = null;
            edgeHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new PulsingDamageAura(plugin); }
    }

    // =========================================================================
    // ATTACK 16 — Void Geysers
    // 5 ground fissures erupt around the egg in sequence (one every 0.5s).
    // Each geyser shoots a pillar of obsidian 6 blocks high, then collapses.
    // Positions form a pentagon around the egg.
    // =========================================================================
    public static class VoidGeysers extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> geyserHandles = new ArrayList<>();
        private final List<Location> geyserLocations = new ArrayList<>();
        private final boolean[] geyserFired = new boolean[5];
        private final boolean[] geyserCollapsed = new boolean[5];

        public VoidGeysers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_void_geysers", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pentagon positions at radius 7
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                geyserLocations.add(center.clone().add(Math.cos(angle) * 7, 0.05, Math.sin(angle) * 7));
            }
            // Warning cracks at all positions
            for (Location loc : geyserLocations) {
                DisplayBuilder.dustParticles(loc, 5, 0.5, 120, 0, 30, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fire geysers in sequence — one every 10 ticks (0.5s), starting at tick 20
            for (int i = 0; i < 5; i++) {
                int fireTick = 20 + i * 10;
                int collapseTick = fireTick + 40;

                // Warning intensifies before each geyser
                if (ticksAlive == fireTick - 10) {
                    DisplayBuilder.dustParticles(geyserLocations.get(i), 8, 0.8, 80, 0, 160, 1.2f);
                    DisplayBuilder.playSound(geyserLocations.get(i), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.3f);
                }

                // Fire geyser
                if (ticksAlive == fireTick && !geyserFired[i]) {
                    geyserFired[i] = true;
                    Location base = geyserLocations.get(i);
                    for (int y = 0; y < 6; y++) {
                        Material mat = (y % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(0, y, 0), mat);
                        h.scale(0.6f, 1.0f, 0.6f).glow(80, 0, 160).interpolation(2, 0);
                        geyserHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    triggerImpactDamage(base);
                    DisplayBuilder.dustParticles(base, 15, 1.5, 128, 0, 255, 1.5f);
                    DisplayBuilder.playSound(base, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.6f);
                }

                // Geyser ambient particles
                if (geyserFired[i] && !geyserCollapsed[i] && ticksAlive % 6 == 0) {
                    Location top = geyserLocations.get(i).clone().add(0, 6, 0);
                    DisplayBuilder.purpleDust(top, 4, 0.5);
                }

                // Collapse geyser
                if (ticksAlive == collapseTick && geyserFired[i] && !geyserCollapsed[i]) {
                    geyserCollapsed[i] = true;
                    DisplayBuilder.dustParticles(geyserLocations.get(i), 10, 2.0, 80, 0, 160, 1.0f);
                    DisplayBuilder.playSound(geyserLocations.get(i), Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            geyserHandles.clear();
            geyserLocations.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new VoidGeysers(plugin); }
    }

    // =========================================================================
    // ATTACK 17 — Obsidian Cage Snap
    // 4 obsidian walls rise around a random area (6x6), forming a cage. After
    // 2 seconds, the ceiling slams down. Players inside take heavy damage;
    // players must escape before the ceiling closes.
    // =========================================================================
    public static class ObsidianCageSnap extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private BlockDisplayHandle ceilingHandle;
        private Location cageCenter;
        private boolean wallsRaised = false;
        private boolean ceilingSlammed = false;

        public ObsidianCageSnap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_obsidian_cage_snap", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(440);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random cage center 6-12 blocks from egg
            double angle = Math.random() * Math.PI * 2;
            double dist = 6 + Math.random() * 6;
            cageCenter = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Warning outline on ground
            double half = 3.0;
            DisplayBuilder.particleLine(
                cageCenter.clone().add(-half, 0.2, -half),
                cageCenter.clone().add(half, 0.2, -half),
                Particle.DUST, 4, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.0f));
            DisplayBuilder.particleLine(
                cageCenter.clone().add(half, 0.2, -half),
                cageCenter.clone().add(half, 0.2, half),
                Particle.DUST, 4, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.0f));
            DisplayBuilder.particleLine(
                cageCenter.clone().add(half, 0.2, half),
                cageCenter.clone().add(-half, 0.2, half),
                Particle.DUST, 4, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.0f));
            DisplayBuilder.particleLine(
                cageCenter.clone().add(-half, 0.2, half),
                cageCenter.clone().add(-half, 0.2, -half),
                Particle.DUST, 4, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.0f));
            DisplayBuilder.playSound(cageCenter, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;
            if (cageCenter == null) return;

            // Walls rise (tick 20-35)
            if (ticksAlive == 20 && !wallsRaised) {
                wallsRaised = true;
                double half = 3.0;
                // 4 walls — each is a flat obsidian display
                // North wall
                BlockDisplayHandle north = displayBuilder.spawnBlock(
                    cageCenter.clone().add(0, 0, -half), Material.OBSIDIAN);
                north.scale(6.0f, 4.0f, 0.3f).glow(80, 0, 160).interpolation(10, 0);
                wallHandles.add(north);
                spawnedEntities.add(north.entity());

                // South wall
                BlockDisplayHandle south = displayBuilder.spawnBlock(
                    cageCenter.clone().add(0, 0, half), Material.OBSIDIAN);
                south.scale(6.0f, 4.0f, 0.3f).glow(80, 0, 160).interpolation(10, 0);
                wallHandles.add(south);
                spawnedEntities.add(south.entity());

                // East wall
                BlockDisplayHandle east = displayBuilder.spawnBlock(
                    cageCenter.clone().add(half, 0, 0), Material.OBSIDIAN);
                east.scale(0.3f, 4.0f, 6.0f).glow(80, 0, 160).interpolation(10, 0);
                wallHandles.add(east);
                spawnedEntities.add(east.entity());

                // West wall
                BlockDisplayHandle west = displayBuilder.spawnBlock(
                    cageCenter.clone().add(-half, 0, 0), Material.OBSIDIAN);
                west.scale(0.3f, 4.0f, 6.0f).glow(80, 0, 160).interpolation(10, 0);
                wallHandles.add(west);
                spawnedEntities.add(west.entity());

                DisplayBuilder.playSound(cageCenter, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.4f);
            }

            // Warning: ceiling forming (tick 50-60)
            if (ticksAlive >= 50 && ticksAlive < 60 && !ceilingSlammed) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(cageCenter.clone().add(0, 4, 0), 10, 2.5, 120, 0, 30, 1.2f);
                    DisplayBuilder.playSound(cageCenter, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.3f + (ticksAlive - 50) * 0.05f);
                }
            }

            // Ceiling slams (tick 60)
            if (ticksAlive == 60 && !ceilingSlammed) {
                ceilingSlammed = true;
                ceilingHandle = displayBuilder.spawnBlock(
                    cageCenter.clone().add(0, 4, 0), Material.CRYING_OBSIDIAN);
                ceilingHandle.scale(6.0f, 0.5f, 6.0f).glow(120, 0, 30).interpolation(2, 0);
                spawnedEntities.add(ceilingHandle.entity());

                // Slam down animation
                ceilingHandle.animateTo(
                    new Vector3f(-3.0f, 0, -3.0f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(6.0f, 0.5f, 6.0f), 4);

                // Damage inside cage
                triggerImpactDamage(cageCenter);
                DisplayBuilder.dustParticles(cageCenter, 25, 3.0, 80, 0, 160, 2.0f);
                DisplayBuilder.playSound(cageCenter, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            wallHandles.clear();
            ceilingHandle = null;
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new ObsidianCageSnap(plugin); }
    }

    // =========================================================================
    // ATTACK 18 — Amethyst Storm Spiral
    // 8 amethyst blocks orbit the egg in a tight spiral, then release outward
    // as projectiles in a pinwheel pattern. The spiral winds up for 2 seconds,
    // building visual tension, then fires all at once.
    // =========================================================================
    public static class AmethystStormSpiral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private double windupAngle = 0;
        private boolean shardsFired = false;
        private int fireStartTick = 0;

        public AmethystStormSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_amethyst_storm_spiral", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(9.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn 8 amethyst shards close to egg
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2, 1.5, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                shardHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Windup spiral — orbit accelerates (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !shardsFired) {
                // Accelerating rotation
                double speed = 0.03 + (ticksAlive / 40.0) * 0.12;
                windupAngle += speed;

                // Tighten radius as windup progresses
                double radius = 2.0 - (ticksAlive / 40.0) * 0.8;

                for (int i = 0; i < shardHandles.size(); i++) {
                    if (!shardHandles.get(i).entity().isValid()) continue;
                    double angle = windupAngle + (2 * Math.PI * i) / 8;
                    Location loc = center.clone().add(
                        Math.cos(angle) * radius, 1.5, Math.sin(angle) * radius);
                    shardHandles.get(i).entity().teleport(loc);

                    float rot = (float)(windupAngle * 3 + i);
                    shardHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.25f, -0.15f),
                        new AxisAngle4f(rot, 0.3f, 1.0f, 0.5f),
                        new Vector3f(0.3f, 0.5f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    shardHandles.get(i).entity().setInterpolationDuration(1);
                    shardHandles.get(i).entity().setInterpolationDelay(0);
                }

                // Windup sound
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.6f,
                        0.5f + (ticksAlive / 40.0f) * 1.0f);
                }
            }

            // Fire shards outward (tick 40)
            if (ticksAlive == 40 && !shardsFired) {
                shardsFired = true;
                fireStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.5f, 0.5f);
                DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 15, 1.5);
            }

            // Shards fly outward in pinwheel (40-80 ticks)
            if (shardsFired && ticksAlive - fireStartTick < 40) {
                double dist = (ticksAlive - fireStartTick) * 0.8;
                for (int i = 0; i < shardHandles.size(); i++) {
                    if (!shardHandles.get(i).entity().isValid()) continue;
                    // Each shard keeps its release angle + slight curve
                    double angle = windupAngle + (2 * Math.PI * i) / 8 + (ticksAlive - fireStartTick) * 0.02;
                    Location loc = center.clone().add(
                        Math.cos(angle) * (1.2 + dist), 1.5, Math.sin(angle) * (1.2 + dist));
                    shardHandles.get(i).entity().teleport(loc);
                }

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    for (BlockDisplayHandle h : shardHandles) {
                        if (h.entity().isValid()) {
                            DisplayBuilder.dustParticles(h.entity().getLocation(), 2, 0.2, 128, 0, 255, 0.8f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            shardHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new AmethystStormSpiral(plugin); }
    }

    // =========================================================================
    // ATTACK 19 — Twin Void Lances
    // Two parallel beams of obsidian extend from the egg in opposite directions,
    // then sweep 90 degrees like clock hands. The beams are 12 blocks long,
    // at waist height. Faster than early Void Arms, covering more area.
    // =========================================================================
    public static class TwinVoidLances extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> lanceHandles = new ArrayList<>();
        private double sweepAngle = 0;
        private double startAngle;
        private boolean lancesSpawned = false;

        public TwinVoidLances(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_twin_void_lances", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(9.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            startAngle = Math.random() * Math.PI * 2;
            // Telegraph: two lines of particles
            for (int i = 0; i < 2; i++) {
                double angle = startAngle + i * Math.PI;
                for (int seg = 1; seg <= 6; seg++) {
                    DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(angle) * seg * 2, 1.2, Math.sin(angle) * seg * 2),
                        3, 0.3, 128, 0, 255, 0.8f);
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.2, 0), 6, 0.5);
                }
                return;
            }

            // Spawn lances (tick 20)
            if (!lancesSpawned) {
                lancesSpawned = true;
                for (int arm = 0; arm < 2; arm++) {
                    double armAngle = startAngle + arm * Math.PI;
                    for (int seg = 1; seg <= 12; seg++) {
                        Location loc = center.clone().add(
                            Math.cos(armAngle) * seg, 1.2, Math.sin(armAngle) * seg);
                        Material mat = (seg % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.3f, 0.5f, 0.3f).glow(80, 0, 160).interpolation(1, 0);
                        lanceHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
            }

            // Sweep 90 degrees over ~80 ticks — 0.02 radians/tick
            sweepAngle += 0.02;

            for (int i = 0; i < lanceHandles.size(); i++) {
                if (!lanceHandles.get(i).entity().isValid()) continue;
                int arm = i / 12;
                int seg = (i % 12) + 1;
                double armAngle = startAngle + arm * Math.PI + sweepAngle;
                Location loc = center.clone().add(
                    Math.cos(armAngle) * seg, 1.2, Math.sin(armAngle) * seg);
                lanceHandles.get(i).entity().teleport(loc);
            }

            // Tip particles
            if (ticksAlive % 3 == 0) {
                for (int arm = 0; arm < 2; arm++) {
                    double tipAngle = startAngle + arm * Math.PI + sweepAngle;
                    Location tip = center.clone().add(Math.cos(tipAngle) * 12, 1.2, Math.sin(tipAngle) * 12);
                    DisplayBuilder.darkPurpleDust(tip, 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            lanceHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new TwinVoidLances(plugin); }
    }

    // =========================================================================
    // ATTACK 20 — Crimson Nova Wave
    // The egg charges with dark crimson energy, then releases a massive flat
    // expanding ring of black concrete and crying obsidian that covers the
    // entire arena floor. Leaves a 3-second lingering damage zone. Players
    // must find gaps (the ring has 4 missing segments to dodge through).
    // =========================================================================
    public static class CrimsonNovaWave extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> novaHandles = new ArrayList<>();
        private double novaRadius = 2.0;
        private boolean novaFired = false;
        private final int[] gapIndices = new int[4]; // 4 gaps in the ring

        public CrimsonNovaWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_crimson_nova_wave", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-compute 4 random gap positions out of 24 ring segments
            for (int i = 0; i < 4; i++) {
                gapIndices[i] = (int)(Math.random() * 24);
            }
            // Crimson warning pulse
            DisplayBuilder.dustParticles(center, 20, 2.0, 120, 0, 30, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Charge-up telegraph (0-35 ticks)
            if (ticksAlive < 35) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, (int)(5 + ticksAlive * 0.3), 1.5,
                        120, 0, 30, 1.0f + ticksAlive * 0.03f);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f,
                        0.3f + ticksAlive * 0.02f);
                }
                return;
            }

            // Fire nova (tick 35)
            if (ticksAlive == 35 && !novaFired) {
                novaFired = true;
                // 24 ring segments (skip gap indices)
                for (int i = 0; i < 24; i++) {
                    boolean isGap = false;
                    for (int g : gapIndices) {
                        if (i == g) { isGap = true; break; }
                    }
                    if (isGap) {
                        novaHandles.add(null); // placeholder for gap
                        continue;
                    }
                    double angle = (2 * Math.PI * i) / 24;
                    Location loc = center.clone().add(
                        Math.cos(angle) * novaRadius, 0.1, Math.sin(angle) * novaRadius);
                    Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.BLACK_CONCRETE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.8f, 0.2f, 0.8f).glow(120, 0, 30).interpolation(2, 0);
                    novaHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.8f);
            }

            if (!novaFired) return;

            // Expand nova ring — 0.4 blocks/tick
            novaRadius += 0.4;
            for (int i = 0; i < novaHandles.size(); i++) {
                BlockDisplayHandle h = novaHandles.get(i);
                if (h == null || !h.entity().isValid()) continue;
                double angle = (2 * Math.PI * i) / novaHandles.size();
                h.entity().teleport(center.clone().add(
                    Math.cos(angle) * novaRadius, 0.1, Math.sin(angle) * novaRadius));
            }

            // Crimson edge particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), novaRadius,
                    Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.5f));
            }

            // Damage players the wave passes (not in gaps)
            if (ticksAlive % 4 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double pDist = player.getLocation().toVector().setY(0)
                        .distance(center.toVector().setY(0));
                    if (Math.abs(pDist - novaRadius) < 2.0 && player.getLocation().getY() < center.getY() + 2.0) {
                        // Check if player is in a gap
                        double playerAngle = Math.atan2(
                            player.getLocation().getZ() - center.getZ(),
                            player.getLocation().getX() - center.getX());
                        if (playerAngle < 0) playerAngle += Math.PI * 2;
                        int segment = (int)((playerAngle / (Math.PI * 2)) * 24) % 24;
                        boolean inGap = false;
                        for (int g : gapIndices) {
                            if (segment == g) { inGap = true; break; }
                        }
                        if (!inGap) {
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            novaHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new CrimsonNovaWave(plugin); }
    }
}
