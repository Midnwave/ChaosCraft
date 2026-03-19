package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit Listener that enforces claim protections.
 * Handles block, container, access, explosion, mob griefing, fire, PvP,
 * piston, mob spawning, bucket, entity interaction, and golden shovel selection.
 */
public class ClaimListener implements Listener {

    private final ChaosCraftPlugin plugin;
    private final ClaimManager manager;

    // Cooldown map to prevent spam messages (player UUID -> last message timestamp)
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 1000;

    // Golden shovel claim selection: first corner storage
    private final Map<UUID, Location> firstCornerSelections = new ConcurrentHashMap<>();

    // Container materials
    private static final Set<Material> CONTAINERS = EnumSet.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.BARREL,
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.HOPPER,
            Material.DROPPER,
            Material.DISPENSER,
            Material.BREWING_STAND,
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
    );

    // Access-interactable materials
    private static final Set<Material> ACCESS_BLOCKS = EnumSet.noneOf(Material.class);

    static {
        // Doors
        for (Material mat : Material.values()) {
            String name = mat.name();
            if (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_FENCE_GATE")) {
                ACCESS_BLOCKS.add(mat);
            }
        }
        // Buttons
        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_BUTTON")) {
                ACCESS_BLOCKS.add(mat);
            }
        }
        // Pressure plates
        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_PRESSURE_PLATE")) {
                ACCESS_BLOCKS.add(mat);
            }
        }
        ACCESS_BLOCKS.add(Material.LEVER);
    }

    public ClaimListener(ChaosCraftPlugin plugin, ClaimManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ========================
    // Message Utility
    // ========================

    /**
     * Send a denial message to the player, rate-limited to once per second.
     */
    private void sendDeniedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MS) return;

        messageCooldowns.put(player.getUniqueId(), now);
        player.sendMessage(Component.text("You don't have permission to do that in this claim.", NamedTextColor.RED));
    }

    // ========================
    // 1. Block Break / Place
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!manager.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!manager.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    // ========================
    // 2 & 3. Container / Access Protection
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Material type = block.getType();

        // Golden shovel detection (priority over claim checks)
        if (event.getItem() != null && event.getItem().getType() == Material.GOLDEN_SHOVEL) {
            handleGoldenShovel(player, block.getLocation());
            event.setCancelled(true);
            return;
        }

        // Container check
        if (CONTAINERS.contains(type)) {
            if (!manager.canOpenContainer(player, block.getLocation())) {
                event.setCancelled(true);
                sendDeniedMessage(player);
            }
            return;
        }

        // Access check
        if (ACCESS_BLOCKS.contains(type)) {
            if (!manager.canAccess(player, block.getLocation())) {
                event.setCancelled(true);
                sendDeniedMessage(player);
            }
        }
    }

    // ========================
    // 4. Explosion Protection
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Claim claim = manager.getClaimAt(block.getLocation());
            return claim != null && !claim.getFlag(ClaimFlag.EXPLOSIONS);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Claim claim = manager.getClaimAt(block.getLocation());
            return claim != null && !claim.getFlag(ClaimFlag.EXPLOSIONS);
        });
    }

    // ========================
    // 5. Mob Griefing
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Enderman) && !(entity instanceof Ravager)) return;

        Claim claim = manager.getClaimAt(event.getBlock().getLocation());
        if (claim != null && !claim.getFlag(ClaimFlag.MOB_GRIEFING)) {
            event.setCancelled(true);
        }
    }

    // ========================
    // 6. Fire Spread
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE && event.getNewState().getType() != Material.FIRE) return;

        Claim claim = manager.getClaimAt(event.getBlock().getLocation());
        if (claim != null && !claim.getFlag(ClaimFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }

    // ========================
    // 7. PvP Protection
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null) return;

        // Check PvP flag at both the victim's and attacker's location
        Claim victimClaim = manager.getClaimAt(victim.getLocation());
        Claim attackerClaim = manager.getClaimAt(attacker.getLocation());

        boolean pvpDisabledAtVictim = victimClaim != null && !victimClaim.getFlag(ClaimFlag.PVP);
        boolean pvpDisabledAtAttacker = attackerClaim != null && !attackerClaim.getFlag(ClaimFlag.PVP);

        if (pvpDisabledAtVictim || pvpDisabledAtAttacker) {
            event.setCancelled(true);
            sendDeniedMessage(attacker);
        }
    }

    /**
     * Resolve the actual player attacker from an entity (handles projectiles).
     */
    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) return player;
        }
        if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) return player;
        }
        return null;
    }

    // ========================
    // 8. Piston Protection
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (handlePistonMovement(event.getBlock().getLocation(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (handlePistonMovement(event.getBlock().getLocation(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    /**
     * Check if a piston movement would push/pull blocks into or out of a claim
     * that has piston protection enabled.
     */
    private boolean handlePistonMovement(Location pistonLoc, List<Block> blocks, org.bukkit.block.BlockFace direction) {
        Claim pistonClaim = manager.getClaimAt(pistonLoc);

        for (Block block : blocks) {
            Location blockLoc = block.getLocation();
            Location destLoc = block.getRelative(direction).getLocation();

            Claim blockClaim = manager.getClaimAt(blockLoc);
            Claim destClaim = manager.getClaimAt(destLoc);

            // Block being pushed out of a claim
            if (blockClaim != null && blockClaim.getFlag(ClaimFlag.PISTON_PROTECTION)) {
                if (destClaim == null || destClaim.getId() != blockClaim.getId()) {
                    return true; // Pushing block out of protected claim
                }
            }

            // Block being pushed into a claim
            if (destClaim != null && destClaim.getFlag(ClaimFlag.PISTON_PROTECTION)) {
                if (pistonClaim == null || pistonClaim.getId() != destClaim.getId()) {
                    return true; // External piston pushing into protected claim
                }
            }
        }
        return false;
    }

    // ========================
    // 9. Mob Spawning
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        Claim claim = manager.getClaimAt(event.getLocation());
        if (claim != null && !claim.getFlag(ClaimFlag.MOB_SPAWNING)) {
            event.setCancelled(true);
        }
    }

    // ========================
    // 10. Bucket Use
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (!manager.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (!manager.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    // ========================
    // 11. Entity Interaction
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame) && !(entity instanceof ArmorStand)) return;

        Player player = event.getPlayer();
        if (!manager.canBuild(player, entity.getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    // ========================
    // 12. Golden Shovel Selection
    // ========================

    /**
     * Handle golden shovel click for claim creation.
     * First click sets corner 1, second click creates the claim.
     */
    private void handleGoldenShovel(Player player, Location clickedLocation) {
        UUID uuid = player.getUniqueId();

        if (!player.hasPermission("chaoscraft.claims.create")) {
            player.sendMessage(Component.text("You don't have permission to create claims.", NamedTextColor.RED));
            return;
        }

        if (!firstCornerSelections.containsKey(uuid)) {
            // First click — set corner 1
            firstCornerSelections.put(uuid, clickedLocation);
            player.sendMessage(Component.text("Claim corner 1 set at ", NamedTextColor.GOLD)
                    .append(Component.text(formatLocation(clickedLocation), NamedTextColor.YELLOW))
                    .append(Component.text(". Right-click another block to set corner 2.", NamedTextColor.GOLD)));
        } else {
            // Second click — create the claim
            Location corner1 = firstCornerSelections.remove(uuid);
            Location corner2 = clickedLocation;

            // Validate same world
            if (corner1.getWorld() == null || corner2.getWorld() == null
                    || !corner1.getWorld().equals(corner2.getWorld())) {
                player.sendMessage(Component.text("Both corners must be in the same world.", NamedTextColor.RED));
                return;
            }

            // Show claim dimensions before creating
            int width = Math.abs(corner2.getBlockX() - corner1.getBlockX()) + 1;
            int length = Math.abs(corner2.getBlockZ() - corner1.getBlockZ()) + 1;
            int area = width * length;

            player.sendMessage(Component.text("Creating claim: ", NamedTextColor.GOLD)
                    .append(Component.text(width + "x" + length, NamedTextColor.YELLOW))
                    .append(Component.text(" (" + area + " claim blocks needed)", NamedTextColor.GOLD)));

            Claim claim = manager.createClaim(player, corner1, corner2);
            if (claim != null) {
                player.sendMessage(Component.text("Claim created successfully! (ID: " + claim.getId() + ")", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Could not create claim. Possible reasons:", NamedTextColor.RED));
                player.sendMessage(Component.text(" - Claim is too small (minimum size required)", NamedTextColor.GRAY));
                player.sendMessage(Component.text(" - Not enough claim blocks available", NamedTextColor.GRAY));
                player.sendMessage(Component.text(" - Overlaps with an existing claim", NamedTextColor.GRAY));
                player.sendMessage(Component.text(" - Maximum claim count reached", NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Cancel a player's pending claim selection (useful for cleanup on quit).
     */
    public void cancelSelection(UUID uuid) {
        firstCornerSelections.remove(uuid);
    }

    /**
     * Check if a player has a pending first corner selection.
     */
    public boolean hasPendingSelection(UUID uuid) {
        return firstCornerSelections.containsKey(uuid);
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }
}
