package com.blockforge.chaoscraft.services.play;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the "play" experience: executing scripts when a player clicks play,
 * and providing random status messages for the %cc_play_status% placeholder.
 */
public class PlayService {

    private final ChaosCraftPlugin plugin;
    private final Set<UUID> playingPlayers = new HashSet<>();
    private final Map<UUID, PlayStatusMessage> statusMessages = new HashMap<>();
    private List<String> messages = new ArrayList<>();
    private List<String> notAcceptedScript = new ArrayList<>();
    private List<String> acceptedScript = new ArrayList<>();
    private final Random random = new Random();
    private File configFile;

    public PlayService(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        plugin.getLogger().info("Play Service initialized with " + messages.size() + " messages, "
                + notAcceptedScript.size() + " not-accepted script lines, "
                + acceptedScript.size() + " accepted script lines.");
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "Play.yml");
        if (!configFile.exists()) {
            plugin.saveResource("Play.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        notAcceptedScript = yaml.getStringList("Scripts.UserAgreementNotAccepted");
        acceptedScript = yaml.getStringList("Scripts.PlayAccepted");
        messages = yaml.getStringList("Messages");

        if (messages.isEmpty()) {
            plugin.getLogger().warning("No messages found in Play.yml! Using default.");
            messages.add("Welcome to the server, %player%!");
        }
        if (notAcceptedScript.isEmpty()) {
            plugin.getLogger().warning("No UserAgreementNotAccepted script found in Play.yml!");
            notAcceptedScript.add("message %player% &cYou must accept the user agreement!");
        }
        if (acceptedScript.isEmpty()) {
            plugin.getLogger().warning("No PlayAccepted script found in Play.yml!");
            acceptedScript.add("message %player% &aWelcome to the server!");
        }
    }

    // ---- Script execution ----

    public void executePlayScript(Player player, boolean hasAccepted) {
        List<String> script = hasAccepted ? acceptedScript : notAcceptedScript;
        plugin.getLogger().info("Executing play script for " + player.getName()
                + " (accepted=" + hasAccepted + ", " + script.size() + " lines)");
        executeScriptAsync(player, script, 0);

        if (hasAccepted) {
            String selectedMessage = getRandomMessage();
            statusMessages.put(player.getUniqueId(), new PlayStatusMessage(selectedMessage, System.currentTimeMillis()));
            plugin.getLogger().info("Selected message for " + player.getName() + ": " + selectedMessage);
        }
    }

    private void executeScriptAsync(Player player, List<String> script, int lineIndex) {
        if (lineIndex >= script.size()) return;

        String line = script.get(lineIndex).trim();
        if (line.isEmpty() || line.startsWith("#")) {
            executeScriptAsync(player, script, lineIndex + 1);
            return;
        }

        line = line.replace("%player%", player.getName());

        // Wait handling
        if (line.startsWith("wait ") && line.endsWith(" ticks")) {
            String ticksStr = line.substring("wait ".length(), line.length() - " ticks".length()).trim();
            try {
                int ticks = Integer.parseInt(ticksStr);
                plugin.getLogger().info("Waiting " + ticks + " ticks before continuing script");
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        executeScriptAsync(player, script, lineIndex + 1), ticks);
                return;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid tick count in wait command: " + ticksStr);
            }
        }

        executeScriptLineSync(player, line);
        executeScriptAsync(player, script, lineIndex + 1);
    }

    private void executeScriptLineSync(Player player, String line) {
        if (line.startsWith("execute console command ")) {
            String command = stripQuotes(line.substring("execute console command ".length()).trim());
            if (plugin.isEnabled()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (line.startsWith("execute player command ")) {
            String command = stripQuotes(line.substring("execute player command ".length()).trim());
            if (plugin.isEnabled()) player.performCommand(command);
        } else if (line.startsWith("message ")) {
            String msg = line.substring("message ".length()).trim();
            if (msg.startsWith("%player% ")) {
                msg = msg.substring("%player% ".length());
            }
            msg = processColorCodes(msg);
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg));
        }
    }

    // ---- State ----

    public boolean isPlaying(UUID playerId) { return playingPlayers.contains(playerId); }
    public void setPlaying(UUID playerId) { playingPlayers.add(playerId); }

    public void removePlaying(UUID playerId) {
        playingPlayers.remove(playerId);
        statusMessages.remove(playerId);
    }

    public void clearPlayMessage(UUID playerId) {
        statusMessages.remove(playerId);
        plugin.getLogger().info("Cleared play message for player: " + playerId);
    }

    public String getPlayStatusMessage(UUID playerId, String playerName) {
        PlayStatusMessage status = statusMessages.get(playerId);
        if (status == null) return "";
        long elapsed = System.currentTimeMillis() - status.timestamp;
        return elapsed > 10_000L ? "" : status.message.replace("%player%", playerName);
    }

    private String getRandomMessage() {
        return messages.isEmpty() ? "Welcome to the server, %player%!" : messages.get(random.nextInt(messages.size()));
    }

    public void reload() { loadConfig(); }
    public ChaosCraftPlugin getPlugin() { return plugin; }

    // ---- Helpers ----

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }

    private String processColorCodes(String text) {
        if (text == null) return "";
        text = processHexColors(text);
        text = text.replace('&', '\u00A7');
        return text;
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("\u00A7x");
            for (char c : hex.toCharArray()) {
                replacement.append('\u00A7').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    // ---- Inner type ----

    private record PlayStatusMessage(String message, long timestamp) {}
}
