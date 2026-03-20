package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Doom Mode — MODEL ENGINE ATTACKS
 * 10 ModelEngine VFX attacks using hellfire/doom themed .bbmodel files.
 *
 * Doom palette for particles:
 * - LAVA, FLAME, SMOKE
 * - DUST with Color(255,80,20), Color(200,40,10), Color(100,20,5)
 * - Sounds: BLOCK_LAVA_POP, ENTITY_BLAZE_SHOOT, ENTITY_GENERIC_EXPLODE,
 *           ENTITY_ENDER_DRAGON_GROWL, BLOCK_FIRE_AMBIENT
 */
public final class DoomModelEngine {
    private DoomModelEngine() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HellmouthRupture(plugin));
        registry.register(new BrimstoneGeyser(plugin));
        registry.register(new HellfireCrackArray(plugin));
        registry.register(new InfernalNova(plugin));
        registry.register(new DamnationRing(plugin));
        registry.register(new ApocalypseShockwave(plugin));
        registry.register(new PerditionLance(plugin));
        registry.register(new TheCondemnation(plugin));
        registry.register(new TheThreshold(plugin));
        registry.register(new KingOfRuin(plugin));
    }

    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) { nearestDist = dist; nearest = p; }
        }
        return nearest;
    }

    // ================================================================
    // 1. HELLMOUTH RUPTURE — Ground opens like a mouth with teeth
    // ================================================================
    public static class HellmouthRupture extends ModelEngineAttack {
        public HellmouthRupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellmouth_rupture", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(50.0); config.setDamageRadius(6.0);
            config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected String getModelId() { return "hellmouth_rupture"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.3f);
            if (c.getWorld() != null) c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 2, 0), 30, 2, 1, 2, 0.1);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            // Jaw damage — players between the teeth
            if (tick % 20 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 36) {
                        p.damage(config.getDamage() * 0.5); p.setNoDamageTicks(0);
                    }
                }
                getCenter().getWorld().spawnParticle(Particle.FLAME, getCenter().clone().add(0, 1, 0), 15, 2, 0.5, 2, 0.03);
            }
        }
        @Override public AbstractAttack newInstance() { return new HellmouthRupture(plugin); }
    }

    // ================================================================
    // 2. BRIMSTONE GEYSER — Erupting column of hellfire
    // ================================================================
    public static class BrimstoneGeyser extends ModelEngineAttack {
        public BrimstoneGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_geyser", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(40.0); config.setDamageRadius(5.0);
            config.setDurationTicks(300); config.setCooldownTicks(250);
        }
        @Override protected String getModelId() { return "brimstone_geyser"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.4f);
            if (c.getWorld() != null) c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 8, 0), 40, 1, 3, 1, 0.2);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 15 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dx = p.getLocation().getX() - getCenter().getX();
                    double dz = p.getLocation().getZ() - getCenter().getZ();
                    if (dx * dx + dz * dz <= 25) { // 5 block XZ radius
                        p.damage(config.getDamage() * 0.4); p.setNoDamageTicks(0);
                    }
                }
                getCenter().getWorld().spawnParticle(Particle.FLAME, getCenter().clone().add(0, 6, 0), 20, 1, 2, 1, 0.05);
            }
        }
        @Override public AbstractAttack newInstance() { return new BrimstoneGeyser(plugin); }
    }

    // ================================================================
    // 3. HELLFIRE CRACK ARRAY — Fan of 5 cracks in the ground
    // ================================================================
    public static class HellfireCrackArray extends ModelEngineAttack {
        public HellfireCrackArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_crack_array", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(30.0); config.setDamageRadius(8.0);
            config.setDurationTicks(350); config.setCooldownTicks(280);
        }
        @Override protected String getModelId() { return "hellfire_crack_array"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            // Crack line damage — 2 block wide along Z axis
            if (tick % 20 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dx = Math.abs(p.getLocation().getX() - getCenter().getX());
                    if (dx <= 2 && p.getLocation().distanceSquared(getCenter()) <= 100) {
                        p.damage(config.getDamage() * 0.3); p.setNoDamageTicks(0);
                    }
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new HellfireCrackArray(plugin); }
    }

    // ================================================================
    // 4. INFERNAL NOVA — Sphere detonation with compression charge
    // ================================================================
    public static class InfernalNova extends ModelEngineAttack {
        private boolean detonated = false;
        public InfernalNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_nova", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(60.0); config.setDamageRadius(8.0);
            config.setDurationTicks(200); config.setCooldownTicks(350);
        }
        @Override protected String getModelId() { return "infernal_nova"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            detonated = false;
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            // Detonation at tick 30
            if (tick == 30 && !detonated) {
                detonated = true;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
                getCenter().getWorld().spawnParticle(Particle.FLAME, getCenter().clone().add(0, 5, 0), 60, 4, 4, 4, 0.1);
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 64) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
            if (tick % 30 == 0 && detonated) {
                getCenter().getWorld().spawnParticle(Particle.SMOKE, getCenter().clone().add(0, 5, 0), 10, 3, 3, 3, 0.02);
            }
        }
        @Override public AbstractAttack newInstance() { return new InfernalNova(plugin); }
    }

    // ================================================================
    // 5. DAMNATION RING — Summoning circle of hellfire
    // ================================================================
    public static class DamnationRing extends ModelEngineAttack {
        public DamnationRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("damnation_ring", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(20.0); config.setDamageRadius(7.0);
            config.setDurationTicks(500); config.setCooldownTicks(400);
        }
        @Override protected String getModelId() { return "damnation_ring"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 25 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distanceSquared(getCenter());
                    // Inside ring = damage
                    if (dist <= 49 && dist >= 16) {
                        p.damage(config.getDamage() * 0.4); p.setNoDamageTicks(0);
                    }
                    // Center sigil = more damage
                    if (dist < 9) {
                        p.damage(config.getDamage() * 0.6); p.setNoDamageTicks(0);
                    }
                }
                DisplayBuilder.particleRing(getCenter().clone().add(0, 0.5, 0), 5.0, Particle.FLAME, 20, null);
            }
        }
        @Override public AbstractAttack newInstance() { return new DamnationRing(plugin); }
    }

    // ================================================================
    // 6. APOCALYPSE SHOCKWAVE — Flat ring of destruction
    // ================================================================
    public static class ApocalypseShockwave extends ModelEngineAttack {
        public ApocalypseShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("apocalypse_shockwave", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(50.0); config.setDamageRadius(10.0);
            config.setDurationTicks(150); config.setCooldownTicks(300);
        }
        @Override protected String getModelId() { return "apocalypse_shockwave"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.2f);
            if (c.getWorld() != null) {
                c.getWorld().spawnParticle(Particle.FLAME, c, 50, 5, 0.5, 5, 0.1);
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 8.0, Particle.SMOKE, 30, null);
            }
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick == 5) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 100) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new ApocalypseShockwave(plugin); }
    }

    // ================================================================
    // 7. PERDITION LANCE — Forged hellfire spear projectile
    // ================================================================
    public static class PerditionLance extends ModelEngineAttack {
        public PerditionLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("perdition_lance", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(45.0); config.setDamageRadius(4.0);
            config.setDurationTicks(100); config.setCooldownTicks(200);
            config.setTracksPlayer(true);
        }
        @Override protected String getModelId() { return "perdition_lance"; }
        @Override protected double getModelScale() { return 1.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            Player target = findNearestPlayer(getCenter(), 15);
            if (target != null && tick % 10 == 0) {
                if (target.getLocation().distanceSquared(getCenter()) <= 16) {
                    target.damage(config.getDamage()); target.setNoDamageTicks(0);
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PLAYER_HURT, 0.8f, 0.5f);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new PerditionLance(plugin); }
    }

    // ================================================================
    // 8. THE CONDEMNATION — Wide sustained hellfire beam
    // ================================================================
    public static class TheCondemnation extends ModelEngineAttack {
        public TheCondemnation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_condemnation", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(30.0); config.setDamageRadius(6.0);
            config.setDurationTicks(250); config.setCooldownTicks(350);
        }
        @Override protected String getModelId() { return "the_condemnation"; }
        @Override protected double getModelScale() { return 1.8; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 10 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    // Beam along Z axis, 3 blocks wide
                    double dx = Math.abs(p.getLocation().getX() - getCenter().getX());
                    double dz = p.getLocation().getZ() - getCenter().getZ();
                    if (dx <= 3 && dz >= 0 && dz <= 15) {
                        p.damage(config.getDamage() * 0.3); p.setNoDamageTicks(0);
                    }
                }
                getCenter().getWorld().spawnParticle(Particle.FLAME,
                        getCenter().clone().add(0, 1, 7), 20, 1.5, 0.5, 3, 0.03);
            }
        }
        @Override public AbstractAttack newInstance() { return new TheCondemnation(plugin); }
    }

    // ================================================================
    // 9. THE THRESHOLD — Demon gate portal
    // ================================================================
    public static class TheThreshold extends ModelEngineAttack {
        public TheThreshold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_threshold", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(25.0); config.setDamageRadius(5.0);
            config.setDurationTicks(600); config.setCooldownTicks(500);
        }
        @Override protected String getModelId() { return "the_threshold"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            if (c.getWorld() != null) c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 4, 0), 30, 1, 2, 1, 0.05);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            // Portal surface damage
            if (tick % 20 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 9) {
                        p.damage(config.getDamage() * 0.6); p.setNoDamageTicks(0);
                    }
                }
            }
            // Pillar proximity damage
            if (tick % 40 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 25) {
                        p.damage(config.getDamage() * 0.2); p.setNoDamageTicks(0);
                    }
                }
                getCenter().getWorld().spawnParticle(Particle.FLAME, getCenter().clone().add(0, 3, 0), 10, 0.5, 2, 0.5, 0.02);
            }
        }
        @Override public AbstractAttack newInstance() { return new TheThreshold(plugin); }
    }

    // ================================================================
    // 10. KING OF RUIN — Asymmetric infernal crown orbital
    // ================================================================
    public static class KingOfRuin extends ModelEngineAttack {
        public KingOfRuin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("king_of_ruin", AttackType.MODEL_ENGINE, 1, "modes/doom/attacks"));
            config.setDamage(35.0); config.setDamageRadius(6.0);
            config.setDurationTicks(400); config.setCooldownTicks(350);
            config.setTracksPlayer(true);
        }
        @Override protected String getModelId() { return "king_of_ruin"; }
        @Override protected double getModelScale() { return 2.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            // Crown spike contact damage
            if (tick % 15 == 0) {
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 36) {
                        p.damage(config.getDamage() * 0.3); p.setNoDamageTicks(0);
                    }
                }
            }
            // Crown pulse every 80 ticks
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 1.0f, 0.3f);
                for (Player p : getCenter().getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= 49) {
                        p.damage(config.getDamage() * 0.5); p.setNoDamageTicks(0);
                    }
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new KingOfRuin(plugin); }
    }
}
