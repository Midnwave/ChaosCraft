package com.blockforge.chaoscraft;

import com.blockforge.chaoscraft.api.mode.ModeManager;
import com.blockforge.chaoscraft.api.music.MusicManager;
import com.blockforge.chaoscraft.api.timer.ModeTimer;
import com.blockforge.chaoscraft.commands.*;
import com.blockforge.chaoscraft.integration.PlaceholderExpansion;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.chain.ChainMode;
import com.blockforge.chaoscraft.modes.corruption.CorruptionMode;
import com.blockforge.chaoscraft.modes.devilsdream.DevilsDreamMode;
import com.blockforge.chaoscraft.api.points.ModePointsListener;
import com.blockforge.chaoscraft.api.points.ModePointsService;
import com.blockforge.chaoscraft.api.points.PointsCommand;
import com.blockforge.chaoscraft.api.timer.ModeTimerHud;
import com.blockforge.chaoscraft.services.claims.ClaimsService;
import com.blockforge.chaoscraft.services.claims.ClaimVisualization;
import com.blockforge.chaoscraft.services.claims.ClaimCommand;
import com.blockforge.chaoscraft.services.codes.CodesChatListener;
import com.blockforge.chaoscraft.services.codes.CodesService;
import com.blockforge.chaoscraft.services.performance.PerformanceCommand;
import com.blockforge.chaoscraft.services.performance.PerformanceService;
import com.blockforge.chaoscraft.services.placeholders.CCPlaceholders;
import com.blockforge.chaoscraft.services.placeholders.TitleScreenPlaceholders;
import com.blockforge.chaoscraft.services.play.PlayService;
import com.blockforge.chaoscraft.services.settings.SettingsService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenService;
import com.blockforge.chaoscraft.services.useragreement.UserAgreementService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ChaosCraftPlugin extends JavaPlugin {

    private static ChaosCraftPlugin instance;

    // Core APIs
    private ModeTimer modeTimer;
    private ModeManager modeManager;
    private MusicManager musicManager;
    private PerformanceService performanceService;

    // 4.0 ported services
    private NamespacedKey itemTagsKey;
    private final Map<String, YamlConfiguration> yamlRegistry = new LinkedHashMap<>();
    private TitleScreenService titleScreenService;
    private SettingsService settingsService;
    private CodesService codesService;
    private UserAgreementService userAgreementService;
    private PlayService playService;
    private ClaimsService claimsService;
    private ModePointsService modePointsService;
    private ModeTimerHud modeTimerHud;
    private com.blockforge.chaoscraft.weapons.ivory.IvoryService ivoryService;
    private com.blockforge.chaoscraft.updater.UpdateChecker updateChecker;
    private com.blockforge.chaoscraft.services.stats.PlayerStatsService playerStatsService;
    private com.blockforge.chaoscraft.services.badges.BadgeService badgeService;
    private com.blockforge.chaoscraft.services.shop.ShopService shopService;
    private com.blockforge.chaoscraft.services.shop.gui.ShopGUIListener shopGUIListener;
    private com.blockforge.chaoscraft.services.mobspawn.MobSpawnService mobSpawnService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Item tags key
        itemTagsKey = new NamespacedKey(this, "itemtags");

        // Ensure data layout and load YAML files
        ensureDataLayout();
        int loaded = reloadAllYaml();
        getLogger().info("Loaded " + loaded + " YAML file(s).");

        // Initialize core APIs
        modeTimer = new ModeTimer(this);
        modeManager = new ModeManager(this);
        musicManager = new MusicManager(this);

        // Register modes
        CalamityMode calamityMode = new CalamityMode(this);
        modeManager.registerMode(calamityMode);

        ChainMode chainMode = new ChainMode(this);
        modeManager.registerMode(chainMode);

        CorruptionMode corruptionMode = new CorruptionMode(this);
        modeManager.registerMode(corruptionMode);

        DevilsDreamMode devilsDreamMode = new DevilsDreamMode(this);
        modeManager.registerMode(devilsDreamMode);

        com.blockforge.chaoscraft.modes.bluemoon.BlueMoonMode blueMoonMode =
                new com.blockforge.chaoscraft.modes.bluemoon.BlueMoonMode(this);
        modeManager.registerMode(blueMoonMode);

        com.blockforge.chaoscraft.modes.freezingice.FreezingIceMode freezingIceMode =
                new com.blockforge.chaoscraft.modes.freezingice.FreezingIceMode(this);
        modeManager.registerMode(freezingIceMode);

        com.blockforge.chaoscraft.modes.doom.DoomMode doomMode =
                new com.blockforge.chaoscraft.modes.doom.DoomMode(this);
        modeManager.registerMode(doomMode);

        com.blockforge.chaoscraft.modes.seer.SeerMode seerMode =
                new com.blockforge.chaoscraft.modes.seer.SeerMode(this);
        modeManager.registerMode(seerMode);

        // Initialize performance service
        performanceService = new PerformanceService(this);
        performanceService.initialize();

        // Initialize title screen service
        if (getConfig().getBoolean("title-screen.enabled", true)) {
            titleScreenService = new TitleScreenService(this);
            getLogger().info("Title screen service enabled.");
        } else {
            getLogger().info("Title screen disabled in config.yml.");
        }

        // Initialize settings service
        settingsService = new SettingsService(this);
        settingsService.initialize();
        getLogger().info("Settings service enabled.");

        // Initialize codes service
        codesService = new CodesService(this);
        codesService.initialize();
        getLogger().info("Codes service enabled.");

        // Initialize user agreement service
        userAgreementService = new UserAgreementService(this);
        userAgreementService.initialize();
        getLogger().info("User Agreement service enabled.");

        // Initialize play service
        playService = new PlayService(this);
        playService.initialize();
        getLogger().info("Play service enabled.");

        // Initialize claims service
        claimsService = new ClaimsService(this);
        claimsService.initialize();

        // Initialize Mode Points service
        modePointsService = new ModePointsService(this);
        modePointsService.initialize();

        // Initialize Player Stats service (kills, s-kills, mode survivals)
        playerStatsService = new com.blockforge.chaoscraft.services.stats.PlayerStatsService(this);
        playerStatsService.initialize();

        // Initialize Badge service
        badgeService = new com.blockforge.chaoscraft.services.badges.BadgeService(this);
        badgeService.initialize();

        // Initialize Shop service
        shopService = new com.blockforge.chaoscraft.services.shop.ShopService(this);
        shopService.initialize();

        // Initialize universal mob spawn service
        mobSpawnService = new com.blockforge.chaoscraft.services.mobspawn.MobSpawnService(this);

        // Initialize Mode Timer HUD
        modeTimerHud = new ModeTimerHud(this);

        // Initialize update checker
        updateChecker = new com.blockforge.chaoscraft.updater.UpdateChecker(this);
        getLogger().info("Update checker ready. Use /cc update check to check for updates.");

        // Register codes chat listener
        getServer().getPluginManager().registerEvents(new CodesChatListener(this, codesService), this);

        // Register Mode Points listener
        getServer().getPluginManager().registerEvents(new ModePointsListener(this, modePointsService), this);

        // Register Kill Tracking listener
        if (playerStatsService != null) {
            getServer().getPluginManager().registerEvents(
                    new com.blockforge.chaoscraft.services.stats.KillTrackingListener(this, playerStatsService), this);
        }

        // Register Badge listener
        if (badgeService != null) {
            getServer().getPluginManager().registerEvents(
                    new com.blockforge.chaoscraft.services.badges.BadgeListener(this, badgeService), this);
        }

        // Register Shop GUI listener
        if (shopService != null) {
            shopGUIListener = new com.blockforge.chaoscraft.services.shop.gui.ShopGUIListener(this, shopService);
            getServer().getPluginManager().registerEvents(shopGUIListener, this);
        }

        // Register claims listener
        if (claimsService.isEnabled()) {
            var claimViz = new ClaimVisualization(this);
            getServer().getPluginManager().registerEvents(
                    new com.blockforge.chaoscraft.services.claims.ClaimListener(this, claimsService.getManager()), this);

            // Register claim commands
            var claimCmd = new ClaimCommand(this, claimViz);
            for (String cmdName : new String[]{"claim", "trust", "untrust", "trustlist", "claimblocks"}) {
                var cmd = getCommand(cmdName);
                if (cmd != null) {
                    cmd.setExecutor(claimCmd);
                    cmd.setTabCompleter(claimCmd);
                }
            }
        }

        // Initialize Celestial Ivory weapon
        ivoryService = new com.blockforge.chaoscraft.weapons.ivory.IvoryService(this);
        ivoryService.initialize();

        // Register commands
        registerCommands();

        // Register PlaceholderAPI expansions (deferred to next tick so PAPI is fully initialized)
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    // All chaoscraft placeholders in one expansion (game-mode + title screen)
                    new PlaceholderExpansion(this).register();
                    getLogger().info("[PlaceholderAPI] Registered 'chaoscraft' expansion (game-mode + title screen).");

                    // Cross-service placeholders (%cc_*%)
                    new CCPlaceholders(titleScreenService, codesService, settingsService, userAgreementService, playService).register();
                    getLogger().info("[PlaceholderAPI] Registered 'cc' cross-service expansion.");
                } catch (Exception e) {
                    getLogger().severe("[PlaceholderAPI] Failed to register expansions: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            getLogger().warning("PlaceholderAPI not found — placeholders will not be available.");
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(modeManager, this);
        getServer().getPluginManager().registerEvents(musicManager, this);

        // Register Calamity mode sub-system listeners
        getServer().getPluginManager().registerEvents(calamityMode.getEggDetector(), this);
        getServer().getPluginManager().registerEvents(calamityMode.getGemManager(), this);
        getServer().getPluginManager().registerEvents(calamityMode.getPortalManager(), this);
        getServer().getPluginManager().registerEvents(calamityMode.getDragonManager(), this);

        // Dependency check logging
        checkDependency("MythicMobs", "Required for Calamity boss spawning (Voidmaw, Dweller)");
        checkDependency("PlaceholderAPI", "Required for title screen placeholders and HUD");
        checkDependency("ModelEngine", "Optional — used for boss model overlays");
        checkDependency("MythicCrucible", "Optional — used for custom items");
        checkDependency("ItemsAdder", "Optional — used for custom portal blocks");

        // Initial config load for all services (same as /cc reload but on startup)
        reload();

        getLogger().info("ChaosCraft v" + getDescription().getVersion() + " enabled.");
    }

    /**
     * Log whether a dependency plugin is loaded, with a description of what it's used for.
     */
    private void checkDependency(String pluginName, String description) {
        if (getServer().getPluginManager().getPlugin(pluginName) != null) {
            getLogger().info("[Dependency] " + pluginName + " found — " + description);
        } else {
            getLogger().warning("[Dependency] " + pluginName + " NOT found — " + description);
        }
    }

    @Override
    public void onDisable() {
        // End active mode if running
        if (modeManager != null && modeManager.isAnyModeActive()) {
            modeManager.endActiveMode();
        }

        // Shutdown performance service
        if (performanceService != null) {
            performanceService.shutdown();
        }

        // Clean up title screen sessions
        if (titleScreenService != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                titleScreenService.endSession(player.getUniqueId());
            }
            titleScreenService.getPingTracker().stop();
        }

        // Shutdown claims
        if (claimsService != null) {
            claimsService.shutdown();
        }

        // Shutdown mob spawn service
        if (mobSpawnService != null) {
            mobSpawnService.shutdown();
        }

        // Shutdown stats
        if (playerStatsService != null) {
            playerStatsService.shutdown();
        }

        // Shutdown badges
        if (badgeService != null) {
            badgeService.shutdown();
        }

        // Shutdown shop
        if (shopService != null) {
            shopService.shutdown();
        }

        // Stop timer
        if (modeTimer != null) {
            modeTimer.stop();
        }

        getLogger().info("ChaosCraft disabled.");
        instance = null;
    }

    private void registerCommands() {
        // Main /chaoscraft (/cc) command -- handles: timer, devs, reload, exempt, dog, debug,
        // AND new subcommands: entertitlescreen, exittitlescreen, item, itemtag, settings, codes, useragreement, play
        var ccCmd = getCommand("chaoscraft");
        if (ccCmd != null) {
            var handler = new ChaosCraftCommand(this);
            ccCmd.setExecutor(handler);
            ccCmd.setTabCompleter(handler);
        }

        var triggerCmd = getCommand("triggermode");
        if (triggerCmd != null) {
            var handler = new TriggerModeCommand(this);
            triggerCmd.setExecutor(handler);
            triggerCmd.setTabCompleter(handler);
        }

        var endCmd = getCommand("endmode");
        if (endCmd != null) {
            var handler = new EndModeCommand(this);
            endCmd.setExecutor(handler);
            endCmd.setTabCompleter(handler);
        }

        var calamityCmd = getCommand("calamity");
        if (calamityCmd != null) {
            var handler = new CalamityCommand(this);
            calamityCmd.setExecutor(handler);
            calamityCmd.setTabCompleter(handler);
        }

        var chainCmd = getCommand("chain");
        if (chainCmd != null) {
            var handler = new ChainCommand(this);
            chainCmd.setExecutor(handler);
            chainCmd.setTabCompleter(handler);
        }

        // Celestial Ivory
        var ivoryCmd = getCommand("ivory");
        if (ivoryCmd != null && ivoryService != null) {
            var handler = new com.blockforge.chaoscraft.weapons.ivory.IvoryCommand(this, ivoryService);
            ivoryCmd.setExecutor(handler);
            ivoryCmd.setTabCompleter(handler);
        }

        var perfCmd = getCommand("ccperformance");
        if (perfCmd != null) {
            var handler = new PerformanceCommand(this, performanceService);
            perfCmd.setExecutor(handler);
            perfCmd.setTabCompleter(handler);
        }

        // Standalone /settings command
        var settingsCmd = getCommand("settings");
        if (settingsCmd != null) {
            var handler = new SettingsCommand(this);
            settingsCmd.setExecutor(handler);
            settingsCmd.setTabCompleter(handler);
        }

        // Standalone /codes command
        var codesCmd = getCommand("codes");
        if (codesCmd != null) {
            var handler = new CodesCommand(this);
            codesCmd.setExecutor(handler);
            codesCmd.setTabCompleter(handler);
        }

        // Standalone /itemtag command
        var itemTagCmd = getCommand("itemtag");
        if (itemTagCmd != null) {
            var handler = new ItemTagStandaloneCommand(this);
            itemTagCmd.setExecutor(handler);
            itemTagCmd.setTabCompleter(handler);
        }

        // /shop command (standalone, opens shop GUI)
        var shopCmd = getCommand("shop");
        if (shopCmd != null && shopService != null && shopGUIListener != null) {
            var handler = new com.blockforge.chaoscraft.services.shop.ShopCommand(this, shopService, shopGUIListener);
            shopCmd.setExecutor(handler);
            shopCmd.setTabCompleter(handler);
        }

        // /points command
        var pointsCmd = getCommand("points");
        if (pointsCmd != null) {
            var handler = new PointsCommand(this);
            pointsCmd.setExecutor(handler);
            pointsCmd.setTabCompleter(handler);
        }
    }

    public void reload() {
        long reloadStart = System.currentTimeMillis();

        reloadConfig();
        reloadAllYaml();

        long t = System.currentTimeMillis();
        modeManager.reloadConfigs();
        getLogger().info("[Reload] Mode configs in " + (System.currentTimeMillis() - t) + "ms");

        musicManager.reload();

        // Reload service configs
        if (performanceService != null) performanceService.reload();
        if (titleScreenService != null) titleScreenService.loadConfig();
        if (settingsService != null) settingsService.loadConfig();
        if (codesService != null) codesService.loadConfig();
        if (playService != null) playService.reload();
        if (claimsService != null) claimsService.reload();
        if (modePointsService != null) modePointsService.reload();
        if (playerStatsService != null) playerStatsService.reload();
        if (badgeService != null) badgeService.reload();
        if (shopService != null) shopService.reload();

        getLogger().info("[Reload] Services in " + (System.currentTimeMillis() - reloadStart) + "ms");

        // Reload Calamity-specific config + attack configs
        t = System.currentTimeMillis();
        var calamity = modeManager.getMode("calamity");
        if (calamity instanceof CalamityMode calamityMode) {
            calamityMode.getCalamityConfig().load();
            calamityMode.getAttackRegistry().reloadConfigs();
        }
        getLogger().info("[Reload] Calamity in " + (System.currentTimeMillis() - t) + "ms");

        // Reload Corruption-specific config + attack configs
        t = System.currentTimeMillis();
        var corruption = modeManager.getMode("corruption");
        if (corruption instanceof CorruptionMode corruptionMode) {
            corruptionMode.getCorruptionConfig().load();
            corruptionMode.getAttackRegistry().reloadConfigs();
        }
        getLogger().info("[Reload] Corruption in " + (System.currentTimeMillis() - t) + "ms");

        // Reload Chain-specific config + attack configs
        t = System.currentTimeMillis();
        var chain = modeManager.getMode("chain");
        if (chain instanceof ChainMode chainMode) {
            chainMode.getChainConfig().load();
            chainMode.getAttackRegistry().reloadConfigs();
        }
        getLogger().info("[Reload] Chain in " + (System.currentTimeMillis() - t) + "ms");

        long totalMs = System.currentTimeMillis() - reloadStart;
        getLogger().info("ChaosCraft configuration reloaded in " + totalMs + "ms");
    }

    // ---- Data layout ----

    private void ensureDataLayout() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Title screen default config
        File titleScreenFile = new File(getDataFolder(), "titlescreen.yml");
        if (!titleScreenFile.exists()) {
            String defaultYaml = """
                    # ChaosCraft Title Screen Configuration

                    TitleScreen:
                      TeleportLocation:
                        world: "CC"
                        x: 1331.65
                        y: 107.00
                        z: -657.95
                        yaw: 1891.05
                        pitch: 0.90

                      Invisible: true
                      Invincible: true
                      ChatDisabled: true
                      MovementDisabled: true
                      ZeroGravity: true
                      Untargetable: false
                      CommandsDisabled: false

                      Commands:
                        OnJoin:
                          - execute console command "say %player% joined the server!"
                          - execute console command "gamemode adventure %player%"
                          - wait 100
                          - execute console command "say OnJoin script finishing"
                          - nextpart

                        DoneLoading:
                          - execute console command "say %player% has finished loading!"
                          - wait 20
                          - execute console command "give %player% diamond 1"

                        VerificationSuccess:
                          - execute console command "say %player% has verified their resource pack!"
                          - wait 10
                          - execute console command "cc exittitlescreen %player%"

                      LeftClick:
                        Example:
                          Conditions: {}
                          Actions:
                            - execute console command "say %player% left-clicked!"
                            - wait 10
                            - execute console command "cc exittitlescreen %player%"
                    """;
            try (var w = new OutputStreamWriter(new FileOutputStream(titleScreenFile), StandardCharsets.UTF_8)) {
                w.write(defaultYaml);
            } catch (IOException e) {
                getLogger().warning("Failed to write default titlescreen.yml: " + e.getMessage());
            }
        }
    }

    // ---- YAML registry ----

    public int reloadAllYaml() {
        yamlRegistry.clear();
        File base = getDataFolder();
        if (!base.exists()) return 0;

        List<File> files = new ArrayList<>();
        collectYaml(base, files);
        for (File f : files) {
            String key = relativizePath(base, f);
            YamlConfiguration yc = new YamlConfiguration();
            try {
                yc.load(f);
                yamlRegistry.put(key, yc);
            } catch (InvalidConfigurationException | IOException e) {
                getLogger().warning("Failed to load YAML " + key + ": " + e.getMessage());
            }
        }
        return yamlRegistry.size();
    }

    private void collectYaml(File root, List<File> out) {
        File[] arr = root.listFiles();
        if (arr == null) return;
        for (File f : arr) {
            if (f.isDirectory()) collectYaml(f, out);
            else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) out.add(f);
        }
    }

    private String relativizePath(File base, File target) {
        String b = base.getAbsolutePath().replace('\\', '/');
        String t = target.getAbsolutePath().replace('\\', '/');
        if (t.startsWith(b)) t = t.substring(b.length());
        if (t.startsWith("/")) t = t.substring(1);
        return t;
    }

    public YamlConfiguration yaml(String key) {
        return yamlRegistry.get(key);
    }

    // ---- Item Tags (PDC-based) ----

    public NamespacedKey getItemTagsKey() { return itemTagsKey; }

    public Set<String> getItemTags(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return new LinkedHashSet<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new LinkedHashSet<>();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String raw = pdc.get(itemTagsKey, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String p : raw.split(";")) {
            String s = p.trim();
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    public void setItemTags(ItemStack item, Collection<String> tags) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String joined = tags.stream().map(s -> s.replace(";", "").trim()).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(";"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (joined.isEmpty()) pdc.remove(itemTagsKey);
        else pdc.set(itemTagsKey, PersistentDataType.STRING, joined);
        item.setItemMeta(meta);
    }

    public boolean addItemTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        if (clean.isEmpty()) return false;
        Set<String> set = getItemTags(item);
        boolean changed = set.add(clean);
        if (changed) setItemTags(item, set);
        return changed;
    }

    public boolean removeItemTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        if (clean.isEmpty()) return false;
        Set<String> set = getItemTags(item);
        boolean changed = set.remove(clean);
        if (changed) setItemTags(item, set);
        return changed;
    }

    public boolean hasItemTag(ItemStack item, String tag) {
        if (tag == null) return false;
        String clean = tag.replace(";", "").trim();
        return !clean.isEmpty() && getItemTags(item).contains(clean);
    }

    // ---- Radiant Core item factory ----

    public ItemStack createRadiantCoreItem() {
        YamlConfiguration yc = yaml("weapons/radiant_core.yml");
        String requiredTag = "radiant_core";
        if (yc != null) {
            requiredTag = Optional.ofNullable(yc.getString("required-item-tag")).orElse(requiredTag);
        }
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("\u00A76\u00A7lRadiant Core"));
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.lore(List.of(
                Component.text("\u00A7bA stellar heart that siphons darkness."),
                Component.text("\u00A77Left-click to activate its radiant link."),
                Component.text("\u00A78Tag: " + requiredTag)
        ));
        item.setItemMeta(meta);
        addItemTag(item, requiredTag);
        return item;
    }

    // ---- Accessors ----

    public static ChaosCraftPlugin getInstance() { return instance; }
    public ModeTimer getModeTimer() { return modeTimer; }
    public ModeManager getModeManager() { return modeManager; }
    public MusicManager getMusicManager() { return musicManager; }
    public PerformanceService getPerformanceService() { return performanceService; }
    public TitleScreenService getTitleScreenService() { return titleScreenService; }
    public SettingsService getSettingsService() { return settingsService; }
    public CodesService getCodesService() { return codesService; }
    public UserAgreementService getUserAgreementService() { return userAgreementService; }
    public PlayService getPlayService() { return playService; }
    public ClaimsService getClaimsService() { return claimsService; }
    public ModePointsService getModePointsService() { return modePointsService; }
    public ModeTimerHud getModeTimerHud() { return modeTimerHud; }
    public com.blockforge.chaoscraft.services.stats.PlayerStatsService getPlayerStatsService() { return playerStatsService; }
    public com.blockforge.chaoscraft.services.badges.BadgeService getBadgeService() { return badgeService; }
    public com.blockforge.chaoscraft.services.shop.ShopService getShopService() { return shopService; }
    public com.blockforge.chaoscraft.services.shop.gui.ShopGUIListener getShopGUIListener() { return shopGUIListener; }
    public com.blockforge.chaoscraft.updater.UpdateChecker getUpdateChecker() { return updateChecker; }
    public com.blockforge.chaoscraft.weapons.ivory.IvoryService getIvoryService() { return ivoryService; }
    public com.blockforge.chaoscraft.services.mobspawn.MobSpawnService getMobSpawnService() { return mobSpawnService; }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().log(Level.INFO, "[DEBUG] " + message);
        }
    }
}
