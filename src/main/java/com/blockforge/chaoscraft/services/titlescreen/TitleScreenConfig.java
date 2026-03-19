package com.blockforge.chaoscraft.services.titlescreen;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Parses and holds all title-screen configuration:
 * teleport location, player state flags, scripts, and left-click interactions.
 */
public class TitleScreenConfig {

    private Location teleportLocation;
    private boolean invisible;
    private boolean chatDisabled;
    private boolean movementDisabled;
    private boolean zeroGravity;
    private boolean untargetable;
    private boolean commandsDisabled;

    private final Map<String, List<ScriptAction>> scripts = new HashMap<>();
    private final List<LeftClickInteraction> leftClickInteractions = new ArrayList<>();

    public TitleScreenConfig(YamlConfiguration yaml) {
        loadFromConfig(yaml);
    }

    // ---- Loading ----

    private void loadFromConfig(YamlConfiguration yaml) {
        if (yaml == null) {
            Bukkit.getLogger().warning("[ChaosCraft] titlescreen.yml not loaded — using defaults. " +
                    "Check that the file exists in plugins/ChaosCraft/titlescreen.yml");
            setDefaults();
            return;
        }
        ConfigurationSection ts = yaml.getConfigurationSection("TitleScreen");
        if (ts == null) {
            Bukkit.getLogger().warning("[ChaosCraft] titlescreen.yml missing 'TitleScreen' section — using defaults.");
            setDefaults();
            return;
        }

        // Teleport location
        if (ts.contains("TeleportLocation")) {
            ConfigurationSection loc = ts.getConfigurationSection("TeleportLocation");
            if (loc != null) {
                String worldName = loc.getString("world", "world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = loc.getDouble("x", 0.0);
                    double y = loc.getDouble("y", 100.0);
                    double z = loc.getDouble("z", 0.0);
                    float yaw = (float) loc.getDouble("yaw", 0.0);
                    float pitch = (float) loc.getDouble("pitch", 0.0);
                    this.teleportLocation = new Location(world, x, y, z, yaw, pitch);
                }
            }
        }

        this.invisible = ts.getBoolean("Invisible", true);
        this.chatDisabled = ts.getBoolean("ChatDisabled", true);
        this.movementDisabled = ts.getBoolean("MovementDisabled", true);
        this.zeroGravity = ts.getBoolean("ZeroGravity", true);
        this.untargetable = ts.getBoolean("Untargetable", false);
        this.commandsDisabled = ts.getBoolean("CommandsDisabled", false);

        loadScripts(ts);
        loadLeftClickInteractions(ts);
    }

    private void setDefaults() {
        invisible = true;
        chatDisabled = true;
        movementDisabled = true;
        zeroGravity = true;
        untargetable = false;
        commandsDisabled = false;
    }

    private void loadScripts(ConfigurationSection ts) {
        ConfigurationSection commands = ts.getConfigurationSection("Commands");
        if (commands == null) return;

        for (String scriptName : commands.getKeys(false)) {
            List<?> rawLines = commands.getList(scriptName);
            if (rawLines == null) continue;

            List<ScriptAction> actions = new ArrayList<>();
            for (Object obj : rawLines) {
                String line = obj.toString().trim();
                ScriptAction action = parseScriptLine(line);
                if (action != null) {
                    actions.add(action);
                }
            }

            if (!actions.isEmpty()) {
                scripts.put(scriptName, actions);
            }
        }
    }

    private void loadLeftClickInteractions(ConfigurationSection ts) {
        ConfigurationSection lc = ts.getConfigurationSection("LeftClick");
        if (lc == null) return;

        for (String key : lc.getKeys(false)) {
            ConfigurationSection interaction = lc.getConfigurationSection(key);
            if (interaction == null) continue;

            Map<String, String> conditions = new HashMap<>();
            ConfigurationSection cond = interaction.getConfigurationSection("Conditions");
            if (cond != null) {
                for (String placeholder : cond.getKeys(false)) {
                    conditions.put(placeholder, cond.getString(placeholder));
                }
            }

            List<?> rawActions = interaction.getList("Actions");
            List<ScriptAction> actions = new ArrayList<>();
            if (rawActions != null) {
                for (Object obj : rawActions) {
                    String line = obj.toString().trim();
                    ScriptAction action = parseScriptLine(line);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }

            if (!actions.isEmpty()) {
                leftClickInteractions.add(new LeftClickInteraction(conditions, actions));
            }
        }
    }

    // ---- Script parsing ----

    private ScriptAction parseScriptLine(String line) {
        if (line.startsWith("execute console command ")) {
            String command = line.substring("execute console command ".length()).trim();
            if (command.startsWith("\"") && command.endsWith("\"")) {
                command = command.substring(1, command.length() - 1);
            }
            return new ScriptAction(ScriptActionType.EXECUTE_COMMAND, command);
        }

        if (line.startsWith("wait ")) {
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int ticks = Integer.parseInt(parts[1]);
                    return new ScriptAction(ScriptActionType.WAIT, String.valueOf(ticks));
                } catch (NumberFormatException e) {
                    // Placeholder-based wait value
                    return new ScriptAction(ScriptActionType.WAIT, parts[1]);
                }
            }
        }

        if (line.trim().equals("nextpart")) {
            return new ScriptAction(ScriptActionType.NEXTPART, "");
        }

        if (line.trim().equals("exittitlescreen")) {
            return new ScriptAction(ScriptActionType.EXITTITLESCREEN, "");
        }

        return null;
    }

    // ---- Accessors ----

    public Location getTeleportLocation() { return teleportLocation; }
    public boolean hasTeleportLocation() { return teleportLocation != null; }
    public boolean isInvisible() { return invisible; }
    public boolean isChatDisabled() { return chatDisabled; }
    public boolean isMovementDisabled() { return movementDisabled; }
    public boolean isZeroGravity() { return zeroGravity; }
    public boolean isUntargetable() { return untargetable; }
    public boolean isCommandsDisabled() { return commandsDisabled; }

    public List<ScriptAction> getScript(String name) {
        return scripts.getOrDefault(name, Collections.emptyList());
    }

    public Set<String> getScriptNames() {
        return scripts.keySet();
    }

    public List<LeftClickInteraction> getLeftClickInteractions() {
        return leftClickInteractions;
    }

    // ---- Inner types ----

    public enum ScriptActionType {
        EXECUTE_COMMAND,
        WAIT,
        NEXTPART,
        EXITTITLESCREEN
    }

    public record ScriptAction(ScriptActionType type, String value) {
        public ScriptActionType getType() { return type; }
        public String getValue() { return value; }
    }

    public record LeftClickInteraction(Map<String, String> conditions, List<ScriptAction> actions) {
        public Map<String, String> getConditions() { return conditions; }
        public List<ScriptAction> getActions() { return actions; }
    }
}
