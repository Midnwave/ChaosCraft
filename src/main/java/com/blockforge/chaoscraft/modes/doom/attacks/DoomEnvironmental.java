package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Doom Mode — ENVIRONMENTAL ATTACKS
 * Heat, tremor, and infernal environmental effects.
 * TODO: Implement full attack set (target: 30-50 attacks)
 *
 * These attacks use particles, sounds, and player effects — NO block displays.
 */
public final class DoomEnvironmental {
    private DoomEnvironmental() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HeatWave(plugin));
        registry.register(new EmberRain(plugin));
        registry.register(new GroundTremor(plugin));
    }

    // ================================================================
    // 1. HEAT WAVE — Pulsing heat that deals damage to all nearby players
    // ================================================================
    public static class HeatWave extends EnvironmentalAttack {
        public HeatWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_wave", AttackType.ENVIRONMENTAL, 1, "modes/doom/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.3f);
        }

        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 20 == 0) {
                World w = getCenter().getWorld();
                w.spawnParticle(Particle.FLAME, getCenter(), 30, 4, 1, 4, 0.02);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new HeatWave(plugin); }
    }

    // ================================================================
    // 2. EMBER RAIN — Falling embers from above dealing contact damage
    // ================================================================
    public static class EmberRain extends EnvironmentalAttack {
        public EmberRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_rain", AttackType.ENVIRONMENTAL, 1, "modes/doom/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.4f);
        }

        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 5 == 0) {
                World w = getCenter().getWorld();
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 16;
                    double oz = (Math.random() - 0.5) * 16;
                    Location drop = getCenter().clone().add(ox, 15, oz);
                    w.spawnParticle(Particle.LAVA, drop, 3, 0.5, 0.5, 0.5, 0.1);
                    w.spawnParticle(Particle.FALLING_LAVA, drop, 2, 0.3, 0.3, 0.3, 0);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new EmberRain(plugin); }
    }

    // ================================================================
    // 3. GROUND TREMOR — Screen shake effect + particles + sound
    // ================================================================
    public static class GroundTremor extends EnvironmentalAttack {
        public GroundTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_tremor", AttackType.ENVIRONMENTAL, 1, "modes/doom/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
        }

        @Override protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.3f);
            center.getWorld().spawnParticle(Particle.BLOCK, center, 40, 6, 0.5, 6, 0.1,
                    Material.NETHERRACK.createBlockData());
        }

        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            if (tick % 4 == 0) {
                getCenter().getWorld().spawnParticle(Particle.BLOCK, getCenter(), 15, 5, 0.3, 5, 0.05,
                        Material.MAGMA_BLOCK.createBlockData());
            }
        }

        @Override public AbstractAttack newInstance() { return new GroundTremor(plugin); }
    }
}
