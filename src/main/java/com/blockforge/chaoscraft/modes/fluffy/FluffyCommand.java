package com.blockforge.chaoscraft.modes.fluffy;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fluffy mode admin commands. Dispatched via {@code /cc modes fluffy <sub>}.
 *
 * Subcommands:
 *  - status                  — runtime info
 *  - debug                   — toggle plugin-wide debug
 *  - test [id]               — list attack ids OR force-spawn one on yourself
 *  - list                    — list every attack with damage/radius/duration
 *  - clearattacks            — remove all currently active attacks
 *  - spawninterval <ticks>   — change attack spawn interval at runtime
 *  - rainspawn               — fire a single rain-from-sky drop now
 *  - rainstatus              — number of falling mobs + rain config
 *  - flora <small|medium|large> — fire a flora bloom event manually
 *  - toggleexempt [player]   — toggle attack exemption for a player
 *  - reload                  — reload fluffy config + attack configs
 */
public class FluffyCommand implements CommandExecutor, TabCompleter {

    private final ChaosCraftPlugin plugin;

    public FluffyCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.fluffy.admin") && !sender.hasPermission("chaoscraft.admin")) {
            sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "status" -> handleStatus(sender);
            case "debug" -> handleDebug(sender);
            case "test" -> handleTest(sender, args);
            case "list" -> handleList(sender);
            case "clearattacks" -> handleClearAttacks(sender);
            case "spawninterval" -> handleSpawnInterval(sender, args);
            case "rainspawn" -> handleRainSpawn(sender);
            case "rainstatus" -> handleRainStatus(sender);
            case "flora" -> handleFlora(sender, args);
            case "toggleexempt" -> handleToggleExempt(sender, args);
            case "reload" -> handleReload(sender);
            case "aispawn" -> handleAiSpawn(sender, args);
            case "aitrigger" -> handleAiTrigger(sender, args);
            case "ailist" -> handleAiList(sender);
            default -> { sendHelp(sender); yield true; }
        };
    }

    // ========================
    // AI roster admin commands
    // ========================

    private static final List<String> AI_TYPES = List.of(
            "bunny", "bear", "fox", "dog", "bird", "default", "fluffy_cat", "fluffy_squirrel");

    private static EntityType defaultEntityForType(String type) {
        return switch (type) {
            case "bunny" -> EntityType.RABBIT;
            case "bear" -> EntityType.POLAR_BEAR;
            case "fox" -> EntityType.FOX;
            case "dog" -> EntityType.WOLF;
            case "bird" -> EntityType.PARROT;
            case "fluffy_cat" -> EntityType.CAT;
            case "fluffy_squirrel" -> EntityType.RABBIT;
            default -> EntityType.COW;
        };
    }

    private boolean handleAiSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        FluffyMode mode = getMode();
        if (mode == null || mode.getMobAI() == null) {
            sender.sendMessage(Component.text("Fluffy mob AI unavailable.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: aispawn <type> [variant-id]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Types: " + String.join(", ", AI_TYPES), NamedTextColor.GRAY));
            return true;
        }
        String type = args[1].toLowerCase();
        if (!AI_TYPES.contains(type)) {
            sender.sendMessage(Component.text("Unknown type: " + type, NamedTextColor.RED));
            sender.sendMessage(Component.text("Types: " + String.join(", ", AI_TYPES), NamedTextColor.GRAY));
            return true;
        }

        Location loc = player.getLocation();
        LivingEntity spawned = null;

        if (args.length >= 3) {
            String variant = args[2];
            // Try MythicMobs first
            try {
                Class<?> mmApiCls = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                Object inst = mmApiCls.getMethod("inst").invoke(null);
                Object mobManager = mmApiCls.getMethod("getMobManager").invoke(inst);
                Object optMob = mobManager.getClass().getMethod("getMythicMob", String.class).invoke(mobManager, variant);
                if (optMob != null) {
                    Object opt = optMob;
                    Object mythicMob = opt.getClass().getMethod("orElse", Object.class).invoke(opt, new Object[]{null});
                    if (mythicMob != null) {
                        Class<?> bukkitAdapter = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
                        Object abstractLoc = bukkitAdapter.getMethod("adapt", Location.class).invoke(null, loc);
                        Object active = mythicMob.getClass().getMethod("spawn",
                                Class.forName("io.lumine.mythic.api.adapters.AbstractLocation"), double.class)
                                .invoke(mythicMob, abstractLoc, 1.0);
                        if (active != null) {
                            Object bukkitEnt = active.getClass().getMethod("getEntity").invoke(active);
                            Object actualBukkit = bukkitEnt.getClass().getMethod("getBukkitEntity").invoke(bukkitEnt);
                            if (actualBukkit instanceof LivingEntity le) spawned = le;
                        }
                    }
                }
            } catch (Throwable ignored) {
                // MythicMobs not present or variant not an MM mob — fall back to vanilla
            }

            if (spawned == null) {
                // Try as vanilla EntityType
                try {
                    EntityType et = EntityType.valueOf(variant.toUpperCase());
                    Entity e = loc.getWorld().spawnEntity(loc, et);
                    if (e instanceof LivingEntity le) spawned = le;
                    else { e.remove(); }
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Component.text("Variant '" + variant + "' not a valid MM id or EntityType.",
                            NamedTextColor.RED));
                    return true;
                }
            }
        } else {
            EntityType et = defaultEntityForType(type);
            try {
                Entity e = loc.getWorld().spawnEntity(loc, et);
                if (e instanceof LivingEntity le) spawned = le;
                else { e.remove(); }
            } catch (Throwable t) {
                sender.sendMessage(Component.text("Failed to spawn: " + t.getMessage(), NamedTextColor.RED));
                return true;
            }
        }

        if (spawned == null) {
            sender.sendMessage(Component.text("Spawn failed.", NamedTextColor.RED));
            return true;
        }
        spawned.addScoreboardTag("fluffy:managed");
        spawned.addScoreboardTag("fluffy:type:" + type);
        mode.getMobAI().register(spawned);
        sender.sendMessage(Component.text("Spawned " + spawned.getType().name() + " as type=" + type
                + " at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAiTrigger(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        FluffyMode mode = getMode();
        if (mode == null || mode.getMobAI() == null) {
            sender.sendMessage(Component.text("Fluffy mob AI unavailable.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: aitrigger <typekey> <special-name>", NamedTextColor.YELLOW));
            sendSpecialList(sender);
            return true;
        }
        String typeKey = args[1].toLowerCase();
        String specialName = args[2].toLowerCase();
        if (!AI_TYPES.contains(typeKey)) {
            sender.sendMessage(Component.text("Unknown type: " + typeKey, NamedTextColor.RED));
            sendSpecialList(sender);
            return true;
        }
        if (!validSpecialsFor(typeKey).contains(specialName)) {
            sender.sendMessage(Component.text("Unknown special '" + specialName + "' for type " + typeKey,
                    NamedTextColor.RED));
            sender.sendMessage(Component.text("Valid: " + String.join(", ", validSpecialsFor(typeKey)),
                    NamedTextColor.GRAY));
            return true;
        }
        LivingEntity nearest = mode.getMobAI().findNearestManaged(player.getLocation(), typeKey, 30.0);
        if (nearest == null) {
            sender.sendMessage(Component.text("No managed " + typeKey + " mob within 30 blocks.",
                    NamedTextColor.RED));
            return true;
        }
        boolean ok = mode.getMobAI().forceTriggerSpecial(nearest, specialName);
        if (ok) {
            sender.sendMessage(Component.text("Triggered '" + specialName + "' on "
                    + nearest.getType().name() + " (" + nearest.getUniqueId() + ").", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to trigger (no target?).", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleAiList(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        FluffyConfig cfg = mode.getFluffyConfig();
        sender.sendMessage(Component.text("=== Fluffy Per-Type Specials ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("global special-roll-on-attack: " + cfg.getSpecialRollOnAttack(),
                NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[bunny]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  tackle (cd " + cfg.getBunnyTackleCooldownTicks()
                + "t, dmgx" + cfg.getBunnyTackleDamageMult() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  multiply (cd " + cfg.getBunnyMultiplyCooldownTicks()
                + "t, hp<=" + cfg.getBunnyMultiplyHpPercent() + "%, chance "
                + cfg.getBunnyMultiplyChance() + ", spawns " + cfg.getBunnyMultiplySpawnCount() + ")",
                NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[bear]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  paw_swipe (cd " + cfg.getBearPawCooldownTicks()
                + "t, cone " + cfg.getBearPawConeRadius() + "/" + cfg.getBearPawConeAngleDeg()
                + "deg, dmgx" + cfg.getBearPawDamageMult() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  slam (cd " + cfg.getBearSlamCooldownTicks()
                + "t, r" + cfg.getBearSlamRadius() + ", knockup " + cfg.getBearSlamKnockupY()
                + ", dmgx" + cfg.getBearSlamDamageMult() + ")", NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[fox]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  pounce (cd " + cfg.getFoxPounceCooldownTicks()
                + "t, dist " + cfg.getFoxPounceDistance() + ", dmgx" + cfg.getFoxPounceDamageMult() + ")",
                NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[dog]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  pack_lunge (cd " + cfg.getDogPackLungeCooldownTicks()
                + "t, alert " + cfg.getDogPackAlertRange() + ", spdx" + cfg.getDogPackLungeSpeedMult() + ")",
                NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[bird]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  dive (cd " + cfg.getBirdDiveCooldownTicks()
                + "t, rise " + cfg.getBirdDiveYRise() + ", drop " + cfg.getBirdDiveYDropVel()
                + ", dmgx" + cfg.getBirdDiveDamageMult() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  buffet (cd " + cfg.getBirdBuffetCooldownTicks()
                + "t, range " + cfg.getBirdBuffetRange() + ", knock " + cfg.getBirdBuffetKnockback()
                + ", dmgx" + cfg.getBirdBuffetDamageMult() + ")", NamedTextColor.GRAY));

        sender.sendMessage(Component.text("[fluffy_cat]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  wiggle_pounce (chance " + cfg.getCatWigglePounceChance()
                + ", stalk/wiggle/prep " + cfg.getCatStalkTicks() + "/" + cfg.getCatWiggleTicks() + "/"
                + cfg.getCatPrepareAttackTicks() + "t, dmgx" + cfg.getCatPounceDamageMult() + ")",
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  triple_swipe (chance " + cfg.getCatTripleSwipeChance()
                + ", interval " + cfg.getCatTripleSwipeIntervalTicks() + "t, dmgx"
                + cfg.getCatTripleSwipeDamageMult() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  no_theatre (fallback melee)", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  shared cooldown " + cfg.getCatSpecialCooldownTicks() + "t",
                NamedTextColor.DARK_GRAY));

        sender.sendMessage(Component.text("[fluffy_squirrel]", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  jump_pounce (cd " + cfg.getSquirrelJumpPounceCooldownTicks()
                + "t, dist " + cfg.getSquirrelJumpPounceDistance() + ", dmgx"
                + cfg.getSquirrelJumpPounceDamageMult() + ")", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  dart (cd " + cfg.getSquirrelDartCooldownTicks()
                + "t, hp<=" + cfg.getSquirrelDartHpPercent() + "%, hops "
                + cfg.getSquirrelDartHopCount() + "x@" + cfg.getSquirrelDartHopDistance() + "b)",
                NamedTextColor.GRAY));

        sender.sendMessage(Component.text("Managed mobs: "
                + (mode.getMobAI() == null ? 0 : mode.getMobAI().getManagedMobCount()), NamedTextColor.GRAY));
        return true;
    }

    private List<String> validSpecialsFor(String typeKey) {
        return switch (typeKey) {
            case "bunny" -> List.of("tackle", "multiply");
            case "bear" -> List.of("paw_swipe", "slam");
            case "fox" -> List.of("pounce");
            case "dog" -> List.of("pack_lunge");
            case "bird" -> List.of("dive", "buffet");
            case "fluffy_cat" -> List.of("wiggle_pounce", "triple_swipe", "no_theatre");
            case "fluffy_squirrel" -> List.of("jump_pounce", "dart");
            default -> Collections.emptyList();
        };
    }

    private void sendSpecialList(CommandSender sender) {
        for (String t : AI_TYPES) {
            List<String> specials = validSpecialsFor(t);
            if (specials.isEmpty()) continue;
            sender.sendMessage(Component.text(t + ": " + String.join(", ", specials), NamedTextColor.GRAY));
        }
    }

    private boolean handleStatus(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Fluffy mode not registered.", NamedTextColor.RED));
            return true;
        }
        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive() && modeManager.getActiveMode() instanceof FluffyMode;

        sender.sendMessage(Component.text("=== Fluffy Status ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"),
                active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(),
                NamedTextColor.LIGHT_PURPLE));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(),
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));
            var world = mode.getFluffyWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.LIGHT_PURPLE));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int meCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount
                + " | ModelEngine: " + meCount, NamedTextColor.LIGHT_PURPLE));

        FluffyConfig cfg = mode.getFluffyConfig();
        sender.sendMessage(Component.text("Duration: " + cfg.getDurationSeconds() + "s | Spawn interval: "
                + cfg.getBaseSpawnInterval() + " ticks", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Rain: " + cfg.getRainSpawnsPerWindow() + " per "
                + cfg.getRainWindowSeconds() + "s | Anim speed: " + cfg.getMobAiAnimationSpeed() + "x",
                NamedTextColor.GRAY));
        return true;
    }

    private boolean handleDebug(CommandSender sender) {
        boolean current = plugin.getConfig().getBoolean("debug", false);
        plugin.getConfig().set("debug", !current);
        plugin.saveConfig();
        sender.sendMessage(Component.text("Debug mode: " + (!current ? "ON" : "OFF"),
                !current ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Fluffy mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
                List<String> ids = mode.getAttackRegistry().getIds(1, type);
                if (!ids.isEmpty()) {
                    sender.sendMessage(Component.text(type.name() + " (" + ids.size() + "):",
                            NamedTextColor.LIGHT_PURPLE));
                    sendIdList(sender, ids);
                }
            }
            return true;
        }

        String id = args[1].toLowerCase();
        AbstractAttack attack = null;
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            attack = mode.getAttackRegistry().get(1, type, id);
            if (attack != null) break;
        }
        if (attack == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            return true;
        }
        mode.getAttackScheduler().forceSpawn(attack, player);
        sender.sendMessage(Component.text("Spawned: " + id, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Damage: " + attack.getConfig().getDamage()
                + " | Radius: " + attack.getConfig().getDamageRadius()
                + " | Duration: " + attack.getConfig().getDurationTicks() + "t", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleClearAttacks(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        int count = mode.getAttackScheduler().getActiveAttackCount();
        mode.getAttackScheduler().clearActiveAttacks();
        sender.sendMessage(Component.text("Cleared " + count + " active attacks.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSpawnInterval(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: spawninterval <ticks>", NamedTextColor.YELLOW));
            return true;
        }
        try {
            int ticks = Math.max(1, Integer.parseInt(args[1]));
            mode.getAttackScheduler().setBaseSpawnInterval(ticks);
            sender.sendMessage(Component.text("Spawn interval: " + ticks + " ticks.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleRainSpawn(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getRainSpawner() == null) {
            sender.sendMessage(Component.text("Rain spawner unavailable.", NamedTextColor.RED));
            return true;
        }
        mode.getRainSpawner().forceDrop();
        sender.sendMessage(Component.text("Forced rain drop fired.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleRainStatus(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getRainSpawner() == null) {
            sender.sendMessage(Component.text("Rain spawner unavailable.", NamedTextColor.RED));
            return true;
        }
        FluffyConfig cfg = mode.getFluffyConfig();
        sender.sendMessage(Component.text("=== Rain From Sky ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("Falling mobs: " + mode.getRainSpawner().getFallingMobCount()
                + " / " + cfg.getRainMaxFallingMobs(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Rate: " + cfg.getRainSpawnsPerWindow() + " per "
                + cfg.getRainWindowSeconds() + "s", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Drop height: " + cfg.getRainDropHeight()
                + " | Target: " + cfg.getRainTargetMode()
                + " | Scatter: " + cfg.getRainScatterRadius(), NamedTextColor.GRAY));
        return true;
    }

    private boolean handleFlora(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null || mode.getWorldEffects() == null) {
            sender.sendMessage(Component.text("World effects unavailable.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: flora <small|medium|large>", NamedTextColor.YELLOW));
            return true;
        }
        String tier = args[1].toLowerCase();
        if (!tier.equals("small") && !tier.equals("medium") && !tier.equals("large")) {
            sender.sendMessage(Component.text("Tier must be small, medium, or large.", NamedTextColor.RED));
            return true;
        }
        boolean fired = mode.getWorldEffects().forceBloom(tier);
        if (fired) {
            sender.sendMessage(Component.text("Triggered " + tier + " flora bloom.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Could not fire bloom (mode not active or no players).",
                    NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: toggleexempt <player>", NamedTextColor.YELLOW));
            return true;
        }
        if (mode.isExempt(target)) {
            mode.removeExempt(target);
            sender.sendMessage(Component.text(target.getName() + " no longer exempt.", NamedTextColor.YELLOW));
        } else {
            mode.addExempt(target);
            sender.sendMessage(Component.text(target.getName() + " is now exempt.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("=== Fluffy Attacks ===", NamedTextColor.LIGHT_PURPLE));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---",
                    NamedTextColor.LIGHT_PURPLE));
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                AbstractAttack atk = mode.getAttackRegistry().get(1, type, id);
                String info = atk != null
                        ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                        atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                        atk.getConfig().getDurationTicks(), atk.getConfig().isEnabled() ? "ON" : "OFF")
                        : "";
                sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        FluffyMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        mode.getFluffyConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Fluffy configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.fluffy.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterStartsWith(args[0], "status", "debug", "test", "list", "clearattacks",
                    "spawninterval", "rainspawn", "rainstatus", "flora", "toggleexempt", "reload",
                    "aispawn", "aitrigger", "ailist");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            FluffyMode mode = getMode();
            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
                return filterStartsWith(args[1], ids);
            }
            if ("flora".equals(sub)) return filterStartsWith(args[1], "small", "medium", "large");
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
            if ("aispawn".equals(sub) || "aitrigger".equals(sub)) {
                return filterStartsWith(args[1], AI_TYPES);
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("aitrigger".equals(sub)) {
                return filterStartsWith(args[2], validSpecialsFor(args[1].toLowerCase()));
            }
        }
        return Collections.emptyList();
    }

    // ========================
    // Helpers
    // ========================

    private FluffyMode getMode() {
        var mode = plugin.getModeManager().getMode("fluffy");
        return mode instanceof FluffyMode fm ? fm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Fluffy Commands ===", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("test [id] — List attacks or spawn one on yourself",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("list — List all attack IDs with stats", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("rainspawn — Force one rain-from-sky drop",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("rainstatus — Show rain config + falling mob count",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("flora <small|medium|large> — Trigger a flora bloom",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("aispawn <type> [variant] — Spawn a managed AI mob",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("aitrigger <type> <special> — Force-fire a special on nearest mob",
                NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("ailist — List per-type specials with cooldowns/chances",
                NamedTextColor.LIGHT_PURPLE));
    }

    private void sendIdList(CommandSender sender, List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(", ");
            if ((i + 1) % 5 == 0) {
                sender.sendMessage(Component.text(sb.toString(), NamedTextColor.GRAY));
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) sender.sendMessage(Component.text(sb.toString(), NamedTextColor.GRAY));
    }

    private List<String> filterStartsWith(String input, String... options) {
        return filterStartsWith(input, Arrays.asList(options));
    }

    private List<String> filterStartsWith(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
