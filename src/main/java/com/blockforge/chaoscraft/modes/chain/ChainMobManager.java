package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.NavigatorParameters;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

/**
 * Chain Mode — NPC Mob Manager
 *
 * Spawns Citizens NPCs with chain-themed skins, zombie-like AI,
 * and custom chain block display skills (hook, pull, lash, slam).
 *
 * NPCs use Citizens' Navigator for pathfinding toward the nearest
 * non-exempt player. When within attack range they execute chain
 * skills using block displays.
 *
 * All behavior is configurable via chain.yml under the "mobs" section.
 */
public class ChainMobManager {

    private final ChaosCraftPlugin plugin;
    private final ChainConfig config;

    // Active NPCs tracked by Citizens NPC ID
    private final List<Integer> activeNpcIds = new ArrayList<>();
    private final Map<Integer, UUID> npcTargets = new HashMap<>(); // NPC ID -> target player UUID
    private final Map<Integer, Integer> npcSkillCooldowns = new HashMap<>(); // NPC ID -> remaining cooldown
    private final Map<Integer, List<Entity>> npcBlockDisplays = new HashMap<>(); // NPC ID -> active display entities

    private boolean citizensAvailable = false;
    private int spawnTickCounter = 0;
    private int skillTickCounter = 0;

    // Skin texture (loaded from config or file)
    private String skinTexture = null;
    private String skinSignature = null;

    public ChainMobManager(ChaosCraftPlugin plugin, ChainConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.citizensAvailable = Bukkit.getPluginManager().getPlugin("Citizens") != null;

        if (!citizensAvailable) {
            plugin.getLogger().warning("[Chain] Citizens not found — chain mobs disabled.");
        }
    }

    public boolean isCitizensAvailable() {
        return citizensAvailable;
    }

    // ========================
    // Lifecycle
    // ========================

    /**
     * Called every tick during Chain Mode.
     */
    public void tick(World world) {
        if (!citizensAvailable || !config.areMobsEnabled()) return;

        // Clean up dead/despawned NPCs
        cleanupDead();

        // Tick skill cooldowns
        tickCooldowns();

        // Spawn check
        spawnTickCounter++;
        if (spawnTickCounter >= config.getMobSpawnIntervalTicks()) {
            spawnTickCounter = 0;
            attemptSpawn(world);
        }

        // AI tick — pathfinding + skill execution
        skillTickCounter++;
        if (skillTickCounter >= 5) { // AI updates every 5 ticks
            skillTickCounter = 0;
            tickAI(world);
        }
    }

    /**
     * Remove all NPCs on mode end.
     */
    public void cleanup() {
        if (!citizensAvailable) return;

        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();
            for (int npcId : new ArrayList<>(activeNpcIds)) {
                NPC npc = registry.getById(npcId);
                if (npc != null) {
                    // Clean up block displays
                    cleanupNpcDisplays(npcId);
                    npc.despawn();
                    npc.destroy();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Chain] Error cleaning up mobs: " + e.getMessage());
        }

        activeNpcIds.clear();
        npcTargets.clear();
        npcSkillCooldowns.clear();
        npcBlockDisplays.clear();
    }

    // ========================
    // Spawning
    // ========================

    private void attemptSpawn(World world) {
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;

        // Count non-exempt players
        List<Player> validPlayers = new ArrayList<>();
        for (Player p : players) {
            if (!isExempt(p)) validPlayers.add(p);
        }
        if (validPlayers.isEmpty()) return;

        // Check max mob cap
        int maxTotal = config.getMobMaxTotal();
        if (activeNpcIds.size() >= maxTotal) return;

        // Check per-player cap
        Player target = validPlayers.get(new Random().nextInt(validPlayers.size()));
        int mobsNearTarget = countMobsNear(target, 30);
        if (mobsNearTarget >= config.getMobMaxPerPlayer()) return;

        // Spawn location: random position around the target
        double spawnDist = config.getMobSpawnDistance();
        double angle = Math.random() * Math.PI * 2;
        Location spawnLoc = target.getLocation().clone().add(
                Math.cos(angle) * spawnDist, 0, Math.sin(angle) * spawnDist);
        // Find safe Y (ground level)
        spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

        spawnNpc(spawnLoc, target);
    }

    private void spawnNpc(Location location, Player target) {
        try {
            NPCRegistry registry = CitizensAPI.getNPCRegistry();

            // Create NPC with ZOMBIE entity type but PLAYER appearance
            NPC npc = registry.createNPC(EntityType.PLAYER, config.getMobDisplayName());
            npc.setProtected(false); // Can take damage

            // Set skin from config — supports URL or player name
            String skinUrl = config.getMobSkinUrl();
            String skinName = config.getMobSkinPlayerName();
            if (!skinUrl.isEmpty()) {
                // HTTP/HTTPS URL — Citizens sends to mineskin.org for processing
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "npc select " + npc.getId());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "npc skin --url " + skinUrl);
                }, 5L);
            } else if (!skinName.isEmpty()) {
                // Use a player name for skin lookup via Citizens data
                npc.data().setPersistent("player-skin-name", skinName);
            }

            // Spawn the NPC
            npc.spawn(location);

            if (npc.getEntity() instanceof LivingEntity living) {
                // Set health
                double maxHealth = config.getMobHealth();
                living.setMaxHealth(maxHealth);
                living.setHealth(maxHealth);

                // Set movement speed via attribute
                var speedAttr = living.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                if (speedAttr != null) {
                    speedAttr.setBaseValue(config.getMobSpeed());
                }

                // Equipment — chain themed
                if (living instanceof Player || living.getEquipment() != null) {
                    var equipment = living.getEquipment();
                    if (equipment != null) {
                        equipment.setItemInMainHand(new ItemStack(Material.CHAIN));
                        equipment.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                        equipment.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                        equipment.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                        equipment.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
                    }
                }

                // Disable NPC default behaviors — we control AI
                npc.getDefaultGoalController().clear();
                npc.getDefaultGoalController().setPaused(true);
            }

            // Configure navigator
            Navigator nav = npc.getNavigator();
            NavigatorParameters params = nav.getLocalParameters();
            params.speedModifier((float) config.getMobSpeed());
            params.range((float) config.getMobDetectionRange());
            params.attackRange(config.getMobAttackRange());
            params.stuckAction(null); // Don't teleport when stuck

            // Track
            activeNpcIds.add(npc.getId());
            npcTargets.put(npc.getId(), target.getUniqueId());
            npcSkillCooldowns.put(npc.getId(), 0);

            // Spawn particles
            DisplayBuilder.playSound(location, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
            location.getWorld().spawnParticle(Particle.SMOKE, location, 15, 0.5, 1, 0.5, 0.03);
            DisplayBuilder.dustParticles(location, 8, 0.5, 150, 150, 160, 1.5f);

            plugin.debug("[Chain] Spawned chain mob NPC #" + npc.getId() + " near " + target.getName());

        } catch (Exception e) {
            plugin.getLogger().warning("[Chain] Failed to spawn chain mob: " + e.getMessage());
        }
    }

    // ========================
    // AI Tick
    // ========================

    private void tickAI(World world) {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();

        for (int npcId : new ArrayList<>(activeNpcIds)) {
            NPC npc = registry.getById(npcId);
            if (npc == null || !npc.isSpawned()) continue;

            Entity entity = npc.getEntity();
            if (entity == null || entity.isDead()) continue;

            // Find/update target — nearest non-exempt player
            Player target = findNearestTarget(entity.getLocation(), world);
            if (target == null) continue;
            npcTargets.put(npcId, target.getUniqueId());

            double distance = entity.getLocation().distance(target.getLocation());

            // Navigate toward target if out of attack range
            if (distance > config.getMobAttackRange()) {
                Navigator nav = npc.getNavigator();
                if (!nav.isNavigating() || nav.getEntityTarget() == null
                        || !nav.getEntityTarget().getTarget().getUniqueId().equals(target.getUniqueId())) {
                    nav.setTarget(target, false); // non-aggressive — we handle damage ourselves
                }
            }

            // Execute chain skill if in range and off cooldown
            int cooldown = npcSkillCooldowns.getOrDefault(npcId, 0);
            if (distance <= config.getMobAttackRange() + 2 && cooldown <= 0) {
                executeChainSkill(npc, target, distance);
                npcSkillCooldowns.put(npcId, config.getMobSkillCooldownTicks());
            }

            // Ambient chain particles on the NPC
            if (skillTickCounter % 4 == 0) {
                Location npcLoc = entity.getLocation().clone().add(0, 1, 0);
                entity.getWorld().spawnParticle(Particle.SMOKE, npcLoc, 2, 0.2, 0.3, 0.2, 0.01);
                DisplayBuilder.dustParticles(npcLoc, 1, 0.3, 150, 150, 160, 0.8f);
            }
        }
    }

    // ========================
    // Chain Skills
    // ========================

    private void executeChainSkill(NPC npc, Player target, double distance) {
        int skill = new Random().nextInt(4);
        Location npcLoc = npc.getEntity().getLocation();
        Location targetLoc = target.getLocation();

        switch (skill) {
            case 0 -> executeHookAndPull(npc, target, npcLoc, targetLoc);
            case 1 -> executeChainLash(npc, target, npcLoc, targetLoc);
            case 2 -> executeChainSlam(npc, npcLoc);
            case 3 -> executeChainSnare(npc, target, npcLoc, targetLoc);
        }
    }

    /**
     * HOOK & PULL — Chain extends from NPC to player, then pulls player toward NPC.
     * Visual: line of chain block displays connecting NPC to player.
     */
    private void executeHookAndPull(NPC npc, Player target, Location npcLoc, Location targetLoc) {
        World w = npcLoc.getWorld();
        if (w == null) return;

        DisplayBuilder builder = new DisplayBuilder(plugin);

        // Chain line from NPC to player
        double dist = npcLoc.distance(targetLoc);
        if (dist < 0.5) return; // Too close — normalize would produce NaN
        int chainCount = Math.min(12, (int)(dist / 0.8));
        List<Entity> displays = new ArrayList<>();

        for (int i = 0; i < chainCount; i++) {
            double t = (double) i / chainCount;
            Location chainLoc = npcLoc.clone().add(
                    (targetLoc.getX() - npcLoc.getX()) * t,
                    1.2 + (targetLoc.getY() - npcLoc.getY()) * t,
                    (targetLoc.getZ() - npcLoc.getZ()) * t);
            BlockDisplayHandle chain = builder.spawnBlock(chainLoc, Material.CHAIN);
            chain.scale(0.4f, 0.8f, 0.4f).glow(180, 180, 190).interpolation(2, 0);
            displays.add(chain.entity());
        }

        npcBlockDisplays.put(npc.getId(), displays);

        // Pull player toward NPC
        Vector pull = npcLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(0.8).setY(0.3);
        target.setVelocity(target.getVelocity().add(pull));

        // Deal damage (respects armor/resistance, no PvP trigger)
        applyDamage(target, config.getMobDamage(), npc);

        // Sound + particles
        DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        DisplayBuilder.playSound(npcLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 0.4f);
        DisplayBuilder.particleLine(npcLoc.clone().add(0, 1.2, 0), targetLoc.clone().add(0, 1, 0),
                Particle.CRIT, 3, null);

        // Schedule cleanup of chain displays after 30 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity e : displays) {
                if (e != null && e.isValid()) e.remove();
            }
            npcBlockDisplays.remove(npc.getId());
        }, 30L);

        plugin.debug("[Chain] NPC #" + npc.getId() + " used HOOK & PULL on " + target.getName());
    }

    /**
     * CHAIN LASH — Whip-like chain sweeps in an arc from the NPC toward the player.
     * Visual: arc of chain blocks that swing through.
     */
    private void executeChainLash(NPC npc, Player target, Location npcLoc, Location targetLoc) {
        World w = npcLoc.getWorld();
        if (w == null) return;

        double dist = npcLoc.distance(targetLoc);
        if (dist < 0.5) return; // Too close — normalize would produce NaN

        DisplayBuilder builder = new DisplayBuilder(plugin);
        List<Entity> displays = new ArrayList<>();

        // Direction from NPC to target
        Vector dir = targetLoc.toVector().subtract(npcLoc.toVector()).normalize();
        double baseAngle = Math.atan2(dir.getZ(), dir.getX());

        // Arc of 8 chain blocks sweeping 90 degrees
        for (int i = 0; i < 8; i++) {
            double angle = baseAngle - 0.8 + (1.6 * i / 7);
            double arcDist = 2.0 + i * 0.3;
            Location chainLoc = npcLoc.clone().add(
                    Math.cos(angle) * arcDist, 1.0 + Math.sin(i * 0.3) * 0.5, Math.sin(angle) * arcDist);
            BlockDisplayHandle chain = builder.spawnBlock(chainLoc, Material.CHAIN);
            chain.scale(0.5f, 1.5f, 0.5f).glow(200, 200, 210).interpolation(2, 0);
            displays.add(chain.entity());
        }

        npcBlockDisplays.put(npc.getId(), displays);

        // Damage + knockback
        applyDamage(target, config.getMobDamage() * 0.8, npc);

        // Knockback away from NPC
        Vector knockback = targetLoc.toVector().subtract(npcLoc.toVector()).normalize().multiply(0.6).setY(0.2);
        target.setVelocity(target.getVelocity().add(knockback));

        DisplayBuilder.playSound(npcLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.4f);
        DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.6f);

        // Cleanup after 20 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity e : displays) { if (e != null && e.isValid()) e.remove(); }
            npcBlockDisplays.remove(npc.getId());
        }, 20L);
    }

    /**
     * CHAIN SLAM — NPC slams chains into the ground creating a shockwave ring.
     * Visual: ring of chain blocks + ground particles.
     */
    private void executeChainSlam(NPC npc, Location npcLoc) {
        World w = npcLoc.getWorld();
        if (w == null) return;

        DisplayBuilder builder = new DisplayBuilder(plugin);
        List<Entity> displays = new ArrayList<>();

        // Ring of chain blocks on the ground
        List<BlockDisplayHandle> ring = builder.spawnRing(npcLoc.clone().add(0, 0.3, 0),
                Material.CHAIN, 3.0, 10);
        for (BlockDisplayHandle h : ring) {
            h.scale(0.5f, 2.0f, 0.5f).glow(180, 180, 190);
            displays.add(h.entity());
        }

        npcBlockDisplays.put(npc.getId(), displays);

        // Damage all non-exempt players in radius
        double slamRadius = 4.0;
        double damage = config.getMobDamage() * 1.2;
        for (Player p : w.getPlayers()) {
            if (isExempt(p)) continue;
            if (p.getLocation().distanceSquared(npcLoc) <= slamRadius * slamRadius) {
                p.damage(damage);
                p.setNoDamageTicks(0);
                // Launch upward
                p.setVelocity(p.getVelocity().add(new Vector(0, 0.6, 0)));
            }
        }

        // Ground impact effects
        DisplayBuilder.playSound(npcLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
        DisplayBuilder.playSound(npcLoc, Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.3f);
        w.spawnParticle(Particle.BLOCK, npcLoc, 20, 2, 0.3, 2, 0.1,
                Material.IRON_BLOCK.createBlockData());
        DisplayBuilder.particleRing(npcLoc, slamRadius, Particle.CRIT, 20, null);

        // Cleanup after 40 ticks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Entity e : displays) { if (e != null && e.isValid()) e.remove(); }
            npcBlockDisplays.remove(npc.getId());
        }, 40L);
    }

    /**
     * CHAIN SNARE — Chains wrap around the player, pulling them toward the NPC
     * continuously for 2 seconds. Player can't escape easily.
     */
    private void executeChainSnare(NPC npc, Player target, Location npcLoc, Location targetLoc) {
        World w = npcLoc.getWorld();
        if (w == null) return;

        DisplayBuilder builder = new DisplayBuilder(plugin);
        List<Entity> displays = new ArrayList<>();

        // Ring of chains around the player
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI * i) / 8;
            Location chainLoc = targetLoc.clone().add(Math.cos(angle) * 1.5, 0.5 + i * 0.15, Math.sin(angle) * 1.5);
            BlockDisplayHandle chain = builder.spawnBlock(chainLoc, Material.CHAIN);
            chain.scale(0.3f, 1.0f, 0.3f).glow(200, 200, 220).interpolation(3, 0);
            displays.add(chain.entity());
        }

        npcBlockDisplays.put(npc.getId(), displays);

        // Continuous pull effect over 40 ticks (2 seconds)
        final int npcId = npc.getId();
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40 || !target.isOnline() || !activeNpcIds.contains(npcId)) {
                    // Cleanup
                    for (Entity e : displays) { if (e != null && e.isValid()) e.remove(); }
                    npcBlockDisplays.remove(npcId);
                    cancel();
                    return;
                }

                NPC snareNpc = CitizensAPI.getNPCRegistry().getById(npcId);
                if (snareNpc == null || !snareNpc.isSpawned()) { cancel(); return; }

                Location currentNpcLoc = snareNpc.getEntity().getLocation();

                // Pull player toward NPC
                Vector diff = currentNpcLoc.toVector().subtract(target.getLocation().toVector());
                if (diff.lengthSquared() < 0.25) return; // Too close
                Vector pull = diff.normalize().multiply(0.15);
                target.setVelocity(target.getVelocity().add(pull));

                // Damage every 10 ticks
                if (ticks % 10 == 0) {
                    double damage = config.getMobDamage() * 0.4;
                    target.damage(damage);
                    target.setNoDamageTicks(0);
                }

                // Chain particles constricting
                if (ticks % 4 == 0) {
                    DisplayBuilder.particleRing(target.getLocation().clone().add(0, 1, 0),
                            1.5 - ticks * 0.02, Particle.CRIT, 8, null);
                    DisplayBuilder.dustParticles(target.getLocation().clone().add(0, 1, 0),
                            3, 0.5, 180, 180, 190, 1.0f);
                }

                if (ticks % 8 == 0) {
                    DisplayBuilder.playSound(target.getLocation(), Sound.BLOCK_CHAIN_STEP, 0.4f, 0.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.3f);
        plugin.debug("[Chain] NPC #" + npc.getId() + " used CHAIN SNARE on " + target.getName());
    }

    // ========================
    // Helpers
    // ========================

    private Player findNearestTarget(Location from, World world) {
        Player nearest = null;
        double nearestDist = config.getMobDetectionRange() * config.getMobDetectionRange();

        for (Player p : world.getPlayers()) {
            if (isExempt(p)) continue;
            double dist = p.getLocation().distanceSquared(from);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    private boolean isExempt(Player player) {
        // Skip non-survival players (creative, spectator, adventure)
        if (player.getGameMode() != GameMode.SURVIVAL) return true;
        // Skip invulnerable / god mode players
        if (player.isInvulnerable()) return true;
        var modeManager = plugin.getModeManager();
        if (modeManager.isAnyModeActive()) {
            var activeMode = modeManager.getActiveMode();
            if (activeMode.isExempt(player)) return true;
        }
        return player.hasPermission("chaoscraft.mode.exempt");
    }

    /**
     * Apply damage to a player respecting armor and resistance.
     * Uses player.damage() with a non-player source to avoid PvP detection.
     */
    private void applyDamage(Player target, double damage, NPC npc) {
        if (target.isInvulnerable() || target.getGameMode() != GameMode.SURVIVAL) return;
        if (target.isDead() || target.getHealth() <= 0) return;
        // Use the NPC entity as the damage source — but since it's a Player entity,
        // we use damage(amount) without a source to avoid PvP plugins.
        // Armor and resistance are applied by Minecraft's damage system.
        target.damage(damage);
        target.setNoDamageTicks(0); // Allow rapid hits from skills
    }

    private int countMobsNear(Player player, double radius) {
        int count = 0;
        double radiusSq = radius * radius;
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        for (int npcId : activeNpcIds) {
            NPC npc = registry.getById(npcId);
            if (npc != null && npc.isSpawned() && npc.getEntity() != null) {
                if (npc.getEntity().getLocation().distanceSquared(player.getLocation()) <= radiusSq) {
                    count++;
                }
            }
        }
        return count;
    }

    private void cleanupDead() {
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        activeNpcIds.removeIf(npcId -> {
            NPC npc = registry.getById(npcId);
            if (npc == null || !npc.isSpawned()) {
                cleanupNpcDisplays(npcId);
                npcTargets.remove(npcId);
                npcSkillCooldowns.remove(npcId);
                if (npc != null) npc.destroy();
                return true;
            }
            Entity entity = npc.getEntity();
            if (entity == null || entity.isDead() || !entity.isValid()) {
                cleanupNpcDisplays(npcId);
                npcTargets.remove(npcId);
                npcSkillCooldowns.remove(npcId);
                npc.despawn();
                npc.destroy();
                return true;
            }
            return false;
        });
    }

    private void cleanupNpcDisplays(int npcId) {
        List<Entity> displays = npcBlockDisplays.remove(npcId);
        if (displays != null) {
            for (Entity e : displays) {
                if (e != null && e.isValid()) e.remove();
            }
        }
    }

    private void tickCooldowns() {
        npcSkillCooldowns.replaceAll((id, cd) -> Math.max(0, cd - 5)); // Decrement by 5 (tick interval)
    }

    public int getActiveCount() {
        return activeNpcIds.size();
    }
}
