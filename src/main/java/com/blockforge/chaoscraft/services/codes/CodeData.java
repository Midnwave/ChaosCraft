package com.blockforge.chaoscraft.services.codes;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Represents a single redeemable code: its metadata, rewards, and per-player redemption history.
 * Each code is backed by its own YAML file in the codes/ folder.
 */
public class CodeData {

    private final String name;
    private final File file;
    private YamlConfiguration yaml;

    private long expiresAt;
    private int maxUsesPerPlayer;
    private long cooldownMillis;
    private String permissionRequirement;

    private List<ItemStack> itemRewards = new ArrayList<>();
    private double vaultMoney = 0.0;
    private List<CodeCommand> commands = new ArrayList<>();
    private final Map<UUID, RedemptionData> redemptions = new HashMap<>();

    public CodeData(String name, File file) {
        this.name = name;
        this.file = file;
        load();
    }

    // ---- Persistence ----

    public void load() {
        yaml = YamlConfiguration.loadConfiguration(file);
        expiresAt = yaml.getLong("ExpiresAt", -1L);
        maxUsesPerPlayer = yaml.getInt("MaxUsesPerPlayer", 1);
        cooldownMillis = yaml.getLong("CooldownMillis", 0L);
        permissionRequirement = yaml.getString("PermissionRequirement", null);

        // Item rewards
        List<?> items = yaml.getList("Rewards.Items");
        itemRewards.clear();
        if (items != null) {
            for (Object obj : items) {
                if (obj instanceof ItemStack item) {
                    itemRewards.add(item);
                }
            }
        }

        vaultMoney = yaml.getDouble("Rewards.VaultMoney", 0.0);

        // Command rewards
        commands.clear();
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> commandsList = (List<Map<?, ?>>) yaml.getList("Rewards.Commands");
        if (commandsList != null) {
            for (Map<?, ?> map : commandsList) {
                String cmd = (String) map.get("command");
                boolean asPlayer = false;
                if (map.containsKey("asPlayer")) {
                    Object val = map.get("asPlayer");
                    if (val instanceof Boolean b) asPlayer = b;
                }
                commands.add(new CodeCommand(cmd, asPlayer));
            }
        }

        // Redemption history
        redemptions.clear();
        if (yaml.contains("Redemptions")) {
            var section = yaml.getConfigurationSection("Redemptions");
            if (section != null) {
                for (String uuidStr : section.getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    int uses = yaml.getInt("Redemptions." + uuidStr + ".uses");
                    long lastRedeemed = yaml.getLong("Redemptions." + uuidStr + ".lastRedeemed");
                    redemptions.put(uuid, new RedemptionData(uses, lastRedeemed));
                }
            }
        }
    }

    public void save() {
        yaml.set("ExpiresAt", expiresAt);
        yaml.set("MaxUsesPerPlayer", maxUsesPerPlayer);
        yaml.set("CooldownMillis", cooldownMillis);
        yaml.set("PermissionRequirement", permissionRequirement);
        yaml.set("Rewards.Items", itemRewards);
        yaml.set("Rewards.VaultMoney", vaultMoney);

        List<Map<String, Object>> commandsList = new ArrayList<>();
        for (CodeCommand cmd : commands) {
            Map<String, Object> map = new HashMap<>();
            map.put("command", cmd.command());
            map.put("asPlayer", cmd.asPlayer());
            commandsList.add(map);
        }
        yaml.set("Rewards.Commands", commandsList);

        yaml.set("Redemptions", null);
        for (var entry : redemptions.entrySet()) {
            String uuid = entry.getKey().toString();
            yaml.set("Redemptions." + uuid + ".uses", entry.getValue().uses);
            yaml.set("Redemptions." + uuid + ".lastRedeemed", entry.getValue().lastRedeemed);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---- Redemption tracking ----

    public RedemptionData getRedemptionData(UUID playerId) {
        return redemptions.get(playerId);
    }

    public void recordRedemption(UUID playerId) {
        RedemptionData data = redemptions.getOrDefault(playerId, new RedemptionData(0, 0L));
        data.uses++;
        data.lastRedeemed = System.currentTimeMillis();
        redemptions.put(playerId, data);
        save();
    }

    // ---- Expiry ----

    public boolean isExpired() {
        return expiresAt != -1L && System.currentTimeMillis() >= expiresAt;
    }

    public void setExpired() {
        expiresAt = System.currentTimeMillis();
        save();
    }

    // ---- Accessors ----

    public String getName() { return name; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public int getMaxUsesPerPlayer() { return maxUsesPerPlayer; }
    public void setMaxUsesPerPlayer(int maxUsesPerPlayer) { this.maxUsesPerPlayer = maxUsesPerPlayer; }
    public long getCooldownMillis() { return cooldownMillis; }
    public void setCooldownMillis(long cooldownMillis) { this.cooldownMillis = cooldownMillis; }
    public String getPermissionRequirement() { return permissionRequirement; }
    public void setPermissionRequirement(String permissionRequirement) { this.permissionRequirement = permissionRequirement; }
    public List<ItemStack> getItemRewards() { return itemRewards; }
    public void setItemRewards(List<ItemStack> itemRewards) { this.itemRewards = itemRewards; }
    public double getVaultMoney() { return vaultMoney; }
    public void setVaultMoney(double vaultMoney) { this.vaultMoney = vaultMoney; }
    public List<CodeCommand> getCommands() { return commands; }
    public void setCommands(List<CodeCommand> commands) { this.commands = commands; }

    // ---- Inner types ----

    public record CodeCommand(String command, boolean asPlayer) {}

    public static class RedemptionData {
        public int uses;
        public long lastRedeemed;

        public RedemptionData(int uses, long lastRedeemed) {
            this.uses = uses;
            this.lastRedeemed = lastRedeemed;
        }
    }
}
