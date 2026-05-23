package com.blockforge.chaoscraft.modes.chain;

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
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Chain mode admin commands. Dispatched via {@code /cc modes chain <sub>}.
 *
 * Subcommands:
 *  - status                    — runtime info
 *  - debug                     — toggle plugin-wide debug
 *  - test [id]                 — list attack ids OR force-spawn one on yourself
 *  - list                      — list every attack with damage/radius/duration
 *  - clearattacks              — remove all currently active attacks
 *  - spawninterval <ticks>     — change attack spawn interval at runtime
 *  - toggleexempt [player]     — toggle attack exemption for a player
 *  - reload                    — reload chain config + attack configs
 *  - me [id]                   — list/spawn ModelEngine attacks specifically
 *  - category <name> (cat)     — list attacks belonging to a named category
 *  - stats                     — extended stats (totals, top-5 damage/radius, entities)
 *  - damagepreview <id> (preview) — full config readout without firing
 *  - designtype <id>           — show skill-based dodge label
 *  - spawncat <category>       — random attack from a category, spawned on you
 */
public class ChainCommand implements CommandExecutor, TabCompleter {

    // ========================
    // Category constants — hardcoded for the 175-attack registry.
    // BlockDisplay categories (75 ids, 10 each except signature which has 15)
    // ========================
    private static final List<String> CAT_PENDULUMS = List.of(
            "wrecking_ball_pendulum", "chain_flail_three_heads", "pendulum_sweep_wall",
            "overhead_chain_lariat", "double_pendulum", "chain_whip_crack",
            "tri_pendulum_fan", "chain_skip_bouncer", "inverted_pendulum",
            "wrecking_ball_orbit");

    private static final List<String> CAT_FALLING = List.of(
            "chain_weight_drop", "anvil_barrage", "chain_curtain", "iron_cage_drop",
            "gravity_hammer", "chain_anchor_fall", "iron_spike_shower",
            "wrecking_ball_crater", "chain_net_descent", "chain_plumb_array");

    private static final List<String> CAT_ERUPTIONS = List.of(
            "wrecking_ball_crater_array", "iron_cage_grid", "chain_eruption",
            "iron_spike_ring", "chain_geyser", "iron_hand", "chain_forest",
            "binding_ring", "iron_cross_eruption", "chain_wellspring");

    private static final List<String> CAT_ORBITING = List.of(
            "magnetic_pillar", "anchor_burst", "iron_gyroscope", "chain_maelstrom",
            "chain_pinwheel", "orbital_wrecking_balls", "iron_halo_descent",
            "chain_helix_tower", "spinning_chain_wall", "iron_tornado");

    private static final List<String> CAT_PROJECTILE_BD = List.of(
            "chain_carousel", "iron_sphere_lattice", "chain_grinder_wheels",
            "chain_lasso", "iron_javelin", "hook_and_chain", "chain_comet",
            "iron_chakram", "chain_drill", "chain_serpent");

    private static final List<String> CAT_CAGES = List.of(
            "iron_bola", "chain_whiplash", "iron_bolt", "wrecking_ball_roll",
            "chain_spear", "netherite_cage", "chain_dome", "iron_cube_crush",
            "binding_spiral", "chain_mandala");

    private static final List<String> CAT_SIGNATURE_BD = List.of(
            "chain_coffin", "iron_coffin_array", "chain_clock", "iron_centipede",
            "wrecking_ball_constellation", "chain_scorpion", "chain_pulley_system",
            "iron_grinder", "iron_dragon", "iron_chimney",
            "chain_tsunami", "iron_golem_fist", "chain_web_trap",
            "iron_giant", "iron_cathedral");

    // ========================
    // Environmental categories (75 ids, 15 each)
    // ========================
    private static final List<String> CAT_AMBIENT = List.of(
            "hanging_chain_clusters", "swaying_lanterns", "dangling_iron_weights",
            "chain_loop_wall_mounts", "iron_ingot_field", "chain_hook_wall",
            "anchor_display", "swinging_cage_door", "iron_gear_display",
            "chain_spiral_ambient", "iron_shackle_ring", "ambient_wrecking_ball",
            "chain_curtain_background", "netherite_ingot_cluster", "iron_axe_weapon_rack");

    private static final List<String> CAT_ATMOSPHERIC = List.of(
            "iron_dust_mist", "rising_smoke_columns", "metal_spark_drifts",
            "chain_energy_wisps", "rust_flake_fall", "electric_arc_flickers",
            "ground_iron_fog", "chain_ground_traces", "distant_impact_rumbles",
            "metallic_rain", "shadow_tendril_snakes", "iron_aurora",
            "shockwave_ground_rings", "forge_ember_glow", "chain_fragment_shower");

    private static final List<String> CAT_FALLING_DRIFTING = List.of(
            "single_chain_link_fall_loop", "iron_shaving_drift", "falling_gear_piece",
            "chain_rope_unravel", "pendulum_shadow", "slow_weight_bob",
            "falling_anvil_warning_ghost", "chain_segment_shower", "metal_dust_plume",
            "chain_drape_descent", "orbit_trail_ghost", "iron_leaf_drift",
            "impact_crater_persist", "chain_spool_unwind", "slow_chain_drape_sway");

    private static final List<String> CAT_STRUCTURAL = List.of(
            "chain_pillar_wrap", "iron_arch_span", "barred_window_effect",
            "chain_support_cables", "iron_floor_grating", "forge_glow_vents",
            "chain_net_overhead", "wrecking_ball_dent_marks", "iron_machinery_ambient",
            "chain_tether_visual", "iron_scaffolding_wall", "anchor_chain_floor_ring",
            "chain_wall_section", "iron_spike_floor_strip", "chain_lock_mechanism");

    private static final List<String> CAT_COMBAT_FEEDBACK = List.of(
            "attack_telegraph_floor_indicator", "impact_crater_ring", "chain_clash_sparks",
            "wrecking_ball_swing_trail", "pendulum_air_displacement", "chain_snap_burst",
            "iron_smash_ground_crack", "falling_weight_warning_pulse", "near_miss_sparks",
            "chain_tangle_visual", "swing_reversal_shockwave", "metal_impact_echo",
            "iron_explosion_debris", "attack_sequence_finale", "chain_mode_ambient_heartbeat");

    // ========================
    // ModelEngine categories (25 ids)
    // ========================
    private static final List<String> CAT_ME_GROUND = List.of(
            "anchor_eruption", "chain_spike_array", "cursed_shackle_burst",
            "spectral_chain_forest", "iron_maiden_ground_trap");

    private static final List<String> CAT_ME_AOE = List.of(
            "chain_shockwave_ring", "void_chain_nova", "spectral_binding_shockwave",
            "iron_crown_burst", "phantom_chain_collapse");

    private static final List<String> CAT_ME_PROJ = List.of(
            "flail_projectile", "cursed_chain_laser", "ghost_chain_volley",
            "chain_comet_me", "spectral_chain_tendril");

    private static final List<String> CAT_ME_SUMMON = List.of(
            "chain_colossus_hands", "cursed_prison_orbital", "spectral_warden_orbital",
            "iron_wheel_orbital", "void_chain_anchor_ring", "chain_orrery",
            "ghost_ship_anchor_array", "cursed_pendulum_array", "spectral_chain_gallery",
            "the_chain");

    /** Lookup table mapping the user-facing category name to its hardcoded id list. */
    private static final Map<String, List<String>> CATEGORIES = Map.ofEntries(
            Map.entry("pendulums", CAT_PENDULUMS),
            Map.entry("falling", CAT_FALLING),
            Map.entry("eruptions", CAT_ERUPTIONS),
            Map.entry("orbiting", CAT_ORBITING),
            Map.entry("projectile-bd", CAT_PROJECTILE_BD),
            Map.entry("cages", CAT_CAGES),
            Map.entry("signature-bd", CAT_SIGNATURE_BD),
            Map.entry("ambient", CAT_AMBIENT),
            Map.entry("atmospheric", CAT_ATMOSPHERIC),
            Map.entry("falling-drifting", CAT_FALLING_DRIFTING),
            Map.entry("structural", CAT_STRUCTURAL),
            Map.entry("combat-feedback", CAT_COMBAT_FEEDBACK),
            Map.entry("ground-eruption-me", CAT_ME_GROUND),
            Map.entry("aoe-burst-me", CAT_ME_AOE),
            Map.entry("projectile-me", CAT_ME_PROJ),
            Map.entry("summoning-me", CAT_ME_SUMMON));

    private final ChaosCraftPlugin plugin;

    public ChainCommand(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.chain.admin") && !sender.hasPermission("chaoscraft.admin")) {
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
            case "toggleexempt" -> handleToggleExempt(sender, args);
            case "reload" -> handleReload(sender);
            case "me" -> handleMe(sender, args);
            case "category", "cat" -> handleCategory(sender, args);
            case "stats" -> handleStats(sender);
            case "damagepreview", "preview" -> handleDamagePreview(sender, args);
            case "designtype" -> handleDesignType(sender, args);
            case "spawncat" -> handleSpawnCat(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }
        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive() && modeManager.getActiveMode() instanceof ChainMode;

        sender.sendMessage(Component.text("=== Chain Status ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Active: " + (active ? "YES" : "NO"),
                active ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Registered attacks: " + mode.getAttackRegistry().size(),
                NamedTextColor.AQUA));

        if (active) {
            var scheduler = mode.getAttackScheduler();
            sender.sendMessage(Component.text("Active attacks: " + scheduler.getActiveAttackCount(),
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Tick counter: " + mode.getTickCounter(), NamedTextColor.GRAY));
            var world = mode.getChainWorld();
            if (world != null) {
                sender.sendMessage(Component.text("World: " + world.getName()
                        + " (" + world.getPlayers().size() + " players)", NamedTextColor.AQUA));
            }
        }

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int meCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        sender.sendMessage(Component.text("Block Display: " + bdCount + " | Environmental: " + envCount
                + " | ModelEngine: " + meCount, NamedTextColor.AQUA));

        ChainConfig cfg = mode.getChainConfig();
        sender.sendMessage(Component.text("Duration: " + cfg.getDurationSeconds() + "s | Spawn interval: "
                + cfg.getBaseSpawnInterval() + " ticks", NamedTextColor.GRAY));
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
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Chain mode not registered.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
                List<String> ids = mode.getAttackRegistry().getIds(1, type);
                if (!ids.isEmpty()) {
                    sender.sendMessage(Component.text(type.name() + " (" + ids.size() + "):",
                            NamedTextColor.AQUA));
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
        ChainMode mode = getMode();
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
        ChainMode mode = getMode();
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

    private boolean handleToggleExempt(CommandSender sender, String[] args) {
        ChainMode mode = getMode();
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
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("=== Chain Attacks ===", NamedTextColor.AQUA));
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            List<String> ids = mode.getAttackRegistry().getIds(1, type);
            if (ids.isEmpty()) continue;
            sender.sendMessage(Component.text("--- " + type.name() + " (" + ids.size() + ") ---",
                    NamedTextColor.AQUA));
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
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        mode.getChainConfig().load();
        mode.getAttackRegistry().reloadConfigs();
        sender.sendMessage(Component.text("Chain configs reloaded.", NamedTextColor.GREEN));
        return true;
    }

    // ========================
    // New: ModelEngine list/spawn
    // ========================
    private boolean handleMe(CommandSender sender, String[] args) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        List<String> meIds = mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE);

        if (args.length < 2) {
            sender.sendMessage(Component.text("=== ModelEngine Attacks (" + meIds.size() + ") ===",
                    NamedTextColor.AQUA));
            sendIdList(sender, meIds);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player to spawn.", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        AbstractAttack attack = mode.getAttackRegistry().get(1, AttackType.MODEL_ENGINE, id);
        if (attack == null) {
            sender.sendMessage(Component.text("Unknown ME attack: " + id, NamedTextColor.RED));
            return true;
        }
        mode.getAttackScheduler().forceSpawn(attack, player);
        sender.sendMessage(Component.text("Spawned ME attack: " + id, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Impact-Damage: " + attack.getConfig().getImpactDamage()
                + " | Impact-Radius: " + attack.getConfig().getImpactRadius()
                + " | Duration: " + attack.getConfig().getDurationTicks() + "t", NamedTextColor.GRAY));
        return true;
    }

    // ========================
    // New: List attacks by category
    // ========================
    private boolean handleCategory(CommandSender sender, String[] args) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: category <name>", NamedTextColor.YELLOW));
            sendCategoryList(sender);
            return true;
        }
        String catName = args[1].toLowerCase();
        List<String> ids = CATEGORIES.get(catName);
        if (ids == null) {
            sender.sendMessage(Component.text("Unknown category: " + catName, NamedTextColor.RED));
            sendCategoryList(sender);
            return true;
        }
        sender.sendMessage(Component.text("=== Category: " + catName + " (" + ids.size() + ") ===",
                NamedTextColor.AQUA));
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            AbstractAttack atk = findAnyAttack(mode, id);
            String info = atk != null
                    ? String.format(" (dmg=%.0f, r=%.1f, dur=%d, %s)",
                    atk.getConfig().getDamage(), atk.getConfig().getDamageRadius(),
                    atk.getConfig().getDurationTicks(),
                    atk.getConfig().isEnabled() ? "ON" : "OFF")
                    : " (not registered?)";
            sender.sendMessage(Component.text((i + 1) + ". " + id + info, NamedTextColor.GRAY));
        }
        return true;
    }

    private void sendCategoryList(CommandSender sender) {
        sender.sendMessage(Component.text("Valid categories:", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(" BlockDisplay: pendulums, falling, eruptions, orbiting, "
                + "projectile-bd, cages, signature-bd", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" Environmental: ambient, atmospheric, falling-drifting, "
                + "structural, combat-feedback", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" ModelEngine: ground-eruption-me, aoe-burst-me, "
                + "projectile-me, summoning-me", NamedTextColor.GRAY));
    }

    // ========================
    // New: Extended stats
    // ========================
    private boolean handleStats(CommandSender sender) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        var modeManager = plugin.getModeManager();
        boolean active = modeManager.isAnyModeActive() && modeManager.getActiveMode() instanceof ChainMode;

        int bdCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.BLOCK_DISPLAY).size();
        int envCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.ENVIRONMENTAL).size();
        int meCount = mode.getAttackRegistry().getByPhaseAndType(1, AttackType.MODEL_ENGINE).size();
        int total = bdCount + envCount + meCount;

        sender.sendMessage(Component.text("=== Chain Extended Stats ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Total registered: " + total, NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  BlockDisplay:  " + bdCount, NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Environmental: " + envCount, NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  ModelEngine:   " + meCount, NamedTextColor.GRAY));

        if (active) {
            sender.sendMessage(Component.text("Active attack instances: "
                    + mode.getAttackScheduler().getActiveAttackCount(), NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Mode inactive.", NamedTextColor.GRAY));
        }

        // Top 5 by damage (use max of damage and impactDamage)
        List<AbstractAttack> all = new ArrayList<>(mode.getAttackRegistry().getAll());
        List<AbstractAttack> byDamage = all.stream()
                .sorted(Comparator.comparingDouble(
                        (AbstractAttack a) -> Math.max(a.getConfig().getDamage(),
                                a.getConfig().getImpactDamage())).reversed())
                .limit(5)
                .collect(Collectors.toList());
        sender.sendMessage(Component.text("Top 5 by damage:", NamedTextColor.AQUA));
        for (AbstractAttack a : byDamage) {
            double effDmg = Math.max(a.getConfig().getDamage(), a.getConfig().getImpactDamage());
            sender.sendMessage(Component.text(String.format("  %s — %.1f (%s)",
                    a.getConfig().getAttackId(), effDmg, a.getConfig().getType().name()),
                    NamedTextColor.GRAY));
        }

        // Top 5 by radius (use max of damageRadius and impactRadius)
        List<AbstractAttack> byRadius = all.stream()
                .sorted(Comparator.comparingDouble(
                        (AbstractAttack a) -> Math.max(a.getConfig().getDamageRadius(),
                                a.getConfig().getImpactRadius())).reversed())
                .limit(5)
                .collect(Collectors.toList());
        sender.sendMessage(Component.text("Top 5 by radius:", NamedTextColor.AQUA));
        for (AbstractAttack a : byRadius) {
            double effR = Math.max(a.getConfig().getDamageRadius(), a.getConfig().getImpactRadius());
            sender.sendMessage(Component.text(String.format("  %s — %.1f (%s)",
                    a.getConfig().getAttackId(), effR, a.getConfig().getType().name()),
                    NamedTextColor.GRAY));
        }

        // Display-entity count in chain world
        var world = mode.getChainWorld();
        if (world != null) {
            long displays = world.getEntities().stream()
                    .filter(e -> e instanceof Display).count();
            sender.sendMessage(Component.text("World entities (Display): " + displays
                    + " in " + world.getName(), NamedTextColor.AQUA));
        }

        // Mode duration / remaining
        ChainConfig cfg = mode.getChainConfig();
        int durationTicks = cfg.getDurationSeconds() * 20;
        int elapsed = mode.getTickCounter();
        int remaining = Math.max(0, durationTicks - elapsed);
        sender.sendMessage(Component.text(String.format(
                "Duration: %ds | Elapsed: %.1fs | Remaining: %.1fs",
                cfg.getDurationSeconds(), elapsed / 20.0, remaining / 20.0),
                NamedTextColor.GRAY));
        return true;
    }

    // ========================
    // New: Full config preview without firing
    // ========================
    private boolean handleDamagePreview(CommandSender sender, String[] args) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: damagepreview <id>", NamedTextColor.YELLOW));
            return true;
        }
        String id = args[1].toLowerCase();
        AbstractAttack atk = findAnyAttack(mode, id);
        if (atk == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            return true;
        }
        var c = atk.getConfig();
        sender.sendMessage(Component.text("Attack: " + c.getAttackId(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  Design Type: "
                + (c.getDesignType() == null || c.getDesignType().isEmpty()
                ? "(not set)" : c.getDesignType()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Type: " + c.getType().name(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format("  Damage: %.1f / Impact-Damage: %.1f",
                c.getDamage(), c.getImpactDamage()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format("  Radius: %.1f / Impact-Radius: %.1f",
                c.getDamageRadius(), c.getImpactRadius()), NamedTextColor.GRAY));
        String tickInfo = c.isDamageOnImpactOnly()
                ? "N/A (impact-only)"
                : c.getTicksBetweenDamage() + "t";
        sender.sendMessage(Component.text("  Ticks Between Damage: " + tickInfo, NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Duration: " + c.getDurationTicks()
                + " / Cooldown: " + c.getCooldownTicks(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Damage Delay: " + c.getDamageDelayTicks() + "t",
                NamedTextColor.GRAY));
        if (c.getType() == AttackType.MODEL_ENGINE) {
            sender.sendMessage(Component.text("  ModelEngine Scale: " + c.getModelengineScale(),
                    NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("  Chance: " + c.getChance(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Tracks Player: " + c.tracksPlayer()
                + " / Follow-AI: " + c.isFollowAiEnabled(), NamedTextColor.GRAY));
        if (c.isFollowAiEnabled()) {
            sender.sendMessage(Component.text("  Follow-AI Speed: " + c.getFollowAiWalkSpeed()
                    + " blocks/tick", NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("  Enabled: " + c.isEnabled(),
                c.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }

    // ========================
    // New: Design-type label only
    // ========================
    private boolean handleDesignType(CommandSender sender, String[] args) {
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: designtype <id>", NamedTextColor.YELLOW));
            return true;
        }
        String id = args[1].toLowerCase();
        AbstractAttack atk = findAnyAttack(mode, id);
        if (atk == null) {
            sender.sendMessage(Component.text("Unknown attack: " + id, NamedTextColor.RED));
            return true;
        }
        String label = atk.getConfig().getDesignType();
        if (label == null || label.isEmpty()) label = "(not set)";
        sender.sendMessage(Component.text(id + ": " + label, NamedTextColor.AQUA));
        return true;
    }

    // ========================
    // New: Random attack from a category
    // ========================
    private boolean handleSpawnCat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }
        ChainMode mode = getMode();
        if (mode == null) {
            sender.sendMessage(Component.text("Not registered.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: spawncat <category>", NamedTextColor.YELLOW));
            sendCategoryList(sender);
            return true;
        }
        String catName = args[1].toLowerCase();
        List<String> ids = CATEGORIES.get(catName);
        if (ids == null || ids.isEmpty()) {
            sender.sendMessage(Component.text("Unknown category: " + catName, NamedTextColor.RED));
            sendCategoryList(sender);
            return true;
        }
        String id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
        AbstractAttack atk = findAnyAttack(mode, id);
        if (atk == null) {
            sender.sendMessage(Component.text("Picked id not in registry: " + id, NamedTextColor.RED));
            return true;
        }
        mode.getAttackScheduler().forceSpawn(atk, player);
        sender.sendMessage(Component.text("Spawned [" + catName + "] " + id, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  Damage: " + atk.getConfig().getDamage()
                + " | Radius: " + atk.getConfig().getDamageRadius()
                + " | Duration: " + atk.getConfig().getDurationTicks() + "t", NamedTextColor.GRAY));
        return true;
    }

    /**
     * Look up an attack by id across all three types — saves a few lines vs the
     * three-arm pattern used by handleTest.
     */
    private AbstractAttack findAnyAttack(ChainMode mode, String id) {
        for (AttackType type : List.of(AttackType.BLOCK_DISPLAY, AttackType.ENVIRONMENTAL, AttackType.MODEL_ENGINE)) {
            AbstractAttack a = mode.getAttackRegistry().get(1, type, id);
            if (a != null) return a;
        }
        return null;
    }

    // ========================
    // Tab completion
    // ========================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chaoscraft.chain.admin") && !sender.hasPermission("chaoscraft.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filterStartsWith(args[0],
                    "status", "debug", "test", "list", "clearattacks",
                    "spawninterval", "toggleexempt", "reload",
                    "me", "category", "cat", "stats",
                    "damagepreview", "preview", "designtype", "spawncat");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            ChainMode mode = getMode();
            if ("test".equals(sub) && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
                return filterStartsWith(args[1], ids);
            }
            if ("me".equals(sub) && mode != null) {
                return filterStartsWith(args[1], mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
            }
            if ("category".equals(sub) || "cat".equals(sub) || "spawncat".equals(sub)) {
                return filterStartsWith(args[1], new ArrayList<>(CATEGORIES.keySet()));
            }
            if (("damagepreview".equals(sub) || "preview".equals(sub) || "designtype".equals(sub))
                    && mode != null) {
                List<String> ids = new ArrayList<>();
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.BLOCK_DISPLAY));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.ENVIRONMENTAL));
                ids.addAll(mode.getAttackRegistry().getIds(1, AttackType.MODEL_ENGINE));
                return filterStartsWith(args[1], ids);
            }
            if ("toggleexempt".equals(sub)) return null;
            if ("spawninterval".equals(sub)) return List.of("20", "40", "60", "100");
        }
        return Collections.emptyList();
    }

    // ========================
    // Helpers
    // ========================

    private ChainMode getMode() {
        var mode = plugin.getModeManager().getMode("chain");
        return mode instanceof ChainMode cm ? cm : null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Chain Commands ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("status — Mode status", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("debug — Toggle debug mode", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("test [id] — List attacks or spawn one on yourself",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("list — List all attack IDs with stats", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("clearattacks — Clear active attacks", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("spawninterval <ticks> — Set spawn interval",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("toggleexempt [player] — Toggle exempt", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("reload — Reload configs", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("me [id] — List ME attacks or spawn one",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("category <name> (cat) — List attacks in a category",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("stats — Extended stats: totals, top-5 damage/radius, entities",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("damagepreview <id> (preview) — Full config readout",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("designtype <id> — Show skill-based dodge label",
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text("spawncat <category> — Spawn random attack from category",
                NamedTextColor.AQUA));
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
