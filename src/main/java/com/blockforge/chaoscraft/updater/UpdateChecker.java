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
    private final String buildSha; // Git SHA baked into JAR at build time

    public UpdateChecker(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        this.githubOwner = plugin.getConfig().getString("updater.github-owner", "Midnwave");
        this.githubRepo = plugin.getConfig().getString("updater.github-repo", "ChaosCraft");
        this.buildSha = loadBuildSha();
        if (!"unknown".equals(buildSha)) {
            plugin.getLogger().info("[Updater] Build SHA: " + buildSha);
        }
    }

    private String loadBuildSha() {
        try (var is = plugin.getClass().getClassLoader().getResourceAsStream("build.properties")) {
            if (is == null) return "unknown";
            var props = new java.util.Properties();
            props.load(is);
            return props.getProperty("build.sha", "unknown").trim();
        } catch (Exception e) {
            return "unknown";
        }
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
                    if (code == 404) {
                        // No tagged releases — check latest commit on main instead
                        checkLatestCommit(notifyTarget);
                        return;
                    }
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
     * Download the latest JAR using the GitHub Contents API (no CDN caching).
     * Fetches the download_url from the API response which points to a fresh,
     * uncached blob URL — unlike raw.githubusercontent.com which caches for 5+ min.
     */
    public void downloadUpdate(CommandSender notifyTarget) {
        if (!updateAvailable && latestVersion == null) {
            notifyTarget.sendMessage(Component.text("Run /cc update check first to see if an update is available.", NamedTextColor.YELLOW));
            return;
        }

        // Determine which JAR file to download via the Contents API
        int buildNum = fetchLatestBuildNumber();
        String jarName = buildNum > 0 ? "ChaosCraft-dev-" + buildNum + ".jar" : "ChaosCraft-latest.jar";

        String versionLabel = latestVersion != null ? "v" + latestVersion : "dev build #" + buildNum;
        notifyTarget.sendMessage(Component.text("Downloading ChaosCraft " + versionLabel + " (" + jarName + ")...", NamedTextColor.YELLOW));

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Step 1: Use GitHub Contents API to get the actual download_url (bypasses CDN cache)
                    String contentsUrl = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo
                            + "/contents/release/" + jarName + "?ref=main";

                    HttpURLConnection apiConn = (HttpURLConnection) URI.create(contentsUrl).toURL().openConnection();
                    apiConn.setRequestMethod("GET");
                    apiConn.setRequestProperty("Accept", "application/vnd.github+json");
                    apiConn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
                    apiConn.setConnectTimeout(10000);
                    apiConn.setReadTimeout(10000);

                    int apiCode = apiConn.getResponseCode();
                    if (apiCode != 200) {
                        notifyAsync(notifyTarget, Component.text("Contents API failed (HTTP " + apiCode + ") for " + jarName, NamedTextColor.RED));
                        return;
                    }

                    String apiBody;
                    try (var reader = new BufferedReader(new InputStreamReader(apiConn.getInputStream()))) {
                        var sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        apiBody = sb.toString();
                    }

                    JsonObject fileInfo = JsonParser.parseString(apiBody).getAsJsonObject();
                    String downloadUrl = fileInfo.get("download_url").getAsString();
                    String fileSha = fileInfo.get("sha").getAsString().substring(0, 7);

                    plugin.getLogger().info("[Updater] Downloading from: " + downloadUrl + " (blob " + fileSha + ")");

                    // Step 2: Download the actual JAR from the blob URL
                    HttpURLConnection dlConn = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
                    dlConn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
                    // Force no-cache to bypass any intermediary caches
                    dlConn.setRequestProperty("Cache-Control", "no-cache, no-store");
                    dlConn.setRequestProperty("Pragma", "no-cache");
                    dlConn.setConnectTimeout(30000);
                    dlConn.setReadTimeout(60000);

                    if (dlConn.getResponseCode() != 200) {
                        notifyAsync(notifyTarget, Component.text("Download failed (HTTP " + dlConn.getResponseCode() + ")", NamedTextColor.RED));
                        return;
                    }

                    // Stage in /plugins/update/ folder (Paper auto-update mechanism)
                    Path updateDir = plugin.getDataFolder().getParentFile().toPath().resolve("update");
                    Files.createDirectories(updateDir);

                    File pluginJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                    Path targetPath = updateDir.resolve(pluginJar.getName());

                    try (InputStream in = dlConn.getInputStream()) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    long size = Files.size(targetPath);
                    notifyAsync(notifyTarget, Component.text("Update downloaded! ", NamedTextColor.GREEN)
                            .append(Component.text(String.format("(%.1f MB, blob %s)", size / 1048576.0, fileSha), NamedTextColor.GRAY)));
                    notifyAsync(notifyTarget, Component.text("Staged at: plugins/update/" + pluginJar.getName(), NamedTextColor.GRAY));
                    notifyAsync(notifyTarget, Component.text("Restart the server to apply the update.", NamedTextColor.YELLOW));

                } catch (Exception e) {
                    notifyAsync(notifyTarget, Component.text("Download failed: " + e.getMessage(), NamedTextColor.RED));
                    plugin.getLogger().warning("[Updater] Download error: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Fallback when no tagged releases exist — check latest commit SHA on main.
     * Compares against the build version to see if the repo has newer commits.
     */
    private void checkLatestCommit(CommandSender notifyTarget) {
        try {
            String apiUrl = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo + "/commits/main";
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code != 200) {
                notifyAsync(notifyTarget, Component.text("Failed to check commits (HTTP " + code + ")", NamedTextColor.RED));
                return;
            }

            String body;
            try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                var sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                body = sb.toString();
            }

            JsonObject commit = JsonParser.parseString(body).getAsJsonObject();
            String sha = commit.get("sha").getAsString().substring(0, 7);
            String message = commit.getAsJsonObject("commit").get("message").getAsString();
            if (message.contains("\n")) message = message.substring(0, message.indexOf("\n"));
            if (message.length() > 60) message = message.substring(0, 60) + "...";

            String currentVersion = plugin.getDescription().getVersion();

            // Download URL is resolved at download time via Contents API
            // (no raw.githubusercontent — that caches for 5+ minutes)
            latestDownloadUrl = null; // Will be resolved fresh in downloadUpdate()
            latestVersion = currentVersion + "-" + sha;

            if (buildSha.equals(sha)) {
                updateAvailable = false;
                notifyAsync(notifyTarget, Component.text("You are running the latest build. ", NamedTextColor.GREEN)
                        .append(Component.text("(v" + currentVersion + " @ " + sha + ")", NamedTextColor.GRAY)));
            } else {
                updateAvailable = true;
                notifyAsync(notifyTarget, Component.text("Dev update available!", NamedTextColor.GREEN));
                notifyAsync(notifyTarget, Component.text("  Current: " + ("unknown".equals(buildSha) ? "unknown SHA" : buildSha), NamedTextColor.GRAY));
                notifyAsync(notifyTarget, Component.text("  Latest:  " + sha + " — " + message, NamedTextColor.AQUA));
                notifyAsync(notifyTarget, Component.text("Run /cc update download to stage the update.", NamedTextColor.YELLOW));
            }

        } catch (Exception e) {
            notifyAsync(notifyTarget, Component.text("Error checking commits: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().warning("[Updater] Commit check error: " + e.getMessage());
        }
    }

    /**
     * Fetch the latest successful workflow run number from GitHub Actions API.
     * Returns 0 if unable to determine.
     */
    private int fetchLatestBuildNumber() {
        try {
            String apiUrl = "https://api.github.com/repos/" + githubOwner + "/" + githubRepo
                    + "/actions/runs?branch=main&status=success&per_page=1";
            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "ChaosCraft-Updater");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) return 0;

            String body;
            try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                var sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                body = sb.toString();
            }

            JsonObject result = JsonParser.parseString(body).getAsJsonObject();
            JsonArray runs = result.getAsJsonArray("workflow_runs");
            if (runs != null && !runs.isEmpty()) {
                JsonObject latestRun = runs.get(0).getAsJsonObject();
                return latestRun.get("run_number").getAsInt();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Updater] Failed to fetch build number: " + e.getMessage());
        }
        return 0;
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
