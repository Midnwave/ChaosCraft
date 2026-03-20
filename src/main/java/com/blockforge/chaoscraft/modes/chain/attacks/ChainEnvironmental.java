package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Chain Mode — ENVIRONMENTAL ATTACKS
 * 40 chain-themed environmental attacks using particles, sounds, and velocity.
 * NO block displays, NO potion effects.
 *
 * Color palette:
 * - Iron gray: RGB(150, 150, 160)
 * - Rust orange: RGB(180, 100, 50)
 * - Dark steel: RGB(80, 80, 90)
 * - Spark white: RGB(220, 220, 230)
 */
public final class ChainEnvironmental {

    private ChainEnvironmental() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Section 1: Chain Forces (14)
        registry.register(new ChainPull(plugin));
        registry.register(new ChainPush(plugin));
        registry.register(new ChainSlam(plugin));
        registry.register(new ChainLaunch(plugin));
        registry.register(new ChainWhip(plugin));
        registry.register(new AnchorDrop(plugin));
        registry.register(new ChainDrag(plugin));
        registry.register(new ShackleBind(plugin));
        registry.register(new IronRain(plugin));
        registry.register(new ChainVortex(plugin));
        registry.register(new LinkSnap(plugin));
        registry.register(new RustWave(plugin));
        registry.register(new MetalScreech(plugin));
        registry.register(new ChainTremor(plugin));
        // Section 2: Industrial Hazards (13)
        registry.register(new GrindingZone(plugin));
        registry.register(new FurnaceBlast(plugin));
        registry.register(new MoltenSplash(plugin));
        registry.register(new SteamVent(plugin));
        registry.register(new GearCrush(plugin));
        registry.register(new RivetBurst(plugin));
        registry.register(new WeldFlash(plugin));
        registry.register(new ChainFence(plugin));
        registry.register(new AnvilStrike(plugin));
        registry.register(new SparkingGround(plugin));
        registry.register(new PistonSlam(plugin));
        registry.register(new ChainMine(plugin));
        registry.register(new IronDustCloud(plugin));
        // Section 3: Chain Weather (13)
        registry.register(new ChainStorm(plugin));
        registry.register(new MetalHail(plugin));
        registry.register(new ForgeWind(plugin));
        registry.register(new IronFog(plugin));
        registry.register(new SparkShower(plugin));
        registry.register(new ChainQuake(plugin));
        registry.register(new MetalFreeze(plugin));
        registry.register(new PressureWave(plugin));
        registry.register(new ChainCascadeEnv(plugin));
        registry.register(new ScrapTornado(plugin));
        registry.register(new StaticDischarge(plugin));
        registry.register(new ChainEcho(plugin));
        registry.register(new TotalLockdown(plugin));
    }

    // Helper: get survival, non-invulnerable players in radius
    private static List<Player> playersInRadius(Location center, double radius) {
        if (center.getWorld() == null) return Collections.emptyList();
        List<Player> result = new ArrayList<>();
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.isInvulnerable()
                    && p.getLocation().distanceSquared(center) <= r2) {
                result.add(p);
            }
        }
        return result;
    }

    // Helper: iron gray dust
    private static final Particle.DustOptions IRON_DUST = new Particle.DustOptions(Color.fromRGB(150, 150, 160), 1.2f);
    private static final Particle.DustOptions RUST_DUST = new Particle.DustOptions(Color.fromRGB(180, 100, 50), 1.2f);
    private static final Particle.DustOptions STEEL_DUST = new Particle.DustOptions(Color.fromRGB(80, 80, 90), 1.0f);
    private static final Particle.DustOptions SPARK_DUST = new Particle.DustOptions(Color.fromRGB(220, 220, 230), 1.0f);

    // ================================================================
    //  SECTION 1: CHAIN FORCES (14 attacks)
    // ================================================================

    // 1. CHAIN PULL — Pull all players toward center
    public static class ChainPull extends EnvironmentalAttack {
        public ChainPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pull", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(10.0);
            config.setDurationTicks(60); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.4f);
            DisplayBuilder.particleRing(center, 10.0, Particle.DUST, 32, IRON_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(c, 15, 5.0, 150, 150, 160, 1.2f);
                for (Player p : playersInRadius(c, 10.0)) {
                    Vector dir = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.3);
                    p.setVelocity(p.getVelocity().add(dir));
                }
            }
            if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 0.6f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainPull(plugin); }
    }

    // 2. CHAIN PUSH — Push all outward
    public static class ChainPush extends EnvironmentalAttack {
        public ChainPush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_push", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(5.0); config.setDamageRadius(12.0);
            config.setDurationTicks(20); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 30, 2.0, 150, 150, 160, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c, tick * 0.6, Particle.DUST, 24, IRON_DUST);
                for (Player p : playersInRadius(c, 12.0)) {
                    Vector dir = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.6);
                    p.setVelocity(p.getVelocity().add(dir));
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainPush(plugin); }
    }

    // 3. CHAIN SLAM — Downward velocity slam
    public static class ChainSlam extends EnvironmentalAttack {
        public ChainSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_slam", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(10.0); config.setDamageRadius(6.0);
            config.setDurationTicks(20); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 40, 3.0, 80, 80, 90, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick == 1) {
                for (Player p : playersInRadius(c, 6.0)) {
                    p.setVelocity(new Vector(0, -1.2, 0));
                }
                DisplayBuilder.particleRing(c, 6.0, Particle.DUST, 24, STEEL_DUST);
            }
            if (tick % 5 == 0) DisplayBuilder.dustParticles(c, 20, 3.0, 80, 80, 90, 1.2f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainSlam(plugin); }
    }

    // 4. CHAIN LAUNCH — Upward velocity, fall damage
    public static class ChainLaunch extends EnvironmentalAttack {
        public ChainLaunch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_launch", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setDurationTicks(20); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.2f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick == 1) {
                for (Player p : playersInRadius(c, 8.0)) {
                    p.setVelocity(new Vector(0, 1.5, 0));
                }
                DisplayBuilder.dustParticles(c, 30, 4.0, 220, 220, 230, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.5f);
            }
            if (tick % 4 == 0) DisplayBuilder.dustParticles(c, 10, 4.0, 150, 150, 160, 1.0f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainLaunch(plugin); }
    }

    // 5. CHAIN WHIP — Particle line sweeps across area
    public static class ChainWhip extends EnvironmentalAttack {
        private double angle = 0;
        public ChainWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whip", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(8.0); config.setDamageRadius(10.0);
            config.setDurationTicks(40); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            angle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            double sweep = angle + (tick / 40.0) * Math.PI;
            for (int i = 0; i < 20; i++) {
                double dist = i * 0.5;
                Location pt = c.clone().add(Math.cos(sweep) * dist, 1.0, Math.sin(sweep) * dist);
                DisplayBuilder.dustParticles(pt, 2, 0.1, 150, 150, 160, 1.3f);
            }
            if (tick % 5 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 1.2f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainWhip(plugin); }
    }

    // 6. ANCHOR DROP — Heavy impact at center with warning
    public static class AnchorDrop extends EnvironmentalAttack {
        public AnchorDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_drop", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(14.0); config.setDamageRadius(3.0);
            config.setDurationTicks(30); config.setCooldownTicks(300);
            config.setDamageDelayTicks(20); config.setTicksBetweenDamage(30);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick < 20) {
                // Warning phase: pulsing ring shrinking toward center
                double r = 3.0 * (1.0 - tick / 20.0);
                DisplayBuilder.particleRing(c, r, Particle.DUST, 16, RUST_DUST);
                if (tick % 5 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            } else if (tick == 20) {
                // Impact
                DisplayBuilder.dustParticles(c, 50, 1.5, 80, 80, 90, 2.0f);
                DisplayBuilder.particleRing(c, 3.0, Particle.DUST, 24, STEEL_DUST);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.5f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnchorDrop(plugin); }
    }

    // 7. CHAIN DRAG — Pull one direction for duration
    public static class ChainDrag extends EnvironmentalAttack {
        private Vector dragDir;
        public ChainDrag(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_drag", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(15.0);
            config.setDurationTicks(60); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            dragDir = new Vector(Math.cos(a), 0, Math.sin(a)).normalize().multiply(0.25);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                for (Player p : playersInRadius(c, 15.0)) {
                    p.setVelocity(p.getVelocity().add(dragDir));
                }
                // Particle trail in drag direction
                for (int i = 0; i < 10; i++) {
                    Location pt = c.clone().add(dragDir.clone().multiply(i * 2));
                    DisplayBuilder.dustParticles(pt, 3, 1.0, 150, 150, 160, 1.0f);
                }
            }
            if (tick % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainDrag(plugin); }
    }

    // 8. SHACKLE BIND — Players in zone can't sprint (speed attribute reduction)
    public static class ShackleBind extends EnvironmentalAttack {
        private final Set<UUID> slowed = new HashSet<>();
        public ShackleBind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shackle_bind", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(5.0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.3f);
            DisplayBuilder.dustParticles(center, 20, 2.5, 80, 80, 90, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 20, STEEL_DUST);
                for (Player p : playersInRadius(c, 5.0)) {
                    p.setWalkSpeed(0.06f); // Reduced from default 0.2
                    slowed.add(p.getUniqueId());
                }
            }
            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.4f);
        }
        @Override protected void onCleanup() {
            for (UUID id : slowed) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.setWalkSpeed(0.2f);
            }
            slowed.clear();
            super.onCleanup();
        }
        @Override public AbstractAttack newInstance() { return new ShackleBind(plugin); }
    }

    // 9. IRON RAIN — Gray particles raining, damage in zone
    public static class IronRain extends EnvironmentalAttack {
        public IronRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_rain", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(8.0);
            config.setDurationTicks(100); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * 16.0;
                    double oz = (Math.random() - 0.5) * 16.0;
                    Location drop = c.clone().add(ox, 10 + Math.random() * 5, oz);
                    DisplayBuilder.dustParticles(drop, 2, 0.3, 150, 150, 160, 1.0f);
                }
            }
            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.0f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronRain(plugin); }
    }

    // 10. CHAIN VORTEX — Swirling pull + orbital push, spiral inward
    public static class ChainVortex extends EnvironmentalAttack {
        public ChainVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_vortex", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(8.0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            // Spinning particle spiral
            double a = tick * 0.2;
            for (int i = 0; i < 3; i++) {
                double r = 8.0 * (1.0 - (double) tick / 80.0);
                double angle = a + i * (Math.PI * 2 / 3);
                Location pt = c.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                DisplayBuilder.dustParticles(pt, 3, 0.2, 150, 150, 160, 1.3f);
            }
            if (tick % 4 == 0) {
                for (Player p : playersInRadius(c, 8.0)) {
                    Vector toCenter = c.toVector().subtract(p.getLocation().toVector());
                    double dist = toCenter.length();
                    if (dist > 0.5) {
                        Vector pull = toCenter.normalize().multiply(0.2);
                        // Add orbital tangent
                        Vector tangent = new Vector(-toCenter.getZ(), 0, toCenter.getX()).normalize().multiply(0.15);
                        p.setVelocity(p.getVelocity().add(pull).add(tangent));
                    }
                }
            }
            if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.8f + tick * 0.01f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainVortex(plugin); }
    }

    // 11. LINK SNAP — Instant burst, chain break sound, AoE damage
    public static class LinkSnap extends EnvironmentalAttack {
        public LinkSnap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("link_snap", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(10.0); config.setDamageRadius(8.0);
            config.setDurationTicks(10); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.5f);
            DisplayBuilder.dustParticles(center, 60, 4.0, 220, 220, 230, 2.0f);
            DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 32, SPARK_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            DisplayBuilder.dustParticles(c, 15, 4.0, 150, 150, 160, 1.0f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LinkSnap(plugin); }
    }

    // 12. RUST WAVE — Orange particle wall sweeps through
    public static class RustWave extends EnvironmentalAttack {
        private Vector waveDir;
        public RustWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rust_wave", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(7.0); config.setDamageRadius(12.0);
            config.setDurationTicks(40); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            waveDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            double progress = tick * 0.6;
            Vector perp = new Vector(-waveDir.getZ(), 0, waveDir.getX());
            Location wallCenter = c.clone().add(waveDir.clone().multiply(progress - 12));
            for (int i = -8; i <= 8; i++) {
                for (int y = 0; y < 3; y++) {
                    Location pt = wallCenter.clone().add(perp.clone().multiply(i)).add(0, y, 0);
                    DisplayBuilder.dustParticles(pt, 1, 0.2, 180, 100, 50, 1.3f);
                }
            }
            if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.6f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RustWave(plugin); }
    }

    // 13. METAL SCREECH — Sound-based attack with damage
    public static class MetalScreech extends EnvironmentalAttack {
        public MetalScreech(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_screech", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(15.0);
            config.setDurationTicks(40); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.5f, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 2.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.2f, 1.5f + (float)(Math.random() * 0.5));
                // Concentric sound wave rings
                double r = (tick % 20) * 0.75;
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), r, Particle.DUST, 16, IRON_DUST);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MetalScreech(plugin); }
    }

    // 14. CHAIN TREMOR — Ground shake with random knockback
    public static class ChainTremor extends EnvironmentalAttack {
        public ChainTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tremor", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(5.0); config.setDamageRadius(12.0);
            config.setDurationTicks(30); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.3f);
            center.getWorld().spawnParticle(Particle.BLOCK, center, 40, 6, 0.3, 6, 0.1,
                    Material.IRON_BLOCK.createBlockData());
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c, 15, 5, 0.2, 5, 0.05,
                        Material.IRON_BLOCK.createBlockData());
                for (Player p : playersInRadius(c, 12.0)) {
                    double rx = (Math.random() - 0.5) * 0.3;
                    double rz = (Math.random() - 0.5) * 0.3;
                    p.setVelocity(p.getVelocity().add(new Vector(rx, 0.05, rz)));
                }
            }
            if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainTremor(plugin); }
    }

    // ================================================================
    //  SECTION 2: INDUSTRIAL HAZARDS (13 attacks)
    // ================================================================

    // 15. GRINDING ZONE — Spark particles, continuous damage
    public static class GrindingZone extends EnvironmentalAttack {
        public GrindingZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grinding_zone", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(5.0);
            config.setDurationTicks(120); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.5f);
            DisplayBuilder.particleRing(center, 5.0, Particle.DUST, 20, SPARK_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (Math.random() - 0.5) * 10.0;
                    double oz = (Math.random() - 0.5) * 10.0;
                    Location pt = c.clone().add(ox, 0.2, oz);
                    DisplayBuilder.dustParticles(pt, 3, 0.3, 220, 220, 230, 0.8f);
                }
            }
            if (tick % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.6f + (float)(Math.random() * 0.4));
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GrindingZone(plugin); }
    }

    // 16. FURNACE BLAST — Cone of spark particles from one direction
    public static class FurnaceBlast extends EnvironmentalAttack {
        private Vector blastDir;
        public FurnaceBlast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("furnace_blast", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setDurationTicks(30); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            blastDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 2 == 0) {
                Vector perp = new Vector(-blastDir.getZ(), 0, blastDir.getX());
                for (int i = 0; i < 8; i++) {
                    double dist = i * 1.0;
                    double spread = dist * 0.4;
                    Location pt = c.clone().add(blastDir.clone().multiply(dist))
                            .add(perp.clone().multiply((Math.random() - 0.5) * spread))
                            .add(0, Math.random() * 2, 0);
                    DisplayBuilder.dustParticles(pt, 2, 0.2, 220, 220, 230, 1.0f);
                    if (Math.random() > 0.7) DisplayBuilder.dustParticles(pt, 1, 0.1, 180, 100, 50, 1.2f);
                }
            }
            if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.8f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FurnaceBlast(plugin); }
    }

    // 17. MOLTEN SPLASH — Orange particle explosion, impact damage
    public static class MoltenSplash extends EnvironmentalAttack {
        public MoltenSplash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_splash", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(10.0); config.setDamageRadius(6.0);
            config.setDurationTicks(15); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 60, 3.0, 180, 100, 50, 2.0f);
            DisplayBuilder.dustParticles(center, 30, 2.0, 220, 220, 230, 1.5f);
            DisplayBuilder.particleRing(center, 6.0, Particle.DUST, 24, RUST_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double ox = (Math.random() - 0.5) * 8.0;
                    double oz = (Math.random() - 0.5) * 8.0;
                    DisplayBuilder.dustParticles(c.clone().add(ox, Math.random() * 3, oz), 2, 0.3, 180, 100, 50, 1.0f);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MoltenSplash(plugin); }
    }

    // 18. STEAM VENT — Upward cloud particles, damage + lift
    public static class SteamVent extends EnvironmentalAttack {
        public SteamVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("steam_vent", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(3.0);
            config.setDurationTicks(60); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CLOUD, c, 8, 0.5, 2, 0.5, 0.05);
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, 1.0, 150, 150, 160, 1.0f);
                for (Player p : playersInRadius(c, 3.0)) {
                    p.setVelocity(p.getVelocity().add(new Vector(0, 0.25, 0)));
                }
            }
            if (tick % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_EXTINGUISH, 0.7f, 1.0f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SteamVent(plugin); }
    }

    // 19. GEAR CRUSH — Two particle walls closing inward, damage when they meet
    public static class GearCrush extends EnvironmentalAttack {
        private Vector wallDir;
        public GearCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gear_crush", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(12.0); config.setDamageRadius(4.0);
            config.setDurationTicks(50); config.setCooldownTicks(300);
            config.setDamageDelayTicks(40); config.setTicksBetweenDamage(50);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            wallDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            double gap = 8.0 * (1.0 - (double) tick / 40.0);
            if (gap < 0) gap = 0;
            Vector perp = new Vector(-wallDir.getZ(), 0, wallDir.getX());
            // Two walls
            for (int side = -1; side <= 1; side += 2) {
                Location wallCenter = c.clone().add(wallDir.clone().multiply(side * gap));
                for (int i = -3; i <= 3; i++) {
                    for (int y = 0; y < 3; y++) {
                        Location pt = wallCenter.clone().add(perp.clone().multiply(i)).add(0, y, 0);
                        DisplayBuilder.dustParticles(pt, 1, 0.1, 80, 80, 90, 1.2f);
                    }
                }
            }
            if (tick == 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                DisplayBuilder.dustParticles(c, 40, 2.0, 220, 220, 230, 1.5f);
            }
            if (tick % 10 == 0 && tick < 40) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 0.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GearCrush(plugin); }
    }

    // 20. RIVET BURST — Small particle projectiles fly outward
    public static class RivetBurst extends EnvironmentalAttack {
        public RivetBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rivet_burst", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(5.0); config.setDamageRadius(10.0);
            config.setDurationTicks(20); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 1.8f);
            DisplayBuilder.dustParticles(center, 20, 0.5, 150, 150, 160, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            // Expanding projectile ring
            double r = tick * 0.5;
            if (r <= 10.0) {
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i + tick * 0.1;
                    Location pt = c.clone().add(Math.cos(angle) * r, 1.0, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pt, 2, 0.1, 220, 220, 230, 0.8f);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RivetBurst(plugin); }
    }

    // 21. WELD FLASH — Bright white flash, instant damage
    public static class WeldFlash extends EnvironmentalAttack {
        public WeldFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weld_flash", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setDurationTicks(5); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 2.0f);
            DisplayBuilder.dustParticles(center, 80, 4.0, 220, 220, 230, 2.5f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 255, 255, 255, 2.0f);
            DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 32, SPARK_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            DisplayBuilder.dustParticles(c, 20, 4.0, 220, 220, 230, 1.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new WeldFlash(plugin); }
    }

    // 22. CHAIN FENCE — Particle barrier, damage when crossing
    public static class ChainFence extends EnvironmentalAttack {
        private Vector fenceDir;
        public ChainFence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_fence", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(6.0);
            config.setDurationTicks(100); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI;
            fenceDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                for (int i = -6; i <= 6; i++) {
                    for (int y = 0; y < 4; y++) {
                        Location pt = c.clone().add(fenceDir.clone().multiply(i)).add(0, y, 0);
                        DisplayBuilder.dustParticles(pt, 1, 0.1, 150, 150, 160, 1.0f);
                    }
                }
            }
            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.7f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainFence(plugin); }
    }

    // 23. ANVIL STRIKE — Single heavy impact, small radius, warning particles
    public static class AnvilStrike extends EnvironmentalAttack {
        public AnvilStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anvil_strike", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(14.0); config.setDamageRadius(3.0);
            config.setDurationTicks(30); config.setCooldownTicks(300);
            config.setDamageDelayTicks(20); config.setTicksBetweenDamage(30);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick < 20) {
                // Warning: descending particle anvil shape
                double height = 8.0 * (1.0 - tick / 20.0);
                DisplayBuilder.dustParticles(c.clone().add(0, height, 0), 8, 0.5, 80, 80, 90, 1.5f);
                DisplayBuilder.particleRing(c, 3.0, Particle.DUST, 12, RUST_DUST);
                if (tick % 4 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.4f + tick * 0.05f);
            } else if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
                DisplayBuilder.dustParticles(c, 50, 1.5, 80, 80, 90, 2.0f);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 30, 1.5, 0.3, 1.5, 0.2,
                        Material.IRON_BLOCK.createBlockData());
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnvilStrike(plugin); }
    }

    // 24. SPARKING GROUND — Ground spark particles, standing = damage
    public static class SparkingGround extends EnvironmentalAttack {
        public SparkingGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sparking_ground", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(7.0);
            config.setDurationTicks(150); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.5f);
            DisplayBuilder.particleRing(center, 7.0, Particle.DUST, 24, SPARK_DUST);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 14.0;
                    double oz = (Math.random() - 0.5) * 14.0;
                    Location pt = c.clone().add(ox, 0.1, oz);
                    DisplayBuilder.dustParticles(pt, 3, 0.2, 220, 220, 230, 0.7f);
                }
            }
            if (tick % 25 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.2f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SparkingGround(plugin); }
    }

    // 25. PISTON SLAM — Sequential particle slams moving in a line
    public static class PistonSlam extends EnvironmentalAttack {
        private Vector slamDir;
        public PistonSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("piston_slam", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(8.0); config.setDamageRadius(4.0);
            config.setDurationTicks(60); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            slamDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            int slamIndex = tick / 10;
            int slamPhase = tick % 10;
            Location slamPt = c.clone().add(slamDir.clone().multiply(slamIndex * 3 - 6));
            if (slamPhase < 5) {
                // Rise
                double height = slamPhase * 1.0;
                DisplayBuilder.dustParticles(slamPt.clone().add(0, height, 0), 8, 0.5, 80, 80, 90, 1.5f);
            } else if (slamPhase == 5) {
                // Slam down
                DisplayBuilder.dustParticles(slamPt, 20, 1.5, 150, 150, 160, 1.5f);
                DisplayBuilder.playSound(slamPt, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.7f);
                for (Player p : playersInRadius(slamPt, 3.0)) {
                    p.setVelocity(p.getVelocity().add(new Vector(0, 0.3, 0)));
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PistonSlam(plugin); }
    }

    // 26. CHAIN MINE — Invisible 40 ticks, proximity triggered, then explodes
    public static class ChainMine extends EnvironmentalAttack {
        private boolean detonated = false;
        public ChainMine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_mine", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(12.0); config.setDamageRadius(5.0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
            config.setDamageDelayTicks(80); // Only damage on detonation
            config.setTicksBetweenDamage(80);
        }
        @Override protected void onSpawn(Location center) {
            // Silent placement
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (detonated) return;
            if (tick < 40) {
                // Invisible phase — subtle ground shimmer every 10 ticks
                if (tick % 10 == 0) DisplayBuilder.dustParticles(c.clone().add(0, 0.1, 0), 2, 0.3, 80, 80, 90, 0.5f);
            } else {
                // Armed — check proximity
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.1, 0), 3, 0.5, 180, 100, 50, 0.6f);
                }
                List<Player> nearby = playersInRadius(c, 3.0);
                if (!nearby.isEmpty()) {
                    detonated = true;
                    // Explode
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);
                    DisplayBuilder.dustParticles(c, 50, 2.5, 180, 100, 50, 2.0f);
                    DisplayBuilder.dustParticles(c, 30, 2.0, 220, 220, 230, 1.5f);
                    DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 24, RUST_DUST);
                    for (Player p : playersInRadius(c, 5.0)) {
                        p.damage(12.0);
                        p.setNoDamageTicks(0);
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.6).setY(0.4);
                        p.setVelocity(kb);
                    }
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainMine(plugin); }
    }

    // 27. IRON DUST CLOUD — Lingering gray cloud, damage inside, drifts
    public static class IronDustCloud extends EnvironmentalAttack {
        private double driftX, driftZ;
        private double offsetX = 0, offsetZ = 0;
        public IronDustCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_dust_cloud", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(5.0);
            config.setDurationTicks(140); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            driftX = (Math.random() - 0.5) * 0.15;
            driftZ = (Math.random() - 0.5) * 0.15;
            DisplayBuilder.playSound(center, Sound.BLOCK_SAND_BREAK, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            offsetX += driftX; offsetZ += driftZ;
            Location cloud = c.clone().add(offsetX, 0, offsetZ);
            setCenter(cloud);
            if (tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 6.0;
                    double oy = Math.random() * 3.0;
                    double oz = (Math.random() - 0.5) * 6.0;
                    DisplayBuilder.dustParticles(cloud.clone().add(ox, oy, oz), 2, 0.5, 150, 150, 160, 1.0f);
                }
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(cloud, Sound.BLOCK_SAND_BREAK, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronDustCloud(plugin); }
    }

    // ================================================================
    //  SECTION 3: CHAIN WEATHER (13 attacks)
    // ================================================================

    // 28. CHAIN STORM — Lightning-like gray particle strikes, random
    public static class ChainStorm extends EnvironmentalAttack {
        public ChainStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_storm", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(12.0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                // Random lightning strike
                double ox = (Math.random() - 0.5) * 20.0;
                double oz = (Math.random() - 0.5) * 20.0;
                Location strike = c.clone().add(ox, 0, oz);
                // Bolt particles from sky to ground
                for (int y = 0; y < 15; y++) {
                    double jx = (Math.random() - 0.5) * 0.8;
                    double jz = (Math.random() - 0.5) * 0.8;
                    DisplayBuilder.dustParticles(strike.clone().add(jx, y, jz), 3, 0.2, 150, 150, 160, 1.5f);
                }
                DisplayBuilder.dustParticles(strike, 15, 1.0, 220, 220, 230, 2.0f);
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.8f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainStorm(plugin); }
    }

    // 29. METAL HAIL — Gray particles falling like hail
    public static class MetalHail extends EnvironmentalAttack {
        public MetalHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_hail", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(10.0);
            config.setDurationTicks(100); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * 20.0;
                    double oz = (Math.random() - 0.5) * 20.0;
                    double oy = 8 + Math.random() * 5;
                    Location pt = c.clone().add(ox, oy, oz);
                    DisplayBuilder.dustParticles(pt, 2, 0.2, 150, 150, 160, 0.8f);
                    // Impact particle at ground
                    if (Math.random() > 0.7) {
                        DisplayBuilder.dustParticles(c.clone().add(ox, 0.2, oz), 2, 0.2, 80, 80, 90, 0.6f);
                    }
                }
            }
            if (tick % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MetalHail(plugin); }
    }

    // 30. FORGE WIND — Directional push + spark particles
    public static class ForgeWind extends EnvironmentalAttack {
        private Vector windDir;
        public ForgeWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forge_wind", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(12.0);
            config.setDurationTicks(40); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            windDir = new Vector(Math.cos(a), 0, Math.sin(a)).normalize().multiply(0.35);
            DisplayBuilder.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                for (Player p : playersInRadius(c, 12.0)) {
                    p.setVelocity(p.getVelocity().add(windDir));
                }
                // Wind spark particles
                for (int i = 0; i < 15; i++) {
                    double ox = (Math.random() - 0.5) * 20.0;
                    double oy = Math.random() * 3.0;
                    double oz = (Math.random() - 0.5) * 20.0;
                    DisplayBuilder.dustParticles(c.clone().add(ox, oy, oz), 1, 0.3, 220, 220, 230, 0.7f);
                }
            }
            if (tick % 10 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_BREEZE_WIND_BURST, 0.6f, 0.7f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ForgeWind(plugin); }
    }

    // 31. IRON FOG — Dense gray particle fog, damage inside
    public static class IronFog extends EnvironmentalAttack {
        public IronFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_fog", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(8.0);
            config.setDurationTicks(100); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SAND_BREAK, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 3 == 0) {
                for (int i = 0; i < 15; i++) {
                    double ox = (Math.random() - 0.5) * 16.0;
                    double oy = Math.random() * 2.5;
                    double oz = (Math.random() - 0.5) * 16.0;
                    DisplayBuilder.dustParticles(c.clone().add(ox, oy, oz), 2, 0.5, 150, 150, 160, 1.2f);
                }
            }
            if (tick % 25 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_SAND_BREAK, 0.4f, 0.4f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronFog(plugin); }
    }

    // 32. SPARK SHOWER — Spark particles raining from above
    public static class SparkShower extends EnvironmentalAttack {
        public SparkShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spark_shower", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(10.0);
            config.setDurationTicks(80); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 20.0;
                    double oz = (Math.random() - 0.5) * 20.0;
                    double startY = 6 + Math.random() * 6;
                    Location pt = c.clone().add(ox, startY, oz);
                    DisplayBuilder.dustParticles(pt, 2, 0.3, 220, 220, 230, 0.9f);
                }
                // Ground impact sparks
                for (int i = 0; i < 3; i++) {
                    double ox = (Math.random() - 0.5) * 18.0;
                    double oz = (Math.random() - 0.5) * 18.0;
                    DisplayBuilder.dustParticles(c.clone().add(ox, 0.1, oz), 3, 0.1, 220, 220, 230, 0.6f);
                }
            }
            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.8f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SparkShower(plugin); }
    }

    // 33. CHAIN QUAKE — Expanding ring from center, must jump over
    public static class ChainQuake extends EnvironmentalAttack {
        public ChainQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_quake", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(7.0); config.setDamageRadius(10.0);
            config.setDurationTicks(30); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(30);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.4f);
            DisplayBuilder.dustParticles(center, 30, 1.0, 80, 80, 90, 2.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            double r = tick * 0.35;
            if (r <= 10.0) {
                DisplayBuilder.particleRing(c, r, Particle.DUST, (int)(r * 8), STEEL_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), r, Particle.DUST, (int)(r * 4), IRON_DUST);
                // Damage players on ground level within the ring band
                if (tick % 3 == 0) {
                    for (Player p : playersInRadius(c, r + 1)) {
                        double dist = p.getLocation().distance(c);
                        if (dist >= r - 1 && dist <= r + 1 && p.isOnGround()) {
                            p.damage(7.0);
                            p.setNoDamageTicks(0);
                            p.setVelocity(p.getVelocity().add(new Vector(0, 0.3, 0)));
                        }
                    }
                }
            }
            if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainQuake(plugin); }
    }

    // 34. METAL FREEZE — Slow players (speed attribute), gray frost particles
    public static class MetalFreeze extends EnvironmentalAttack {
        private final Set<UUID> slowed = new HashSet<>();
        public MetalFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_freeze", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(3.0); config.setDamageRadius(8.0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 30, 4.0, 150, 150, 160, 1.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * 16.0;
                    double oy = Math.random() * 2.0;
                    double oz = (Math.random() - 0.5) * 16.0;
                    DisplayBuilder.dustParticles(c.clone().add(ox, oy, oz), 2, 0.3, 150, 150, 160, 1.0f);
                }
            }
            if (tick % 10 == 0) {
                for (Player p : playersInRadius(c, 8.0)) {
                    p.setWalkSpeed(0.08f);
                    slowed.add(p.getUniqueId());
                }
            }
            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.6f);
        }
        @Override protected void onCleanup() {
            for (UUID id : slowed) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.setWalkSpeed(0.2f);
            }
            slowed.clear();
            super.onCleanup();
        }
        @Override public AbstractAttack newInstance() { return new MetalFreeze(plugin); }
    }

    // 35. PRESSURE WAVE — Shockwave expanding ring, knockback
    public static class PressureWave extends EnvironmentalAttack {
        public PressureWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pressure_wave", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(12.0);
            config.setDurationTicks(20); config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            DisplayBuilder.dustParticles(center, 30, 1.0, 150, 150, 160, 2.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            double r = tick * 0.6;
            if (r <= 12.0) {
                DisplayBuilder.particleRing(c, r, Particle.DUST, (int)(r * 6), IRON_DUST);
                DisplayBuilder.particleRing(c.clone().add(0, 1, 0), r * 0.8, Particle.DUST, (int)(r * 3), SPARK_DUST);
                for (Player p : playersInRadius(c, r + 1.5)) {
                    double dist = p.getLocation().distance(c);
                    if (dist >= r - 1.5 && dist <= r + 1.5) {
                        Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.7).setY(0.2);
                        p.setVelocity(p.getVelocity().add(kb));
                    }
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PressureWave(plugin); }
    }

    // 36. CHAIN CASCADE — Sequential damage points in a line
    public static class ChainCascadeEnv extends EnvironmentalAttack {
        private Vector cascadeDir;
        public ChainCascadeEnv(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_cascade_env", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(6.0); config.setDamageRadius(6.0);
            config.setDurationTicks(60); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            cascadeDir = new Vector(Math.cos(a), 0, Math.sin(a));
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            int pointIndex = tick / 8;
            int phase = tick % 8;
            Location pt = c.clone().add(cascadeDir.clone().multiply(pointIndex * 3 - 9));
            if (phase == 0) {
                // Strike at this point
                DisplayBuilder.dustParticles(pt, 25, 1.5, 150, 150, 160, 1.5f);
                DisplayBuilder.dustParticles(pt.clone().add(0, 1, 0), 10, 0.5, 220, 220, 230, 1.2f);
                DisplayBuilder.playSound(pt, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.8f);
                for (Player p : playersInRadius(pt, 3.0)) {
                    p.damage(6.0);
                    p.setNoDamageTicks(0);
                }
            } else if (phase < 4) {
                // Lingering particles
                DisplayBuilder.dustParticles(pt, 5, 1.0, 80, 80, 90, 1.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainCascadeEnv(plugin); }
    }

    // 37. SCRAP TORNADO — Small particle tornado wandering, damage on contact
    public static class ScrapTornado extends EnvironmentalAttack {
        private double wanderAngle;
        private double offX = 0, offZ = 0;
        public ScrapTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scrap_tornado", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(5.0); config.setDamageRadius(3.0);
            config.setDurationTicks(120); config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            wanderAngle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.6f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            // Wander
            wanderAngle += (Math.random() - 0.5) * 0.3;
            offX += Math.cos(wanderAngle) * 0.2;
            offZ += Math.sin(wanderAngle) * 0.2;
            Location tornadoLoc = c.clone().add(offX, 0, offZ);
            setCenter(tornadoLoc);
            // Spinning column
            for (int y = 0; y < 6; y++) {
                double a = tick * 0.3 + y * 0.8;
                double r = 1.0 + y * 0.3;
                Location pt = tornadoLoc.clone().add(Math.cos(a) * r, y * 0.5, Math.sin(a) * r);
                DisplayBuilder.dustParticles(pt, 2, 0.2, 150, 150, 160, 1.0f);
                if (y % 2 == 0) DisplayBuilder.dustParticles(pt, 1, 0.1, 180, 100, 50, 0.8f);
            }
            if (tick % 15 == 0) DisplayBuilder.playSound(tornadoLoc, Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 0.8f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ScrapTornado(plugin); }
    }

    // 38. STATIC DISCHARGE — Charge for 40 ticks then burst
    public static class StaticDischarge extends EnvironmentalAttack {
        public StaticDischarge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("static_discharge", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(10.0); config.setDamageRadius(8.0);
            config.setDurationTicks(50); config.setCooldownTicks(300);
            config.setDamageDelayTicks(40); config.setTicksBetweenDamage(50);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 2.0f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick < 40) {
                // Charging phase: particles converging to center
                double intensity = tick / 40.0;
                double r = 8.0 * (1.0 - intensity);
                for (int i = 0; i < (int)(4 + intensity * 12); i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location pt = c.clone().add(Math.cos(angle) * r, Math.random() * 2, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pt, 2, 0.2, 220, 220, 230, (float)(0.5 + intensity));
                }
                if (tick % 8 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f + (float)(intensity * 0.5));
            } else if (tick == 40) {
                // Discharge
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 2.0f);
                DisplayBuilder.dustParticles(c, 80, 4.0, 220, 220, 230, 2.5f);
                DisplayBuilder.particleRing(c, 8.0, Particle.DUST, 40, SPARK_DUST);
                for (Player p : playersInRadius(c, 8.0)) {
                    Vector kb = p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.5).setY(0.3);
                    p.setVelocity(p.getVelocity().add(kb));
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new StaticDischarge(plugin); }
    }

    // 39. CHAIN ECHO — Repeating damage pulses from center every 15 ticks
    public static class ChainEcho extends EnvironmentalAttack {
        public ChainEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_echo", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(6.0);
            config.setDurationTicks(90); config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            if (tick % 15 == 0) {
                // Pulse ring outward
                DisplayBuilder.dustParticles(c, 25, 1.5, 150, 150, 160, 1.5f);
                DisplayBuilder.particleRing(c, 3.0, Particle.DUST, 16, IRON_DUST);
                DisplayBuilder.particleRing(c, 6.0, Particle.DUST, 24, STEEL_DUST);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.6f + (tick / 90.0f) * 0.8f);
            }
            // Expanding echo ring animation between pulses
            int phase = tick % 15;
            if (phase > 0 && phase < 10) {
                double r = phase * 0.7;
                DisplayBuilder.particleRing(c, r, Particle.DUST, (int)(r * 4), IRON_DUST);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainEcho(plugin); }
    }

    // 40. TOTAL LOCKDOWN — All players take unavoidable damage, chains everywhere
    public static class TotalLockdown extends EnvironmentalAttack {
        public TotalLockdown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_lockdown", AttackType.ENVIRONMENTAL, 1, "modes/chain/attacks"));
            config.setDamage(4.0); config.setDamageRadius(25.0);
            config.setDurationTicks(10); config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
            // Massive particle burst in all directions
            DisplayBuilder.dustParticles(center, 100, 12.0, 150, 150, 160, 2.0f);
            DisplayBuilder.dustParticles(center, 60, 10.0, 80, 80, 90, 1.5f);
            DisplayBuilder.dustParticles(center, 40, 8.0, 180, 100, 50, 1.5f);
            for (double r = 5; r <= 25; r += 5) {
                DisplayBuilder.particleRing(center, r, Particle.DUST, (int)(r * 4), IRON_DUST);
            }
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            // Continued chain sounds and particles
            DisplayBuilder.dustParticles(c, 40, 12.0, 150, 150, 160, 1.2f);
            if (tick % 3 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.5f + (float)(Math.random() * 0.5));
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TotalLockdown(plugin); }
    }
}
