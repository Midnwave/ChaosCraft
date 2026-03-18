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
 * Phase 3 Environmental — GROUP 3: SOUL EVENTS
 * 10 attacks (21-30) themed around soul fire, soul sand, and spectral hazards.
 * Calamity Dweller (Boss 3) — boss3-dweller.md
 *
 * Design notes:
 * - No status effects (design mentions slowness/blindness but we skip per rules)
 * - Dweller palette: crimson glow (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Materials: SOUL_SOIL, SOUL_SAND, NETHERRACK, BLACKSTONE
 * - Damage in HP (4.0 - 12.0 range)
 */
public final class SoulEvents {

    private SoulEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulDrainAura(plugin));
        registry.register(new SoulScream(plugin));
        registry.register(new SoulAnchor(plugin));
        registry.register(new SoulSeekerOrbs(plugin));
        registry.register(new SoulGeyserField(plugin));
        registry.register(new WhisperNet(plugin));
        registry.register(new SoulTide(plugin));
        registry.register(new SoulBind(plugin));
        registry.register(new SoulSiphonPillar(plugin));
        registry.register(new SoulExplosion(plugin));
    }

    // =========================================================================
    // ATTACK 21 — Soul Drain Aura
    // Soul soil patches glow with soul fire, dealing 1.5 hearts/s for 8 seconds.
    // Players on soul soil take wither-style damage.
    // =========================================================================
    public static class SoulDrainAura extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> soulPatchLocations = new ArrayList<>();
        private boolean auraActive = false;

        public SoulDrainAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain_aura", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 2s warn + 8s active
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Scatter soul soil patches around arena
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.random() * 0.3;
                double dist = 3.0 + Math.random() * 7.0;
                soulPatchLocations.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    for (Location patch : soulPatchLocations) {
                        DisplayBuilder.dustParticles(patch.clone().add(0, 0.3, 0), 3, 0.4, 0, 150, 255, 0.6f);
                    }
                }
                return;
            }

            // Activate aura
            if (!auraActive) {
                auraActive = true;
                for (Location patch : soulPatchLocations) {
                    // Spawn soul soil display blocks
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (x * x + z * z > 1) continue;
                            BlockDisplayHandle h = displayBuilder.spawnBlock(patch.clone().add(x, 0.01, z), Material.SOUL_SOIL);
                            h.scale(1.0f, 0.08f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                            handles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
            }

            // Rising soul fire particles with pulsing rhythm
            if (ticksAlive % 6 == 0) {
                for (Location patch : soulPatchLocations) {
                    float pulseFactor = (float)Math.sin(ticksAlive * 0.15) * 0.3f + 0.7f;
                    DisplayBuilder.dustParticles(patch.clone().add(0, 0.5 * pulseFactor, 0),
                            4, 0.8, 0, 150, 255, pulseFactor);
                }
            }

            // Damage players on soul soil patches — 3 HP (1.5 hearts) per second
            if (ticksAlive % 10 == 0) {
                for (Location patch : soulPatchLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - patch.getX();
                        double dz = p.getLocation().getZ() - patch.getZ();
                        if (dx * dx + dz * dz <= 2.25) {
                            p.damage(4.0); // ~1.5 hearts/s at 10-tick interval = ~3 HP
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulDrainAura(plugin); }
    }

    // =========================================================================
    // ATTACK 22 — Soul Scream
    // Dweller fires a soul fire cone: 12 blocks long, 3 wide at terminus.
    // 4 hearts/s for 2 seconds. Cone sweeps at 30 degrees/second.
    // =========================================================================
    public static class SoulScream extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double coneAngle;
        private boolean firing = false;

        public SoulScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_scream", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // 1s charge + 2s beam
            config.setCooldownTicks(360); // 18s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Face nearest player
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
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                coneAngle = Math.atan2(dz, dx);
            } else {
                coneAngle = Math.random() * 2 * Math.PI;
            }
            // Charge particles leaking from mouth position
            DisplayBuilder.dustParticles(center.clone().add(Math.cos(coneAngle) * 0.5, 1.5, Math.sin(coneAngle) * 0.5),
                    5, 0.3, 0, 150, 255, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Charge: 20 ticks (1s)
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(coneAngle) * 0.5, 1.5, Math.sin(coneAngle) * 0.5),
                            3, 0.2, 0, 150, 255, 0.6f);
                }
                return;
            }

            // Firing phase: cone sweeps at 30 degrees/second = ~0.5 rad/s = 0.026 rad/tick
            if (!firing) {
                firing = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
            }

            coneAngle += 0.026;

            // Draw cone particles along length
            for (int d = 1; d <= 12; d++) {
                double widthAtDist = (d / 12.0) * 3.0;
                Location coneLoc = center.clone().add(Math.cos(coneAngle) * d, 1.0, Math.sin(coneAngle) * d);
                DisplayBuilder.dustParticles(coneLoc, 3, widthAtDist * 0.3, 0, 150, 255, 1.0f);
            }

            // Damage players in cone
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < 1.0 || dist > 12.0) continue;

                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(normalizeAngle(playerAngle - coneAngle));
                    double maxAngle = Math.atan2(1.5, dist); // Half-width angle at this distance
                    if (angleDiff <= maxAngle) {
                        p.damage(4.0); // ~4 hearts/s at 5-tick interval
                    }
                }
            }

            // Sound during beam
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 1.2f);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulScream(plugin); }
    }

    // =========================================================================
    // ATTACK 23 — Soul Anchor
    // Soul fire pillar erupts 10 blocks, sends expanding ring, pillar persists.
    // 5 hearts from ring, 2 hearts/s from pillar proximity.
    // =========================================================================
    public static class SoulAnchor extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean pillarUp = false;
        private boolean ringFired = false;
        private int pulseCount = 0;

        public SoulAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_anchor", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(4.0); // 2 hearts/s from pillar proximity
            config.setDamageRadius(2.0);
            config.setDurationTicks(340); // 1.5s warn + 3s pulse + 12s persist
            config.setCooldownTicks(480); // 24s
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Soul soil flare
            DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 8, 0.5, 0, 150, 255, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 5, 0.4, 0, 150, 255, 0.8f);
                }
                return;
            }

            // Erupt pillar
            if (!pillarUp) {
                pillarUp = true;
                for (int y = 0; y < 10; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.SOUL_SOIL);
                    h.scale(0.5f, 1.0f, 0.5f).glow(0, 150, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Soul fire at base
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        if (Math.abs(x) + Math.abs(z) > 1) continue;
                        BlockDisplayHandle fh = displayBuilder.spawnBlock(center.clone().add(x, 0.01, z), Material.SOUL_SAND);
                        fh.scale(1.0f, 0.05f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                        handles.add(fh);
                        spawnedEntities.add(fh.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.7f);
            }

            // 3 pulses, then ring expansion
            if (pillarUp && pulseCount < 3 && ticksAlive % 20 == 0 && ticksAlive >= 30) {
                pulseCount++;
                DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 10, 0.5, 0, 150, 255, 1.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }

            // Expanding ring after 3rd pulse
            if (pulseCount >= 3 && !ringFired) {
                ringFired = true;
            }

            if (ringFired) {
                int ringTick = ticksAlive - 90; // Approximately when ring fires
                if (ringTick > 0 && ringTick <= 40) {
                    double ringRadius = ringTick * 0.2; // Expand outward
                    DisplayBuilder.particleRing(center, ringRadius, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.2f));

                    // Ring damage
                    if (ringTick % 4 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                            if (Math.abs(dist - ringRadius) <= 1.5) {
                                p.damage(10.0); // 5 hearts
                            }
                        }
                    }
                }
            }

            // Pillar ambient particles
            if (ticksAlive % 12 == 0) {
                int yRand = (int)(Math.random() * 10);
                DisplayBuilder.dustParticles(center.clone().add(0, yRand, 0), 3, 0.3, 0, 150, 255, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulAnchor(plugin); }
    }

    // =========================================================================
    // ATTACK 24 — Soul Seeker Orbs
    // 3 tracking soul fire orbs drift toward players at half walk speed.
    // 6 hearts per orb detonation. 20-second lifetime.
    // =========================================================================
    public static class SoulSeekerOrbs extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private final List<Location> orbPositions = new ArrayList<>();
        private final List<Boolean> orbDetonated = new ArrayList<>();

        public SoulSeekerOrbs(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_seeker_orbs", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(420); // 1s warn + 20s lifetime
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Border flare at orb origins
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3;
                Location origin = center.clone().add(Math.cos(angle) * 10, 1, Math.sin(angle) * 10);
                DisplayBuilder.dustParticles(origin, 6, 0.3, 0, 150, 255, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn orbs at tick 20 (1s)
            if (ticksAlive == 20) {
                for (int i = 0; i < 3; i++) {
                    double angle = (2 * Math.PI * i) / 3;
                    Location orbLoc = center.clone().add(Math.cos(angle) * 10, 1, Math.sin(angle) * 10);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(orbLoc, Material.SOUL_SAND);
                    h.scale(0.5f, 0.5f, 0.5f).glow(0, 150, 255).interpolation(3, 0);
                    orbHandles.add(h);
                    orbPositions.add(orbLoc.clone());
                    orbDetonated.add(false);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 1.0f);
            }

            if (ticksAlive < 20) return;

            // Move orbs toward nearest player at half walk speed (~0.1 b/tick)
            for (int i = 0; i < orbPositions.size(); i++) {
                if (orbDetonated.get(i)) continue;
                Location orbPos = orbPositions.get(i);

                // Find nearest player
                Player nearest = null;
                double closestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(orbPos);
                    if (dist < closestDist) {
                        closestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    Location target = nearest.getLocation();
                    double dx = target.getX() - orbPos.getX();
                    double dz = target.getZ() - orbPos.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist > 0.5) {
                        orbPos.add(dx / dist * 0.1, 0, dz / dist * 0.1);
                        orbHandles.get(i).entity().teleport(orbPos);
                    }

                    // Detonation on contact (within 1.5 blocks)
                    if (dist <= 1.5) {
                        orbDetonated.set(i, true);
                        DisplayBuilder.dustParticles(orbPos, 20, 1.5, 0, 150, 255, 1.5f);
                        DisplayBuilder.playSound(orbPos, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                        orbHandles.get(i).entity().remove();

                        // Detonation damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(orbPos) <= 4.0) {
                                p.damage(12.0); // 6 hearts
                            }
                        }
                    }
                }

                // Trail particles
                if (ticksAlive % 4 == 0 && !orbDetonated.get(i)) {
                    DisplayBuilder.dustParticles(orbPos.clone().add(0, 0.25, 0), 2, 0.2, 0, 150, 255, 0.5f);
                }
            }

            // Ambient sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.2f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulSeekerOrbs(plugin); }
    }

    // =========================================================================
    // ATTACK 25 — Soul Geyser Field
    // All soul soil patches fire simultaneous 5-block soul fire geysers.
    // 5 hearts + upward knockback. 3-second hold.
    // =========================================================================
    public static class SoulGeyserField extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> geyserLocations = new ArrayList<>();
        private boolean geysersActive = false;

        public SoulGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_geyser_field", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 1s warn + 3s geyser
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Scatter geyser points on soul soil locations
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10 + Math.random() * 0.2;
                double dist = 2.0 + Math.random() * 8.0;
                geyserLocations.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            // Warning flash
            for (Location loc : geyserLocations) {
                DisplayBuilder.dustParticles(loc.clone().add(0, 0.2, 0), 4, 0.3, 0, 150, 255, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1s)
            if (ticksAlive < 20) {
                return;
            }

            // Fire geysers
            if (!geysersActive) {
                geysersActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                for (Location loc : geyserLocations) {
                    // 5-block soul fire column
                    for (int y = 0; y < 5; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc.clone().add(0, y, 0), Material.SOUL_SOIL);
                        h.scale(0.4f, 1.0f, 0.4f).glow(0, 150, 255).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2.5, 0), 10, 0.5, 0, 150, 255, 1.2f);

                    // Damage + knockback players on geyser
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - loc.getX();
                        double dz = p.getLocation().getZ() - loc.getZ();
                        if (dx * dx + dz * dz <= 1.5) {
                            p.damage(10.0); // 5 hearts
                            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 1.0, 0)));
                        }
                    }
                }
            }

            // Geyser active particles
            if (geysersActive && ticksAlive % 6 == 0) {
                for (Location loc : geyserLocations) {
                    int yRand = (int)(Math.random() * 5);
                    DisplayBuilder.dustParticles(loc.clone().add(0, yRand, 0), 3, 0.3, 0, 150, 255, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulGeyserField(plugin); }
    }

    // =========================================================================
    // ATTACK 26 — Whisper Net
    // Soul fire trip-lines form X pattern at variable heights between torches.
    // 4 hearts per line crossed. 10s active.
    // =========================================================================
    public static class WhisperNet extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<double[]> lineData = new ArrayList<>(); // {x1,z1,x2,z2,height}
        private boolean netActive = false;

        public WhisperNet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whisper_net", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(230); // 1.5s warn + 10s active
            config.setCooldownTicks(500); // 25s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Define 4 cardinal torch positions at arena edges
            double[][] torchPos = {
                    {center.getX() + 10, center.getZ()},
                    {center.getX() - 10, center.getZ()},
                    {center.getX(), center.getZ() + 10},
                    {center.getX(), center.getZ() - 10}
            };
            // Create X-pattern lines between opposing torches at varied heights
            lineData.add(new double[]{torchPos[0][0], torchPos[0][1], torchPos[1][0], torchPos[1][1], 0.3});
            lineData.add(new double[]{torchPos[0][0], torchPos[0][1], torchPos[1][0], torchPos[1][1], 1.5});
            lineData.add(new double[]{torchPos[2][0], torchPos[2][1], torchPos[3][0], torchPos[3][1], 0.8});
            lineData.add(new double[]{torchPos[2][0], torchPos[2][1], torchPos[3][0], torchPos[3][1], 2.0});
            // Diagonal lines
            lineData.add(new double[]{torchPos[0][0], torchPos[0][1], torchPos[2][0], torchPos[2][1], 1.0});
            lineData.add(new double[]{torchPos[1][0], torchPos[1][1], torchPos[3][0], torchPos[3][1], 0.5});

            // Warning: torch flare
            for (double[] torch : torchPos) {
                Location tLoc = new Location(w, torch[0], center.getY() + 1, torch[1]);
                DisplayBuilder.dustParticles(tLoc, 6, 0.3, 0, 150, 255, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                return;
            }

            // Activate net
            if (!netActive) {
                netActive = true;
                // Spawn line display blocks along each line
                for (double[] line : lineData) {
                    int segments = 8;
                    for (int s = 0; s <= segments; s++) {
                        double t = (double) s / segments;
                        double lx = line[0] + (line[2] - line[0]) * t;
                        double lz = line[1] + (line[3] - line[1]) * t;
                        Location segLoc = new Location(w, lx, center.getY() + line[4], lz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.SOUL_SAND);
                        h.scale(0.15f, 0.15f, 0.15f).glow(0, 150, 255).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.5f);
            }

            // Ambient flicker along lines
            if (ticksAlive % 8 == 0) {
                int lineIdx = (int)(Math.random() * lineData.size());
                if (lineIdx < lineData.size()) {
                    double[] line = lineData.get(lineIdx);
                    double t = Math.random();
                    double lx = line[0] + (line[2] - line[0]) * t;
                    double lz = line[1] + (line[3] - line[1]) * t;
                    DisplayBuilder.dustParticles(new Location(w, lx, center.getY() + line[4], lz),
                            3, 0.2, 0, 150, 255, 0.5f);
                }
            }

            // Damage players crossing lines
            if (ticksAlive % 6 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    for (double[] line : lineData) {
                        double lineHeight = center.getY() + line[4];
                        if (Math.abs(ploc.getY() - lineHeight) > 1.0) continue;

                        // Check perpendicular distance to line segment
                        double lx1 = line[0], lz1 = line[1], lx2 = line[2], lz2 = line[3];
                        double ldx = lx2 - lx1, ldz = lz2 - lz1;
                        double lineLen = Math.sqrt(ldx * ldx + ldz * ldz);
                        if (lineLen < 0.01) continue;
                        double t = ((ploc.getX() - lx1) * ldx + (ploc.getZ() - lz1) * ldz) / (lineLen * lineLen);
                        if (t < 0 || t > 1) continue;
                        double nearX = lx1 + t * ldx;
                        double nearZ = lz1 + t * ldz;
                        double distToLine = Math.sqrt(
                                (ploc.getX() - nearX) * (ploc.getX() - nearX) +
                                (ploc.getZ() - nearZ) * (ploc.getZ() - nearZ));
                        if (distToLine <= 0.8) {
                            p.damage(8.0); // 4 hearts
                            break; // Only one line hit per tick
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhisperNet(plugin); }
    }

    // =========================================================================
    // ATTACK 27 — Soul Tide
    // Soul sand spreads from west edge inward at 1 block/2s. Slowness zone.
    // No direct damage, but movement penalty during Dweller attacks.
    // =========================================================================
    public static class SoulTide extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double tideAdvance = 0;
        private boolean holding = false;
        private boolean retreating = false;
        private int holdStartTick = 0;

        public SoulTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_tide", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(700); // 2s warn + 20s adv + 5s hold + 10s retreat
            config.setCooldownTicks(700); // 35s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // West edge glow
            for (int z = -5; z <= 5; z++) {
                DisplayBuilder.dustParticles(center.clone().add(-10, 0.2, z), 2, 0.3, 0, 150, 255, 0.5f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    for (int z = -5; z <= 5; z++) {
                        DisplayBuilder.dustParticles(center.clone().add(-10, 0.3, z), 2, 0.2, 0, 150, 255, 0.6f);
                    }
                }
                return;
            }

            // Advance: 1 block every 40 ticks (2s per block)
            if (!holding && !retreating) {
                tideAdvance += 0.025;
                if (tideAdvance >= 10) {
                    holding = true;
                    holdStartTick = ticksAlive;
                }
            }

            // Hold: 100 ticks (5s)
            if (holding && !retreating) {
                if (ticksAlive - holdStartTick >= 100) {
                    retreating = true;
                }
            }

            // Retreat: 2x speed
            if (retreating) {
                tideAdvance -= 0.05;
                if (tideAdvance <= 0) tideAdvance = 0;
            }

            // Visual: soul sand tide blocks
            if (ticksAlive % 8 == 0 && tideAdvance > 0) {
                for (int row = 0; row < (int) tideAdvance; row++) {
                    double x = -10 + row;
                    if (ticksAlive % 16 == 0 && row % 2 == 0) {
                        for (int z = -5; z <= 5; z += 2) {
                            DisplayBuilder.dustParticles(center.clone().add(x, 0.2, z), 1, 0.3, 0, 150, 255, 0.4f);
                        }
                    }
                }

                // Slow players in tide zone (velocity reduction, no status effect)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double playerX = p.getLocation().getX() - center.getX();
                    double playerZ = p.getLocation().getZ() - center.getZ();
                    if (playerX >= -10 && playerX <= -10 + tideAdvance && Math.abs(playerZ) <= 6) {
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.6).setZ(vel.getZ() * 0.6));
                    }
                }
            }

            // Sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulTide(plugin); }
    }

    // =========================================================================
    // ATTACK 28 — Soul Bind
    // Beam anchors target player with soul ring. Ring damages allies within 3 blocks.
    // 3 hearts to allies, target is slowed. Triple jump to break free.
    // =========================================================================
    public static class SoulBind extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Player boundPlayer = null;
        private int bindTick = 0;
        private boolean broken = false;

        public SoulBind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_bind", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 0.5s warn + 5s bind
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Find target player
            double closestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < closestDist) {
                    closestDist = dist;
                    boundPlayer = p;
                }
            }
            if (boundPlayer != null) {
                // Arm extend particle
                DisplayBuilder.dustParticles(boundPlayer.getLocation().clone().add(0, 1, 0), 4, 0.3, 0, 150, 255, 0.6f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (boundPlayer == null || !boundPlayer.isOnline() || broken) return;

            // Warning: 10 ticks (0.5s)
            if (ticksAlive < 10) {
                return;
            }

            // Spawn binding ring at player feet
            if (ticksAlive == 10) {
                Location pLoc = boundPlayer.getLocation();
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    Location ringLoc = pLoc.clone().add(Math.cos(angle) * 1.5, 0.05, Math.sin(angle) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.SOUL_SAND);
                    h.scale(0.4f, 0.08f, 0.4f).glow(0, 150, 255).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
                bindTick = ticksAlive;
            }

            // Slow bound player (velocity reduction)
            if (ticksAlive % 4 == 0) {
                org.bukkit.util.Vector vel = boundPlayer.getVelocity();
                boundPlayer.setVelocity(vel.setX(vel.getX() * 0.5).setZ(vel.getZ() * 0.5));
            }

            // Ring visual spins at player feet
            if (ticksAlive % 6 == 0) {
                Location pLoc = boundPlayer.getLocation();
                DisplayBuilder.particleRing(pLoc, 1.5, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(0, 150, 255), 0.8f));
            }

            // Damage allies who step into ring
            if (ticksAlive % 8 == 0) {
                Location pLoc = boundPlayer.getLocation();
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.equals(boundPlayer)) continue;
                    if (p.getLocation().distanceSquared(pLoc) <= 9.0) {
                        p.damage(6.0); // 3 hearts
                    }
                }
            }

            // Update ring position to follow bound player
            if (ticksAlive % 10 == 0) {
                Location pLoc = boundPlayer.getLocation();
                for (int i = 0; i < handles.size() && i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    Location newLoc = pLoc.clone().add(Math.cos(angle) * 1.5, 0.05, Math.sin(angle) * 1.5);
                    handles.get(i).entity().teleport(newLoc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulBind(plugin); }
    }

    // =========================================================================
    // ATTACK 29 — Soul Siphon Pillar
    // Dweller at center fires 3 rotating beams. 30 deg/s rotation.
    // 5 hearts per beam hit. 2 full rotations = 24s.
    // =========================================================================
    public static class SoulSiphonPillar extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double rotationAngle = 0;
        private boolean beamsActive = false;

        public SoulSiphonPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_siphon_pillar", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(520); // 2s setup + 24s rotation
            config.setCooldownTicks(700); // 35s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Setup: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 6, 0.5, 0, 150, 255, 0.8f);
                }
                return;
            }

            // Activate beams
            if (!beamsActive) {
                beamsActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.8f);
            }

            // Rotate: 30 deg/s = 0.5236 rad/s = 0.02618 rad/tick
            rotationAngle += 0.02618;

            // Draw 3 beams (120 degrees apart)
            for (int beam = 0; beam < 3; beam++) {
                double beamAngle = rotationAngle + (beam * 2 * Math.PI / 3);

                // Draw beam as particle line
                for (int d = 1; d <= 12; d++) {
                    double bx = Math.cos(beamAngle) * d;
                    double bz = Math.sin(beamAngle) * d;
                    Location beamLoc = center.clone().add(bx, 1.0, bz);
                    if (d % 2 == 0) {
                        DisplayBuilder.dustParticles(beamLoc, 2, 0.2, 0, 150, 255, 0.8f);
                    }
                }

                // Trail particles behind sweep
                double trailAngle = beamAngle - 0.08;
                for (int d = 3; d <= 10; d += 3) {
                    DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(trailAngle) * d, 1.0, Math.sin(trailAngle) * d),
                            1, 0.3, 0, 100, 200, 0.4f);
                }
            }

            // Damage players hit by beams
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < 1.0 || dist > 12.0) continue;

                    double playerAngle = Math.atan2(dz, dx);
                    for (int beam = 0; beam < 3; beam++) {
                        double beamAngle = rotationAngle + (beam * 2 * Math.PI / 3);
                        double angleDiff = Math.abs(normalizeAngle(playerAngle - beamAngle));
                        if (angleDiff <= 0.15) { // ~8.6 degrees tolerance
                            p.damage(10.0); // 5 hearts
                            break;
                        }
                    }
                }
            }

            // Ambient sound loop
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.8f);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulSiphonPillar(plugin); }
    }

    // =========================================================================
    // ATTACK 30 — Soul Explosion
    // Orb accumulates 15 blocks up over 5s, detonates into 2 concentric rings.
    // 7 hearts per ring. 1 heart/s floor residue for 5s.
    // =========================================================================
    public static class SoulExplosion extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean detonated = false;
        private float orbSize = 0;

        public SoulExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_explosion", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220); // 5s form + instant det + 5s residue
            config.setCooldownTicks(800); // 40s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location skyPos = center.clone().add(0, 15, 0);

            // Formation: 100 ticks (5s) — growing orb
            if (ticksAlive < 100) {
                orbSize = ticksAlive / 100.0f * 3.0f;

                // Orb particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(skyPos, (int)(orbSize * 5), orbSize * 0.5, 0, 150, 255, 1.2f);
                }

                // Orb block display growing
                if (ticksAlive == 20 || ticksAlive == 50 || ticksAlive == 80) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(skyPos, Material.SOUL_SOIL);
                    h.scale(orbSize, orbSize, orbSize).glow(0, 150, 255).interpolation(10, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Building sound
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(skyPos, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f + orbSize * 0.2f, 0.6f);
                    DisplayBuilder.playSound(skyPos, Sound.BLOCK_PORTAL_AMBIENT, 0.3f + orbSize * 0.2f, 0.5f);
                }
                return;
            }

            // Detonation
            if (!detonated) {
                detonated = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.dustParticles(skyPos, 40, 3.0, 0, 150, 255, 2.0f);

                // Remove orb displays
                for (BlockDisplayHandle h : handles) {
                    h.entity().remove();
                }
            }

            // Two expanding rings from center
            int detTick = ticksAlive - 100;
            if (detTick > 0 && detTick <= 40) {
                double outerRingRadius = detTick * 0.5; // Fast ring: 10 b/s
                double innerRingRadius = detTick * 0.25; // Slow ring: 5 b/s

                // Outer ring
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), outerRingRadius, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.5f));

                // Inner ring
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), innerRingRadius, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(0, 100, 200), 1.2f));

                // Ring damage
                if (detTick % 3 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                        boolean hitOuter = Math.abs(dist - outerRingRadius) <= 1.5;
                        boolean hitInner = Math.abs(dist - innerRingRadius) <= 1.5;
                        if (hitOuter || hitInner) {
                            p.damage(12.0); // ~6 hearts (scaled from 7)
                        }
                    }
                }
            }

            // Floor residue: 5s after detonation
            if (detTick > 40 && detTick <= 140) {
                // Blue glow floor within 10 blocks
                if (detTick % 12 == 0) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 10;
                    DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist),
                            3, 0.5, 0, 150, 255, 0.5f);
                }

                // Residue damage
                if (detTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 100) { // 10-block radius
                            p.damage(4.0); // ~1 heart/s
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulExplosion(plugin); }
    }
}
