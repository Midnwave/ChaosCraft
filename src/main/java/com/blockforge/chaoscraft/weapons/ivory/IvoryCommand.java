package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.weapons.ivory.abilities.AbilityManager;
import com.blockforge.chaoscraft.weapons.ivory.abilities.AbilityRegistry;
import com.blockforge.chaoscraft.weapons.ivory.abilities.base.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IvoryCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private final IvoryService ivoryService;

    public IvoryCommand(ChaosCraftPlugin plugin, IvoryService ivoryService) {
        this.plugin = plugin;
        this.ivoryService = ivoryService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.ivory.test")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) { showHelp(sender, label); return true; }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "state" -> handleState(sender, args);
            case "ability", "test" -> handleAbilityTest(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> handleAbilityTest(sender, new String[]{"ability", args[0], args.length > 1 ? args[1] : null});
        }
        return true;
    }

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== Celestial Ivory Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " give [player] [state]", NamedTextColor.YELLOW)
                .append(Component.text(" - Give ivory item", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " state <off|charging|active|rage> [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Set held ivory state", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " ability <name> [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Test an ability", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all abilities", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload ivory config", NamedTextColor.GRAY)));
    }

    private void handleGive(CommandSender sender, String[] args) {
        var state = IvoryService.IvoryState.OFF;
        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                try {
                    state = IvoryService.IvoryState.valueOf(args[1].toUpperCase());
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage(Component.text("Specify a player when using from console.", NamedTextColor.RED));
                        return;
                    }
                    target = p;
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                    return;
                }
            }
            if (args.length >= 3) {
                try { state = IvoryService.IvoryState.valueOf(args[2].toUpperCase()); }
                catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid state. Use: off, charging, active, rage", NamedTextColor.RED));
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Specify a player when using from console.", NamedTextColor.RED));
                return;
            }
            target = p;
        }

        var stateManager = ivoryService.getStateManager();
        if (stateManager == null) {
            sender.sendMessage(Component.text("Ivory service not initialized.", NamedTextColor.RED));
            return;
        }
        ItemStack ivory = stateManager.createIvoryItem(state);
        target.getInventory().addItem(ivory);
        sender.sendMessage(Component.text("Gave Celestial Ivory (" + state.name() + ") to " + target.getName(), NamedTextColor.GREEN));
    }

    private void handleState(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ivorytest state <off|charging|active|rage> [player]", NamedTextColor.RED));
            return;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Specify a player when using from console.", NamedTextColor.RED));
                return;
            }
            target = p;
        }

        IvoryService.IvoryState state;
        try { state = IvoryService.IvoryState.valueOf(args[1].toUpperCase()); }
        catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid state. Use: off, charging, active, rage", NamedTextColor.RED));
            return;
        }

        ItemStack held = target.getInventory().getItemInMainHand();
        if (!ivoryService.isIvoryWeapon(held)) {
            sender.sendMessage(Component.text(target.getName() + " is not holding a Celestial Ivory weapon.", NamedTextColor.RED));
            return;
        }

        var stateManager = ivoryService.getStateManager();
        switch (state) {
            case OFF -> stateManager.transitionToOff(held);
            case CHARGING -> stateManager.transitionToCharging(held);
            case ACTIVE -> stateManager.transitionToActive(held);
            case RAGE -> stateManager.transitionToRage(held);
        }
        sender.sendMessage(Component.text("Set " + target.getName() + "'s ivory to " + state.name(), NamedTextColor.GREEN));
    }

    private void handleAbilityTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ivorytest ability <name> [player]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /ivorytest list to see available abilities.", NamedTextColor.GRAY));
            return;
        }
        String abilityName = args[1].toLowerCase();
        Player target;
        if (args.length >= 3 && args[2] != null) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(Component.text("Specify a player when using from console.", NamedTextColor.RED));
                return;
            }
            target = p;
        }

        AbilityManager abilityManager = ivoryService.getAbilityManager();
        if (abilityManager == null) {
            sender.sendMessage(Component.text("Ability manager not initialized.", NamedTextColor.RED));
            return;
        }
        AbilityRegistry registry = abilityManager.getRegistry();
        Ability ability = registry.getAbility(abilityName);
        if (ability == null) {
            sender.sendMessage(Component.text("Unknown ability: " + abilityName, NamedTextColor.RED));
            sender.sendMessage(Component.text("Use /ivorytest list to see available abilities.", NamedTextColor.GRAY));
            return;
        }

        var settings = ivoryService.getConfig().getAbilitySettings(abilityName);
        if (!settings.enabled) {
            sender.sendMessage(Component.text("Warning: This ability is disabled in config, but executing anyway for testing.", NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Executing ability: ", NamedTextColor.GREEN)
                .append(Component.text(ability.getDisplayName(), NamedTextColor.GOLD))
                .append(Component.text(" for " + target.getName(), NamedTextColor.GREEN)));
        ability.execute(target);
    }

    private void handleList(CommandSender sender) {
        AbilityManager abilityManager = ivoryService.getAbilityManager();
        if (abilityManager == null) {
            sender.sendMessage(Component.text("Ability manager not initialized.", NamedTextColor.RED));
            return;
        }
        AbilityRegistry registry = abilityManager.getRegistry();
        var abilityIds = new ArrayList<>(registry.getAllAbilityIds());
        sender.sendMessage(Component.text("=== Celestial Ivory Abilities (" + abilityIds.size() + ") ===", NamedTextColor.GOLD));
        IvoryConfig config = ivoryService.getConfig();
        for (String id : abilityIds) {
            Ability ability = registry.getAbility(id);
            if (ability == null) continue;
            var settings = config.getAbilitySettings(id);
            String status = settings.enabled ? "\u2713" : "\u2717";
            var statusColor = settings.enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
            sender.sendMessage(Component.text(status, statusColor)
                    .append(Component.text(" " + id, NamedTextColor.YELLOW))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(ability.getDisplayName(), NamedTextColor.WHITE))
                    .append(Component.text(" (CD: " + settings.cooldownSeconds + "s)", NamedTextColor.GRAY)));
        }
    }

    private void handleReload(CommandSender sender) {
        ivoryService.reload();
        sender.sendMessage(Component.text("Celestial Ivory configuration reloaded.", NamedTextColor.GREEN));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("chaoscraft.ivory.test")) return List.of();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("give", "state", "ability", "test", "list", "reload"));
            var abilityManager = ivoryService.getAbilityManager();
            if (abilityManager != null) subs.addAll(abilityManager.getRegistry().getAllAbilityIds());
            return subs.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "give" -> {
                    var options = new ArrayList<String>();
                    Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    options.addAll(Arrays.asList("off", "charging", "active", "rage"));
                    yield options.stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                case "state" -> Arrays.asList("off", "charging", "active", "rage").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "ability", "test" -> {
                    var am = ivoryService.getAbilityManager();
                    yield am != null ? am.getRegistry().getAllAbilityIds().stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList()) : List.<String>of();
                }
                default -> Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            };
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("state") || sub.equals("ability") || sub.equals("test")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
