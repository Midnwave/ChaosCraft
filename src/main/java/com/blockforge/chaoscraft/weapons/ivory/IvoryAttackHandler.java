package com.blockforge.chaoscraft.weapons.ivory;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class IvoryAttackHandler implements Listener {

    private final ChaosCraftPlugin plugin;
    private IvoryConfig config;
    private final IvoryStateManager stateManager;
    private final IvorySoundManager soundManager;
    private final IvoryEffectsManager effectsManager;

    public IvoryAttackHandler(ChaosCraftPlugin plugin, IvoryConfig config,
                              IvoryStateManager stateManager, IvorySoundManager soundManager,
                              IvoryEffectsManager effectsManager) {
        this.plugin = plugin;
        this.config = config;
        this.stateManager = stateManager;
        this.soundManager = soundManager;
        this.effectsManager = effectsManager;
    }

    public void updateConfig(IvoryConfig config) {
        this.config = config;
    }

    public void cleanup() {}

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClick(PlayerInteractEvent event) {
        if (!config.isEnabled()) return;
        var action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        var state = stateManager.getState(item);
        if (state == null) return;

        switch (state) {
            case OFF, CHARGING -> event.setCancelled(true);
            case ACTIVE, RAGE -> {
                if (stateManager.isOnAttackCooldown(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!config.isEnabled()) return;
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        var state = stateManager.getState(item);
        if (state == null) return;

        Entity target = event.getEntity();
        if (!isValidTarget(target)) return;

        switch (state) {
            case OFF, CHARGING -> {
                event.setCancelled(true);
                return;
            }
            case ACTIVE, RAGE -> {
                if (stateManager.isOnAttackCooldown(player)) {
                    event.setCancelled(true);
                    return;
                }
                boolean isAbilityDamage = target.hasMetadata("chaoscraft_ability_damage");
                double baseDamage = (state == IvoryService.IvoryState.RAGE)
                        ? config.getBaseDamageRage()
                        : config.getBaseDamageActive();
                event.setDamage(baseDamage);

                if (!isAbilityDamage) {
                    soundManager.playSwingSound(player);
                    soundManager.playHitSound(target.getLocation());
                    applyKnockback(player, target, state);
                    if (target instanceof LivingEntity livingTarget) {
                        effectsManager.applyHitEffects(player, livingTarget);
                    }
                    executeOnHitScript(player, target);
                    stateManager.setAttackCooldown(player);
                }
            }
        }
    }

    private void applyKnockback(Player player, Entity target, IvoryService.IvoryState state) {
        if (!(target instanceof LivingEntity)) return;
        double horizontal = (state == IvoryService.IvoryState.RAGE)
                ? config.getKnockbackHorizontalRage()
                : config.getKnockbackHorizontalActive();
        double vertical = (state == IvoryService.IvoryState.RAGE)
                ? config.getKnockbackVerticalRage()
                : config.getKnockbackVerticalActive();
        if (horizontal == 0.0 && vertical == 0.0) return;

        Vector direction = target.getLocation().toVector()
                .subtract(player.getLocation().toVector()).normalize();
        Vector knockback = new Vector(direction.getX() * horizontal, vertical, direction.getZ() * horizontal);
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                target.setVelocity(target.getVelocity().add(knockback)), 1L);
    }

    private boolean isValidTarget(Entity entity) {
        if (entity instanceof Player) return false;
        if (entity instanceof ArmorStand) return false;
        return entity instanceof LivingEntity;
    }

    private String getMinecraftDimension(World world) {
        if (world == null) return "minecraft:overworld";
        return switch (world.getEnvironment()) {
            case NETHER -> "minecraft:the_nether";
            case THE_END -> "minecraft:the_end";
            default -> "minecraft:overworld";
        };
    }

    private void executeOnHitScript(Player player, Entity target) {
        List<String> script = config.getOnHitScript();
        if (script == null || script.isEmpty()) return;

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();
        String worldDimension = getMinecraftDimension(targetLoc.getWorld());
        String playerWorldDimension = getMinecraftDimension(playerLoc.getWorld());

        for (String line : script) {
            line = line.trim();
            if (line.startsWith("execute console command ")) {
                String command = line.substring("execute console command ".length()).trim();
                if (command.startsWith("\"") && command.endsWith("\"")) {
                    command = command.substring(1, command.length() - 1);
                }
                command = command.replace("%player%", player.getName())
                        .replace("%player_world%", playerWorldDimension)
                        .replace("%player_x%", String.valueOf(playerLoc.getBlockX()))
                        .replace("%player_y%", String.valueOf(playerLoc.getBlockY()))
                        .replace("%player_z%", String.valueOf(playerLoc.getBlockZ()))
                        .replace("%entity%", target.getType().name())
                        .replace("%world%", worldDimension)
                        .replace("%x%", String.valueOf(targetLoc.getBlockX()))
                        .replace("%y%", String.valueOf(targetLoc.getBlockY()))
                        .replace("%z%", String.valueOf(targetLoc.getBlockZ()))
                        .replace("~ ~ ~", targetLoc.getBlockX() + " " + targetLoc.getBlockY() + " " + targetLoc.getBlockZ());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public long getRemainingCooldownMs(Player player) {
        return stateManager.getRemainingCooldownMs(player);
    }

    public boolean canAttack(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return false;
        var state = stateManager.getState(item);
        if (state == null) return false;
        return switch (state) {
            case ACTIVE, RAGE -> !stateManager.isOnAttackCooldown(player);
            default -> false;
        };
    }
}
