package com.blockforge.chaoscraft.commands;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * /calamity command for Calamity mode-specific operations.
 * Subcommands: testblockdisplay, testenvironmentalattack, testbossattack
 *
 * Test commands force-spawn an attack on the player, bypassing exempt
 * checks and cooldowns. Full tab completion from the AttackRegistry.
 */
public class CalamityCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;
    private static final List<String> ROOT_SUBS = List.of(
            "testblockdisplay", "testenvironmentalattack", "testbossattack"
    );
    private static final List<String> PHASES = List.of("1", "2", "3", "4", "5");
    private static final List<String> BOSSES = List.of(
            "voidmaw", "dog", "dweller", "dragon", "calamitas"
    );

    public CalamityCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /calamity <testblockdisplay|testenvironmentalattack|testbossattack>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "testblockdisplay" -> handleTestBlockDisplay(player, args);
            case "testenvironmentalattack" -> handleTestEnvironmental(player, args);
            case "testbossattack" -> handleTestBossAttack(player, args);
            default -> sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
        }
        return true;
    }

    private void handleTestBlockDisplay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testblockdisplay <phase> <id>", NamedTextColor.RED));
            return;
        }
        int phase = parsePhase(args[1]);
        if (phase < 1 || phase > 5) {
            player.sendMessage(Component.text("Phase must be 1-5.", NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        AbstractAttack template = registry.get(phase, AttackType.BLOCK_DISPLAY, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Block display attack not found: phase " + phase + ", id: " + attackId, NamedTextColor.RED));
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned block display: " + attackId + " (phase " + phase + ")", NamedTextColor.LIGHT_PURPLE));
    }

    private void handleTestEnvironmental(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testenvironmentalattack <phase> <id>", NamedTextColor.RED));
            return;
        }
        int phase = parsePhase(args[1]);
        if (phase < 1 || phase > 5) {
            player.sendMessage(Component.text("Phase must be 1-5.", NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        AbstractAttack template = registry.get(phase, AttackType.ENVIRONMENTAL, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Environmental attack not found: phase " + phase + ", id: " + attackId, NamedTextColor.RED));
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned environmental: " + attackId + " (phase " + phase + ")", NamedTextColor.LIGHT_PURPLE));
    }

    private void handleTestBossAttack(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /calamity testbossattack <boss> <id>", NamedTextColor.RED));
            return;
        }
        String boss = args[1].toLowerCase();
        if (!BOSSES.contains(boss)) {
            player.sendMessage(Component.text("Unknown boss: " + boss + ". Options: " + String.join(", ", BOSSES), NamedTextColor.RED));
            return;
        }
        String attackId = args[2].toLowerCase();

        AttackRegistry registry = getRegistry();
        if (registry == null) {
            player.sendMessage(Component.text("Calamity mode not registered.", NamedTextColor.RED));
            return;
        }

        // Boss attacks are stored with their boss phase: voidmaw=1, dog=2, dweller=3, dragon=4, calamitas=5
        int bossPhase = bossToPhase(boss);
        AbstractAttack template = registry.get(bossPhase, AttackType.BOSS, attackId);
        if (template == null) {
            player.sendMessage(Component.text("Boss attack not found: " + boss + ", id: " + attackId, NamedTextColor.RED));
            return;
        }

        getScheduler().forceSpawn(template, player);
        player.sendMessage(Component.text("Spawned boss attack: " + attackId + " (" + boss + ")", NamedTextColor.LIGHT_PURPLE));
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(ROOT_SUBS, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("testbossattack")) {
                return filterStartsWith(BOSSES, args[1]);
            }
            return filterStartsWith(PHASES, args[1]);
        }
        if (args.length == 3) {
            AttackRegistry registry = getRegistry();
            if (registry == null) return List.of();

            switch (args[0].toLowerCase()) {
                case "testblockdisplay" -> {
                    int phase = parsePhase(args[1]);
                    if (phase >= 1 && phase <= 5) {
                        return filterStartsWith(registry.getIds(phase, AttackType.BLOCK_DISPLAY), args[2]);
                    }
                }
                case "testenvironmentalattack" -> {
                    int phase = parsePhase(args[1]);
                    if (phase >= 1 && phase <= 5) {
                        return filterStartsWith(registry.getIds(phase, AttackType.ENVIRONMENTAL), args[2]);
                    }
                }
                case "testbossattack" -> {
                    String boss = args[1].toLowerCase();
                    return filterStartsWith(registry.getBossAttackIds(boss), args[2]);
                }
            }
        }
        return List.of();
    }

    // ========================
    // Helpers
    // ========================

    private AttackRegistry getRegistry() {
        var mode = plugin.getModeManager().getMode("calamity");
        if (mode instanceof CalamityMode calamityMode) {
            return calamityMode.getAttackRegistry();
        }
        return null;
    }

    private com.blockforge.chaoscraft.modes.calamity.attacks.AttackScheduler getScheduler() {
        var mode = plugin.getModeManager().getMode("calamity");
        if (mode instanceof CalamityMode calamityMode) {
            return calamityMode.getAttackScheduler();
        }
        return null;
    }

    private int bossToPhase(String boss) {
        return switch (boss) {
            case "voidmaw" -> 1;
            case "dog" -> 2;
            case "dweller" -> 3;
            case "dragon" -> 4;
            case "calamitas" -> 5;
            default -> 1;
        };
    }

    private int parsePhase(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
