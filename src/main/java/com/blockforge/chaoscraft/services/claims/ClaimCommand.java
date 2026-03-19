package com.blockforge.chaoscraft.services.claims;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all claim-related commands registered as separate Bukkit commands.
 *
 * /claim [show|info|list|delete|resize] — Main claim management
 * /trust <player> [level]              — Grant trust on current claim
 * /untrust <player>                    — Remove trust on current claim
 * /trustlist                           — List trusted players on current claim
 * /claimblocks [buy <amount>]          — View/buy claim blocks
 */
public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private final ClaimsService claimsService;
    private final ClaimVisualization visualization;

    public ClaimCommand(ChaosCraftPlugin plugin, ClaimVisualization visualization) {
        this.plugin = plugin;
        this.claimsService = plugin.getClaimsService();
        this.visualization = visualization;
    }

    // ========================
    // Command routing
    // ========================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.claims.use")) {
            sender.sendMessage(Component.text("You don't have permission to use claim commands.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use claim commands.", NamedTextColor.RED));
            return true;
        }

        if (!claimsService.isEnabled()) {
            player.sendMessage(Component.text("The claims system is currently disabled.", NamedTextColor.RED));
            return true;
        }

        return switch (command.getName().toLowerCase()) {
            case "claim" -> handleClaim(player, args);
            case "trust" -> handleTrust(player, args);
            case "untrust" -> handleUntrust(player, args);
            case "trustlist" -> handleTrustList(player);
            case "claimblocks" -> handleClaimBlocks(player, args);
            default -> false;
        };
    }

    // ========================
    // /claim
    // ========================

    private boolean handleClaim(Player player, String[] args) {
        if (args.length == 0) {
            sendClaimHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "show" -> handleClaimShow(player);
            case "info" -> handleClaimInfo(player);
            case "list" -> handleClaimList(player);
            case "delete" -> handleClaimDelete(player);
            case "resize" -> handleClaimResize(player);
            default -> { sendClaimHelp(player); yield true; }
        };
    }

    /**
     * /claim show — Visualize the claim the player is standing in.
     */
    private boolean handleClaimShow(Player player) {
        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        visualization.showClaim(player, claim);
        player.sendMessage(Component.text("Showing claim boundaries for claim #" + claim.getId() + ".", NamedTextColor.GREEN));
        return true;
    }

    /**
     * /claim info — Show detailed info about the claim at the player's location.
     */
    private boolean handleClaimInfo(Player player) {
        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        String ownerName = resolvePlayerName(claim.getOwner());

        player.sendMessage(Component.text("=== Claim Info (#" + claim.getId() + ") ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Owner: ", NamedTextColor.AQUA)
                .append(Component.text(ownerName, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("World: ", NamedTextColor.AQUA)
                .append(Component.text(claim.getWorldName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Size: ", NamedTextColor.AQUA)
                .append(Component.text(claim.getWidth() + "x" + claim.getLength() + " (" + claim.getArea() + " blocks)", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Corners: ", NamedTextColor.AQUA)
                .append(Component.text("[" + claim.getMinX() + ", " + claim.getMinZ() + "] to [" + claim.getMaxX() + ", " + claim.getMaxZ() + "]", NamedTextColor.WHITE)));

        // Trust list
        Map<UUID, TrustLevel> trusts = claim.getTrusts();
        if (trusts.isEmpty()) {
            player.sendMessage(Component.text("Trusts: ", NamedTextColor.AQUA)
                    .append(Component.text("None", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(Component.text("Trusts:", NamedTextColor.AQUA));
            for (Map.Entry<UUID, TrustLevel> entry : trusts.entrySet()) {
                String name = resolvePlayerName(entry.getKey());
                player.sendMessage(Component.text("  " + name + " — " + entry.getValue().getDisplayName(), NamedTextColor.WHITE));
            }
        }

        // Flags
        player.sendMessage(Component.text("Flags:", NamedTextColor.AQUA));
        for (Map.Entry<ClaimFlag, Boolean> entry : claim.getFlags().entrySet()) {
            ClaimFlag flag = entry.getKey();
            boolean value = entry.getValue();
            NamedTextColor valueColor = value ? NamedTextColor.GREEN : NamedTextColor.RED;
            player.sendMessage(Component.text("  " + flag.getConfigKey() + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value ? "ON" : "OFF", valueColor)));
        }

        return true;
    }

    /**
     * /claim list — List all claims owned by the player.
     */
    private boolean handleClaimList(Player player) {
        List<Claim> claims = claimsService.getPlayerClaims(player.getUniqueId());
        if (claims.isEmpty()) {
            player.sendMessage(Component.text("You don't own any claims.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("=== Your Claims (" + claims.size() + ") ===", NamedTextColor.GOLD));
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            player.sendMessage(Component.text((i + 1) + ". ", NamedTextColor.AQUA)
                    .append(Component.text("#" + claim.getId(), NamedTextColor.WHITE))
                    .append(Component.text(" — " + claim.getWorldName(), NamedTextColor.GRAY))
                    .append(Component.text(" — " + claim.getWidth() + "x" + claim.getLength() + " (" + claim.getArea() + " blocks)", NamedTextColor.GRAY))
                    .append(Component.text(" — [" + claim.getMinX() + ", " + claim.getMinZ() + "]", NamedTextColor.GRAY)));
        }

        return true;
    }

    /**
     * /claim delete — Delete the claim the player is standing in.
     */
    private boolean handleClaimDelete(Player player) {
        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        // Must be owner or admin
        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("chaoscraft.claims.admin")) {
            player.sendMessage(Component.text("You can only delete claims you own.", NamedTextColor.RED));
            return true;
        }

        boolean success = claimsService.deleteClaim(claim, player);
        if (success) {
            player.sendMessage(Component.text("Claim #" + claim.getId() + " has been deleted.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to delete claim #" + claim.getId() + ". Deletion was cancelled.", NamedTextColor.RED));
        }

        return true;
    }

    /**
     * /claim resize — Instruct the player on how to resize.
     */
    private boolean handleClaimResize(Player player) {
        player.sendMessage(Component.text("Use your golden shovel to resize: right-click two new corners.", NamedTextColor.AQUA));
        return true;
    }

    // ========================
    // /trust
    // ========================

    /**
     * /trust <player> [level] — Grant trust on the claim the player is standing in.
     */
    private boolean handleTrust(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /trust <player> [level]", NamedTextColor.RED));
            player.sendMessage(Component.text("Levels: access, container, build, permission", NamedTextColor.GRAY));
            return true;
        }

        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        // Must be owner or have PERMISSION trust or be admin
        if (!claim.getOwner().equals(player.getUniqueId())
                && !claim.hasTrust(player.getUniqueId(), TrustLevel.PERMISSION)
                && !player.hasPermission("chaoscraft.claims.admin")) {
            player.sendMessage(Component.text("You don't have permission to manage trusts on this claim.", NamedTextColor.RED));
            return true;
        }

        // Resolve target player
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        if (target.getUniqueId().equals(claim.getOwner())) {
            player.sendMessage(Component.text("The claim owner already has full access.", NamedTextColor.RED));
            return true;
        }

        // Parse trust level (default to BUILD)
        TrustLevel level = TrustLevel.BUILD;
        if (args.length >= 2) {
            level = TrustLevel.fromString(args[1]);
            if (level == null) {
                player.sendMessage(Component.text("Invalid trust level: " + args[1], NamedTextColor.RED));
                player.sendMessage(Component.text("Valid levels: access, container, build, permission", NamedTextColor.GRAY));
                return true;
            }
        }

        boolean success = claimsService.addTrust(claim, player, target.getUniqueId(), level);
        if (success) {
            player.sendMessage(Component.text("Granted " + level.getDisplayName() + " trust to " + target.getName() + " on claim #" + claim.getId() + ".", NamedTextColor.GREEN));
            // Notify the target player if online
            target.sendMessage(Component.text(player.getName() + " granted you " + level.getDisplayName() + " trust on their claim.", NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("Failed to set trust. The action was cancelled.", NamedTextColor.RED));
        }

        return true;
    }

    // ========================
    // /untrust
    // ========================

    /**
     * /untrust <player> — Remove all trusts from a player on the current claim.
     */
    private boolean handleUntrust(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /untrust <player>", NamedTextColor.RED));
            return true;
        }

        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        // Must be owner or have PERMISSION trust or be admin
        if (!claim.getOwner().equals(player.getUniqueId())
                && !claim.hasTrust(player.getUniqueId(), TrustLevel.PERMISSION)
                && !player.hasPermission("chaoscraft.claims.admin")) {
            player.sendMessage(Component.text("You don't have permission to manage trusts on this claim.", NamedTextColor.RED));
            return true;
        }

        // Resolve target — try exact online match first, then check UUID in trusts
        Player onlineTarget = Bukkit.getPlayerExact(args[0]);
        UUID targetUUID = null;
        String targetName = args[0];

        if (onlineTarget != null) {
            targetUUID = onlineTarget.getUniqueId();
            targetName = onlineTarget.getName();
        } else {
            // Search through existing trusts to find an offline player by name
            for (UUID trusted : claim.getTrusts().keySet()) {
                String name = resolvePlayerName(trusted);
                if (name.equalsIgnoreCase(args[0])) {
                    targetUUID = trusted;
                    targetName = name;
                    break;
                }
            }
        }

        if (targetUUID == null) {
            player.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        if (claim.getTrust(targetUUID) == null) {
            player.sendMessage(Component.text(targetName + " is not trusted on this claim.", NamedTextColor.RED));
            return true;
        }

        boolean success = claimsService.removeTrust(claim, player, targetUUID);
        if (success) {
            player.sendMessage(Component.text("Removed all trust from " + targetName + " on claim #" + claim.getId() + ".", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to remove trust. The action was cancelled.", NamedTextColor.RED));
        }

        return true;
    }

    // ========================
    // /trustlist
    // ========================

    /**
     * /trustlist — Show all trusted players and their levels on the current claim.
     */
    private boolean handleTrustList(Player player) {
        Claim claim = claimsService.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(Component.text("You are not standing in a claim.", NamedTextColor.RED));
            return true;
        }

        Map<UUID, TrustLevel> trusts = claim.getTrusts();
        String ownerName = resolvePlayerName(claim.getOwner());

        player.sendMessage(Component.text("=== Trust List — Claim #" + claim.getId() + " ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Owner: " + ownerName, NamedTextColor.AQUA));

        if (trusts.isEmpty()) {
            player.sendMessage(Component.text("No trusted players.", NamedTextColor.GRAY));
        } else {
            // Group by trust level for cleaner display
            Map<TrustLevel, List<String>> grouped = new LinkedHashMap<>();
            for (TrustLevel level : TrustLevel.values()) {
                grouped.put(level, new ArrayList<>());
            }
            for (Map.Entry<UUID, TrustLevel> entry : trusts.entrySet()) {
                String name = resolvePlayerName(entry.getKey());
                grouped.get(entry.getValue()).add(name);
            }

            for (TrustLevel level : TrustLevel.values()) {
                List<String> players = grouped.get(level);
                if (!players.isEmpty()) {
                    player.sendMessage(Component.text(level.getDisplayName() + ": ", NamedTextColor.AQUA)
                            .append(Component.text(String.join(", ", players), NamedTextColor.WHITE)));
                    player.sendMessage(Component.text("  " + level.getDescription(), NamedTextColor.GRAY));
                }
            }
        }

        return true;
    }

    // ========================
    // /claimblocks
    // ========================

    /**
     * /claimblocks [buy <amount>] — View or buy claim blocks.
     */
    private boolean handleClaimBlocks(Player player, String[] args) {
        if (args.length == 0) {
            return handleClaimBlocksInfo(player);
        }

        String sub = args[0].toLowerCase();
        if ("buy".equals(sub)) {
            return handleClaimBlocksBuy(player, args);
        }

        // Unknown subcommand — show info
        return handleClaimBlocksInfo(player);
    }

    /**
     * /claimblocks — Show available, used, and remaining claim blocks.
     */
    private boolean handleClaimBlocksInfo(Player player) {
        UUID uuid = player.getUniqueId();
        int total = claimsService.getClaimBlocks(uuid);
        int used = claimsService.getUsedClaimBlocks(uuid);
        int remaining = total - used;

        player.sendMessage(Component.text("=== Claim Blocks ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Total: ", NamedTextColor.AQUA)
                .append(Component.text(String.valueOf(total), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Used: ", NamedTextColor.AQUA)
                .append(Component.text(String.valueOf(used), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Remaining: ", NamedTextColor.AQUA)
                .append(Component.text(String.valueOf(remaining), remaining >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED)));

        // Show earning info
        ClaimBlockTracker tracker = claimsService.getBlockTracker();
        player.sendMessage(Component.text("", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Earning rates:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  Playtime: ", NamedTextColor.GRAY)
                .append(Component.text(tracker.getPlaytimeBlocksPerHour() + " blocks/hour", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Voting: ", NamedTextColor.GRAY)
                .append(Component.text(tracker.getVoteBlocks() + " blocks/vote", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Mode survival: ", NamedTextColor.GRAY)
                .append(Component.text(tracker.getModeSurvivalBonus() + " blocks/survival", NamedTextColor.WHITE)));

        if (tracker.isEconomyEnabled()) {
            player.sendMessage(Component.text("  Buy: ", NamedTextColor.GRAY)
                    .append(Component.text("/claimblocks buy <amount> ($" + String.format("%.2f", tracker.getPricePerBlock()) + "/block)", NamedTextColor.WHITE)));
        }

        return true;
    }

    /**
     * /claimblocks buy <amount> — Buy claim blocks with economy.
     */
    private boolean handleClaimBlocksBuy(Player player, String[] args) {
        ClaimBlockTracker tracker = claimsService.getBlockTracker();

        if (!tracker.isEconomyEnabled()) {
            player.sendMessage(Component.text("Buying claim blocks is currently disabled.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /claimblocks buy <amount>", NamedTextColor.RED));
            player.sendMessage(Component.text("Price: $" + String.format("%.2f", tracker.getPricePerBlock()) + " per block.", NamedTextColor.GRAY));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount: " + args[1], NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        double cost = amount * tracker.getPricePerBlock();
        boolean success = tracker.buyClaimBlocks(player, amount);

        if (success) {
            player.sendMessage(Component.text("Purchased " + amount + " claim blocks for $" + String.format("%.2f", cost) + ".", NamedTextColor.GREEN));
            // Show updated totals
            int total = claimsService.getClaimBlocks(player.getUniqueId());
            int used = claimsService.getUsedClaimBlocks(player.getUniqueId());
            player.sendMessage(Component.text("New balance: " + (total - used) + " blocks remaining (" + total + " total).", NamedTextColor.AQUA));
        } else {
            player.sendMessage(Component.text("Purchase failed. You may not have enough money ($" + String.format("%.2f", cost) + " needed) or economy is unavailable.", NamedTextColor.RED));
        }

        return true;
    }

    // ========================
    // Help
    // ========================

    private void sendClaimHelp(Player player) {
        player.sendMessage(Component.text("=== Claim Commands ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/claim show", NamedTextColor.AQUA)
                .append(Component.text(" — Visualize claim boundaries", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/claim info", NamedTextColor.AQUA)
                .append(Component.text(" — Show claim details (owner, size, trusts, flags)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/claim list", NamedTextColor.AQUA)
                .append(Component.text(" — List all your claims", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/claim delete", NamedTextColor.AQUA)
                .append(Component.text(" — Delete the claim you're standing in", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/claim resize", NamedTextColor.AQUA)
                .append(Component.text(" — Resize instructions", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/trust <player> [level]", NamedTextColor.AQUA)
                .append(Component.text(" — Grant trust (default: build)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/untrust <player>", NamedTextColor.AQUA)
                .append(Component.text(" — Remove player's trust", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trustlist", NamedTextColor.AQUA)
                .append(Component.text(" — Show trusted players on current claim", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/claimblocks", NamedTextColor.AQUA)
                .append(Component.text(" — View your claim block balance", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/claimblocks buy <amount>", NamedTextColor.AQUA)
                .append(Component.text(" — Purchase claim blocks", NamedTextColor.GRAY)));
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.claims.use")) {
            return Collections.emptyList();
        }

        String cmdName = command.getName().toLowerCase();
        return switch (cmdName) {
            case "claim" -> tabCompleteClaim(args);
            case "trust" -> tabCompleteTrust(args);
            case "untrust" -> tabCompleteUntrust(args);
            case "trustlist" -> Collections.emptyList();
            case "claimblocks" -> tabCompleteClaimBlocks(args);
            default -> Collections.emptyList();
        };
    }

    /**
     * /claim <sub>
     */
    private List<String> tabCompleteClaim(String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], "show", "info", "list", "delete", "resize");
        }
        return Collections.emptyList();
    }

    /**
     * /trust <player> [level]
     */
    private List<String> tabCompleteTrust(String[] args) {
        if (args.length == 1) {
            // Online player names
            return filterStartsWith(args[0], getOnlinePlayerNames());
        }
        if (args.length == 2) {
            // Trust levels
            return filterStartsWith(args[1], "access", "container", "build", "permission");
        }
        return Collections.emptyList();
    }

    /**
     * /untrust <player>
     */
    private List<String> tabCompleteUntrust(String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    /**
     * /claimblocks [buy]
     */
    private List<String> tabCompleteClaimBlocks(String[] args) {
        if (args.length == 1) {
            return filterStartsWith(args[0], "buy");
        }
        if (args.length == 2 && "buy".equalsIgnoreCase(args[0])) {
            return List.of("10", "50", "100", "500", "1000");
        }
        return Collections.emptyList();
    }

    // ========================
    // Helpers
    // ========================

    /**
     * Resolve a UUID to a player name. Uses online player first, falls back to offline player cache.
     */
    private String resolvePlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        // Fallback to offline player name (may be null if never joined)
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : uuid.toString();
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(String input, String... options) {
        return filterStartsWith(input, Arrays.asList(options));
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
