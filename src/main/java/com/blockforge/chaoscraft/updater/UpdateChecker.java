package com.blockforge.chaoscraft.updater;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Checks GitHub releases for updates and downloads the latest JAR.
 * The update is staged in the /plugins/update/ folder and applied on server restart.
 */
public class UpdateChecker {

    private final ChaosCraftPlugin plugin;
    private final String githubOwner;
    private final String githubRepo;

    private String latestVersion = null;
    private String latestDownloadUrl = null;
    private boolean updateAvailable = false;

    public UpdateChecker(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        // Configurable — defaults to BlockForge Studios repo
        this.githubOwner = plugin.getConfig().getString("updater.github-owner", "Midnwave");
        this.githubRepo = plugin.getConfig().getString("updater.github-repo", "ChaosCraft");
    }

    /**
     * Async check for the latest release on GitHub.
     */
    public void checkForUpdate(CommandSender notifyTarget) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String apiUrl = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/releases/latest";
                    HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int code = conn.getResponseCode();
                    if (code != 200) {
                        notifyAsync(notifyTarget, Component.text("Failed to check for updates (HTTP " + code + ")", NamedTextColor.RED));
                        return;
                    }

                    String body;
                    try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        var sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        body = sb.toString();
                    }

                    JsonObject release = JsonParser.parseString(body).getAsJsonObject();
                    String tagName = release.get("tag_name").getAsString();
                    latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;

                    // Find the .jar asset
                    JsonArray assets = release.getAsJsonArray("assets");
                    latestDownloadUrl = null;
                    for (JsonElement asset : assets) {
                        JsonObject a = asset.getAsJsonObject();
                        String name = a.get("name").getAsString();
                        if (name.endsWith(".jar")) {
                            latestDownloadUrl = a.get("browser_download_url").getAsString();
                            break;
                        }
                    }

                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = !latestVersion.equals(currentVersion) && latestDownloadUrl != null;

                    if (updateAvailable) {
                        notifyAsync(notifyTarget, Component.text("Update available! ", NamedTextColor.GREEN)
                                .append(Component.text("v" + currentVersion, NamedTextColor.GRAY))
                                .append(Component.text(" → ", NamedTextColor.YELLOW))
                                .append(Component.text("v" + latestVersion, NamedTextColor.GREEN)));
                        notifyAsync(notifyTarget, Component.text("Run /cc update download to stage the update.", NamedTextColor.YELLOW));
                    } else {
                        notifyAsync(notifyTarget, Component.text("You are running the latest version (v" + currentVersion + ").", NamedTextColor.GREEN));
                    }

                } catch (Exception e) {
                    notifyAsync(notifyTarget, Component.text("Error checking for updates: " + e.getMessage(), NamedTextColor.RED));
                    plugin.getLogger().warning("[Updater] " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Download the latest release JAR and stage it for restart.
     * Paper/Spigot automatically replaces plugin JARs from /plugins/update/ on restart.
     */
    public void downloadUpdate(CommandSender notifyTarget) {
        if (!updateAvailable || latestDownloadUrl == null) {
            notifyTarget.sendMessage(Component.text("No update available. Run /cc update check first.", NamedTextColor.RED));
            return;
        }

        notifyTarget.sendMessage(Component.text("Downloading ChaosCraft v" + latestVersion + "...", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) URI.create(latestDownloadUrl).toURL().openConnection();
                    conn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(60000);

                    if (conn.getResponseCode() != 200) {
                        notifyAsync(notifyTarget, Component.text("Download failed (HTTP " + conn.getResponseCode() + ")", NamedTextColor.RED));
                        return;
                    }

                    // Stage in /plugins/update/ folder (Spigot/Paper auto-update mechanism)
                    Path updateDir = plugin.getDataFolder().getParentFile().toPath().resolve("update");
                    Files.createDirectories(updateDir);

                    // Get the original JAR name
                    File pluginJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                    Path targetPath = updateDir.resolve(pluginJar.getName());

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    long size = Files.size(targetPath);
                    notifyAsync(notifyTarget, Component.text("Update downloaded! ", NamedTextColor.GREEN)
                            .append(Component.text(String.format("(%.1f MB)", size / 1048576.0), NamedTextColor.GRAY)));
                    notifyAsync(notifyTarget, Component.text("Staged at: plugins/update/" + pluginJar.getName(), NamedTextColor.GRAY));
                    notifyAsync(notifyTarget, Component.text("Restart the server to apply the update.", NamedTextColor.YELLOW));

                } catch (Exception e) {
                    notifyAsync(notifyTarget, Component.text("Download failed: " + e.getMessage(), NamedTextColor.RED));
                    plugin.getLogger().warning("[Updater] Download error: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void notifyAsync(CommandSender target, Component message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                target.sendMessage(message);
            }
        }.runTask(plugin);
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
