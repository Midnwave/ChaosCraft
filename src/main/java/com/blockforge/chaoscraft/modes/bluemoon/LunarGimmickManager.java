package com.blockforge.chaoscraft.modes.bluemoon;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages all 52 Blue Moon gimmick mechanics.
 * Each gimmick is a toggleable mechanic loaded from bluemoon.yml under "gimmicks:".
 * The manager ticks enabled gimmicks each server tick from BlueMoonMode.
 *
 * 10 gimmicks are fully implemented; the remaining 42 are stubbed with TODO.
 */
public class LunarGimmickManager {

    /**
     * All 52 gimmick identifiers. Order matches the design document categories:
     * Moon Phase (10), Player Tracking (6), Timed Events (4), Terrain/Environment (6),
     * Gravity/Physics (3), Boss Interaction (4), Visual/Audio (5), Difficulty Scaling (5),
     * Special Triggers (5), Meta/Misc (4).
     */
    private static final List<String> ALL_GIMMICKS = List.of(
            // Moon Phase (10)
            "moon_phase_wheel", "moonrise_moonset", "waxing_waning_power", "supermoon_event",
            "orbital_decay", "lunar_link", "moon_fragments", "phase_lock", "reflection_pool", "selenography",
            // Player Tracking (6)
            "moonlight_exposure", "lunar_corruption_score", "moon_shadow", "moon_dust", "the_hunt", "pack_instinct",
            // Timed Events (4)
            "blue_hour", "celestial_alignment", "tidal_breathing", "lunar_eclipse_events",
            // Terrain/Environment (6)
            "frost_creep", "crater_impact", "frozen_world", "lunar_bloom", "moonstone_veins", "night_eternal",
            // Gravity/Physics (3)
            "tidal_gravity_wells", "gravity_pulse_heartbeat", "weight_of_the_moon",
            // Boss Interaction (4)
            "boss_aura_pulse", "lunar_tether", "moonfall_countdown", "boss_phase_gimmick_shift",
            // Visual/Audio (5)
            "silver_sky_filter", "howl_echo_ambient", "star_trail_overlay", "frost_vignette", "moonbeam_spotlight",
            // Difficulty Scaling (5)
            "creeping_dread", "escalation_curve", "mercy_window", "desperation_spike", "endurance_test",
            // Special Triggers (4)
            "blood_moon_flash", "eclipse_blackout", "meteor_shower_burst", "lunar_convergence",
            // Meta/Misc (4)
            "shared_fate", "last_stand_aura", "moonlit_revival", "final_howl"
    );

    private final ChaosCraftPlugin plugin;
    private final Map<String, Boolean> gimmickStates = new LinkedHashMap<>();

    // ── State for implemented gimmicks ─────────────────────────────────

    // orbital_decay
    private double orbitalDecayOffset = 0.0;

    // blue_hour
    private boolean blueHourActive = false;
    private int blueHourTimer = 0;
    private int blueHourCooldown = 0;
    private static final int BLUE_HOUR_INTERVAL = 3600;  // 3 min
    private static final int BLUE_HOUR_DURATION = 300;    // 15 sec

    // frost_creep
    private final List<Location> frostCreepLocations = new ArrayList<>();
    private final Map<Location, Double> frostCreepRadii = new HashMap<>();
    private static final int MAX_FROST_CREEP_LOCATIONS = 80;

    // night_eternal
    // (no extra state needed)

    // pack_instinct
    // (computed per-tick, no persistent state)

    // moonlight_exposure
    private final Map<UUID, Integer> exposureScores = new HashMap<>();

    // weight_of_the_moon
    private long weightStartTick = -1;
    private static final long WEIGHT_FULL_DURATION = 18000; // 15 minutes in ticks

    // the_hunt
    private UUID preyPlayerUUID = null;
    private int huntCooldown = 0;
    private static final int HUNT_INTERVAL = 1200; // 60 sec

    // crater_impact
    private final List<Location> craterLocations = new ArrayList<>();
    private static final int MAX_CRATER_LOCATIONS = 50;

    // frozen_world
    private long frozenWorldTick = 0;
    private long frozenWorldMaxTick = 18000; // 15 min default

    // ── Tick counter ───────────────────────────────────────────────────
    private long totalTicks = 0;

    public LunarGimmickManager(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
        // Initialize all gimmicks as enabled by default
        for (String name : ALL_GIMMICKS) {
            gimmickStates.put(name, true);
        }
    }

    // ========================
    // Config
    // ========================

    /**
     * Load gimmick toggles from the BlueMoon config's underlying YAML.
     * Reads from "gimmicks.<name>" with default true for each.
     */
    public void loadConfig(BlueMoonConfig config) {
        // Access the config file directly
        try {
            java.io.File configFile = new java.io.File(
                    plugin.getDataFolder(), "modes/bluemoon/bluemoon.yml");
            if (!configFile.exists()) {
                // Config not yet created; all gimmicks stay at defaults (true)
                return;
            }
            org.bukkit.configuration.file.FileConfiguration yaml =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);

            boolean needsSave = false;
            for (String name : ALL_GIMMICKS) {
                String key = "gimmicks." + name;
                if (yaml.contains(key)) {
                    gimmickStates.put(name, yaml.getBoolean(key, true));
                } else {
                    // Key missing — add default
                    yaml.set(key, true);
                    gimmickStates.put(name, true);
                    needsSave = true;
                }
            }

            // Load frozen_world max tick from timer config
            frozenWorldMaxTick = yaml.getLong("timer.default-seconds", 900) * 20L;

            if (needsSave) {
                try {
                    yaml.save(configFile);
                } catch (java.io.IOException e) {
                    plugin.getLogger().warning("[BlueMoon] Failed to save gimmick defaults: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[BlueMoon] Failed to load gimmick config: " + e.getMessage());
        }
    }

    // ========================
    // Tick
    // ========================

    /**
     * Called every tick from BlueMoonMode. Runs all enabled gimmick logic.
     */
    public void tick(World world) {
        totalTicks++;

        if (isGimmickEnabled("orbital_decay"))       tickOrbitalDecay();
        if (isGimmickEnabled("blue_hour"))            tickBlueHour(world);
        if (isGimmickEnabled("frost_creep"))          tickFrostCreep(world);
        if (isGimmickEnabled("night_eternal"))        tickNightEternal(world);
        if (isGimmickEnabled("pack_instinct"))        tickPackInstinct(world);
        if (isGimmickEnabled("moonlight_exposure"))   tickMoonlightExposure(world);
        if (isGimmickEnabled("weight_of_the_moon"))   tickWeightOfTheMoon(world);
        if (isGimmickEnabled("the_hunt"))             tickTheHunt(world);
        if (isGimmickEnabled("crater_impact"))        tickCraterImpact(world);
        if (isGimmickEnabled("frozen_world"))         tickFrozenWorld(world);

        // ── Stubbed gimmicks (logic not yet implemented) ───────────────

        // TODO: Implement moon_phase_wheel — Cycle through 8 moon phases affecting attack power/speed
        // TODO: Implement moonrise_moonset — Visual moon arc across sky, boss power tied to position
        // TODO: Implement waxing_waning_power — Boss gains/loses strength on a half-cycle
        // TODO: Implement supermoon_event — Rare burst where moon grows huge, all attacks amplified
        // TODO: Implement lunar_link — Players share a portion of damage taken (tethered by moonlight)
        // TODO: Implement moon_fragments — Collectible moon shards that drop from attacks for bonus rewards
        // TODO: Implement phase_lock — Boss locks current moon phase, preventing natural phase cycling
        // TODO: Implement reflection_pool — Mirror zones that duplicate attacks symmetrically
        // TODO: Implement selenography — Map-based moon surface terrain that shifts arena geometry
        // TODO: Implement lunar_corruption_score — Per-player corruption tracker, high corruption = harder attacks
        // TODO: Implement moon_shadow — Player shadows become hazard zones under moonlight
        // TODO: Implement moon_dust — Particle trail follows players, slowing if they stop moving
        // TODO: Implement celestial_alignment — Periodic planet alignment event with stacking attack buffs
        // TODO: Implement tidal_breathing — Arena boundary pulses inward/outward like tides
        // TODO: Implement lunar_eclipse_events — Periodic eclipse phases where boss becomes invulnerable briefly
        // TODO: Implement lunar_bloom — Frost flowers grow at random spots, explode if stepped on
        // TODO: Implement moonstone_veins — Glowing vein lines spread across ground over time
        // TODO: Implement tidal_gravity_wells — Localized gravity pockets that pull/push players
        // TODO: Implement gravity_pulse_heartbeat — Rhythmic gravity pulses synced to boss heartbeat
        // TODO: Implement boss_aura_pulse — Boss emits periodic aura damage in close range
        // TODO: Implement lunar_tether — Moonlight beam connects boss to nearest player, damage over time
        // TODO: Implement moonfall_countdown — Visual countdown to a devastating boss ultimate attack
        // TODO: Implement boss_phase_gimmick_shift — Different gimmicks activate per boss health phase
        // TODO: Implement silver_sky_filter — Sky gradually shifts to silver-white as mode progresses
        // TODO: Implement howl_echo_ambient — Periodic wolf howl echoes that intensify over time
        // TODO: Implement star_trail_overlay — Shooting star particles across the sky throughout the mode
        // TODO: Implement frost_vignette — Screen-edge frost particle effect that thickens over time
        // TODO: Implement moonbeam_spotlight — Random moonbeam spotlights track and illuminate players
        // TODO: Implement creeping_dread — Attack frequency slowly increases over time
        // TODO: Implement escalation_curve — Damage multiplier scales with elapsed time
        // TODO: Implement mercy_window — Brief safe periods after intense attack clusters
        // TODO: Implement desperation_spike — Boss attack rate surges when below 25% health
        // TODO: Implement endurance_test — Survival bonus scales with time survived
        // TODO: Implement blood_moon_flash — Rare red-tinted flash that doubles next attack damage
        // TODO: Implement eclipse_blackout — Temporary full darkness with only attack glow visible
        // TODO: Implement meteor_shower_burst — Burst of 20+ simultaneous starfall attacks
        // TODO: Implement lunar_convergence — All active attacks converge on one player simultaneously
        // TODO: Implement shared_fate — If one player dies, all others take percentage damage
        // TODO: Implement last_stand_aura — Last surviving player gets damage resistance aura
        // TODO: Implement moonlit_revival — Downed players can be revived by standing in moonbeam
        // TODO: Implement final_howl — On boss death, massive howl shockwave as victory fanfare
    }

    // ========================
    // Implemented Gimmicks
    // ========================

    /**
     * 1. ORBITAL DECAY — Boss descends 0.01 blocks/tick over the mode.
     * Tracked as a cumulative offset applied to the boss float height.
     */
    private void tickOrbitalDecay() {
        orbitalDecayOffset += 0.01;
    }

    /**
     * 2. BLUE HOUR — Every 3 min, activate a 15-sec blue hour.
     * During: particles, chat message, damage multiplier flag.
     */
    private void tickBlueHour(World world) {
        if (blueHourActive) {
            blueHourTimer--;
            if (blueHourTimer <= 0) {
                blueHourActive = false;
                blueHourCooldown = BLUE_HOUR_INTERVAL;
                // End message
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.GRAY + "The blue hour fades...");
                }
            } else {
                // Active blue hour particles on all players
                if (blueHourTimer % 4 == 0) {
                    for (Player p : world.getPlayers()) {
                        if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                            DisplayBuilder.dustParticles(p.getLocation().add(0, 2.5, 0),
                                    6, 2.0, 180, 210, 255, 1.0f);
                        }
                    }
                }
            }
        } else {
            blueHourCooldown--;
            if (blueHourCooldown <= 0) {
                blueHourActive = true;
                blueHourTimer = BLUE_HOUR_DURATION;
                // Start message
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "The Blue Hour begins...");
                    DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 1.5f);
                }
            }
        }
    }

    /**
     * 3. FROST CREEP — Track attack impact locations. Every 20 ticks,
     * spawn frost particles at tracked locations expanding outward.
     */
    private void tickFrostCreep(World world) {
        if (totalTicks % 20 != 0) return;

        Iterator<Map.Entry<Location, Double>> it = frostCreepRadii.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, Double> entry = it.next();
            Location loc = entry.getKey();
            double radius = entry.getValue();

            // Expand frost
            radius += 0.5;
            entry.setValue(radius);

            // Max radius before removal
            if (radius > 10.0) {
                it.remove();
                frostCreepLocations.remove(loc);
                continue;
            }

            // Spawn frost particles in expanding ring
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                DisplayBuilder.particleRing(loc.clone().add(0, 0.1, 0), radius, Particle.DUST, (int)(radius * 3),
                        new Particle.DustOptions(Color.fromRGB(150, 230, 255), 0.6f));
            }
        }
    }

    /**
     * 4. NIGHT ETERNAL — Force time to 18000 every 100 ticks.
     */
    private void tickNightEternal(World world) {
        if (totalTicks % 100 == 0) {
            world.setTime(18000L);
        }
    }

    /**
     * 5. PACK INSTINCT — Every 40 ticks, check player clustering.
     * Players within 5 blocks of another get silver particles (good).
     * Solo players get red particles (bad/exposed).
     */
    private void tickPackInstinct(World world) {
        if (totalTicks % 40 != 0) return;

        List<Player> survivors = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                survivors.add(p);
            }
        }

        for (Player p : survivors) {
            boolean nearAlly = false;
            for (Player other : survivors) {
                if (other.equals(p)) continue;
                if (p.getLocation().distance(other.getLocation()) <= 5.0) {
                    nearAlly = true;
                    break;
                }
            }

            if (nearAlly) {
                // Silver dust — pack together (safe indicator)
                DisplayBuilder.dustParticles(p.getLocation().add(0, 2.2, 0),
                        6, 1.0, 200, 200, 220, 0.8f);
            } else {
                // Red dust — alone (danger indicator)
                DisplayBuilder.dustParticles(p.getLocation().add(0, 2.2, 0),
                        6, 1.0, 255, 80, 80, 0.8f);
            }
        }
    }

    /**
     * 6. MOONLIGHT EXPOSURE — Track per-player exposure score (0-100).
     * Under open sky: +1/sec. Under cover: -2/sec.
     */
    private void tickMoonlightExposure(World world) {
        // Run once per second (20 ticks)
        if (totalTicks % 20 != 0) return;

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

            UUID uuid = p.getUniqueId();
            int current = exposureScores.getOrDefault(uuid, 0);

            // Check if player can see sky
            int highestBlockY = world.getHighestBlockYAt(p.getLocation());
            boolean underSky = p.getLocation().getBlockY() >= highestBlockY;

            if (underSky) {
                current = Math.min(100, current + 1);
            } else {
                current = Math.max(0, current - 2);
            }

            exposureScores.put(uuid, current);

            // Visual feedback at high exposure
            if (current >= 80 && totalTicks % 40 == 0) {
                DisplayBuilder.dustParticles(p.getLocation().add(0, 2.5, 0),
                        4, 0.8, 240, 240, 255, 0.6f);
            }
        }
    }

    /**
     * 7. WEIGHT OF THE MOON — Gradually cap jump velocity.
     * Over 15 minutes, reduce max Y velocity from 0.42 to 0.2.
     */
    private void tickWeightOfTheMoon(World world) {
        if (weightStartTick < 0) {
            weightStartTick = totalTicks;
        }

        long elapsed = totalTicks - weightStartTick;
        double progress = Math.min(1.0, (double) elapsed / WEIGHT_FULL_DURATION);
        double maxYVelocity = 0.42 - (progress * 0.22); // 0.42 -> 0.20

        // Cap player Y velocity if positive (jumping)
        if (totalTicks % 2 == 0) {
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

                Vector vel = p.getVelocity();
                if (vel.getY() > maxYVelocity) {
                    p.setVelocity(new Vector(vel.getX(), maxYVelocity, vel.getZ()));
                }
            }
        }
    }

    /**
     * 8. THE HUNT — Every 60 sec, pick random survival player as "prey".
     * Mark with red dust particle aura. Reset previous prey.
     */
    private void tickTheHunt(World world) {
        huntCooldown--;

        // Show prey aura
        if (preyPlayerUUID != null && totalTicks % 5 == 0) {
            Player prey = plugin.getServer().getPlayer(preyPlayerUUID);
            if (prey != null && prey.isOnline() && prey.getWorld().equals(world)) {
                DisplayBuilder.dustParticles(prey.getLocation().add(0, 2.3, 0),
                        8, 1.2, 255, 50, 50, 1.0f);
                // Ring at feet
                DisplayBuilder.particleRing(prey.getLocation().add(0, 0.1, 0), 1.5, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(255, 50, 50), 0.7f));
            }
        }

        if (huntCooldown <= 0) {
            huntCooldown = HUNT_INTERVAL;

            List<Player> survivors = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                    survivors.add(p);
                }
            }

            if (survivors.isEmpty()) {
                preyPlayerUUID = null;
                return;
            }

            // Pick random prey (avoid same player twice in a row if possible)
            Player newPrey;
            if (survivors.size() > 1) {
                List<Player> candidates = new ArrayList<>(survivors);
                candidates.removeIf(p -> p.getUniqueId().equals(preyPlayerUUID));
                newPrey = candidates.get(new Random().nextInt(candidates.size()));
            } else {
                newPrey = survivors.get(0);
            }

            preyPlayerUUID = newPrey.getUniqueId();

            // Announce
            for (Player p : world.getPlayers()) {
                p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The Hunt targets " +
                        ChatColor.WHITE + newPrey.getName() + ChatColor.RED + ChatColor.BOLD + "!");
                DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.7f, 0.8f);
            }
        }
    }

    /**
     * 9. CRATER IMPACT — Track attack expiry locations for frost craters.
     * Managed externally via addCraterLocation; tick just renders them.
     */
    private void tickCraterImpact(World world) {
        if (totalTicks % 10 != 0) return;

        for (Location loc : craterLocations) {
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                DisplayBuilder.dustParticles(loc.clone().add(0, 0.2, 0),
                        4, 2.0, 150, 230, 255, 0.5f);
            }
        }
    }

    /**
     * 10. FROZEN WORLD — Progressive frost particle density.
     * At tick 0: 5 particles/player. At max time: 50 particles/player.
     */
    private void tickFrozenWorld(World world) {
        frozenWorldTick++;

        if (totalTicks % 5 != 0) return;

        double progress = Math.min(1.0, (double) frozenWorldTick / frozenWorldMaxTick);
        int particleCount = 5 + (int)(progress * 45); // 5 -> 50

        Random rng = new Random();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

            for (int i = 0; i < particleCount; i++) {
                double ox = (rng.nextDouble() - 0.5) * 8;
                double oy = rng.nextDouble() * 4;
                double oz = (rng.nextDouble() - 0.5) * 8;
                Location particleLoc = p.getLocation().add(ox, oy, oz);
                p.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 200, 220), 0.4f + (float)(progress * 0.4)));
            }
        }
    }

    // ========================
    // External Integration
    // ========================

    /**
     * Called by attack system when an attack expires, to track frost creep locations.
     */
    public void addFrostCreepLocation(Location location) {
        if (!isGimmickEnabled("frost_creep")) return;
        if (frostCreepLocations.size() >= MAX_FROST_CREEP_LOCATIONS) {
            // Remove oldest
            Location oldest = frostCreepLocations.remove(0);
            frostCreepRadii.remove(oldest);
        }
        frostCreepLocations.add(location);
        frostCreepRadii.put(location, 0.5);
    }

    /**
     * Called by attack system when an attack expires, to track crater locations.
     */
    public void addCraterLocation(Location location) {
        if (!isGimmickEnabled("crater_impact")) return;
        if (craterLocations.size() >= MAX_CRATER_LOCATIONS) {
            craterLocations.remove(0);
        }
        craterLocations.add(location);
    }

    // ========================
    // Public API
    // ========================

    /**
     * Check if a gimmick is currently enabled.
     */
    public boolean isGimmickEnabled(String name) {
        return gimmickStates.getOrDefault(name, false);
    }

    /**
     * Toggle a gimmick on/off.
     */
    public void toggleGimmick(String name) {
        gimmickStates.computeIfPresent(name, (k, v) -> !v);
    }

    /**
     * Get all 52 gimmick names.
     */
    public List<String> getGimmickNames() {
        return ALL_GIMMICKS;
    }

    /**
     * Get only enabled gimmick names.
     */
    public List<String> getEnabledGimmicks() {
        List<String> enabled = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : gimmickStates.entrySet()) {
            if (entry.getValue()) enabled.add(entry.getKey());
        }
        return enabled;
    }

    /**
     * Get moonlight exposure score for a player (0-100).
     */
    public int getExposure(Player player) {
        return exposureScores.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Check if the blue hour event is currently active.
     */
    public boolean isBlueHourActive() {
        return blueHourActive;
    }

    /**
     * Get the current boss descent offset from orbital decay.
     */
    public double getOrbitalDecayOffset() {
        return orbitalDecayOffset;
    }

    /**
     * Get the current hunt target player, or null if none.
     */
    public Player getPreyPlayer() {
        if (preyPlayerUUID == null) return null;
        return plugin.getServer().getPlayer(preyPlayerUUID);
    }

    /**
     * Reset all gimmick state. Called when the mode ends.
     */
    public void cleanup() {
        orbitalDecayOffset = 0.0;
        blueHourActive = false;
        blueHourTimer = 0;
        blueHourCooldown = 0;
        frostCreepLocations.clear();
        frostCreepRadii.clear();
        exposureScores.clear();
        weightStartTick = -1;
        preyPlayerUUID = null;
        huntCooldown = 0;
        craterLocations.clear();
        frozenWorldTick = 0;
        totalTicks = 0;
    }
}
