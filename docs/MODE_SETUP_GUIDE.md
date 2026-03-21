# ChaosCraft — Mode Setup Guide

Complete setup instructions for every ChaosCraft game mode including external plugin configuration, MythicMobs definitions, ModelEngine models, and arena setup.

---

## Table of Contents
1. [Global Requirements](#global-requirements)
2. [BetterHud Timer Setup](#betterhud-timer-setup)
3. [Chain Mode](#chain-mode)
4. [Corrupted Corruption](#corrupted-corruption)
5. [Devil's Dream](#devils-dream)
6. [Blue Moon](#blue-moon)
7. [Freezing Ice](#freezing-ice)
8. [Doom Mode](#doom-mode)
9. [Seer Mode](#seer-mode)
10. [Calamity Mode](#calamity-mode)

---

## Global Requirements

**Required plugins for ALL modes:**
- **PlaceholderAPI** — HUD placeholders, timer display
- **BetterHud** — Mode timer bar display

**Recommended plugins:**
- **Vault** — Economy for shop system
- **packetevents** — Advanced packet manipulation (freeze overlay, etc.)

**Auto-generated configs:**
All mode configs are created automatically at first startup in:
```
plugins/ChaosCraft/modes/{mode-name}/{mode-name}.yml
```

**Starting any mode:**
```
/cc modes <modename> start
/cc modes <modename> stop
```

**Universal admin commands (every mode):**
```
/cc modes <mode> status          — Mode status info
/cc modes <mode> debug           — Toggle damage radius circles
/cc modes <mode> test <id>       — Spawn specific attack on yourself
/cc modes <mode> list            — List all attack IDs
/cc modes <mode> clearattacks    — Remove all active attacks
/cc modes <mode> spawninterval <ticks> — Override spawn rate
/cc modes <mode> toggleexempt [player] — Toggle exempt
/cc modes <mode> reload          — Reload configs
```

---

## BetterHud Timer Setup

Every mode uses these PlaceholderAPI placeholders for the timer HUD:

| Placeholder | Returns | Use |
|---|---|---|
| `%chaoscraft_mode_timer_active%` | `true`/`false` | Show/hide timer overlay |
| `%chaoscraft_mode_timer_flash%` | `true`/`false` | Alternates every 10 ticks when timer is low |
| `%chaoscraft_mode_timer_low%` | `true`/`false` | Steady true when below threshold |
| `%chaoscraft_mode_display_name%` | e.g. `CHAIN MODE` | Mode name text |
| `%chaoscraft_mode_color%` | e.g. `gray` | Mode color for theming |
| `%chaoscraft_mode_timer_ticks%` | `1`-`20` | Animation sync counter |

**BetterHud layout setup:**
1. Create **Layout A** (normal timer) — condition: `%chaoscraft_mode_timer_active% = true` AND `%chaoscraft_mode_timer_flash% = false`
2. Create **Layout B** (flashing red timer) — condition: `%chaoscraft_mode_timer_flash% = true`

Each mode's config controls when flashing starts:
```yaml
timer-hud:
  display-name: "MODE NAME"
  color: "#FFFFFF"
  flash-color: "red"
  flash-threshold-seconds: 60   # Flash in last 60 seconds
```

---

## Chain Mode

**External plugins required:** Citizens 2, LibsDisguises (optional for skins)

**Setup steps:**

### 1. Install Citizens
Download Citizens from https://ci.citizensnpcs.co/ and place in plugins folder.

### 2. Configure chain.yml
```
plugins/ChaosCraft/modes/chain/chain.yml
```

Key settings:
```yaml
world: ""                    # Leave empty for default overworld, or set world name
timer:
  default-seconds: 600       # 10 minutes

mobs:
  enabled: true
  display-name: "&7Chain Walker"
  skin-player-name: ""       # Use a player's skin (e.g. "Notch")
  skin-url: ""               # OR use HTTP URL to skin PNG
  health: 40.0               # 20 hearts
  damage: 6.0                # 3 hearts base damage
  speed: 0.28
  detection-range: 32.0
  attack-range: 6.0
  spawn-distance: 20.0
  spawn-interval-ticks: 200  # Every 10 seconds
  max-per-player: 3
  max-total: 15
```

### 3. NPC Skin (optional)
**Option A — Player name:**
```yaml
mobs:
  skin-player-name: "SomePlayer"
```

**Option B — URL (upload skin PNG to imgur/GitHub):**
```yaml
mobs:
  skin-url: "https://i.imgur.com/yourimage.png"
```

### 4. Test
```
/cc modes chain start
/cc modes chain test chain_downpour
```

**Attack count:** 115 block display + 40 environmental = 155 total

---

## Corrupted Corruption

**External plugins required:** None (ItemsAdder/CraftEngine optional for custom blocks)

**Setup steps:**

### 1. Configure corruption.yml
```
plugins/ChaosCraft/modes/corruption/corruption.yml
```

Key settings:
```yaml
world: "world"               # MUST match your actual world name

floating-blocks:
  enabled: true
  max-per-chunk: 30
  damage: 20.0               # Damage on collision

block-replacement:
  enabled: true
  blocks-per-tick: 5
  max-radius-chunks: 5

mob-glitch:
  enabled: true
  hostile-only: true
  intensity: 0.3

ambient:
  dark-particles: true
  corruption-fog: true

respect-claims: true          # Don't corrupt claimed land
```

### 2. Custom Block Palette (optional)
Add ItemsAdder or CraftEngine blocks to the corruption spread:
```yaml
block-replacement:
  itemsadder-blocks:
    - "chaoscraft:corrupted_stone"
    - "chaoscraft:void_brick"
  craftengine-blocks:
    - "chaoscraft:glitch_block"
```

### 3. Test
```
/cc modes corruption start
/cc modes corruption corruption     # View engine stats
```

**Attack count:** 120 block display + 40 environmental + 30 ModelEngine = 190 total

---

## Devil's Dream

**External plugins required:** MythicMobs (for nightmare creatures)

**Setup steps:**

### 1. Create MythicMobs Mob Definitions
Create files in `plugins/MythicMobs/Mobs/`:

**NightmareHound.yml:**
```yaml
NightmareHound:
  Type: WOLF
  Display: "&4Nightmare Hound"
  Health: 40
  Damage: 8
  Options:
    MovementSpeed: 0.35
    AlwaysShowName: true
  DamageModifiers:
  - ENTITY_ATTACK 1.0
```

Create similar definitions for: `DreamWraith`, `ShadowStalker`, `InfernalImp`, `NightmareBrute`, `SoulHarvester`, `DreamPhantom`, `BoneRevenant`

### 2. Configure devilsdream.yml
```yaml
world: ""
timer:
  default-seconds: 900

adaptation:
  score-per-action: 5
  decay-interval-ticks: 200
  decay-amount: 2
  threshold: 50
  max-score: 200
  max-multiplier: 3.0

mythicmobs:
  enabled: true
  spawn-interval-ticks: 400
  max-alive: 8
```

### 3. Test
```
/cc modes devilsdream start
/cc modes devilsdream adaptation    # View adaptation scores
/cc modes devilsdream resetadaptation
```

**Attack count:** 104 block display + 104 environmental = 208 total

---

## Blue Moon

**External plugins required:** MythicMobs + ModelEngine R4

**Setup steps:**

### 1. Create MythicMobs Boss Definition
Create `plugins/MythicMobs/Mobs/blue_moon_boss.yml`:
```yaml
blue_moon_boss:
  Type: PHANTOM
  Display: "&b&lThe Blue Moon"
  Health: 500
  Options:
    MovementSpeed: 0
    KnockbackResistance: 1.0
    Silent: true
    PreventOtherDrops: true
    PreventRandomEquipment: true
    NoDamageTicks: 0
  Model:
    Id: blue_moon_boss
    Scale: 3.0
```

### 2. Import ModelEngine Models
The following .bbmodel files must be in ModelEngine's model directory:
```
plugins/ModelEngine/blueprints/
├── blue_moon_boss.bbmodel        (boss model)
├── blue_midnight.bbmodel
├── cartography.bbmodel
├── celestial_aurora.bbmodel
├── crater_maker.bbmodel
├── dark_half.bbmodel
├── event_horizon.bbmodel
├── fault_line.bbmodel
├── full_pull.bbmodel
├── gravity_well.bbmodel
├── lunar_fog.bbmodel
├── lunar_monolith.bbmodel
├── moon_phase_totem.bbmodel
├── moondust_fall.bbmodel
├── moonfall_crater.bbmodel
├── perigee.bbmodel
├── ray_of_selene.bbmodel
├── riptide.bbmodel
├── scar.bbmodel
├── selenite_spear.bbmodel
├── silver_tree_of_tides.bbmodel
├── the_weight.bbmodel
├── tidal_pool.bbmodel
├── tidal_sentinel.bbmodel
├── tide_breaker.bbmodel
└── totality.bbmodel
```

These are bundled in the plugin jar at `models/bluemoon/`. Copy them to ModelEngine's blueprints folder and run `/meg reload`.

### 3. Configure bluemoon.yml
```yaml
world: ""
force-night: true
timer:
  default-seconds: 900

boss:
  enabled: true
  mythicmob-id: "blue_moon_boss"
  modelengine-id: "blue_moon_boss"
  health: 500.0
  float-height: 25.0
  orbit-radius: 15.0
  orbit-speed: 0.02
  phase2-threshold: 0.75
  phase3-threshold: 0.50
  phase4-threshold: 0.25
  phase4-enrage-seconds: 60
  spawn-delay-ticks: 100
  super-laser:
    enabled: true
    damage: 12.0
    beam-damage-multiplier: 3.0
    charge-ticks: 40
    duration-ticks: 60
    cooldown-ticks: 600
```

### 4. Gimmick Configuration
52 toggleable gimmicks under `gimmicks:` section. All default to true. Toggle with:
```
/cc modes bluemoon gimmick list
/cc modes bluemoon gimmick toggle <name>
```

### 5. Test
```
/cc modes bluemoon start
/cc modes bluemoon boss spawn
/cc modes bluemoon boss phase 4
/cc modes bluemoon boss laser
/cc modes bluemoon boss kill
```

**Attack count:** 104 block display + 104 environmental + 12 boss + 25 ModelEngine = 245 total

---

## Freezing Ice

**External plugins required:** None

**Setup steps:**

### 1. Configure freezingice.yml
```yaml
world: ""
timer:
  default-seconds: 900

temperature:
  start: 100
  decay-rate: 1                # Lose 1 temp per interval
  decay-interval-ticks: 100    # Every 5 seconds
  freeze-threshold: 0          # Fully frozen at 0

  speed-debuffs:
    75: -0.03                  # Slight slow at 75% temp
    50: -0.06                  # Medium slow at 50%
    25: -0.10                  # Heavy slow at 25%

  heat-sources:
    enabled: true
    radius: 3.0                # Blocks near heat source
    restore-rate: 5            # +5 temp/sec near fire
    blocks:                    # What counts as heat
      - TORCH
      - CAMPFIRE
      - SOUL_CAMPFIRE
      - LAVA
```

### 2. ModelEngine Models (optional)
If using ModelEngine attacks, copy the ice .bbmodel files from the jar to `plugins/ModelEngine/blueprints/`.

### 3. Test
```
/cc modes freezingice start
/cc modes freezingice test glacial_saw
```

**Attack count:** 104 block display + 104 environmental = 208+ total

---

## Doom Mode

**External plugins required:** None (ModelEngine optional for VFX attacks)

**Setup steps:**

### 1. Set Arena Boundaries (MANDATORY)
Stand at corner 1 of your arena and run:
```
/cc function setdoommodepos1
```
Stand at the opposite corner and run:
```
/cc function setdoommodepos2
```

### 2. Configure doom.yml
```yaml
world: ""

arena:
  boundary-enforcement: true
  boundary-push-strength: 0.5

lava-rise:
  start-y: 60               # Y level where lava starts
  max-y: 100                 # Y level where lava stops
  rise-interval-ticks: 200   # Rise every 10 seconds
  rise-amount: 1             # 1 block per rise
  blocks-per-level-tick: 500 # Performance tuning
  auto-cleanup: true         # Remove lava when mode ends

lava-damage:
  damage: 2.0
  interval-ticks: 20

attack-weights:
  block-display: 45
  environmental: 35
  model-engine: 20
```

### 3. ModelEngine Models (optional)
Copy doom .bbmodel files to `plugins/ModelEngine/blueprints/` for the 20% ModelEngine attack weight.

### 4. Test
```
/cc modes doom start
/cc modes doom test doom_meteor
```

---

## Seer Mode

**External plugins required:** MythicMobs + ModelEngine R4

**Setup steps:**

### 1. Create Dedicated Arena World
Seer requires a dedicated arena world. Create one with Multiverse:
```
/mv create seer_arena normal
```

### 2. Create MythicMobs Boss Definition
Create `plugins/MythicMobs/Mobs/seer_boss.yml`:
```yaml
seer_boss:
  Type: ZOMBIE
  Display: "&5&lThe Seer"
  Health: 100000000
  Options:
    MovementSpeed: 0
    KnockbackResistance: 1.0
    Silent: true
    PreventOtherDrops: true
    PreventRandomEquipment: true
    NoDamageTicks: 0
    NoAI: true
  Model:
    Id: eyeboss
    Scale: 3.0
```

### 3. Import ModelEngine Model
Copy `eyeboss.bbmodel` from the plugin jar (`models/seer/eyeboss.bbmodel`) to:
```
plugins/ModelEngine/blueprints/eyeboss.bbmodel
```
Run `/meg reload` to load the model.

### 4. Set Orb Positions (MANDATORY — 10 positions)
Go to each orb location in the seer_arena world and run:
```
/cc function setorbspawn 1
/cc function setorbspawn 2
/cc function setorbspawn 3
... (repeat for all 10)
/cc function setorbspawn 10
```

**Tips for orb placement:**
- Spread them evenly around the arena
- Mix heights (ground level, elevated platforms, etc.)
- Don't place underground (orbs spawn on the surface)
- Keep within the boss's 500-block detection range

### 5. Configure seer.yml
```yaml
world: "seer_arena"
timer:
  default-seconds: 1200       # 20 minutes

boss:
  mythicmob-id: "seer_boss"
  modelengine-id: "eyeboss"
  scale: 3.0
  float-height: 15.0
  move-speed: 0.3
  detection-range: 500.0
  beam-range: 50.0
  beam-charge-ticks: 100     # 5 second charge
  beam-damage-per-tick: 1.0  # 0.5 hearts/tick (respects armor)

orbs:
  count: 10
  material: CRYING_OBSIDIAN
  hardness-multiplier: 5.0   # 5x harder to break
  health-per-orb: 10000000   # 10M HP per orb
  break-health-divisor: 2.0  # Halves player health on break
  break-knockback: 2.0
```

### 6. Test
```
/cc modes seer start
/cc modes seer boss spawn
/cc modes seer boss beam
/cc modes seer orbs status
/cc modes seer orbs break 1
/cc modes seer boss kill
```

**Attack count:** 50 block display + 50 environmental = 100 total

**How the fight works:**
1. Boss spawns as floating eyeball, orbits above players
2. Boss fires beam — 1 HP/tick drain (respects armor), 50-block range
3. Players must find and break 10 crying obsidian orbs around the arena
4. Breaking an orb halves the breaker's current health + knockback
5. Each orb destroyed reduces boss max HP by 10M
6. All 10 orbs broken = boss dies instantly

---

## Calamity Mode

**External plugins required:** MythicMobs (for bosses)

**Setup steps:**

### 1. Create MythicMobs Boss Definitions
Create the following in `plugins/MythicMobs/Mobs/`:

**voidmaw_boss.yml:**
```yaml
voidmaw_boss:
  Type: WITHER_SKELETON
  Display: "&4&lVoidmaw"
  Health: 200
  Damage: 12
  Options:
    MovementSpeed: 0.3
    KnockbackResistance: 0.8
```

Create similar for: `dweller_boss`, `void_emperor_minion1-4`

### 2. Configure calamity.yml
```yaml
world: ""
timer:
  default-seconds: 1800

bosses:
  voidmaw:
    mythic-id: "voidmaw_boss"
  dweller:
    mythic-id: "dweller_boss"

gem-system:
  spawn-radius: 30
  spawn-height: 40
  requirement-per-phase: 50
```

### 3. Arena Setup
- Build arena in The End or a custom world
- Set gem spawn area (30-block radius, 40 blocks above arena floor)
- Configure ItemsAdder portal block if using custom textures

### 4. Test
```
/cc modes calamity start
/cc modes calamity setphase 2
/cc modes calamity spawnboss
```

**Attack count:** 1,792 total (495 block display + 599 environmental + 698 boss)

---

## Quick Reference — All Modes

| Mode | Timer | Boss | Special Setup | Attacks |
|---|---|---|---|---|
| Chain | 10 min | No | Citizens plugin | 155 |
| Corruption | 15 min | No | Block palette | 190 |
| Devil's Dream | 15 min | No | MythicMobs mobs | 208 |
| Blue Moon | 15 min | Yes (moon) | MythicMobs + ModelEngine + 26 models | 245 |
| Freezing Ice | 15 min | No | Heat sources in arena | 208+ |
| Doom | 10 min | No | Arena pos1/pos2, lava config | ~300 |
| Seer | 20 min | Yes (eye) | MythicMobs + ModelEngine + 10 orb positions | 100 |
| Calamity | 30 min | Yes (5 bosses) | MythicMobs + arena + gem system | 1,792 |

---

## Permissions

| Permission | Description |
|---|---|
| `chaoscraft.mode.trigger` | Start modes |
| `chaoscraft.mode.end` | Stop modes |
| `chaoscraft.mode.exempt` | Exempt from mode attacks |
| `chaoscraft.admin` | Full admin access (all modes) |
| `chaoscraft.chain.admin` | Chain mode admin commands |
| `chaoscraft.corruption.admin` | Corruption mode admin commands |
| `chaoscraft.devilsdream.admin` | Devil's Dream admin commands |
| `chaoscraft.bluemoon.admin` | Blue Moon admin commands |
| `chaoscraft.freezingice.admin` | Freezing Ice admin commands |
| `chaoscraft.doom.admin` | Doom mode admin commands |
| `chaoscraft.seer.admin` | Seer mode admin commands |
| `chaoscraft.calamity.admin` | Calamity mode admin commands |
