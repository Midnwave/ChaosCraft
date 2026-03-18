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
 * EGG ATTACKS 1-10 (EARLY)
 * Between Boss 1 and Boss 2 — slow, telegraphed, simple patterns.
 * The charging egg pulses with void energy, sending out shockwaves and bolts.
 * Colors: deep purple (80,0,160), violet (128,0,255), dark crimson (120,0,30).
 * Damage range: 4.0 - 8.0 HP.
 * NO status effects. All attacks originate from the egg outward.
 */
public final class EggAttacksEarly {

    private EggAttacksEarly() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidPulseRing(plugin));
        registry.register(new ObsidianBolt(plugin));
        registry.register(new PurpleShockwave(plugin));
        registry.register(new SlowVoidArms(plugin));
        registry.register(new EggTremor(plugin));
        registry.register(new CryingMeteor(plugin));
        registry.register(new VoidDrip(plugin));
        registry.register(new AmethystBurst(plugin));
        registry.register(new DarkPulsar(plugin));
        registry.register(new EndstoneRain(plugin));
    }

    // =========================================================================
    // ATTACK 1 — Void Pulse Ring
    // A single ring of purple stained glass expands outward from the egg along
    // the ground. Slow expansion, clear telegraph (particles first), damages on
    // contact as it passes through players.
    // =========================================================================
    public static class VoidPulseRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean ringSpawned = false;
        private double currentRadius = 1.0;

        public VoidPulseRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_void_pulse_ring", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph: purple dust swirl at egg base for 1.5 seconds
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.3, 0), 15, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph phase (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.3, 0), 8, 1.0);
                }
                return;
            }

            // Spawn ring on first active tick
            if (!ringSpawned) {
                ringSpawned = true;
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI * i) / 16;
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.1, z), Material.PURPLE_STAINED_GLASS);
                    h.scale(0.6f, 0.3f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                    ringHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.5f);
            }

            // Expand ring outward — slow 0.25 blocks/tick
            currentRadius += 0.25;
            for (int i = 0; i < ringHandles.size(); i++) {
                if (!ringHandles.get(i).entity().isValid()) continue;
                double angle = (2 * Math.PI * i) / ringHandles.size();
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                ringHandles.get(i).entity().teleport(center.clone().add(x, 0.1, z));
            }

            // Ring particles every 4 ticks
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), currentRadius,
                    Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
            }

            // Damage players the ring passes through
            if (ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().toVector().setY(0)
                        .distance(center.toVector().setY(0));
                    if (Math.abs(dist - currentRadius) < 2.0) {
                        triggerImpactDamage(player.getLocation());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            ringHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new VoidPulseRing(plugin); }
    }

    // =========================================================================
    // ATTACK 2 — Obsidian Bolt
    // A single crying obsidian bolt fires from the egg in a random horizontal
    // direction. Slow-moving, glowing trail, impacts ground with small burst.
    // =========================================================================
    public static class ObsidianBolt extends BlockDisplayAttack {
        private BlockDisplayHandle boltHandle;
        private double dirX, dirZ;
        private boolean boltFired = false;
        private double boltX, boltZ;

        public ObsidianBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_obsidian_bolt", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random horizontal direction
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            boltX = center.getX();
            boltZ = center.getZ();
            // Telegraph: single bright pulse at egg
            DisplayBuilder.dustParticles(center.clone().add(dirX * 2, 1.5, dirZ * 2),
                8, 0.3, 120, 0, 30, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.2f);
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
                    DisplayBuilder.dustParticles(center.clone().add(dirX * 2, 1.5, dirZ * 2),
                        5, 0.2, 120, 0, 30, 1.2f);
                }
                return;
            }

            // Fire bolt
            if (!boltFired) {
                boltFired = true;
                Location boltLoc = center.clone().add(dirX * 2, 1.5, dirZ * 2);
                boltHandle = displayBuilder.spawnBlock(boltLoc, Material.CRYING_OBSIDIAN);
                boltHandle.scale(0.4f, 0.4f, 0.8f).glow(80, 0, 160).interpolation(1, 0);
                spawnedEntities.add(boltHandle.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.2f, 0.7f);
            }

            if (boltHandle == null || !boltHandle.entity().isValid()) return;

            // Move bolt forward — 0.5 blocks/tick (slow)
            boltX += dirX * 0.5;
            boltZ += dirZ * 0.5;
            Location newLoc = new Location(w, boltX, center.getY() + 1.5, boltZ);
            boltHandle.entity().teleport(newLoc);

            // Trail particles
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.darkPurpleDust(newLoc, 3, 0.3);
            }

            // Impact check — traveled 20+ blocks
            double traveled = Math.sqrt(Math.pow(boltX - center.getX(), 2) +
                Math.pow(boltZ - center.getZ(), 2));
            if (traveled > 20) {
                triggerImpactDamage(newLoc);
                DisplayBuilder.dustParticles(newLoc, 20, 2.0, 80, 0, 160, 1.5f);
                DisplayBuilder.playSound(newLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new ObsidianBolt(plugin); }
    }

    // =========================================================================
    // ATTACK 3 — Purple Shockwave
    // A flat disc of obsidian expands along the ground from the egg. Players
    // must jump over it. Slow expansion, clearly visible dark purple.
    // =========================================================================
    public static class PurpleShockwave extends BlockDisplayAttack {
        private BlockDisplayHandle discHandle;
        private float currentScale = 1.0f;

        public PurpleShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_purple_shockwave", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(5.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground tremor warning
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.2, 0), 12, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-25 ticks)
            if (ticksAlive < 25) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 0.2, 0), 6, 1.5);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.3f, 0.4f);
                }
                return;
            }

            // Spawn disc
            if (discHandle == null) {
                discHandle = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.OBSIDIAN);
                discHandle.scale(1.0f, 0.1f, 1.0f).glow(80, 0, 160).interpolation(3, 0);
                spawnedEntities.add(discHandle.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.6f);
            }

            // Expand disc — slow growth
            currentScale += 0.3f;
            float halfScale = currentScale / 2;
            discHandle.entity().setTransformation(new Transformation(
                new Vector3f(-halfScale, 0, -halfScale),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(currentScale, 0.1f, currentScale),
                new AxisAngle4f(0, 0, 1, 0)
            ));
            discHandle.entity().setInterpolationDuration(3);
            discHandle.entity().setInterpolationDelay(0);

            // Edge particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), currentScale / 2,
                    Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.2f));
            }

            // Damage players on the ground within disc radius (jumping avoids)
            if (ticksAlive % 5 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dist = ploc.toVector().setY(0).distance(center.toVector().setY(0));
                    if (dist < currentScale / 2 && ploc.getY() < center.getY() + 1.5) {
                        triggerImpactDamage(ploc);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            discHandle = null;
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new PurpleShockwave(plugin); }
    }

    // =========================================================================
    // ATTACK 4 — Slow Void Arms
    // Two long arms of crying obsidian rotate slowly around the egg at waist
    // height. Players must walk between or jump over. Full 360 rotation.
    // =========================================================================
    public static class SlowVoidArms extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> armHandles = new ArrayList<>();
        private double rotationAngle = 0;
        private boolean armsSpawned = false;

        public SlowVoidArms(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_slow_void_arms", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: two purple lines flash at initial arm positions
            for (int arm = 0; arm < 2; arm++) {
                double angle = arm * Math.PI;
                for (int i = 1; i <= 8; i++) {
                    Location loc = center.clone().add(
                        Math.cos(angle) * i * 1.2, 1.0, Math.sin(angle) * i * 1.2);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 128, 0, 255, 1.0f);
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-30 ticks)
            if (ticksAlive < 30) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.0, 0), 6, 1.0);
                }
                return;
            }

            // Spawn arms
            if (!armsSpawned) {
                armsSpawned = true;
                for (int arm = 0; arm < 2; arm++) {
                    double baseAngle = arm * Math.PI;
                    for (int i = 1; i <= 8; i++) {
                        Location loc = center.clone().add(
                            Math.cos(baseAngle) * i * 1.2, 1.0, Math.sin(baseAngle) * i * 1.2);
                        Material mat = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.5f, 0.8f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                        armHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
            }

            // Rotate arms — slow: 0.03 radians/tick (~6 seconds per revolution)
            rotationAngle += 0.03;
            for (int i = 0; i < armHandles.size(); i++) {
                if (!armHandles.get(i).entity().isValid()) continue;
                int arm = i / 8;
                int seg = (i % 8) + 1;
                double baseAngle = arm * Math.PI + rotationAngle;
                double x = Math.cos(baseAngle) * seg * 1.2;
                double z = Math.sin(baseAngle) * seg * 1.2;
                armHandles.get(i).entity().teleport(center.clone().add(x, 1.0, z));

                // Gentle spin on each segment
                BlockDisplay bd = armHandles.get(i).entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.25f, -0.4f, -0.25f),
                    new AxisAngle4f((float)(rotationAngle * 2), 0, 1, 0),
                    new Vector3f(0.5f, 0.8f, 0.5f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDuration(2);
                bd.setInterpolationDelay(0);
            }

            // Tip particles
            if (ticksAlive % 4 == 0) {
                for (int arm = 0; arm < 2; arm++) {
                    double tipAngle = arm * Math.PI + rotationAngle;
                    Location tip = center.clone().add(
                        Math.cos(tipAngle) * 9.6, 1.0, Math.sin(tipAngle) * 9.6);
                    DisplayBuilder.purpleDust(tip, 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            armHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new SlowVoidArms(plugin); }
    }

    // =========================================================================
    // ATTACK 5 — Egg Tremor
    // Ground blocks around the egg visually shake and erupt. Obsidian blocks
    // pop up from the ground in a random spread, hover briefly, then slam down
    // creating small impact zones.
    // =========================================================================
    public static class EggTremor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tremorHandles = new ArrayList<>();
        private final List<Location> tremorLocations = new ArrayList<>();
        private boolean tremorsSpawned = false;
        private boolean slammed = false;

        public EggTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_tremor", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate 6 random positions around egg
            for (int i = 0; i < 6; i++) {
                double ox = (Math.random() - 0.5) * 14;
                double oz = (Math.random() - 0.5) * 14;
                tremorLocations.add(center.clone().add(ox, 0.05, oz));
            }
            // Ground crack particles
            for (Location loc : tremorLocations) {
                DisplayBuilder.dustParticles(loc, 5, 0.5, 80, 0, 160, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph — cracks widen (0-40 ticks)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    for (Location loc : tremorLocations) {
                        DisplayBuilder.dustParticles(loc, 4, 0.8, 120, 0, 30, 1.0f);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.3f);
                }
                return;
            }

            // Blocks erupt upward (tick 40)
            if (!tremorsSpawned) {
                tremorsSpawned = true;
                for (Location loc : tremorLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.8f, 0.8f, 0.8f).glow(80, 0, 160).interpolation(3, 0);
                    tremorHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
            }

            // Blocks rise (40-60 ticks)
            if (ticksAlive >= 40 && ticksAlive < 60 && !slammed) {
                float rise = (ticksAlive - 40) * 0.2f;
                for (int i = 0; i < tremorHandles.size(); i++) {
                    if (!tremorHandles.get(i).entity().isValid()) continue;
                    Location base = tremorLocations.get(i);
                    tremorHandles.get(i).entity().teleport(base.clone().add(0, rise, 0));
                }
            }

            // Blocks slam down (tick 60)
            if (ticksAlive == 60 && !slammed) {
                slammed = true;
                for (int i = 0; i < tremorHandles.size(); i++) {
                    if (!tremorHandles.get(i).entity().isValid()) continue;
                    Location base = tremorLocations.get(i);
                    tremorHandles.get(i).entity().teleport(base);
                    triggerImpactDamage(base);
                    DisplayBuilder.dustParticles(base, 12, 1.5, 80, 0, 160, 1.5f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            tremorHandles.clear();
            tremorLocations.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new EggTremor(plugin); }
    }

    // =========================================================================
    // ATTACK 6 — Crying Meteor
    // A single crying obsidian meteor rises from the egg, arcs upward, then
    // slams into a random location nearby. Clear upward telegraph, slow arc.
    // =========================================================================
    public static class CryingMeteor extends BlockDisplayAttack {
        private BlockDisplayHandle meteorHandle;
        private double targetX, targetZ;
        private boolean launched = false;
        private boolean impacted = false;

        public CryingMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_crying_meteor", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(140);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random target 8-15 blocks from egg
            double angle = Math.random() * Math.PI * 2;
            double dist = 8 + Math.random() * 7;
            targetX = center.getX() + Math.cos(angle) * dist;
            targetZ = center.getZ() + Math.sin(angle) * dist;

            // Upward streak particles
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 10, 0.5, 128, 0, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
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
                    DisplayBuilder.purpleDust(center.clone().add(0, 2 + ticksAlive * 0.2, 0), 5, 0.3);
                }
                return;
            }

            // Launch meteor (tick 15)
            if (!launched) {
                launched = true;
                meteorHandle = displayBuilder.spawnBlock(
                    center.clone().add(0, 3, 0), Material.CRYING_OBSIDIAN);
                meteorHandle.scale(1.0f, 1.0f, 1.0f).glow(120, 0, 30).interpolation(1, 0);
                spawnedEntities.add(meteorHandle.entity());

                // Warning marker at target
                DisplayBuilder.dustParticles(new Location(w, targetX, center.getY() + 0.2, targetZ),
                    8, 1.5, 120, 0, 30, 1.0f);
            }

            if (meteorHandle == null || !meteorHandle.entity().isValid()) return;

            // Parabolic arc: 40-tick flight
            int flightTick = ticksAlive - 15;
            int flightDuration = 40;
            if (flightTick < flightDuration && !impacted) {
                double t = (double) flightTick / flightDuration;
                double x = center.getX() + (targetX - center.getX()) * t;
                double z = center.getZ() + (targetZ - center.getZ()) * t;
                // Parabolic height: peaks at midpoint, 10 blocks high
                double height = 10 * (4 * t * (1 - t));
                Location pos = new Location(w, x, center.getY() + height, z);
                meteorHandle.entity().teleport(pos);

                // Spin
                float rot = (float) (flightTick * 0.15);
                meteorHandle.entity().setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(rot, 0.5f, 1.0f, 0.3f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                meteorHandle.entity().setInterpolationDuration(1);
                meteorHandle.entity().setInterpolationDelay(0);

                // Trail
                if (flightTick % 2 == 0) {
                    DisplayBuilder.darkPurpleDust(pos, 4, 0.3);
                }
            }

            // Impact
            if (flightTick >= flightDuration && !impacted) {
                impacted = true;
                Location impactLoc = new Location(w, targetX, center.getY(), targetZ);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.dustParticles(impactLoc, 25, 2.5, 80, 0, 160, 2.0f);
                DisplayBuilder.dustParticles(impactLoc, 15, 1.5, 120, 0, 30, 1.5f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new CryingMeteor(plugin); }
    }

    // =========================================================================
    // ATTACK 7 — Void Drip
    // Purple stained glass blocks "drip" down from 8 blocks above the egg,
    // falling slowly one at a time in a circle pattern. Each creates a small
    // impact zone on landing. Slow cadence, easy to dodge individually.
    // =========================================================================
    public static class VoidDrip extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> dripHandles = new ArrayList<>();
        private final List<double[]> dripTargets = new ArrayList<>();
        private int dripsSpawned = 0;
        private static final int TOTAL_DRIPS = 8;

        public VoidDrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_void_drip", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-compute drip targets in a circle
            for (int i = 0; i < TOTAL_DRIPS; i++) {
                double angle = (2 * Math.PI * i) / TOTAL_DRIPS;
                double x = Math.cos(angle) * 6;
                double z = Math.sin(angle) * 6;
                dripTargets.add(new double[]{x, z});
            }
            // Overhead rumble warning
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 8, 0), 10, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn new drip every 15 ticks
            if (ticksAlive % 15 == 0 && dripsSpawned < TOTAL_DRIPS) {
                double[] target = dripTargets.get(dripsSpawned);
                Location spawnLoc = center.clone().add(target[0], 8, target[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnLoc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                dripHandles.add(h);
                spawnedEntities.add(h.entity());
                dripsSpawned++;
                DisplayBuilder.playSound(spawnLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
            }

            // Animate existing drips — fall slowly
            for (int i = 0; i < dripHandles.size(); i++) {
                BlockDisplayHandle h = dripHandles.get(i);
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                if (loc.getY() > center.getY() + 0.2) {
                    // Still falling — 0.15 blocks/tick
                    h.entity().teleport(loc.clone().add(0, -0.15, 0));
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.purpleDust(loc, 2, 0.2);
                    }
                } else if (loc.getY() <= center.getY() + 0.2) {
                    // Impact
                    double[] target = dripTargets.get(i);
                    Location impactLoc = center.clone().add(target[0], 0.1, target[1]);
                    h.entity().teleport(impactLoc);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 10, 1.0, 128, 0, 255, 1.2f);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
                    // Mark as grounded by scaling flat
                    h.entity().setTransformation(new Transformation(
                        new Vector3f(-0.4f, 0, -0.4f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.8f, 0.05f, 0.8f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }
        }

        @Override
        protected void onCleanup() {
            dripHandles.clear();
            dripTargets.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new VoidDrip(plugin); }
    }

    // =========================================================================
    // ATTACK 8 — Amethyst Burst
    // A ring of amethyst blocks rapidly materializes around the egg (radius 4),
    // glows bright, then shatters outward as small projectile fragments. The
    // formation gives 2 seconds to move away before the burst.
    // =========================================================================
    public static class AmethystBurst extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean burstFired = false;

        public AmethystBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_amethyst_burst", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flash at ring radius
            DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0), 4.0,
                Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.0f));
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Form ring (tick 10)
            if (ticksAlive == 10) {
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    Location loc = center.clone().add(Math.cos(angle) * 4, 1.0, Math.sin(angle) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                    ringHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
            }

            // Glow intensifies (10-50 ticks = 2 sec warning)
            if (ticksAlive > 10 && ticksAlive < 50 && !burstFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0), 4.0,
                        Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(200, 100, 255), 1.2f));
                }
            }

            // Burst outward (tick 50)
            if (ticksAlive == 50 && !burstFired) {
                burstFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.2f, 0.5f);
            }

            // Shards fly outward (50-90 ticks)
            if (burstFired && ticksAlive > 50 && ticksAlive < 90) {
                float dist = (ticksAlive - 50) * 0.4f;
                for (int i = 0; i < ringHandles.size(); i++) {
                    if (!ringHandles.get(i).entity().isValid()) continue;
                    double angle = (2 * Math.PI * i) / ringHandles.size();
                    Location loc = center.clone().add(
                        Math.cos(angle) * (4 + dist), 1.0 - dist * 0.02, Math.sin(angle) * (4 + dist));
                    ringHandles.get(i).entity().teleport(loc);

                    // Spin fragments
                    float rot = (float)(ticksAlive * 0.2);
                    ringHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.2f, -0.3f, -0.2f),
                        new AxisAngle4f(rot, 0.3f, 1.0f, 0.5f),
                        new Vector3f(0.4f, 0.6f, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    ringHandles.get(i).entity().setInterpolationDuration(1);
                    ringHandles.get(i).entity().setInterpolationDelay(0);
                }

                // Damage check on outer ring
                if (ticksAlive % 5 == 0) {
                    double outerRadius = 4 + dist;
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double pDist = player.getLocation().toVector().setY(0)
                            .distance(center.toVector().setY(0));
                        if (Math.abs(pDist - outerRadius) < 2.0 && player.getLocation().getY() < center.getY() + 3) {
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            ringHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new AmethystBurst(plugin); }
    }

    // =========================================================================
    // ATTACK 9 — Dark Pulsar
    // The egg emits a vertical pillar of void energy upward, then the pillar
    // "falls" outward into 4 cardinal directions as ground-level beams. Slow
    // and telegraphed — the pillar builds for 2 seconds first.
    // =========================================================================
    public static class DarkPulsar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean pillarBuilt = false;
        private boolean beamsFired = false;

        public DarkPulsar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_dark_pulsar", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(5.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1, 0), 8, 0.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Build pillar upward (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pillarBuilt) {
                if (ticksAlive % 5 == 0) {
                    int height = ticksAlive / 5;
                    Location loc = center.clone().add(0, 1 + height, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                    h.scale(0.4f, 1.0f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                    pillarHandles.add(h);
                    spawnedEntities.add(h.entity());
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.5f, 0.3f + height * 0.1f);
                }
            }

            // Pillar complete (tick 40) — pulse
            if (ticksAlive == 40) {
                pillarBuilt = true;
                DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 20, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.8f);
            }

            // Pillar "falls" into 4 beams (tick 50)
            if (ticksAlive == 50 && !beamsFired) {
                beamsFired = true;
                // Remove pillar blocks
                for (BlockDisplayHandle h : pillarHandles) {
                    if (h.entity().isValid()) h.entity().remove();
                }
                pillarHandles.clear();

                // Spawn 4 cardinal beams
                double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (double[] dir : dirs) {
                    for (int i = 1; i <= 10; i++) {
                        Location loc = center.clone().add(dir[0] * i, 0.3, dir[1] * i);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.8f);
            }

            // Beam particles
            if (beamsFired && ticksAlive > 50 && ticksAlive % 5 == 0) {
                double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (double[] dir : dirs) {
                    Location end = center.clone().add(dir[0] * 10, 0.5, dir[1] * 10);
                    DisplayBuilder.purpleDust(end, 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() {
            pillarHandles.clear();
            beamHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new DarkPulsar(plugin); }
    }

    // =========================================================================
    // ATTACK 10 — Endstone Rain
    // 10 small end stone blocks slowly float upward from the egg, spread out
    // in random directions, then rain down onto the arena floor. Each creates
    // a small impact zone. Gentle, slow, but covers area.
    // =========================================================================
    public static class EndstoneRain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stoneHandles = new ArrayList<>();
        private final List<double[]> targetOffsets = new ArrayList<>();
        private boolean stonesLaunched = false;
        private boolean stonesFalling = false;
        private static final int STONE_COUNT = 10;

        public EndstoneRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_endstone_rain", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-compute random target offsets
            for (int i = 0; i < STONE_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                targetOffsets.add(new double[]{ox, oz});
            }
            DisplayBuilder.purpleDust(center.clone().add(0, 1, 0), 8, 1.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn stones floating up (tick 10)
            if (ticksAlive == 10 && !stonesLaunched) {
                stonesLaunched = true;
                for (int i = 0; i < STONE_COUNT; i++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 1, 0), Material.END_STONE);
                    h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    stoneHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Float upward and spread (10-60 ticks)
            if (stonesLaunched && ticksAlive > 10 && ticksAlive < 60 && !stonesFalling) {
                float t = (ticksAlive - 10) / 50.0f;
                float height = 1 + t * 10; // Rise to 11 blocks
                for (int i = 0; i < stoneHandles.size(); i++) {
                    if (!stoneHandles.get(i).entity().isValid()) continue;
                    double[] target = targetOffsets.get(i);
                    double x = target[0] * t;
                    double z = target[1] * t;
                    stoneHandles.get(i).entity().teleport(center.clone().add(x, height, z));

                    // Gentle rotation
                    float rot = (float)(ticksAlive * 0.05 + i);
                    stoneHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.2f, -0.2f, -0.2f),
                        new AxisAngle4f(rot, 0.3f, 1.0f, 0.5f),
                        new Vector3f(0.4f, 0.4f, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    stoneHandles.get(i).entity().setInterpolationDuration(2);
                    stoneHandles.get(i).entity().setInterpolationDelay(0);
                }
            }

            // Pause at apex (60-80 ticks)
            if (ticksAlive == 60) {
                stonesFalling = false; // Still at apex
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.4f);
                // Warning dust at target locations
                for (int i = 0; i < targetOffsets.size(); i++) {
                    double[] target = targetOffsets.get(i);
                    DisplayBuilder.dustParticles(center.clone().add(target[0], 0.2, target[1]),
                        5, 0.5, 120, 0, 30, 1.0f);
                }
            }

            // Fall (tick 80)
            if (ticksAlive >= 80 && !stonesFalling) {
                stonesFalling = true;
            }

            // Stones fall (80-120 ticks)
            if (stonesFalling && ticksAlive >= 80 && ticksAlive < 120) {
                float fallT = (ticksAlive - 80) / 40.0f;
                float height = 11 * (1 - fallT);
                for (int i = 0; i < stoneHandles.size(); i++) {
                    if (!stoneHandles.get(i).entity().isValid()) continue;
                    double[] target = targetOffsets.get(i);
                    stoneHandles.get(i).entity().teleport(
                        center.clone().add(target[0], Math.max(0.1, height), target[1]));
                }
            }

            // Impact (tick 120)
            if (ticksAlive == 120) {
                for (int i = 0; i < stoneHandles.size(); i++) {
                    double[] target = targetOffsets.get(i);
                    Location impactLoc = center.clone().add(target[0], 0.1, target[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 8, 1.0, 80, 0, 160, 1.0f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            stoneHandles.clear();
            targetOffsets.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new EndstoneRain(plugin); }
    }
}
