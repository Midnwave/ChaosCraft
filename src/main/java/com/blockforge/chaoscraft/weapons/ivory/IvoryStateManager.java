package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.itemtags.ItemTagsAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IvoryStateManager implements Listener {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;
    private final Map<UUID, ChargingSession> chargingSessions = new ConcurrentHashMap<>();
    private final Map<UUID, RageSession> rageSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> attackCooldowns = new ConcurrentHashMap<>();

    public IvoryStateManager(ChaosCraftPlugin plugin, IvoryConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void updateConfig(IvoryConfig config) {
        this.config = config;
    }

    public void cleanup() {
        chargingSessions.values().forEach(session -> {
            if (session.task != null && !session.task.isCancelled()) session.task.cancel();
        });
        chargingSessions.clear();
        rageSessions.values().forEach(session -> {
            if (session.endTask != null && !session.endTask.isCancelled()) session.endTask.cancel();
        });
        rageSessions.clear();
        attackCooldowns.clear();
    }

    public void transitionToOff(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemTagsAPI.removeTag(item, config.getTagCharging());
        ItemTagsAPI.removeTag(item, config.getTagActive());
        ItemTagsAPI.removeTag(item, config.getTagRage());
        ItemTagsAPI.addTag(item, config.getTagOff());
        updateItemMeta(item, IvoryService.IvoryState.OFF);
    }

    public void transitionToCharging(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemTagsAPI.removeTag(item, config.getTagOff());
        ItemTagsAPI.removeTag(item, config.getTagActive());
        ItemTagsAPI.removeTag(item, config.getTagRage());
        ItemTagsAPI.addTag(item, config.getTagCharging());
        updateItemMeta(item, IvoryService.IvoryState.CHARGING);
    }

    public void transitionToActive(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemTagsAPI.removeTag(item, config.getTagOff());
        ItemTagsAPI.removeTag(item, config.getTagCharging());
        ItemTagsAPI.removeTag(item, config.getTagRage());
        ItemTagsAPI.addTag(item, config.getTagActive());
        updateItemMeta(item, IvoryService.IvoryState.ACTIVE);
    }

    public void transitionToRage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemTagsAPI.removeTag(item, config.getTagOff());
        ItemTagsAPI.removeTag(item, config.getTagCharging());
        ItemTagsAPI.removeTag(item, config.getTagActive());
        ItemTagsAPI.addTag(item, config.getTagRage());
        updateItemMeta(item, IvoryService.IvoryState.RAGE);
    }

    private void updateItemMeta(ItemStack item, IvoryService.IvoryState state) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(switch (state) {
            case OFF -> config.getNameOff();
            case CHARGING -> config.getNameCharging();
            case ACTIVE -> config.getNameActive();
            case RAGE -> config.getNameRage();
        });
        meta.setCustomModelData(config.getModelDataForTag(getTagForState(state)));

        double attackSpeed = switch (state) {
            case OFF, CHARGING -> 100.0;
            case ACTIVE -> calculateAttackSpeed(config.getAttackCooldownActive());
            case RAGE -> calculateAttackSpeed(config.getAttackCooldownRage());
        };

        if (meta.hasAttributeModifiers()) {
            meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        }
        double modifier = attackSpeed - 4.0;
        var attackSpeedMod = new AttributeModifier(
                new NamespacedKey(plugin, "ivory_attack_speed"),
                modifier, Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND);
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, attackSpeedMod);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
    }

    private double calculateAttackSpeed(double cooldownSeconds) {
        if (cooldownSeconds <= 0.0) return 16.0;
        if (cooldownSeconds >= 10.0) return 0.1;
        return 1.0 / cooldownSeconds;
    }

    private String getTagForState(IvoryService.IvoryState state) {
        return switch (state) {
            case OFF -> config.getTagOff();
            case CHARGING -> config.getTagCharging();
            case ACTIVE -> config.getTagActive();
            case RAGE -> config.getTagRage();
        };
    }

    public IvoryService.IvoryState getState(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (ItemTagsAPI.hasTag(item, config.getTagRage())) return IvoryService.IvoryState.RAGE;
        if (ItemTagsAPI.hasTag(item, config.getTagActive())) return IvoryService.IvoryState.ACTIVE;
        if (ItemTagsAPI.hasTag(item, config.getTagCharging())) return IvoryService.IvoryState.CHARGING;
        if (ItemTagsAPI.hasTag(item, config.getTagOff())) return IvoryService.IvoryState.OFF;
        return null;
    }

    public void startCharging(Player player, ItemStack item) {
        cancelCharging(player);
        transitionToCharging(item);
        var session = new ChargingSession(player, item, config.getChargeScript());
        chargingSessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void cancelCharging(Player player) {
        var session = chargingSessions.remove(player.getUniqueId());
        if (session != null) session.cancel();
    }

    public boolean isCharging(Player player) {
        return chargingSessions.containsKey(player.getUniqueId());
    }

    private void completeCharging(Player player, ItemStack item) {
        chargingSessions.remove(player.getUniqueId());
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && ItemTagsAPI.hasTag(mainHand, config.getTagCharging())) {
            transitionToActive(mainHand);
        }
    }

    public void activateRage(Player player, ItemStack item) {
        cancelRage(player);
        transitionToRage(item);
        executeScript(config.getOnRageActivateScript(), player);
        var session = new RageSession(player, item);
        rageSessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void cancelRage(Player player) {
        var session = rageSessions.remove(player.getUniqueId());
        if (session != null) session.cancel();
    }

    public boolean isInRage(Player player) {
        return rageSessions.containsKey(player.getUniqueId());
    }

    private void endRage(Player player) {
        rageSessions.remove(player.getUniqueId());
        executeScript(config.getOnRageDeactivateScript(), player);
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && ItemTagsAPI.hasTag(mainHand, config.getTagRage())) {
            transitionToActive(mainHand);
        }
    }

    public boolean isOnAttackCooldown(Player player) {
        Long lastAttack = attackCooldowns.get(player.getUniqueId());
        if (lastAttack == null) return false;
        long now = System.currentTimeMillis();
        var state = getState(player.getInventory().getItemInMainHand());
        double cooldownSeconds = switch (state) {
            case ACTIVE -> config.getAttackCooldownActive();
            case RAGE -> config.getAttackCooldownRage();
            default -> 0.0;
        };
        return now - lastAttack < (long) (cooldownSeconds * 1000.0);
    }

    public void setAttackCooldown(Player player) {
        attackCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        var state = getState(item);
        double cooldownSeconds = switch (state) {
            case ACTIVE -> config.getAttackCooldownActive();
            case RAGE -> config.getAttackCooldownRage();
            default -> 0.0;
        };
        if (cooldownSeconds > 0.0) {
            player.setCooldown(item.getType(), (int) (cooldownSeconds * 20.0));
        }
    }

    public long getRemainingCooldownMs(Player player) {
        Long lastAttack = attackCooldowns.get(player.getUniqueId());
        if (lastAttack == null) return 0L;
        var state = getState(player.getInventory().getItemInMainHand());
        double cooldownSeconds = switch (state) {
            case ACTIVE -> config.getAttackCooldownActive();
            case RAGE -> config.getAttackCooldownRage();
            default -> 0.0;
        };
        long cooldownMs = (long) (cooldownSeconds * 1000.0);
        long elapsed = System.currentTimeMillis() - lastAttack;
        return Math.max(0L, cooldownMs - elapsed);
    }

    public ItemStack createIvoryItem(IvoryService.IvoryState state) {
        ItemStack item = new ItemStack(config.getBaseMaterial());
        switch (state) {
            case OFF -> { ItemTagsAPI.addTag(item, config.getTagOff()); updateItemMeta(item, IvoryService.IvoryState.OFF); }
            case CHARGING -> { ItemTagsAPI.addTag(item, config.getTagCharging()); updateItemMeta(item, IvoryService.IvoryState.CHARGING); }
            case ACTIVE -> { ItemTagsAPI.addTag(item, config.getTagActive()); updateItemMeta(item, IvoryService.IvoryState.ACTIVE); }
            case RAGE -> { ItemTagsAPI.addTag(item, config.getTagRage()); updateItemMeta(item, IvoryService.IvoryState.RAGE); }
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (!config.isEnabled() || !config.isRightClickToCharge()) return;
        var action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (ItemTagsAPI.hasTag(item, config.getTagOff())) {
            event.setCancelled(true);
            startCharging(player, item);
        }
    }

    private String getMinecraftDimension(World world) {
        if (world == null) return "minecraft:overworld";
        return switch (world.getEnvironment()) {
            case NETHER -> "minecraft:the_nether";
            case THE_END -> "minecraft:the_end";
            default -> "minecraft:overworld";
        };
    }

    private void executeScript(List<String> script, Player player) {
        if (script == null || script.isEmpty()) return;
        Location loc = player.getLocation();
        String worldDimension = getMinecraftDimension(loc.getWorld());
        for (String line : script) {
            line = line.trim();
            if (line.startsWith("execute console command ")) {
                String command = line.substring("execute console command ".length()).trim();
                if (command.startsWith("\"") && command.endsWith("\"")) {
                    command = command.substring(1, command.length() - 1);
                }
                command = command.replace("%player%", player.getName())
                        .replace("%world%", worldDimension)
                        .replace("%x%", String.valueOf(loc.getBlockX()))
                        .replace("%y%", String.valueOf(loc.getBlockY()))
                        .replace("%z%", String.valueOf(loc.getBlockZ()))
                        .replace("~ ~ ~", loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    // --- Inner classes ---

    private class ChargingSession {
        final Player player;
        final ItemStack item;
        final List<String> script;
        BukkitTask task;
        int scriptIndex = 0;

        ChargingSession(Player player, ItemStack item, List<String> script) {
            this.player = player;
            this.item = item;
            this.script = new ArrayList<>(script);
        }

        void start() { processNextLine(); }

        void processNextLine() {
            if (scriptIndex >= script.size()) {
                completeCharging(player, item);
                return;
            }
            if (!player.isOnline()) { cancel(); return; }
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || !ItemTagsAPI.hasTag(mainHand, config.getTagCharging())) {
                cancel(); return;
            }

            String line = script.get(scriptIndex).trim();
            scriptIndex++;

            if (line.startsWith("execute console command ")) {
                String command = line.substring("execute console command ".length()).trim();
                if (command.startsWith("\"") && command.endsWith("\"")) {
                    command = command.substring(1, command.length() - 1);
                }
                Location loc = player.getLocation();
                String worldDimension = getMinecraftDimension(loc.getWorld());
                command = command.replace("%player%", player.getName())
                        .replace("%world%", worldDimension)
                        .replace("%x%", String.valueOf(loc.getBlockX()))
                        .replace("%y%", String.valueOf(loc.getBlockY()))
                        .replace("%z%", String.valueOf(loc.getBlockZ()))
                        .replace("~ ~ ~", loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                processNextLine();
            } else if (line.startsWith("wait ")) {
                try {
                    int ticks = Integer.parseInt(line.substring("wait ".length()).trim());
                    task = Bukkit.getScheduler().runTaskLater(plugin, this::processNextLine, ticks);
                } catch (NumberFormatException e) { processNextLine(); }
            } else if (line.equals("finish")) {
                completeCharging(player, item);
            } else {
                processNextLine();
            }
        }

        void cancel() {
            if (task != null && !task.isCancelled()) task.cancel();
            chargingSessions.remove(player.getUniqueId());
        }
    }

    private class RageSession {
        final Player player;
        final ItemStack item;
        BukkitTask endTask;
        private static final String[] RAINBOW_COLORS = {
                "\u00A7c", "\u00A76", "\u00A7e", "\u00A7a", "\u00A7b", "\u00A79", "\u00A7d"
        };

        RageSession(Player player, ItemStack item) {
            this.player = player;
            this.item = item;
        }

        void start() {
            int durationTicks = config.getRageDurationSeconds() * 20;
            setRainbowGradientName(item);
            endTask = Bukkit.getScheduler().runTaskLater(plugin,
                    () -> IvoryStateManager.this.endRage(player), durationTicks);
        }

        private void setRainbowGradientName(ItemStack item) {
            String text = "Celestial Ivory";
            var coloredName = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == ' ') {
                    coloredName.append(' ');
                } else {
                    int colorIndex = i % RAINBOW_COLORS.length;
                    coloredName.append(RAINBOW_COLORS[colorIndex]).append("\u00A7l").append(c);
                }
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(coloredName.toString()));
                item.setItemMeta(meta);
            }
        }

        void cancel() {
            if (endTask != null && !endTask.isCancelled()) endTask.cancel();
            rageSessions.remove(player.getUniqueId());
        }
    }
}
