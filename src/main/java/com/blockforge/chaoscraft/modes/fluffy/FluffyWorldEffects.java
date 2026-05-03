package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The eight FluffyMode passive world effects:
 *
 * <ol>
 *   <li>Petal weather (cherry leaves + pink dust drifting from sky)</li>
 *   <li>Glowing footprints (per-PlayerMoveEvent particle trail)</li>
 *   <li>Squeaky tiles (random squeak on block change)</li>
 *   <li>Singing flowers (passive notes when near placed flora)</li>
 *   <li>Glowing eyes (perimeter blink)</li>
 *   <li>Adoptable fluff (frozen glowing decoy mob)</li>
 *   <li>Picnic spot (heal zone with food displays)</li>
 *   <li>Rainbow arc (one-shot 7-color particle arc)</li>
 * </ol>
 *
 * Plus the always-running flora-bloom system that places flowers in the
 * arena and reverts them on mode end.
 */
public class FluffyWorldEffects implements Listener {

    private final ChaosCraftPlugin plugin;
    private final FluffyConfig config;
    private final Random random = new Random();

    private World world;
    private Location arenaCenter;
    private boolean active = false;
    private int tickCounter = 0;

    // Periodic timers
    private int glowingEyesNext = -1;
    private int adoptableFluffNext = -1;
    private int picnicNext = -1;
    private boolean rainbowFired = false;
    private int floraBloomNext = -1;

    // Tracked state
    private final Map<Location, Material> originalBlocks = new LinkedHashMap<>();
    private final Set<UUID> spawnedFluff = new HashSet<>();
    private final List<PicnicSpot> picnics = new ArrayList<>();
    private final List<RainbowArcInstance> rainbows = new ArrayList<>();

    public FluffyWorldEffects(ChaosCraftPlugin plugin, FluffyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start(World world, Location arenaCenter) {
        this.world = world;
        this.arenaCenter = arenaCenter != null ? arenaCenter.clone() : null;
        this.active = true;
        this.tickCounter = 0;
        this.glowingEyesNext = config.getGlowingEyesFirstFire();
        this.adoptableFluffNext = config.getAdoptableFluffFirstFire();
        this.picnicNext = config.getPicnicFirstFire();
        this.rainbowFired = false;
        this.floraBloomNext = config.getFloraBloomInterval();
        this.originalBlocks.clear();
        this.spawnedFluff.clear();
        this.picnics.clear();
        this.rainbows.clear();
    }

    public void stop() {
        active = false;
        // Revert flora
        if (config.isFloraBloomRevertOnEnd()) {
            for (var entry : originalBlocks.entrySet()) {
                try {
                    entry.getKey().getBlock().setType(entry.getValue(), false);
                } catch (Throwable ignored) {}
            }
        }
        originalBlocks.clear();

        // Despawn fluff
        for (UUID id : spawnedFluff) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        }
        spawnedFluff.clear();

        // Despawn picnic items
        for (PicnicSpot p : picnics) {
            for (UUID id : p.displays) {
                Entity e = Bukkit.getEntity(id);
                if (e != null && e.isValid()) e.remove();
            }
        }
        picnics.clear();
        rainbows.clear();
    }

    // ========================
    // Tick driver
    // ========================

    public void tick() {
        if (!active || !config.isWorldEffectsEnabled() || world == null) return;
        tickCounter++;

        tickPetalWeather();
        tickSingingFlowers();
        tickPicnicSpots();
        tickRainbows();

        if (config.isGlowingEyesEnabled() && tickCounter >= glowingEyesNext) {
            fireGlowingEyes();
            glowingEyesNext = tickCounter + config.getGlowingEyesInterval();
        }
        if (config.isAdoptableFluffEnabled() && tickCounter >= adoptableFluffNext) {
            fireAdoptableFluff();
            adoptableFluffNext = tickCounter + config.getAdoptableFluffInterval();
        }
        if (config.isPicnicEnabled() && tickCounter >= picnicNext) {
            firePicnicSpot();
            picnicNext = tickCounter + config.getPicnicInterval();
        }
        if (config.isRainbowArcEnabled() && !rainbowFired && tickCounter >= config.getRainbowArcFireTick()) {
            fireRainbowArc();
            rainbowFired = true;
        }
        if (config.isFloraBloomEnabled() && tickCounter >= floraBloomNext) {
            fireFloraBloom();
            floraBloomNext = tickCounter + config.getFloraBloomInterval();
        }
    }

    // ========================
    // 1. Petal weather
    // ========================

    private void tickPetalWeather() {
        if (arenaCenter == null) return;
        int density = config.getPetalDensity();
        int radius = config.getArenaRadius();
        int yOff = config.getPetalSpawnYOffset();

        for (int i = 0; i < density; i++) {
            double dx = (random.nextDouble() * 2 - 1) * radius;
            double dz = (random.nextDouble() * 2 - 1) * radius;
            Location loc = arenaCenter.clone().add(dx, yOff, dz);
            try {
                world.spawnParticle(Particle.CHERRY_LEAVES, loc, 1, 0.3, 0.3, 0.3, 0.0);
            } catch (Throwable ignored) {
                // Older versions may lack CHERRY_LEAVES — skip
            }
            try {
                Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(255, 182, 193), 0.8f);
                world.spawnParticle(Particle.DUST, loc, 1, 0.3, 0.3, 0.3, 0.0, pink);
            } catch (Throwable ignored) {}
        }
    }

    // ========================
    // 2. Glowing footprints + 3. Squeaky tiles (PlayerMoveEvent)
    // ========================

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!active || !config.isWorldEffectsEnabled()) return;
        Player player = event.getPlayer();
        World pWorld = player.getWorld();
        if (world != null && !pWorld.equals(world)) return;

        if (config.isFootprintsEnabled() && event.getFrom().distanceSquared(event.getTo()) > 0.001) {
            Location feet = event.getTo().clone();
            try {
                pWorld.spawnParticle(Particle.ENCHANT, feet, 1, 0.05, 0.05, 0.05, 0.0);
            } catch (Throwable ignored) {}
            try {
                Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 0.7f);
                pWorld.spawnParticle(Particle.DUST, feet, 1, 0.1, 0.05, 0.1, 0.0, pink);
            } catch (Throwable ignored) {}
        }

        if (config.isSqueakyTilesEnabled()
                && event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            if (random.nextDouble() < config.getSqueakyChance()) {
                try {
                    pWorld.playSound(event.getTo(), Sound.BLOCK_WOOL_HIT, 0.4f, 2.0f);
                } catch (Throwable ignored) {}
            }
        }
    }

    // ========================
    // 4. Singing flowers
    // ========================

    private void tickSingingFlowers() {
        if (!config.isSingingFlowersEnabled()) return;
        if (random.nextDouble() > config.getSingingFlowersChance()) return;

        double radius = config.getSingingFlowersRadius();
        int r = (int) Math.ceil(radius);
        for (Player p : world.getPlayers()) {
            // Scan a small box around the player
            Location pl = p.getLocation();
            for (int x = -r; x <= r; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -r; z <= r; z++) {
                        Block b = pl.getBlock().getRelative(x, y, z);
                        if (isFloraBlock(b.getType())) {
                            try {
                                p.playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.3f,
                                        0.7f + random.nextFloat() * 0.6f);
                            } catch (Throwable ignored) {}
                            return; // one note per tick across all players
                        }
                    }
                }
            }
        }
    }

    private boolean isFloraBlock(Material m) {
        if (m == null || m == Material.AIR) return false;
        for (Material flora : FluffyVersionUtil.getFlora()) {
            if (m == flora) return true;
        }
        return false;
    }

    // ========================
    // 5. Glowing eyes
    // ========================

    private void fireGlowingEyes() {
        if (arenaCenter == null) return;
        // Pick a perimeter point
        double angle = random.nextDouble() * Math.PI * 2;
        double r = config.getArenaRadius();
        double x = arenaCenter.getX() + Math.cos(angle) * r;
        double z = arenaCenter.getZ() + Math.sin(angle) * r;
        int gy = world.getHighestBlockYAt((int) x, (int) z);
        Location eyeLoc = new Location(world, x, gy + 1.5, z);

        new org.bukkit.scheduler.BukkitRunnable() {
            int blink = 0;
            @Override
            public void run() {
                if (blink >= 6 || !active) { cancel(); return; }
                Color color = (blink % 2 == 0) ? Color.RED : Color.fromRGB(0, 255, 0);
                Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
                try {
                    world.spawnParticle(Particle.DUST,
                            eyeLoc.clone().add(-0.25, 0, 0), 4, 0.05, 0.05, 0.05, 0.0, dust);
                    world.spawnParticle(Particle.DUST,
                            eyeLoc.clone().add(0.25, 0, 0), 4, 0.05, 0.05, 0.05, 0.0, dust);
                } catch (Throwable ignored) {}
                blink++;
            }
        }.runTaskTimer(plugin, 0L, 18L);
    }

    // ========================
    // 6. Adoptable fluff
    // ========================

    private void fireAdoptableFluff() {
        if (arenaCenter == null) return;
        double r = config.getArenaRadius() * 0.6;
        double dx = (random.nextDouble() * 2 - 1) * r;
        double dz = (random.nextDouble() * 2 - 1) * r;
        int x = (int) (arenaCenter.getX() + dx);
        int z = (int) (arenaCenter.getZ() + dz);
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location spawn = new Location(world, x + 0.5, y, z + 0.5);

        EntityType[] choices = {EntityType.CAT, EntityType.RABBIT, EntityType.FOX};
        EntityType type = choices[random.nextInt(choices.length)];

        try {
            Entity ent = world.spawnEntity(spawn, type);
            if (ent instanceof LivingEntity le) {
                le.setAI(false);
                le.setInvulnerable(true);
                le.setGlowing(true);
                le.setSilent(true);
                le.addScoreboardTag("fluffy:adoptable");
                spawnedFluff.add(le.getUniqueId());
                final UUID id = le.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null && e.isValid()) e.remove();
                    spawnedFluff.remove(id);
                }, config.getAdoptableFluffLifetime());
            }
        } catch (Throwable ignored) {}
    }

    // ========================
    // 7. Picnic spot
    // ========================

    private void firePicnicSpot() {
        if (arenaCenter == null) return;
        double r = config.getArenaRadius() * 0.5;
        double dx = (random.nextDouble() * 2 - 1) * r;
        double dz = (random.nextDouble() * 2 - 1) * r;
        int x = (int) (arenaCenter.getX() + dx);
        int z = (int) (arenaCenter.getZ() + dz);
        int y = world.getHighestBlockYAt(x, z) + 1;
        Location center = new Location(world, x + 0.5, y, z + 0.5);

        Material[] foods = {Material.CAKE, Material.COOKIE, Material.MILK_BUCKET, Material.APPLE};
        PicnicSpot spot = new PicnicSpot();
        spot.center = center;
        spot.expiresAt = tickCounter + config.getPicnicLifetime();

        for (int i = 0; i < foods.length; i++) {
            double angle = (Math.PI * 2) * i / foods.length;
            double ox = Math.cos(angle) * 1.0;
            double oz = Math.sin(angle) * 1.0;
            Location at = center.clone().add(ox, 0.5, oz);
            try {
                ItemDisplay d = world.spawn(at, ItemDisplay.class);
                d.setItemStack(new ItemStack(foods[i]));
                d.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)));
                d.addScoreboardTag("fluffy:picnic");
                spot.displays.add(d.getUniqueId());
            } catch (Throwable ignored) {}
        }
        picnics.add(spot);
    }

    private void tickPicnicSpots() {
        if (picnics.isEmpty()) return;
        var iter = picnics.iterator();
        while (iter.hasNext()) {
            PicnicSpot spot = iter.next();
            if (tickCounter >= spot.expiresAt) {
                for (UUID id : spot.displays) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null && e.isValid()) e.remove();
                }
                iter.remove();
                continue;
            }
            // Healing
            int interval = config.getPicnicHealInterval();
            if (interval > 0 && tickCounter % interval == 0) {
                double r = config.getPicnicHealRadius();
                double rSq = r * r;
                double heal = config.getPicnicHealAmount();
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distanceSquared(spot.center) <= rSq) {
                        try {
                            double max = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                            p.setHealth(Math.min(max, p.getHealth() + heal));
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }

    // ========================
    // 8. Rainbow arc
    // ========================

    private void fireRainbowArc() {
        if (arenaCenter == null) return;
        RainbowArcInstance arc = new RainbowArcInstance();
        arc.expiresAt = tickCounter + config.getRainbowArcDuration();
        rainbows.add(arc);
    }

    private void tickRainbows() {
        if (rainbows.isEmpty()) return;
        var iter = rainbows.iterator();
        while (iter.hasNext()) {
            RainbowArcInstance arc = iter.next();
            if (tickCounter >= arc.expiresAt) { iter.remove(); continue; }
            drawRainbowArc();
        }
    }

    private void drawRainbowArc() {
        if (arenaCenter == null) return;
        Color[] colors = {
                Color.fromRGB(255, 0, 0),
                Color.fromRGB(255, 127, 0),
                Color.fromRGB(255, 255, 0),
                Color.fromRGB(0, 255, 0),
                Color.fromRGB(0, 127, 255),
                Color.fromRGB(75, 0, 130),
                Color.fromRGB(143, 0, 255)
        };
        int radius = config.getArenaRadius();
        for (int i = 0; i < colors.length; i++) {
            double r = radius - i * 0.8;
            Particle.DustOptions dust = new Particle.DustOptions(colors[i], 1.5f);
            for (double t = 0; t <= Math.PI; t += 0.18) {
                double x = Math.cos(t) * r;
                double y = Math.sin(t) * r;
                Location pt = arenaCenter.clone().add(x, y + 5, 0);
                try {
                    world.spawnParticle(Particle.DUST, pt, 1, 0.0, 0.0, 0.0, 0.0, dust);
                } catch (Throwable ignored) {}
            }
        }
    }

    // ========================
    // Flora bloom
    // ========================

    private void fireFloraBloom() {
        if (arenaCenter == null) return;
        double roll = random.nextDouble();
        int radius;
        int count;
        if (roll < config.getFloraBloomLargeChance()) {
            radius = config.getFloraBloomLargeRadius();
            count = 30;
        } else if (roll < config.getFloraBloomLargeChance() + config.getFloraBloomMediumChance()) {
            radius = config.getFloraBloomMediumRadius();
            count = 15;
        } else {
            radius = config.getFloraBloomSmallRadius();
            count = 6;
        }
        fireBloomAt(radius, count);
    }

    /**
     * Force a flora bloom of the given tier ("small" / "medium" / "large").
     * Returns false if the mode isn't active or there's no arena center.
     */
    public boolean forceBloom(String tier) {
        if (arenaCenter == null || world == null) return false;
        int radius;
        int count;
        switch (tier.toLowerCase()) {
            case "large" -> { radius = config.getFloraBloomLargeRadius(); count = 30; }
            case "medium" -> { radius = config.getFloraBloomMediumRadius(); count = 15; }
            default -> { radius = config.getFloraBloomSmallRadius(); count = 6; }
        }
        fireBloomAt(radius, count);
        return true;
    }

    private void fireBloomAt(int radius, int count) {
        if (arenaCenter == null || world == null) return;
        // Pick random point in arena
        double dx = (random.nextDouble() * 2 - 1) * config.getArenaRadius();
        double dz = (random.nextDouble() * 2 - 1) * config.getArenaRadius();
        int cx = (int) (arenaCenter.getX() + dx);
        int cz = (int) (arenaCenter.getZ() + dz);

        List<Material> flora = FluffyVersionUtil.getFlora();
        if (flora.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * radius;
            int bx = (int) (cx + Math.cos(a) * dist);
            int bz = (int) (cz + Math.sin(a) * dist);
            int by = world.getHighestBlockYAt(bx, bz);
            Block ground = world.getBlockAt(bx, by, bz);
            Block above = world.getBlockAt(bx, by + 1, bz);
            if (above.getType() != Material.AIR) continue;
            // Only place on solid surface
            if (!ground.getType().isSolid()) continue;
            Material chosen = flora.get(random.nextInt(flora.size()));
            Location aboveLoc = above.getLocation();
            originalBlocks.putIfAbsent(aboveLoc, above.getType());
            try {
                above.setType(chosen, false);
            } catch (Throwable ignored) {
                // some flora won't place here — try another
            }
        }
    }

    // ========================
    // Holders
    // ========================

    private static class PicnicSpot {
        Location center;
        int expiresAt;
        final List<UUID> displays = new ArrayList<>();
    }

    private static class RainbowArcInstance {
        int expiresAt;
    }
}
