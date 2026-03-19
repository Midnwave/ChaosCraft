package com.blockforge.chaoscraft.modes.calamity;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.api.mode.AbstractMode;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackScheduler;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase2.blockdisplay.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase3.boss.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental.*;
import com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss.*;
import com.blockforge.chaoscraft.modes.calamity.boss.DoGManager;
import com.blockforge.chaoscraft.modes.calamity.boss.DragonManager;
import com.blockforge.chaoscraft.modes.calamity.boss.calamitas.CalamitasManager;
import com.blockforge.chaoscraft.modes.calamity.egg.EggAnimation;
import com.blockforge.chaoscraft.modes.calamity.ritual.DragonEggDetector;
import com.blockforge.chaoscraft.modes.calamity.ritual.GemManager;
import com.blockforge.chaoscraft.modes.calamity.ritual.PortalManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Calamity GS Mode — 5-boss sequential horror boss rush in The End.
 *
 * Phase flow:
 * 1. Dragon egg ritual detected → mode starts
 * 2. All players teleported to The End
 * 3. Dragon egg becomes animated BlockDisplay
 * 4. Ender Dragon spawns (invincible until Phase 4)
 * 5. Gems rain from sky → players collect and deposit at portal
 * 6. Phase 1: Enough gems deposited → Voidmaw spawns (MythicMobs)
 * 7. Phase 2: Enough gems → DoG spawns (datapack)
 * 8. Phase 3: Enough gems → Dweller spawns (MythicMobs)
 * 9. Phase 4: Enough gems → Dragon becomes fightable + mini bosses
 * 10. Phase 5: Enough gems → Supreme Calamitas (custom NMS AI)
 * 11. Final boss defeated → mode complete
 *
 * Between bosses: egg has its own attacks, charges up for next boss.
 */
public class CalamityMode extends AbstractMode {

    // Calamity-specific config (separate from generic ModeConfig)
    private final CalamityConfig calamityConfig;

    // Sub-systems
    private final DragonEggDetector eggDetector;
    private final GemManager gemManager;
    private final PortalManager portalManager;
    private final EggAnimation eggAnimation;
    private final DoGManager dogManager;
    private final DragonManager dragonManager;
    private final CalamitasManager calamitasManager;

    // Attack framework
    private final AttackRegistry attackRegistry;
    private final AttackScheduler attackScheduler;

    // State
    private int currentPhase = 0;
    private boolean bossAlive = false;
    private boolean waitingForDeposit = false;
    private long tickCounter = 0;

    // Boss names for the 5 phases
    private static final String[] BOSS_NAMES = {"voidmaw", "dog", "dweller", "dragon", "calamitas"};

    // Egg orbit around arena
    private double eggOrbitAngle = 0;
    private static final double EGG_ORBIT_ARENA_RADIUS = 20.0;
    private static final double EGG_ORBIT_ARENA_SPEED = 0.005;

    public CalamityMode(ChaosCraftPlugin plugin) {
        super(plugin, "calamity");
        this.calamityConfig = new CalamityConfig(plugin);
        this.eggDetector = new DragonEggDetector(plugin);
        this.gemManager = new GemManager(plugin, calamityConfig);
        this.portalManager = new PortalManager(plugin, calamityConfig);
        this.eggAnimation = new EggAnimation(plugin, calamityConfig);
        this.dogManager = new DoGManager(plugin);
        this.dragonManager = new DragonManager(plugin, calamityConfig);
        this.calamitasManager = new CalamitasManager(plugin, calamityConfig);
        this.attackRegistry = new AttackRegistry(plugin);
        this.attackScheduler = new AttackScheduler(plugin, attackRegistry, calamityConfig);
        registerPhase1BlockDisplayAttacks();
        registerPhase1EnvironmentalAttacks();
        registerPhase1BossAttacks();
        registerPhase2BlockDisplayAttacks();
        registerPhase2EnvironmentalAttacks();
        registerPhase2BossAttacks();
        registerPhase3BlockDisplayAttacks();
        registerPhase3EnvironmentalAttacks();
        registerPhase3BossAttacks();
        registerPhase4BlockDisplayAttacks();
        registerPhase4EnvironmentalAttacks();
        registerPhase4BossAttacks();
        registerPhase5BlockDisplayAttacks();
        registerPhase5EnvironmentalAttacks();
        registerPhase5BossAttacks();

        // Generate/load per-attack YAML config files
        attackRegistry.reloadConfigs();
        plugin.getLogger().info("[Calamity] Registered " + attackRegistry.size() + " attacks, configs loaded.");
    }

    // ========================
    // Attack Registration
    // ========================

    private void registerPhase1BlockDisplayAttacks() {
        GroundEruptions.registerAll(plugin, attackRegistry);
        SkyOverhead.registerAll(plugin, attackRegistry);
        WallsBarriers.registerAll(plugin, attackRegistry);
        EnclosuresCages.registerAll(plugin, attackRegistry);
        OrganicShapes.registerAll(plugin, attackRegistry);
        GeometricShapes.registerAll(plugin, attackRegistry);
        PortalsGateways.registerAll(plugin, attackRegistry);
        WeaponsTools.registerAll(plugin, attackRegistry);
        CosmicCelestial.registerAll(plugin, attackRegistry);
        ColossalMonumental.registerAll(plugin, attackRegistry);
    }

    private void registerPhase1EnvironmentalAttacks() {
        SkyFalls.registerAll(plugin, attackRegistry);
        FloorEruptions.registerAll(plugin, attackRegistry);
        EdgeHazards.registerAll(plugin, attackRegistry);
        SweepingAttacks.registerAll(plugin, attackRegistry);
        ZoneDenial.registerAll(plugin, attackRegistry);
        Atmospheric.registerAll(plugin, attackRegistry);
        Targeted.registerAll(plugin, attackRegistry);
        DelayedTrap.registerAll(plugin, attackRegistry);
        ChainReaction.registerAll(plugin, attackRegistry);
        CorruptionSpread.registerAll(plugin, attackRegistry);
    }

    private void registerPhase1BossAttacks() {
        SlamImpact.registerAll(plugin, attackRegistry);
        Projectile.registerAll(plugin, attackRegistry);
        PullGravity.registerAll(plugin, attackRegistry);
        Summon.registerAll(plugin, attackRegistry);
        BeamLaser.registerAll(plugin, attackRegistry);
        AOEBurst.registerAll(plugin, attackRegistry);
        ZoneControl.registerAll(plugin, attackRegistry);
        Debuff.registerAll(plugin, attackRegistry);
        ChargeMovement.registerAll(plugin, attackRegistry);
        EnragePhase.registerAll(plugin, attackRegistry);
    }

    private void registerPhase2BlockDisplayAttacks() {
        CrystalEruptions.registerAll(plugin, attackRegistry);
        CosmicOverhead.registerAll(plugin, attackRegistry);
        DimensionalRifts.registerAll(plugin, attackRegistry);
        LaserArrays.registerAll(plugin, attackRegistry);
        SerpentinePhantom.registerAll(plugin, attackRegistry);
        CrystalFormations.registerAll(plugin, attackRegistry);
        WormTrails.registerAll(plugin, attackRegistry);
        CosmicWeaponry.registerAll(plugin, attackRegistry);
        DimensionalAnchors.registerAll(plugin, attackRegistry);
        TranscendentMonuments.registerAll(plugin, attackRegistry);
    }

    private void registerPhase2EnvironmentalAttacks() {
        CrystalScarTriggers.registerAll(plugin, attackRegistry);
        CosmicSkyEvents.registerAll(plugin, attackRegistry);
        DimensionalInstability.registerAll(plugin, attackRegistry);
        IslandDeterioration.registerAll(plugin, attackRegistry);
        AtmosphericCosmic.registerAll(plugin, attackRegistry);
        ScarOverload.registerAll(plugin, attackRegistry);
        RiftStorm.registerAll(plugin, attackRegistry);
        RealityCollapse.registerAll(plugin, attackRegistry);
        GodsHunger.registerAll(plugin, attackRegistry);
        TranscendenceEvents.registerAll(plugin, attackRegistry);
    }

    private void registerPhase2BossAttacks() {
        ChargeSweep.registerAll(plugin, attackRegistry);
        DoGProjectile.registerAll(plugin, attackRegistry);
        LaserAttacks.registerAll(plugin, attackRegistry);
        SummonAttacks.registerAll(plugin, attackRegistry);
        AOEField.registerAll(plugin, attackRegistry);
        RiftCharge.registerAll(plugin, attackRegistry);
        CosmicLaserBarrage.registerAll(plugin, attackRegistry);
        RealityTear.registerAll(plugin, attackRegistry);
        TranscendentForm.registerAll(plugin, attackRegistry);
        EnrageAttacks.registerAll(plugin, attackRegistry);
    }

    private void registerPhase3BlockDisplayAttacks() {
        BrimstoneSpine.registerAll(plugin, attackRegistry);
        SuspendedLattice.registerAll(plugin, attackRegistry);
        PortalRiftFrames.registerAll(plugin, attackRegistry);
        FireSoulArrays.registerAll(plugin, attackRegistry);
        CorruptionTrails.registerAll(plugin, attackRegistry);
        TierThreeDisplaysA.registerAll(plugin, attackRegistry);
        TierThreeDisplaysB.registerAll(plugin, attackRegistry);
        TierFourDisplaysA.registerAll(plugin, attackRegistry);
        TierFourDisplaysB.registerAll(plugin, attackRegistry);
        TierFiveDisplays.registerAll(plugin, attackRegistry);
    }

    private void registerPhase3EnvironmentalAttacks() {
        MagmaFloor.registerAll(plugin, attackRegistry);
        BasaltColumn.registerAll(plugin, attackRegistry);
        SoulEvents.registerAll(plugin, attackRegistry);
        CorruptionTrailEvents.registerAll(plugin, attackRegistry);
        BrimstoneSky.registerAll(plugin, attackRegistry);
        IslandConsumption.registerAll(plugin, attackRegistry);
        DwellerEcho.registerAll(plugin, attackRegistry);
        HellfireCascade.registerAll(plugin, attackRegistry);
        VoidBelow.registerAll(plugin, attackRegistry);
        FinalConflagration.registerAll(plugin, attackRegistry);
    }

    private void registerPhase3BossAttacks() {
        StalkerStrike.registerAll(plugin, attackRegistry);
        StalkerAssault.registerAll(plugin, attackRegistry);
        HunterStrike.registerAll(plugin, attackRegistry);
        HunterAssault.registerAll(plugin, attackRegistry);
        WrathStrike.registerAll(plugin, attackRegistry);
        WrathAssault.registerAll(plugin, attackRegistry);
        InfernoStrike.registerAll(plugin, attackRegistry);
        InfernoAssault.registerAll(plugin, attackRegistry);
        LastHuntStrike.registerAll(plugin, attackRegistry);
        LastHuntAssault.registerAll(plugin, attackRegistry);
    }

    private void registerPhase4BlockDisplayAttacks() {
        DragonThrone.registerAll(plugin, attackRegistry);
        BladeRingArrays.registerAll(plugin, attackRegistry);
        AmethystCrystalArrays.registerAll(plugin, attackRegistry);
        RiftPanels.registerAll(plugin, attackRegistry);
        DragonWingShadows.registerAll(plugin, attackRegistry);
        OrbitingCelestials.registerAll(plugin, attackRegistry);
        DragonCrystalHorns.registerAll(plugin, attackRegistry);
        NetherStarArrays.registerAll(plugin, attackRegistry);
        DragonsMemory.registerAll(plugin, attackRegistry);
        PhaseTransitionArch.registerAll(plugin, attackRegistry);
    }

    private void registerPhase4EnvironmentalAttacks() {
        VoidTearEvents.registerAll(plugin, attackRegistry);
        CrystalVeinEvents.registerAll(plugin, attackRegistry);
        BrimstoneScars.registerAll(plugin, attackRegistry);
        SanctumCollapseEvents.registerAll(plugin, attackRegistry);
        EssenceStorms.registerAll(plugin, attackRegistry);
        StreamConvergence.registerAll(plugin, attackRegistry);
        ArenaDegradation.registerAll(plugin, attackRegistry);
        EssenceEcho.registerAll(plugin, attackRegistry);
        SanctumWrath.registerAll(plugin, attackRegistry);
        FinalFracture.registerAll(plugin, attackRegistry);
    }

    private void registerPhase4BossAttacks() {
        // Dragon Phase 1
        DragonPhase1A.registerAll(plugin, attackRegistry);
        DragonPhase1B.registerAll(plugin, attackRegistry);
        DragonPhase1C.registerAll(plugin, attackRegistry);
        DragonPhase1D.registerAll(plugin, attackRegistry);
        // Dragon Phase 2
        DragonPhase2A.registerAll(plugin, attackRegistry);
        DragonPhase2B.registerAll(plugin, attackRegistry);
        DragonPhase2C.registerAll(plugin, attackRegistry);
        // Dragon Phase 3
        DragonPhase3A.registerAll(plugin, attackRegistry);
        DragonPhase3B.registerAll(plugin, attackRegistry);
        DragonPhase3C.registerAll(plugin, attackRegistry);
        // Mini Bosses
        VoidShardAttacks.registerAll(plugin, attackRegistry);
        CrystalWardenAttacks.registerAll(plugin, attackRegistry);
        EmberWraithAttacks.registerAll(plugin, attackRegistry);
        SanctumSentinelAttacks.registerAll(plugin, attackRegistry);
    }

    private void registerPhase5BlockDisplayAttacks() {
        CalamitasAltar.registerAll(plugin, attackRegistry);
        TridentArrays.registerAll(plugin, attackRegistry);
        BrimstoneSkullArrays.registerAll(plugin, attackRegistry);
        PhaseTransformStructures.registerAll(plugin, attackRegistry);
        CalamitasAura.registerAll(plugin, attackRegistry);
        TridentStormArrays.registerAll(plugin, attackRegistry);
        CrimsonGeometry.registerAll(plugin, attackRegistry);
        SoulArchitecture.registerAll(plugin, attackRegistry);
        DynamicSkyAnchors.registerAll(plugin, attackRegistry);
        FinalSequenceArch.registerAll(plugin, attackRegistry);
    }

    private void registerPhase5EnvironmentalAttacks() {
        CalamityDawnA.registerAll(plugin, attackRegistry);
        CalamityDawnB.registerAll(plugin, attackRegistry);
        CalamityDawnC.registerAll(plugin, attackRegistry);
        CalamityDawnD.registerAll(plugin, attackRegistry);
        CalamityDawnE.registerAll(plugin, attackRegistry);
        TotalChaosA.registerAll(plugin, attackRegistry);
        TotalChaosB.registerAll(plugin, attackRegistry);
        TotalChaosC.registerAll(plugin, attackRegistry);
        TotalChaosD.registerAll(plugin, attackRegistry);
        TotalChaosE.registerAll(plugin, attackRegistry);
        TridentEnvironmentA.registerAll(plugin, attackRegistry);
        TridentEnvironmentB.registerAll(plugin, attackRegistry);
        TridentEnvironmentC.registerAll(plugin, attackRegistry);
        ApocalypseA.registerAll(plugin, attackRegistry);
        ApocalypseB.registerAll(plugin, attackRegistry);
        FinalCalamityA.registerAll(plugin, attackRegistry);
        FinalCalamityB.registerAll(plugin, attackRegistry);
        FinalCalamityC.registerAll(plugin, attackRegistry);
        FinalCalamityD.registerAll(plugin, attackRegistry);
        FinalCalamityE.registerAll(plugin, attackRegistry);
    }

    private void registerPhase5BossAttacks() {
        CalamitasArrivalA.registerAll(plugin, attackRegistry);
        CalamitasArrivalB.registerAll(plugin, attackRegistry);
        CalamitasArrivalC.registerAll(plugin, attackRegistry);
        CalamitasArrivalD.registerAll(plugin, attackRegistry);
        CalamitasArrivalE.registerAll(plugin, attackRegistry);
        CalamitasEternityA.registerAll(plugin, attackRegistry);
        CalamitasEternityB.registerAll(plugin, attackRegistry);
        CalamitasEternityC.registerAll(plugin, attackRegistry);
        CalamitasEternityD.registerAll(plugin, attackRegistry);
        CalamitasEternityE.registerAll(plugin, attackRegistry);
        CalamitasUnmakingA.registerAll(plugin, attackRegistry);
        CalamitasUnmakingB.registerAll(plugin, attackRegistry);
        CalamitasUnmakingC.registerAll(plugin, attackRegistry);
        CalamitasUnmakingD.registerAll(plugin, attackRegistry);
        CalamitasUnmakingE.registerAll(plugin, attackRegistry);
        CalamitasEndA.registerAll(plugin, attackRegistry);
        CalamitasEndB.registerAll(plugin, attackRegistry);
        CalamitasEndC.registerAll(plugin, attackRegistry);
        CalamitasEndD.registerAll(plugin, attackRegistry);
        CalamitasEndE.registerAll(plugin, attackRegistry);
    }

    // ========================
    // Lifecycle
    // ========================

    @Override
    public void onStart() {
        currentPhase = 0;
        bossAlive = false;
        waitingForDeposit = false;
        tickCounter = 0;
        eggOrbitAngle = 0;

        plugin.getLogger().info("[Calamity] Mode starting — initializing systems...");

        World endWorld = getEndWorld();
        if (endWorld == null) {
            plugin.getLogger().severe("[Calamity] Cannot start — The End world not found! " +
                    "Make sure The End dimension is enabled in bukkit.yml and loaded.");
            return;
        }

        // 1. Remove dragon egg from portal and get its location
        Location eggLocation = eggDetector.removeEggFromPortal(endWorld);
        if (eggLocation == null) {
            plugin.getLogger().warning("[Calamity] Dragon egg not found at portal — using default End spawn location.");
            eggLocation = new Location(endWorld, 0.5, 68, 0.5);
        }

        // 2. Teleport all players to The End
        portalManager.start();
        int playerCount = plugin.getServer().getOnlinePlayers().size();
        portalManager.teleportAllToEnd();
        plugin.getLogger().info("[Calamity] Teleporting " + playerCount + " players to The End.");

        if (playerCount == 0) {
            plugin.getLogger().warning("[Calamity] No players online — events will not spawn until someone joins The End.");
        }

        // 3. Spawn ItemsAdder portal blocks (if available)
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            portalManager.spawnItemsAdderPortals();
        } else {
            plugin.getLogger().info("[Calamity] ItemsAdder not found — skipping portal block spawning.");
        }

        // 4. Start the animated egg BlockDisplay
        eggAnimation.start(eggLocation);

        // 5. Spawn the invincible Ender Dragon
        dragonManager.spawnInvincibleDragon();

        // 6. Start the gem system
        gemManager.start();

        // 7. Register phase music
        registerPhaseMusic();

        // 8. Play initial mode music
        plugin.getMusicManager().playModeMusic(this);

        // 9. Load attack configs and start scheduler
        attackRegistry.reloadConfigs();

        // Validate attack registry
        if (attackRegistry.size() == 0) {
            plugin.getLogger().severe("[Calamity] WARNING: 0 attacks registered! Check attack registration in CalamityMode constructor.");
        } else {
            // Log per-phase counts
            for (int p = 1; p <= 5; p++) {
                int bd = attackRegistry.getByPhaseAndType(p, com.blockforge.chaoscraft.modes.calamity.attacks.AttackType.BLOCK_DISPLAY).size();
                int env = attackRegistry.getByPhaseAndType(p, com.blockforge.chaoscraft.modes.calamity.attacks.AttackType.ENVIRONMENTAL).size();
                int boss = attackRegistry.getByPhaseAndType(p, com.blockforge.chaoscraft.modes.calamity.attacks.AttackType.BOSS).size();
                plugin.getLogger().info("[Calamity] Phase " + p + ": " + bd + " BD, " + env + " ENV, " + boss + " BOSS attacks");
            }
        }

        // 10. Advance to Phase 1 (pre-boss gem collection)
        advanceToPhase(1);

        plugin.getLogger().info("[Calamity] Mode fully started. Phase 1 — collecting gems for Voidmaw."
                + " (" + attackRegistry.size() + " attacks registered)");
    }

    @Override
    public void onTick() {
        tickCounter++;

        // Orbit the egg around the arena
        if (eggAnimation.isActive()) {
            eggOrbitAngle += EGG_ORBIT_ARENA_SPEED;
            World endWorld = getEndWorld();
            if (endWorld != null) {
                double x = Math.cos(eggOrbitAngle) * EGG_ORBIT_ARENA_RADIUS;
                double z = Math.sin(eggOrbitAngle) * EGG_ORBIT_ARENA_RADIUS;
                double y = 72 + Math.sin(eggOrbitAngle * 2) * 3; // Gentle vertical bob
                eggAnimation.updateCenter(new Location(endWorld, x, y, z));
            }
        }

        // Check if we have enough deposited gems to spawn the next boss
        if (waitingForDeposit && !bossAlive && gemManager.hasEnoughDeposited()) {
            waitingForDeposit = false;
            spawnCurrentPhaseBoss();
        }

        // Tick the attack scheduler (spawns block displays, environmental, boss attacks)
        attackScheduler.tick();
    }

    @Override
    public void onEnd() {
        plugin.getLogger().info("[Calamity] Mode ending — cleaning up...");

        // Stop all sub-systems
        attackScheduler.stop();
        gemManager.stop();
        eggAnimation.stop();
        dragonManager.stop();
        portalManager.removeItemsAdderPortals();
        portalManager.stop();

        // Stop music
        plugin.getMusicManager().stopAll();
        plugin.getMusicManager().clearPhaseMusic();

        // Reset state
        currentPhase = 0;
        bossAlive = false;
        waitingForDeposit = false;

        plugin.getLogger().info("[Calamity] Mode ended. All systems cleaned up.");
    }

    @Override
    public void onPlayerJoin(Player player) {
        // Teleport joining player to The End
        portalManager.teleportJoiningPlayer(player);
        // Replay current music
        plugin.getMusicManager().playForPlayer(player);
    }

    @Override
    public void onPlayerDeath(Player player) {
        // Player respawn is handled by PortalManager (forces End respawn)
        plugin.debug("[Calamity] " + player.getName() + " died. Respawning in The End.");
    }

    @Override
    public void onPlayerChangeDimension(Player player) {
        // Music replayed via MusicManager listener
    }

    // ========================
    // Phase management
    // ========================

    /**
     * Advance to the next phase. Sets gem requirement, starts egg charging.
     */
    private void advanceToPhase(int phase) {
        if (phase < 1 || phase > 5) return;

        currentPhase = phase;
        String bossName = BOSS_NAMES[phase - 1];
        int gemReq = calamityConfig.getBossGemRequirement(bossName);

        plugin.getLogger().info("[Calamity] Phase " + phase + " — need " + gemReq + " gems for " + bossName);

        // Reset deposit counter for this phase
        gemManager.resetDeposits();
        gemManager.setCurrentGemRequirement(gemReq);
        gemManager.setBossActive(false);
        gemManager.setBetweenBosses(phase > 1); // Between bosses after phase 1

        // Start egg charging animation
        if (phase > 1) {
            eggAnimation.startCharging();
        }

        waitingForDeposit = true;

        // Switch music to this phase
        plugin.getMusicManager().playPhaseMusic(phase);

        // Start attack scheduler for this phase — always restart to reset cooldowns/counts
        attackScheduler.start(phase);
    }

    /**
     * Spawn the boss for the current phase.
     */
    private void spawnCurrentPhaseBoss() {
        if (currentPhase < 1 || currentPhase > 5) return;

        String bossName = BOSS_NAMES[currentPhase - 1];
        plugin.getLogger().info("[Calamity] Spawning boss: " + bossName + " (Phase " + currentPhase + ")");

        bossAlive = true;
        gemManager.setBossActive(true);
        gemManager.setBetweenBosses(false);
        attackScheduler.setBossActive(true, bossName);

        // Play boss spawn explosion on the egg
        eggAnimation.stopCharging();
        eggAnimation.playBossSpawnExplosion(() -> {
            // After explosion animation completes, actually spawn the boss
            switch (currentPhase) {
                case 1 -> spawnVoidmaw();
                case 2 -> spawnDoG();
                case 3 -> spawnDweller();
                case 4 -> startDragonPhase();
                case 5 -> spawnSupremeCalamitas();
            }
        });
    }

    /**
     * Called when a boss is defeated. Advances to the next phase.
     */
    public void onBossDefeated() {
        bossAlive = false;
        attackScheduler.setBossActive(false, null);

        if (currentPhase >= 5) {
            // Final boss defeated — Calamity mode complete!
            plugin.getLogger().info("[Calamity] Supreme Calamitas defeated! Calamity mode COMPLETE!");
            // The ModeManager will handle rewards and end commands
            plugin.getModeManager().endActiveMode();
            return;
        }

        // Advance to next phase
        advanceToPhase(currentPhase + 1);
    }

    // ========================
    // Boss spawning (Phase 2+ implementations)
    // ========================

    /**
     * Phase 1 boss: Voidmaw — spawned via MythicMobs.
     */
    private void spawnVoidmaw() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        String mythicId = calamityConfig.getBossMythicId("voidmaw");
        Location spawnLoc = calamityConfig.getBossSpawnLocation("voidmaw", endWorld);

        if (mythicId.isEmpty()) {
            plugin.getLogger().warning("[Calamity] Voidmaw MythicMobs ID not configured!");
            return;
        }

        // Spawn via MythicMobs API (soft dependency)
        spawnMythicMob(mythicId, spawnLoc);
        plugin.getLogger().info("[Calamity] Voidmaw spawned at " + formatLocation(spawnLoc));

        // TODO Phase 4: Track Voidmaw death via MythicMobs death event → onBossDefeated()
    }

    /**
     * Phase 2 boss: Devourer of Gods — spawned via datapack command.
     */
    private void spawnDoG() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        Location spawnLoc = calamityConfig.getBossSpawnLocation("dog", endWorld);

        // Spawn DoG via datapack command
        plugin.getServer().dispatchCommand(
                plugin.getServer().getConsoleSender(),
                "function calamity:summon_dog"
        );

        plugin.getLogger().info("[Calamity] DoG summoned via datapack at " + formatLocation(spawnLoc));

        // TODO Phase 5: DoG health monitoring, phase 2 trigger at 6M HP
        // TODO Phase 5: DoG death detection → onBossDefeated()
    }

    /**
     * Phase 3 boss: Dweller — spawned via MythicMobs.
     */
    private void spawnDweller() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        String mythicId = calamityConfig.getBossMythicId("dweller");
        Location spawnLoc = calamityConfig.getBossSpawnLocation("dweller", endWorld);

        if (mythicId.isEmpty()) {
            plugin.getLogger().warning("[Calamity] Dweller MythicMobs ID not configured!");
            return;
        }

        spawnMythicMob(mythicId, spawnLoc);
        plugin.getLogger().info("[Calamity] Dweller spawned at " + formatLocation(spawnLoc));

        // TODO Phase 4: Track Dweller death via MythicMobs death event → onBossDefeated()
    }

    /**
     * Phase 4: Ender Dragon becomes fightable + mini bosses spawn.
     */
    private void startDragonPhase() {
        plugin.getLogger().info("[Calamity] Phase 4 — Dragon fight begins!");

        // Spawn mini bosses first (dragon invincible while they live)
        int miniBossCount = calamityConfig.getDragonMiniBossCount();
        var miniBossIds = calamityConfig.getDragonMiniBossIds();
        int spawnRadius = calamityConfig.getDragonMiniBossSpawnRadius();

        dragonManager.setAliveMiniBosse(miniBossCount);

        World endWorld = getEndWorld();
        if (endWorld != null && !miniBossIds.isEmpty()) {
            var players = endWorld.getPlayers();
            for (int i = 0; i < miniBossCount && i < miniBossIds.size(); i++) {
                String mythicId = miniBossIds.get(i % miniBossIds.size());
                // Spawn near a random player
                if (!players.isEmpty()) {
                    Player target = players.get(i % players.size());
                    double angle = Math.random() * Math.PI * 2;
                    Location spawnLoc = target.getLocation().add(
                            Math.cos(angle) * spawnRadius,
                            0,
                            Math.sin(angle) * spawnRadius
                    );
                    spawnMythicMob(mythicId, spawnLoc);
                }
            }
        }

        // Make dragon vulnerable (but shielded while mini bosses alive)
        dragonManager.makeVulnerable();

        // Set dragon death callback
        dragonManager.setOnDragonDeath(this::onBossDefeated);

        // TODO Phase 4: Mini boss death tracking → dragonManager.decrementMiniBoss()
    }

    /**
     * Phase 5: Supreme Calamitas — custom NMS AI entity.
     */
    private void spawnSupremeCalamitas() {
        World endWorld = getEndWorld();
        if (endWorld == null) return;

        Location spawnLoc = calamityConfig.getBossSpawnLocation("calamitas", endWorld);

        plugin.getLogger().info("[Calamity] Phase 5 — Supreme Calamitas awakens!");

        // TODO Phase 6: NMS custom entity spawn with Citizens + custom AI
        // TODO Phase 6: ModelEngine model overlay
        // TODO Phase 6: Configurable held item, fly speed, detection range
        // For now, log placeholder
        plugin.getLogger().info("[Calamity] Supreme Calamitas spawn at " + formatLocation(spawnLoc)
                + " — awaiting Phase 6 NMS implementation.");
    }

    // ========================
    // MythicMobs integration
    // ========================

    /**
     * Spawn a MythicMobs mob at the given location.
     * Uses reflection to avoid hard dependency.
     */
    private void spawnMythicMob(String mythicId, Location location) {
        if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.getLogger().severe("[Calamity] MythicMobs is NOT installed! Cannot spawn boss '" + mythicId + "'. "
                    + "Install MythicMobs from https://mythiccraft.io and configure the mob ID in calamity.yml");
            return;
        }

        if (mythicId == null || mythicId.isEmpty()) {
            plugin.getLogger().severe("[Calamity] MythicMobs mob ID is empty! "
                    + "Configure the boss mob ID in plugins/ChaosCraft/calamity.yml under bosses.<bossname>.mythic-id");
            return;
        }

        try {
            // MythicMobs 5.x API via reflection
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicBukkit = mythicBukkitClass.getMethod("inst").invoke(null);
            Object mobManager = mythicBukkitClass.getMethod("getMobManager").invoke(mythicBukkit);

            // MobManager.spawnMob(String, Location)
            var spawnMethod = mobManager.getClass().getMethod("spawnMob", String.class, Location.class);
            Object result = spawnMethod.invoke(mobManager, mythicId, location);

            if (result == null) {
                plugin.getLogger().severe("[Calamity] MythicMobs returned null when spawning '" + mythicId + "'! "
                        + "Check that the mob ID exists in your MythicMobs mobs/ folder. "
                        + "Run /mm mobs to see all available mob types.");
            } else {
                plugin.getLogger().info("[Calamity] Successfully spawned MythicMob: " + mythicId + " at " + formatLocation(location));
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[Calamity] MythicMobs API class not found — wrong MythicMobs version? "
                    + "ChaosCraft requires MythicMobs 5.x+ (io.lumine.mythic.bukkit.MythicBukkit)");
        } catch (NoSuchMethodException e) {
            plugin.getLogger().severe("[Calamity] MythicMobs API method not found: " + e.getMessage()
                    + " — MythicMobs version may be incompatible. Requires 5.x+");
        } catch (Exception e) {
            plugin.getLogger().severe("[Calamity] Failed to spawn MythicMob '" + mythicId + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================
    // Music registration
    // ========================

    private void registerPhaseMusic() {
        var musicManager = plugin.getMusicManager();
        musicManager.clearPhaseMusic();

        // Register per-phase music overrides from config
        for (var entry : calamityConfig.getPhaseMusicOverrides().entrySet()) {
            int phase = entry.getKey();
            var music = entry.getValue();
            if (!music.soundId().isEmpty()) {
                musicManager.setPhaseMusic(phase, music.soundId(), music.loop(), music.durationTicks());
            }
        }

        // DoG Phase 2 special music (uses a special phase number: 22)
        var dogP2 = calamityConfig.getDogPhase2Music();
        if (!dogP2.soundId().isEmpty()) {
            musicManager.setPhaseMusic(22, dogP2.soundId(), dogP2.loop(), dogP2.durationTicks());
        }
    }

    // ========================
    // Calamity-specific API
    // ========================

    public int getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(int phase) {
        this.currentPhase = phase;
        plugin.getMusicManager().playPhaseMusic(phase);
        plugin.getLogger().info("[Calamity] Phase changed to: " + phase);
    }

    public int getGemCount() {
        return gemManager.getGlobalGemCount();
    }

    public void addGems(int amount) {
        gemManager.setGlobalGemCount(gemManager.getGlobalGemCount() + amount);
        plugin.getModeTimer().addTime(amount);
    }

    public void setGemCount(int count) {
        gemManager.setGlobalGemCount(count);
    }

    public boolean isBossAlive() {
        return bossAlive;
    }

    public CalamityConfig getCalamityConfig() {
        return calamityConfig;
    }

    public DragonEggDetector getEggDetector() {
        return eggDetector;
    }

    public GemManager getGemManager() {
        return gemManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public EggAnimation getEggAnimation() {
        return eggAnimation;
    }

    public DoGManager getDoGManager() {
        return dogManager;
    }

    public CalamitasManager getCalamitasManager() {
        return calamitasManager;
    }

    public DragonManager getDragonManager() {
        return dragonManager;
    }

    public AttackRegistry getAttackRegistry() {
        return attackRegistry;
    }

    public AttackScheduler getAttackScheduler() {
        return attackScheduler;
    }

    // ========================
    // Helpers
    // ========================

    private World getEndWorld() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) return world;
        }
        return null;
    }

    private String formatLocation(Location loc) {
        return String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());
    }
}
