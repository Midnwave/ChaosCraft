package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

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
 * Phase 4D Environmental -- GROUP 6: PARTICLE STREAM CONVERGENCE EVENTS
 * 10 attacks (#51-60) where the Void Emperor hijacks ambient sky streams and
 * redirects them as focused arena weapons.
 *
 * Design notes:
 * - No status effects
 * - Void Emperor palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Streams converge from sky to ground -- players watch the sky for warnings
 * - Late fight -- damage range 8.0-18.0 HP
 */
public final class StreamConvergence {

    private StreamConvergence() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MagentaConvergence(plugin));
        registry.register(new GoldConvergence(plugin));
        registry.register(new WhiteCurtainSweep(plugin));
        registry.register(new PurpleImpact(plugin));
        registry.register(new CrossTypeConvergence(plugin));
        registry.register(new SequentialSweep(plugin));
        registry.register(new HelixDescent(plugin));
        registry.register(new TotalSkyConvergence(plugin));
        registry.register(new ArcBombardment(plugin));
        registry.register(new StreamWhiteout(plugin));
    }

    // =========================================================================
    // 51. MAGENTA CONVERGENCE -- three magenta streams angle toward a single arena point
    // =========================================================================
    public static class MagentaConvergence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private Location targetPoint;
        private boolean impacted = false;

        public MagentaConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magenta_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds total
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(11.0); // 5.5 hearts
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Select target point offset from center
            double angle = Math.random() * 2 * Math.PI;
            double dist = 4 + Math.random() * 8;
            targetPoint = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Warning: low resonant hum
            DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4f, 0.6f);

            // Ground pulsing -- magenta dust rising at target
            DisplayBuilder.dustParticles(targetPoint, 25, 1.0, 180, 0, 200, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || targetPoint == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks (2 seconds) -- streams angling downward
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    // Three converging stream trails from sky
                    for (int s = 0; s < 3; s++) {
                        double streamAngle = (2 * Math.PI * s) / 3;
                        double progress = ticksAlive / 40.0;
                        double skyX = targetPoint.getX() + Math.cos(streamAngle) * (20 - 20 * progress);
                        double skyZ = targetPoint.getZ() + Math.sin(streamAngle) * (20 - 20 * progress);
                        double skyY = targetPoint.getY() + 25 - 25 * progress;
                        Location streamPos = new Location(w, skyX, skyY, skyZ);
                        DisplayBuilder.dustParticles(streamPos, 8, 0.5, 180, 0, 200, 1.5f);
                    }
                    // Ground pulse intensifying
                    DisplayBuilder.dustParticles(targetPoint.clone().add(0, 0.5, 0), 12, 1.5, 180, 0, 200, 1.0f);
                }
                return;
            }

            // Impact at tick 41
            if (!impacted) {
                impacted = true;
                triggerImpactDamage(targetPoint);

                // Impact sound cluster
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.4f);
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.5f);
                DisplayBuilder.playSound(targetPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.7f);

                // Massive particle burst at convergence point
                w.spawnParticle(Particle.WITCH, targetPoint, 200, 3, 3, 3, 0);
                w.spawnParticle(Particle.FLASH, targetPoint, 1, 0, 0, 0, 0);

                // Impact cylinder block displays
                for (int i = 0; i < 6; i++) {
                    double a = (2 * Math.PI * i) / 6;
                    Location pillarBase = targetPoint.clone().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(pillarBase, Material.AMETHYST_BLOCK);
                    h.scale(0.6f, 5.0f, 0.6f).glow(180, 0, 200).interpolation(5, 0);
                    impactHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Three rising columns of particles
                for (int col = 0; col < 3; col++) {
                    double colAngle = (2 * Math.PI * col) / 3;
                    Location colBase = targetPoint.clone().add(Math.cos(colAngle) * 1.5, 0, Math.sin(colAngle) * 1.5);
                    for (double y = 0; y < 15; y += 0.5) {
                        w.spawnParticle(Particle.WITCH, colBase.clone().add(0, y, 0), 3, 0.2, 0.2, 0.2, 0);
                    }
                }
            }

            // Aftermath: fading particles at target for 3 seconds
            int afterTick = ticksAlive - 41;
            if (afterTick > 0 && afterTick <= 60 && afterTick % 8 == 0) {
                w.spawnParticle(Particle.WITCH, targetPoint, 15, 2.5, 0.5, 2.5, 0);
            }

            // Trail damage to players near stream paths during buildup aftermath
            if (afterTick > 0 && afterTick <= 40 && afterTick % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetPoint) <= 9.0) {
                        p.damage(3.0); // 1.5 hearts tick damage near ground zero
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagentaConvergence(plugin); }
    }

    // =========================================================================
    // 52. GOLD CONVERGENCE -- two gold streams cross in an X pattern
    // =========================================================================
    public static class GoldConvergence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private Location crossingPoint;
        private Location endpointA;
        private Location endpointB;
        private boolean impacted = false;

        public GoldConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gold_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(15.0); // 7.5 hearts at crossing
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crossing point near center
            double cx = center.getX() + (Math.random() - 0.5) * 10;
            double cz = center.getZ() + (Math.random() - 0.5) * 10;
            crossingPoint = new Location(w, cx, center.getY(), cz);

            // Two endpoints forming an X
            double armAngle = Math.random() * Math.PI;
            endpointA = crossingPoint.clone().add(Math.cos(armAngle) * 8, 0, Math.sin(armAngle) * 8);
            endpointB = crossingPoint.clone().add(Math.cos(armAngle + Math.PI) * 8, 0, Math.sin(armAngle + Math.PI) * 8);

            // Warning: totem particles rising at endpoints
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, endpointA.clone().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, endpointB.clone().add(0, 1, 0), 20, 0.5, 1, 0.5, 0.1);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || crossingPoint == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 5 == 0) {
                    // Gold stream trails from sky toward endpoints and crossing
                    double progress = ticksAlive / 50.0;
                    for (int i = 0; i < 2; i++) {
                        Location ep = (i == 0) ? endpointA : endpointB;
                        double skyY = ep.getY() + 30 * (1 - progress);
                        Location streamPos = new Location(w, ep.getX(), skyY, ep.getZ());
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 10, 0.3, 0.3, 0.3, 0.02);
                    }

                    // Early CRIT burst at midpoint at tick 30
                    if (ticksAlive == 30) {
                        w.spawnParticle(Particle.CRIT, crossingPoint.clone().add(0, 2, 0), 50, 1, 1, 1, 0.2);
                    }
                }
                return;
            }

            // Impact at tick 51
            if (!impacted) {
                impacted = true;

                // Crossing point: 15 hearts damage
                triggerImpactDamage(crossingPoint);

                // Endpoint damage: 9 hearts each
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dA = p.getLocation().distanceSquared(endpointA);
                    double dB = p.getLocation().distanceSquared(endpointB);
                    if (dA <= 25.0 || dB <= 25.0) { // 5 block radius
                        p.damage(9.0);
                    }
                }

                // Sounds
                DisplayBuilder.playSound(crossingPoint, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.6f, 0.8f);
                DisplayBuilder.playSound(crossingPoint, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.2f);
                DisplayBuilder.playSound(crossingPoint, Sound.BLOCK_BEACON_ACTIVATE, 0.3f, 1.8f);

                // Visual burst at crossing
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, crossingPoint, 80, 2, 2, 2, 0.15);
                w.spawnParticle(Particle.CRIT, crossingPoint, 50, 1.5, 1.5, 1.5, 0.2);

                // Endpoint bursts
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, endpointA, 80, 2, 2, 2, 0.1);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, endpointB, 80, 2, 2, 2, 0.1);

                // Cross pattern block displays
                BlockDisplayHandle hCenter = displayBuilder.spawnBlock(crossingPoint, Material.GOLD_BLOCK);
                hCenter.scale(1.5f, 0.3f, 1.5f).glow(255, 200, 0).interpolation(5, 0);
                crossHandles.add(hCenter);
                spawnedEntities.add(hCenter.entity());

                // Connecting lines of particle trails
                DisplayBuilder.particleLine(endpointA, crossingPoint, Particle.TOTEM_OF_UNDYING, 3, null);
                DisplayBuilder.particleLine(endpointB, crossingPoint, Particle.TOTEM_OF_UNDYING, 3, null);
            }

            // Aftermath: gold dust falling at crossing
            int afterTick = ticksAlive - 51;
            if (afterTick > 0 && afterTick <= 80 && afterTick % 6 == 0) {
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, crossingPoint.clone().add(0, 3, 0), 8, 1, 0.5, 1, 0.02);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldConvergence(plugin); }
    }

    // =========================================================================
    // 53. WHITE CURTAIN SWEEP -- horizontal curtain of white light sweeps arena
    // =========================================================================
    public static class WhiteCurtainSweep extends EnvironmentalAttack {

        private Location sweepOrigin;
        private boolean sweeping = false;
        private double sweepZ;

        public WhiteCurtainSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_curtain_sweep", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepOrigin = center.clone();
            sweepZ = center.getZ() - 25; // Start north of arena

            // Warning: END_ROD rising from floor
            for (int i = 0; i < 30; i++) {
                double x = center.getX() + (Math.random() - 0.5) * 40;
                double z = center.getZ() + (Math.random() - 0.5) * 40;
                w.spawnParticle(Particle.END_ROD, new Location(w, x, center.getY() + 0.5, z), 3, 0, 2, 0, 0.05);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    // Streams flattening toward Y+8
                    double curtainY = center.getY() + 8;
                    for (int i = 0; i < 15; i++) {
                        double x = center.getX() + (Math.random() - 0.5) * 40;
                        w.spawnParticle(Particle.END_ROD, new Location(w, x, curtainY, sweepZ), 2, 0.3, 0.3, 0.3, 0.01);
                    }
                }
                return;
            }

            // Sweep: 30 ticks (1.5 seconds) across full arena diameter
            if (!sweeping) {
                sweeping = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 0.5f);
            }

            int sweepTick = ticksAlive - 60;
            double sweepSpeed = 50.0 / 30.0; // ~1.67 blocks per tick
            sweepZ = (center.getZ() - 25) + sweepTick * sweepSpeed;

            double curtainY = center.getY() + 8;

            // Dense horizontal sheet
            if (sweepTick <= 30) {
                for (int i = 0; i < 20; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 40;
                    w.spawnParticle(Particle.END_ROD, new Location(w, x, curtainY, sweepZ), 3, 0.2, 0.2, 0.2, 0.01);
                }

                // Damage players at curtain Y level
                if (sweepTick % 2 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        // Must be near the curtain Z and at or below curtain Y
                        if (Math.abs(pl.getZ() - sweepZ) <= 2.0 && pl.getY() < curtainY) {
                            p.damage(14.0); // 7 hearts
                        }
                    }
                }
            }

            // Aftermath: lingering particles at south edge
            if (sweepTick > 30 && sweepTick <= 60 && sweepTick % 8 == 0) {
                double southEdge = center.getZ() + 25;
                for (int i = 0; i < 8; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 40;
                    w.spawnParticle(Particle.END_ROD, new Location(w, x, curtainY, southEdge), 2, 0.5, 0.3, 0.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteCurtainSweep(plugin); }
    }

    // =========================================================================
    // 54. PURPLE IMPACT -- single purple stream thickens and slams into arena
    // =========================================================================
    public static class PurpleImpact extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> fogHandles = new ArrayList<>();
        private Location targetPoint;
        private boolean impacted = false;
        private int fogStartTick = -1;

        public PurpleImpact(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purple_impact", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(13.0); // 6.5 hearts
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 10;
            targetPoint = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Dragon breath fountain shooting upward from target
            w.spawnParticle(Particle.DRAGON_BREATH, targetPoint, 40, 0.5, 10, 0.5, 0.02);

            // Heartbeat-like rumble
            DisplayBuilder.playSound(targetPoint, Sound.BLOCK_NETHERRACK_BREAK, 0.2f, 0.3f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(targetPoint, Sound.BLOCK_NETHERRACK_BREAK, 0.2f, 0.3f), 6L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(targetPoint, Sound.BLOCK_NETHERRACK_BREAK, 0.2f, 0.3f), 12L);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || targetPoint == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 30 ticks (1.5 seconds) -- shorter warning
            if (ticksAlive <= 30) {
                if (ticksAlive % 3 == 0) {
                    // Thickened stream trail from sky
                    double skyY = targetPoint.getY() + 30 - (30.0 * ticksAlive / 30.0);
                    Location streamPos = targetPoint.clone().add(0, skyY - targetPoint.getY(), 0);
                    w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 15, 1, 0.5, 1, 0.02);
                    DisplayBuilder.dustParticles(streamPos, 10, 1.0, 128, 0, 255, 1.5f);
                }
                return;
            }

            // Impact at tick 31
            if (!impacted) {
                impacted = true;
                fogStartTick = ticksAlive;
                triggerImpactDamage(targetPoint);

                // Sounds
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.8f, 0.5f);
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.4f);

                // Dragon breath explosion
                w.spawnParticle(Particle.DRAGON_BREATH, targetPoint, 300, 3, 3, 3, 0.05);

                // Ground fog block displays in radius
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    double r = 3.0 + Math.random() * 4.0;
                    Location fogLoc = targetPoint.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(fogLoc, Material.PURPLE_STAINED_GLASS);
                    h.scale(2.0f, 0.2f, 2.0f).glow(128, 0, 255).interpolation(5, 0);
                    fogHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Ground fog damage: 4/second for 6 seconds (120 ticks)
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 120) {
                // Fog particles
                if ((ticksAlive - fogStartTick) % 4 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, targetPoint.clone().add(0, 1, 0), 25, 3.5, 1, 3.5, 0.01);
                }

                // Damage players in fog
                if ((ticksAlive - fogStartTick) % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        double hDist = Math.sqrt(Math.pow(pl.getX() - targetPoint.getX(), 2) + Math.pow(pl.getZ() - targetPoint.getZ(), 2));
                        if (hDist <= 7.0 && pl.getY() <= targetPoint.getY() + 2.0) {
                            p.damage(8.0); // 4 hearts per second
                        }
                    }
                }
            }

            // Crystal pillar crawl effect (particles on nearby pillars)
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 80 && ticksAlive % 10 == 0) {
                for (double yOff = 0; yOff < 8; yOff += 1.0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, targetPoint.clone().add(5, yOff, 0), 3, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PurpleImpact(plugin); }
    }

    // =========================================================================
    // 55. CROSS-TYPE CONVERGENCE -- magenta + gold streams hit same zone
    // =========================================================================
    public static class CrossTypeConvergence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private Location targetPoint;
        private boolean impacted = false;

        public CrossTypeConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cross_type_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); // 9 hearts per type, 18 overlap
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 8;
            targetPoint = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || targetPoint == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    double progress = ticksAlive / 40.0;

                    // Magenta streams from one direction
                    for (int s = 0; s < 2; s++) {
                        double sa = (Math.PI * s) + 0.3;
                        double skyX = targetPoint.getX() + Math.cos(sa) * (18 - 18 * progress);
                        double skyZ = targetPoint.getZ() + Math.sin(sa) * (18 - 18 * progress);
                        double skyY = targetPoint.getY() + 20 - 20 * progress;
                        DisplayBuilder.dustParticles(new Location(w, skyX, skyY, skyZ), 10, 0.5, 180, 0, 200, 1.5f);
                    }

                    // Gold stream from another direction
                    double ga = Math.PI * 0.5;
                    double gSkyX = targetPoint.getX() + Math.cos(ga) * (18 - 18 * progress);
                    double gSkyZ = targetPoint.getZ() + Math.sin(ga) * (18 - 18 * progress);
                    double gSkyY = targetPoint.getY() + 22 - 22 * progress;
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, new Location(w, gSkyX, gSkyY, gSkyZ), 8, 0.3, 0.3, 0.3, 0.02);
                }
                return;
            }

            // Impact at tick 41
            if (!impacted) {
                impacted = true;
                triggerImpactDamage(targetPoint);

                // Both magenta and gold damage overlap
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(targetPoint) <= 25.0) {
                        p.damage(18.0); // Second type hit
                    }
                }

                // Sounds
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.5f);
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.8f);
                DisplayBuilder.playSound(targetPoint, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.2f);

                // Pink-gold firework burst
                w.spawnParticle(Particle.WITCH, targetPoint, 150, 3, 3, 3, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, targetPoint, 100, 3, 3, 3, 0.1);
                w.spawnParticle(Particle.CRIT, targetPoint, 50, 2, 2, 2, 0.2);

                // Impact display
                BlockDisplayHandle h = displayBuilder.spawnBlock(targetPoint, Material.GOLD_BLOCK);
                h.scale(2.0f, 0.5f, 2.0f).glow(255, 180, 200).interpolation(5, 0);
                impactHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Aftermath: mixed particles rising
            int afterTick = ticksAlive - 41;
            if (afterTick > 0 && afterTick <= 60 && afterTick % 6 == 0) {
                w.spawnParticle(Particle.WITCH, targetPoint.clone().add(0, 1, 0), 10, 2, 1, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, targetPoint.clone().add(0, 1.5, 0), 10, 2, 1, 2, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrossTypeConvergence(plugin); }
    }

    // =========================================================================
    // 56. SEQUENTIAL SWEEP -- streams hit 4 arena points in sequence
    // =========================================================================
    public static class SequentialSweep extends EnvironmentalAttack {

        private final Location[] impactPoints = new Location[4];
        private final boolean[] fired = new boolean[4];
        private final int[] fireTicks = {20, 35, 50, 70}; // Staggered

        public SequentialSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sequential_sweep", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Four impact points in a line across the arena
            double lineAngle = Math.random() * Math.PI;
            for (int i = 0; i < 4; i++) {
                double offset = (i - 1.5) * 12;
                impactPoints[i] = center.clone().add(
                        Math.cos(lineAngle) * offset,
                        0,
                        Math.sin(lineAngle) * offset
                );
                fired[i] = false;
            }

            // Warning: brief magenta flash
            DisplayBuilder.dustParticles(center, 20, 5.0, 180, 0, 200, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Check each impact point's fire time
            for (int i = 0; i < 4; i++) {
                if (impactPoints[i] == null) continue;

                // Pre-impact warning
                if (ticksAlive >= fireTicks[i] - 15 && ticksAlive < fireTicks[i] && !fired[i]) {
                    if (ticksAlive % 5 == 0) {
                        DisplayBuilder.dustParticles(impactPoints[i], 10, 1.0, 180, 0, 200, 1.2f);
                    }
                }

                // Fire impact
                if (ticksAlive >= fireTicks[i] && !fired[i]) {
                    fired[i] = true;

                    // Particle burst based on stream type (cycle through)
                    switch (i % 4) {
                        case 0 -> w.spawnParticle(Particle.WITCH, impactPoints[i], 80, 2, 2, 2, 0);
                        case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, impactPoints[i], 80, 2, 2, 2, 0.1);
                        case 2 -> w.spawnParticle(Particle.END_ROD, impactPoints[i], 80, 2, 2, 2, 0.05);
                        case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, impactPoints[i], 80, 2, 2, 2, 0.02);
                    }

                    // Sound per impact
                    DisplayBuilder.playSound(impactPoints[i], Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.7f);

                    // Damage: 16 in 4-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(impactPoints[i]) <= 16.0) {
                            p.damage(16.0); // 8 hearts
                        }
                    }
                }
            }

            // Aftermath: smoke at all fired points
            if (ticksAlive > 70) {
                for (int i = 0; i < 4; i++) {
                    if (fired[i] && impactPoints[i] != null && ticksAlive % 10 == 0) {
                        w.spawnParticle(Particle.SMOKE, impactPoints[i].clone().add(0, 0.5, 0), 5, 1, 0.5, 1, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SequentialSweep(plugin); }
    }

    // =========================================================================
    // 57. HELIX DESCENT -- white streams spiral downward in a corkscrew drill
    // =========================================================================
    public static class HelixDescent extends EnvironmentalAttack {

        private Location targetPoint;
        private boolean impacted = false;

        public HelixDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("helix_descent", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180); // 9 seconds
            config.setCooldownTicks(2000); // 100 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * 2 * Math.PI;
            double dist = 2 + Math.random() * 10;
            targetPoint = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            // Sustained resonant sound
            DisplayBuilder.playSound(targetPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || targetPoint == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks (4 seconds) -- helix descending
            if (ticksAlive <= 80) {
                double progress = ticksAlive / 80.0;
                double currentY = targetPoint.getY() + 30 * (1 - progress);
                double helixRadius = 3.0 * (1 - progress * 0.7); // Tightening spiral

                // Helix particles along the spiral path
                for (int p = 0; p < 3; p++) {
                    double helixAngle = (ticksAlive * 0.3) + (2 * Math.PI * p / 3);
                    double hx = targetPoint.getX() + Math.cos(helixAngle) * helixRadius;
                    double hz = targetPoint.getZ() + Math.sin(helixAngle) * helixRadius;
                    w.spawnParticle(Particle.END_ROD, new Location(w, hx, currentY, hz), 3, 0.1, 0.1, 0.1, 0.01);
                }

                // Sound escalation
                if (ticksAlive % 20 == 0) {
                    float pitch = 0.6f + (float) progress * 0.6f;
                    DisplayBuilder.playSound(targetPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, pitch);
                }
                return;
            }

            // Impact at tick 81
            if (!impacted) {
                impacted = true;
                triggerImpactDamage(targetPoint);

                // Expanding pulse damage
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist2 = p.getLocation().distanceSquared(targetPoint);
                        if (dist2 <= 16.0 && dist2 > 4.0) { // 2-4 block ring
                            p.damage(10.0);
                        }
                    }
                }, 10L);

                // Sounds
                DisplayBuilder.playSound(targetPoint, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.0f);
                DisplayBuilder.playSound(targetPoint, Sound.BLOCK_GLASS_BREAK, 0.7f, 0.3f);

                // END_ROD expanding ring burst
                for (int ring = 0; ring < 20; ring++) {
                    double a = (2 * Math.PI * ring) / 20;
                    for (double r = 0; r < 4; r += 0.5) {
                        w.spawnParticle(Particle.END_ROD, targetPoint.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r), 2, 0, 0, 0, 0.01);
                    }
                }
            }

            // Aftermath: dim pulsing END_ROD
            int afterTick = ticksAlive - 81;
            if (afterTick > 0 && afterTick <= 80 && afterTick % 10 == 0) {
                DisplayBuilder.particleRing(targetPoint.clone().add(0, 0.3, 0), 2.0, Particle.END_ROD, 12, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HelixDescent(plugin); }
    }

    // =========================================================================
    // 58. TOTAL SKY CONVERGENCE -- every stream converges on arena center (once at 35% HP)
    // =========================================================================
    public static class TotalSkyConvergence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> voidHandles = new ArrayList<>();
        private boolean impacted = false;
        private int fogStartTick = -1;

        public TotalSkyConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_sky_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400); // 20 seconds total
            config.setCooldownTicks(99999); // Once per fight
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Title message: "The sky folds."
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(center) <= 10000) { // 100 block range
                    p.sendTitle(" ", "\u00a75The sky folds.", 10, 40, 20);
                }
            }

            // Ground cracking at center
            w.spawnParticle(Particle.BLOCK, center, 80, 2, 0.5, 2, 0,
                    Material.OBSIDIAN.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks (5 seconds) -- all streams converging
            if (ticksAlive <= 100) {
                if (ticksAlive % 4 == 0) {
                    double progress = ticksAlive / 100.0;

                    // All stream types converging from multiple angles
                    for (int s = 0; s < 8; s++) {
                        double sa = (2 * Math.PI * s) / 8;
                        double skyR = 25 * (1 - progress);
                        double skyY = center.getY() + 35 * (1 - progress);
                        Location streamPos = center.clone().add(Math.cos(sa) * skyR, skyY - center.getY(), Math.sin(sa) * skyR);

                        switch (s % 4) {
                            case 0 -> DisplayBuilder.dustParticles(streamPos, 8, 0.5, 180, 0, 200, 1.5f);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 6, 0.3, 0.3, 0.3, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 6, 0.3, 0.3, 0.3, 0.01);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 6, 0.3, 0.3, 0.3, 0.01);
                        }
                    }

                    // Sigil flare at ground
                    if (ticksAlive % 20 == 0) {
                        DisplayBuilder.dustParticles(center, 30, 3.0, 128, 0, 255, 1.5f);
                    }
                }
                return;
            }

            // Impact at tick 101
            if (!impacted) {
                impacted = true;
                fogStartTick = ticksAlive;

                // Tiered damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 6) p.damage(40.0);       // 20 hearts
                    else if (dist <= 12) p.damage(25.0);  // 12.5 hearts
                    else if (dist <= 20) p.damage(12.0);  // 6 hearts
                }

                // Full sound cluster
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.3f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.7f);

                // Mega particle burst -- all types
                w.spawnParticle(Particle.WITCH, center, 150, 4, 4, 4, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 4, 4, 4, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 100, 4, 4, 4, 0.05);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 200, 4, 4, 4, 0.05);
                w.spawnParticle(Particle.CRIT, center, 50, 3, 3, 3, 0.2);

                // Void zone displays at center
                for (int i = 0; i < 6; i++) {
                    double a = (2 * Math.PI * i) / 6;
                    Location vLoc = center.clone().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(vLoc, Material.OBSIDIAN);
                    h.scale(1.5f, 0.3f, 1.5f).glow(128, 0, 255).interpolation(5, 0);
                    voidHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Void zone: 8 dmg/sec for 10 seconds (200 ticks)
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 200) {
                if ((ticksAlive - fogStartTick) % 4 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1, 0), 25, 1.5, 1.5, 1.5, 0.01);
                }
                if ((ticksAlive - fogStartTick) % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 9.0) {
                            p.damage(16.0); // 8 hearts per second
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalSkyConvergence(plugin); }
    }

    // =========================================================================
    // 59. ARC BOMBARDMENT -- streams track Dragon's recent flight path
    // =========================================================================
    public static class ArcBombardment extends EnvironmentalAttack {

        private final Location[] impactPoints = new Location[3];
        private final boolean[] fired = new boolean[3];
        private final int[] fireTicks = {40, 60, 80};

        public ArcBombardment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arc_bombardment", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three points along a trajectory path away from center
            double pathAngle = Math.random() * 2 * Math.PI;
            for (int i = 0; i < 3; i++) {
                double pathDist = 5 + i * 8;
                impactPoints[i] = center.clone().add(
                        Math.cos(pathAngle) * pathDist,
                        0,
                        Math.sin(pathAngle) * pathDist
                );
                fired[i] = false;
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sequential impacts along dragon's trail
            for (int i = 0; i < 3; i++) {
                if (impactPoints[i] == null) continue;

                // Pre-warning at each point
                if (ticksAlive >= fireTicks[i] - 20 && ticksAlive < fireTicks[i] && !fired[i]) {
                    if (ticksAlive % 5 == 0) {
                        DisplayBuilder.dustParticles(impactPoints[i], 8, 1.0, 180, 0, 200, 1.0f);
                        w.spawnParticle(Particle.SMOKE, impactPoints[i].clone().add(0, 3, 0), 5, 0.5, 1, 0.5, 0.02);
                    }
                }

                if (ticksAlive >= fireTicks[i] && !fired[i]) {
                    fired[i] = true;

                    // Impact sound
                    DisplayBuilder.playSound(impactPoints[i], Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.7f);

                    // Particle burst
                    w.spawnParticle(Particle.WITCH, impactPoints[i], 40, 2, 2, 2, 0);
                    w.spawnParticle(Particle.DRAGON_BREATH, impactPoints[i], 40, 2, 2, 2, 0.02);

                    // 14 damage, 4 block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(impactPoints[i]) <= 16.0) {
                            p.damage(14.0); // 7 hearts
                        }
                    }
                }
            }

            // Aftermath smoke
            if (ticksAlive > 80) {
                for (int i = 0; i < 3; i++) {
                    if (fired[i] && impactPoints[i] != null && ticksAlive % 12 == 0) {
                        w.spawnParticle(Particle.SMOKE, impactPoints[i].clone().add(0, 0.5, 0), 5, 1, 0.5, 1, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArcBombardment(plugin); }
    }

    // =========================================================================
    // 60. STREAM WHITEOUT -- all streams at 5x density for phase transition
    // =========================================================================
    public static class StreamWhiteout extends EnvironmentalAttack {

        private boolean whiteoutStarted = false;

        public StreamWhiteout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_whiteout", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(99999); // Scripted only
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Full whiteout: 30 ticks (1.5 seconds) of maximum density
            if (ticksAlive <= 30) {
                if (!whiteoutStarted) {
                    whiteoutStarted = true;
                }

                // All stream types at overwhelming density
                for (int burst = 0; burst < 5; burst++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 20;
                    double skyY = center.getY() + 15 + Math.random() * 20;
                    Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);

                    switch (burst % 4) {
                        case 0 -> DisplayBuilder.dustParticles(skyPos, 6, 1.0, 180, 0, 200, 2.0f);
                        case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 5, 0.5, 0.5, 0.5, 0.05);
                        case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 5, 0.5, 0.5, 0.5, 0.02);
                        case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 5, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }

            // Decay: 30-40 ticks, rapid falloff
            if (ticksAlive > 30 && ticksAlive <= 40) {
                int remaining = 40 - ticksAlive;
                if (remaining > 0 && ticksAlive % 2 == 0) {
                    for (int i = 0; i < remaining; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 20;
                        double skyY = center.getY() + 15 + Math.random() * 15;
                        Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);
                        w.spawnParticle(Particle.END_ROD, skyPos, 3, 0.5, 0.5, 0.5, 0.01);
                    }
                }
            }

            // Residual fog damage in active void zones
            if (ticksAlive <= 30 && ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9.0) {
                        p.damage(8.0); // 4 hearts residual
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamWhiteout(plugin); }
    }
}
