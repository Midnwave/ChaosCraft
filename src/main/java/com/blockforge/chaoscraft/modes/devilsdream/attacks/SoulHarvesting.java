package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Soul Harvesting — soul-draining and spirit-based environmental attacks.
 * Soul drains, ghost touches, spirit swarms, and ectoplasmic effects.
 */
public class SoulHarvesting {

    private SoulHarvesting() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulDrain(plugin));
        registry.register(new GhostTouch(plugin));
        registry.register(new SpiritSwarm(plugin));
        registry.register(new SoulSiphon(plugin));
        registry.register(new ReaperMark(plugin));
        registry.register(new SoulStorm(plugin));
        registry.register(new PhantomChains(plugin));
        registry.register(new SpiritBurst(plugin));
        registry.register(new GhostWail(plugin));
        registry.register(new SoulTrap(plugin));
        registry.register(new AncestralWrath(plugin));
        registry.register(new EctoplasmicWave(plugin));
        registry.register(new SoulFunnel(plugin));
    }

    public static class SoulDrain extends EnvironmentalAttack {
        public SoulDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulDrain(plugin); }
    }

    public static class GhostTouch extends EnvironmentalAttack {
        public GhostTouch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_touch", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GhostTouch(plugin); }
    }

    public static class SpiritSwarm extends EnvironmentalAttack {
        public SpiritSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spirit_swarm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SpiritSwarm(plugin); }
    }

    public static class SoulSiphon extends EnvironmentalAttack {
        public SoulSiphon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_siphon", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulSiphon(plugin); }
    }

    public static class ReaperMark extends EnvironmentalAttack {
        public ReaperMark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reaper_mark", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ReaperMark(plugin); }
    }

    public static class SoulStorm extends EnvironmentalAttack {
        public SoulStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulStorm(plugin); }
    }

    public static class PhantomChains extends EnvironmentalAttack {
        public PhantomChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_chains", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PhantomChains(plugin); }
    }

    public static class SpiritBurst extends EnvironmentalAttack {
        public SpiritBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spirit_burst", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SpiritBurst(plugin); }
    }

    public static class GhostWail extends EnvironmentalAttack {
        public GhostWail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_wail", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GhostWail(plugin); }
    }

    public static class SoulTrap extends EnvironmentalAttack {
        public SoulTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_trap", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulTrap(plugin); }
    }

    public static class AncestralWrath extends EnvironmentalAttack {
        public AncestralWrath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ancestral_wrath", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AncestralWrath(plugin); }
    }

    public static class EctoplasmicWave extends EnvironmentalAttack {
        public EctoplasmicWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ectoplasmic_wave", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EctoplasmicWave(plugin); }
    }

    public static class SoulFunnel extends EnvironmentalAttack {
        public SoulFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_funnel", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulFunnel(plugin); }
    }
}
