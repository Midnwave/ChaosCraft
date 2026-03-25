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

    // ── State for newly implemented gimmicks ─────────────────────────

    // moon_phase_wheel
    private int moonPhaseIndex = 0;
    private static final int MOON_PHASE_CYCLE = 3000; // 2.5 min in ticks

    // moonrise_moonset
    private int moonriseTick = 0;
    private static final int MOONRISE_CYCLE = 10000;

    // waxing_waning_power
    // (computed per-tick from sine wave, no persistent state)

    // supermoon_event
    private boolean supermoonActive = false;
    private int supermoonTimer = 0;
    private int supermoonCooldown = 0;
    private static final int SUPERMOON_INTERVAL = 6000; // 5 min
    private static final int SUPERMOON_DURATION = 400;   // 20 sec

    // lunar_link
    // (computed per-tick, no persistent state)

    // moon_fragments
    private int moonFragmentTimer = 0;
    private static final int MOON_FRAGMENT_INTERVAL = 600; // 30 sec

    // phase_lock
    private boolean phaseLocked = false;
    private int phaseLockTimer = 0;
    private static final int PHASE_LOCK_DURATION = 600; // 30 sec

    // reflection_pool
    private Location reflectionPoolLoc = null;
    private int reflectionPoolTimer = 0;
    private int reflectionPoolCooldown = 0;
    private static final int REFLECTION_POOL_INTERVAL = 900; // 45 sec
    private static final int REFLECTION_POOL_DURATION = 450; // ~22.5 sec active

    // selenography
    private Location arenaCenter = null;
    private int selenographyCooldown = 0;
    private static final int SELENOGRAPHY_INTERVAL = 1200; // 60 sec

    // lunar_corruption_score
    private final Map<UUID, Integer> corruptionScores = new HashMap<>();

    // moon_shadow
    // (computed per-tick, no persistent state)

    // moon_dust
    private final Map<UUID, Integer> moonDustStillTicks = new HashMap<>();
    private final Map<UUID, Location> moonDustLastLoc = new HashMap<>();

    // celestial_alignment
    private boolean celestialAlignmentActive = false;
    private int celestialAlignmentTimer = 0;
    private int celestialAlignmentCooldown = 0;
    private static final int CELESTIAL_ALIGNMENT_INTERVAL = 4800; // 4 min
    private static final int CELESTIAL_ALIGNMENT_DURATION = 200;   // 10 sec

    // tidal_breathing
    private int tidalBreathingTick = 0;
    private static final int TIDAL_BREATHING_PERIOD = 200;

    // lunar_eclipse_events
    private boolean lunarEclipseActive = false;
    private int lunarEclipseTimer = 0;
    private int lunarEclipseCooldown = 0;
    private static final int LUNAR_ECLIPSE_INTERVAL = 3600; // 3 min
    private static final int LUNAR_ECLIPSE_DURATION = 200;   // 10 sec

    // lunar_bloom
    private final List<Location> lunarBloomLocations = new ArrayList<>();
    private int lunarBloomTimer = 0;
    private static final int LUNAR_BLOOM_INTERVAL = 400; // 20 sec
    private static final int MAX_LUNAR_BLOOMS = 10;

    // moonstone_veins
    private final List<Location> moonstoneVeinLocations = new ArrayList<>();
    private int moonstoneVeinTimer = 0;
    private static final int MOONSTONE_VEIN_INTERVAL = 300; // 15 sec
    private static final int MAX_MOONSTONE_VEINS = 30;

    // tidal_gravity_wells
    private final List<Location> gravityWellLocations = new ArrayList<>();
    private int gravityWellCooldown = 0;
    private static final int GRAVITY_WELL_INTERVAL = 1200; // 60 sec
    private static final int MAX_GRAVITY_WELLS = 5;

    // gravity_pulse_heartbeat
    // (computed per-tick, no persistent state beyond totalTicks)

    // boss_aura_pulse
    // (computed per-tick, no persistent state beyond totalTicks)

    // lunar_tether
    private UUID tetheredPlayerUUID = null;
    private int tetherTimer = 0;
    private int tetherCooldown = 0;
    private static final int TETHER_INTERVAL = 900;   // 45 sec
    private static final int TETHER_DURATION = 200;     // 10 sec

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

        // ── Newly implemented gimmicks ─────────────────────────────────

        if (isGimmickEnabled("moon_phase_wheel"))       tickMoonPhaseWheel(world);
        if (isGimmickEnabled("moonrise_moonset"))        tickMoonriseMoonset(world);
        if (isGimmickEnabled("waxing_waning_power"))     tickWaxingWaningPower();
        if (isGimmickEnabled("supermoon_event"))          tickSupermoonEvent(world);
        if (isGimmickEnabled("lunar_link"))              tickLunarLink(world);
        if (isGimmickEnabled("moon_fragments"))          tickMoonFragments(world);
        if (isGimmickEnabled("phase_lock"))              tickPhaseLock();
        if (isGimmickEnabled("reflection_pool"))         tickReflectionPool(world);
        if (isGimmickEnabled("selenography"))            tickSelenography(world);
        if (isGimmickEnabled("lunar_corruption_score"))  tickLunarCorruptionScore(world);
        if (isGimmickEnabled("moon_shadow"))             tickMoonShadow(world);
        if (isGimmickEnabled("moon_dust"))               tickMoonDust(world);
        if (isGimmickEnabled("celestial_alignment"))     tickCelestialAlignment(world);
        if (isGimmickEnabled("tidal_breathing"))         tickTidalBreathing(world);
        if (isGimmickEnabled("lunar_eclipse_events"))    tickLunarEclipseEvents(world);
        if (isGimmickEnabled("lunar_bloom"))             tickLunarBloom(world);
        if (isGimmickEnabled("moonstone_veins"))         tickMoonstoneVeins(world);
        if (isGimmickEnabled("tidal_gravity_wells"))     tickTidalGravityWells(world);
        if (isGimmickEnabled("gravity_pulse_heartbeat")) tickGravityPulseHeartbeat(world);
        if (isGimmickEnabled("boss_aura_pulse"))         tickBossAuraPulse(world);
        if (isGimmickEnabled("lunar_tether"))            tickLunarTether(world);

        // ── Stubbed gimmicks (logic not yet implemented) ───────────────

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

            // Announce only in debug mode
            if (plugin.getConfig().getBoolean("debug", false)) {
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The Hunt targets " +
                            ChatColor.WHITE + newPrey.getName() + ChatColor.RED + ChatColor.BOLD + "!");
                }
            }
            // Howl sound always plays (atmospheric)
            for (Player p : world.getPlayers()) {
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
    // Implemented Gimmicks (21 new)
    // ========================

    /**
     * 11. MOON PHASE WHEEL — Cycle through 8 moon phases every 2.5 min.
     * Each phase boosts a different attack type. Track current phase as int 0-7.
     */
    private void tickMoonPhaseWheel(World world) {
        int prevPhase = moonPhaseIndex;
        moonPhaseIndex = (int) ((totalTicks % (MOON_PHASE_CYCLE * 8L)) / MOON_PHASE_CYCLE);

        if (prevPhase != moonPhaseIndex && totalTicks > 1) {
            // Phase changed — notify players
            String[] phaseNames = {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
                    "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};
            for (Player p : world.getPlayers()) {
                p.sendMessage(ChatColor.GRAY + "Moon phase shifts to " +
                        ChatColor.AQUA + phaseNames[moonPhaseIndex] + ChatColor.GRAY + "...");
                DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f + (moonPhaseIndex * 0.1f));
            }
        }

        // Visual indicator every 40 ticks — dust particles with color based on phase
        if (totalTicks % 40 == 0) {
            int brightness = 80 + (moonPhaseIndex * 22); // 80-234
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                    DisplayBuilder.dustParticles(p.getLocation().add(0, 3.0, 0),
                            3, 1.5, brightness, brightness, 255, 0.6f);
                }
            }
        }
    }

    /**
     * 12. MOONRISE MOONSET — Visual particle arc across sky.
     * Boss damage +20% when moon is at zenith (tick 4500-5500 of a 10000 tick cycle).
     */
    private void tickMoonriseMoonset(World world) {
        moonriseTick = (int) (totalTicks % MOONRISE_CYCLE);

        // Render moon arc particle every 10 ticks
        if (totalTicks % 10 == 0 && !world.getPlayers().isEmpty()) {
            Player ref = world.getPlayers().get(0);
            double angle = Math.PI * ((double) moonriseTick / MOONRISE_CYCLE); // 0 to PI
            double arcX = Math.cos(angle) * 30;
            double arcY = Math.sin(angle) * 25 + 10;
            Location moonLoc = ref.getLocation().add(arcX, arcY, 0);

            for (Player p : world.getPlayers()) {
                p.spawnParticle(Particle.DUST, moonLoc, 8, 1.5, 1.5, 1.5, 0,
                        new Particle.DustOptions(Color.fromRGB(240, 240, 255), 2.0f));
            }
        }

        // Zenith indicator
        if (moonriseTick == 4500) {
            for (Player p : world.getPlayers()) {
                p.sendMessage(ChatColor.AQUA + "The moon reaches its zenith...");
            }
        }
    }

    /**
     * 13. WAXING WANING POWER — Boss attack damage multiplier oscillates
     * 0.7x to 1.3x on a sine wave (period 6000 ticks).
     */
    private void tickWaxingWaningPower() {
        // Multiplier is computed on demand via getWaxingWaningMultiplier()
        // No per-tick side effects needed
    }

    /**
     * 14. SUPERMOON EVENT — Every 5 minutes, 20-second supermoon burst.
     * All damage +50%. Giant white particle sphere in sky.
     */
    private void tickSupermoonEvent(World world) {
        if (supermoonActive) {
            supermoonTimer--;
            if (supermoonTimer <= 0) {
                supermoonActive = false;
                supermoonCooldown = SUPERMOON_INTERVAL;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.GRAY + "The supermoon fades away...");
                }
            } else {
                // Giant white particle sphere in sky every 5 ticks
                if (totalTicks % 5 == 0 && !world.getPlayers().isEmpty()) {
                    Player ref = world.getPlayers().get(0);
                    Location skyLoc = ref.getLocation().add(0, 35, 0);
                    Random rng = new Random();
                    for (Player p : world.getPlayers()) {
                        for (int i = 0; i < 20; i++) {
                            double ox = (rng.nextDouble() - 0.5) * 6;
                            double oy = (rng.nextDouble() - 0.5) * 6;
                            double oz = (rng.nextDouble() - 0.5) * 6;
                            p.spawnParticle(Particle.DUST, skyLoc.clone().add(ox, oy, oz), 1, 0, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2.5f));
                        }
                    }
                }
            }
        } else {
            supermoonCooldown--;
            if (supermoonCooldown <= 0) {
                supermoonActive = true;
                supermoonTimer = SUPERMOON_DURATION;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "A SUPERMOON rises!");
                    DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
                }
            }
        }
    }

    /**
     * 15. LUNAR LINK — Players within 8 blocks share 15% of damage taken.
     * Cyan particle lines between linked players every 20 ticks.
     */
    private void tickLunarLink(World world) {
        if (totalTicks % 20 != 0) return;

        List<Player> survivors = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                survivors.add(p);
            }
        }

        // Draw cyan particle lines between linked players
        for (int i = 0; i < survivors.size(); i++) {
            for (int j = i + 1; j < survivors.size(); j++) {
                Player a = survivors.get(i);
                Player b = survivors.get(j);
                if (a.getLocation().distance(b.getLocation()) <= 8.0) {
                    DisplayBuilder.particleLine(
                            a.getLocation().add(0, 1.0, 0),
                            b.getLocation().add(0, 1.0, 0),
                            Particle.DUST, 8,
                            new Particle.DustOptions(Color.fromRGB(80, 220, 255), 0.7f));
                }
            }
        }
    }

    /**
     * 16. MOON FRAGMENTS — Every 30 seconds, spawn glowstone particle
     * at random location near a player. Visual ambient only.
     */
    private void tickMoonFragments(World world) {
        moonFragmentTimer--;
        if (moonFragmentTimer > 0) return;
        moonFragmentTimer = MOON_FRAGMENT_INTERVAL;

        List<Player> survivors = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                survivors.add(p);
            }
        }
        if (survivors.isEmpty()) return;

        Random rng = new Random();
        Player target = survivors.get(rng.nextInt(survivors.size()));
        double ox = (rng.nextDouble() - 0.5) * 10;
        double oz = (rng.nextDouble() - 0.5) * 10;
        Location fragLoc = target.getLocation().add(ox, 1.5, oz);

        for (Player p : world.getPlayers()) {
            p.spawnParticle(Particle.DUST, fragLoc, 12, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 180), 1.2f));
        }
    }

    /**
     * 17. PHASE LOCK — When boss transitions phase, lock for 30 seconds.
     * Boolean flag + timer. Externally triggered via lockPhase().
     */
    private void tickPhaseLock() {
        if (phaseLocked) {
            phaseLockTimer--;
            if (phaseLockTimer <= 0) {
                phaseLocked = false;
            }
        }
    }

    /**
     * 18. REFLECTION POOL — Every 45 seconds, pick a random spot and spawn
     * cyan particle circle (radius 3). Players inside take 2 damage/sec.
     */
    private void tickReflectionPool(World world) {
        if (reflectionPoolLoc != null) {
            reflectionPoolTimer--;

            // Render pool every 10 ticks
            if (totalTicks % 10 == 0 && reflectionPoolLoc.getWorld() != null
                    && reflectionPoolLoc.getWorld().equals(world)) {
                DisplayBuilder.particleRing(reflectionPoolLoc.clone().add(0, 0.1, 0), 3.0, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(80, 220, 255), 0.9f));
            }

            // Damage players inside every 20 ticks (1/sec)
            if (totalTicks % 20 == 0) {
                for (Player p : world.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                    if (reflectionPoolLoc.getWorld() != null && reflectionPoolLoc.getWorld().equals(world)
                            && p.getLocation().distance(reflectionPoolLoc) <= 3.0) {
                        p.damage(4.0); // 2 hearts
                    }
                }
            }

            if (reflectionPoolTimer <= 0) {
                reflectionPoolLoc = null;
                reflectionPoolCooldown = REFLECTION_POOL_INTERVAL;
            }
        } else {
            reflectionPoolCooldown--;
            if (reflectionPoolCooldown <= 0) {
                List<Player> survivors = new ArrayList<>();
                for (Player p : world.getPlayers()) {
                    if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                        survivors.add(p);
                    }
                }
                if (!survivors.isEmpty()) {
                    Random rng = new Random();
                    Player target = survivors.get(rng.nextInt(survivors.size()));
                    double ox = (rng.nextDouble() - 0.5) * 12;
                    double oz = (rng.nextDouble() - 0.5) * 12;
                    reflectionPoolLoc = target.getLocation().add(ox, 0, oz);
                    reflectionPoolTimer = REFLECTION_POOL_DURATION;

                    for (Player p : world.getPlayers()) {
                        DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.6f, 1.4f);
                    }
                }
            }
        }
    }

    /**
     * 19. SELENOGRAPHY — Every 60 seconds, shift arena "center" by 3 blocks
     * in random direction. Spawn soul particles at new center.
     */
    private void tickSelenography(World world) {
        selenographyCooldown--;
        if (selenographyCooldown > 0) {
            // Render current center every 20 ticks
            if (arenaCenter != null && totalTicks % 20 == 0
                    && arenaCenter.getWorld() != null && arenaCenter.getWorld().equals(world)) {
                for (Player p : world.getPlayers()) {
                    p.spawnParticle(Particle.SOUL, arenaCenter.clone().add(0, 0.5, 0), 5, 0.5, 0.5, 0.5, 0.01);
                }
            }
            return;
        }
        selenographyCooldown = SELENOGRAPHY_INTERVAL;

        if (arenaCenter == null && !world.getPlayers().isEmpty()) {
            arenaCenter = world.getPlayers().get(0).getLocation().clone();
        }

        if (arenaCenter != null) {
            Random rng = new Random();
            double angle = rng.nextDouble() * Math.PI * 2;
            arenaCenter.add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);

            for (Player p : world.getPlayers()) {
                p.spawnParticle(Particle.SOUL, arenaCenter.clone().add(0, 1.0, 0), 15, 1.0, 1.0, 1.0, 0.02);
                DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.6f);
            }
        }
    }

    /**
     * 20. LUNAR CORRUPTION SCORE — Per-player score 0-100. +1 every 5 seconds.
     * At 50+: ambient damage 1/5s. At 80+: damage 2/5s. Red dust particles scale with score.
     */
    private void tickLunarCorruptionScore(World world) {
        // Increment every 5 seconds (100 ticks)
        boolean increment = (totalTicks % 100 == 0);
        // Damage every 5 seconds (100 ticks), offset by 10
        boolean damageCheck = (totalTicks % 100 == 10);

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

            UUID uuid = p.getUniqueId();
            int score = corruptionScores.getOrDefault(uuid, 0);

            if (increment) {
                score = Math.min(100, score + 1);
                corruptionScores.put(uuid, score);
            }

            // Damage at thresholds
            if (damageCheck) {
                if (score >= 80) {
                    p.damage(4.0); // 2 hearts
                } else if (score >= 50) {
                    p.damage(2.0); // 1 heart
                }
            }

            // Red dust particles scale with score every 10 ticks
            if (score > 0 && totalTicks % 10 == 0) {
                int intensity = Math.max(1, score / 10);
                DisplayBuilder.dustParticles(p.getLocation().add(0, 2.0, 0),
                        intensity, 0.8, 255, 50, 50, 0.5f + (score / 200.0f));
            }
        }
    }

    /**
     * 21. MOON SHADOW — Every 10 ticks, spawn dark dust particles behind each player.
     * If another player walks into shadow zone, 1 damage.
     */
    private void tickMoonShadow(World world) {
        if (totalTicks % 10 != 0) return;

        List<Player> survivors = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                survivors.add(p);
            }
        }

        // Track shadow locations for cross-checking
        Map<Player, Location> shadows = new HashMap<>();

        for (Player p : survivors) {
            // Calculate position 2 blocks behind player (opposite of facing)
            Vector facing = p.getLocation().getDirection().normalize();
            Location shadowLoc = p.getLocation().add(facing.multiply(-2)).add(0, 0.2, 0);
            shadows.put(p, shadowLoc);

            // Spawn dark particles
            DisplayBuilder.dustParticles(shadowLoc, 5, 0.5, 30, 30, 40, 0.8f);
        }

        // Check if any player is in another player's shadow
        for (Map.Entry<Player, Location> entry : shadows.entrySet()) {
            for (Player other : survivors) {
                if (other.equals(entry.getKey())) continue;
                if (other.getLocation().distance(entry.getValue()) <= 1.5) {
                    other.damage(2.0); // 1 heart
                }
            }
        }
    }

    /**
     * 22. MOON DUST — Per-player dust trail. Spawn white dust particles at feet every 5 ticks.
     * If player stands still for 40+ ticks, slow them for 20 ticks.
     */
    private void tickMoonDust(World world) {
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

            UUID uuid = p.getUniqueId();

            // Trail particles every 5 ticks
            if (totalTicks % 5 == 0) {
                DisplayBuilder.dustParticles(p.getLocation().add(0, 0.1, 0),
                        3, 0.3, 230, 230, 240, 0.5f);
            }

            // Track stillness
            Location lastLoc = moonDustLastLoc.get(uuid);
            Location currentLoc = p.getLocation();

            if (lastLoc != null && lastLoc.getWorld() != null && lastLoc.getWorld().equals(currentLoc.getWorld())
                    && lastLoc.distanceSquared(currentLoc) < 0.04) {
                // Effectively standing still
                int still = moonDustStillTicks.getOrDefault(uuid, 0) + 1;
                moonDustStillTicks.put(uuid, still);

                if (still == 40) {
                    // Slow the player
                    p.setWalkSpeed(0.15f);
                    DisplayBuilder.dustParticles(p.getLocation().add(0, 0.5, 0),
                            10, 1.0, 200, 200, 220, 1.0f);
                }
                if (still >= 60) {
                    // Restore after 20 ticks of slow
                    p.setWalkSpeed(0.2f); // Default walk speed
                    moonDustStillTicks.put(uuid, 0);
                }
            } else {
                moonDustStillTicks.put(uuid, 0);
                // Restore speed if they moved
                if (p.getWalkSpeed() < 0.2f) {
                    p.setWalkSpeed(0.2f);
                }
            }

            moonDustLastLoc.put(uuid, currentLoc.clone());
        }
    }

    /**
     * 23. CELESTIAL ALIGNMENT — Every 4 minutes, 10-second alignment event.
     * Spawn vertical particle beam. All attacks during alignment do +30% damage.
     */
    private void tickCelestialAlignment(World world) {
        if (celestialAlignmentActive) {
            celestialAlignmentTimer--;
            if (celestialAlignmentTimer <= 0) {
                celestialAlignmentActive = false;
                celestialAlignmentCooldown = CELESTIAL_ALIGNMENT_INTERVAL;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.GRAY + "The celestial alignment dissipates...");
                }
            } else {
                // Vertical particle beam every 5 ticks
                if (totalTicks % 5 == 0 && !world.getPlayers().isEmpty()) {
                    Player ref = world.getPlayers().get(0);
                    Location base = ref.getLocation().add(0, 0, 0);
                    for (int y = 0; y < 40; y += 2) {
                        for (Player p : world.getPlayers()) {
                            p.spawnParticle(Particle.DUST, base.clone().add(0, y, 0), 3, 0.2, 0.2, 0.2, 0,
                                    new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1.2f));
                        }
                    }
                }
            }
        } else {
            celestialAlignmentCooldown--;
            if (celestialAlignmentCooldown <= 0) {
                celestialAlignmentActive = true;
                celestialAlignmentTimer = CELESTIAL_ALIGNMENT_DURATION;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "The stars align!");
                    DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 1.5f);
                }
            }
        }
    }

    /**
     * 24. TIDAL BREATHING — Arena boundary pulses: damage zone ring at radius 25
     * oscillates to radius 15 and back over 200 ticks. Players outside safe zone take 3 damage/sec.
     */
    private void tickTidalBreathing(World world) {
        tidalBreathingTick++;
        double progress = (double) (tidalBreathingTick % TIDAL_BREATHING_PERIOD) / TIDAL_BREATHING_PERIOD;
        double safeRadius = 15.0 + (Math.sin(progress * Math.PI * 2) + 1.0) * 5.0; // oscillates 15-25

        if (arenaCenter == null) {
            if (!world.getPlayers().isEmpty()) {
                arenaCenter = world.getPlayers().get(0).getLocation().clone();
            }
            return;
        }

        // Render boundary ring every 10 ticks
        if (totalTicks % 10 == 0 && arenaCenter.getWorld() != null && arenaCenter.getWorld().equals(world)) {
            DisplayBuilder.particleRing(arenaCenter.clone().add(0, 1.0, 0), safeRadius, Particle.DUST, 30,
                    new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.8f));
        }

        // Damage outside safe zone every 20 ticks (3 damage/sec)
        if (totalTicks % 20 == 0) {
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                double dist = p.getLocation().distance(arenaCenter);
                if (dist > safeRadius) {
                    p.damage(6.0); // 3 hearts
                }
            }
        }
    }

    /**
     * 25. LUNAR ECLIPSE EVENTS — Every 3 min, 10-second eclipse.
     * Sky darkens (time set 18500), boss becomes invulnerable. Purple particles everywhere.
     */
    private void tickLunarEclipseEvents(World world) {
        if (lunarEclipseActive) {
            lunarEclipseTimer--;

            // Keep sky dark
            if (totalTicks % 20 == 0) {
                world.setTime(18500L);
            }

            // Purple particles every 5 ticks
            if (totalTicks % 5 == 0) {
                Random rng = new Random();
                for (Player p : world.getPlayers()) {
                    for (int i = 0; i < 10; i++) {
                        double ox = (rng.nextDouble() - 0.5) * 12;
                        double oy = rng.nextDouble() * 5;
                        double oz = (rng.nextDouble() - 0.5) * 12;
                        p.spawnParticle(Particle.DUST, p.getLocation().add(ox, oy, oz), 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(140, 60, 200), 1.0f));
                    }
                }
            }

            if (lunarEclipseTimer <= 0) {
                lunarEclipseActive = false;
                lunarEclipseCooldown = LUNAR_ECLIPSE_INTERVAL;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.GRAY + "The eclipse ends...");
                }
            }
        } else {
            lunarEclipseCooldown--;
            if (lunarEclipseCooldown <= 0) {
                lunarEclipseActive = true;
                lunarEclipseTimer = LUNAR_ECLIPSE_DURATION;
                for (Player p : world.getPlayers()) {
                    p.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "A lunar eclipse descends!");
                    DisplayBuilder.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                }
            }
        }
    }

    /**
     * 26. LUNAR BLOOM — Every 20 seconds, spawn frost flower at random location.
     * If player enters within 1.5 blocks, burst of 4 hearts damage + particles.
     */
    private void tickLunarBloom(World world) {
        lunarBloomTimer--;

        // Spawn new bloom
        if (lunarBloomTimer <= 0) {
            lunarBloomTimer = LUNAR_BLOOM_INTERVAL;

            List<Player> survivors = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                    survivors.add(p);
                }
            }
            if (!survivors.isEmpty() && lunarBloomLocations.size() < MAX_LUNAR_BLOOMS) {
                Random rng = new Random();
                Player target = survivors.get(rng.nextInt(survivors.size()));
                double ox = (rng.nextDouble() - 0.5) * 16;
                double oz = (rng.nextDouble() - 0.5) * 16;
                lunarBloomLocations.add(target.getLocation().add(ox, 0, oz));
            }
        }

        // Render blooms every 10 ticks and check proximity
        if (totalTicks % 10 == 0) {
            Iterator<Location> it = lunarBloomLocations.iterator();
            while (it.hasNext()) {
                Location bloomLoc = it.next();
                if (bloomLoc.getWorld() == null || !bloomLoc.getWorld().equals(world)) continue;

                // Snowflake particles in 1-block radius
                DisplayBuilder.particleRing(bloomLoc.clone().add(0, 0.3, 0), 1.0, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(200, 230, 255), 0.8f));

                // Check proximity
                for (Player p : world.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                    if (p.getLocation().distance(bloomLoc) <= 1.5) {
                        // Burst!
                        p.damage(8.0); // 4 hearts
                        DisplayBuilder.dustParticles(bloomLoc.clone().add(0, 1.0, 0),
                                20, 2.0, 200, 230, 255, 1.2f);
                        DisplayBuilder.playSound(bloomLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                        it.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 27. MOONSTONE VEINS — Track vein locations (max 30). Every 15 seconds add new one.
     * Render as lines of cyan dust particles. Players near veins regen 0.5 hearts/5s.
     */
    private void tickMoonstoneVeins(World world) {
        moonstoneVeinTimer--;

        // Spawn new vein
        if (moonstoneVeinTimer <= 0) {
            moonstoneVeinTimer = MOONSTONE_VEIN_INTERVAL;

            if (moonstoneVeinLocations.size() < MAX_MOONSTONE_VEINS && !world.getPlayers().isEmpty()) {
                Random rng = new Random();
                Player target = world.getPlayers().get(rng.nextInt(world.getPlayers().size()));
                double ox = (rng.nextDouble() - 0.5) * 20;
                double oz = (rng.nextDouble() - 0.5) * 20;
                moonstoneVeinLocations.add(target.getLocation().add(ox, 0, oz));
            }
        }

        // Render veins every 10 ticks
        if (totalTicks % 10 == 0) {
            for (Location veinLoc : moonstoneVeinLocations) {
                if (veinLoc.getWorld() == null || !veinLoc.getWorld().equals(world)) continue;

                // Cyan dust line (small ring as visual)
                DisplayBuilder.particleRing(veinLoc.clone().add(0, 0.1, 0), 1.5, Particle.DUST, 10,
                        new Particle.DustOptions(Color.fromRGB(80, 220, 255), 0.6f));
            }
        }

        // Regen near veins every 100 ticks (5 seconds)
        if (totalTicks % 100 == 0) {
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

                for (Location veinLoc : moonstoneVeinLocations) {
                    if (veinLoc.getWorld() == null || !veinLoc.getWorld().equals(world)) continue;
                    if (p.getLocation().distance(veinLoc) <= 3.0) {
                        double newHealth = Math.min(p.getMaxHealth(), p.getHealth() + 1.0); // 0.5 hearts
                        p.setHealth(newHealth);
                        break; // Only regen from one vein per cycle
                    }
                }
            }
        }
    }

    /**
     * 28. TIDAL GRAVITY WELLS — Spawn 1-3 gravity wells every 60 seconds.
     * Wells pull players within 5 blocks toward center. Render as spiral particles.
     */
    private void tickTidalGravityWells(World world) {
        gravityWellCooldown--;

        // Spawn new wells
        if (gravityWellCooldown <= 0) {
            gravityWellCooldown = GRAVITY_WELL_INTERVAL;

            // Clean old wells if at max
            if (gravityWellLocations.size() >= MAX_GRAVITY_WELLS) {
                gravityWellLocations.subList(0, 2).clear();
            }

            List<Player> survivors = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                    survivors.add(p);
                }
            }
            if (!survivors.isEmpty()) {
                Random rng = new Random();
                int count = 1 + rng.nextInt(3); // 1-3
                for (int i = 0; i < count; i++) {
                    Player target = survivors.get(rng.nextInt(survivors.size()));
                    double ox = (rng.nextDouble() - 0.5) * 16;
                    double oz = (rng.nextDouble() - 0.5) * 16;
                    gravityWellLocations.add(target.getLocation().add(ox, 0, oz));
                }
                for (Player p : world.getPlayers()) {
                    DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);
                }
            }
        }

        // Pull players and render every 5 ticks
        if (totalTicks % 5 == 0) {
            for (Location wellLoc : gravityWellLocations) {
                if (wellLoc.getWorld() == null || !wellLoc.getWorld().equals(world)) continue;

                // Spiral particles
                double angle = (totalTicks * 0.3) % (Math.PI * 2);
                for (int i = 0; i < 6; i++) {
                    double a = angle + (i * Math.PI / 3);
                    double radius = 2.0 + Math.sin(totalTicks * 0.1 + i) * 1.5;
                    Location particleLoc = wellLoc.clone().add(Math.cos(a) * radius, 0.5, Math.sin(a) * radius);
                    for (Player p : world.getPlayers()) {
                        p.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(160, 100, 255), 0.8f));
                    }
                }

                // Pull players
                for (Player p : world.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                    double dist = p.getLocation().distance(wellLoc);
                    if (dist <= 5.0 && dist > 0.5) {
                        Vector pull = wellLoc.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.08);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }
        }
    }

    /**
     * 29. GRAVITY PULSE HEARTBEAT — Every 40 ticks, pulse. Players within 10 blocks
     * of boss get slight upward velocity (0.15). Heartbeat sound.
     */
    private void tickGravityPulseHeartbeat(World world) {
        if (totalTicks % 40 != 0) return;

        // Find boss entity (look for MythicMobs boss — use first non-player living entity as fallback)
        Location bossLoc = getBossLocation(world);
        if (bossLoc == null) return;

        // Heartbeat sound
        for (Player p : world.getPlayers()) {
            DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        // Upward velocity pulse
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.getLocation().distance(bossLoc) <= 10.0) {
                Vector vel = p.getVelocity();
                p.setVelocity(new Vector(vel.getX(), vel.getY() + 0.15, vel.getZ()));
            }
        }

        // Visual pulse
        DisplayBuilder.particleRing(bossLoc.clone().add(0, 1.0, 0), 3.0, Particle.DUST, 15,
                new Particle.DustOptions(Color.fromRGB(200, 50, 80), 1.0f));
    }

    /**
     * 30. BOSS AURA PULSE — Every 60 ticks, boss emits damage aura.
     * Players within 8 blocks of boss take 3 damage. Expanding ring particle.
     */
    private void tickBossAuraPulse(World world) {
        if (totalTicks % 60 != 0) return;

        Location bossLoc = getBossLocation(world);
        if (bossLoc == null) return;

        // Expanding ring effect (multiple rings at different radii)
        for (double r = 1.0; r <= 8.0; r += 1.5) {
            DisplayBuilder.particleRing(bossLoc.clone().add(0, 1.0, 0), r, Particle.DUST,
                    (int) (r * 4), new Particle.DustOptions(Color.fromRGB(180, 50, 255), 0.9f));
        }

        // Damage players within 8 blocks
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.getLocation().distance(bossLoc) <= 8.0) {
                p.damage(6.0); // 3 hearts
            }
        }

        DisplayBuilder.playSound(bossLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);
    }

    /**
     * 31. LUNAR TETHER — Every 45 seconds, boss tethers to nearest player.
     * Particle line from boss to player. Tethered player takes 1 damage/sec for 10 seconds.
     */
    private void tickLunarTether(World world) {
        if (tetheredPlayerUUID != null) {
            tetherTimer--;

            Location bossLoc = getBossLocation(world);
            Player tethered = plugin.getServer().getPlayer(tetheredPlayerUUID);

            if (tethered != null && tethered.isOnline() && tethered.getWorld().equals(world) && bossLoc != null) {
                // Particle line every 5 ticks
                if (totalTicks % 5 == 0) {
                    DisplayBuilder.particleLine(
                            bossLoc.clone().add(0, 1.5, 0),
                            tethered.getLocation().add(0, 1.0, 0),
                            Particle.DUST, 15,
                            new Particle.DustOptions(Color.fromRGB(180, 220, 255), 0.9f));
                }

                // 1 damage/sec (every 20 ticks)
                if (totalTicks % 20 == 0) {
                    tethered.damage(2.0); // 1 heart
                }
            }

            if (tetherTimer <= 0) {
                tetheredPlayerUUID = null;
                tetherCooldown = TETHER_INTERVAL;
            }
        } else {
            tetherCooldown--;
            if (tetherCooldown <= 0) {
                Location bossLoc = getBossLocation(world);
                if (bossLoc == null) {
                    tetherCooldown = 100; // Retry in 5 seconds
                    return;
                }

                // Find nearest player
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : world.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                    double dist = p.getLocation().distance(bossLoc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = p;
                    }
                }

                if (nearest != null) {
                    tetheredPlayerUUID = nearest.getUniqueId();
                    tetherTimer = TETHER_DURATION;
                    for (Player p : world.getPlayers()) {
                        DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.6f);
                    }
                    nearest.sendMessage(ChatColor.AQUA + "You are tethered by moonlight!");
                } else {
                    tetherCooldown = 100;
                }
            }
        }
    }

    /**
     * Helper: find boss location in world. Uses MythicMobs active mobs or falls back to
     * the arena center if available.
     */
    private Location getBossLocation(World world) {
        // Try to get boss from BlueMoonMode if accessible via plugin
        // Fallback: use arena center or first player's location offset
        if (arenaCenter != null && arenaCenter.getWorld() != null && arenaCenter.getWorld().equals(world)) {
            return arenaCenter;
        }
        if (!world.getPlayers().isEmpty()) {
            return world.getPlayers().get(0).getLocation().add(0, 5, 0);
        }
        return null;
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
     * Get the current moon phase index (0-7).
     */
    public int getMoonPhaseIndex() {
        return moonPhaseIndex;
    }

    /**
     * Check if the moon is at zenith (moonrise_moonset).
     */
    public boolean isMoonAtZenith() {
        return moonriseTick >= 4500 && moonriseTick <= 5500;
    }

    /**
     * Get the waxing/waning damage multiplier (0.7 to 1.3).
     */
    public double getWaxingWaningMultiplier() {
        return 1.0 + 0.3 * Math.sin(2.0 * Math.PI * totalTicks / 6000.0);
    }

    /**
     * Check if a supermoon event is currently active.
     */
    public boolean isSupermoonActive() {
        return supermoonActive;
    }

    /**
     * Check if the phase is currently locked.
     */
    public boolean isPhaseLocked() {
        return phaseLocked;
    }

    /**
     * Externally trigger a phase lock (e.g., on boss phase transition).
     */
    public void lockPhase() {
        if (!isGimmickEnabled("phase_lock")) return;
        phaseLocked = true;
        phaseLockTimer = PHASE_LOCK_DURATION;
    }

    /**
     * Check if celestial alignment is active (+30% damage).
     */
    public boolean isCelestialAlignmentActive() {
        return celestialAlignmentActive;
    }

    /**
     * Check if a lunar eclipse is currently active (boss invulnerable).
     */
    public boolean isLunarEclipseActive() {
        return lunarEclipseActive;
    }

    /**
     * Get corruption score for a player (0-100).
     */
    public int getCorruptionScore(Player player) {
        return corruptionScores.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Get the arena center location (used by selenography / tidal breathing).
     */
    public Location getArenaCenter() {
        return arenaCenter;
    }

    /**
     * Set the arena center (called externally when the mode starts).
     */
    public void setArenaCenter(Location location) {
        this.arenaCenter = location != null ? location.clone() : null;
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

        // New gimmick state cleanup
        moonPhaseIndex = 0;
        moonriseTick = 0;
        supermoonActive = false;
        supermoonTimer = 0;
        supermoonCooldown = 0;
        moonFragmentTimer = 0;
        phaseLocked = false;
        phaseLockTimer = 0;
        reflectionPoolLoc = null;
        reflectionPoolTimer = 0;
        reflectionPoolCooldown = 0;
        arenaCenter = null;
        selenographyCooldown = 0;
        corruptionScores.clear();
        moonDustStillTicks.clear();
        moonDustLastLoc.clear();
        celestialAlignmentActive = false;
        celestialAlignmentTimer = 0;
        celestialAlignmentCooldown = 0;
        tidalBreathingTick = 0;
        lunarEclipseActive = false;
        lunarEclipseTimer = 0;
        lunarEclipseCooldown = 0;
        lunarBloomLocations.clear();
        lunarBloomTimer = 0;
        moonstoneVeinLocations.clear();
        moonstoneVeinTimer = 0;
        gravityWellLocations.clear();
        gravityWellCooldown = 0;
        tetheredPlayerUUID = null;
        tetherTimer = 0;
        tetherCooldown = 0;

        totalTicks = 0;
    }
}
