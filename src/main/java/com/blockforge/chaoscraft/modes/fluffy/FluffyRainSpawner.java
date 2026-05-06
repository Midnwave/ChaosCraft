package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.mobspawn.MobSpawnEntry;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * "Raining cats and dogs" spawner — drops invulnerable / AI-disabled mobs
 * from above the arena. Mobs activate once they touch the ground.
 *
 * Tracks falling mobs and respects {@code rain-from-sky.max-falling-mobs}.
 */
public class FluffyRainSpawner {

    private final ChaosCraftPlugin plugin;
    private final FluffyConfig config;

    private final Set<UUID> fallingMobs = new HashSet<>();
    private final List<MobSpawnEntry> entries = new ArrayList<>();
    private final Random random = new Random();

    private int tickCounter = 0;
    private int nextDropTick = 0;
    private int landingPollTick = 0;
    private boolean active = false;

    public FluffyRainSpawner(ChaosCraftPlugin plugin, FluffyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        if (!config.isRainEnabled()) return;
        active = true;
        tickCounter = 0;
        nextDropTick = 20; // 1s warmup
        landingPollTick = 0;
        fallingMobs.clear();
        entries.clear();
        entries.addAll(parseEntries(config.getRainMobsRaw()));
    }

    public void stop() {
        active = false;
        // Cleanup any still-falling mobs
        for (UUID id : new HashSet<>(fallingMobs)) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) {
                e.remove();
            }
        }
        fallingMobs.clear();
    }

    /** Number of currently airborne (still-falling, not yet landed) rain mobs. */
    public int getFallingMobCount() {
        return fallingMobs.size();
    }

    /** Force a single rain drop right now (admin command). Returns true if a mob was attempted. */
    public boolean forceDrop() {
        if (entries.isEmpty()) return false;
        attemptDrop();
        return true;
    }

    public void tick() {
        if (!active) return;
        tickCounter++;
        landingPollTick++;

        if (tickCounter >= nextDropTick) {
            attemptDrop();
            int spawnsPerWindow = Math.max(1, config.getRainSpawnsPerWindow());
            int windowTicks = Math.max(20, config.getRainWindowSeconds() * 20);
            int interval = Math.max(5, windowTicks / spawnsPerWindow);
            nextDropTick = tickCounter + interval;
        }

        // Poll for landed mobs every 3 ticks
        if (landingPollTick >= 3) {
            landingPollTick = 0;
            pollLandings();
        }
    }

    // ========================
    // Drop logic
    // ========================

    private void attemptDrop() {
        if (entries.isEmpty()) return;
        if (fallingMobs.size() >= config.getRainMaxFallingMobs()) return;

        World world = getWorld();
        if (world == null) return;
        if (world.getPlayers().isEmpty()) return;

        // Pick target XZ
        Location target = pickTargetXZ(world);
        if (target == null) return;

        // Pick mob entry (weighted)
        MobSpawnEntry entry = pickWeighted();
        if (entry == null) return;

        int dropY = world.getHighestBlockYAt(target.getBlockX(), target.getBlockZ()) + config.getRainDropHeight();
        Location spawn = new Location(world, target.getX() + 0.5, dropY, target.getZ() + 0.5);

        EntityType type = entry.resolveEntityType();
        if (type == null) return;

        try {
            Entity ent = world.spawnEntity(spawn, type);
            if (!(ent instanceof LivingEntity le)) {
                ent.remove();
                return;
            }
            le.setInvulnerable(true);
            le.setAI(false);
            le.setSilent(true);
            le.addScoreboardTag("fluffy:managed");
            le.addScoreboardTag("fluffy:falling");

            // Apply multipliers (entry × global difficulty-multiplier)
            double diffMult = 1.0;
            try { diffMult = config.getDifficultyMultiplier(); } catch (Throwable ignored) {}
            try {
                var maxHpAttr = le.getAttribute(Attribute.MAX_HEALTH);
                if (maxHpAttr != null) {
                    double hpMult = entry.getHealthMultiplier() * diffMult;
                    double newMax = maxHpAttr.getBaseValue() * hpMult;
                    maxHpAttr.setBaseValue(newMax);
                    le.setHealth(Math.min(le.getHealth() * hpMult, newMax));
                }
            } catch (Throwable ignored) {}
            try {
                var dmgAttr = le.getAttribute(Attribute.ATTACK_DAMAGE);
                if (dmgAttr != null) {
                    double dmgMult = entry.getDamageMultiplier() * diffMult;
                    dmgAttr.setBaseValue(dmgAttr.getBaseValue() * dmgMult);
                }
            } catch (Throwable ignored) {}

            // Slight downward velocity to commit the fall
            le.setVelocity(new Vector(0, -0.1, 0));
            fallingMobs.add(le.getUniqueId());
            plugin.debug("[FluffyRain] Dropped " + entry.getId() + " at "
                    + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ()
                    + " (active: " + fallingMobs.size() + ")");
        } catch (Throwable t) {
            plugin.debug("[FluffyRain] Failed to spawn " + entry.getId() + ": " + t.getMessage());
        }
    }

    private Location pickTargetXZ(World world) {
        if ("near_players".equalsIgnoreCase(config.getRainTargetMode())) {
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) return null;
            Player p = players.get(random.nextInt(players.size()));
            double scatter = config.getRainScatterRadius();
            double dx = (random.nextDouble() * 2 - 1) * scatter;
            double dz = (random.nextDouble() * 2 - 1) * scatter;
            return p.getLocation().clone().add(dx, 0, dz);
        }
        // random_arena — center on a random player and scatter by arena radius
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return null;
        Player p = players.get(random.nextInt(players.size()));
        double r = config.getArenaRadius();
        double dx = (random.nextDouble() * 2 - 1) * r;
        double dz = (random.nextDouble() * 2 - 1) * r;
        return p.getLocation().clone().add(dx, 0, dz);
    }

    private MobSpawnEntry pickWeighted() {
        if (entries.isEmpty()) return null;
        int total = 0;
        for (MobSpawnEntry e : entries) total += e.getWeight();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        int cum = 0;
        for (MobSpawnEntry e : entries) {
            cum += e.getWeight();
            if (roll < cum) return e;
        }
        return entries.get(entries.size() - 1);
    }

    // ========================
    // Landing detection
    // ========================

    private void pollLandings() {
        if (fallingMobs.isEmpty()) return;
        var iter = fallingMobs.iterator();
        while (iter.hasNext()) {
            UUID id = iter.next();
            Entity e = Bukkit.getEntity(id);
            if (e == null || !e.isValid() || e.isDead()) {
                iter.remove();
                continue;
            }
            if (!(e instanceof LivingEntity le)) {
                iter.remove();
                continue;
            }
            World world = le.getWorld();
            int groundY = world.getHighestBlockYAt(le.getLocation().getBlockX(), le.getLocation().getBlockZ());
            if (le.isOnGround() || le.getLocation().getY() < groundY + 1.5) {
                onLanded(le);
                iter.remove();
            }
        }
    }

    private void onLanded(LivingEntity le) {
        try {
            le.setInvulnerable(false);
            le.setAI(true);
            le.setSilent(false);
            le.removeScoreboardTag("fluffy:falling");
        } catch (Throwable ignored) {}

        Location loc = le.getLocation().clone().add(0, 0.1, 0);
        World w = loc.getWorld();
        if (w == null) return;

        // Landing puff: dust burst + cloud
        try {
            Particle.DustOptions dust = new Particle.DustOptions(Color.WHITE, 1.4f);
            w.spawnParticle(Particle.DUST, loc, 24, 0.4, 0.05, 0.4, dust);
        } catch (Throwable ignored) {}
        try {
            w.spawnParticle(Particle.CLOUD, loc, 8, 0.3, 0.05, 0.3, 0.02);
        } catch (Throwable ignored) {}
        try {
            w.playSound(loc, Sound.ENTITY_GENERIC_SMALL_FALL, 0.6f, 1.2f);
        } catch (Throwable ignored) {}
    }

    // ========================
    // Helpers
    // ========================

    private World getWorld() {
        String worldName = config.getWorldName();
        if (worldName != null && !worldName.isEmpty()) {
            World w = plugin.getServer().getWorld(worldName);
            if (w != null) return w;
        }
        var worlds = plugin.getServer().getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    private List<MobSpawnEntry> parseEntries(List<Map<?, ?>> raw) {
        List<MobSpawnEntry> out = new ArrayList<>();
        if (raw == null) return out;
        for (Map<?, ?> map : raw) {
            try {
                String id = String.valueOf(map.get("id") != null ? map.get("id") : "RABBIT");
                String typeStr = String.valueOf(map.get("type") != null ? map.get("type") : "vanilla").toUpperCase();
                MobSpawnEntry.MobType type;
                try { type = MobSpawnEntry.MobType.valueOf(typeStr); }
                catch (IllegalArgumentException e) { type = MobSpawnEntry.MobType.VANILLA; }
                int weight = toInt(map.get("weight"), 10);
                int minCount = toInt(map.get("min-count"), 1);
                int maxCount = toInt(map.get("max-count"), 1);
                double hp = toDouble(map.get("health-multiplier"), 1.0);
                double dmg = toDouble(map.get("damage-multiplier"), 1.0);
                out.add(new MobSpawnEntry(id, type, weight, minCount, maxCount, hp, dmg));
            } catch (Throwable t) {
                plugin.debug("[FluffyRain] Skipping malformed mob entry: " + t.getMessage());
            }
        }
        return out;
    }

    private static int toInt(Object o, int fallback) {
        if (o == null) return fallback;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static double toDouble(Object o, double fallback) {
        if (o == null) return fallback;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); }
        catch (NumberFormatException e) { return fallback; }
    }
}
