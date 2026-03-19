package com.blockforge.chaoscraft.services.titlescreen;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Represents a single player's title-screen session, including the
 * four-stage loading pipeline, verification, and inventory save/restore.
 */
public class TitleScreenSession {

    private final TitleScreenService service;
    private final UUID playerId;
    private final Player player;
    private final long joinTick;
    private final Random random = new Random();

    // Loading pipeline
    private LoadingStage currentStage = LoadingStage.LOADING_DATA;
    private final AtomicInteger progress = new AtomicInteger(0);
    private boolean isLoading = true;
    private boolean inTitleScreen = true;
    private boolean allowMovement = false;

    // Resource pack tracking
    private boolean resourcePackLoading = false;
    private int resourcePackProgress = 0;

    // Verification
    private String verificationCode;
    private boolean awaitingVerification = false;
    private int verificationAttempts = 0;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private BukkitTask verificationTimeoutTask;

    // Script gating
    private boolean nextPartGateOpen = false;

    // Tasks
    private BukkitTask currentTask;
    private BukkitTask movementRestoreTask;

    // Original state
    private boolean originalInvisible;

    public TitleScreenSession(TitleScreenService service, Player player) {
        this.service = service;
        this.player = player;
        this.playerId = player.getUniqueId();
        this.joinTick = service.getPlugin().getServer().getCurrentTick();
    }

    // ---- Lifecycle ----

    public void start() {
        saveOriginalState();
        applyTitleScreenEffects();

        boolean hasOnJoinScript = !service.getConfig().getScript("OnJoin").isEmpty();
        if (hasOnJoinScript) {
            nextPartGateOpen = false;
            log("Gate closed for " + player.getName() + " (OnJoin script detected)");
            service.executeScript("OnJoin", player);
        } else {
            nextPartGateOpen = true;
            log("Gate open for " + player.getName() + " (no OnJoin script)");
        }

        startLoadingData();
    }

    public void exitTitleScreen() {
        cancelTask(currentTask);
        currentTask = null;
        cancelTask(movementRestoreTask);
        movementRestoreTask = null;
        cancelTask(verificationTimeoutTask);
        verificationTimeoutTask = null;

        inTitleScreen = false;
        isLoading = false;
        awaitingVerification = false;
        allowMovement = true;
        restoreOriginalState();
    }

    // ---- State management ----

    private void saveOriginalState() {
        originalInvisible = player.isInvisible();
        saveInventoryToFile();
        player.getInventory().clear();
    }

    private void applyTitleScreenEffects() {
        TitleScreenConfig config = service.getConfig();
        if (config.hasTeleportLocation()) {
            player.teleport(config.getTeleportLocation());
        }
        if (config.isInvisible()) {
            player.setInvisible(true);
        }
        if (config.isZeroGravity()) {
            player.setGravity(false);
        }
    }

    private void restoreOriginalState() {
        player.setInvisible(false);
        player.setGravity(true);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        restoreInventoryFromFile();
    }

    // ---- Loading stages ----

    private void startLoadingData() {
        currentStage = LoadingStage.LOADING_DATA;
        progress.set(0);
        currentTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            private final int totalDuration = 60 + random.nextInt(20);
            private int nextProgressUpdate = random.nextInt(3) + 1;
            private int pauseAtHundredTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                ticksElapsed++;
                if (progress.get() >= 100) {
                    if (++pauseAtHundredTicks >= 60) {
                        cancel();
                        Bukkit.getScheduler().runTask(service.getPlugin(), TitleScreenSession.this::startLoadingAssets);
                    }
                    return;
                }

                if (ticksElapsed >= nextProgressUpdate) {
                    int increment = random.nextInt(3) + 1;
                    progress.set(Math.min(100, progress.get() + increment));
                    nextProgressUpdate = ticksElapsed + random.nextInt(5) + 1;
                }

                if (ticksElapsed >= totalDuration && progress.get() < 100) {
                    progress.set(100);
                }
            }
        }.runTaskTimer(service.getPlugin(), 0L, 1L);
    }

    private void startLoadingAssets() {
        currentStage = LoadingStage.LOADING_ASSETS;
        progress.set(0);
        currentTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            private int nextProgressUpdate = random.nextInt(3) + 1;
            private int pauseAtHundredTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                ticksElapsed++;
                if (progress.get() >= 100) {
                    if (++pauseAtHundredTicks >= 60) {
                        cancel();
                        Bukkit.getScheduler().runTask(service.getPlugin(), TitleScreenSession.this::startStabilizingPing);
                    }
                    return;
                }

                if (resourcePackLoading && resourcePackProgress > 0) {
                    progress.set(Math.min(100, resourcePackProgress));
                } else if (ticksElapsed >= nextProgressUpdate) {
                    int increment = random.nextInt(4) + 2;
                    progress.set(Math.min(100, progress.get() + increment));
                    nextProgressUpdate = ticksElapsed + random.nextInt(4) + 2;
                }

                if (ticksElapsed >= 100 && progress.get() < 100) {
                    progress.set(100);
                }
            }
        }.runTaskTimer(service.getPlugin(), 0L, 1L);
    }

    private void startStabilizingPing() {
        currentStage = LoadingStage.STABILIZING_PING;
        progress.set(0);
        currentTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            private int pauseAtHundredTicks = 0;
            private boolean pingAcceptable = false;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                ticksElapsed++;
                if (progress.get() >= 100) {
                    if (++pauseAtHundredTicks >= 60) {
                        cancel();
                        Bukkit.getScheduler().runTask(service.getPlugin(), TitleScreenSession.this::startFinalizing);
                    }
                    return;
                }

                if (ticksElapsed <= 200) {
                    progress.set(Math.min(99, ticksElapsed * 99 / 200));
                }

                if (ticksElapsed >= 200) {
                    int currentPing = service.getPingTracker().getPing(player);
                    if (currentPing < 500 && currentPing >= 0) {
                        if (!pingAcceptable) {
                            log("Ping stabilized for " + player.getName() + " (" + currentPing + "ms)");
                            pingAcceptable = true;
                        }
                    } else {
                        log("Ping check complete for " + player.getName() + " (ping: " + currentPing + "ms) - continuing");
                    }
                    progress.set(100);
                }
            }
        }.runTaskTimer(service.getPlugin(), 0L, 1L);
    }

    private void startFinalizing() {
        currentStage = LoadingStage.FINALIZING;
        progress.set(0);
        currentTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            private final int totalDuration = 180 + random.nextInt(40);
            private int nextProgressUpdate = random.nextInt(4) + 2;
            private int pauseAtHundredTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                ticksElapsed++;
                if (progress.get() >= 100) {
                    if (++pauseAtHundredTicks >= 60) {
                        cancel();
                        Bukkit.getScheduler().runTask(service.getPlugin(), TitleScreenSession.this::completeLoading);
                    }
                    return;
                }

                if (ticksElapsed >= nextProgressUpdate) {
                    int increment = random.nextInt(2) + 1;
                    progress.set(Math.min(100, progress.get() + increment));
                    nextProgressUpdate = ticksElapsed + random.nextInt(6) + 3;
                }

                if (ticksElapsed >= totalDuration && progress.get() < 100) {
                    progress.set(100);
                }
            }
        }.runTaskTimer(service.getPlugin(), 0L, 1L);
    }

    private void completeLoading() {
        isLoading = false;
        log("Loading completed for " + player.getName() + ", executing DoneLoading script");
        service.executeScript("DoneLoading", player, () -> {
            log("DoneLoading completed for " + player.getName() + ", starting verification");
            startVerification();
        });
    }

    // ---- Verification ----

    private void startVerification() {
        verificationCode = String.format("%010d", random.nextInt(1_000_000_000));
        awaitingVerification = true;
        verificationAttempts = 0;
        log("Verification code for " + player.getName() + ": " + verificationCode);

        player.sendMessage(Component.text(
                "Please enter the code you see on screen in order to confirm your resource pack is loaded.",
                NamedTextColor.GREEN
        ));

        verificationTimeoutTask = Bukkit.getScheduler().runTaskLater(service.getPlugin(), () -> {
            if (awaitingVerification) {
                player.kick(Component.text("Failed to enter in correct code within expected time frame", NamedTextColor.RED));
            }
        }, 24000L);
    }

    /**
     * @return true if the message was consumed by the verification system
     */
    public boolean handleVerificationAttempt(String message) {
        if (!awaitingVerification) return false;

        verificationAttempts++;

        if (message.equals(verificationCode)) {
            awaitingVerification = false;
            cancelTask(verificationTimeoutTask);

            log(player.getName() + " passed verification (attempt " + verificationAttempts + "/" + MAX_VERIFICATION_ATTEMPTS + ")");
            player.sendMessage(Component.text("Verification successful!", NamedTextColor.GREEN));

            Bukkit.getScheduler().runTask(service.getPlugin(), () -> {
                log("Executing VerificationSuccess for " + player.getName());
                service.executeScript("VerificationSuccess", player);
            });
            return true;
        }

        int attemptsLeft = MAX_VERIFICATION_ATTEMPTS - verificationAttempts;
        if (attemptsLeft <= 0) {
            awaitingVerification = false;
            cancelTask(verificationTimeoutTask);

            Bukkit.getScheduler().runTask(service.getPlugin(), () -> {
                player.kick(Component.text(
                        "It seems like you do not have the resource pack loaded, please try again. " +
                                "If the issue persists, join our discord https://discord.gg/jQGMhKA5m6",
                        NamedTextColor.RED
                ));
            });
            log(player.getName() + " failed verification (" + MAX_VERIFICATION_ATTEMPTS + "/" + MAX_VERIFICATION_ATTEMPTS + " attempts used)");
            return true;
        }

        player.sendMessage(Component.text(
                "Incorrect code. You have " + attemptsLeft + " attempt(s) remaining.",
                NamedTextColor.RED
        ));
        log(player.getName() + " incorrect verification code (attempt " + verificationAttempts + "/" + MAX_VERIFICATION_ATTEMPTS + ")");
        return true;
    }

    // ---- Script gating ----

    public void openNextPartGate() {
        nextPartGateOpen = true;
        log("Gate opened for " + player.getName());
        if (!isLoading && awaitingVerification) {
            log("Gate opened - executing pending DoneLoading for " + player.getName());
            service.executeScript("DoneLoading", player);
        }
    }

    // ---- Resource pack ----

    public void setResourcePackLoading(boolean loading) {
        this.resourcePackLoading = loading;
    }

    public void setResourcePackProgress(int progress) {
        this.resourcePackProgress = Math.max(0, Math.min(100, progress));
    }

    // ---- Inventory persistence ----

    private void saveInventoryToFile() {
        try {
            File invFile = new File(service.getPlugin().getDataFolder(), "inventories.yml");
            YamlConfiguration invConfig = YamlConfiguration.loadConfiguration(invFile);
            String uuid = player.getUniqueId().toString();

            if (invConfig.contains(uuid)) {
                log("Inventory already saved for " + player.getName() + " - not overwriting");
                return;
            }

            invConfig.set(uuid + ".contents", player.getInventory().getContents());
            invConfig.set(uuid + ".armor", player.getInventory().getArmorContents());
            invConfig.set(uuid + ".offhand", player.getInventory().getItemInOffHand());
            invConfig.save(invFile);
            log("Saved inventory for " + player.getName() + " to inventories.yml");
        } catch (Exception e) {
            service.getPlugin().getLogger().severe("Failed to save inventory for " + player.getName() + ":");
            e.printStackTrace();
        }
    }

    private void restoreInventoryFromFile() {
        try {
            File invFile = new File(service.getPlugin().getDataFolder(), "inventories.yml");
            if (!invFile.exists()) {
                service.getPlugin().getLogger().warning("inventories.yml does not exist - cannot restore inventory for " + player.getName());
                return;
            }

            YamlConfiguration invConfig = YamlConfiguration.loadConfiguration(invFile);
            String uuid = player.getUniqueId().toString();

            if (!invConfig.contains(uuid)) {
                service.getPlugin().getLogger().warning("No saved inventory found for " + player.getName());
                return;
            }

            Object contentsObj = invConfig.get(uuid + ".contents");
            Object armorObj = invConfig.get(uuid + ".armor");
            Object offhandObj = invConfig.get(uuid + ".offhand");

            if (contentsObj instanceof ItemStack[] contents) {
                player.getInventory().setContents(contents);
            }
            if (armorObj instanceof ItemStack[] armor) {
                player.getInventory().setArmorContents(armor);
            }
            if (offhandObj instanceof ItemStack offhand) {
                player.getInventory().setItemInOffHand(offhand);
            }

            invConfig.set(uuid, null);
            invConfig.save(invFile);
            log("Restored inventory for " + player.getName() + " from inventories.yml");
        } catch (Exception e) {
            service.getPlugin().getLogger().severe("Failed to restore inventory for " + player.getName() + ":");
            e.printStackTrace();
        }
    }

    // ---- Helpers ----

    private void cancelTask(BukkitTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private void log(String message) {
        service.getPlugin().getLogger().info(message);
    }

    // ---- Accessors ----

    public LoadingStage getCurrentStage() { return currentStage; }
    public int getProgress() { return progress.get(); }
    public boolean isLoading() { return isLoading; }
    public boolean isInTitleScreen() { return inTitleScreen; }
    public UUID getPlayerId() { return playerId; }
    public long getJoinTick() { return joinTick; }
    public boolean shouldAllowMovement() { return allowMovement; }
    public String getVerificationCode() { return verificationCode != null ? verificationCode : ""; }
    public boolean isAwaitingVerification() { return awaitingVerification; }
    public int getVerificationAttempts() { return verificationAttempts; }
    public int getRemainingVerificationAttempts() { return MAX_VERIFICATION_ATTEMPTS - verificationAttempts; }
    public boolean isNextPartGateOpen() { return nextPartGateOpen; }

    // ---- Loading stage enum ----

    public enum LoadingStage {
        LOADING_DATA("Loading Data"),
        LOADING_ASSETS("Loading Assets"),
        STABILIZING_PING("Stabilizing Ping"),
        FINALIZING("Finalizing");

        private final String displayName;

        LoadingStage(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }
}
