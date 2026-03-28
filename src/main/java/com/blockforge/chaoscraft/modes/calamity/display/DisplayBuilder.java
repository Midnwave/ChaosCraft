package com.blockforge.chaoscraft.modes.calamity.display;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for spawning and animating BlockDisplay/ItemDisplay entities.
 * Used by all attack classes to create visual structures.
 *
 * Features:
 * - Spawn BlockDisplay with any block type
 * - Spawn ItemDisplay with any item
 * - Set transformation (translation, rotation, scale)
 * - Configure interpolation for smooth animations
 * - Glow color, brightness, view range
 * - Particle attachment helpers
 * - Sound cue helpers
 * - All entities auto-tracked for cleanup
 *
 * Always spawns straight (yaw=0, pitch=0) per user requirements.
 */
public class DisplayBuilder {

    private final ChaosCraftPlugin plugin;
    private final List<Entity> entities = new ArrayList<>();

    public DisplayBuilder(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    // BlockDisplay creation
    // ========================

    /**
     * Spawn a BlockDisplay at the given location.
     * Returns a handle for further configuration.
     */
    public BlockDisplayHandle spawnBlock(Location location, Material material) {
        return spawnBlock(location, material.createBlockData());
    }

    public BlockDisplayHandle spawnBlock(Location location, BlockData blockData) {
        Location straight = location.clone();
        straight.setYaw(0);
        straight.setPitch(0);

        BlockDisplay display = location.getWorld().spawn(straight, BlockDisplay.class, d -> {
            d.setBlock(blockData);
            d.setBrightness(new Display.Brightness(15, 15));
            d.addScoreboardTag("chaoscraft_display"); // Tag for cleanup safety net
            d.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        });
        entities.add(display);
        return new BlockDisplayHandle(display);
    }

    /**
     * Spawn multiple blocks in a pattern relative to a center point.
     * Offsets are {x, y, z} relative to center.
     */
    public List<BlockDisplayHandle> spawnPattern(Location center, Material material, double[][] offsets) {
        List<BlockDisplayHandle> handles = new ArrayList<>();
        for (double[] offset : offsets) {
            Location loc = center.clone().add(offset[0], offset[1], offset[2]);
            handles.add(spawnBlock(loc, material));
        }
        return handles;
    }

    /**
     * Spawn a line of blocks from start to end.
     */
    public List<BlockDisplayHandle> spawnLine(Location start, Location end, Material material, int count) {
        List<BlockDisplayHandle> handles = new ArrayList<>();
        double dx = (end.getX() - start.getX()) / Math.max(1, count - 1);
        double dy = (end.getY() - start.getY()) / Math.max(1, count - 1);
        double dz = (end.getZ() - start.getZ()) / Math.max(1, count - 1);

        for (int i = 0; i < count; i++) {
            Location loc = start.clone().add(dx * i, dy * i, dz * i);
            handles.add(spawnBlock(loc, material));
        }
        return handles;
    }

    /**
     * Spawn blocks in a ring pattern (horizontal circle).
     */
    public List<BlockDisplayHandle> spawnRing(Location center, Material material, double radius, int count) {
        List<BlockDisplayHandle> handles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            handles.add(spawnBlock(center.clone().add(x, 0, z), material));
        }
        return handles;
    }

    /**
     * Spawn blocks in a sphere pattern.
     */
    public List<BlockDisplayHandle> spawnSphere(Location center, Material material, double radius, int count) {
        List<BlockDisplayHandle> handles = new ArrayList<>();
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            double y = 1 - (2.0 * i / (count - 1));
            double radiusAtY = Math.sqrt(1 - y * y);
            double theta = goldenAngle * i;
            double x = Math.cos(theta) * radiusAtY * radius;
            double z = Math.sin(theta) * radiusAtY * radius;
            handles.add(spawnBlock(center.clone().add(x, y * radius, z), material));
        }
        return handles;
    }

    /**
     * Spawn blocks in a vertical pillar.
     */
    public List<BlockDisplayHandle> spawnPillar(Location base, Material material, int height) {
        List<BlockDisplayHandle> handles = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            handles.add(spawnBlock(base.clone().add(0, y, 0), material));
        }
        return handles;
    }

    // ========================
    // ItemDisplay creation
    // ========================

    public ItemDisplayHandle spawnItem(Location location, ItemStack item) {
        Location straight = location.clone();
        straight.setYaw(0);
        straight.setPitch(0);

        ItemDisplay display = location.getWorld().spawn(straight, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setBrightness(new Display.Brightness(15, 15));
            d.addScoreboardTag("chaoscraft_display"); // Tag for cleanup safety net
            d.setTransformation(new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        });
        entities.add(display);
        return new ItemDisplayHandle(display);
    }

    // ========================
    // Particle helpers
    // ========================

    /**
     * Spawn dust particles with Calamity colors.
     */
    public static void dustParticles(Location location, int count, double spread,
                                      int r, int g, int b, float size) {
        if (location.getWorld() == null) return;
        location.getWorld().spawnParticle(Particle.DUST, location, count,
                spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(r, g, b), size));
    }

    /** Purple calamity dust */
    public static void purpleDust(Location location, int count, double spread) {
        dustParticles(location, count, spread, 128, 0, 255, 1.2f);
    }

    /** Cyan calamity dust */
    public static void cyanDust(Location location, int count, double spread) {
        dustParticles(location, count, spread, 0, 200, 255, 1.2f);
    }

    /** Crimson calamity dust */
    public static void crimsonDust(Location location, int count, double spread) {
        dustParticles(location, count, spread, 200, 0, 50, 1.2f);
    }

    /** Dark purple dust */
    public static void darkPurpleDust(Location location, int count, double spread) {
        dustParticles(location, count, spread, 80, 0, 160, 1.5f);
    }

    /**
     * Spawn a particle line between two points.
     */
    public static void particleLine(Location start, Location end, Particle particle,
                                     int density, Object data) {
        World world = start.getWorld();
        if (world == null) return;
        double dist = start.distance(end);
        int points = (int) (dist * density);
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            Location point = start.clone().add(
                    (end.getX() - start.getX()) * t,
                    (end.getY() - start.getY()) * t,
                    (end.getZ() - start.getZ()) * t
            );
            if (data != null) {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
            } else {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawn particles in a ring.
     */
    public static void particleRing(Location center, double radius, Particle particle,
                                     int points, Object data) {
        World world = center.getWorld();
        if (world == null) return;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            Location point = center.clone().add(
                    Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            if (data != null) {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0, data);
            } else {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
            }
        }
    }

    // ========================
    // Sound helpers
    // ========================

    public static void playSound(Location location, Sound sound, float volume, float pitch) {
        if (location.getWorld() == null) return;
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    // ========================
    // Cleanup
    // ========================

    /**
     * Remove all entities spawned by this builder.
     */
    public void removeAll() {
        for (Entity entity : entities) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        entities.clear();
    }

    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * Safety net: remove ALL block displays tagged "chaoscraft_display" in a world.
     * Call this on mode end to catch any orphaned displays that weren't cleaned up normally.
     */
    public static void cleanupAllDisplaysInWorld(org.bukkit.World world) {
        if (world == null) return;
        int removed = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains("chaoscraft_display")) {
                entity.remove();
                removed++;
            }
        }
        if (removed > 0) {
            org.bukkit.Bukkit.getLogger().info("[DisplayBuilder] Safety cleanup removed " + removed + " orphaned block displays in " + world.getName());
        }
    }

    // ========================
    // Display entity handles for fluent API
    // ========================

    /**
     * Fluent handle for configuring a BlockDisplay after spawn.
     */
    public static class BlockDisplayHandle {
        private final BlockDisplay display;

        BlockDisplayHandle(BlockDisplay display) {
            this.display = display;
        }

        public BlockDisplayHandle scale(float x, float y, float z) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(x, y, z),
                    t.getRightRotation()
            ));
            return this;
        }

        public BlockDisplayHandle translate(float x, float y, float z) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    new Vector3f(x, y, z),
                    t.getLeftRotation(),
                    t.getScale(),
                    t.getRightRotation()
            ));
            return this;
        }

        public BlockDisplayHandle rotate(float angle, float axisX, float axisY, float axisZ) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, axisX, axisY, axisZ),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            return this;
        }

        public BlockDisplayHandle glow(int r, int g, int b) {
            display.setGlowColorOverride(Color.fromRGB(r, g, b));
            display.setGlowing(true);
            return this;
        }

        public BlockDisplayHandle interpolation(int duration, int delay) {
            display.setInterpolationDuration(duration);
            display.setInterpolationDelay(delay);
            return this;
        }

        public BlockDisplayHandle brightness(int block, int sky) {
            display.setBrightness(new Display.Brightness(block, sky));
            return this;
        }

        public BlockDisplayHandle viewRange(float range) {
            display.setViewRange(range);
            return this;
        }

        public BlockDisplay entity() {
            return display;
        }

        /**
         * Animate to a new transformation over the given interpolation duration.
         */
        public BlockDisplayHandle animateTo(Vector3f translation, AxisAngle4f rotation,
                                             Vector3f scale, int durationTicks) {
            display.setInterpolationDuration(durationTicks);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(
                    translation, rotation, scale,
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            return this;
        }
    }

    /**
     * Fluent handle for configuring an ItemDisplay after spawn.
     */
    public static class ItemDisplayHandle {
        private final ItemDisplay display;

        ItemDisplayHandle(ItemDisplay display) {
            this.display = display;
        }

        public ItemDisplayHandle scale(float x, float y, float z) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(x, y, z),
                    t.getRightRotation()
            ));
            return this;
        }

        public ItemDisplayHandle translate(float x, float y, float z) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    new Vector3f(x, y, z),
                    t.getLeftRotation(),
                    t.getScale(),
                    t.getRightRotation()
            ));
            return this;
        }

        public ItemDisplayHandle rotate(float angle, float axisX, float axisY, float axisZ) {
            Transformation t = display.getTransformation();
            display.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, axisX, axisY, axisZ),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            return this;
        }

        public ItemDisplayHandle glow(int r, int g, int b) {
            display.setGlowColorOverride(Color.fromRGB(r, g, b));
            display.setGlowing(true);
            return this;
        }

        public ItemDisplayHandle interpolation(int duration, int delay) {
            display.setInterpolationDuration(duration);
            display.setInterpolationDelay(delay);
            return this;
        }

        public ItemDisplay entity() {
            return display;
        }

        public ItemDisplayHandle animateTo(Vector3f translation, AxisAngle4f rotation,
                                            Vector3f scale, int durationTicks) {
            display.setInterpolationDuration(durationTicks);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(
                    translation, rotation, scale,
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            return this;
        }
    }
}
