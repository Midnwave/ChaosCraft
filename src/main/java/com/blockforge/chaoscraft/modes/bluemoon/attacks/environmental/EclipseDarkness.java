package com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental;

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
 * Blue Moon Mode — ECLIPSE DARKNESS
 * 13 shadow/darkness environmental attacks.
 * Palette: dark gray (40,40,50), shadow purple (60,20,80), deep blue (30,30,60)
 */
public final class EclipseDarkness {

    private EclipseDarkness() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowPool(plugin));
        registry.register(new CreepingShadow(plugin));
        registry.register(new DarkPulse(plugin));
        registry.register(new UmbralZone(plugin));
        registry.register(new ShadowBolt(plugin));
        registry.register(new EclipticPath(plugin));
        registry.register(new ShadowStep(plugin));
        registry.register(new ConsumingDark(plugin));
        registry.register(new ShadowMimic(plugin));
        registry.register(new TotalDarkness(plugin));
        registry.register(new DarkSpike(plugin));
        registry.register(new EclipseWave(plugin));
        registry.register(new NightTerror(plugin));
    }

    // ================================================================
    // 1. SHADOW POOL — Dark dust on ground, expanding, damage standing in it
    // ================================================================
    public static class ShadowPool extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double currentRadius = 5.0;

        public ShadowPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_pool", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            currentRadius = 5.0;
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 15, 2.0, 40, 40, 50, 1.5f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Expand radius
            currentRadius = 5.0 + (ticksAlive / 160.0) * 5.0;
            config.setDamageRadius(currentRadius);

            // Dark pool particles on ground
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * currentRadius;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    Location pLoc = center.clone().add(ox, 0.1 + RNG.nextDouble() * 0.3, oz);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.4, 40, 40, 50, 1.8f);
                }
            }

            // Edge particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * 2 * Math.PI + ticksAlive * 0.02;
                    Location edge = center.clone().add(
                            Math.cos(angle) * currentRadius, 0.2, Math.sin(angle) * currentRadius);
                    DisplayBuilder.dustParticles(edge, 2, 0.3, 60, 20, 80, 1.2f);
                }
            }

            // Wisp particles rising
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * currentRadius * 2;
                double oz = (RNG.nextDouble() - 0.5) * currentRadius * 2;
                if (ox * ox + oz * oz <= currentRadius * currentRadius) {
                    w.spawnParticle(Particle.SMOKE, center.clone().add(ox, 0.3, oz), 3, 0.1, 0.3, 0.1, 0.01);
                }
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShadowPool(plugin);
        }
    }

    // ================================================================
    // 2. CREEPING SHADOW — Shadow particles advance in one direction like wall
    // ================================================================
    public static class CreepingShadow extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double shadowDirX, shadowDirZ;
        private double shadowPos = -15;

        public CreepingShadow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_shadow", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            shadowDirX = Math.cos(angle);
            shadowDirZ = Math.sin(angle);
            shadowPos = -15;
            // Warning edge
            double perpX = -shadowDirZ;
            double perpZ = shadowDirX;
            for (int i = -8; i <= 8; i++) {
                Location edge = center.clone().add(
                        shadowDirX * shadowPos + perpX * i, 0.3, shadowDirZ * shadowPos + perpZ * i);
                DisplayBuilder.dustParticles(edge, 2, 0.3, 30, 30, 50, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Advance shadow wall
            shadowPos = -15 + (30.0 * ticksAlive / 80.0);
            double perpX = -shadowDirZ;
            double perpZ = shadowDirX;

            // Draw shadow wall (tall dark particle curtain)
            if (ticksAlive % 2 == 0) {
                for (int i = -8; i <= 8; i++) {
                    for (int y = 0; y < 4; y++) {
                        Location wallLoc = center.clone().add(
                                shadowDirX * shadowPos + perpX * i, y * 0.8, shadowDirZ * shadowPos + perpZ * i);
                        DisplayBuilder.dustParticles(wallLoc, 1, 0.3, 30, 30, 50, 2.0f);
                    }
                }
                // Trailing darkness behind the wall
                for (int i = 0; i < 5; i++) {
                    double trailPos = shadowPos - 1 - RNG.nextDouble() * 5;
                    double spread = (RNG.nextDouble() - 0.5) * 16;
                    Location trail = center.clone().add(
                            shadowDirX * trailPos + perpX * spread, 0.1, shadowDirZ * trailPos + perpZ * spread);
                    DisplayBuilder.dustParticles(trail, 1, 0.5, 40, 40, 50, 1.5f);
                }
            }

            // Damage players engulfed by shadow (behind the wall)
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double projection = dx * shadowDirX + dz * shadowDirZ;
                    double perpDist = Math.abs(dx * perpX + dz * perpZ);
                    // Player is behind wall (covered by shadow) and within width
                    if (projection < shadowPos && perpDist <= 10) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CreepingShadow(plugin);
        }
    }

    // ================================================================
    // 3. DARK PULSE — Expanding dark particle ring, gap behind ring is safe
    // ================================================================
    public static class DarkPulse extends EnvironmentalAttack {
        private double pulseRadius = 0;

        public DarkPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_pulse", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            pulseRadius = 0;
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 20, 1.0, 60, 20, 80, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Expand dark ring
            pulseRadius = (ticksAlive / 30.0) * 14.0;

            // Draw dark ring
            if (ticksAlive % 2 == 0) {
                int points = Math.max(16, (int) (pulseRadius * 4));
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double ox = Math.cos(angle) * pulseRadius;
                    double oz = Math.sin(angle) * pulseRadius;
                    Location pLoc = center.clone().add(ox, 0.3, oz);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.3, 30, 30, 50, 2.0f);
                    // Second layer slightly higher
                    DisplayBuilder.dustParticles(pLoc.clone().add(0, 1.5, 0), 1, 0.3, 60, 20, 80, 1.5f);
                }
            }

            // Damage players on the ring (not inside or outside)
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                double dist = Math.sqrt(player.getLocation().distanceSquared(center));
                double ringDiff = Math.abs(dist - pulseRadius);
                if (ringDiff <= 2.0) {
                    player.damage(config.getDamage());
                    player.setNoDamageTicks(0);
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DarkPulse(plugin);
        }
    }

    // ================================================================
    // 4. UMBRAL ZONE — Large dark particle area, damage/tick
    // ================================================================
    public static class UmbralZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public UmbralZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("umbral_zone", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Darkness engulfs the area
            for (int i = 0; i < 30; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                double y = RNG.nextDouble() * 5;
                DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 30, 30, 50, 2.0f);
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Dense dark particles filling the zone
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 10;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    double y = RNG.nextDouble() * 5;
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 30, 30, 50, 2.0f);
                }
            }

            // Shadow wisps
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 18;
                    double oz = (RNG.nextDouble() - 0.5) * 18;
                    w.spawnParticle(Particle.SMOKE, center.clone().add(ox, 0.5, oz), 3, 0.2, 0.5, 0.2, 0.01);
                }
            }

            // Boundary particles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 10.0, Particle.DUST,
                        20, new Particle.DustOptions(Color.fromRGB(60, 20, 80), 1.2f));
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new UmbralZone(plugin);
        }
    }

    // ================================================================
    // 5. SHADOW BOLT — Dark particle projectile from sky, impact damage
    // ================================================================
    public static class ShadowBolt extends EnvironmentalAttack {
        private boolean impacted = false;
        private double boltY = 25;

        public ShadowBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_bolt", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            boltY = 25;
            // Warning: dark circle on ground
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 3.0, Particle.DUST,
                    12, new Particle.DustOptions(Color.fromRGB(60, 20, 80), 1.5f));
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!impacted) {
                boltY = 25 - (25.0 * ticksAlive / 12.0);
                if (boltY < 0) boltY = 0;

                Location boltLoc = center.clone().add(0, boltY, 0);
                // Dark bolt visuals
                DisplayBuilder.dustParticles(boltLoc, 8, 0.5, 30, 30, 50, 2.5f);
                DisplayBuilder.dustParticles(boltLoc, 4, 0.3, 60, 20, 80, 1.8f);
                w.spawnParticle(Particle.SMOKE, boltLoc, 3, 0.2, 0.2, 0.2, 0.02);

                // Trail
                DisplayBuilder.dustParticles(boltLoc.clone().add(0, 2, 0), 3, 0.3, 40, 40, 60, 1.2f);

                if (boltY <= 0.5) {
                    impacted = true;
                    triggerImpactDamage(center);
                    DisplayBuilder.dustParticles(center, 25, 3.0, 30, 30, 50, 2.5f);
                    DisplayBuilder.dustParticles(center, 15, 2.0, 60, 20, 80, 2.0f);
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, 1, 0), 15, 1.5, 1.0, 1.5, 0.05);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
                }
            }

            // Lingering shadow
            if (impacted && ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 3, 1.5, 40, 40, 50, 1.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShadowBolt(plugin);
        }
    }

    // ================================================================
    // 6. ECLIPTIC PATH — Arc of dark particles across sky, damage beneath, arc moves
    // ================================================================
    public static class EclipticPath extends EnvironmentalAttack {
        private double arcAngle = 0;

        public EclipticPath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ecliptic_path", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            arcAngle = 0;
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            arcAngle += 0.04;

            // Draw arc across sky
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double t = i / 12.0;
                    double localAngle = arcAngle + t * Math.PI;
                    double arcX = Math.cos(localAngle) * 10;
                    double arcY = 10 + Math.sin(t * Math.PI) * 8;
                    double arcZ = Math.sin(localAngle) * 10;
                    Location arcLoc = center.clone().add(arcX, arcY, arcZ);
                    DisplayBuilder.dustParticles(arcLoc, 2, 0.4, 30, 30, 60, 1.8f);
                }
            }

            // Shadow on ground beneath the arc's current position
            double shadowX = Math.cos(arcAngle + Math.PI / 2) * 8;
            double shadowZ = Math.sin(arcAngle + Math.PI / 2) * 8;
            Location shadowCenter = center.clone().add(shadowX, 0, shadowZ);

            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (new Random().nextDouble() - 0.5) * 6;
                    double oz = (new Random().nextDouble() - 0.5) * 6;
                    DisplayBuilder.dustParticles(shadowCenter.clone().add(ox, 0.1, oz), 2, 0.4, 40, 40, 50, 1.5f);
                }
            }

            // Damage players beneath the shadow
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(shadowCenter) <= 25) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(shadowCenter, Sound.AMBIENT_CAVE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EclipticPath(plugin);
        }
    }

    // ================================================================
    // 7. SHADOW STEP — Random dark markers, burst damage if player stands on active one
    // ================================================================
    public static class ShadowStep extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<Location> markers = new ArrayList<>();
        private final List<Integer> markerActiveTick = new ArrayList<>();

        public ShadowStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_step", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(80);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Place 12 random shadow markers
            for (int i = 0; i < 12; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                markers.add(center.clone().add(ox, 0, oz));
                markerActiveTick.add(10 + RNG.nextInt(60)); // Random activation time
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < markers.size(); i++) {
                Location marker = markers.get(i);
                int activeTick = markerActiveTick.get(i);

                // Inactive markers: dim glow
                if (ticksAlive < activeTick) {
                    if (ticksAlive % 6 == 0) {
                        DisplayBuilder.dustParticles(marker.clone().add(0, 0.1, 0), 1, 0.3, 40, 40, 50, 1.0f);
                    }
                    continue;
                }

                // Active marker: bright dark pulse
                if (ticksAlive == activeTick) {
                    DisplayBuilder.dustParticles(marker.clone().add(0, 0.2, 0), 8, 0.8, 60, 20, 80, 2.0f);
                    DisplayBuilder.playSound(marker, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.5f);

                    // Check if any player is standing on it
                    for (Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(marker) <= 4) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                            // Burst effect
                            DisplayBuilder.dustParticles(marker, 15, 1.5, 30, 30, 50, 2.5f);
                            w.spawnParticle(Particle.SMOKE, marker.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.05);
                            DisplayBuilder.playSound(marker, Sound.ENTITY_PHANTOM_BITE, 0.8f, 0.5f);
                        }
                    }
                }

                // Post-activation: fade
                if (ticksAlive > activeTick && ticksAlive < activeTick + 10 && ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(marker.clone().add(0, 0.1, 0), 2, 0.5, 50, 50, 60, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            markers.clear();
            markerActiveTick.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShadowStep(plugin);
        }
    }

    // ================================================================
    // 8. CONSUMING DARK — Darkness from multiple points converging, shrinking safe zone
    // ================================================================
    public static class ConsumingDark extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double safeRadius = 12.0;

        public ConsumingDark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("consuming_dark", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            safeRadius = 12.0;
            // Darkness at edges
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * 2 * Math.PI;
                Location edge = center.clone().add(Math.cos(angle) * 14, 1, Math.sin(angle) * 14);
                DisplayBuilder.dustParticles(edge, 5, 1.0, 30, 30, 50, 2.0f);
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Shrink safe zone
            safeRadius = 12.0 - (11.0 * ticksAlive / 100.0);
            if (safeRadius < 1.0) safeRadius = 1.0;

            // Draw darkness wall at safe radius boundary
            if (ticksAlive % 2 == 0) {
                int points = Math.max(16, (int) (safeRadius * 4));
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double ox = Math.cos(angle) * safeRadius;
                    double oz = Math.sin(angle) * safeRadius;
                    Location wallLoc = center.clone().add(ox, 0, oz);
                    for (int y = 0; y < 4; y++) {
                        DisplayBuilder.dustParticles(wallLoc.clone().add(0, y * 0.8, 0), 1, 0.3, 30, 30, 50, 2.0f);
                    }
                }
            }

            // Dark particles outside the safe zone
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = safeRadius + 2 + RNG.nextDouble() * 5;
                    Location darkLoc = center.clone().add(
                            Math.cos(angle) * dist, RNG.nextDouble() * 3, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(darkLoc, 2, 0.5, 40, 40, 50, 1.8f);
                }
            }

            // Damage players outside the safe zone
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = Math.sqrt(player.getLocation().distanceSquared(center));
                    if (dist > safeRadius) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ConsumingDark(plugin);
        }
    }

    // ================================================================
    // 9. SHADOW MIMIC — Dark particles at player's position 40 ticks ago
    // ================================================================
    public static class ShadowMimic extends EnvironmentalAttack {
        private final List<Location> positionHistory = new ArrayList<>();

        public ShadowMimic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_mimic", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            positionHistory.clear();
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 1.0, 60, 20, 80, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Record current tracked player position
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                positionHistory.add(target.getLocation().clone());
            }

            // Shadow appears at position from 40 ticks ago
            int delayedIndex = positionHistory.size() - 40;
            if (delayedIndex >= 0 && delayedIndex < positionHistory.size()) {
                Location shadowLoc = positionHistory.get(delayedIndex);

                // Draw shadow figure
                if (ticksAlive % 2 == 0) {
                    for (int y = 0; y < 3; y++) {
                        DisplayBuilder.dustParticles(shadowLoc.clone().add(0, y * 0.6, 0),
                                3, 0.3, 30, 30, 50, 1.8f);
                    }
                    w.spawnParticle(Particle.SMOKE, shadowLoc.clone().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
                }

                // Damage if shadow overlaps current position
                if (ticksAlive % 10 == 0 && target != null && target.isOnline()) {
                    if (!isExempt(target) && target.getLocation().distanceSquared(shadowLoc) <= 9) {
                        target.damage(config.getDamage());
                        target.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(shadowLoc, 10, 1.0, 60, 20, 80, 2.0f);
                        DisplayBuilder.playSound(shadowLoc, Sound.ENTITY_PHANTOM_BITE, 0.6f, 0.5f);
                    }
                }
            }

            // Ambient trail
            if (ticksAlive % 8 == 0 && delayedIndex > 0 && delayedIndex < positionHistory.size()) {
                Location trailLoc = positionHistory.get(Math.max(0, delayedIndex - 5));
                DisplayBuilder.dustParticles(trailLoc.clone().add(0, 0.3, 0), 2, 0.4, 40, 40, 50, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            positionHistory.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ShadowMimic(plugin);
        }
    }

    // ================================================================
    // 10. TOTAL DARKNESS — All players near 1 HP. Deep boom. Rare.
    // ================================================================
    public static class TotalDarkness extends EnvironmentalAttack {
        private boolean triggered = false;

        public TotalDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_darkness", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(50.0);
            config.setDurationTicks(5);
            config.setCooldownTicks(6000); // 5 minutes — very rare
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Massive dark fog
            for (int i = 0; i < 60; i++) {
                double ox = (new Random().nextDouble() - 0.5) * 60;
                double oz = (new Random().nextDouble() - 0.5) * 60;
                double y = new Random().nextDouble() * 8;
                DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 1.5, 10, 10, 15, 3.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.1f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!triggered) {
                triggered = true;
                // Set all survival players to near 1 HP
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 2500) {
                        double currentHealth = player.getHealth();
                        if (currentHealth > 2.0) {
                            player.damage(currentHealth - 2.0); // Leave at 1 heart
                        }
                    }
                }
            }

            // Lingering dark fog
            for (int i = 0; i < 20; i++) {
                double ox = (new Random().nextDouble() - 0.5) * 40;
                double oz = (new Random().nextDouble() - 0.5) * 40;
                DisplayBuilder.dustParticles(center.clone().add(ox, new Random().nextDouble() * 5, oz),
                        2, 1.0, 10, 10, 15, 3.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TotalDarkness(plugin);
        }
    }

    // ================================================================
    // 11. DARK SPIKE — Shadow pillar erupts from ground, narrow column
    // ================================================================
    public static class DarkSpike extends EnvironmentalAttack {
        private boolean erupted = false;

        public DarkSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_spike", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning circle at base
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 2.0, Particle.DUST,
                    10, new Particle.DustOptions(Color.fromRGB(60, 20, 80), 1.2f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Warning phase (0-15 ticks)
            if (ticksAlive < 15) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 4, 0.8, 60, 20, 80, 1.5f);
                    DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0),
                            2.0 - ticksAlive * 0.08, Particle.DUST,
                            8, new Particle.DustOptions(Color.fromRGB(80, 30, 100), 1.0f));
                }
                return;
            }

            // Eruption at tick 15
            if (!erupted) {
                erupted = true;
                triggerImpactDamage(center);

                // Dark pillar shooting up
                for (int y = 0; y < 10; y++) {
                    DisplayBuilder.dustParticles(center.clone().add(0, y, 0), 5, 0.5, 30, 30, 50, 2.0f);
                    DisplayBuilder.dustParticles(center.clone().add(0, y, 0), 3, 0.3, 60, 20, 80, 1.5f);
                }
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 5, 0), 15, 0.5, 3.0, 0.5, 0.05);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.5f);

                // Knock players up
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 4) {
                        player.setVelocity(player.getVelocity().add(new Vector(0, 0.8, 0)));
                    }
                }
            }

            // Lingering pillar
            if (erupted && ticksAlive % 3 == 0) {
                double height = 10 * (1.0 - (ticksAlive - 15.0) / 25.0);
                if (height > 0) {
                    for (int y = 0; y < (int) height; y += 2) {
                        DisplayBuilder.dustParticles(center.clone().add(0, y, 0), 2, 0.3, 40, 40, 50, 1.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DarkSpike(plugin);
        }
    }

    // ================================================================
    // 12. ECLIPSE WAVE — Semicircle darkness sweeps 180 degrees
    // ================================================================
    public static class EclipseWave extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double startAngle;
        private double sweepAngle = 0;

        public EclipseWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_wave", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            startAngle = RNG.nextDouble() * 2 * Math.PI;
            sweepAngle = 0;
            // Warning at start angle
            for (int r = 2; r <= 10; r += 2) {
                Location warnLoc = center.clone().add(Math.cos(startAngle) * r, 0.3, Math.sin(startAngle) * r);
                DisplayBuilder.dustParticles(warnLoc, 3, 0.3, 60, 20, 80, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sweep 180 degrees over duration
            sweepAngle = (ticksAlive / 40.0) * Math.PI;
            double currentAngle = startAngle + sweepAngle;

            // Draw dark semicircle leading edge
            if (ticksAlive % 2 == 0) {
                for (double r = 1; r <= 10; r += 1.0) {
                    // Fan of dark particles at current sweep position
                    for (double a = -0.2; a <= 0.2; a += 0.1) {
                        Location loc = center.clone().add(
                                Math.cos(currentAngle + a) * r, 0.3 + r * 0.05,
                                Math.sin(currentAngle + a) * r);
                        DisplayBuilder.dustParticles(loc, 1, 0.3, 30, 30, 50, 2.0f);
                    }
                }
            }

            // Trailing darkness
            if (ticksAlive % 3 == 0) {
                double trailAngle = currentAngle - 0.3;
                for (double r = 2; r <= 10; r += 2) {
                    Location trail = center.clone().add(
                            Math.cos(trailAngle) * r, 0.1, Math.sin(trailAngle) * r);
                    DisplayBuilder.dustParticles(trail, 1, 0.4, 40, 40, 50, 1.5f);
                }
            }

            // Damage players in the sweep arc
            if (ticksAlive % 5 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dist = Math.sqrt(ploc.distanceSquared(center));
                    if (dist <= 10 && dist >= 1) {
                        double playerAngle = Math.atan2(ploc.getZ() - center.getZ(), ploc.getX() - center.getX());
                        double angleDiff = normalizeAngle(playerAngle - currentAngle);
                        if (Math.abs(angleDiff) < 0.4) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                        }
                    }
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.4f, 0.5f);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EclipseWave(plugin);
        }
    }

    // ================================================================
    // 13. NIGHT TERROR — Multiple shadow bolts converge on one player
    // ================================================================
    public static class NightTerror extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final List<double[]> boltOrigins = new ArrayList<>(); // {angle, height, progress}
        private boolean converged = false;

        public NightTerror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("night_terror", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(800);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 bolts from different directions
            for (int i = 0; i < 6; i++) {
                double angle = (i / 6.0) * 2 * Math.PI + RNG.nextDouble() * 0.3;
                double height = 10 + RNG.nextDouble() * 8;
                boltOrigins.add(new double[]{angle, height, 0});
            }
            // Warning: dark particles converging
            for (double[] bolt : boltOrigins) {
                Location origin = center.clone().add(
                        Math.cos(bolt[0]) * 12, bolt[1], Math.sin(bolt[0]) * 12);
                DisplayBuilder.dustParticles(origin, 5, 1.0, 60, 20, 80, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            Player target = getTargetPlayer();
            Location targetLoc = (target != null && target.isOnline()) ? target.getLocation() : center;

            if (!converged && ticksAlive < 40) {
                // Bolts closing in
                double progress = ticksAlive / 40.0;
                for (double[] bolt : boltOrigins) {
                    double startX = Math.cos(bolt[0]) * 12;
                    double startZ = Math.sin(bolt[0]) * 12;
                    double startY = bolt[1];

                    double currentX = startX * (1 - progress) + (targetLoc.getX() - center.getX()) * progress;
                    double currentZ = startZ * (1 - progress) + (targetLoc.getZ() - center.getZ()) * progress;
                    double currentY = startY * (1 - progress);

                    Location boltLoc = center.clone().add(currentX, currentY, currentZ);

                    if (ticksAlive % 2 == 0) {
                        DisplayBuilder.dustParticles(boltLoc, 4, 0.4, 30, 30, 50, 2.0f);
                        DisplayBuilder.dustParticles(boltLoc, 2, 0.2, 60, 20, 80, 1.5f);
                        // Trail
                        double trailX = startX * (1 - progress * 0.9);
                        double trailZ = startZ * (1 - progress * 0.9);
                        double trailY = startY * (1 - progress * 0.9);
                        DisplayBuilder.dustParticles(center.clone().add(trailX, trailY, trailZ),
                                1, 0.3, 40, 40, 50, 1.0f);
                    }
                }

                // Warning at target
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.particleRing(targetLoc.clone().add(0, 0.1, 0), 3.0, Particle.DUST,
                            10, new Particle.DustOptions(Color.fromRGB(60, 20, 80), 1.2f));
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(targetLoc, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.3f);
                }
            }

            // Convergence at tick 40
            if (!converged && ticksAlive >= 40) {
                converged = true;
                triggerImpactDamage(targetLoc);

                // Massive dark explosion
                DisplayBuilder.dustParticles(targetLoc, 30, 3.0, 30, 30, 50, 2.5f);
                DisplayBuilder.dustParticles(targetLoc, 20, 2.0, 60, 20, 80, 2.0f);
                w.spawnParticle(Particle.SMOKE, targetLoc.clone().add(0, 2, 0), 20, 2.0, 2.0, 2.0, 0.1);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.4f);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
            }

            // Lingering darkness
            if (converged && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(targetLoc.clone().add(0, 0.5, 0), 5, 2.0, 40, 40, 50, 1.5f);
            }
        }

        @Override
        protected void onCleanup() {
            boltOrigins.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new NightTerror(plugin);
        }
    }
}
