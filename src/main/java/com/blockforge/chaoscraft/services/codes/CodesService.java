package com.blockforge.chaoscraft.services.codes;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roblox-style promo code system: create/redeem codes with item, Vault,
 * and command rewards. Includes blacklisting after repeated failures.
 */
public class CodesService {

    private final ChaosCraftPlugin plugin;
    private CodesConfig config;
    private BlacklistData blacklistData;
    private RewardSetupGUI rewardGUI;
    private final File codesFolder;
    private final Map<String, CodeData> codes = new HashMap<>();
    private final Set<UUID> activeSessions = ConcurrentHashMap.newKeySet();
    private final Map<UUID, StatusEntry> statusMap = new ConcurrentHashMap<>();

    public CodesService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.codesFolder = new File(plugin.getDataFolder(), "codes");
    }

    public void initialize() {
        if (!codesFolder.exists()) codesFolder.mkdirs();

        loadConfig();
        blacklistData = new BlacklistData(new File(codesFolder, "blacklist.yml"));
        loadCodes();
        plugin.getLogger().info("Loaded " + codes.size() + " code(s).");
        rewardGUI = new RewardSetupGUI(plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredStatuses, 20L, 20L);
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "CodesConfig.yml");
        if (!configFile.exists()) {
            plugin.saveResource("CodesConfig.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        config = new CodesConfig();
        config.load(yaml);
    }

    public void loadCodes() {
        codes.clear();
        File[] files = codesFolder.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("blacklist.yml"));
        if (files == null) return;

        for (File file : files) {
            String codeName = file.getName().replace(".yml", "");
            codes.put(codeName.toLowerCase(), new CodeData(codeName, file));
        }
    }

    // ---- Code CRUD ----

    public CodeData createCode(String name, long expiresAt, int maxUsesPerPlayer, long cooldownMillis) {
        File file = new File(codesFolder, name + ".yml");
        CodeData codeData = new CodeData(name, file);
        codeData.setExpiresAt(expiresAt);
        codeData.setMaxUsesPerPlayer(maxUsesPerPlayer);
        codeData.setCooldownMillis(cooldownMillis);
        codeData.save();
        codes.put(name.toLowerCase(), codeData);
        return codeData;
    }

    public boolean deleteCode(String name) {
        CodeData codeData = codes.remove(name.toLowerCase());
        if (codeData == null) return false;
        return new File(codesFolder, name + ".yml").delete();
    }

    public CodeData getCode(String name) {
        return codes.get(name.toLowerCase());
    }

    public Set<String> getCodeNames() {
        return codes.keySet();
    }

    // ---- Session management ----

    public boolean isInSession(UUID playerId) { return activeSessions.contains(playerId); }
    public void startSession(UUID playerId) { activeSessions.add(playerId); }
    public void endSession(UUID playerId) { activeSessions.remove(playerId); }

    // ---- Redemption ----

    public CodeRedemptionResult attemptRedemption(Player player, String codeName) {
        UUID playerId = player.getUniqueId();

        // Blacklist check
        if (blacklistData.isBlacklisted(playerId)) {
            long timeRemaining = blacklistData.getBlacklistTimeRemaining(playerId);
            String formattedTime = CodesConfig.formatTimeRemaining(timeRemaining);
            setStatus(playerId, CodeStatus.BLACKLISTED, formattedTime);
            playSound(player, config.getBlacklistedSound());
            return new CodeRedemptionResult(false,
                    "You are currently blacklisted from redeeming codes. You may try again in " + formattedTime);
        }

        CodeData codeData = getCode(codeName);

        // Invalid code
        if (codeData == null) {
            blacklistData.recordIncorrectAttempt(playerId, config.getMaxIncorrectAttempts(), config.getBlacklistDurationMillis());
            int attempts = blacklistData.getIncorrectAttempts(playerId);
            int remaining = config.getMaxIncorrectAttempts() - attempts;
            setStatus(playerId, CodeStatus.INVALID, null);
            playSound(player, config.getInvalidSound());
            return remaining <= 0
                    ? new CodeRedemptionResult(false, "Invalid code! You have been blacklisted for too many incorrect attempts.")
                    : new CodeRedemptionResult(false, "Invalid code! " + remaining + " attempt(s) remaining before blacklist.");
        }

        // Valid code found -- reset incorrect attempts
        blacklistData.resetAttempts(playerId);

        // Expired
        if (codeData.isExpired()) {
            setStatus(playerId, CodeStatus.EXPIRED, null);
            playSound(player, config.getExpiredSound());
            return new CodeRedemptionResult(false, "This code has expired!");
        }

        // Permission requirement
        if (codeData.getPermissionRequirement() != null && !player.hasPermission(codeData.getPermissionRequirement())) {
            setStatus(playerId, CodeStatus.INVALID, null);
            playSound(player, config.getInvalidSound());
            return new CodeRedemptionResult(false, "You don't have permission to redeem this code!");
        }

        // Redemption limits / cooldowns
        CodeData.RedemptionData redemptionData = codeData.getRedemptionData(playerId);
        if (redemptionData != null) {
            int maxUses = codeData.getMaxUsesPerPlayer();
            if (maxUses != -1 && redemptionData.uses >= maxUses) {
                setStatus(playerId, CodeStatus.ALREADY_REDEEMED, null);
                playSound(player, config.getAlreadyRedeemedSound());
                return new CodeRedemptionResult(false, "You have already redeemed this code!");
            }
            if (maxUses == -1 && codeData.getCooldownMillis() > 0L) {
                long timeSinceLastRedeem = System.currentTimeMillis() - redemptionData.lastRedeemed;
                if (timeSinceLastRedeem < codeData.getCooldownMillis()) {
                    long remaining = codeData.getCooldownMillis() - timeSinceLastRedeem;
                    String formattedTime = CodesConfig.formatTimeRemaining(remaining);
                    setStatus(playerId, CodeStatus.COOLDOWN, formattedTime);
                    playSound(player, config.getCooldownSound());
                    return new CodeRedemptionResult(false,
                            "You have already redeemed this code! Cooldown will expire in " + formattedTime);
                }
            }
        }

        // Redeem
        redeemCode(player, codeData);
        setStatus(playerId, CodeStatus.SUCCESS, null);
        playSound(player, config.getSuccessSound());
        return new CodeRedemptionResult(true, "Successfully redeemed code!");
    }

    private void redeemCode(Player player, CodeData codeData) {
        codeData.recordRedemption(player.getUniqueId());

        // Item rewards
        for (ItemStack item : codeData.getItemRewards()) {
            player.getInventory().addItem(item.clone());
        }

        // Vault money
        if (codeData.getVaultMoney() > 0.0 && plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            try {
                Economy economy = plugin.getServer().getServicesManager().getRegistration(Economy.class).getProvider();
                economy.depositPlayer(player, codeData.getVaultMoney());
                player.sendMessage(Component.text("+$" + codeData.getVaultMoney(), NamedTextColor.GREEN));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give Vault money: " + e.getMessage());
            }
        }

        // Command rewards
        for (CodeData.CodeCommand cmd : codeData.getCommands()) {
            String command = cmd.command().replace("%player%", player.getName());
            if (plugin.isEnabled()) {
                if (cmd.asPlayer()) {
                    player.performCommand(command);
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }

        plugin.getLogger().info(player.getName() + " redeemed code: " + codeData.getName());
    }

    // ---- Status tracking ----

    private void setStatus(UUID playerId, CodeStatus status, String extraData) {
        statusMap.put(playerId, new StatusEntry(status, extraData, System.currentTimeMillis() + 5_000L));
    }

    public String getStatusText(UUID playerId) {
        StatusEntry entry = statusMap.get(playerId);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) return "";
        return stripColorCodes(entry.status.getText(entry.extraData));
    }

    private void cleanupExpiredStatuses() {
        long now = System.currentTimeMillis();
        statusMap.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }

    // ---- Sound ----

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace("MINECRAFT:", "").replace(".", "_"));
            player.playSound(player.getLocation(), sound, 10000.0F, 1.0F);
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), soundName, 10000.0F, 1.0F);
        }
    }

    // ---- Helpers ----

    private String stripColorCodes(String text) {
        return text == null ? "" : text.replaceAll("\u00A7[0-9a-fk-or]", "");
    }

    // ---- Blacklist delegation ----

    public boolean isBlacklisted(UUID playerId) { return blacklistData.isBlacklisted(playerId); }
    public void clearBlacklist(UUID playerId) { blacklistData.resetAttempts(playerId); }

    // ---- Reward GUI ----

    public void openRewardGUI(Player player, String codeName) {
        CodeData codeData = codes.get(codeName.toLowerCase());
        if (codeData != null && rewardGUI != null) {
            rewardGUI.openMainGUI(player, codeData);
        }
    }

    // ---- Accessors ----

    public CodesConfig getConfig() { return config; }
    public BlacklistData getBlacklistData() { return blacklistData; }
    public ChaosCraftPlugin getPlugin() { return plugin; }

    // ---- Inner types ----

    public enum CodeStatus {
        SUCCESS("Successfully Redeemed Code!"),
        INVALID("Invalid Code."),
        EXPIRED("Code has Expired!"),
        COOLDOWN("You have already redeemed this code! Cooldown will expire in %s"),
        ALREADY_REDEEMED("You have already redeemed this code!"),
        BLACKLISTED("You are currently blacklisted from redeeming codes, you may try again in %s");

        private final String text;

        CodeStatus(String text) { this.text = text; }

        public String getText(String extraData) {
            return extraData != null ? text.replace("%s", extraData) : text;
        }
    }

    public record CodeRedemptionResult(boolean success, String message) {}

    private record StatusEntry(CodeStatus status, String extraData, long expiresAt) {}
}
