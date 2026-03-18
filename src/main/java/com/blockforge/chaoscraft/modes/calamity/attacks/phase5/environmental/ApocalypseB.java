package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

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
 * Phase 4E Environmental -- APOCALYPSE B
 * Attacks #141-150: Phase 4 escalation attacks. Soul fire propagation, sky descent,
 * second Calamitas illusion, subsonic pressure, crimson geyser field, void pillar
 * remembrance, magenta storm surge, ground zero pulse, final hour convergence,
 * the last breath.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 10.0-20.0 HP (escalating, Phase 4 exclusive attacks)
 * - These attacks represent Calamitas abandoning restraint entirely
 */
public final class ApocalypseB {

    private ApocalypseB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulFirePropagation(plugin));
        registry.register(new SkyDescent(plugin));
        registry.register(new TheSecondCalamitas(plugin));
        registry.register(new SubsonicPressure(plugin));
        registry.register(new CrimsonGeyserField(plugin));
        registry.register(new VoidPillarRemembrance(plugin));
        registry.register(new MagentaStormSurge(plugin));
        registry.register(new GroundZeroPulse(plugin));
        registry.register(new FinalHourConvergence(plugin));
        registry.register(new TheLastBreath(plugin));
    }

    // =========================================================================
    // 141. SOUL FIRE PROPAGATION -- all soul fire patches permanently expand
    // =========================================================================
    public static class SoulFirePropagation extends EnvironmentalAttack {

        private final List<Location> existingPatches = new ArrayList<>();
        private boolean expanded = false;

        public SoulFirePropagation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_propagation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(2400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 2 + Math.random() * 7;
                existingPatches.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- existing patches pulse higher
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    for (Location patch : existingPatches) {
                        double spikeHeight = Math.min(ticksAlive * 0.06, 4);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, patch.clone().add(0, spikeHeight / 2, 0),
                                12, 0.5, spikeHeight / 2, 0.5, 0.01);
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 1.0f, 0.5f);
                }
                return;
            }

            // Instant expansion
            if (!expanded) {
                expanded = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_BURN, 1.4f, 0.4f);

                for (Location patch : existingPatches) {
                    // Expanded emission zone
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, patch.clone().add(0, 0.5, 0),
                            40, 2.0, 0.5, 2.0, 0.02);
                }
            }

            int expandTick = ticksAlive - 60;

            // Continuous expanded soul fire
            if (expandTick % 4 == 0) {
                for (Location patch : existingPatches) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, patch.clone().add(0, 0.5, 0),
                            12, 2.0, 0.3, 2.0, 0.01);
                }
            }

            // Expanded zone damage
            if (expandTick % 20 == 0) {
                for (Location patch : existingPatches) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - patch.getX();
                        double dz = p.getLocation().getZ() - patch.getZ();
                        if (dx * dx + dz * dz <= 4.0) {
                            p.damage(10.0);
                        }
                    }
                }
            }

            // Soul fire ambient
            if (expandTick == 20) {
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFirePropagation(plugin); }
    }

    // =========================================================================
    // 142. SKY DESCENT -- all five stream types descend toward arena
    // =========================================================================
    public static class SkyDescent extends EnvironmentalAttack {

        private double streamHeight = 30;
        private boolean descending = true;

        public SkyDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_descent", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(3000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- streams begin descending
            if (ticksAlive <= 100) {
                streamHeight = 30 - ticksAlive * 0.18;
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 10;
                        Location streamPos = center.clone().add(Math.cos(a) * d, streamHeight, Math.sin(a) * d);
                        switch (i) {
                            case 0: w.spawnParticle(Particle.WITCH, streamPos, 6, 1.0, 0.5, 1.0, 0); break;
                            case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 4, 1.0, 0.5, 1.0, 0.02); break;
                            case 2: w.spawnParticle(Particle.END_ROD, streamPos, 5, 1.0, 0.5, 1.0, 0.01); break;
                            case 3: w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 6, 1.0, 0.5, 1.0, 0.01); break;
                            default: DisplayBuilder.dustParticles(streamPos, 5, 1.0, 180, 0, 0, 1.5f); break;
                        }
                    }
                }
                if (ticksAlive == 90) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.4f, 0.2f);
                }
                return;
            }

            int holdTick = ticksAlive - 100;
            streamHeight = 12; // locked at near head height

            // Hold at Y+12 for 200 ticks (10 seconds)
            if (holdTick <= 200) {
                if (holdTick % 3 == 0) {
                    for (int x = -8; x <= 8; x += 4) {
                        for (int z = -8; z <= 8; z += 4) {
                            Location streamPos = center.clone().add(x, streamHeight, z);
                            int type = (int) (Math.random() * 5);
                            switch (type) {
                                case 0: w.spawnParticle(Particle.WITCH, streamPos, 4, 1.0, 0.5, 1.0, 0); break;
                                case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 3, 1.0, 0.5, 1.0, 0.01); break;
                                case 2: w.spawnParticle(Particle.END_ROD, streamPos, 4, 1.0, 0.5, 1.0, 0.005); break;
                                case 3: w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 4, 1.0, 0.5, 1.0, 0.005); break;
                                default: DisplayBuilder.dustParticles(streamPos, 4, 1.0, 180, 0, 0, 1.0f); break;
                            }
                        }
                    }
                }

                // Contact damage from low stream
                if (holdTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() >= center.getY() + 10 &&
                                p.getLocation().distanceSquared(center) <= 100.0) {
                            p.damage(14.0);
                        }
                    }
                }

                if (holdTick == 100) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_MOOD, 0.7f, 0.5f);
                }
            }

            // Ascent: streams rise back
            if (holdTick > 200) {
                streamHeight = 12 + (holdTick - 200) * 0.2;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyDescent(plugin); }
    }

    // =========================================================================
    // 143. THE SECOND CALAMITAS -- psychological illusion, zero damage
    // =========================================================================
    public static class TheSecondCalamitas extends EnvironmentalAttack {

        private Location illusionPos;
        private boolean illusionSpawned = false;

        public TheSecondCalamitas(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_second_calamitas", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(340);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Opposite end of arena
            illusionPos = center.clone().add(0, 0, -9);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || illusionPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 120 ticks -- helix at center
            if (ticksAlive <= 120) {
                double helixAngle = ticksAlive * Math.toRadians(10);
                double helixHeight = Math.min(ticksAlive * 0.06, 8);
                if (ticksAlive % 2 == 0) {
                    for (int y = 0; y <= (int) helixHeight; y++) {
                        double a = helixAngle + y * 0.5;
                        Location helixPos = center.clone().add(Math.cos(a) * 4, y, Math.sin(a) * 4);
                        w.spawnParticle(Particle.WITCH, helixPos, 4, 0.2, 0.2, 0.2, 0);
                        DisplayBuilder.dustParticles(helixPos, 3, 0.2, 200, 0, 50, 1.5f);
                    }
                }
                if (ticksAlive == 100) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.3f);
                }
                return;
            }

            // Illusion materializes
            if (!illusionSpawned) {
                illusionSpawned = true;
                DisplayBuilder.playSound(illusionPos, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.7f);
            }

            int illusionTick = ticksAlive - 120;

            // Illusion exists for 240 ticks (12 seconds) -- zero damage
            if (illusionTick <= 240) {
                if (illusionTick % 4 == 0) {
                    // Body particles
                    w.spawnParticle(Particle.WITCH, illusionPos.clone().add(0, 1, 0),
                            15, 0.3, 0.8, 0.3, 0);
                    // Absolute silence during presence
                }
            }

            // Dissolution at 240 ticks
            if (illusionTick == 240) {
                DisplayBuilder.playSound(illusionPos, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
                w.spawnParticle(Particle.WITCH, illusionPos.clone().add(0, 1, 0),
                        60, 2.5, 2.0, 2.5, 0);
                DisplayBuilder.dustParticles(illusionPos.clone().add(0, 1, 0),
                        60, 2.5, 200, 0, 50, 2.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheSecondCalamitas(plugin); }
    }

    // =========================================================================
    // 144. SUBSONIC PRESSURE -- bass burst knocks airborne players
    // =========================================================================
    public static class SubsonicPressure extends EnvironmentalAttack {

        private boolean burstFired = false;

        public SubsonicPressure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subsonic_pressure", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(2200);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- barely perceptible rumble
            if (ticksAlive <= 60) {
                if (ticksAlive == 5) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.1f);
                }
                // Subtle TOTEM gold drift increase
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < 3; i++) {
                        Location skyPos = center.clone().add(
                                (Math.random() - 0.5) * 16, 15 + Math.random() * 5,
                                (Math.random() - 0.5) * 16);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 4, 1.0, 0.5, 1.0, 0.02);
                    }
                }
                return;
            }

            // Bass burst
            if (!burstFired) {
                burstFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.1f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 1.5f, 0.2f);

                // Knock airborne players horizontally
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (!p.isOnGround()) {
                        double knockAngle = Math.random() * 2 * Math.PI;
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(
                                Math.cos(knockAngle) * 0.5, 0, Math.sin(knockAngle) * 0.5)));
                    }
                }
            }

            // Post-burst: doubled particle emission for 4 seconds
            int burstTick = ticksAlive - 60;
            if (burstTick <= 80 && burstTick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 10;
                    Location pos = center.clone().add(Math.cos(a) * d, Math.random() * 15,
                            Math.sin(a) * d);
                    int type = (int) (Math.random() * 5);
                    switch (type) {
                        case 0: w.spawnParticle(Particle.WITCH, pos, 3, 0.5, 0.5, 0.5, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, pos, 2, 0.5, 0.5, 0.5, 0.01); break;
                        case 2: w.spawnParticle(Particle.END_ROD, pos, 3, 0.5, 0.5, 0.5, 0.005); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, pos, 3, 0.5, 0.5, 0.5, 0.005); break;
                        default: DisplayBuilder.dustParticles(pos, 3, 0.5, 180, 0, 0, 1.0f); break;
                    }
                }
            }

            // Low rumble continues
            if (burstTick == 40) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.15f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SubsonicPressure(plugin); }
    }

    // =========================================================================
    // 145. CRIMSON GEYSER FIELD -- 8 geysers with delayed second eruption
    // =========================================================================
    public static class CrimsonGeyserField extends EnvironmentalAttack {

        private final List<Location> geyserTiles = new ArrayList<>();
        private boolean firstEruption = false;
        private boolean secondEruption = false;

        public CrimsonGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_geyser_field", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(2000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 2 + Math.random() * 7;
                geyserTiles.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- wisps rise from marked tiles
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.dustParticles(tile.clone().add(0, 0.3, 0),
                                4, 0.3, 180, 0, 0, 1.5f);
                    }
                }
                if (ticksAlive == 50) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.playSound(tile, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.5f);
                    }
                }
                return;
            }

            int eruptTick = ticksAlive - 60;

            // First eruption at tick 0
            if (!firstEruption) {
                firstEruption = true;
                for (Location tile : geyserTiles) {
                    DisplayBuilder.playSound(tile, Sound.ENTITY_BLAZE_BURN, 1.2f, 0.6f);
                }
            }

            // First geyser columns
            if (eruptTick <= 30) {
                double height = Math.min(eruptTick * 0.5, 14);
                if (eruptTick % 2 == 0) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.dustParticles(tile.clone().add(0, height / 2, 0),
                                12, 0.75, 180, 0, 0, 2.0f);
                        DisplayBuilder.dustParticles(tile.clone().add(0, height / 2, 0),
                                6, 0.75, 220, 40, 0, 1.5f);
                    }
                }

                if (eruptTick % 10 == 0) {
                    for (Location tile : geyserTiles) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - tile.getX();
                            double dz = p.getLocation().getZ() - tile.getZ();
                            if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= center.getY() + height) {
                                p.damage(18.0);
                            }
                        }
                    }
                }
            }

            // Residual emission between eruptions
            if (eruptTick > 30 && eruptTick < 80) {
                if (eruptTick % 5 == 0) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.dustParticles(tile.clone().add(0, 0.3, 0),
                                3, 0.5, 180, 0, 0, 1.0f);
                    }
                }
            }

            // Second eruption at tick 80 -- no warning
            if (eruptTick == 80 && !secondEruption) {
                secondEruption = true;
                for (Location tile : geyserTiles) {
                    DisplayBuilder.playSound(tile, Sound.ENTITY_BLAZE_BURN, 1.4f, 0.7f);
                }
            }

            if (eruptTick >= 80 && eruptTick <= 110) {
                double height = Math.min((eruptTick - 80) * 0.5, 14);
                if (eruptTick % 2 == 0) {
                    for (Location tile : geyserTiles) {
                        DisplayBuilder.dustParticles(tile.clone().add(0, height / 2, 0),
                                12, 0.75, 180, 0, 0, 2.0f);
                    }
                }

                if (eruptTick % 10 == 0) {
                    for (Location tile : geyserTiles) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - tile.getX();
                            double dz = p.getLocation().getZ() - tile.getZ();
                            if (dx * dx + dz * dz <= 2.25) {
                                p.damage(18.0);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonGeyserField(plugin); }
    }

    // =========================================================================
    // 146. VOID PILLAR REMEMBRANCE -- 3 void pillars from Voidmaw's domain
    // =========================================================================
    public static class VoidPillarRemembrance extends EnvironmentalAttack {

        private final List<Location> pillarSites = new ArrayList<>();
        private boolean pillarsActive = false;

        public VoidPillarRemembrance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pillar_remembrance", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(3200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 3; i++) {
                double a = (2 * Math.PI * i) / 3 + Math.random() * 0.5;
                pillarSites.add(center.clone().add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks
            if (ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    for (Location site : pillarSites) {
                        DisplayBuilder.dustParticles(site.clone().add(0, 0.3, 0),
                                4, 1.0, 20, 0, 40, 1.5f);
                        w.spawnParticle(Particle.END_ROD, site.clone().add(0, ticksAlive * 0.2, 0),
                                6, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                if (ticksAlive == 70) {
                    for (Location site : pillarSites) {
                        DisplayBuilder.playSound(site, Sound.ENTITY_ENDERMAN_AMBIENT, 1.2f, 0.3f);
                    }
                }
                return;
            }

            // Pillar eruption
            if (!pillarsActive) {
                pillarsActive = true;
                for (Location site : pillarSites) {
                    DisplayBuilder.playSound(site, Sound.ENTITY_ENDERMAN_TELEPORT, 1.4f, 0.4f);
                }
            }

            int pillarTick = ticksAlive - 80;

            // Pillars hold for 100 ticks
            if (pillarTick <= 100) {
                double spinAngle = pillarTick * Math.toRadians(5);
                if (pillarTick % 2 == 0) {
                    for (Location site : pillarSites) {
                        for (int y = 0; y <= 22; y += 2) {
                            double ox = Math.cos(spinAngle + y * 0.1) * 0.5;
                            double oz = Math.sin(spinAngle + y * 0.1) * 0.5;
                            DisplayBuilder.dustParticles(site.clone().add(ox, y, oz),
                                    20, 1.0, 10, 0, 30, 2.5f);
                        }
                    }
                }

                // Contact damage
                if (pillarTick % 20 == 0) {
                    for (Location site : pillarSites) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - site.getX();
                            double dz = p.getLocation().getZ() - site.getZ();
                            if (dx * dx + dz * dz <= 4.0) {
                                p.damage(20.0);
                            }
                        }
                    }
                }
            }

            // Dissolution: debris falls
            if (pillarTick > 100 && pillarTick <= 140) {
                if (pillarTick % 3 == 0) {
                    for (Location site : pillarSites) {
                        double fallY = 22 - (pillarTick - 100) * 0.5;
                        DisplayBuilder.dustParticles(site.clone().add(0, Math.max(fallY, 0), 0),
                                6, 1.0, 5, 0, 15, 1.5f);
                    }
                }
                if (pillarTick == 120) {
                    for (Location site : pillarSites) {
                        DisplayBuilder.playSound(site, Sound.ENTITY_ENDERMAN_DEATH, 0.8f, 0.5f);
                    }
                }
            }

            // Void stain patches persist
            if (pillarTick > 140 && pillarTick % 10 == 0) {
                for (Location site : pillarSites) {
                    DisplayBuilder.dustParticles(site.clone().add(0, 0.2, 0),
                            2, 0.8, 10, 0, 20, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPillarRemembrance(plugin); }
    }

    // =========================================================================
    // 147. MAGENTA STORM SURGE -- SPELL_WITCH downpour from sky
    // =========================================================================
    public static class MagentaStormSurge extends EnvironmentalAttack {

        private boolean stormActive = false;

        public MagentaStormSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magenta_storm_surge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(2600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- sky magenta quadruples
            if (ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < (int) (ticksAlive / 10.0); i++) {
                        Location skyPos = center.clone().add(
                                (Math.random() - 0.5) * 20, 20 + Math.random() * 5,
                                (Math.random() - 0.5) * 20);
                        w.spawnParticle(Particle.WITCH, skyPos, 8, 1.0, 0.5, 1.0, 0);
                    }
                }
                if (ticksAlive == 70) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.6f);
                }
                return;
            }

            // Full downpour: 240 ticks (12 seconds)
            if (!stormActive) {
                stormActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_DRINK, 1.2f, 0.5f);
            }

            int stormTick = ticksAlive - 80;

            if (stormTick <= 240) {
                // Rainfall pattern across arena
                if (stormTick % 2 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double ox = (Math.random() - 0.5) * 20;
                        double oz = (Math.random() - 0.5) * 20;
                        double drift = (Math.random() - 0.5) * 2;
                        Location rainPos = center.clone().add(ox + drift, 24 - (stormTick % 48) * 0.5, oz);
                        w.spawnParticle(Particle.WITCH, rainPos, 3, 0.2, 0.5, 0.2, 0);
                    }
                }

                // Damage from dense contact zones
                if (stormTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 100.0) {
                            if (Math.random() < 0.4) {
                                p.damage(16.0);
                            }
                        }
                    }
                }

                // Rain ambient
                if (stormTick % 60 == 0) {
                    DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.7f);
                }
            }

            // Rain end
            if (stormTick == 240) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagentaStormSurge(plugin); }
    }

    // =========================================================================
    // 148. GROUND ZERO PULSE -- triple expanding shockwave rings from center
    // =========================================================================
    public static class GroundZeroPulse extends EnvironmentalAttack {

        private int ringsFired = 0;

        public GroundZeroPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_zero_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(2800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- expanding pulse markers
            if (ticksAlive <= 60) {
                double pulseRadius = (ticksAlive % 20) * 0.3;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), pulseRadius,
                            Particle.DUST, (int) (pulseRadius * 4),
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.0f));
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.3f);
                }
                return;
            }

            int pulseTick = ticksAlive - 60;

            // Three rings: at tick 0, 20, 40
            int[] ringTimes = {0, 20, 40};
            float[] ringPitches = {0.35f, 0.4f, 0.45f};

            for (int r = 0; r < 3; r++) {
                if (pulseTick >= ringTimes[r] && pulseTick <= ringTimes[r] + 10) {
                    double radius = (pulseTick - ringTimes[r]) * 2.0;

                    if (pulseTick == ringTimes[r] && ringsFired <= r) {
                        ringsFired = r + 1;
                        float vol = 1.8f - r * 0.3f;
                        DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, vol, ringPitches[r]);
                    }

                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius,
                            Particle.DUST, (int) (radius * 6),
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.5f));

                    // Damage players in ring
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = Math.sqrt(
                                Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                        if (Math.abs(dist - radius) <= 2.0) {
                            p.damage(16.0);
                        }
                    }
                }
            }

            // Permanent center scar
            if (pulseTick > 50 && pulseTick % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0),
                        5, 1.0, 180, 0, 0, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundZeroPulse(plugin); }
    }

    // =========================================================================
    // 149. FINAL HOUR CONVERGENCE -- all permanent effects double, hazards reactivate
    // =========================================================================
    public static class FinalHourConvergence extends EnvironmentalAttack {

        private boolean convergenceFired = false;

        public FinalHourConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_hour_convergence", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 160 ticks -- everything intensifies to double
            if (ticksAlive <= 160) {
                double mult = 1.0 + ticksAlive / 160.0;
                if (ticksAlive % 3 == 0) {
                    // All sky streams descend further
                    for (int i = 0; i < (int) (5 * mult); i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 10;
                        Location streamPos = center.clone().add(Math.cos(a) * d,
                                20 - ticksAlive * 0.03, Math.sin(a) * d);
                        int type = (int) (Math.random() * 5);
                        switch (type) {
                            case 0: w.spawnParticle(Particle.WITCH, streamPos, 4, 0.5, 0.5, 0.5, 0); break;
                            case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 3, 0.5, 0.5, 0.5, 0.01); break;
                            case 2: w.spawnParticle(Particle.END_ROD, streamPos, 4, 0.5, 0.5, 0.5, 0.005); break;
                            case 3: w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 3, 0.5, 0.5, 0.5, 0.005); break;
                            default: DisplayBuilder.dustParticles(streamPos, 3, 0.5, 180, 0, 0, 1.0f); break;
                        }
                    }

                    // Soul fire at floor
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(
                            (Math.random() - 0.5) * 16, 0.3, (Math.random() - 0.5) * 16),
                            (int) (5 * mult), 1.0, 0.2, 1.0, 0.01);
                }

                if (ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.3f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
                }
                return;
            }

            // Maximum intensity pulse at tick 160
            if (!convergenceFired) {
                convergenceFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.3f);
            }

            int convTick = ticksAlive - 160;

            // 5-second visual flood: all particles compressed into single tick bursts
            if (convTick <= 100 && convTick % 4 == 0) {
                // Arena-wide particle flood
                for (int i = 0; i < 30; i++) {
                    double ox = (Math.random() - 0.5) * 18;
                    double oz = (Math.random() - 0.5) * 18;
                    Location floodPos = center.clone().add(ox, Math.random() * 10, oz);
                    switch ((int) (Math.random() * 5)) {
                        case 0: w.spawnParticle(Particle.WITCH, floodPos, 5, 0.5, 0.5, 0.5, 0); break;
                        case 1: w.spawnParticle(Particle.TOTEM_OF_UNDYING, floodPos, 4, 0.5, 0.5, 0.5, 0.02); break;
                        case 2: w.spawnParticle(Particle.END_ROD, floodPos, 5, 0.5, 0.5, 0.5, 0.01); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, floodPos, 4, 0.5, 0.5, 0.5, 0.01); break;
                        default: DisplayBuilder.dustParticles(floodPos, 4, 0.5, 180, 0, 0, 1.5f); break;
                    }
                }
            }

            // Hazard re-activation window (3 seconds)
            if (convTick >= 100 && convTick <= 160 && convTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100.0) {
                        // Residual hazard damage
                        if (Math.random() < 0.3) {
                            p.damage(10.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalHourConvergence(plugin); }
    }

    // =========================================================================
    // 150. THE LAST BREATH -- final environmental attack before death
    // =========================================================================
    public static class TheLastBreath extends EnvironmentalAttack {

        private boolean silencePhase = true;
        private boolean stormPhase = false;
        private int exponentialRate = 1;

        public TheLastBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_last_breath", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Complete silence (0-100 ticks = 5 seconds)
            if (ticksAlive <= 100) {
                // Nothing. No particles. No sound. Just the stillness.
                return;
            }

            // Phase 2: Exponential buildup from Calamitas' body (100-200 ticks)
            if (ticksAlive <= 200) {
                int buildTick = ticksAlive - 100;

                // Exponential doubling every 10 ticks
                if (buildTick % 10 == 0) {
                    exponentialRate = Math.min(exponentialRate * 2, 512);
                }

                if (buildTick % 2 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                            Math.min(exponentialRate, 100), 3.0, 180, 0, 0, 2.0f);
                }

                if (buildTick == 50) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.2f);
                }
                if (buildTick == 80) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.2f);
                }
                return;
            }

            // Phase 3: Total particle storm (200-300 ticks = 5 seconds)
            if (!stormPhase) {
                stormPhase = true;
                silencePhase = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.3f);
            }

            int stormTick = ticksAlive - 200;

            if (stormTick <= 100) {
                // ALL particle types at maximum across 15-block sphere
                if (stormTick % 2 == 0) {
                    for (int i = 0; i < 40; i++) {
                        double ox = (Math.random() - 0.5) * 30;
                        double oy = Math.random() * 15;
                        double oz = (Math.random() - 0.5) * 30;
                        Location stormPos = center.clone().add(ox, oy, oz);

                        DisplayBuilder.dustParticles(stormPos, 8, 1.0, 180, 0, 0, 3.0f);
                        w.spawnParticle(Particle.WITCH, stormPos, 4, 0.5, 0.5, 0.5, 0);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, stormPos, 3, 0.5, 0.5, 0.5, 0.02);
                        w.spawnParticle(Particle.END_ROD, stormPos, 4, 0.5, 0.5, 0.5, 0.01);
                        w.spawnParticle(Particle.DRAGON_BREATH, stormPos, 3, 0.5, 0.5, 0.5, 0.01);
                    }
                }

                // Storm damage every second
                if (stormTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        p.damage(10.0);
                    }
                }
            }

            // Phase 4: Everything cuts to zero (tick 300+)
            if (stormTick == 100) {
                // Complete silence. Complete void. Boss is killable.
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastBreath(plugin); }
    }
}
