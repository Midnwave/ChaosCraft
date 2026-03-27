package com.blockforge.chaoscraft.api.mode;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;

/**
 * Handles end-of-mode results: titles, sounds, chat rewards summary,
 * and distribution of money/items/badges/commands.
 *
 * Called 10 seconds after a mode ends. Only players who stayed online
 * the entire mode receive full rewards.
 *
 * Config per mode (in mode YAML):
 *   rewards:
 *     survived:
 *       money: 500
 *       items:
 *         - "diamond 3"
 *         - "golden_apple 1"
 *       badges:
 *         - "survivor_chain"
 *       commands:
 *         - "give %player% experience_bottle 5"
 *     died:
 *       money: 100
 *       items: []
 *       badges: []
 *       commands: []
 *     bonus:
 *       tutorial-complete:
 *         money: 1000
 *         badge: "tutorial_master"
 *         commands:
 *           - "broadcast %player% completed the tutorial!"
 */
public class ModeResultsService {

    private final ChaosCraftPlugin plugin;

    // Track players who were online when the mode started (only these get rewards)
    private final Set<UUID> sessionPlayers = new HashSet<>();

    public ModeResultsService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Track a player as being in this session (called when mode starts).
     */
    public void trackPlayer(UUID uuid) {
        sessionPlayers.add(uuid);
    }

    /**
     * Called when mode ends. Schedules results display after 10 seconds.
     */
    public void scheduleResults(AbstractMode mode) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> displayResults(mode), 200L); // 10 seconds
    }

    /**
     * Display results to all tracked players.
     */
    private void displayResults(AbstractMode mode) {
        var config = mode.getModeConfig().get();

        // Global sounds from main config.yml
        String survivedSound = plugin.getConfig().getString("results.survived-sound", "minecraft:entity.player.levelup");
        String diedSound = plugin.getConfig().getString("results.died-sound", "minecraft:entity.villager.no");

        for (UUID uuid : sessionPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            boolean survived = mode.hasSurvived(player);

            // Title
            showResultTitle(player, survived);

            // Sound
            String soundId = survived ? survivedSound : diedSound;
            try {
                player.playSound(player.getLocation(), soundId, org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
            } catch (Exception ignored) {}

            // Chat results
            sendChatResults(player, survived, mode.isBossKilled(), config, mode.getName());

            // Distribute rewards
            distributeRewards(player, survived, mode.isBossKilled(), config);
        }

        sessionPlayers.clear();
    }

    /**
     * Show survived/died title.
     */
    private void showResultTitle(Player player, boolean survived) {
        Component title = Component.text("Mode Has Ended", NamedTextColor.AQUA, TextDecoration.BOLD);
        Component subtitle;
        if (survived) {
            subtitle = Component.text("You Survived", NamedTextColor.GREEN, TextDecoration.BOLD);
        } else {
            subtitle = Component.text("You Did Not Survive", NamedTextColor.DARK_RED, TextDecoration.BOLD);
        }

        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(8), Duration.ofMillis(1000))));
    }

    /**
     * Send chat message with reward summary.
     */
    private void sendChatResults(Player player, boolean survived, boolean bossKilled, org.bukkit.configuration.file.FileConfiguration config, String modeName) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text(" Mode Results ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("— " + modeName.toUpperCase(), NamedTextColor.GRAY)));
        player.sendMessage(Component.empty());

        if (survived) {
            player.sendMessage(Component.text(" ✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .append(Component.text("You survived!", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text(" ✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                    .append(Component.text("You did not survive.", NamedTextColor.RED)));
        }

        player.sendMessage(Component.empty());

        // Show rewards
        String rewardPath = survived ? "rewards.survived" : "rewards.died";
        ConfigurationSection rewardSection = config.getConfigurationSection(rewardPath);
        if (rewardSection != null) {
            double money = rewardSection.getDouble("money", 0);
            List<String> items = rewardSection.getStringList("items");
            List<String> badges = rewardSection.getStringList("badges");

            player.sendMessage(Component.text(" Rewards:", NamedTextColor.GOLD, TextDecoration.BOLD));

            // Build context string: "for Surviving Tutorial" or "for Not Surviving Tutorial"
            String modeDisplayName = modeName.substring(0, 1).toUpperCase() + modeName.substring(1);
            String reason = survived ? "Surviving " + modeDisplayName : "Playing " + modeDisplayName;

            if (money > 0) {
                player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                        .append(Component.text("$" + String.format("%.0f", money), NamedTextColor.GOLD))
                        .append(Component.text(" for " + reason, NamedTextColor.GRAY)));
            }

            for (String itemStr : items) {
                String[] parts = itemStr.split(" ");
                String name = parts[0].replace("_", " ");
                String qty = parts.length > 1 ? parts[1] : "1";
                player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                        .append(Component.text(qty + "x " + name, NamedTextColor.WHITE))
                        .append(Component.text(" for " + reason, NamedTextColor.GRAY)));
            }

            for (String badge : badges) {
                player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                        .append(Component.text("Badge: " + badge, NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" for " + reason, NamedTextColor.GRAY)));
            }

            if (money <= 0 && items.isEmpty() && badges.isEmpty()) {
                player.sendMessage(Component.text("  No rewards", NamedTextColor.GRAY));
            }
        }

        // Boss-killed bonus rewards (only if boss was killed AND player survived)
        if (bossKilled && survived) {
            ConfigurationSection bossSection = config.getConfigurationSection("rewards.boss-killed");
            if (bossSection != null) {
                double bossMoney = bossSection.getDouble("money", 0);
                List<String> bossItems = bossSection.getStringList("items");
                List<String> bossBadges = bossSection.getStringList("badges");

                if (bossMoney > 0 || !bossItems.isEmpty() || !bossBadges.isEmpty()) {
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text(" Boss Killed Bonus:", NamedTextColor.GOLD, TextDecoration.BOLD));

                    String modeDisplayName2 = modeName.substring(0, 1).toUpperCase() + modeName.substring(1);
                    String bossReason = "Beating " + modeDisplayName2;

                    if (bossMoney > 0) {
                        player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                                .append(Component.text("$" + String.format("%.0f", bossMoney), NamedTextColor.GOLD))
                                .append(Component.text(" for " + bossReason, NamedTextColor.GRAY)));
                    }
                    for (String itemStr : bossItems) {
                        String[] parts = itemStr.split(" ");
                        String itemName = parts[0].replace("_", " ");
                        String qty = parts.length > 1 ? parts[1] : "1";
                        player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                                .append(Component.text(qty + "x " + itemName, NamedTextColor.WHITE))
                                .append(Component.text(" for " + bossReason, NamedTextColor.GRAY)));
                    }
                    for (String badge : bossBadges) {
                        player.sendMessage(Component.text("  + ", NamedTextColor.GREEN)
                                .append(Component.text("Badge: " + badge, NamedTextColor.LIGHT_PURPLE))
                                .append(Component.text(" for " + bossReason, NamedTextColor.GRAY)));
                    }
                }
            }
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("═══════════════════════════════", NamedTextColor.DARK_GRAY));
    }

    /**
     * Distribute actual rewards: money, items, badges, commands.
     */
    private void distributeRewards(Player player, boolean survived, boolean bossKilled, org.bukkit.configuration.file.FileConfiguration config) {
        String rewardPath = survived ? "rewards.survived" : "rewards.died";
        ConfigurationSection section = config.getConfigurationSection(rewardPath);
        if (section != null) {
            grantRewardSection(player, section);
        }

        // Boss-killed bonus (only if boss was killed AND player survived)
        if (bossKilled && survived) {
            ConfigurationSection bossSection = config.getConfigurationSection("rewards.boss-killed");
            if (bossSection != null) {
                grantRewardSection(player, bossSection);
            }
        }
    }

    /**
     * Grant rewards from a single config section (money, items, badges, commands).
     */
    private void grantRewardSection(Player player, ConfigurationSection section) {

        // Money (Vault)
        double money = section.getDouble("money", 0);
        if (money > 0) {
            var economy = plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (economy != null) {
                economy.getProvider().depositPlayer(player, money);
            }
        }

        // Items
        for (String itemStr : section.getStringList("items")) {
            String[] parts = itemStr.split(" ");
            Material mat = Material.matchMaterial(parts[0]);
            int qty = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            if (mat != null) {
                var remaining = player.getInventory().addItem(new ItemStack(mat, qty));
                // Overflow to virtual storage
                if (!remaining.isEmpty() && plugin.getClass().getSimpleName().contains("ChaosCraft")) {
                    // Drop at player's feet if no virtual storage
                    for (ItemStack leftover : remaining.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }
        }

        // Badges
        var badgeService = plugin.getBadgeService();
        if (badgeService != null) {
            for (String badgeId : section.getStringList("badges")) {
                badgeService.grantBadge(player, badgeId);
            }
        }

        // Commands
        for (String cmd : section.getStringList("commands")) {
            String resolved = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }
    }

    /**
     * Grant a bonus reward by key (e.g., "tutorial-complete").
     * Called by mode-specific code when a bonus condition is met.
     */
    public void grantBonus(Player player, String bonusKey, org.bukkit.configuration.file.FileConfiguration config) {
        ConfigurationSection bonusSection = config.getConfigurationSection("rewards.bonus." + bonusKey);
        if (bonusSection == null) return;

        double money = bonusSection.getDouble("money", 0);
        if (money > 0) {
            var economy = plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (economy != null) {
                economy.getProvider().depositPlayer(player, money);
            }
        }

        String badge = bonusSection.getString("badge", "");
        if (!badge.isEmpty() && plugin.getBadgeService() != null) {
            plugin.getBadgeService().grantBadge(player, badge);
        }

        for (String cmd : bonusSection.getStringList("commands")) {
            String resolved = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        // Notify player
        String bonusLabel = bonusKey.replace("-", " ");
        bonusLabel = bonusLabel.substring(0, 1).toUpperCase() + bonusLabel.substring(1);
        player.sendMessage(Component.text(" ★ Bonus: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("for " + bonusLabel, NamedTextColor.YELLOW)));
    }
}
