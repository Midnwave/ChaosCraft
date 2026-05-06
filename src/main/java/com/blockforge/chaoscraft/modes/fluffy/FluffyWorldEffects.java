package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import org.bukkit.util.Vector;
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
 * The eight FluffyMode passive world effects (drastically upgraded so none
 * can be confused with vanilla Minecraft weather/effects):
 *
 * <ol>
 *   <li>Petal weather — cherry leaves + spiraling pink/white/yellow petal
 *       ItemDisplays + rare 50-petal burst clouds.</li>
 *   <li>Glowing footprints — persistent pink paw-print ItemDisplays that
 *       fade over 3s, plus enchant + soul-flame sparks.</li>
 *   <li>Squeaky tiles — 6 NOTE particles + a popping NOTE_BLOCK ItemDisplay
 *       that hops up and back, color-coded by pitch.</li>
 *   <li>Singing flowers — nearest 3 flowers each pulse with a column of
 *       NOTE particles + a bobbing flower ItemDisplay; flute + harp duet.</li>
 *   <li>Glowing eyes — black-concrete pupils with soul-flame halos, a
 *       LIGHT_GRAY shadow silhouette behind them, sculk-soul vertical
 *       trail, enderman ambient SFX, and pupils slowly track players.</li>
 *   <li>Adoptable fluff — vanilla cute mob + heart aura + spinning enchant
 *       crown + pink wool ribbon ItemDisplay + cat purr loop + bell ding.</li>
 *   <li>Picnic spot — 4×4 checkered red/white carpet blanket, 8 rotating
 *       food ItemDisplays in a circle, central player-head host, vertical
 *       heart heal beam, enchant boundary ring.</li>
 *   <li>Rainbow arc — 3D arching rainbow (~80×40 blocks), color sweep that
 *       flows along the arc, 12-wool fluffy cloud at apex, two pots of
 *       gold at the endpoints, 6 figure-8 butterfly ItemDisplays,
 *       sparkle bursts, ambient harp music, firework spawn/expire flares.</li>
 * </ol>
 *
 * Plus the always-running flora-bloom system that places flowers in the
 * arena and reverts them on mode end.
 */
public class FluffyWorldEffects implements Listener {

    private final ChaosCraftPlugin plugin;
    private final FluffyConfig config;
    private final Random random = new Random();
    private final DisplayBuilder displayBuilder;

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
    private int petalBurstNext = -1;

    // Tracked state
    private final Map<Location, Material> originalBlocks = new LinkedHashMap<>();
    private final Set<UUID> spawnedFluff = new HashSet<>();
    private final List<PicnicSpot> picnics = new ArrayList<>();
    private final List<RainbowArcInstance> rainbows = new ArrayList<>();
    private final Set<UUID> miscDisplays = new HashSet<>();
    /** Most recent foot location per player so we can space paw-prints. */
    private final Map<UUID, Location> lastFootprint = new HashMap<>();
    /** Alternates left/right paw side per player. */
    private final Map<UUID, Boolean> footSide = new HashMap<>();

    public FluffyWorldEffects(ChaosCraftPlugin plugin, FluffyConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.displayBuilder = new DisplayBuilder(plugin, "fluffy");
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
        this.petalBurstNext = 600 + random.nextInt(400);
        this.originalBlocks.clear();
        this.spawnedFluff.clear();
        this.picnics.clear();
        this.rainbows.clear();
        this.miscDisplays.clear();
        this.lastFootprint.clear();
        this.footSide.clear();
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

        // Despawn fluff (and their attached ribbons/halos via miscDisplays)
        for (UUID id : spawnedFluff) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        }
        spawnedFluff.clear();

        // Despawn picnic items (foods + carpet blanket + host head)
        for (PicnicSpot p : picnics) {
            for (UUID id : p.displays) {
                Entity e = Bukkit.getEntity(id);
                if (e != null && e.isValid()) e.remove();
            }
        }
        picnics.clear();

        // Despawn rainbow apex cloud, pots of gold, and butterflies
        for (RainbowArcInstance arc : rainbows) {
            for (UUID id : arc.displays) {
                Entity e = Bukkit.getEntity(id);
                if (e != null && e.isValid()) e.remove();
            }
        }
        rainbows.clear();

        // Despawn footprints, squeak NOTE_BLOCKs, singing-flower bobbers, eye structures
        for (UUID id : miscDisplays) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.isValid()) e.remove();
        }
        miscDisplays.clear();
        lastFootprint.clear();
        footSide.clear();
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
        // Big petal-burst event piggybacks on petal weather
        if (tickCounter >= petalBurstNext) {
            firePetalBurst();
            petalBurstNext = tickCounter + 700 + random.nextInt(500);
        }
    }

    // ========================
    // 1. Petal weather
    // ========================

    /** Three rotating petal hues so it never reads as vanilla cherry-leaf decay. */
    private static final Color[] PETAL_HUES = {
            Color.fromRGB(255, 182, 193), // pink
            Color.fromRGB(255, 248, 230), // soft white
            Color.fromRGB(255, 235, 130)  // pastel yellow
    };

    private void tickPetalWeather() {
        if (arenaCenter == null) return;
        int density = config.getPetalDensity();
        int radius = config.getArenaRadius();
        int yOff = config.getPetalSpawnYOffset();

        // Layered colored dust drift (3 hues alternating)
        for (int i = 0; i < density; i++) {
            double dx = (random.nextDouble() * 2 - 1) * radius;
            double dz = (random.nextDouble() * 2 - 1) * radius;
            Location loc = arenaCenter.clone().add(dx, yOff, dz);
            try {
                world.spawnParticle(Particle.CHERRY_LEAVES, loc, 1, 0.3, 0.3, 0.3, 0.0);
            } catch (Throwable ignored) {}
            try {
                Color hue = PETAL_HUES[(tickCounter / 4 + i) % PETAL_HUES.length];
                Particle.DustOptions dust = new Particle.DustOptions(hue, 0.9f);
                world.spawnParticle(Particle.DUST, loc, 1, 0.3, 0.3, 0.3, 0.0, dust);
            } catch (Throwable ignored) {}
        }

        // Occasional spiraling petal ItemDisplay (every ~25 ticks per density unit)
        if (tickCounter % 25 == 0) {
            spawnSpiralPetal();
        }
    }

    private void spawnSpiralPetal() {
        int radius = config.getArenaRadius();
        int yOff = config.getPetalSpawnYOffset();
        double dx = (random.nextDouble() * 2 - 1) * radius;
        double dz = (random.nextDouble() * 2 - 1) * radius;
        Location start = arenaCenter.clone().add(dx, yOff, dz);

        Material petalMat;
        try {
            petalMat = Material.PINK_PETALS;
        } catch (Throwable t) {
            petalMat = Material.PINK_TULIP;
        }
        ItemDisplayHandle h;
        try {
            h = displayBuilder.spawnItem(start, new ItemStack(petalMat));
        } catch (Throwable t) {
            return;
        }
        h.scale(0.45f, 0.45f, 0.45f).glow(255, 200, 220);
        ItemDisplay disp = h.entity();
        miscDisplays.add(disp.getUniqueId());
        final UUID id = disp.getUniqueId();

        // Spiral down: rotate + descend over ~80 ticks
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            double angle = random.nextDouble() * Math.PI * 2;
            final double swirlR = 0.6 + random.nextDouble() * 0.5;
            final Location base = start.clone();
            @Override
            public void run() {
                Entity e = Bukkit.getEntity(id);
                if (!(e instanceof ItemDisplay d) || !active || t > 80) {
                    if (e != null && e.isValid()) e.remove();
                    miscDisplays.remove(id);
                    cancel();
                    return;
                }
                angle += 0.3;
                double yDrop = -t * 0.18;
                double ox = Math.cos(angle) * swirlR;
                double oz = Math.sin(angle) * swirlR;
                d.teleport(base.clone().add(ox, yDrop, oz));
                Transformation tr = d.getTransformation();
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        tr.getTranslation(),
                        new AxisAngle4f((float) (angle * 1.5), 0, 1, 0),
                        tr.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)));
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Big rare petal-burst: 50 petals exploding outward from a random arena point. */
    private void firePetalBurst() {
        if (arenaCenter == null) return;
        int radius = config.getArenaRadius();
        double dx = (random.nextDouble() * 2 - 1) * radius * 0.7;
        double dz = (random.nextDouble() * 2 - 1) * radius * 0.7;
        Location burst = arenaCenter.clone().add(dx, 6 + random.nextDouble() * 8, dz);

        try {
            world.playSound(burst, Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.6f, 1.4f);
            world.playSound(burst, Sound.BLOCK_AZALEA_PLACE, 1.0f, 1.2f);
            world.playSound(burst, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.4f, 2.0f);
        } catch (Throwable ignored) {}

        Material petalMat;
        try { petalMat = Material.PINK_PETALS; } catch (Throwable t) { petalMat = Material.PINK_TULIP; }

        // 50 petal ItemDisplays bursting outward
        for (int i = 0; i < 50; i++) {
            final ItemDisplayHandle h;
            try {
                h = displayBuilder.spawnItem(burst, new ItemStack(petalMat));
            } catch (Throwable t) { continue; }
            Color hue = PETAL_HUES[i % PETAL_HUES.length];
            h.scale(0.4f, 0.4f, 0.4f).glow(hue.getRed(), hue.getGreen(), hue.getBlue());
            ItemDisplay disp = h.entity();
            miscDisplays.add(disp.getUniqueId());
            final UUID id = disp.getUniqueId();

            // Random outward velocity vector
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = (random.nextDouble() - 0.3) * Math.PI;
            final double vx = Math.cos(theta) * Math.cos(phi) * 0.35;
            final double vy = Math.abs(Math.sin(phi)) * 0.25 + 0.05;
            final double vz = Math.sin(theta) * Math.cos(phi) * 0.35;

            new org.bukkit.scheduler.BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    Entity e = Bukkit.getEntity(id);
                    if (!(e instanceof ItemDisplay d) || !active || t > 60) {
                        if (e != null && e.isValid()) e.remove();
                        miscDisplays.remove(id);
                        cancel();
                        return;
                    }
                    Location cur = d.getLocation();
                    double drag = 1.0 - t * 0.012;
                    double yFall = -t * 0.04;
                    cur.add(vx * drag, vy * drag + yFall, vz * drag);
                    d.teleport(cur);
                    Transformation tr = d.getTransformation();
                    d.setInterpolationDuration(2);
                    d.setInterpolationDelay(0);
                    d.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f(t * 0.3f, 0, 1, 0),
                            tr.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)));
                    t++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
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
            handleFootprint(player, event.getTo().clone());
        }

        boolean tileChanged = event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
        if (config.isSqueakyTilesEnabled() && tileChanged) {
            if (random.nextDouble() < config.getSqueakyChance()) {
                fireSqueak(event.getTo().clone());
            }
        }
    }

    /** Persistent paw-print ItemDisplay trailing the player, fades over 3s. */
    private void handleFootprint(Player player, Location to) {
        World pWorld = to.getWorld();
        if (pWorld == null) return;

        // Sparkle particles every step
        try {
            pWorld.spawnParticle(Particle.ENCHANT, to, 4, 0.15, 0.05, 0.15, 0.1);
        } catch (Throwable ignored) {}
        try {
            pWorld.spawnParticle(Particle.SOUL_FIRE_FLAME, to.clone().add(0, 0.05, 0), 2,
                    0.1, 0.02, 0.1, 0.0);
        } catch (Throwable ignored) {}

        // Only drop a paw-print every ~0.6 blocks
        UUID pid = player.getUniqueId();
        Location prev = lastFootprint.get(pid);
        if (prev != null && prev.getWorld() == pWorld && prev.distanceSquared(to) < 0.36) return;
        lastFootprint.put(pid, to.clone());

        boolean rightSide = !footSide.getOrDefault(pid, false);
        footSide.put(pid, rightSide);

        // Side-offset so paws read as left/right rather than centered
        Vector facing = to.getDirection().setY(0).normalize();
        Vector side = new Vector(-facing.getZ(), 0, facing.getX()).multiply(rightSide ? 0.18 : -0.18);
        Location pawLoc = to.clone().add(side.getX(), 0.02, side.getZ());

        ItemDisplayHandle h;
        try {
            h = displayBuilder.spawnItem(pawLoc, new ItemStack(Material.PINK_CONCRETE_POWDER));
        } catch (Throwable t) {
            return;
        }
        // Paw-print proportions: wide, very flat, pointing along travel direction
        float yawRad = (float) Math.atan2(-facing.getX(), facing.getZ());
        h.scale(0.4f, 0.05f, 0.4f).glow(255, 160, 200);
        ItemDisplay disp = h.entity();
        Transformation tr = disp.getTransformation();
        disp.setTransformation(new Transformation(
                tr.getTranslation(),
                new AxisAngle4f(yawRad, 0, 1, 0),
                tr.getScale(),
                new AxisAngle4f(0, 0, 1, 0)));
        miscDisplays.add(disp.getUniqueId());
        final UUID id = disp.getUniqueId();

        // Fade over 3 seconds (60 ticks): shrink scale to zero
        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                Entity e = Bukkit.getEntity(id);
                if (!(e instanceof ItemDisplay d) || !active || t >= 60) {
                    if (e != null && e.isValid()) e.remove();
                    miscDisplays.remove(id);
                    cancel();
                    return;
                }
                float k = 1f - (t / 60f);
                Transformation cur = d.getTransformation();
                d.setInterpolationDuration(4);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        cur.getTranslation(),
                        cur.getLeftRotation(),
                        new Vector3f(0.4f * k, 0.05f * k, 0.4f * k),
                        cur.getRightRotation()));
                t += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    /** Squeak tile FX: 6 NOTE particles + popping NOTE_BLOCK ItemDisplay + colored dust. */
    private void fireSqueak(Location at) {
        World pWorld = at.getWorld();
        if (pWorld == null) return;
        try {
            pWorld.playSound(at, Sound.BLOCK_WOOL_HIT, 0.4f, 2.0f);
        } catch (Throwable ignored) {}

        // Map random pitch to a hue band
        float pitch = 0.6f + random.nextFloat() * 1.4f;
        Color band;
        if (pitch < 1.0f)      band = Color.fromRGB(120, 180, 255); // low = blue
        else if (pitch < 1.5f) band = Color.fromRGB(255, 220, 120); // mid = yellow
        else                   band = Color.fromRGB(255, 130, 200); // high = pink

        try {
            pWorld.spawnParticle(Particle.NOTE, at.clone().add(0, 0.6, 0), 6, 0.4, 0.2, 0.4, 1.0);
        } catch (Throwable ignored) {}
        try {
            Particle.DustOptions dust = new Particle.DustOptions(band, 1.0f);
            pWorld.spawnParticle(Particle.DUST, at.clone().add(0, 0.5, 0), 8, 0.3, 0.2, 0.3, 0.0, dust);
        } catch (Throwable ignored) {}

        // Popping NOTE_BLOCK: spawns at feet, hops up Y+0.5 then drops back, removed at ~16t
        Location spawn = at.clone().add(0, 0.1, 0);
        ItemDisplayHandle h;
        try {
            h = displayBuilder.spawnItem(spawn, new ItemStack(Material.NOTE_BLOCK));
        } catch (Throwable t) { return; }
        h.scale(0.35f, 0.35f, 0.35f).glow(band.getRed(), band.getGreen(), band.getBlue());
        ItemDisplay disp = h.entity();
        miscDisplays.add(disp.getUniqueId());
        final UUID id = disp.getUniqueId();

        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                Entity e = Bukkit.getEntity(id);
                if (!(e instanceof ItemDisplay d) || !active || t > 16) {
                    if (e != null && e.isValid()) e.remove();
                    miscDisplays.remove(id);
                    cancel();
                    return;
                }
                // Sin curve hop: peaks at t=8, returns at t=16
                double y = Math.sin((Math.PI * t) / 16.0) * 0.5;
                d.teleport(spawn.clone().add(0, y, 0));
                Transformation tr = d.getTransformation();
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        tr.getTranslation(),
                        new AxisAngle4f(t * 0.4f, 0, 1, 0),
                        tr.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)));
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
            // Find the nearest 3 flora blocks
            Location pl = p.getLocation();
            List<Block> found = new ArrayList<>();
            for (int x = -r; x <= r && found.size() < 3; x++) {
                for (int y = -1; y <= 1 && found.size() < 3; y++) {
                    for (int z = -r; z <= r && found.size() < 3; z++) {
                        Block b = pl.getBlock().getRelative(x, y, z);
                        if (isFloraBlock(b.getType())) found.add(b);
                    }
                }
            }
            if (!found.isEmpty()) {
                singFlowers(p, found);
                return; // one chord per tick across all players
            }
        }
    }

    private void singFlowers(Player p, List<Block> flowers) {
        // Layered duet: flute + harp at random matching pitches
        float pitch = 0.7f + random.nextFloat() * 0.6f;
        try {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.35f, pitch);
        } catch (Throwable ignored) {}
        try {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.3f, pitch * 0.95f);
        } catch (Throwable ignored) {}

        for (Block b : flowers) {
            Location base = b.getLocation().add(0.5, 0.0, 0.5);
            // Vertical column of NOTE particles (8 levels)
            for (int yi = 0; yi < 8; yi++) {
                Location lvl = base.clone().add(
                        (random.nextDouble() - 0.5) * 0.2,
                        0.4 + yi * 0.18,
                        (random.nextDouble() - 0.5) * 0.2);
                try {
                    world.spawnParticle(Particle.NOTE, lvl, 1, 0.0, 0.0, 0.0, 1.0);
                } catch (Throwable ignored) {}
            }

            // Bobbing flower ItemDisplay
            Material flower = (b.getType() == Material.POPPY) ? Material.POPPY : Material.DANDELION;
            ItemDisplayHandle h;
            try {
                h = displayBuilder.spawnItem(base.clone().add(0, 0.4, 0), new ItemStack(flower));
            } catch (Throwable t) { continue; }
            h.scale(0.5f, 0.5f, 0.5f).glow(255, 240, 150);
            ItemDisplay disp = h.entity();
            miscDisplays.add(disp.getUniqueId());
            final UUID id = disp.getUniqueId();
            final Location anchor = base.clone();

            new org.bukkit.scheduler.BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    Entity e = Bukkit.getEntity(id);
                    if (!(e instanceof ItemDisplay d) || !active || t > 30) {
                        if (e != null && e.isValid()) e.remove();
                        miscDisplays.remove(id);
                        cancel();
                        return;
                    }
                    // Bob Y+0.3 in time with the note
                    double y = 0.4 + Math.sin((Math.PI * t) / 15.0) * 0.3;
                    d.teleport(anchor.clone().add(0, y, 0));
                    Transformation tr = d.getTransformation();
                    d.setInterpolationDuration(2);
                    d.setInterpolationDelay(0);
                    d.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f(t * 0.25f, 0, 1, 0),
                            tr.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)));
                    t++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
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
        Location eyeCenter = new Location(world, x, gy + 1.5, z);

        try {
            world.playSound(eyeCenter, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
        } catch (Throwable ignored) {}

        // Pupil ItemDisplays (BLACK_CONCRETE 0.3 scale)
        final List<UUID> eyeDisplays = new ArrayList<>();
        ItemDisplayHandle leftEye = null, rightEye = null;
        try {
            leftEye = displayBuilder.spawnItem(
                    eyeCenter.clone().add(-0.25, 0, 0), new ItemStack(Material.BLACK_CONCRETE));
            leftEye.scale(0.3f, 0.3f, 0.3f).glow(255, 60, 60);
            rightEye = displayBuilder.spawnItem(
                    eyeCenter.clone().add(0.25, 0, 0), new ItemStack(Material.BLACK_CONCRETE));
            rightEye.scale(0.3f, 0.3f, 0.3f).glow(255, 60, 60);
            miscDisplays.add(leftEye.entity().getUniqueId());
            miscDisplays.add(rightEye.entity().getUniqueId());
            eyeDisplays.add(leftEye.entity().getUniqueId());
            eyeDisplays.add(rightEye.entity().getUniqueId());
        } catch (Throwable ignored) {}

        // Faint creature shadow silhouette behind the eyes (4 stacked LIGHT_GRAY blocks)
        try {
            for (int yi = -1; yi < 3; yi++) {
                for (int sx = -1; sx <= 1; sx++) {
                    if (yi == 2 && Math.abs(sx) == 1) continue; // taper top
                    if (yi == -1 && sx == 0) continue;          // legs gap
                    ItemDisplayHandle silh = displayBuilder.spawnItem(
                            eyeCenter.clone().add(sx * 0.6, yi * 0.7 - 0.3, 0.3),
                            new ItemStack(Material.LIGHT_GRAY_CONCRETE));
                    silh.scale(0.55f, 0.65f, 0.25f).glow(80, 80, 90);
                    ItemDisplay sd = silh.entity();
                    sd.setBrightness(new org.bukkit.entity.Display.Brightness(2, 2));
                    miscDisplays.add(sd.getUniqueId());
                    eyeDisplays.add(sd.getUniqueId());
                }
            }
        } catch (Throwable ignored) {}

        new org.bukkit.scheduler.BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t >= 120 || !active) {
                    for (UUID id : eyeDisplays) {
                        Entity e = Bukkit.getEntity(id);
                        if (e != null && e.isValid()) e.remove();
                        miscDisplays.remove(id);
                    }
                    cancel();
                    return;
                }

                // Halo of 8 SOUL_FIRE_FLAME particles around each eye
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2 * i / 8) + (t * 0.15);
                    double ox = Math.cos(a) * 0.35;
                    double oy = Math.sin(a) * 0.25;
                    try {
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                eyeCenter.clone().add(-0.25 + ox, oy, 0), 0,
                                0, 0, 0, 0);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                eyeCenter.clone().add(0.25 + ox, oy, 0), 0,
                                0, 0, 0, 0);
                    } catch (Throwable ignored) {}
                }
                // Vertical SCULK_SOUL trail between the eyes
                if (t % 4 == 0) {
                    try {
                        world.spawnParticle(Particle.SCULK_SOUL,
                                eyeCenter.clone().add(0, -0.3 + (t % 12) * 0.05, 0), 1,
                                0.05, 0.05, 0.05, 0.01);
                        world.spawnParticle(Particle.SCULK_SOUL,
                                eyeCenter.clone().add(0, 0.5 + (t % 12) * 0.05, 0), 1,
                                0.05, 0.05, 0.05, 0.01);
                    } catch (Throwable ignored) {}
                }

                // Track the nearest player (slowly rotate to face them)
                Player closest = null;
                double bestSq = Double.MAX_VALUE;
                for (Player p : world.getPlayers()) {
                    double ds = p.getLocation().distanceSquared(eyeCenter);
                    if (ds < bestSq) { bestSq = ds; closest = p; }
                }
                if (closest != null && t % 4 == 0) {
                    Vector toP = closest.getLocation().toVector().subtract(eyeCenter.toVector());
                    if (toP.lengthSquared() > 0.01) {
                        toP.setY(0).normalize();
                        Vector side = new Vector(-toP.getZ(), 0, toP.getX()).multiply(0.25);
                        teleportEye(eyeDisplays.get(0), eyeCenter.clone().add(-side.getX(), 0, -side.getZ()));
                        teleportEye(eyeDisplays.size() > 1 ? eyeDisplays.get(1) : null,
                                eyeCenter.clone().add(side.getX(), 0, side.getZ()));
                    }
                }

                // Blink (toggle pupil scale every ~18 ticks)
                if (t % 18 == 0) {
                    boolean closed = ((t / 18) % 2) == 1;
                    for (int i = 0; i < 2 && i < eyeDisplays.size(); i++) {
                        Entity e = Bukkit.getEntity(eyeDisplays.get(i));
                        if (e instanceof ItemDisplay d) {
                            Transformation tr = d.getTransformation();
                            d.setInterpolationDuration(3);
                            d.setInterpolationDelay(0);
                            float ys = closed ? 0.04f : 0.3f;
                            d.setTransformation(new Transformation(
                                    tr.getTranslation(), tr.getLeftRotation(),
                                    new Vector3f(0.3f, ys, 0.3f),
                                    tr.getRightRotation()));
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void teleportEye(UUID id, Location to) {
        if (id == null) return;
        Entity e = Bukkit.getEntity(id);
        if (e instanceof ItemDisplay d) d.teleport(to);
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
            world.playSound(spawn, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.6f);
        } catch (Throwable ignored) {}

        try {
            Entity ent = world.spawnEntity(spawn, type);
            if (ent instanceof LivingEntity le) {
                le.setAI(false);
                le.setInvulnerable(true);
                le.setGlowing(true);
                le.setSilent(true);
                le.addScoreboardTag("fluffy:adoptable");
                final UUID mobId = le.getUniqueId();
                spawnedFluff.add(mobId);

                // Attach a pink wool ribbon ItemDisplay around its neck
                final List<UUID> attach = new ArrayList<>();
                try {
                    ItemDisplayHandle ribbon = displayBuilder.spawnItem(
                            le.getLocation().add(0, 0.4, 0), new ItemStack(Material.PINK_WOOL));
                    ribbon.scale(0.3f, 0.1f, 0.4f).glow(255, 120, 180);
                    miscDisplays.add(ribbon.entity().getUniqueId());
                    attach.add(ribbon.entity().getUniqueId());
                } catch (Throwable ignored) {}

                final int lifetime = config.getAdoptableFluffLifetime();
                new org.bukkit.scheduler.BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        Entity e = Bukkit.getEntity(mobId);
                        if (!(e instanceof LivingEntity living) || !active || t >= lifetime) {
                            for (UUID id : attach) {
                                Entity a = Bukkit.getEntity(id);
                                if (a != null && a.isValid()) a.remove();
                                miscDisplays.remove(id);
                            }
                            if (e != null && e.isValid()) e.remove();
                            spawnedFluff.remove(mobId);
                            cancel();
                            return;
                        }
                        Location at = living.getLocation();

                        // HEART aura: 4 hearts/sec across a 3-block radius
                        if (t % 5 == 0) {
                            for (int i = 0; i < 1; i++) {
                                double a = random.nextDouble() * Math.PI * 2;
                                double rr = random.nextDouble() * 2.5;
                                Location hp = at.clone().add(Math.cos(a) * rr, 0.6 + random.nextDouble() * 0.5, Math.sin(a) * rr);
                                try {
                                    world.spawnParticle(Particle.HEART, hp, 1, 0, 0, 0, 0);
                                } catch (Throwable ignored) {}
                            }
                        }

                        // Spinning ENCHANT crown above its head (ring of 8)
                        for (int i = 0; i < 8; i++) {
                            double a = (Math.PI * 2 * i / 8) + (t * 0.2);
                            Location ep = at.clone().add(Math.cos(a) * 0.5, 1.1, Math.sin(a) * 0.5);
                            try {
                                world.spawnParticle(Particle.ENCHANT, ep, 1, 0, 0, 0, 0);
                            } catch (Throwable ignored) {}
                        }

                        // Move ribbon with the mob
                        if (!attach.isEmpty()) {
                            Entity rib = Bukkit.getEntity(attach.get(0));
                            if (rib instanceof ItemDisplay d) {
                                d.teleport(at.clone().add(0, 0.45, 0));
                            }
                        }

                        // Cat purr every 40 ticks
                        if (t % 40 == 0) {
                            try {
                                world.playSound(at, Sound.ENTITY_CAT_PURR, 0.6f, 1.2f);
                            } catch (Throwable ignored) {}
                        }
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
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

        try {
            world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.4f);
        } catch (Throwable ignored) {}

        PicnicSpot spot = new PicnicSpot();
        spot.center = center;
        spot.expiresAt = tickCounter + config.getPicnicLifetime();

        // 4×4 checkered carpet blanket at ground level
        for (int bx = 0; bx < 4; bx++) {
            for (int bz = 0; bz < 4; bz++) {
                Material carpet = ((bx + bz) % 2 == 0) ? Material.RED_CARPET : Material.WHITE_CARPET;
                Location at = center.clone().add(bx - 1.5, 0.05, bz - 1.5);
                try {
                    ItemDisplayHandle d = displayBuilder.spawnItem(at, new ItemStack(carpet));
                    d.scale(1.0f, 0.1f, 1.0f).glow(255, 200, 200);
                    spot.displays.add(d.entity().getUniqueId());
                } catch (Throwable ignored) {}
            }
        }

        // 8 foods arranged in a circle, slowly rotating
        Material[] foods = {
                Material.CAKE, Material.COOKIE, Material.MILK_BUCKET, Material.APPLE,
                Material.BREAD, Material.COOKED_CHICKEN, Material.GLOW_BERRIES, Material.HONEY_BOTTLE
        };
        for (int i = 0; i < foods.length; i++) {
            double angle = (Math.PI * 2) * i / foods.length;
            double ox = Math.cos(angle) * 1.2;
            double oz = Math.sin(angle) * 1.2;
            Location at = center.clone().add(ox, 0.5, oz);
            try {
                ItemDisplayHandle d = displayBuilder.spawnItem(at, new ItemStack(foods[i]));
                d.scale(0.6f, 0.6f, 0.6f).glow(255, 220, 180);
                d.entity().addScoreboardTag("fluffy:picnic");
                spot.displays.add(d.entity().getUniqueId());
                spot.foodIds.add(d.entity().getUniqueId());
            } catch (Throwable ignored) {}
        }

        // Teddy host (PLAYER_HEAD) at center
        try {
            ItemDisplayHandle host = displayBuilder.spawnItem(
                    center.clone().add(0, 0.6, 0), new ItemStack(Material.PLAYER_HEAD));
            host.scale(0.7f, 0.7f, 0.7f).glow(255, 180, 160);
            spot.displays.add(host.entity().getUniqueId());
            spot.hostId = host.entity().getUniqueId();
        } catch (Throwable ignored) {}

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

            // Rotate the 8 foods in a slow circle around center
            double angleBase = (tickCounter % 200) * (Math.PI * 2 / 200.0);
            for (int i = 0; i < spot.foodIds.size(); i++) {
                Entity e = Bukkit.getEntity(spot.foodIds.get(i));
                if (e instanceof ItemDisplay d) {
                    double a = angleBase + (Math.PI * 2 * i / spot.foodIds.size());
                    d.teleport(spot.center.clone().add(Math.cos(a) * 1.2, 0.5, Math.sin(a) * 1.2));
                    Transformation tr = d.getTransformation();
                    d.setInterpolationDuration(2);
                    d.setInterpolationDelay(0);
                    d.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f((float) (a * 2), 0, 1, 0),
                            tr.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)));
                }
            }

            // Vertical heart heal beam from center
            if (tickCounter % 4 == 0) {
                for (int yi = 0; yi < 6; yi++) {
                    Location hp = spot.center.clone().add(
                            (random.nextDouble() - 0.5) * 0.3,
                            0.6 + yi * 0.4,
                            (random.nextDouble() - 0.5) * 0.3);
                    try {
                        world.spawnParticle(Particle.HEART, hp, 1, 0, 0, 0, 0);
                    } catch (Throwable ignored) {}
                }
            }

            // ENCHANT halo at heal-radius edge
            double hr = config.getPicnicHealRadius();
            int ringPts = 18;
            int phase = tickCounter % ringPts;
            for (int i = 0; i < ringPts; i++) {
                if ((i + phase) % 3 != 0) continue;
                double a = (Math.PI * 2 * i / ringPts);
                Location ep = spot.center.clone().add(Math.cos(a) * hr, 0.4, Math.sin(a) * hr);
                try {
                    world.spawnParticle(Particle.ENCHANT, ep, 1, 0, 0, 0, 0);
                } catch (Throwable ignored) {}
            }

            // Healing
            int interval = config.getPicnicHealInterval();
            if (interval > 0 && tickCounter % interval == 0) {
                double rSq = hr * hr;
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

    /** True 3D rainbow horizon-arch dimensions. */
    private static final double RAINBOW_HALF_SPAN = 40.0; // 80 blocks horizontal
    private static final double RAINBOW_PEAK_HEIGHT = 40.0;
    private static final double RAINBOW_BAND_THICKNESS = 0.8;
    private static final int RAINBOW_BAND_POINTS = 60;
    private static final Color[] RAINBOW_BANDS = {
            Color.fromRGB(255, 0, 0),
            Color.fromRGB(255, 127, 0),
            Color.fromRGB(255, 255, 0),
            Color.fromRGB(0, 255, 0),
            Color.fromRGB(0, 127, 255),
            Color.fromRGB(75, 0, 130),
            Color.fromRGB(143, 0, 255)
    };

    private void fireRainbowArc() {
        if (arenaCenter == null) return;
        RainbowArcInstance arc = new RainbowArcInstance();
        arc.expiresAt = tickCounter + config.getRainbowArcDuration();

        // Anchor: arc spans X axis, Z is slight curve
        arc.anchor = arenaCenter.clone();
        int gy = world.getHighestBlockYAt(arc.anchor.getBlockX(), arc.anchor.getBlockZ());
        arc.groundY = gy + 1.0;

        // Spawn flourish
        try {
            Location apex = arc.anchor.clone().add(0, RAINBOW_PEAK_HEIGHT, 0);
            world.spawnParticle(Particle.FIREWORK, apex, 5, 1.0, 0.5, 1.0, 0.2);
            world.playSound(apex, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.5f, 1.8f);
            for (Player p : world.getPlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            }
        } catch (Throwable ignored) {}

        // 12-WHITE_WOOL fluffy cloud at apex
        Location apex = arc.anchor.clone().add(0, RAINBOW_PEAK_HEIGHT, 0);
        for (int i = 0; i < 12; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double rr = 1.0 + random.nextDouble() * 1.8;
            double yj = (random.nextDouble() - 0.5) * 1.0;
            Location wp = apex.clone().add(Math.cos(a) * rr, yj, Math.sin(a) * rr);
            try {
                ItemDisplayHandle wh = displayBuilder.spawnItem(wp, new ItemStack(Material.WHITE_WOOL));
                wh.scale(1.4f, 1.4f, 1.4f).glow(255, 255, 255);
                arc.displays.add(wh.entity().getUniqueId());
                arc.cloudIds.add(wh.entity().getUniqueId());
            } catch (Throwable ignored) {}
        }

        // Pots of gold at the two arc endpoints (X = ±RAINBOW_HALF_SPAN)
        for (int side = -1; side <= 1; side += 2) {
            Location end = arc.anchor.clone().add(side * RAINBOW_HALF_SPAN, 0, 0);
            int egy = world.getHighestBlockYAt(end.getBlockX(), end.getBlockZ());
            end.setY(egy + 0.5);
            try {
                ItemDisplayHandle pot = displayBuilder.spawnItem(end, new ItemStack(Material.GOLD_BLOCK));
                pot.scale(0.9f, 0.9f, 0.9f).glow(255, 215, 0);
                arc.displays.add(pot.entity().getUniqueId());
                arc.potIds.add(pot.entity().getUniqueId());
            } catch (Throwable ignored) {}
        }

        // 6 butterfly FEATHER ItemDisplays
        for (int i = 0; i < 6; i++) {
            try {
                ItemDisplayHandle bf = displayBuilder.spawnItem(apex, new ItemStack(Material.FEATHER));
                bf.scale(0.4f, 0.4f, 0.4f).glow(
                        180 + random.nextInt(75),
                        100 + random.nextInt(155),
                        180 + random.nextInt(75));
                arc.displays.add(bf.entity().getUniqueId());
                arc.butterflyIds.add(bf.entity().getUniqueId());
            } catch (Throwable ignored) {}
        }

        rainbows.add(arc);
    }

    private void tickRainbows() {
        if (rainbows.isEmpty()) return;
        var iter = rainbows.iterator();
        while (iter.hasNext()) {
            RainbowArcInstance arc = iter.next();
            if (tickCounter >= arc.expiresAt) {
                // Expire flourish
                Location apex = arc.anchor.clone().add(0, RAINBOW_PEAK_HEIGHT, 0);
                try {
                    for (int i = 0; i < 3; i++) {
                        Location off = apex.clone().add(
                                (random.nextDouble() - 0.5) * 4,
                                (random.nextDouble() - 0.2) * 2,
                                (random.nextDouble() - 0.5) * 4);
                        world.spawnParticle(Particle.FIREWORK, off, 12, 1.5, 0.8, 1.5, 0.25);
                    }
                    world.playSound(apex, Sound.BLOCK_NOTE_BLOCK_BELL, 1.6f, 1.0f);
                    world.playSound(apex, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.4f, 0.8f);
                } catch (Throwable ignored) {}

                for (UUID id : arc.displays) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null && e.isValid()) e.remove();
                }
                iter.remove();
                continue;
            }
            drawRainbowArc(arc);
            tickRainbowDisplays(arc);
        }
    }

    /** 7-band 3D arc with flowing color sweep. */
    private void drawRainbowArc(RainbowArcInstance arc) {
        if (arc.anchor == null) return;
        double phase = (tickCounter % 200) / 200.0; // 0..1 sweep
        for (int b = 0; b < RAINBOW_BANDS.length; b++) {
            // Each band sits at a different height within the arc thickness
            double bandOffset = (b - (RAINBOW_BANDS.length - 1) / 2.0) * RAINBOW_BAND_THICKNESS;
            for (int i = 0; i < RAINBOW_BAND_POINTS; i++) {
                double t = (double) i / (RAINBOW_BAND_POINTS - 1); // 0..1 across arc
                // Parametric arc: x sweeps, y rises and falls, z is slight curve
                double xs = (t * 2 - 1) * RAINBOW_HALF_SPAN;
                double ys = Math.sin(t * Math.PI) * RAINBOW_PEAK_HEIGHT;
                double zs = Math.sin(t * Math.PI) * 4.0; // slight Z bow

                // Outward normal of arc at point t (for band thickness offset)
                // Tangent ~ (1, cos(t·pi)·peak·pi/span, ...) — approximate normal upward
                double yOff = bandOffset; // simple vertical band stacking

                Location pt = arc.anchor.clone().add(xs, arc.groundY - arc.anchor.getY() + ys + yOff, zs);

                // Color sweep: shift band hue by a phase that travels along the arc
                int hueIdx = (b + (int) Math.floor(phase * RAINBOW_BANDS.length + t * RAINBOW_BANDS.length))
                        % RAINBOW_BANDS.length;
                if (hueIdx < 0) hueIdx += RAINBOW_BANDS.length;
                Particle.DustOptions dust = new Particle.DustOptions(RAINBOW_BANDS[hueIdx], 1.6f);
                try {
                    world.spawnParticle(Particle.DUST, pt, 1, 0.0, 0.0, 0.0, 0.0, dust);
                } catch (Throwable ignored) {}
            }
        }

        // Sparkle bursts along the arc every ~10 ticks
        if (tickCounter % 10 == 0) {
            double t = random.nextDouble();
            double xs = (t * 2 - 1) * RAINBOW_HALF_SPAN;
            double ys = Math.sin(t * Math.PI) * RAINBOW_PEAK_HEIGHT;
            double zs = Math.sin(t * Math.PI) * 4.0;
            Location pt = arc.anchor.clone().add(xs, arc.groundY - arc.anchor.getY() + ys, zs);
            try {
                world.spawnParticle(Particle.ENCHANT, pt, 15, 0.8, 0.8, 0.8, 0.5);
            } catch (Throwable ignored) {}
        }

        // Ambient harp every 30 ticks
        if (tickCounter % 30 == 0) {
            Location apex = arc.anchor.clone().add(0, RAINBOW_PEAK_HEIGHT, 0);
            try {
                world.playSound(apex, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.8f + random.nextFloat() * 1.0f);
            } catch (Throwable ignored) {}
        }
    }

    /** Animate cloud rotation, pot rotation, and butterfly figure-8 flight. */
    private void tickRainbowDisplays(RainbowArcInstance arc) {
        Location apex = arc.anchor.clone().add(0, RAINBOW_PEAK_HEIGHT, 0);

        // Slowly rotate cloud puffs around apex
        double cloudPhase = (tickCounter % 400) * (Math.PI * 2 / 400.0);
        for (int i = 0; i < arc.cloudIds.size(); i++) {
            Entity e = Bukkit.getEntity(arc.cloudIds.get(i));
            if (e instanceof ItemDisplay d) {
                double a = cloudPhase + (Math.PI * 2 * i / arc.cloudIds.size());
                double rr = 2.5;
                d.teleport(apex.clone().add(Math.cos(a) * rr, Math.sin(a * 0.7) * 0.5, Math.sin(a) * rr));
            }
        }

        // Rotate pots of gold + sparkle around them
        double potPhase = (tickCounter % 80) * (Math.PI * 2 / 80.0);
        for (int i = 0; i < arc.potIds.size(); i++) {
            Entity e = Bukkit.getEntity(arc.potIds.get(i));
            if (e instanceof ItemDisplay d) {
                Transformation tr = d.getTransformation();
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        tr.getTranslation(),
                        new AxisAngle4f((float) potPhase, 0, 1, 0),
                        tr.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)));
                if (tickCounter % 4 == 0) {
                    try {
                        world.spawnParticle(Particle.FIREWORK, d.getLocation().add(0, 0.5, 0),
                                3, 0.4, 0.4, 0.4, 0.05);
                    } catch (Throwable ignored) {}
                }
            }
        }

        // Butterflies: figure-8 paths around the arc
        for (int i = 0; i < arc.butterflyIds.size(); i++) {
            Entity e = Bukkit.getEntity(arc.butterflyIds.get(i));
            if (e instanceof ItemDisplay d) {
                double offset = (Math.PI * 2 * i / arc.butterflyIds.size());
                double phase = (tickCounter % 200) * (Math.PI * 2 / 200.0) + offset;
                // Figure-8 lemniscate
                double scale = RAINBOW_HALF_SPAN * 0.6;
                double denom = 1 + Math.sin(phase) * Math.sin(phase);
                double x = scale * Math.cos(phase) / denom;
                double y = RAINBOW_PEAK_HEIGHT * 0.6 + (scale * 0.3) * Math.sin(phase) * Math.cos(phase) / denom;
                double z = (scale * 0.3) * Math.sin(phase) * Math.cos(phase) / denom;
                d.teleport(arc.anchor.clone().add(x, arc.groundY - arc.anchor.getY() + y, z));
                Transformation tr = d.getTransformation();
                d.setInterpolationDuration(2);
                d.setInterpolationDelay(0);
                d.setTransformation(new Transformation(
                        tr.getTranslation(),
                        new AxisAngle4f((float) phase * 2, 0, 1, 0),
                        tr.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)));
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
        UUID hostId;
        final List<UUID> displays = new ArrayList<>();
        final List<UUID> foodIds = new ArrayList<>();
    }

    private static class RainbowArcInstance {
        int expiresAt;
        Location anchor;
        double groundY;
        final List<UUID> displays = new ArrayList<>();
        final List<UUID> cloudIds = new ArrayList<>();
        final List<UUID> potIds = new ArrayList<>();
        final List<UUID> butterflyIds = new ArrayList<>();
    }
}
