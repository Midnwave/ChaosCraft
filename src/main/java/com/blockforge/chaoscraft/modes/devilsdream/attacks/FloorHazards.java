package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Floor Hazards — ground-level environmental attacks.
 * Lava cracks, soul sand patches, fire trails, and treacherous terrain.
 */
public class FloorHazards {

    private FloorHazards() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LavaCracks(plugin));
        registry.register(new SoulSandPatch(plugin));
        registry.register(new FireTrail(plugin));
        registry.register(new MagmaBubble(plugin));
        registry.register(new BrimstoneFloor(plugin));
        registry.register(new CrumblingGround(plugin));
        registry.register(new NightmareQuicksand(plugin));
        registry.register(new HellfireGeyserLine(plugin));
        registry.register(new SoulFireRing(plugin));
        registry.register(new DarkIce(plugin));
        registry.register(new BurningFootprints(plugin));
        registry.register(new CorrosivePool(plugin));
        registry.register(new NetherFissure(plugin));
    }

    public static class LavaCracks extends EnvironmentalAttack {
        public LavaCracks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_cracks", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new LavaCracks(plugin); }
    }

    public static class SoulSandPatch extends EnvironmentalAttack {
        public SoulSandPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_sand_patch", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new SoulSandPatch(plugin); }
    }

    public static class FireTrail extends EnvironmentalAttack {
        public FireTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_trail", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new FireTrail(plugin); }
    }

    public static class MagmaBubble extends EnvironmentalAttack {
        public MagmaBubble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_bubble", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new MagmaBubble(plugin); }
    }

    public static class BrimstoneFloor extends EnvironmentalAttack {
        public BrimstoneFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_floor", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new BrimstoneFloor(plugin); }
    }

    public static class CrumblingGround extends EnvironmentalAttack {
        public CrumblingGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumbling_ground", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new CrumblingGround(plugin); }
    }

    public static class NightmareQuicksand extends EnvironmentalAttack {
        public NightmareQuicksand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_quicksand", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightmareQuicksand(plugin); }
    }

    public static class HellfireGeyserLine extends EnvironmentalAttack {
        public HellfireGeyserLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_geyser_line", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HellfireGeyserLine(plugin); }
    }

    public static class SoulFireRing extends EnvironmentalAttack {
        public SoulFireRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_ring", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new SoulFireRing(plugin); }
    }

    public static class DarkIce extends EnvironmentalAttack {
        public DarkIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_ice", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DarkIce(plugin); }
    }

    public static class BurningFootprints extends EnvironmentalAttack {
        public BurningFootprints(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burning_footprints", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new BurningFootprints(plugin); }
    }

    public static class CorrosivePool extends EnvironmentalAttack {
        public CorrosivePool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrosive_pool", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new CorrosivePool(plugin); }
    }

    public static class NetherFissure extends EnvironmentalAttack {
        public NetherFissure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_fissure", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NetherFissure(plugin); }
    }
}
