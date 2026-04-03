# ChaosCraft Debug Checklist

Common bugs and their root causes found during development. Check these FIRST when debugging new modes.

## ModelEngine Issues

### Model not loading ("Error: null", "Unknown format", "Index out of bounds")
- **UV format mismatch**: `modded_entity` format uses `uv[x,y] + uv_size[w,h]`, but `free` format needs `uv[x1,y1,x2,y2]`. Convert with script.
- **Missing rotation/origin arrays**: Elements need `"rotation": [0,0,0]` and `"origin": [0,0,0]`. Empty arrays `[]` or `null` cause "Index 2 out of bounds".
- **Wrong model_format**: ME4 R4.0.8 VFX models need `"model_format": "free"` and `"format_version": "4.10"`.
- **Missing hitbox bone**: ME4 requires at least one hitbox bone in the outliner.
- **Missing root fields**: `free` format needs `visible_box`, `variable_placeholders`, `variable_placeholder_buttons`, `timeline_setups`, `unhandled_root_fields`.

### Scale not working
- **Hardcoded `getModelScale()` override**: If a subclass overrides `getModelScale()` with a constant, the YAML config value is IGNORED. Remove the override to use config.
- **Bukkit SCALE attribute conflicts**: Don't set `Attribute.SCALE` on the host entity — it scales the invisible zombie, not the ME model. Only use `activeModel.setScale(double)`.
- **Marker ArmorStand zeroes dimensions**: ME4 can't scale models on marker armor stands. Use invisible Zombie instead.

### Model sinks into ground
- Zombie origin is at feet. Offset spawn location Y+1.5 blocks.

### Steve/armor showing on host entity
- Zombie spawns with random equipment. Call `getEquipment().clear()` + `setCanPickupItems(false)`.
- If model fails to load, zombie stays invisible but may show Steve in some ME4 versions.

### Walk animation playing on boss
- ME4 auto-detects velocity from NMS `setDeltaMovement()` and triggers walk animation.
- Fix: `MOVEMENT_SPEED=0` + force-stop walk animation + replay idle every 20 ticks.

## Config Issues

### Config value not loading from YAML
- **Hardcoded override in constructor**: Check if the attack class sets values with `config.set*()` in constructor — these override YAML values.
- **Hardcoded `getX()` override**: Check if a subclass overrides a getter (like `getModelScale()`) with a hardcoded return value.
- **Wrong file path**: All ME attacks share one file per type: `modelengine.yml`, `blockdisplays.yml`, etc. Not one file per attack.
- **Config saved on first load**: If the key doesn't exist, the plugin writes defaults and saves — check if your edit was overwritten.
- **Old config-version**: Config migrations may reset values. Check `config-version` matches `CURRENT_CONFIG_VERSION`.

## Performance Issues

### TPS spikes / player timeouts
- **Entity spam per tick**: Don't create+destroy 100+ block displays every tick. Reuse/pool or rebuild every 4 ticks.
- **getHighestBlockYAt() every tick**: Cache the result, update every 20 ticks.
- **O(n^2) player loops**: Avoid nested player distance checks. Use `distanceSquared()` instead of `distance()`.
- **Unbounded maps keyed by UUID**: Clean up on player disconnect, not just mode end.
- **Iterating entire attack registry**: Cache filtered lists (e.g., BLOCK_DISPLAY attacks) at mode start.

### Lag monitor causing lag
- ModereX `ServerStatusManager.detectLaggySources()` was scanning every chunk every tick (38% CPU). The lag monitor was the lag.

## Damage / Combat Issues

### Players can't take rapid damage
- Set `player.setMaximumNoDamageTicks(0)` during modes. Restore to 20 on mode end.

### Movement stutter on damage
- `EntityKnockbackByEntityEvent` only catches entity-source knockback.
- Use `EntityKnockbackEvent` (Paper API, covers ALL sources including NMS hurt velocity).
- Set `KNOCKBACK_RESISTANCE` attribute to 1.0 — zeroes out velocity math in NMS `hurt()`.
- Zero out NMS `hurtTime`/`hurtDuration`/`invulnerableTime` via CraftPlayer.getHandle().
- Restore all to vanilla defaults on mode end.

### Boss can't be damaged
- Check if `setInvulnerable(true)` is stuck on — use glow-based invincibility instead.
- Check if glow (`isGlowing()`) is stuck on when laser ends.
- Boss zombie has default 10-tick noDamageTicks — set `setMaximumNoDamageTicks(0)`.

## Entity Cleanup Issues

### Block/item displays not despawning
- Tag ALL display entities (BlockDisplay + ItemDisplay) with `chaoscraft_display`.
- Safety net `cleanupAllDisplaysInWorld()` must catch ALL entity types, not just BlockDisplay.
- Laser model entities need `removeAll()` (displays + ME entity) not just `removeDisplays()`.

## JVM / Server Issues

### GC pressure
- Don't allocate temporary ArrayLists every tick — cache or reuse.
- Throttle gimmick ticks (every 4 ticks instead of every tick).

### Startup flags (24GB plan)
```
-Xms18G -Xmx18G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50 -XX:G1HeapRegionSize=16M -XX:G1ReservePercent=15 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1
```
