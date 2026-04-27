# Devil's Dream — Block Display Attack Plans (105 Attacks)

## Technical Foundation

All attacks use Paper 1.21.4 BlockDisplay API with:
- **Matrix4f transforms** via `setTransformationMatrix()` for composing complex rotations
- **Client interpolation** for animations — set initial state → wait 1 tick → set target with `setInterpolationDuration()`
- **Passenger hierarchy** (Marker anchor + BD passengers) for mobile attacks that move as a group
- **Non-cube blocks** for complex shapes: stairs (wedge/L), trapdoors (0.1875-thick panels), fences (cross posts), chains, end rods, slabs
- **Scale tricks**: `scale(0.05f, 2f, 0.05f)` = thin rod | `scale(2f, 0.05f, 2f)` = flat plane | `scale(0.3f, 1.8f, 0.3f)` = elongated rod
- **Teleport duration** (0–59 ticks) for smooth position tracking
- **Culling boxes** set on every attack via `setDisplayWidth/Height`
- All spawns use yaw=0, pitch=0 (no player orientation dependency)
- Damage config: `AttackConfig("id", BLOCK_DISPLAY, 1, "modes/devilsdream/attacks")`

---

## CATEGORY 1: DreamArchitecture (DreamArchitecture.java)
*Impossible buildings, nightmare geometry, Escher-inspired structures*
*Theme: Deepslate gray, polished blackstone, tinted glass panels, crying obsidian trim, warped wood*

---

### 1. THE ENDLESS HOTEL — 34 blocks
**Visual**: An Art Deco hotel facade leaning impossibly toward the player, windows flickering, architecture that should not physically stand.
**Materials**: deepslate_bricks (columns), polished_blackstone_bricks (trim), tinted_glass (windows), dark_oak_trapdoor (door panels), crying_obsidian (glow accents)
**Assembly**:
- 4 tapered columns: 6 deepslate_brick blocks each stacked, scale decreasing 1.0→0.7 top (24 blocks)
- 2 entablature beams: polished_blackstone_bricks scale(2.5f, 0.18f, 0.9f) wide flat plates (2 blocks)
- 6 windows: tinted_glass trapdoors rotated vertical, scale(0.85f, 1.4f, 0.05f) ultra-thin panels (6 blocks)
- 2 grand door arch: dark_oak_trapdoors scale(0.35f, 1.8f, 0.05f) tall thin panels (2 blocks)

**Animation**: Entire structure mounted on Marker anchor. After 1 tick, Marker tilts 25° toward nearest player over 40 ticks via teleport. Windows oscillate scale 0.85↔1.0 on 20-tick loop via independent interpolation. Columns have phase-offset Y-bob (teleportDuration=15, ±0.2) creating uneven sway.
**Sounds**: `BLOCK_DEEPSLATE_PLACE` (0.8f, 0.3f) on spawn | `AMBIENT_CAVE` every 80t
**Particles**: `REVERSE_PORTAL` drifting from each window | `SCULK_SOUL` rising from base
**Config**: radius=7, damage=28, ticks-between=25, delay=15, duration=400, cooldown=300

---

### 2. THE INVERSE CATHEDRAL — 38 blocks
**Visual**: A gothic cathedral hanging upside-down from the sky, spire pointing at the ground, slowly spinning.
**Materials**: end_stone_bricks (walls), polished_deepslate (trim), purple_stained_glass (rose windows), blackstone_brick_stairs (gothic arches), purpur_pillar (spire)
**Assembly**:
- Central inverted spire: 7 purpur_pillar blocks scale(0.35f, 0.9f, 0.35f) stacked downward (7 blocks)
- 4 flying buttresses: each 3 end_stone_brick blocks scale(0.4f, 0.2f, 1.3f) angled 35° outward via Matrix4f rotateX (12 blocks)
- 2 nave walls: 4 blackstone_brick_stairs in arc formation using alternating facing per arch (8 blocks)
- 6 rose window segments: purple_stained_glass scale(0.7f, 0.05f, 0.7f) flat plates in ring (6 blocks)
- 5 gargoyle accent blocks: carved_pumpkin at corners + top (5 blocks)

**Animation**: Whole structure continuously rotates Y via Matrix4f accumulating 180°+0.1f every 120 ticks (interpolation duration 120). Buttresses pulse outward scale 0.2↔0.3 on 40-tick stagger. Rose windows spin independently on Z-axis.
**Sounds**: `BLOCK_END_PORTAL_SPAWN` (1.0f, 0.4f) | `ENTITY_GHAST_SCREAM` (0.5f, 0.3f) every 100t
**Particles**: `REVERSE_PORTAL` spiraling downward from spire | `PORTAL` at rose windows
**Config**: radius=7, damage=32, ticks-between=20, delay=20, duration=500, cooldown=350

---

### 3. THE MELTING MANSION — 36 blocks
**Visual**: A mansion whose walls slowly droop and deform downward, blocks "melting" toward the ground with dripping geometry.
**Materials**: polished_blackstone_bricks (walls), crying_obsidian (melt drips), dark_oak_stairs (drooped roof segments), tinted_glass (sagging windows), chiseled_polished_blackstone (facades)
**Assembly**:
- 4 wall panels: polished_blackstone_bricks scale(1.2f, 1.5f, 0.15f) vertical flat plates (4 blocks)
- 8 drip blocks: crying_obsidian scale(0.25f, 0.8f, 0.25f) thin rods positioned below wall edges at offset Y intervals (8 blocks)
- 4 sagging roof segments: dark_oak_stairs rotated and scaled scale(1.2f, 0.3f, 0.9f) angled downward (4 blocks)
- 6 window melt shapes: tinted_glass scale(0.6f, 0.05f, 0.8f) flat panels drooping off-angle (6 blocks)
- 4 chimney stacks: netherite_block scale(0.3f, 1.0f, 0.3f) (4 blocks) [note: use blackstone if netherite not available]
- 10 corner/accent blocks: chiseled_polished_blackstone at structural intersections (10 blocks)

**Animation**: Each drip block extends Y scale 0.8→2.5 over 60 ticks independently with staggered 10-tick offsets, then snaps back. Wall panels slowly lean via Matrix4f rotateX 0°→-8° over 80 ticks. Roof segments droop via Y-translation -0.5 over 50 ticks then reset.
**Sounds**: `BLOCK_CRYING_OBSIDIAN_HURT` every 40t | `BLOCK_GLASS_BREAK` (0.6f, 0.3f) on drip reset
**Particles**: `DRIPPING_OBSIDIAN_TEAR` from drip tips | `FALLING_OBSIDIAN_TEAR` from walls
**Config**: radius=6, damage=25, ticks-between=20, delay=10, duration=450, cooldown=300

---

### 4. THE PENROSE TRAP — 33 blocks
**Visual**: An impossible Penrose staircase — four staircases that each go up one level but connect in a continuous loop, making a closed rectangle that defies gravity.
**Materials**: polished_deepslate (stairs), deepslate_bricks (platforms), polished_blackstone (rails), blackstone (supports)
**Assembly**:
- 16 stair blocks: polished_deepslate_stairs arranged as 4 rising groups of 4 (4 per side of square) at positions forming paradox loop (16 blocks)
- 4 platform connectors: deepslate_bricks scale(1.0f, 0.18f, 1.0f) flat plates joining stair sections (4 blocks)
- 8 rail posts: polished_blackstone scale(0.1f, 0.5f, 0.1f) thin rods at stair edges (8 blocks)
- 5 inner void accent: blackstone scale(0.5f, 0.5f, 0.5f) floating at center (5 blocks)

**Animation**: Entire structure rotates on Y continuously (Matrix4f 180°+0.1f every 80t). Each of the 4 stair segments also independently oscillates Y-scale 1.0↔1.15 with 10-tick phase offset between segments, making the ascent look continuous. Glows purple.
**Sounds**: `BLOCK_STONE_STEP` repeating every 20t (0.5f, 1.2f) as if someone walks the loop
**Particles**: `PORTAL` along stair edges | `REVERSE_PORTAL` at impossible corners
**Config**: radius=8, damage=30, ticks-between=20, delay=10, duration=500, cooldown=350

---

### 5. THE SHATTERED OBSERVATORY — 35 blocks
**Visual**: An observatory dome that has exploded outward — dome segments floating frozen mid-explosion, each fragment slowly rotating.
**Materials**: end_stone_bricks (dome shell), polished_deepslate (floor), tinted_glass (telescope lens pieces), amethyst_block (glowing fragments), chiseled_deepslate (detail blocks)
**Assembly**:
- 12 dome shell fragments: end_stone_bricks scale(1.0f, 0.2f, 1.0f) flat plates at different explosion angles (12 blocks)
- 4 telescope lens shards: tinted_glass scale(0.8f, 0.05f, 0.8f) flat translucent (4 blocks)
- 8 amethyst fragment shards: amethyst_block scale(0.25f, 1.2f, 0.25f) elongated crystals pointing in different directions (8 blocks)
- 6 floor/base blocks: polished_deepslate at ground level forming circular pattern scale(0.7f, 0.15f, 0.7f) (6 blocks)
- 5 detail accent: chiseled_deepslate scale(0.4f, 0.4f, 0.4f) floating between fragments (5 blocks)

**Animation**: Each of the 12 dome fragments has an independent gentle spin on different axes (staggered rotateY, rotateX via Matrix4f update every 30t, interpolation=30). Crystal shards pulse scale 0.25↔0.45 with 15-tick offsets. Whole cluster slowly expands outward via teleport duration (fragments drift apart 0.1 blocks/tick).
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` every 30t | `BLOCK_GLASS_BREAK` on spawn
**Particles**: `FALLING_SPORE_BLOSSOM` from amethysts | `REVERSE_PORTAL` between fragments
**Config**: radius=7, damage=27, ticks-between=20, delay=20, duration=400, cooldown=280

---

### 6. THE RECURSIVE CUBE — 30 blocks
**Visual**: A large cube, inside which is a medium cube, inside which is a small cube — each rotating on different axes at different speeds. Impossible depth effect.
**Materials**: crying_obsidian (outer), polished_blackstone (middle), amethyst_block (inner), tinted_glass (connecting "windows" between layers)
**Assembly**:
- 8 outer cube corners: crying_obsidian scale(0.5f, 0.5f, 0.5f) at cube corners radius 2 (8 blocks)
- 12 outer edge beams: polished_blackstone scale(0.1f, 0.1f, 1.8f) spanning edges (12 blocks total, oriented per edge)
- 6 inner cube corners: crying_obsidian scale(0.35f, 0.35f, 0.35f) at radius 1 (6 blocks)
- 4 inner glowing core: amethyst_block scale(0.25f, 0.25f, 0.25f) floating at center (4 blocks)

**Animation**: Outer frame rotates Y, inner frame rotates Z, core rotates X — all at different speeds. Each layer uses separate Marker anchors as passengers. Outer: 120t per half-turn. Inner: 80t per half-turn. Core: 60t per half-turn. Glow pulses purple→cyan.
**Sounds**: `BLOCK_BEACON_AMBIENT` low pitch loop | `ENTITY_ENDERMAN_TELEPORT` every 80t
**Particles**: `PORTAL` tracing cube edges | `REVERSE_PORTAL` at core
**Config**: radius=7, damage=30, ticks-between=20, delay=15, duration=450, cooldown=300

---

### 7. THE FALLING ROOM — 35 blocks
**Visual**: An entire room — floor, ceiling, 4 walls, furniture outlines — falling from Y+10 while rotating. Players see a complete room tumbling toward them.
**Materials**: blackstone_bricks (walls/floor/ceiling), polished_blackstone (furniture outlines), iron_bars (window grates), dark_oak_trapdoor (floor details), netherrack (broken wall details)
**Assembly**:
- 6 room panels (4 walls + floor + ceiling): blackstone_bricks scale(3.0f, 0.1f, 3.0f) flat plates (6 blocks)
- 8 iron_bars fence posts for window grates: scale(0.1f, 1.2f, 0.1f) in cross pattern (8 blocks)
- 4 furniture outline blocks: polished_blackstone scale(1.0f, 0.2f, 0.5f) flat panels (table, shelf shapes) (4 blocks)
- 8 corner structural blocks: dark_oak_trapdoor thin panels at corner angles (8 blocks)
- 9 debris floating pieces: netherrack scale(0.3f, 0.3f, 0.3f) scattered around room (9 blocks)

**Animation**: Full room spawns at Y+12 on a Marker. Marker falls via repeated teleport (Y-0.15 per tick with teleportDuration=10). Room tumbles by rotating Marker on all 3 axes via Matrix4f. Impact: when Y reaches player level, `damageOnImpactOnly=true`, 8 block radius.
**Sounds**: `BLOCK_ANVIL_LAND` on impact | `AMBIENT_CAVE` during fall | `BLOCK_STONE_BREAK` crackling
**Particles**: `BLOCK_CRACK(BLACKSTONE_BRICKS)` trailing during fall | `EXPLOSION_EMITTER` on impact
**Config**: radius=8, impact-only=true, impactDamage=45, impactRadius=8, duration=160, cooldown=400

---

### 8. THE CLOCKWORK LABYRINTH — 36 blocks
**Visual**: Interlocking labyrinth walls made of gearwork-like blocks, spinning and rearranging as if the maze reconfgures itself.
**Materials**: smooth_stone (walls), polished_blackstone (gear-like blocks), iron_trapdoor (mechanical panels), chain (gear teeth), grindstone (actual gear shapes)
**Assembly**:
- 8 wall segments: smooth_stone scale(1.8f, 1.5f, 0.15f) vertical flat panels in square arrangement (8 blocks)
- 6 gear face blocks: grindstone at wall intersections (6 blocks)
- 8 chain-tooth details: chain scale(0.5f, 0.05f, 0.5f) thin ring segments at gear edges (8 blocks)
- 8 iron_trapdoor mechanical panels: scale(0.7f, 0.7f, 0.05f) thin horizontal panels above walls (8 blocks)
- 6 polished_blackstone corner pillars: scale(0.25f, 1.6f, 0.25f) vertical rods (6 blocks)

**Animation**: Each wall segment rotates on Y independently, timed so they cycle open→closed at 60-tick intervals (creates maze reconfiguration). Gear blocks spin on Z-axis. Chain details orbit the gear blocks via teleport updates (small circular paths).
**Sounds**: `BLOCK_CHAIN_BREAK` every 60t on reconfigure | `BLOCK_PISTON_EXTEND` (0.8f, 0.8f)
**Particles**: `CRIT` (metal sparks) at gear intersections | `SMOKE` from chain sections
**Config**: radius=6, damage=26, ticks-between=20, delay=20, duration=500, cooldown=300

---

### 9. THE NIGHTMARE MIRROR — 33 blocks
**Visual**: A massive ornate standing mirror whose reflection shows impossible things — the frame is elaborate bone/obsidian, the "mirror surface" is a flat plane that has aberrant shadow figures moving across it.
**Materials**: bone_block (frame), polished_blackstone (frame inlays), amethyst_shard (not a block, use amethyst_block) (sparkle accents), tinted_glass (mirror surface), blackstone (base)
**Assembly**:
- 2 tall frame sides: bone_block scale(0.35f, 2.5f, 0.35f) vertical rods (2 blocks)
- 1 top arch frame: 5 bone_block stairs in arc arrangement scale(0.8f, 0.4f, 0.35f) (5 blocks)
- 2 decorative side wings: bone_block scale(0.6f, 0.05f, 0.4f) flat ornamental pieces (2 blocks)
- 1 mirror surface: tinted_glass scale(1.8f, 2.2f, 0.02f) ultra-thin flat plane (1 block)
- 8 amethyst glow accents: amethyst_block scale(0.15f, 0.15f, 0.15f) dotted around frame (8 blocks)
- 8 polished_blackstone inlay blocks: scale(0.3f, 0.3f, 0.05f) thin decorative inlays (8 blocks)
- 7 base/pedestal blocks: blackstone + polished_blackstone forming ornate base (7 blocks)

**Animation**: Mirror surface shimmers — scale(1.8, 2.2, 0.02) pulses to scale(1.85, 2.25, 0.04) and back every 15t. Frame slowly rotates on Y ±10° oscillation via Matrix4f. Amethyst accents strobe.
**Sounds**: `BLOCK_GLASS_PLACE` eerie glass tone every 40t | `ENTITY_ENDERMAN_STARE` (0.3f, 0.5f)
**Particles**: `PORTAL` seeping from mirror edges | `REVERSE_PORTAL` "reflections" on mirror surface
**Config**: radius=6, damage=28, ticks-between=20, delay=15, duration=450, cooldown=300

---

### 10. THE UPSIDE-DOWN HOUSE — 38 blocks
**Visual**: A fully recognizable house (chimney, door, windows, roof) but inverted — foundation at top, chimney pointing down. Slowly descends toward player.
**Materials**: oak_planks (walls), spruce_stairs (inverted roof), dark_oak_trapdoor (windows), brick (chimney), iron_door (front door), cobblestone (foundation)
**Assembly**:
- 2 wall panels: oak_planks scale(2.5f, 1.8f, 0.12f) thin walls (2 blocks)
- 4 inverted roof segments: spruce_stairs with HALF=TOP facing outward, scale(1.2f, 0.7f, 0.7f) (4 blocks)
- 4 windows: dark_oak_trapdoor scale(0.7f, 0.8f, 0.05f) thin panels (4 blocks)
- 3 chimney: brick scale(0.45f, 0.8f, 0.45f) pointing downward (3 blocks)
- 2 door panels: iron_door (use dark_oak_door) scale(0.55f, 0.9f, 0.08f) (2 blocks)
- 6 foundation blocks: cobblestone scale(0.5f, 0.2f, 0.5f) at top (where bottom should be) (6 blocks)
- 8 structural corner/edge blocks: oak_planks scale(0.2f, 1.8f, 0.2f) corner rods (8 blocks)
- 9 interior debris: various scale(0.3f, 0.3f, 0.3f) floating inside (9 blocks)

**Animation**: Entire house on Marker anchor. Marker descends Y-0.1/tick with teleportDuration=15. House tilts 15° side-to-side via slow Y-rotation. On ground contact: `damageOnImpactOnly=true`.
**Sounds**: `BLOCK_WOOD_BREAK` crackling | `ENTITY_MINECART_RIDING` structural groaning | `BLOCK_ANVIL_LAND` on impact
**Particles**: `BLOCK_CRACK(OAK_PLANKS)` trailing | `CAMPFIRE_COSY_SMOKE` from chimney
**Config**: radius=8, impact-only=true, impactDamage=40, impactRadius=8, duration=180, cooldown=380

---

### 11. THE IMPOSSIBLE BRIDGE — 34 blocks
**Visual**: A stone bridge that curves in an impossible loop — it starts going up-left and ends going down-left, yet they connect, forming a Möbius-strip-like arch you could theoretically walk forever.
**Materials**: smooth_stone (deck), polished_andesite (arches), stone_bricks (railings), mossy_cobblestone (age texture blocks), chiseled_stone_bricks (ornate sections)
**Assembly**:
- 10 bridge deck plates: smooth_stone scale(1.5f, 0.15f, 0.7f) flat plates in a looping arc path (10 blocks)
- 4 supporting arch blocks: polished_andesite stairs in arc orientation (4 blocks)
- 8 railing posts: stone_bricks scale(0.1f, 0.7f, 0.1f) thin vertical rods along edges (8 blocks)
- 6 railing rails: stone_bricks scale(1.5f, 0.1f, 0.05f) horizontal rails (6 blocks)
- 6 accent/age blocks: mossy_cobblestone scale(0.5f, 0.5f, 0.5f) scattered on bridge deck (6 blocks)

**Animation**: Entire bridge on Y-axis continuous rotation (120-tick half-turn). Each deck section individually bobs ±0.15 with 5-tick phase offsets creating bridge wave motion. Railing posts oscillate scale 0.7↔0.9 on 25-tick loop.
**Sounds**: `BLOCK_STONE_STEP` periodic | `AMBIENT_CAVE` (0.4f, 0.2f) eerie wind
**Particles**: `PORTAL` along bridge underside | `FALLING_SPORE_BLOSSOM` drifting up from bridge
**Config**: radius=7, damage=28, ticks-between=20, delay=15, duration=420, cooldown=300

---

### 12. THE FRACTAL TOWER — 32 blocks
**Visual**: A tower where each layer is identical to the whole at a smaller scale — a central tall section surrounded by 4 medium-sized versions surrounded by 8 small versions, creating self-similar recursion.
**Materials**: deepslate_bricks (main), polished_deepslate (secondary), deepslate_tile_stairs (detail), amethyst_block (glowing accents)
**Assembly**:
- 1 central tower core: deepslate_bricks scale(0.4f, 3.0f, 0.4f) thin tall rod (1 block)
- 4 medium sub-towers: deepslate_bricks scale(0.25f, 1.8f, 0.25f) at radius 2 (4 blocks)
- 8 small sub-towers: polished_deepslate scale(0.15f, 1.0f, 0.15f) at radius 4 (8 blocks)
- 3 cap platforms: deepslate_tile_stairs scale(0.8f, 0.2f, 0.8f) at 3 heights (3 blocks)
- 8 cross-beam connectors: deepslate_bricks scale(0.08f, 0.08f, 1.8f) beams between tiers (8 blocks)
- 8 amethyst glows: amethyst_block scale(0.18f, 0.18f, 0.18f) at junction points (8 blocks)

**Animation**: Central tower slowly rotates Y. Medium towers orbit around central (teleport in arc updated every 5t, teleportDuration=8). Small towers orbit around medium towers. Amethyst accents pulse scale 0.18↔0.28 sequentially ascending.
**Sounds**: `BLOCK_AMETHYST_BLOCK_STEP` | `BLOCK_BEACON_AMBIENT` (0.3f, 0.7f)
**Particles**: `PORTAL` spiraling up central tower | `REVERSE_PORTAL` at each sub-tower
**Config**: radius=8, damage=30, ticks-between=20, delay=20, duration=500, cooldown=350

---

### 13. THE HAUNTED LIBRARY — 40 blocks
**Visual**: Towering bookshelves crammed with phantom books, some floating out and flying open, with a reading lectern at center glowing ominously.
**Materials**: dark_oak_planks (shelf frames), bookshelf (book texture), lectern (center piece), purple_stained_glass (ghost books), tinted_glass (window), gold_block (lectern accents)
**Assembly**:
- 4 shelf unit frames: dark_oak_planks scale(0.15f, 2.5f, 0.15f) vertical corner posts of each shelf (8 total = 8 blocks)
- 8 bookshelf fill blocks: bookshelf scale(1.8f, 0.7f, 0.4f) flat shelf layers (8 blocks)
- 8 floating "ghost books": purple_stained_glass scale(0.5f, 0.7f, 0.05f) thin flat panels floating at various angles (8 blocks)
- 1 lectern: actual lectern block display at center (1 block)
- 6 lectern accent: gold_block scale(0.2f, 0.2f, 0.2f) floating above lectern (6 blocks)
- 4 dark_oak crossbars connecting shelves: scale(1.8f, 0.1f, 0.15f) horizontal (4 blocks)
- 5 atmospheric scatter: dark_oak_planks scale(0.4f, 0.4f, 0.4f) loose debris on shelves (5 blocks)

**Animation**: Ghost books fly out from shelves randomly (one every 30t) via teleport to radius 4 with teleportDuration=20, then return. Lectern gold accents orbit on Y-axis. Shelves gently sway as passenger group.
**Sounds**: `ENTITY_VEX_AMBIENT` (0.6f, 0.8f) | `BLOCK_ENCHANTMENT_TABLE_USE` every 40t
**Particles**: `ENCHANT` from lectern | `SOUL_FIRE_FLAME` from floating books | `REVERSE_PORTAL` drifting
**Config**: radius=7, damage=28, ticks-between=25, delay=15, duration=450, cooldown=320

---

### 14. THE NIGHTMARE DOOR — 31 blocks
**Visual**: An ornate freestanding doorframe with no wall around it — yet the space inside the door shows a different, wrong dimension. The door swings open and shut. Anything that passes through the opening takes damage.
**Materials**: crimson_planks (frame), blackstone_brick_stairs (ornate top arch), nether_bricks (frame inlays), purple_stained_glass (portal surface inside), shroomlight (glow accents)
**Assembly**:
- 2 door frame pillars: crimson_planks scale(0.45f, 3.0f, 0.45f) tall thick posts (2 blocks)
- 5 arch top: blackstone_brick_stairs in fan arc, scale(0.8f, 0.4f, 0.45f) (5 blocks)
- 4 frame inlay: nether_bricks scale(0.6f, 0.4f, 0.05f) flat decorative panels (4 blocks)
- 2 door wings: crimson_planks scale(0.9f, 2.4f, 0.08f) thin door panels (2 blocks)
- 4 portal fill: purple_stained_glass scale(0.85f, 0.7f, 0.02f) ultra-thin layered panels (4 blocks)
- 6 shroomlight accents: shroomlight scale(0.2f, 0.2f, 0.2f) at arch and pillar tops (6 blocks)
- 8 surrounding rune circles: nether_bricks scale(0.3f, 0.05f, 0.3f) flat plates on ground in ring (8 blocks)

**Animation**: Door wings swing open (Matrix4f rotateY 0°→80°) over 30t then swing closed over 30t, repeating. Portal fill shimmers scale 0.85↔0.9 every 10t. Rune circles rotate on Y slowly. Damage peaks when door is fully open (damageDelayTicks aligns with open phase).
**Sounds**: `BLOCK_IRON_DOOR_OPEN` | `BLOCK_END_PORTAL_FRAME_FILL` eerie loop | `ENTITY_ENDERMAN_TELEPORT`
**Particles**: `PORTAL` pouring from doorway | `REVERSE_PORTAL` from rune circles
**Config**: radius=6, damage=35, ticks-between=25, delay=30 (sync with door-open), duration=400, cooldown=300

---

### 15. THE ESCHER WELL — 33 blocks
**Visual**: A stone well that goes both up and down simultaneously — look down and you see sky, look up and you see depth. Impossible brick rings spiral at impossible angles.
**Materials**: stone_brick_walls (well ring), mossy_stone_bricks (aged texture), smooth_stone (flat plates), iron_bars (interior grating), tinted_glass (impossible depth panes), chain (bucket chain)
**Assembly**:
- 8 well wall ring blocks: stone_brick_walls in octagonal ring scale(0.7f, 1.2f, 0.7f) (8 blocks)
- 4 inner ring deeper: mossy_stone_bricks scale(0.5f, 0.8f, 0.5f) at Y-1.5 for depth illusion (4 blocks)
- 4 iron_bars grate: iron_bars in cross at Y-0.5 scale(0.1f, 0.8f, 0.1f) (4 blocks)
- 6 depth tinted_glass: tinted_glass scale(0.8f, 0.05f, 0.8f) flat plates at different Y levels below (6 blocks)
- 2 crossbeam support: smooth_stone scale(1.5f, 0.15f, 0.15f) crossed beams at top (2 blocks)
- 4 chain links: chain scale(0.3f, 0.6f, 0.3f) hanging from crossbeam (4 blocks)
- 5 scatter/rim accent: smooth_stone scale(0.5f, 0.1f, 0.5f) flat plates on rim (5 blocks)

**Animation**: Well ring slowly rotates Y. Inner rings counter-rotate. Depth glass panels oscillate Y scale 0.05↔0.1 creating shimmer. Chain sways sinusoidally via teleport (±0.3 offset every 15t, teleportDuration=15). Whole structure sinks Y-0.05/tick then jumps back up.
**Sounds**: `BLOCK_CHAIN_WALK` periodic | `AMBIENT_UNDERWATER_ENTER` (0.3f, 0.3f) impossible water sound
**Particles**: `REVERSE_PORTAL` rising from well | `FALLING_WATER` going upward (via Y+ velocity)
**Config**: radius=5, damage=25, ticks-between=20, delay=20, duration=400, cooldown=280

---

## CATEGORY 2: DemonicCreatures (DemonicCreatures.java)
*Demon shapes, nightmare beasts built from blocks with creature anatomy*
*Theme: Coal black, crimson, bone white, blackstone, nether brick red*

---

### 16. BAPHOMET LORD — 48 blocks
**Visual**: A towering goat-horned demon lord in humanoid pose — recognizable head with spiraling goat horns, winged back, cloven hooves, muscular arms raised overhead.
**Materials**: coal_block (main body), netherrack (flesh tones), bone_block (horns/teeth), blackstone (armor), nether_brick (muscle texture), shroomlight (red eyes)
**Assembly**:
- Torso: 4 coal_block scale(1.2f, 1.4f, 0.8f) wide body sections stacked (4 blocks)
- Head: 2 coal_block scale(1.0f, 1.0f, 0.9f) + 1 netherrack jaw scale(0.9f, 0.4f, 0.8f) (3 blocks)
- 2 Spiral horns: each horn 5 bone_blocks scale(0.25f, 0.6f, 0.25f) arranged in ascending arc with 15° rotation per segment (10 blocks)
- 2 Arms: each 3 coal_block scale(0.45f, 1.0f, 0.45f) elongated + 1 fist scale(0.7f, 0.7f, 0.7f) = 8 blocks
- 2 Wings: each 5 blackstone_stairs in fan arrangement, scale(0.9f, 0.1f, 0.7f) thin arched (10 blocks)
- 2 Legs: each 2 coal_block scale(0.5f, 1.1f, 0.5f) + 1 hoof nether_brick scale(0.7f, 0.3f, 0.9f) = 6 blocks
- 4 red eye accents: shroomlight scale(0.25f, 0.2f, 0.1f) at eye positions (4 blocks)
- 3 chest rune: nether_bricks scale(0.3f, 0.3f, 0.05f) flat plate runes on chest (3 blocks)

**Animation**: Arms raise over 30t (Matrix4f rotateX arm anchor forward). Wings flap — each wing rotates Z ±25° every 20t with interpolation=18. Head tracks nearest player via slow Y-rotation (teleportDuration=20 per head update, every 20t). Horn tips glow red.
**Sounds**: `ENTITY_RAVAGER_ROAR` on spawn (0.9f, 0.3f) | `ENTITY_BLAZE_AMBIENT` loop
**Particles**: `FLAME` from eyes | `SOUL_FIRE_FLAME` from hooves | `LAVA` at wing edges during flap
**Config**: radius=8, damage=40, ticks-between=20, delay=20, duration=500, cooldown=350, tracks-player=true

---

### 17. THE WATCHER — 36 blocks
**Visual**: A colossal floating eyeball — unmistakably an eye with iris, pupil, sclera, and 8 writhing tentacle-lash appendages extending outward.
**Materials**: bone_block (sclera), red_concrete (iris ring), coal_block (pupil core), obsidian (tendrils), crying_obsidian (iris detail), blackstone (pupil detail)
**Assembly**:
- Sclera sphere: 16 bone_block using spawnSphere(radius=2, count=16) scale(0.7f, 0.7f, 0.7f) each (16 blocks)
- Iris ring: 8 red_concrete blocks in ring at equator scale(0.5f, 0.5f, 0.2f) flat plates (8 blocks)
- Pupil core: 3 coal_block scale(0.6f, 0.6f, 0.3f) overlapping flat plates (3 blocks)
- 8 tentacle lashes: obsidian scale(0.12f, 0.12f, 1.5f) elongated rods pointing outward at equator-level at 45° angles (8 blocks)
- Iris detail: 1 crying_obsidian scale(0.4f, 0.4f, 0.15f) veined iris center (1 block)

**Animation**: Entire eye rotates on Z-axis slow (200-tick rotation). Iris ring spins counter to sclera on Y. Tentacle lashes oscillate scale 0.12↔0.2 and extend/retract (scale 1.5↔2.5 on Z) with 8-tick phase offset between them. Pupil tracks player by Y-rotating the pupil layer toward nearest player.
**Sounds**: `ENTITY_ELDER_GUARDIAN_AMBIENT` | `ENTITY_ENDERMAN_STARE` loop (0.3f, 0.4f)
**Particles**: `REVERSE_PORTAL` around eyeball perimeter | `SCULK_SOUL` from tentacle tips
**Config**: radius=8, damage=35, ticks-between=15, delay=10, duration=400, cooldown=300, tracks-player=true

---

### 18. ABYSSAL LEVIATHAN — 50 blocks
**Visual**: Multi-headed serpentine sea monster made of bone/obsidian — three necks and three distinct serpent heads rising from a coiling body.
**Materials**: bone_block (skeleton), obsidian (scales), netherrack (gum/flesh inside mouths), crying_obsidian (accent), blackstone (body segments)
**Assembly**:
- Body coil base: 8 blackstone scale(0.9f, 0.6f, 0.9f) in S-curve arrangement decreasing in size (8 blocks)
- 3 necks × 5 segments each: bone_block scale(0.55f, 0.6f, 0.55f) stacked, each neck at different angle 60° apart = 15 blocks
- 3 heads × 4 blocks each: 2 obsidian scale(0.9f, 0.7f, 1.0f) upper skull + 1 netherrack jaw scale(0.85f, 0.3f, 0.9f) lower jaw + 1 bone_block snout = 12 blocks
- 12 obsidian scale plates: scale(0.7f, 0.1f, 0.5f) flat plates along neck sides (12 blocks)
- 3 shroomlight eyes: scale(0.25f, 0.2f, 0.15f) per head (3 blocks)

**Animation**: Body coil slowly rotates Y. Each neck independently sways (sine-wave Y-oscillation, phase-offset 120° between necks). Each head independently tracks player — the neck with nearest alignment "strikes" by extending (Y-scale 0.6→0.9 over 15t). Jaw on striking head opens (Matrix4f rotateX on jaw -20°→0°).
**Sounds**: `ENTITY_ENDER_DRAGON_GROWL` (0.7f, 0.3f) | `ENTITY_GHAST_WARN` on neck strike
**Particles**: `LAVA` from mouths | `SOUL_FIRE_FLAME` along body | `DRIPPING_OBSIDIAN_TEAR` from neck scales
**Config**: radius=7, damage=38, ticks-between=20, delay=15, duration=500, cooldown=350, tracks-player=true

---

### 19. THE IMP TRIBUNAL — 44 blocks
**Visual**: Five distinct imp figures standing in a semicircle, each 8 blocks tall, pointing accusingly at the player. Each imp slightly different.
**Materials**: netherrack (body), red_nether_bricks (clothing), blackstone (feet/hands), bone_block (horns), shroomlight (eyes)
**Assembly** (per imp × 5, slight variations):
- Body: 2 netherrack scale(0.6f, 0.8f, 0.5f) stacked torso/head (2 blocks)
- Arms: 2 netherrack scale(0.3f, 0.7f, 0.3f) elongated arms with pointing pose via Matrix4f rotateX (2 blocks)
- Legs: 2 netherrack scale(0.3f, 0.7f, 0.3f) (2 blocks)
- 1 horn: bone_block scale(0.2f, 0.4f, 0.2f) atop head (1 block)
- 1 shroomlight eye (half-imp has 2): scale(0.2f, 0.15f, 0.1f) (1 block)
- Total per imp: ~8 blocks × 5 imps = 40 blocks
- 4 floor rune accent: nether_bricks flat plates in ring between imps (4 blocks)

**Animation**: Each imp independently hops (Y-translation bounce every 30t with 6-tick offset per imp). Arms alternate point vs. idle via Matrix4f rotateX on arm anchor (0°→-40°→0°). Heads slowly turn toward player (teleportDuration=25 per head update). Middle imp "shrieks" — scale 1.0→1.2→1.0 every 60t.
**Sounds**: `ENTITY_VEX_HURT` × 3 staggered | `ENTITY_BLAZE_SHOOT` (0.4f, 1.2f)
**Particles**: `SMOKE` from each imp | `FLAME` eyes | `LAVA` from pointing finger tips
**Config**: radius=6, damage=30, ticks-between=15, delay=10, duration=450, cooldown=300, tracks-player=true

---

### 20. THE SOUL REAPER — 42 blocks
**Visual**: A classic grim reaper figure — towering skeletal humanoid in dark robes, enormous curved scythe raised, hollow skull grinning.
**Materials**: bone_block (skeleton), black_concrete (robes), blackstone (scythe blade), obsidian (robe deep shadow), shroomlight (eye glow), polished_blackstone (scythe handle)
**Assembly**:
- Skull: 3 bone_block — 2 scale(0.9f, 0.8f, 0.85f) upper skull + 1 scale(0.85f, 0.3f, 0.8f) jaw (3 blocks)
- 2 eye shroomlight: scale(0.3f, 0.25f, 0.15f) (2 blocks)
- Robe body: 5 black_concrete scale(1.1f, 1.0f, 0.8f) tapering toward bottom (5 blocks)
- Robe drape: 8 black_concrete scale(1.4f, 0.3f, 0.9f) flat wide plates at different Y heights creating layered drape (8 blocks)
- Arms: 2 bone_block scale(0.3f, 1.2f, 0.3f) thin elongated arms emerging from robe (2 blocks)
- Scythe handle: 5 polished_blackstone scale(0.12f, 1.0f, 0.12f) thin rod segments (5 blocks)
- Scythe blade: 7 blackstone_stairs in crescent arc — scale(0.9f, 0.3f, 0.6f) each rotated to form curved blade (7 blocks)
- 5 robe hem ground: obsidian scale(0.8f, 0.05f, 0.5f) flat thin ground-level drape (5 blocks)
- 5 floating robe fragments: black_concrete scale(0.3f, 0.3f, 0.3f) around figure (5 blocks)

**Animation**: Entire reaper floats 2 blocks off ground, bobbing ±0.4 every 40t. Scythe sweeps — arm anchor rotates Z from -30°→-180° over 20t (sweeping slash) then resets over 10t, repeating every 60t. Robe drape layers oscillate Z-scale 1.0↔1.2 with sequential offsets creating billowing. Head slowly turns toward player.
**Sounds**: `ENTITY_VEX_DEATH` (0.8f, 0.3f) | `ENTITY_WITHER_SKELETON_AMBIENT` | `ENTITY_PHANTOM_FLAP`
**Particles**: `SOUL_FIRE_FLAME` from eyes | `SOUL` trailing behind scythe during sweep | `DRIPPING_OBSIDIAN_TEAR` from robe hem
**Config**: radius=8, damage=42, ticks-between=20, delay=5, duration=500, cooldown=350, tracks-player=true

---

### 21. THE PLAGUE MOTH — 40 blocks
**Visual**: A giant moth with skull-patterned wings spread wide — immediately recognizable as a death's-head hawkmoth scaled massive. Wings span ~8 blocks.
**Materials**: brown_mushroom_block (wing membrane), blackstone (wing veins), bone_block (skull wing pattern), coal_block (body), shroomlight (skull eyes on wings), white_wool (lighter wing areas)
**Assembly**:
- Body: 4 coal_block scale(0.5f, 0.8f, 0.5f) stacked cylindrical segments (4 blocks)
- Head: 2 coal_block scale(0.7f, 0.6f, 0.7f) (2 blocks)
- 2 antenna: bone_block scale(0.08f, 0.08f, 1.2f) thin rods curving up (2 blocks)
- 2 lower wing panels: brown_mushroom_block scale(2.5f, 0.08f, 1.5f) ultra-thin large plates (2 blocks)
- 2 upper wing panels: brown_mushroom_block scale(2.0f, 0.08f, 1.2f) thin plates angled higher (2 blocks)
- Wing veins: 8 blackstone scale(0.06f, 0.08f, 0.9f) thin rods on wing surfaces (8 blocks)
- Skull pattern (on wings): 4 bone_block scale(0.5f, 0.5f, 0.05f) arranged as skull face on wings (4 blocks)
- 4 shroomlight skull eyes: scale(0.18f, 0.15f, 0.05f) glowing on wing skulls (4 blocks)
- 6 white_wool lighter wing sections: scale(0.7f, 0.08f, 0.4f) spot patterns (6 blocks)
- 6 leg hooks: blackstone scale(0.1f, 0.5f, 0.1f) six insect legs below body (6 blocks)

**Animation**: Wings flap — wing anchor rotates Z ±20° every 15t (interpolation=13). Wing flap is slightly asymmetric (different interpolation durations left vs right) for organic feel. Moth circles player at radius 6 (Marker teleport arc, teleportDuration=10, updated every 5t). Body segments sway Y.
**Sounds**: `ENTITY_PHANTOM_FLAP` | `ENTITY_BAT_AMBIENT` (0.5f, 0.3f)
**Particles**: `ASH` from wings | `SOUL_FIRE_FLAME` from skull eyes | `FALLING_SPORE_BLOSSOM` scattered below
**Config**: radius=6, damage=32, ticks-between=20, delay=10, duration=450, cooldown=310, tracks-player=true

---

### 22. BONE COLOSSUS — 52 blocks
**Visual**: A towering skeletal giant, 8 blocks tall, stomping toward the player. Every element identifiable — ribcage, spine, arm bones, leg bones, skull.
**Materials**: bone_block (skeleton), netherrack (marrow/inner detail), blackstone (shadow), sculk_sensor (detail), pale_oak_planks (no — use smooth_bone = bone_block), wither_skeleton_skull (if obtainable — else carved_pumpkin)
**Assembly**:
- Skull: 3 bone_block — 2 cranium scale(1.1f, 0.9f, 1.0f) + 1 jaw scale(1.0f, 0.4f, 0.9f) (3 blocks)
- Spine: 6 bone_block scale(0.35f, 0.5f, 0.35f) stacked (6 blocks)
- Ribcage: 8 bone_block arranged as 4 rib pairs, each pair scale(0.15f, 0.5f, 0.08f) curving outward via Matrix4f rotateZ (8 blocks)
- Pelvis: 3 bone_block scale(1.2f, 0.5f, 0.8f) wide hip bones (3 blocks)
- 2 upper arms: bone_block scale(0.4f, 1.1f, 0.4f) each (2 blocks)
- 2 forearms: bone_block scale(0.35f, 0.9f, 0.35f) + 1 fist scale(0.6f, 0.6f, 0.6f) each = 6 blocks
- 2 thighbones: bone_block scale(0.5f, 1.3f, 0.5f) each (2 blocks)
- 2 shinbones: bone_block scale(0.4f, 1.2f, 0.4f) each (2 blocks)
- 2 feet: bone_block scale(0.8f, 0.3f, 1.0f) flat feet (2 blocks)
- Rib detail fill: 16 netherrack scale(0.1f, 0.1f, 0.1f) marrow dots in ribcage interior (16 blocks)
- Glow accents: sculk_sensor scale(0.2f, 0.2f, 0.2f) at skull eye sockets × 2 (2 blocks)

**Animation**: Entire colossus on Marker that walks toward player (arc movement, teleportDuration=15, step every 10t). Right leg lifts 30° forward, left stays back, alternating every 20t — "walking" animation via arm anchor Matrix4f rotateX. Arms swing opposite to legs. On reaching player: stomp — leg anchor slams down (translateY -2 over 5t), triggers damageOnImpactOnly at radius 8.
**Sounds**: `ENTITY_IRON_GOLEM_STEP` on each stomp | `ENTITY_SKELETON_HURT` | `ENTITY_WITHER_SPAWN` on spawn
**Particles**: `BLOCK_CRACK(BONE_BLOCK)` during steps | `SOUL` rising from footsteps
**Config**: radius=8, impact-only=false, damage=40, ticks-between=20, delay=20, duration=500, cooldown=400, tracks-player=true

---

### 23. THE INFERNAL GARGOYLE — 38 blocks
**Visual**: A stone gargoyle perched on a rocky pedestal — hunched bat-like body, gargoyle head with open fanged mouth, wings folded tight, clawed feet gripping. Then it activates and swoops.
**Materials**: deepslate_bricks (stone body), polished_deepslate (wing membrane), blackstone_stairs (angular body parts), obsidian (dark shadow blocks), shroomlight (eyes), red_nether_bricks (mouth interior)
**Assembly**:
- Pedestal: 4 blackstone_bricks scale(0.8f, 0.5f, 0.8f) stacked base (4 blocks)
- Hunched body: 4 deepslate_bricks scale(1.0f, 0.9f, 0.8f) + 2 scale(0.8f, 0.7f, 0.7f) shoulder humps (6 blocks)
- Head: 2 deepslate_bricks scale(0.9f, 0.8f, 1.0f) + 1 red_nether_bricks jaw scale(0.8f, 0.3f, 0.9f) (3 blocks)
- Fangs: 4 bone_block scale(0.15f, 0.4f, 0.15f) pointed downward in mouth (4 blocks)
- 2 Wings folded: polished_deepslate scale(0.8f, 0.08f, 1.8f) thin flat wings folded along back (2 blocks)
- 4 wing ribs: obsidian scale(0.07f, 0.08f, 1.5f) thin rods along wing span (4 blocks)
- 2 clawed feet: 3 deepslate_bricks blocks + 3 blackstone_stairs as talons each side = 6 blocks
- 2 shroomlight eyes: scale(0.3f, 0.25f, 0.15f) red-glow (2 blocks)
- 5 accent blocks: various texture detail (5 blocks)
- Tail: 2 deepslate_bricks scale(0.3f, 0.3f, 0.8f) (2 blocks)

**Animation**: Starts perched (crouched, wings folded). After 30t: eyes glow brighter, wings unfold (Matrix4f rotateZ 0°→80° over 20t). After 60t: gargoyle swoops — Marker dives toward player (teleportDuration=8, updated every 4t following player). Swoops in arc, passes through, circles back. Wings flap while moving.
**Sounds**: `ENTITY_PHANTOM_DEATH` (0.7f, 0.4f) on launch | `ENTITY_PHANTOM_FLAP` during swoop | `BLOCK_STONE_BREAK` on pedestal
**Particles**: `BLOCK_CRACK(DEEPSLATE_BRICKS)` from pedestal cracking | `SOUL_FIRE_FLAME` from eyes during swoop | `SMOKE` trailing
**Config**: radius=6, damage=38, ticks-between=15, delay=60, duration=450, cooldown=350, tracks-player=true

---

### 24. THE CHIMERA — 44 blocks
**Visual**: Three-headed monster — lion head, goat head, dragon head — on one body, each head at a different angle. Recognizable from mythological iconography.
**Materials**: orange_concrete (lion), netherrack (goat/flesh), coal_block (dragon), bone_block (teeth/horns), blackstone (body), red_concrete (mane), shroomlight (eyes)
**Assembly**:
- Main body: 5 blackstone scale(1.2f, 1.0f, 1.0f) (5 blocks)
- Lion head: 2 orange_concrete + 1 bone_block muzzle + 1 red_concrete mane ring scale(1.2f, 0.3f, 1.2f) = 4 blocks; mane: 6 red_concrete scale(0.4f, 0.4f, 0.4f) surrounding head = 10 blocks total
- Goat head: 2 netherrack + 1 bone_block muzzle + 2 bone_block horns curved = 5 blocks
- Dragon head: 2 coal_block + 1 bone_block fang row + 1 netherrack frill = 4 blocks
- 4 legs: blackstone scale(0.45f, 1.0f, 0.45f) each = 4 blocks
- Tail (dragon tail): 5 coal_block scale(0.3f, 0.3f, 0.7f) in tapering arc = 5 blocks
- 6 shroomlight eyes: 2 per head = 6 blocks
- 5 other detail blocks (claws, mane-edge, etc.) = 5 blocks

**Animation**: Each of 3 heads tracks player independently (rotating toward nearest player on Y+X axes via teleportDuration=20). Each head "snaps" at player in turn — extends neck scale 1.0→1.5 over 8t then retracts (staggered every 25t). Body slowly rotates to reorient overall. Lion head shakes mane (scale pulse). Dragon head emits flame particles on snap.
**Sounds**: `ENTITY_RAVAGER_ROAR` (lion) | `ENTITY_ENDER_DRAGON_GROWL` (dragon) | `ENTITY_GOAT_HORN_BREAK` (goat) — rotating which plays
**Particles**: `FLAME` from dragon head | `CRIT` from lion head | `SMOKE` from goat head
**Config**: radius=7, damage=38, ticks-between=15, delay=10, duration=500, cooldown=350, tracks-player=true

---

### 25. NIGHTMARE SPIDER BROOD — 42 blocks
**Visual**: A massive queen spider with 4 smaller spiders arranged around it. Queen has 8 hairy legs, large abdomen, cluster of eyes. Brood shares the queen's glow.
**Materials**: coal_block (body), obsidian (abdomen), blackstone (leg segments), bone_block (spines/fangs), red_concrete (eye cluster), nether_bricks (leg joints)
**Assembly** (Queen = 24 blocks):
- Cephalothorax: 3 coal_block scale(0.9f, 0.7f, 1.0f) (3 blocks)
- Abdomen: 4 obsidian scale(1.1f, 0.9f, 1.3f) tapered oval (4 blocks)
- 8 legs × 2 segments each: blackstone scale(0.15f, 0.08f, 0.9f) angled at varying degrees via Matrix4f = 16 blocks
- Eye cluster: 1 red_concrete scale(0.6f, 0.4f, 0.1f) + 1 bone_block fang = 1 block

(4 small spiders × 3 blocks each = 12 blocks):
- Each: 1 coal_block body scale(0.5f, 0.4f, 0.6f) + 4 thin blackstone legs via scale(0.08f, 0.06f, 0.6f) arranged ×8 in pairs but 4 used scale = combined 2 scaled wide blocks = 3 blocks each

**Animation**: Queen stays stationary, legs oscillate in walking gait (alternating pairs lift via Matrix4f rotateX on leg anchor ±15°, 12t cycle). Small spiders circle queen on individual circular paths (teleport arc, different radii 3–5, different speeds). One spider periodically lunges toward player. Abdomen pulses scale.
**Sounds**: `ENTITY_SPIDER_AMBIENT` × 2 | `ENTITY_SPIDER_HURT` on lunge
**Particles**: `DRIPPING_POISON` from fangs | `SMOKE` from abdomen | `CRIT` from spider eyes
**Config**: radius=7, damage=30, ticks-between=15, delay=10, duration=450, cooldown=300

---

### 26. THE VOID WRAITH — 35 blocks
**Visual**: A translucent hovering entity — partially visible humanoid form made of floating, disconnected bone/obsidian fragments drifting loosely together like a ghost held by unseen force.
**Materials**: bone_block (skeleton fragments), obsidian (dark shadow masses), tinted_glass (translucent ghost form), crying_obsidian (glow tears), sculk (dark ambient)
**Assembly**:
- 16 floating bone fragments: bone_block scale(0.35f→0.5f varying, 0.35f, 0.35f) scattered in humanoid silhouette at random small offsets (16 blocks)
- 8 obsidian shadow masses: scale(0.4f, 0.4f, 0.4f) filling gaps between bones (8 blocks)
- 4 tinted_glass form panels: scale(0.8f, 1.5f, 0.05f) ultra-thin translucent layered (4 blocks)
- 4 crying_obsidian tears: scale(0.2f, 0.3f, 0.2f) drifting from eye area (4 blocks)
- 3 sculk ambient: scale(0.3f, 0.1f, 0.3f) flat plates at base (3 blocks)

**Animation**: The wraith is constantly shifting — all 24 bone/obsidian fragments independently drift (each gets a unique sine offset, 0.1-0.3 amplitude, 15-30 tick period, teleportDuration=8). The overall mass tracks player slowly (Marker teleport, teleportDuration=25, every 20t). Tinted glass flickers opacity (via scale Z 0.05↔0.01).
**Sounds**: `ENTITY_VEX_AMBIENT` (0.7f, 0.4f) | `ENTITY_PHANTOM_AMBIENT` | `ENTITY_WITHER_SKELETON_AMBIENT`
**Particles**: `SOUL_FIRE_FLAME` from fragments | `SCULK_SOUL` rising | `REVERSE_PORTAL` surrounding
**Config**: radius=6, damage=30, ticks-between=15, delay=5, duration=450, cooldown=280, tracks-player=true

---

### 27. THE NIGHTMARE CENTIPEDE — 45 blocks
**Visual**: A massive centipede 10+ segments long, undulating and writhing, clearly segmented with legs and a fanged head. The longest creature attack in Devil's Dream.
**Materials**: red_nether_bricks (body segments), netherrack (underbelly), bone_block (fangs/legs), blackstone (head), shroomlight (compound eyes)
**Assembly**:
- 12 body segments: red_nether_bricks scale(0.8f, 0.5f, 0.8f) in winding horizontal line, each slightly angled (12 blocks)
- Head: 2 blackstone scale(1.0f, 0.7f, 1.0f) + 1 netherrack jaw (3 blocks)
- 4 fangs: bone_block scale(0.15f, 0.4f, 0.15f) from head (4 blocks)
- 2 compound eyes: shroomlight scale(0.4f, 0.3f, 0.2f) (2 blocks)
- 24 legs: bone_block scale(0.1f, 0.08f, 0.5f) two legs per segment × 12 = 24 blocks
- Tail tip: 2 red_nether_bricks scale(0.4f, 0.3f, 0.4f) (doesn't add to 45 but approximate)

**Animation**: Body segments follow each other in caterpillar chain motion — each segment's Y-position follows a sine wave with phase offset proportional to segment index (1.5-tick offset per segment, 25-tick period, 0.4 amplitude). Legs alternate up/down in pairs synchronized with segment motion. Head leads toward player. Whole centipede circles player at radius 5.
**Sounds**: `ENTITY_SPIDER_STEP` × multiple | `ENTITY_BEE_LOOP` (0.3f, 0.3f) chittering
**Particles**: `CRIT` from leg tips | `DRIPPING_POISON` from fangs | `BLOCK_CRACK(NETHERRACK)` during movement
**Config**: radius=5, damage=28, ticks-between=10, delay=10, duration=450, cooldown=280, tracks-player=true

---

### 28. DEMON WINGS MANIFESTATION — 36 blocks
**Visual**: Just the wings — two enormous bat/demon wings, no body, filling the sky above the player. The wings beat down hard, slamming the area.
**Materials**: blackstone (wing membrane), obsidian (wing bones/ribs), red_concrete (wing vein glow lines), bone_block (outer edge spines), crimson_planks (inner membrane lighter)
**Assembly**:
- 2 main wing membranes: blackstone scale(3.5f, 0.06f, 2.5f) ultra-thin massive flat plates (2 blocks)
- 2 inner lighter wing areas: crimson_planks scale(2.0f, 0.05f, 1.5f) overlapping (2 blocks)
- 10 wing rib bones: obsidian scale(0.08f, 0.07f, 2.2f) fan out from wing joint on each wing × 5 each (10 blocks)
- 6 outer edge spines: bone_block scale(0.15f, 0.15f, 0.25f) at wing tips (6 blocks)
- 4 red vein lines: red_concrete scale(0.06f, 0.05f, 1.8f) overlaid on membranes (4 blocks)
- 2 wing joint shoulder blocks: blackstone scale(0.6f, 0.6f, 0.6f) where wings meet center (2 blocks)
- 8 dark fragment scatter: obsidian scale(0.2f, 0.2f, 0.2f) floating at wing tips and joints (8 blocks)
- 2 shadow casting flat plates: blackstone scale(4.0f, 0.02f, 3.0f) at Y+8 creating shadow (2 blocks)

**Animation**: Wings start open at Y+6 above player. Beat down — both wings simultaneously rotate X-axis (wing anchor rotateX 0°→-60°→0° over 15t then 10t reset, repeating every 40t). On downbeat: velocity pulse pushes all players downward. Massive shadow effect from top plates. Membrane inner lighter area alternates scale on beat.
**Sounds**: `ENTITY_PHANTOM_FLAP` × 2 loud | `ENTITY_ENDER_DRAGON_FLAP` (0.9f, 0.4f) on downbeat
**Particles**: `ASH` raining from wings | `SOUL_FIRE_FLAME` from rib tips | `CAMPFIRE_SIGNAL_SMOKE` above wings
**Config**: radius=9, damage=35, ticks-between=15, delay=5, duration=400, cooldown=300

---

### 29. ABYSSAL CRAB — 42 blocks
**Visual**: A massive crustacean with an oversized pincer claw, heavy shell, and multiple legs. The giant claw snaps at the player.
**Materials**: blackstone (shell), obsidian (claw), nether_bricks (leg segments), bone_block (claw teeth), deepslate_bricks (body underside), shroomlight (stalked eyes)
**Assembly**:
- Shell: 5 blackstone scale(1.5f, 0.8f, 2.0f) dome plates tapering with overlapping (5 blocks)
- Body underside: 3 deepslate_bricks scale(1.2f, 0.3f, 1.6f) flat ventral plates (3 blocks)
- Giant claw (right): 4 obsidian — 2 arm scale(0.4f, 0.4f, 1.5f) + 2 claw fingers scale(0.3f, 0.3f, 1.0f) (4 blocks)
- Claw teeth: 4 bone_block scale(0.15f, 0.25f, 0.15f) on inner claw edge (4 blocks)
- Small claw (left): 2 obsidian scale(0.3f, 0.3f, 0.7f) smaller claw (2 blocks)
- 6 walking legs: nether_bricks scale(0.2f, 0.1f, 1.0f) × 3 pairs, angled down-out (6 blocks per leg = simplified as single oriented block) = 6 blocks
- 2 stalked eyes: blackstone scale(0.1f, 0.5f, 0.1f) stalk + shroomlight scale(0.3f, 0.3f, 0.3f) eye = 4 blocks (stalks + eyes)
- 6 shell detail accent: blackstone stairs at scale(0.5f, 0.2f, 0.5f) for ridged shell texture (6 blocks)
- 4 scatter legs extra detail: nether_bricks (4 blocks)
- 4 base/sand scatter: deepslate_bricks scale(0.3f, 0.1f, 0.3f) (4 blocks)

**Animation**: Crab sidesteps (Marker teleport sideways arc, teleportDuration=10, every 5t). Giant claw snaps open/closed (Matrix4f rotateZ on claw finger anchors ±35° every 20t). Eye stalks independently bob and track player (teleportDuration=15). Shell rocks from side to side via gentle X-rotation.
**Sounds**: `ENTITY_SHULKER_SHOOT` (claw snap) | `ENTITY_ELDER_GUARDIAN_HURT` | `BLOCK_WET_GRASS_STEP` scuttling
**Particles**: `BUBBLE_COLUMN_BUBBLE` from claw snaps | `CRIT` from pincer impact | `BLOCK_CRACK(BLACKSTONE)` from shell
**Config**: radius=6, damage=36, ticks-between=15, delay=10, duration=480, cooldown=320, tracks-player=true

---

### 30. THE FACELESS TALL ONE — 36 blocks
**Visual**: A towering 6-block tall humanoid with a perfectly featureless face and unnaturally long arms — Silent Hill-inspired horror. Thin, stretched body. No eyes, no mouth. Just a smooth blank surface.
**Materials**: bone_block (pale body), white_concrete (featureless face), polished_blackstone (dark limbs), obsidian (shadow joints), sculk (darkness at feet)
**Assembly**:
- Body (torso + neck): 4 bone_block scale(0.7f, 1.2f, 0.5f) stacked (4 blocks)
- Featureless head: 2 white_concrete scale(0.8f, 0.9f, 0.7f) smooth blank (2 blocks)
- 2 Arms (extremely long): bone_block scale(0.25f, 2.0f, 0.25f) hanging nearly to ground × 2 (2 blocks)
- 2 Legs: polished_blackstone scale(0.3f, 1.5f, 0.3f) thin long legs (2 blocks)
- 2 Feet: polished_blackstone scale(0.6f, 0.2f, 0.8f) (2 blocks)
- 8 obsidian joint dots: scale(0.2f, 0.2f, 0.2f) at shoulders, elbows, knees, hips (8 blocks)
- 8 sculk ground aura: scale(0.4f, 0.02f, 0.4f) flat dark plates spreading around feet in expanding ring (8 blocks)
- 8 floating fragments: bone_block scale(0.2f, 0.2f, 0.2f) orbiting figure slowly (8 blocks)

**Animation**: Walks toward player via smooth Marker teleport (teleportDuration=20 every 10t, slow pace). Long arms sway slightly (rotateX ±10° on arm anchors, 40t period). Head slowly turns toward player. Occasional "jitter" — entire figure teleports 1 block in random direction then back (unsettling teleport effect). Sculk ground plates expand outward then contract.
**Sounds**: `ENTITY_VEX_AMBIENT` barely audible (0.2f, 0.3f) | `BLOCK_SCULK_SENSOR_CLICKING` intermittent | silence with occasional `ENTITY_ENDERMAN_STARE`
**Particles**: `SCULK_SOUL` at feet | `SCULK_CHARGE` particles | `SOUL_FIRE_FLAME` very faint from eye area (even though no eyes are visible)
**Config**: radius=5, damage=32, ticks-between=20, delay=10, duration=500, cooldown=320, tracks-player=true

---

## CATEGORY 3: BodyHorror (BodyHorror.java)
*Giant skulls, teeth walls, bone structures, eyes, reaching hands*
*Theme: Bone white, netherrack flesh, obsidian, red accent, shroomlight eyes*

---

### 31. THE SCREAMING SKULL — 46 blocks
**Visual**: A giant skull — proportionate to a human but 3× oversized — with jaw opening in a perpetual scream. Recognizable: cranium, cheekbones, eye sockets, teeth, nasal cavity.
**Materials**: bone_block (cranium/face), netherrack (inner mouth/nasal cavity), blackstone (shadow depths), polished_blackstone (teeth), shroomlight (glowing eye sockets)
**Assembly**:
- Cranium: 8 bone_block arranged in dome-like formation, scale varying 1.0→0.8 toward top (8 blocks)
- Cheekbone ridges: 2 bone_block scale(0.8f, 0.3f, 0.6f) flat ridges (2 blocks)
- Nasal cavity: 2 blackstone scale(0.4f, 0.5f, 0.3f) dark recesses (2 blocks)
- Eye sockets: 2 blackstone scale(0.7f, 0.6f, 0.4f) deep recesses + 2 shroomlight scale(0.4f, 0.35f, 0.15f) glow inside = 4 blocks
- Upper jaw: 4 bone_block forming upper palette including forehead/brow (4 blocks)
- Upper teeth row: 8 polished_blackstone scale(0.25f, 0.5f, 0.2f) teeth along upper jaw (8 blocks)
- Lower jaw: 3 bone_block movable jaw anchor (3 blocks)
- Lower teeth row: 8 polished_blackstone scale(0.25f, 0.45f, 0.2f) lower teeth (8 blocks)
- Neck/base attachment: 3 bone_block scale(0.5f, 0.5f, 0.5f) (3 blocks)
- Netherrack throat interior: 2 netherrack scale(0.6f, 0.3f, 0.4f) (2 blocks)

**Animation**: Jaw opens (Matrix4f rotateX on jaw anchor 0°→-40°→0°→-60° over 120t cycle — the scream). Jaw opening triggers `ENTITY_GHAST_SCREAM` sound. Eye sockets glow (shroomlight scale 0.4→0.6 on 30t pulse). Skull slowly rotates to face player. Skull bobs slightly while screaming. Teeth in lower jaw shake during scream.
**Sounds**: `ENTITY_GHAST_SCREAM` (1.0f, 0.3f) on full jaw-open | `ENTITY_VEX_AMBIENT` loop | `ENTITY_WITHER_AMBIENT`
**Particles**: `SOUL_FIRE_FLAME` from eye sockets | `SOUL` from mouth | `DRIPPING_OBSIDIAN_TEAR` from jaw
**Config**: radius=7, damage=38, ticks-between=20, delay=10, duration=500, cooldown=350, tracks-player=true

---

### 32. THE EYE GARDEN — 40 blocks
**Visual**: 7 giant eyeballs on varying-height fleshy stalks, each eye independently tracking the player, all blinking out of sync. A horrible garden of watching eyes.
**Materials**: bone_block (sclera), red_concrete (iris), coal_block (pupil), netherrack (stalk/flesh), polished_blackstone (veins on sclera), shroomlight (glow accents)
**Assembly** (per eye × 7 approx, plus extras):
Per stalk+eye (~4 blocks each = 28 blocks):
- Stalk: netherrack scale(0.2f, height_varies_0.5_to_2.0f, 0.2f) (1 block per stalk)
- Eye ball (sclera): bone_block scale(0.7f, 0.7f, 0.6f) (1 block)
- Iris: red_concrete scale(0.5f, 0.5f, 0.15f) flat plate in front of sclera (1 block)
- Pupil: coal_block scale(0.3f, 0.3f, 0.08f) flat plate in front of iris (1 block)
= 4 per eye × 7 = 28 blocks

Additional detail:
- 4 vein lines: polished_blackstone scale(0.05f, 0.05f, 0.6f) on each sclera × some = 4 blocks
- 8 scattered stalk bases: netherrack scale(0.5f, 0.1f, 0.5f) flat plates at stalk bases (8 blocks)

**Animation**: Each eye has an independent "blink" cycle — the iris/pupil scale Y from 0.5→0.05 (eye closing) over 3t then back (blinking). Each eye tracks nearest player independently (iris+pupil translate to aim at player, updated every 10t). Stalks sway (sine oscillation X/Z, 20-30t period, different per stalk). One eye periodically twitches rapidly.
**Sounds**: `ENTITY_SHULKER_TELEPORT` (blinking sound) | `ENTITY_ENDERMAN_STARE` (0.3f, 0.5f) loop
**Particles**: `SOUL_FIRE_FLAME` from pupil centers | `DRIPPING_OBSIDIAN_TEAR` from stalk bases
**Config**: radius=8, damage=30, ticks-between=20, delay=5, duration=450, cooldown=300, tracks-player=true

---

### 33. THE HAND WALL — 44 blocks
**Visual**: A solid wall of stone from which dozens of arms are reaching and grasping outward. The hands are unmistakably hands — palm, fingers, knuckles. Some fingers individually curl.
**Materials**: deepslate_bricks (wall base), bone_block (hand/fingers), netherrack (palm interior), polished_blackstone (fingernail accents), blackstone (wrist joint)
**Assembly**:
- Wall: 4 deepslate_bricks scale(2.5f, 2.5f, 0.25f) vertical flat panels forming wall (4 blocks)
- 6 distinct arms emerging from wall: each arm = 1 blackstone scale(0.4f, 0.4f, 0.6f) wrist + 1 bone_block scale(0.6f, 0.4f, 0.7f) palm + 5 bone_block finger rods scale(0.12f, 0.5f, 0.12f) = 7 blocks per arm × 6 = 42 blocks total (34 finger rods count). Simplify: 5 arms × ~6 blocks = 30 blocks
- Netherrack palm detail: 3 netherrack scale(0.4f, 0.25f, 0.15f) per palm × 5 = 5 blocks
- 5 accent: polished_blackstone scale(0.1f, 0.1f, 0.05f) fingernail details = 5 blocks

**Animation**: All arms emerge together from wall (scale from 0.01→full over 20t). Fingers on each hand independently curl (Matrix4f rotateX on each finger anchor, 0°→-50°→0° on 15-tick grasping cycle). Each arm has slight reach pulse (Z-scale 0.6→1.0 on 20t intervals staggered). One arm reaches much further every 60t (grab attempt).
**Sounds**: `BLOCK_DEEPSLATE_BREAK` cracking from wall | `ENTITY_IRON_GOLEM_HURT` straining | `ENTITY_VEX_HURT` from hands
**Particles**: `BLOCK_CRACK(DEEPSLATE_BRICKS)` from wall gaps | `SCULK_SOUL` rising from cracks
**Config**: radius=7, damage=32, ticks-between=15, delay=10, duration=450, cooldown=300

---

### 34. THE TOOTHED CORRIDOR — 45 blocks
**Visual**: Two parallel walls of massive serrated teeth forming a corridor — like being inside a giant mouth. Teeth slide up/down threatening to close and bite.
**Materials**: polished_blackstone (tooth blocks), bone_block (tooth roots), netherrack (gum flesh), blackstone (jaw bone structure), red_concrete (bloody gum accents)
**Assembly**:
- 2 jaw-bone base plates: blackstone scale(4.0f, 0.3f, 0.5f) flat wide jaw bases on each side (2 blocks)
- 2 gum flesh layers: netherrack scale(3.8f, 0.25f, 0.4f) on top of jaw bone (2 blocks)
- 14 upper teeth: polished_blackstone scale(0.5f, 1.5f, 0.4f) pointed downward, 7 per upper jaw (14 blocks)
- 14 lower teeth: polished_blackstone scale(0.5f, 1.2f, 0.4f) pointing upward, 7 per lower jaw (14 blocks)
- 6 tooth roots: bone_block scale(0.3f, 0.4f, 0.3f) at base of teeth (6 blocks)
- 4 red gum accent: red_concrete scale(0.6f, 0.1f, 0.4f) (4 blocks)
- 3 saliva drip: tinted_glass scale(0.1f, 0.5f, 0.1f) thin rod drips between teeth (3 blocks)

**Animation**: Upper jaw descends (translateY from +2.5→-0.5 over 20t — the bite) then rises (over 10t). Lower jaw rises (translateY from -2.5→0 over 20t synchronized). Creates a clamping bite action. Bite damage at the moment of closure. Corridor rotates to face player at spawn.
**Sounds**: `ENTITY_RAVAGER_BITE` (1.0f, 0.5f) on bite | `ENTITY_ELDER_GUARDIAN_AMBIENT` jaw creaking
**Particles**: `DRIPPING_WATER` as saliva between teeth | `BLOCK_CRACK(BLACKSTONE)` during bite | `CRIMSON_SPORE` from gums
**Config**: radius=6, impact-only=false, damage=40, ticks-between=20, damageOnImpactOnly=true (bite peak), impactDamage=40, impactRadius=4, duration=300, cooldown=350

---

### 35. THE RIBCAGE PRISON — 42 blocks
**Visual**: A massive ribcage descends over the player — 10 rib pairs curving overhead, spine running down the center. Player is briefly imprisoned inside.
**Materials**: bone_block (ribs), polished_blackstone (marrow detail), netherrack (cartilage connectors), blackstone (sternum plate)
**Assembly**:
- Central spine: 8 bone_block scale(0.4f, 0.5f, 0.35f) stacked vertically (8 blocks)
- 10 rib pairs (20 ribs total): bone_block scale(0.18f, 0.15f, 1.4f) arcing outward — each rib uses Matrix4f rotateX to angle outward ±30°, staggered height. Left side rotated one way, right side mirror = 20 blocks
- Sternum: 3 blackstone scale(0.5f, 2.5f, 0.2f) vertical plate running front center (3 blocks)
- Cartilage connections: 6 netherrack scale(0.3f, 0.15f, 0.3f) at rib-sternum junctions (6 blocks)
- 5 floating rib fragment: bone_block scale(0.3f, 0.1f, 0.8f) drifting nearby (5 blocks)

**Animation**: Ribcage spawns at Y+8, descends over player via Marker teleport (Y-0.25/tick, teleportDuration=12). Once descended: ribs contract inward (scale Z reduces 1.4→0.8 over 30t — squeezing). Spine bows slightly. After 100t: cage rises back up. Damage applies during "squeeze" phase.
**Sounds**: `ENTITY_RAVAGER_ATTACK_BLOCKED` (bone grinding) | `ENTITY_IRON_GOLEM_HURT` creaking | `ENTITY_SKELETON_HURT` cracking
**Particles**: `BLOCK_CRACK(BONE_BLOCK)` during squeeze | `SOUL` rising through rib gaps | `SOUL_FIRE_FLAME` from spine tip
**Config**: radius=5, damage=35, ticks-between=15, delay=50 (sync with squeeze), duration=250, cooldown=380

---

### 36. THE BRAIN MASS — 38 blocks
**Visual**: A massive exposed brain sitting on a bone pedestal, wrinkled convolutions recognizable. Pulsating rhythmically. Tendrils reach downward.
**Materials**: nether_wart_block (brain folds — dark pinkish), red_nether_bricks (deeper fold shadows), bone_block (pedestal), netherrack (stem), blackstone (shadow depths), shroomlight (glow veins)
**Assembly**:
- Brain corpus callosum base: 4 nether_wart_block scale(1.5f, 0.5f, 2.0f) wide flat base (4 blocks)
- Left hemisphere: 6 nether_wart_block at various scale(1.0f, 0.8f, 1.0f)→scale(0.7f, 0.6f, 0.7f) forming dome (6 blocks)
- Right hemisphere: 6 nether_wart_block mirror of left (6 blocks)
- Convolution ridges: 8 red_nether_bricks scale(0.8f, 0.15f, 0.6f) flat plates at angles across brain surface (8 blocks)
- Pedestal: 4 bone_block scale(0.8f, 1.0f, 0.8f) stacked (4 blocks)
- Brain stem: 2 netherrack scale(0.4f, 0.6f, 0.4f) (2 blocks)
- Tendrils: 4 netherrack scale(0.15f, 0.8f, 0.15f) hanging below pedestal (4 blocks)
- Glow veins: 4 shroomlight scale(0.08f, 0.08f, 0.6f) very thin lines on surface (4 blocks)

**Animation**: The brain pulses — all brain blocks scale 1.0→1.12 in unison (heartbeat) every 20t. Convolution ridges heighten on pulse. Tendrils writhe (sine Y motion, 15t period, different phase). Glow veins brighten during pulse (scale 0.08→0.15). The pedestal twitches on each pulse.
**Sounds**: `ENTITY_WARDEN_HEARTBEAT` (0.7f, 0.5f) | `ENTITY_VEX_AMBIENT` low (0.3f, 0.2f) | `BLOCK_SLIME_BLOCK_STEP` squishy
**Particles**: `CRIMSON_SPORE` from brain surface | `DRIPPING_LAVA` from brain stem | `FALLING_NECTAR` dripping quality
**Config**: radius=6, damage=28, ticks-between=15, delay=5, duration=450, cooldown=300

---

### 37. THE OSSUARY THRONE — 45 blocks
**Visual**: A throne assembled entirely from skulls and long bones — exactly like a Khorne/necromantic throne from heavy metal art. Immediately recognizable.
**Materials**: bone_block (main structure), carved_pumpkin or wither_skeleton_skull (skull blocks used as skulls), polished_blackstone (dark inlay), nether_bricks (blood-stained mortaring), red_concrete (blood accents)
**Assembly**:
- Throne seat: 3 bone_block scale(1.6f, 0.3f, 1.4f) flat seat + 2 armrests scale(0.3f, 0.7f, 1.2f) (5 blocks)
- Throne back: 8 bone_block stacked in arch, scale tapering taller at center (8 blocks)
- Leg bones of throne base: 4 bone_block scale(0.3f, 1.5f, 0.3f) (4 blocks)
- Skull cluster decorations: 8 bone_block scale(0.6f, 0.7f, 0.6f) shaped as skulls at throne back and armrests (8 blocks)
- Skull eyes: 8 shroomlight scale(0.15f, 0.12f, 0.1f) glow in skull sockets × some (8 blocks)
- Red blood accents: 4 red_concrete scale(0.3f, 0.1f, 0.8f) flat plates as blood stains (4 blocks)
- Nether brick mortar: 6 nether_bricks scale(0.3f, 0.15f, 0.3f) at skull gaps (6 blocks)
- 4 long bone armrests decoration: bone_block scale(0.15f, 0.15f, 1.0f) elongated (4 blocks)
- 4 base scatter skulls on ground: bone_block scale(0.5f, 0.45f, 0.5f) (4 blocks)

**Animation**: Throne glows intensely. Skulls on throne periodically animate their mouth (jaw-open animation via scale). Throne itself slowly spins to face player. Eyes glow pulse. Blood drips (particle drips). One skull "turns" its head every 45t.
**Sounds**: `ENTITY_SKELETON_AMBIENT` × multiple | `ENTITY_WITHER_SHOOT` (0.3f, 0.3f) eerie  | `BLOCK_BONE_BLOCK_STEP` settling
**Particles**: `DRIPPING_LAVA` from throne (blood) | `SOUL_FIRE_FLAME` from skull eyes | `SCULK_SOUL` around base
**Config**: radius=8, damage=35, ticks-between=20, delay=20, duration=500, cooldown=350, tracks-player=true

---

### 38. THE WRITHING MASS — 42 blocks
**Visual**: A chaotic mass of body parts — arms, legs, torso sections — fused and writhing, pulsating like a living entity, approaching the player slowly.
**Materials**: netherrack (flesh), bone_block (protruding bones), red_concrete (blood/muscle), nether_wart_block (organs), blackstone (void spaces)
**Assembly**:
- Core mass: 8 netherrack scale(0.8f, 0.8f, 0.8f) clustered in rough sphere (8 blocks)
- Protruding limb segments: 10 netherrack scale(0.4f, 0.4f, 0.8f) reaching outward in random directions (10 blocks)
- Exposed bones: 8 bone_block scale(0.2f, 0.7f, 0.2f) jutting from mass (8 blocks)
- Organ bulges: 4 nether_wart_block scale(0.6f, 0.6f, 0.6f) (4 blocks)
- Red muscle strands: 6 red_concrete scale(0.1f, 0.1f, 0.8f) thin rods (6 blocks)
- Void space holes: 4 blackstone scale(0.3f, 0.3f, 0.3f) dark recesses (4 blocks)
- Scatter around mass: 2 netherrack (2 blocks)

**Animation**: Each block individually pulses on its own frequency (scale 0.8→1.0→0.8 or specific per-block oscillation, 10-25 tick periods varying). Mass slowly rolls toward player (Marker teleport, teleportDuration=20, every 10t). Limbs extend-retract randomly. Whole mass oscillates in size ±15%.
**Sounds**: `BLOCK_SLIME_BLOCK_STEP` squishy | `ENTITY_VEX_HURT` | `ENTITY_GHAST_WARN` (0.5f, 0.2f)
**Particles**: `CRIMSON_SPORE` from mass | `DRIPPING_LAVA` (blood) from hanging limbs | `BLOCK_CRACK(NETHERRACK)` splitting
**Config**: radius=7, damage=32, ticks-between=10, delay=5, duration=450, cooldown=300, tracks-player=true

---

### 39. THE REACHING HANDS FLOOR — 40 blocks
**Visual**: Multiple hands erupting directly from the ground below the player's feet, reaching upward and grasping. Different sizes, different orientations. Pure floor-threat horror.
**Materials**: bone_block (hands), netherrack (grasping flesh), blackstone (shadow between fingers), polished_blackstone (fingernails)
**Assembly**:
- 6 hand assemblies, each: 1 palm bone_block scale(0.7f, 0.3f, 0.7f) + 5 finger rods scale(0.13f, 0.6f, 0.13f) at angles = ~6 blocks × 6 = 36 blocks
- 2 wrist bone: netherrack scale(0.3f, 0.4f, 0.3f) × 2 = 2 blocks
- 2 nail accent: polished_blackstone scale(0.15f, 0.05f, 0.12f) × 2 = 2 blocks
- Total: ~40 blocks

**Animation**: All hands emerge from Y-2 to Y+0.5 over 15t (scale from 0.01→full). Then fingers grasp — each finger curls (Matrix4f rotateX -60° over 10t). Hands from different positions (spread 8 blocks around player). Each hand grabs in sequence (staggered 5-tick offsets). After 60t all retract back into ground.
**Sounds**: `BLOCK_GRAVEL_BREAK` erupting from ground | `ENTITY_VEX_HURT` grasping sounds | `BLOCK_BONE_BLOCK_BREAK` at peak
**Particles**: `BLOCK_CRACK(DIRT)` erupting from floor | `SCULK_SOUL` rising with hands | `SOUL_FIRE_FLAME` in palm centers
**Config**: radius=6, damage=35, ticks-between=15, delay=5, duration=200, cooldown=380

---

### 40. THE NIGHTMARE SPINE TREE — 36 blocks
**Visual**: A tree whose trunk is a giant spine and whose branches are long bones — grotesque but recognizable as both tree and skeleton. Vertebrae are clearly defined.
**Materials**: bone_block (vertebrae), blackstone (intervertebral space), polished_blackstone (branch ends), netherrack (root flesh), nether_bricks (bark texture)
**Assembly**:
- Main spine trunk: 8 bone_block scale(0.7f, 0.5f, 0.7f) + 7 blackstone scale(0.4f, 0.1f, 0.4f) intervertebral discs alternating = 15 blocks
- 6 branch bones: bone_block scale(0.2f, 0.15f, 1.5f) elongated radiating from spine at 3 heights × 2 sides (6 blocks)
- Branch tips: 6 polished_blackstone scale(0.3f, 0.3f, 0.3f) at branch ends (6 blocks)
- Root tentacles: 5 netherrack scale(0.25f, 0.15f, 0.8f) spreading from base (5 blocks)
- Bark texture: 4 nether_bricks scale(0.8f, 0.15f, 0.05f) flat plates around trunk (4 blocks)

**Animation**: Spine sways (entire trunk oscillates X/Z sinusoidally, 40-tick period, ±8°). Branch bones independently oscillate up/down (rotateZ ±15° each, 25-35 tick period per branch). Root tentacles writhe. Vertebrae compress/extend (Y-scale 0.5→0.7 per vertebra on wave). Entire structure grows from scale 0.01→full over 20t on spawn.
**Sounds**: `BLOCK_BONE_BLOCK_STEP` creaking | `ENTITY_SKELETON_HURT` cracking | `ENTITY_VEX_AMBIENT` (0.3f, 0.3f) wind through bones
**Particles**: `SOUL_FIRE_FLAME` at branch tips | `ASH` falling from branches | `SOUL` rising from roots
**Config**: radius=7, damage=28, ticks-between=20, delay=10, duration=450, cooldown=300

---

### 41. THE DISSECTION — 40 blocks
**Visual**: A humanoid form "dissected" and laid open — ribs spread open, organs visible inside. A horrific but anatomically recognizable cross-section display.
**Materials**: bone_block (ribs/skeleton), netherrack (organs/flesh), nether_wart_block (viscera), red_concrete (blood), blackstone (shadow of cavity), polished_blackstone (rib spreader tools)
**Assembly**:
- Prone body outline: 4 bone_block scale(0.7f, 0.15f, 1.8f) flat horizontal (4 blocks)
- Spread rib pairs (8 ribs): bone_block scale(0.15f, 0.1f, 1.2f) angled outward ±45° via Matrix4f each (8 blocks)
- Organ cluster: 4 netherrack scale(0.6f, 0.3f, 0.6f) in chest cavity (4 blocks)
- Viscera: 4 nether_wart_block scale(0.4f, 0.25f, 0.5f) (4 blocks)
- Blood pool: 6 red_concrete scale(0.7f, 0.02f, 0.7f) flat plates (6 blocks)
- 2 rib spreader tools: polished_blackstone scale(0.15f, 0.05f, 1.0f) thin metallic instruments (2 blocks)
- Cavity shadow: 4 blackstone scale(0.5f, 0.08f, 0.5f) dark flat inside cavity (4 blocks)
- 8 detail scatter (limbs, detail): various (8 blocks)

**Animation**: Ribs slowly spread wider (rotate outward via arm anchors, Matrix4f rotateX 45°→65° over 40t). Organs pulse. Blood pool expands (scale X/Z 0.7→1.2 over 80t). The whole form is on the ground (Y+0) and tilts slightly at spawn to face camera-visible angle. Blood drips.
**Sounds**: `BLOCK_SLIME_BLOCK_BREAK` (0.7f, 0.4f) | `ENTITY_VEX_HURT` | `BLOCK_BONE_BLOCK_STEP` (ribs grinding)
**Particles**: `DRIPPING_LAVA` (blood) from edges | `CRIMSON_SPORE` from organs | `DRIPPING_HONEY` viscera drip
**Config**: radius=7, damage=30, ticks-between=15, delay=10, duration=400, cooldown=300

---

### 42. THE LIVING SKULL SWARM — 36 blocks
**Visual**: 12 small skulls floating in formation, each independently animated — different orientations, jaws snapping at different times, orbiting a center point.
**Materials**: bone_block (skull bodies), polished_blackstone (lower jaws), shroomlight (eye glow), blackstone (shadow)
**Assembly** (per skull × 12, 3 blocks each = 36):
- Cranium: 1 bone_block scale(0.55f, 0.55f, 0.55f)
- Jaw: 1 polished_blackstone scale(0.5f, 0.2f, 0.5f) (positioned below cranium)
- Eye glow: 1 shroomlight scale(0.2f, 0.15f, 0.1f) inside eye socket
= 3 per skull × 12 = 36 blocks

**Animation**: All 12 skulls orbit the center at varying radii (3-6 blocks) on different orbital planes (some tilted 30°, some 60° from horizontal). Each skull independently snaps jaw every 15-35t (randomized). One skull per player periodically breaks orbit to dive at the player (teleportDuration=8, then returns). Skulls rotate on their Y-axis independently.
**Sounds**: `ENTITY_SKELETON_AMBIENT` chorus (multiple instances) | `ENTITY_SKELETON_HURT` on snap
**Particles**: `SOUL_FIRE_FLAME` from each skull's eyes | `SCULK_SOUL` at orbit trails | `BONE_MEAL` flash from snaps
**Config**: radius=5, damage=28, ticks-between=10, delay=5, duration=400, cooldown=300, tracks-player=true

---

### 43. THE HEART CHAMBER — 38 blocks
**Visual**: A massive anatomical heart — four chambers, aorta arch, ventricles. Unmistakably heart-shaped. Pulsates with each "beat."
**Materials**: red_concrete (main heart), nether_wart_block (darker muscle), nether_bricks (shadowed areas), polished_blackstone (aorta/vessels), crimson_planks (lighter heart areas), shroomlight (blood vessel glow)
**Assembly**:
- Left ventricle: 5 red_concrete scale(1.2f, 1.5f, 1.0f) large lower-left lobe (5 blocks)
- Right ventricle: 4 red_concrete scale(1.0f, 1.3f, 0.9f) lower-right (4 blocks)
- Left atrium: 3 nether_wart_block scale(0.9f, 0.8f, 0.8f) upper-left (3 blocks)
- Right atrium: 3 nether_wart_block scale(0.8f, 0.8f, 0.8f) upper-right (3 blocks)
- Aorta arch: 4 polished_blackstone scale(0.4f, 0.4f, 0.8f) curving up from top (4 blocks)
- Pulmonary vessels: 4 crimson_planks scale(0.3f, 0.5f, 0.15f) branching (4 blocks)
- Muscle detail: 8 nether_bricks scale(0.6f, 0.2f, 0.7f) flat plate striations (8 blocks)
- Blood vessel glow: 3 shroomlight scale(0.1f, 0.1f, 0.6f) (3 blocks)

**Animation**: Heartbeat — all cardiac blocks simultaneously scale ×1.15 over 5t then back over 5t (systole). Repeats every 20t (60 BPM). Aorta pulsates (scale Y 0.4→0.6) on beat. Vessels throb. After several beats, heart "fibrillates" — rapid irregular beats at 6t intervals for 30t then back to normal. Damage peaks during fibrillation.
**Sounds**: `ENTITY_WARDEN_HEARTBEAT` (1.0f, 0.7f) each beat | `ENTITY_ELDER_GUARDIAN_AMBIENT` on fibrillation | `BLOCK_SLIME_BLOCK_STEP`
**Particles**: `DRIPPING_LAVA` (blood) from aorta | `CRIMSON_SPORE` from ventricles | `SOUL_FIRE_FLAME` from vessels
**Config**: radius=7, damage=32, ticks-between=10 (fast ticks during fibrillation), delay=5, duration=400, cooldown=320

---

### 44. THE FACE IN THE WALL — 35 blocks
**Visual**: A stone wall emerges with a huge tormented face pressing through it from the other side — face recognizable (eyes, nose, mouth, forehead) but distorted by the wall.
**Materials**: deepslate_bricks (wall), bone_block (face features), blackstone (eye socket shadows), netherrack (open screaming mouth interior), polished_blackstone (protruding detail)
**Assembly**:
- Wall: 3 deepslate_bricks scale(4.0f, 3.0f, 0.2f) wide thin vertical wall (3 blocks)
- Forehead bulge: 2 bone_block scale(1.8f, 0.5f, 0.4f) pressing through wall surface (2 blocks)
- Brow ridges: 2 bone_block scale(0.8f, 0.3f, 0.3f) each (2 blocks)
- 2 eye sockets: 2 blackstone scale(0.6f, 0.5f, 0.3f) recesses (2 blocks)
- Nose bridge: 1 bone_block scale(0.5f, 0.6f, 0.35f) (1 block)
- Cheekbones: 2 bone_block scale(0.7f, 0.3f, 0.35f) (2 blocks)
- Screaming mouth: 3 netherrack scale(0.9f, 0.4f, 0.3f) gaping mouth cavity (3 blocks)
- Teeth: 6 polished_blackstone scale(0.2f, 0.35f, 0.2f) (6 blocks)
- 12 accent/texture blocks: deepslate_bricks at varying depths creating dimensional effect (12 blocks)

**Animation**: Face "presses through" — initially flat, then all face features extend forward (Z-translation 0→0.4 over 30t) over wall surface. Mouth opens during extension. Eyes "open" (shrink dark socket to reveal glow). Then recedes back. Repeating 80-tick cycle.
**Sounds**: `ENTITY_GHAST_SCREAM` muffled (0.5f, 0.4f) | `BLOCK_DEEPSLATE_BREAK` cracking | `ENTITY_VEX_AMBIENT` when receding
**Particles**: `BLOCK_CRACK(DEEPSLATE_BRICKS)` from wall around face | `SCULK_SOUL` from mouth | `REVERSE_PORTAL` from eye area
**Config**: radius=6, damage=30, ticks-between=20, delay=15, duration=350, cooldown=320

---

### 45. THE BONE HURRICANE — 40 blocks
**Visual**: A vortex/hurricane of bones — skull fragments, rib pieces, long bones — spinning rapidly in a visible tornado column.
**Materials**: bone_block (fragments), polished_blackstone (dark matter between), netherrack (flesh tags on bones), blackstone (dark core)
**Assembly**:
- Dark central core: 4 blackstone scale(0.3f, 1.5f, 0.3f) thin vertical column (4 blocks)
- Inner orbit bones (radius 1): 8 bone_block scale(0.4f, 0.4f, 0.3f) spinning at low height (8 blocks)
- Mid orbit (radius 2): 8 bone_block scale(0.5f, 0.3f, 0.4f) at mid height (8 blocks)
- Outer orbit (radius 3.5): 8 polished_blackstone scale(0.5f, 0.5f, 0.5f) at high position (8 blocks)
- Irregular orbit (radius 2.5): 6 netherrack scale(0.3f, 0.3f, 0.3f) (flesh tags) (6 blocks)
- Rib arc pieces: 4 bone_block scale(0.15f, 0.1f, 0.8f) elongated spinning (4 blocks)
- Base debris: 2 bone_block flat scale(0.6f, 0.05f, 0.6f) at floor level (2 blocks)

**Animation**: All orbiting bones are driven by per-tick teleport arc updates (each bone moves in a helical path — circular arc on X/Z with Y oscillation). Inner ring spins fastest (5-tick arc step), outer slowest (15-tick arc step). All bones rotate on their own axes via interpolation. Player inside the column radius takes rapid damage.
**Sounds**: `BLOCK_BONE_BLOCK_BREAK` (multiple, 0.4f, 0.8f) continuous | `ENTITY_VEX_HURT` | `ENTITY_SKELETON_HURT`
**Particles**: `SOUL_FIRE_FLAME` throughout vortex | `ASH` flying outward | `SCULK_SOUL` at column base
**Config**: radius=4 (inside vortex), damage=25, ticks-between=8, delay=5, duration=300, cooldown=350

---

## CATEGORY 4: NightmareWeaponry (NightmareWeaponry.java)
*Demonic weapons, sacrificial implements, nightmare siege engines*
*Theme: Obsidian black, netherite-gray, crimson, bone white, soul blue*

---

### 46. THE SOUL SCYTHE — 40 blocks
**Visual**: An enormous 7-block-tall scythe with a glowing soul-blue edge. Handle of dark twisted wood, blade made of compressed darkness with soul fire coursing through it.
**Materials**: blackstone (handle sections), obsidian (blade spine), polished_blackstone (blade edge), soul_sand (soul blue glow source), amethyst_block (magic energy), crying_obsidian (dark magic)
**Assembly**:
- Handle: 7 blackstone scale(0.18f, 0.9f, 0.18f) thin stacked rod (7 blocks)
- Crossguard: 2 blackstone scale(0.8f, 0.18f, 0.18f) horizontal (2 blocks)
- Blade spine: 5 obsidian scale(0.15f, 0.15f, 0.9f) forming curved spine via Matrix4f rotateZ per segment (5 blocks)
- Blade edge: 8 polished_blackstone_stairs facing various ways forming crescent (8 blocks)
- Blade surface fill: 4 crying_obsidian scale(0.7f, 0.08f, 0.5f) flat plates for blade surface (4 blocks)
- Soul edge glow: 6 soul_sand scale(0.08f, 0.07f, 0.7f) ultra-thin glowing blue lines on blade (6 blocks)
- Amethyst magic runes: 4 amethyst_block scale(0.2f, 0.2f, 0.05f) flat plate runes on blade (4 blocks)
- Pommel: 2 blackstone scale(0.45f, 0.45f, 0.45f) (2 blocks)
- Ground rune ring: 2 soul_sand scale(0.4f, 0.02f, 0.4f) (2 blocks)

**Animation**: Scythe sweeps in a 180° arc — the entire structure rotates on Y-axis from 0°→190° over 15t (the sweep), then resets over 5t. Repeats every 60t. Soul edge glows intensify (scale 0.08→0.18) during sweep. Blade surface shimmers scale during sweep. Soul particles follow blade arc during sweep.
**Sounds**: `ENTITY_PLAYER_ATTACK_SWEEP` (0.9f, 0.3f) | `ENTITY_VEX_DEATH` (0.5f, 0.5f) | `ENTITY_PHANTOM_FLAP` on sweep
**Particles**: `SOUL_FIRE_FLAME` along blade edge | `SOUL` trailing behind during sweep | `REVERSE_PORTAL` from runes
**Config**: radius=8, damage=42, ticks-between=15, delay=5 (leading edge), duration=480, cooldown=350

---

### 47. THE GUILLOTINE — 35 blocks
**Visual**: A full-height execution guillotine — 2 tall uprights with lunette at top, the weighted blade hovering above, a lunette/bascule at bottom. The blade falls.
**Materials**: dark_oak_planks (wooden frame), iron_block (blade), blackstone (weight on blade), polished_blackstone (metal hardware), netherrack (bloodstained base)
**Assembly**:
- Left upright: 6 dark_oak_planks scale(0.3f, 0.9f, 0.3f) (6 blocks)
- Right upright: 6 dark_oak_planks scale(0.3f, 0.9f, 0.3f) mirror (6 blocks)
- Top crossbeam: 2 dark_oak_planks scale(1.5f, 0.3f, 0.3f) (2 blocks)
- Blade: 3 iron_block scale(1.1f, 0.15f, 0.3f) + 1 polished_blackstone wedge at bottom = 4 blocks
- Counterweight: 2 blackstone scale(0.5f, 0.6f, 0.5f) on top of blade (2 blocks)
- Guide rails: 2 polished_blackstone scale(0.1f, 5.0f, 0.1f) thin vertical rail (2 blocks)
- Lunette holes and bascule: 2 dark_oak_trapdoor scale(0.7f, 0.05f, 0.7f) thin panels (2 blocks)
- Netherrack blood base: 3 netherrack scale(1.2f, 0.1f, 0.5f) (3 blocks)
- Hardware bolts: 6 polished_blackstone scale(0.15f, 0.15f, 0.15f) (6 blocks)

**Animation**: Blade starts at Y+5 (top position). Falls over 10t (Y-translation -5.0, teleportDuration=8). `damageOnImpactOnly=true` at the bottom. After 20t, blade rises back over 40t. Blade glints as it falls (scale flash). Sound of blade sliding down guides.
**Sounds**: `BLOCK_IRON_TRAPDOOR_OPEN` (swoosh) | `BLOCK_ANVIL_LAND` (1.0f, 0.8f) on impact | `ENTITY_IRON_GOLEM_ATTACK` on blade slam
**Particles**: `BLOCK_CRACK(IRON_BLOCK)` blade glint during fall | `DRIPPING_LAVA` blood at base | `CRIT` on impact
**Config**: radius=4, impact-only=true, impactDamage=50, impactRadius=4, duration=250, cooldown=400

---

### 48. THE DEMONIC BOW — 38 blocks
**Visual**: A massive war bow of bone and obsidian, 4 blocks tall, with a spectral arrow drawn back. The arrow releases at the player.
**Materials**: bone_block (bow limbs), obsidian (riser/handle), polished_blackstone (arrow shaft), crying_obsidian (arrowhead), amethyst_block (magical charge energy), chain (bowstring)
**Assembly**:
- Upper bow limb: 3 bone_block scale(0.2f, 0.9f, 0.2f) angled outward via Matrix4f rotateX (3 blocks)
- Lower bow limb: 3 bone_block mirror (3 blocks)
- Bow riser: 2 obsidian scale(0.5f, 0.8f, 0.4f) center handle (2 blocks)
- Bowstring: 4 chain scale(0.05f, 0.05f, 0.9f) angled thin lines connecting limbs to handle in V-shape (4 blocks)
- Arrow shaft: 5 polished_blackstone scale(0.1f, 0.1f, 1.5f) long rod (5 blocks)
- Arrowhead: 2 crying_obsidian scale(0.2f, 0.2f, 0.3f) double point (2 blocks)
- Arrow fletching: 3 polished_blackstone scale(0.4f, 0.5f, 0.05f) thin flat fins (3 blocks)
- Amethyst charge: 6 amethyst_block scale(0.15f, 0.15f, 0.15f) orbiting riser (6 blocks)
- Bow limb detail: 4 bone_block scale(0.15f, 0.15f, 0.15f) joint accents (4 blocks)
- 4 ground platform: blackstone scale(0.5f, 0.05f, 0.5f) (4 blocks)

**Animation**: Arrow starts pulled back (Z-translation from string center to -1.5 behind bow). After 40t: bow string draws (limbs flex via rotateX ±5°). After 60t: arrow releases — projectile fires at nearest player (teleportDuration=3 rapid movement). Then new arrow materializes (scale 0.01→full over 10t). Amethyst charges glow intensely before fire.
**Sounds**: `ENTITY_ARROW_SHOOT` | `ENTITY_CROSSBOW_SHOOT` (deep bow release) | `ENTITY_ARROW_HIT` on player proximity
**Particles**: `SOUL_FIRE_FLAME` along arrow in flight | `ENCHANT` from draw | `SONIC_BOOM` on release
**Config**: radius=4, impact-only=true, impactDamage=40, impactRadius=4, duration=300, cooldown=380

---

### 49. THE NIGHTMARE FLAIL — 38 blocks
**Visual**: A giant spiked morning star flail — a blackstone handle attached to chain to a massive spiked obsidian ball. The ball swings in wide circles.
**Materials**: obsidian (spiked ball core), bone_block (spikes), blackstone (chain segments + handle), polished_blackstone (handle metal guard), chain (actual chain blocks)
**Assembly**:
- Handle: 5 blackstone scale(0.25f, 0.9f, 0.25f) (5 blocks)
- Guard: 2 polished_blackstone scale(0.7f, 0.2f, 0.15f) flat crossguard (2 blocks)
- Chain: 6 chain scale(0.3f, 0.3f, 0.3f) in hanging curve from handle end (6 blocks)
- Flail ball core: 4 obsidian scale(0.7f, 0.7f, 0.7f) cluster (4 blocks)
- Spikes: 12 bone_block scale(0.18f, 0.18f, 0.6f) radiating from ball in all directions (12 blocks)
- Small secondary spikes: 6 polished_blackstone scale(0.1f, 0.1f, 0.4f) (6 blocks)
- Base anchor: 3 blackstone scale(0.4f, 0.5f, 0.4f) at ground (3 blocks)

**Animation**: The spiked ball swings in a horizontal circle around the handle (teleport arc update every 3t, teleportDuration=5, radius=4). Ball rotation speed increases and decreases (sinusoidal arc speed). Chain links update position to form catenary curve dynamically. Handle sways slightly. Ball spins on own axis via Matrix4f.
**Sounds**: `BLOCK_CHAIN_BREAK` (0.7f, 0.6f) chain rattling | `ENTITY_IRON_GOLEM_ATTACK` on max swing speed
**Particles**: `CRIT` trailing behind ball | `SOUL_FIRE_FLAME` from spike tips | `SMOKE` from chain
**Config**: radius=5, damage=40, ticks-between=8, delay=3, duration=400, cooldown=320

---

### 50. THE SACRIFICIAL ALTAR — 42 blocks
**Visual**: A dark stone altar table with carved demonic imagery, surrounded by ritual implements — bone knife embedded in it, candelabras at corners, offering bowls, rune carvings.
**Materials**: polished_blackstone (altar slab), nether_bricks (altar base), bone_block (ritual knife/bones), blackstone (candlestick), shroomlight (candle flame), amethyst_block (ritual gems), red_concrete (blood stains)
**Assembly**:
- Altar slab: 3 polished_blackstone scale(2.5f, 0.25f, 1.5f) + 1 scale(2.5f, 0.2f, 1.5f) = 4 blocks table
- Altar base legs: 4 nether_bricks scale(0.4f, 0.9f, 0.4f) (4 blocks)
- Altar side carvings: 4 polished_blackstone scale(2.2f, 0.7f, 0.1f) flat decorative panels (4 blocks)
- Ritual knife: 1 bone_block scale(0.12f, 0.8f, 0.12f) blade + 1 polished_blackstone scale(0.2f, 0.25f, 0.2f) handle (2 blocks)
- 4 candelabras: each = 1 blackstone scale(0.1f, 0.6f, 0.1f) stem + 1 shroomlight scale(0.25f, 0.3f, 0.25f) flame = 8 blocks
- Rune carvings: 4 amethyst_block scale(0.3f, 0.3f, 0.05f) flat plate runes (4 blocks)
- Blood stains: 4 red_concrete scale(0.6f, 0.02f, 0.6f) flat plates (4 blocks)
- Offering bowls: 3 polished_blackstone scale(0.4f, 0.2f, 0.4f) (3 blocks)
- Ground rune circle: 4 amethyst_block scale(0.3f, 0.02f, 0.3f) flat plates in ring (4 blocks)
- Flying debris: 5 polished_blackstone scale(0.25f, 0.25f, 0.25f) orbiting altar (5 blocks)

**Animation**: Flying debris orbits altar (teleport arc, teleportDuration=6). Candelabra flames pulsate (scale 0.25→0.4 on 15t). Rune carvings glow (amethyst scale 0.3→0.5 sequentially clockwise). Knife slowly rises off altar (Y-translation +0.5 over 60t) then slams back down (impact damage). Blood stains spread (scale grows).
**Sounds**: `BLOCK_RESPAWN_ANCHOR_CHARGE` | `ENTITY_VEX_AMBIENT` | `BLOCK_BELL_USE` (0.5f, 0.3f) ritual bell
**Particles**: `SOUL_FIRE_FLAME` from candles | `REVERSE_PORTAL` from runes | `DRIPPING_LAVA` blood | `ENCHANT` from gems
**Config**: radius=6, damage=32, ticks-between=20, delay=20, duration=500, cooldown=350

---

*[Attacks 51-105 continue in same format...]*

---

## CATEGORY 5: SurrealDreamObjects (SurrealDreamObjects.java)
*Dream-logic objects, Dali-esque surrealism, nightmare props*

### 51. THE MELTING CLOCK — 36 blocks
**Visual**: A massive pocket watch/clock whose face is clearly readable (12 marks) but whose body is sagging off an edge like a Salvador Dali painting.
**Materials**: polished_blackstone (clock case), iron_block (clock face), polished_blackstone (hour/minute hands), bone_block (dripping sag), blackstone_slab (sagging overhang), nether_bricks (clock numbers)
**Assembly**:
- Clock face disc: 8 iron_block scale(0.7f, 0.05f, 0.7f) flat plates arranged in circle, slightly overlapping (8 blocks)
- Clock face inner circle: 2 polished_blackstone scale(0.3f, 0.04f, 0.3f) (2 blocks)
- 12 number markers: nether_bricks scale(0.2f, 0.04f, 0.1f) flat at clock positions (12 blocks)
- Hour hand: 2 polished_blackstone scale(0.08f, 0.04f, 0.5f) rotated (2 blocks)
- Minute hand: 1 polished_blackstone scale(0.06f, 0.03f, 0.7f) (1 block)
- Clock rim: 4 polished_blackstone scale(0.7f, 0.2f, 0.1f) around face (4 blocks)
- Sagging body drooping off edge: 5 bone_block scale(0.4f, 0.08f, 0.4f) in melting cascade Y positions (5 blocks)
- Edge it rests on: 2 blackstone_slab (2 blocks)

**Animation**: Clock hands rotate (hour: full rotation over 200t, minute: full rotation over 50t). Sagging body drips lower every 30t (Y-translation -0.1 per drip tick, 5 drip segments). Clock face spins on its plane when struck (random sudden rotation). Time appears to go backward periodically (hands reverse for 20t).
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` tick-tock | `ENTITY_ENDERMAN_TELEPORT` (time warp) | `BLOCK_BELL_USE` on hour hand
**Particles**: `REVERSE_PORTAL` from clock face | `DRIPPING_OBSIDIAN_TEAR` from melting edges
**Config**: radius=6, damage=26, ticks-between=20, delay=20, duration=500, cooldown=350

---

### 52. THE NIGHTMARE CAROUSEL — 44 blocks
**Visual**: A classic carousel with nightmare horses — 4 horses (each made of bone and darkness), a central pillar, a top canopy, poles connecting horses to canopy. Spinning.
**Materials**: bone_block (horses), blackstone (poles), polished_blackstone (canopy rim), dark_oak_planks (canopy top), coal_block (horse shadow-flesh), shroomlight (horse eyes), crimson_planks (saddles)
**Assembly**:
- Central pillar: 5 polished_blackstone scale(0.25f, 0.9f, 0.25f) stacked (5 blocks)
- Canopy top: 4 dark_oak_planks scale(1.5f, 0.15f, 1.5f) in cross (4 blocks)
- Canopy rim: 4 polished_blackstone scale(1.8f, 0.12f, 0.15f) flat rim pieces (4 blocks)
- 4 poles: blackstone scale(0.08f, 0.08f, 1.8f) angled from canopy to horse (4 blocks)
- 4 nightmare horses (each = 5 blocks):
  - Horse body: 2 coal_block scale(0.7f, 0.6f, 0.9f) (2)
  - Horse head: 1 coal_block scale(0.5f, 0.55f, 0.4f) (1)
  - Horse legs: 1 bone_block scale(0.6f, 0.6f, 0.1f) flat plate (hooves group) (1)
  - Eye: 1 shroomlight scale(0.2f, 0.15f, 0.1f) (1)
  = 5 × 4 horses = 20 blocks
- 4 saddles: crimson_planks scale(0.5f, 0.12f, 0.35f) (4 blocks)
- Ground platform: 3 dark_oak_planks (3 blocks)

**Animation**: Entire carousel on Marker anchor rotating Y (80t per full revolution via Matrix4f). Horses bob up and down on poles (translateY ±0.6 each, 40t period, 90° phase offset between horses). Horses independently tilt their heads to face player. Canopy lanterns strobe. Carousel accelerates then slows like a real ride.
**Sounds**: `BLOCK_NOTE_BLOCK_CHIME` melody | `ENTITY_VEX_AMBIENT` (0.4f, 0.3f) screaming horses | `ENTITY_HORSE_AMBIENT` corrupted
**Particles**: `SOUL_FIRE_FLAME` from horse eyes | `REVERSE_PORTAL` from canopy | `ASH` swirling from rotation
**Config**: radius=7, damage=32, ticks-between=20, delay=10, duration=480, cooldown=320

---

### 53. THE DREAM HOURGLASS — 36 blocks
**Visual**: A massive hourglass with particles as the sand, clearly pinched in the middle, with ornate end caps. Particles actually flow from top chamber to bottom.
**Materials**: tinted_glass (glass chambers), polished_blackstone (end caps and frame), gold_block (ornate frame), amethyst_block (magical sand-glow), smooth_stone (base)
**Assembly**:
- Top glass dome: 5 tinted_glass scale(0.9f, 0.4f, 0.9f) forming dome shape (5 blocks)
- Bottom glass dome: 5 tinted_glass scale(0.9f, 0.4f, 0.9f) inverted (5 blocks)
- Pinch neck: 2 tinted_glass scale(0.2f, 0.3f, 0.2f) thin middle (2 blocks)
- Top endcap: 3 polished_blackstone scale(1.2f, 0.2f, 1.2f) + gold accent rings (3 blocks)
- Bottom endcap: 3 polished_blackstone (3 blocks)
- 4 frame pillars: gold_block scale(0.1f, 2.5f, 0.1f) tall thin rods at corners (4 blocks)
- Frame cross-braces: 4 polished_blackstone scale(0.08f, 0.08f, 1.1f) (4 blocks)
- Magical "sand" glow: 3 amethyst_block scale(0.3f, 0.3f, 0.3f) in bottom chamber glowing (3 blocks)
- Base: 4 smooth_stone scale(0.5f, 0.2f, 0.5f) (4 blocks)
- 3 ornate accent: gold_block scale(0.2f, 0.2f, 0.2f) at neck (3 blocks)

**Animation**: Hourglass slowly rotates Y. Periodically flips (rotates X 180° over 40t — the "flip"). After flip, top/bottom reverse. Sand particles flow through neck continuously. When flipped, the rush of "sand" triggers a particle burst. Time pressure damage increases as hourglass nears completion.
**Sounds**: `BLOCK_AMETHYST_BLOCK_STEP` sand-like sound | `ENTITY_ENDERMAN_TELEPORT` on flip | `BLOCK_BEACON_AMBIENT` hum
**Particles**: `FALLING_SPORE_BLOSSOM` falling through neck (as sand) | `PORTAL` swirling in chambers | `REVERSE_PORTAL` from end caps
**Config**: radius=6, damage=28, ticks-between=20, delay=10, duration=400, cooldown=300

---

### 54. THE IMPOSSIBLE VASE — 32 blocks
**Visual**: An ornate decorative vase — wide at the base, narrow neck, wide mouth — that clearly cannot exist because it twists non-Euclideanly: looking from one angle it's a vase, from another it's an empty room. Klein bottle inspired.
**Materials**: polished_blackstone (vase body), crying_obsidian (ornate swirl patterns), amethyst_block (gemstone accents), tinted_glass (impossible see-through sections), smooth_basalt (foot base)
**Assembly**:
- Vase foot: 2 smooth_basalt scale(1.2f, 0.2f, 1.2f) flat base plates (2 blocks)
- Vase lower body: 5 polished_blackstone scale(varying 1.0→1.3→1.0 = bulge) (5 blocks)
- Vase neck: 2 polished_blackstone scale(0.4f, 0.7f, 0.4f) thin neck (2 blocks)
- Vase mouth: 3 polished_blackstone scale(0.9f, 0.3f, 0.9f) flared mouth (3 blocks)
- Ornate swirl patterns: 8 crying_obsidian scale(0.05f, 0.5f, 0.05f) thin rod swirls on vase surface (8 blocks)
- Interior "impossibility" glass: 4 tinted_glass scale(0.8f, 0.8f, 0.02f) ultra-thin inside vase where you shouldn't see (4 blocks)
- Gemstone accents: 4 amethyst_block scale(0.2f, 0.2f, 0.05f) (4 blocks)
- Particle of 4 floaty elements around it (4 blocks misc)

**Animation**: Vase slowly rotates Y revealing its impossible geometry. Interior glass panels rotate at a different speed than exterior, creating visual dissonance. Swirl patterns "flow" up the surface via Y-translation updates. Mouth periodically "drinks" — particles flow downward into it.
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` | `BLOCK_GLASS_PLACE` (0.4f, 1.2f)
**Particles**: `REVERSE_PORTAL` pouring into mouth | `ENCHANT` from gem accents | `SCULK_SOUL` from base
**Config**: radius=5, damage=24, ticks-between=20, delay=15, duration=400, cooldown=280

---

### 55. THE FLYING COFFIN — 38 blocks
**Visual**: A fully detailed gothic coffin (hexagonal shape, wood grain, silver handles, interior silk lining visible when open) flying through the air and diving at the player.
**Materials**: dark_oak_planks (coffin wood), polished_blackstone (metal handles), iron_block (metal hardware), white_concrete (interior lining), blackstone (shadow details), bone_block (skeletal contents)
**Assembly**:
- Coffin body: 6 dark_oak_planks forming hexagonal cross-section (6 blocks)
- Coffin lid: 3 dark_oak_planks scale(1.1f, 0.1f, 0.6f) flat with hexagonal taper (3 blocks)
- Interior lining: 3 white_concrete scale(0.9f, 0.08f, 0.5f) inside lid and bottom (3 blocks)
- 4 silver handles: polished_blackstone scale(0.35f, 0.15f, 0.08f) on sides (4 blocks)
- Hardware bolts: 4 iron_block scale(0.1f, 0.1f, 0.1f) (4 blocks)
- Cross symbol: 2 polished_blackstone scale(0.5f, 0.1f, 0.08f) cross on lid (2 blocks)
- Interior skeleton outline: 6 bone_block scale(0.25f, 0.15f, 0.3f) inside visible through lid gap (6 blocks)
- Dark shadow detail: 4 blackstone scale(0.2f, 0.2f, 0.1f) (4 blocks)
- Floating surrounding (6 blocks): dark debris

**Animation**: Coffin spawns at Y+6 tumbling (Matrix4f rotates on all axes slowly). Lid opens (rotateX on lid anchor 0°→-100°) during dive. Coffin dives at player (Marker teleportDuration=6 tracking player). Impact triggers `damageOnImpactOnly`. After impact, coffin wobbles and rises back up. Lid snaps shut.
**Sounds**: `BLOCK_IRON_DOOR_OPEN` (creaking lid) | `ENTITY_ELDER_GUARDIAN_HURT` (wood impact) | `ENTITY_SKELETON_HURT` on hit
**Particles**: `DRIPPING_OBSIDIAN_TEAR` from coffin | `SOUL_FIRE_FLAME` seeping from joints | `SOUL` trailing lid
**Config**: radius=5, impact-only=true, impactDamage=45, impactRadius=5, duration=200, cooldown=380

---

### 56. THE REALITY TEAR — 33 blocks
**Visual**: A geometric fracture ripped in space — jagged edges of "torn reality" like broken glass, with the void visible through the center.
**Materials**: obsidian (tear edges), crying_obsidian (glowing tear edge), tinted_glass (void interior), polished_blackstone (outer frame), amethyst_block (energy release)
**Assembly**:
- Void interior panels: 6 tinted_glass scale(0.9f, 0.9f, 0.02f) ultra-thin layers overlapping at slight offsets (6 blocks)
- Tear edge shards: 10 obsidian scale(varying 0.2-0.5f, 0.7-1.2f, 0.15f) jagged pointed pieces at angles around void (10 blocks)
- Glowing edge crying: 8 crying_obsidian scale(0.1f, 0.5f, 0.1f) thin rods at edge jagged points (8 blocks)
- Amethyst energy discharge: 6 amethyst_block scale(0.2f, 0.2f, 0.15f) floating near edges (6 blocks)
- Outer blackstone frame ghost: 3 polished_blackstone scale(0.4f, 0.4f, 0.05f) faint rectangular frame (3 blocks)

**Animation**: Tear edge shards jitter (rapid small teleport oscillations, ±0.05 amplitude every 2t). Void interior panels scale Z oscillation (0.02→0.05) creating depth shimmer. Amethyst energy discharges shoot outward occasionally (teleport to radius 3 then return). The whole tear slowly rotates on Y.
**Sounds**: `BLOCK_END_PORTAL_FRAME_FILL` | `ENTITY_ENDERMAN_TELEPORT` | `BLOCK_GLASS_BREAK` crackling
**Particles**: `PORTAL` flooding through tear | `REVERSE_PORTAL` on edges | `SONIC_BOOM` shockwave from tear
**Config**: radius=6, damage=32, ticks-between=15, delay=5, duration=400, cooldown=300

---

### 57. THE PUZZLE BOX — 34 blocks
**Visual**: The Hellraiser/Lemarchand puzzle box — a cube with intricate geometric sliding panels. Each face shows different demonic symbols. Panels slide and rotate as it "opens."
**Materials**: polished_blackstone (panel faces), gold_block (golden inlay lines), amethyst_block (gems), crying_obsidian (dark panel borders), blackstone (base cube body), nether_bricks (symbol carving)
**Assembly**:
- Base cube: 6 blackstone scale(0.6f, 0.6f, 0.6f) inner cube (6 blocks)
- 6 face panels: polished_blackstone scale(0.8f, 0.05f, 0.8f) thin flat plates on each face (6 blocks)
- Golden inlay lines: 8 gold_block scale(0.7f, 0.02f, 0.05f) thin lines on faces (8 blocks)
- Symbol carvings: 4 nether_bricks scale(0.3f, 0.02f, 0.2f) on face panels (4 blocks)
- Gem accents: 4 amethyst_block scale(0.15f, 0.15f, 0.15f) at corners (4 blocks)
- Crying obsidian border: 4 crying_obsidian scale(0.6f, 0.03f, 0.1f) (4 blocks)
- Sliding panel pieces (offset panels): 2 polished_blackstone scale(0.4f, 0.06f, 0.8f) partially slid (2 blocks)

**Animation**: Box floats and slowly rotates on all 3 axes at different speeds (X=100t, Y=80t, Z=120t cycles). Periodically "opens" — individual face panels slide outward (translateZ +0.3 via Matrix4f over 15t) revealing inner darkness. Opening triggers an aura and damage pulse. Box can become "fully open" — all panels displaced outward simultaneously.
**Sounds**: `BLOCK_RESPAWN_ANCHOR_CHARGE` (box activating) | `BLOCK_GOLD_STEP` (sliding panels) | `ENTITY_VEX_HURT` on open
**Particles**: `SOUL_FIRE_FLAME` from gaps when opening | `REVERSE_PORTAL` from panels | `ENCHANT` from gems
**Config**: radius=5, damage=38, ticks-between=20, delay=40 (after open), duration=400, cooldown=350

---

### 58. THE NIGHTMARE TELEPHONE — 31 blocks
**Visual**: A giant old rotary telephone (1950s style) floating and ringing, receiver dangling. It's the wrong size (2 blocks tall) and clearly wrong place (floating mid-air, ringing in nightmare).
**Materials**: coal_block (phone body), polished_blackstone (phone body detail), blackstone (dial), bone_block (receiver/handset), iron_block (phone cord), nether_bricks (dial numbers)
**Assembly**:
- Phone body: 4 coal_block scale(1.0f, 0.4f, 0.7f) forming main phone housing (4 blocks)
- Body detail: 3 polished_blackstone scale(0.9f, 0.35f, 0.65f) overlapping (3 blocks)
- Rotary dial: 2 blackstone scale(0.5f, 0.05f, 0.5f) flat circle + 1 nether_bricks scale(0.4f, 0.04f, 0.4f) = 3 blocks
- Dial finger holes: 5 nether_bricks scale(0.08f, 0.06f, 0.08f) small dots in ring (5 blocks)
- Receiver: 3 bone_block — 2 earpiece/mouthpiece scale(0.3f, 0.2f, 0.35f) + 1 handle scale(0.12f, 0.12f, 0.8f) (3 blocks)
- Curly cord: 5 iron_block scale(0.06f, 0.06f, 0.4f) in spiral/zigzag (5 blocks)
- Bells on top: 3 polished_blackstone scale(0.25f, 0.2f, 0.25f) (3 blocks)
- Base feet: 4 coal_block scale(0.2f, 0.15f, 0.2f) (4 blocks)
- Floating position: 2 atmospheric accent (2 blocks)

**Animation**: Phone floats and bobs Y ±0.3. When ringing: phone shakes rapidly (teleport jitter ±0.1 every 2t, 20t duration every 60t). Receiver dangles and swings. Dial rotates. Bells vibrate.
**Sounds**: `BLOCK_NOTE_BLOCK_BELL` ringing rhythm | `ENTITY_VEX_AMBIENT` (0.3f, 0.5f) wrong answer
**Particles**: `CRIT` from ringing bells | `REVERSE_PORTAL` from dial
**Config**: radius=4, damage=22, ticks-between=20, delay=5, duration=350, cooldown=280

---

### 59. THE DOORWAY TO PERDITION — 40 blocks
**Visual**: An enormous ornate door frame — wider than a regular door, columns, lintel, decorative pediment — standing alone with a blazing hellfire portal inside it.
**Materials**: polished_blackstone (columns), blackstone_brick_stairs (ornate arch), gold_block (golden inlay on columns), nether_bricks (portal frame border), tinted_glass (portal surface), shroomlight (glowing fire inside)
**Assembly**:
- Left column: 7 polished_blackstone scale(0.45f, 0.9f, 0.45f) (7 blocks)
- Right column: 7 polished_blackstone mirror (7 blocks)
- Column capital (top): 2 polished_blackstone scale(0.7f, 0.35f, 0.7f) wider base at top (2 blocks each = 4)
- Lintel: 2 polished_blackstone scale(2.2f, 0.4f, 0.4f) (2 blocks)
- Pediment (triangle top): 4 blackstone_brick_stairs in triangular arrangement (4 blocks)
- Portal fill: 3 tinted_glass scale(1.8f, 2.5f, 0.02f) ultra-thin overlapping portal surface (3 blocks)
- Shroomlight fire: 4 shroomlight scale(0.5f, 0.5f, 0.5f) inside portal at different Y levels (4 blocks)
- Gold inlay: 4 gold_block scale(0.05f, 2.5f, 0.05f) thin lines on columns (4 blocks)
- Base steps: 5 polished_blackstone scale(0.7f, 0.15f, 1.0f) (5 blocks)
- Rune floor circle: 5 nether_bricks flat plates on ground (5 blocks)

**Animation**: Portal surface shimmers (Z-scale 0.02→0.06 oscillating). Shroomlight fires inside flicker scale. Gold lines on columns pulse. Pediment glows then dims. Door periodically "opens" — the portal surface brightens and any player near the opening takes burst damage.
**Sounds**: `BLOCK_END_PORTAL_SPAWN` | `ENTITY_BLAZE_SHOOT` from portal | `ENTITY_ENDER_DRAGON_GROWL` (0.4f, 0.3f)
**Particles**: `FLAME` and `LAVA` from inside portal | `PORTAL` at frame edges | `REVERSE_PORTAL` flowing through
**Config**: radius=7, damage=36, ticks-between=20, delay=10, duration=480, cooldown=340

---

### 60. THE INVERTED PYRAMID — 34 blocks
**Visual**: A massive pyramid hanging upside down, apex pointing at the ground, slowly spinning. The anti-gravity pyramid defies all logic and looms menacingly.
**Materials**: polished_blackstone (pyramid faces), gold_block (edge lines), obsidian (apex), crying_obsidian (glow edges), nether_bricks (base/top platform)
**Assembly**:
- 4 pyramid faces (inverted): polished_blackstone scale(2.0f, 0.05f, 1.5f) flat plates tilted inward by Matrix4f rotateX ±35° (4 blocks = 1 per face, but each face has 3 of different scales for stepping = 12 blocks)
- Edge lines: 8 gold_block scale(0.08f, 0.08f, 1.5f) beams along all 8 pyramid edges (8 blocks)
- Apex: 3 obsidian scale(0.25f, 0.25f, 0.4f) at bottom point (3 blocks)
- Glow apex tip: 2 crying_obsidian scale(0.15f, 0.3f, 0.15f) (2 blocks)
- Top "base" (actually hanging below the flat part): 3 nether_bricks scale(0.8f, 0.15f, 0.8f) inverted base caps (3 blocks)
- 6 floating accent fragments: crying_obsidian scale(0.2f, 0.2f, 0.2f) orbiting pyramid (6 blocks)

**Animation**: Pyramid spins on Y continuously (120t half-revolution). Slowly descends toward player (Y-0.05/tick, teleportDuration=15). Apex points directly at player (Marker rotates to aim apex at nearest player). When apex gets within 3 blocks of player: damage increases. Orbit fragments spin opposite to main pyramid.
**Sounds**: `ENTITY_ELDER_GUARDIAN_AMBIENT` | `BLOCK_END_PORTAL_FRAME_FILL` humming
**Particles**: `REVERSE_PORTAL` from apex | `PORTAL` swirling around pyramid | `SONIC_BOOM` from apex on proximity
**Config**: radius=7, damage=34, ticks-between=15, delay=20, duration=400, cooldown=320

---

## CATEGORY 6: InfernalMachinery (InfernalMachinery.java)
*Hellish contraptions, torture devices, mechanical nightmare engines*
*Theme: Smooth stone gray, polished blackstone, iron, chain, coal, fire elements*

---

### 61. THE HELLISH GEAR ASSEMBLY — 45 blocks
**Visual**: Three interlocking gears of different sizes — a large main gear, medium secondary gear, small tertiary gear — all visibly connected and counter-rotating as they should mechanically.
**Materials**: polished_blackstone (gear teeth), smooth_stone (gear bodies), chain (connecting elements), iron_block (axles), blackstone_brick_stairs (tooth shapes), polished_deepslate (gear faces)
**Assembly**:
- Large gear face: 8 smooth_stone in ring scale(0.6f, 0.15f, 0.6f) (8 blocks)
- Large gear teeth: 12 blackstone_brick_stairs scale(0.5f, 0.2f, 0.4f) around perimeter (12 blocks)
- Large gear axle: 1 iron_block scale(0.3f, 0.3f, 0.3f) (1 block)
- Medium gear: 6 smooth_stone in smaller ring + 8 teeth = 14 blocks
- Small gear: 4 polished_deepslate + 6 small teeth = 10 blocks
- 3 axle posts: iron_block scale(0.15f, 2.5f, 0.15f) vertical (3 blocks)
- 3 chain links connecting: chain scale(0.4f, 0.4f, 0.4f) between gears (3 blocks)
- Base platform: 6 smooth_stone scale(0.5f, 0.15f, 0.5f) (6 blocks — less than listed)
- Mechanical detail: varies to reach 45

**Animation**: Large gear rotates on Z-axis (motor speed, 60t per revolution). Medium gear counter-rotates at 1.5× speed. Small gear counter-rotates at 3× speed. Mechanical interlocking is implied by timing. Occasionally a gear "catches" — all gears stop for 5t then SLAM back to motion (damage spike). Chain links update to span between gear edges.
**Sounds**: `BLOCK_CHAIN_WALK` continuous | `BLOCK_PISTON_EXTEND` on gear-catch | `BLOCK_IRON_DOOR_OPEN` metal grinding
**Particles**: `CRIT` (metal sparks) from gear teeth | `SMOKE` from axles | `CAMPFIRE_COSY_SMOKE` from friction
**Config**: radius=6, damage=30, ticks-between=15, delay=10, duration=480, cooldown=320

---

### 62. THE SOUL FURNACE — 42 blocks
**Visual**: An industrial blast furnace of demonic design — intake chimney, main chamber, exhaust stacks, soul glass viewing windows. Clearly a furnace, clearly infernal.
**Materials**: smooth_stone (furnace walls), nether_bricks (infernal details), iron_bars (intake grate), blackstone_slab (chimney), polished_blackstone (chimney stacks), tinted_glass (window panels), shroomlight (internal fire glow)
**Assembly**:
- Main chamber walls: 4 smooth_stone scale(1.0f, 1.5f, 0.2f) × 4 sides = 4 blocks
- Chimney body: 6 smooth_stone scale(0.5f, 1.0f, 0.5f) stacked ascending (6 blocks)
- 2 exhaust stacks: polished_blackstone scale(0.3f, 2.0f, 0.3f) twin chimneys (2 blocks each = 4)
- Intake grate: 3 iron_bars scale(0.9f, 0.05f, 0.9f) flat grate in arc (3 blocks)
- Nether brick details: 6 nether_bricks scale(0.5f, 0.5f, 0.2f) on walls (6 blocks)
- 3 viewing windows: tinted_glass scale(0.5f, 0.5f, 0.05f) on front wall (3 blocks)
- Internal glow: 4 shroomlight scale(0.3f, 0.3f, 0.3f) inside chamber glowing (4 blocks)
- Base foundation: 4 smooth_stone scale(0.5f, 0.2f, 0.5f) (4 blocks)
- Metal hardware: 4 polished_blackstone scale(0.2f, 0.2f, 0.15f) (4 blocks)
- Chimney cap: 2 blackstone_slab (2 blocks)
- 2 pressure gauges: polished_blackstone scale(0.3f, 0.3f, 0.1f) (2 blocks)
- Overpressure burst: 4 iron_block scale(0.2f, 0.2f, 0.2f) debris orbit (4 blocks)

**Animation**: Internal glow pulses (shroomlight scale 0.3→0.5). Exhaust stacks emit smoke via particle. Pressure gauges "spin" (rotate). Periodically the furnace "overloads" — all blocks pulse bright, then EXPLOSION particle burst from top + damage spike. Intake grate glows then fades.
**Sounds**: `BLOCK_FURNACE_FIRE_CRACKLE` continuous | `ENTITY_BLAZE_SHOOT` on overload | `BLOCK_IRON_DOOR_OPEN`
**Particles**: `LARGE_SMOKE` from chimney stacks | `LAVA` from viewing windows | `FLAME` ambient | `SMOKE` intake
**Config**: radius=6, damage=28, ticks-between=15, delay=5, duration=500, cooldown=300

---

### 63. THE TORTURE WHEEL — 36 blocks
**Visual**: A medieval breaking wheel on a frame — a massive spoked wooden wheel mounted on a post with structural cross-bracing. Spins. Spikes on the rim.
**Materials**: dark_oak_planks (wheel and frame), polished_blackstone (metal rims and spike bases), bone_block (spikes/spines on rim), blackstone (post/axle), iron_block (metal hardware)
**Assembly**:
- Wheel center: 2 blackstone scale(0.3f, 0.3f, 0.3f) axle center (2 blocks)
- Wheel hub ring: 6 polished_blackstone in ring scale(0.3f, 0.15f, 0.3f) (6 blocks)
- 8 spokes: dark_oak_planks scale(0.12f, 0.12f, 0.85f) radiating from center (8 blocks)
- Outer rim: 8 dark_oak_planks scale(0.4f, 0.15f, 0.4f) curved positions at rim (8 blocks)
- 8 rim spikes: bone_block scale(0.15f, 0.15f, 0.4f) pointing outward from rim (8 blocks)
- Post: 3 blackstone scale(0.3f, 1.5f, 0.3f) vertical support (3 blocks)
- A-frame braces: 2 dark_oak_planks scale(0.2f, 0.15f, 1.2f) angled cross bracing (2 blocks)
- Iron hardware: 4 iron_block scale(0.2f, 0.2f, 0.15f) at axle and bracing (4 blocks)
- Base: 3 dark_oak_planks scale(0.5f, 0.2f, 0.5f) footer (3 blocks)
- Extra detail: 2 more (2 blocks)

**Animation**: Wheel spins on its plane via Z-axis rotation (continuous, Matrix4f 180°+0.1f every 40t). Spikes flash as they pass through player zone. Post sways slightly. Wheel accelerates then reverses direction occasionally. Spikes glow during reverse.
**Sounds**: `BLOCK_IRON_DOOR_OPEN` creaking | `BLOCK_CHAIN_BREAK` (0.7f, 0.6f) | `ENTITY_IRON_GOLEM_ATTACK` on spike pass
**Particles**: `CRIT` from spikes | `SMOKE` from axle | `SOUL_FIRE_FLAME` in wheel center
**Config**: radius=4, damage=35, ticks-between=8, delay=5, duration=400, cooldown=320

---

### 64. THE NIGHTMARE PIPE ORGAN — 48 blocks
**Visual**: A massive pipe organ — clearly recognizable with tall pipes of varying heights, console, keyboard, pedals. The pipes emit flames when "playing."
**Materials**: polished_blackstone (pipe bodies), smooth_stone (console), dark_oak_planks (wooden cabinet), gold_block (golden trim pipes), iron_block (metal fittings), netherrack (flame color at pipe tops)
**Assembly**:
- Console cabinet: 4 dark_oak_planks scale(1.8f, 0.8f, 0.5f) box shape (4 blocks)
- Console keyboard: 1 smooth_stone scale(1.5f, 0.1f, 0.4f) flat (1 block)
- 12 large center pipes: polished_blackstone scale(0.25f, varying_heights 1.0-3.5, 0.25f) in ascending arch (12 blocks)
- 8 medium side pipes: smooth_stone scale(0.2f, 0.8-1.8, 0.2f) flanking (8 blocks)
- 4 gold accent pipes: gold_block scale(0.2f, 2.5f, 0.2f) tall decorative (4 blocks)
- 6 pipe top flares: netherrack scale(0.35f, 0.2f, 0.35f) at top of largest pipes (6 blocks)
- Console pedal board: 2 dark_oak_planks scale(1.2f, 0.1f, 0.5f) (2 blocks)
- 6 front face panel: polished_blackstone scale(0.5f, 1.5f, 0.05f) decorative panels (6 blocks)
- Metal fittings: 5 iron_block scale(0.2f, 0.2f, 0.15f) (5 blocks)

**Animation**: Organ "plays" — pipes activate in sequence (netherrack flame tops scale 0.2→0.5 sequentially with 3t per pipe offset). This creates a visible playing effect. The gold pipes glow with each note. Sound plays matching the visual. The console vibrates slightly during play.
**Sounds**: `BLOCK_NOTE_BLOCK_BASS` + `BLOCK_NOTE_BLOCK_BASEDRUM` + `BLOCK_NOTE_BLOCK_SNARE` in sequence creating organ melody | `ENTITY_VEX_AMBIENT`
**Particles**: `SOUL_FIRE_FLAME` from pipe tops | `SMOKE` from console | `REVERSE_PORTAL` from pedal board
**Config**: radius=7, damage=30, ticks-between=15, delay=10, duration=500, cooldown=340

---

### 65. THE BLOOD PUMP — 36 blocks
**Visual**: An anatomical heart reimagined as steampunk machinery — mechanical valves, iron tubes as vessels, a piston pump chamber, gauge dials. Pumping rhythmically.
**Materials**: iron_block (pump chamber), polished_blackstone (tube vessels), red_concrete (blood liquid color), netherrack (organic connecting tissue), chain (drive chains), polished_deepslate (gauge faces)
**Assembly**:
- Main pump chamber: 4 iron_block scale(0.8f, 0.8f, 0.8f) box core (4 blocks)
- Piston rods: 4 polished_blackstone scale(0.2f, 0.2f, 0.8f) extending from chamber (4 blocks)
- Output tubes: 4 red_concrete scale(0.25f, 0.25f, 1.2f) angled outward (4 blocks)
- Input tubes: 2 red_concrete scale(0.25f, 1.0f, 0.25f) vertical intake (2 blocks)
- Connecting tissue: 4 netherrack scale(0.3f, 0.3f, 0.3f) organic connectors (4 blocks)
- Drive chains: 4 chain scale(0.3f, 0.3f, 0.3f) around gears (4 blocks)
- 4 gauge dials: polished_deepslate scale(0.3f, 0.3f, 0.05f) flat (4 blocks)
- Pressure valve handles: 4 iron_block scale(0.1f, 0.1f, 0.4f) (4 blocks)
- 6 rivets/bolts: polished_blackstone scale(0.1f, 0.1f, 0.1f) (6 blocks)

**Animation**: Piston rods pump (translateZ 0.8→0.4 over 10t, then back 10t = heartbeat cycle 20t). On pump: red tube pulses bright. Pressure gauges "swing" (rotate Z on gauge needle). Chains jitter. Drive mechanism spins. Overpressure event every 100t: all pistons fire simultaneously, damage burst.
**Sounds**: `ENTITY_WARDEN_HEARTBEAT` (0.8f, 0.6f) per pump | `BLOCK_PISTON_EXTEND` on max compression | `BLOCK_IRON_DOOR_OPEN`
**Particles**: `DRIPPING_LAVA` (blood) from tube ends | `SMOKE` from pressure valves | `CRIT` from piston impacts
**Config**: radius=5, damage=28, ticks-between=10 (heartbeat timing), delay=5, duration=450, cooldown=300

---

### 66. THE BLADE ARRAY — 40 blocks
**Visual**: 8 circular spinning saw-blade shapes suspended in a grid pattern, each rotating on its own plane. Clearly deadly, clearly mechanical.
**Materials**: polished_blackstone (blade bodies), iron_block (blade edges — sharp reflection), obsidian (dark blade sections), polished_deepslate (blade hub centers), blackstone_stairs (blade tooth shapes)
**Assembly** (per blade × 8, ~5 blocks = 40):
- Blade body ring: 2 polished_blackstone in ring scale(0.5f, 0.05f, 0.5f) (2 blocks)
- Blade teeth: 1 blackstone_stairs scale(0.4f, 0.15f, 0.3f) tooth representation (1 block, or 2 for larger blades)
- Hub: 1 polished_deepslate scale(0.2f, 0.2f, 0.2f) (1 block)
- Edge accent: 1 iron_block scale(0.4f, 0.04f, 0.4f) outer ring (1 block)
= 5 per blade × 8 = 40 blocks

**Animation**: Each blade spins on Z-axis at own speed (ranging 15-60t per revolution). Blades are arranged in 2-block-spaced grid (4×2 arrangement). Periodically they all advance toward the player (teleportDuration=20, Z-movement) then retreat. Blades occasionally tilt (rotateX 0°→15°→0°) for variety. Player in blade path takes rapid damage.
**Sounds**: `BLOCK_IRON_TRAPDOOR_OPEN` blade whoosh (loop) | `ENTITY_IRON_GOLEM_ATTACK` on advance | `BLOCK_IRON_DOOR_CLOSE`
**Particles**: `CRIT` (sparks) trailing each blade | `SMOKE` from hubs | `CAMPFIRE_COSY_SMOKE` overheating
**Config**: radius=5, damage=32, ticks-between=8, delay=5, duration=400, cooldown=320

---

### 67. THE INFERNAL CANNON — 42 blocks
**Visual**: A massive demonic siege cannon — ornate barrel, carriage with wheels, breech, muzzle with demonic face carving. Fires burning projectiles.
**Materials**: polished_blackstone (barrel), dark_oak_planks (cannon carriage), iron_block (metal banding on barrel), blackstone (wheel bodies), nether_bricks (demonic face carving), netherrack (muzzle fire), magma_block (shot core)
**Assembly**:
- Barrel: 6 polished_blackstone scale(0.7f, 0.7f, 0.9f) in row forming cylinder (6 blocks)
- Barrel end (muzzle): 1 netherrack scale(0.8f, 0.8f, 0.3f) flared muzzle (1 block)
- Barrel reinforcing bands: 4 iron_block scale(0.75f, 0.75f, 0.15f) rings around barrel (4 blocks)
- Cannon carriage: 4 dark_oak_planks scale(0.9f, 0.4f, 1.8f) (4 blocks)
- 2 carriage wheels: each 8 polished_blackstone in ring scale(0.3f, 0.15f, 0.3f) = 16 total... simplified: 4 per wheel = 8 blocks
- Wheel spokes: 4 dark_oak_planks scale(0.1f, 0.1f, 0.5f) per wheel × 2 = 8 blocks (or simplify to 4 total)
- Breech knob: 2 iron_block scale(0.3f, 0.3f, 0.3f) (2 blocks)
- Demonic face: 3 nether_bricks scale(varying) on barrel (3 blocks)
- Shot (cannonball): 3 magma_block scale(0.6f, 0.6f, 0.6f) cluster (3 blocks)
- Ground impact scatter: 6 polished_blackstone scale(0.3f, 0.1f, 0.3f) (6 blocks)

**Animation**: Cannon aims toward player (Marker rotates, teleportDuration=15). Fires sequence: charge glow, then shot fires (magma ball teleports rapidly toward player, teleportDuration=4). Impact: `damageOnImpactOnly`. Cannon recoils backward (translateZ -0.5 over 5t then forward). Fires every 80t.
**Sounds**: `ENTITY_GENERIC_EXPLODE` (cannon fire) | `BLOCK_FIRE_AMBIENT` charging | `ENTITY_BLAZE_SHOOT` shot in flight
**Particles**: `FLAME` from muzzle | `LAVA` from shot projectile | `EXPLOSION_EMITTER` on impact | `CAMPFIRE_COSY_SMOKE` post-fire
**Config**: radius=6, impact-only=true, impactDamage=50, impactRadius=6, duration=400, cooldown=400

---

### 68. THE NIGHTMARE LOOM — 38 blocks
**Visual**: A massive industrial loom weaving "soul threads" — frame, heddles, beater bar, warp threads (thin rods), shuttle. Soul particles are the threads.
**Materials**: dark_oak_planks (frame), polished_blackstone (metal heddles), iron_block (metal rails), chain (warp threads), tinted_glass (light-catching glass components), bone_block (shuttle)
**Assembly**:
- Main frame: 6 dark_oak_planks scale(0.2f, 0.9f, 0.2f) vertical posts × 6 (6 blocks)
- Horizontal rails: 4 dark_oak_planks scale(2.0f, 0.2f, 0.2f) (4 blocks)
- 12 warp threads: chain scale(0.04f, 0.04f, 2.0f) thin vertical "threads" (12 blocks)
- Beater bar: 2 polished_blackstone scale(2.0f, 0.15f, 0.15f) (2 blocks)
- Heddles: 4 iron_block scale(1.5f, 0.08f, 0.08f) horizontal heddle bars (4 blocks)
- Shuttle: 3 bone_block scale(0.8f, 0.15f, 0.2f) (3 blocks)
- Glass tension windows: 3 tinted_glass scale(0.5f, 0.5f, 0.05f) (3 blocks)
- Detail components: 4 misc (4 blocks)

**Animation**: Beater bar sweeps (translateZ 0→0.6 over 8t, back over 8t, repeating). Heddles raise/lower alternately (translateY ±0.4, 20t cycle). Shuttle flies across (teleportDuration=6, X-direction). The "weaving" is a recognizable mechanical action. Warp threads glow when shuttle passes.
**Sounds**: `BLOCK_WOOD_STEP` rhythmic clacking | `BLOCK_CHAIN_WALK` thread tension | `ENTITY_VEX_AMBIENT` low hum
**Particles**: `SOUL` flowing as threads | `SCULK_SOUL` woven patterns | `ENCHANT` from heddle motion
**Config**: radius=6, damage=26, ticks-between=15, delay=10, duration=450, cooldown=300

---

### 69. THE PAIN BATTERY — 40 blocks
**Visual**: An electrical torture apparatus — tall electrode towers, connecting arcs, a central containment sphere, energy collector rods.
**Materials**: polished_blackstone (electrode towers), iron_block (conductors), amethyst_block (energy sphere), tinted_glass (containment shell), polished_deepslate (base), end_rod (energy discharge rods)
**Assembly**:
- 2 electrode towers: each 4 polished_blackstone scale(0.25f, 1.0f, 0.25f) stacked (8 blocks)
- 2 electrode tips: iron_block scale(0.3f, 0.4f, 0.3f) pointed at top (2 blocks)
- Central containment sphere: 8 tinted_glass in golden-angle sphere pattern scale(0.6f, 0.6f, 0.6f) (8 blocks)
- Energy core: 4 amethyst_block scale(0.35f, 0.35f, 0.35f) inside sphere (4 blocks)
- 4 collector rods: end_rod scale(0.1f, 0.1f, 1.0f) angled from base to sphere (4 blocks)
- Base platform: 4 polished_deepslate scale(0.5f, 0.15f, 0.5f) (4 blocks)
- Discharge "arc" blocks: 6 end_rod scale(0.05f, 0.05f, 1.5f) between towers and sphere (6 blocks)
- Energy overflow: 4 amethyst_block scale(0.15f, 0.15f, 0.15f) floating discharge (4 blocks)

**Animation**: Energy sphere pulses (scale 0.6→0.9 on 30t cycle). Discharge arcs appear between towers and sphere (end rods scale from 0.01→1.5 over 5t, then flash back). Collector rods rotate slowly. Electrode tips glow intensely before discharge. Full discharge every 40t: all arcs fire simultaneously + SONIC_BOOM + damage spike.
**Sounds**: `ENTITY_LIGHTNING_THUNDER` (0.4f, 1.2f) on discharge | `BLOCK_BEACON_AMBIENT` charging hum | `BLOCK_AMETHYST_BLOCK_CHIME` energy
**Particles**: `END_ROD` (actual end rod particles) along discharge arcs | `PORTAL` from sphere | `ELECTRIC_SPARK` style dust
**Config**: radius=7, damage=38, ticks-between=10 (fast during discharge), damageDelayTicks=30 (sync with discharge), duration=480, cooldown=320

---

### 70. THE INFERNAL CLOCK MECHANISM — 44 blocks
**Visual**: An exposed clockwork mechanism — multiple clock hands at crazy angles, overlapping gears visible, escapement, pendulum, all the internal parts of a clock exploded and visible simultaneously.
**Materials**: polished_blackstone (clock frame/gears), iron_block (metal components), gold_block (gear faces), polished_deepslate (secondary gears), chain (connecting chains), dark_oak_trapdoor (flat escapement panels)
**Assembly**:
- Main clock face ring: 8 polished_blackstone in ring scale(0.5f, 0.15f, 0.5f) (8 blocks)
- 4 clock hands: iron_block scale(0.08f, 0.05f, 0.9f) rotating at different speeds (4 blocks)
- 4 large gears: gold_block scale(0.5f, 0.1f, 0.5f) flat in ring + iron_block teeth × 4 per gear = 16 blocks total
- 2 small gears: polished_deepslate scale(0.3f, 0.1f, 0.3f) (4 blocks including teeth)
- Pendulum: 3 polished_blackstone scale(0.1f, 0.1f, 0.9f) (rod) + 1 iron_block (weight) = 4 blocks
- 4 chain drives: chain scale(0.3f, 0.3f, 0.3f) (4 blocks)
- Escapement lever: 2 dark_oak_trapdoor (2 blocks)
- 6 misc springs/axles: iron_block/polished_blackstone (6 blocks)

**Animation**: Every element rotates independently at different speeds — clock hands trace circles, gears mesh, pendulum swings (rotateX ±20° on 20t period). The entire assembly is slightly overloaded — periodically one gear catches (stops), all stress builds, then SLAM restart with explosion particles. Time appears to stutter.
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` tick-tock | `BLOCK_IRON_DOOR_OPEN` gear grind | `BLOCK_PISTON_EXTEND` on catch-slam
**Particles**: `CRIT` (sparks) from grinding gears | `SMOKE` from overstress | `CAMPFIRE_COSY_SMOKE` friction heat
**Config**: radius=7, damage=32, ticks-between=15, delay=10, duration=480, cooldown=330

---

### 71. THE GATE MECHANISM — 45 blocks
**Visual**: A massive portcullis with visible raising mechanism — the iron gate itself, the counterweight system, the windlass (winch), and the two flanking towers it descends into.
**Materials**: iron_bars (portcullis grid), iron_block (metal frame), dark_oak_planks (windlass/wooden mechanism), polished_blackstone (towers), chain (raising chains), obsidian (dark tower stone)
**Assembly**:
- Portcullis grid: 12 iron_bars scale(0.15f, 0.15f, 2.0f) in 4×3 vertical grid (12 blocks)
- Portcullis frame: 4 iron_block scale(0.3f, 2.5f, 0.3f) vertical frame sides + 2 iron_block horizontal = 6 blocks
- Portcullis spikes: 6 iron_block scale(0.15f, 0.4f, 0.15f) at bottom (6 blocks)
- Left tower: 4 polished_blackstone scale(0.4f, 0.9f, 0.4f) stacked (4 blocks)
- Right tower: 4 polished_blackstone mirror (4 blocks)
- Raising chains: 3 chain scale(0.2f, 1.8f, 0.2f) hanging from top (3 blocks)
- Counterweight: 2 obsidian scale(0.5f, 0.5f, 0.5f) (2 blocks)
- Windlass: 2 dark_oak_planks scale(0.8f, 0.3f, 0.3f) + 1 iron_block center (3 blocks)
- 5 misc hardware detail: iron_block scale(0.2f, 0.2f, 0.2f) (5 blocks)

**Animation**: Portcullis rises (translateY 0→3 over 30t, the gate opening). Then slams down (translateY 3→0 over 8t — the crushing slam). `damageOnImpactOnly` when slam hits bottom. Chains update length as gate moves. Counterweight moves opposite to gate (when gate rises, counterweight drops). Windlass spins during motion.
**Sounds**: `BLOCK_CHAIN_BREAK` chain rattling | `BLOCK_IRON_TRAPDOOR_OPEN` on raise | `BLOCK_ANVIL_LAND` on slam impact
**Particles**: `BLOCK_CRACK(IRON_BLOCK)` during slam | `SMOKE` from windlass | `CRIT` from spike tips on slam
**Config**: radius=5, impact-only=true, impactDamage=48, impactRadius=5, duration=250, cooldown=400

---

## CATEGORY 7: VoidGeometry (VoidGeometry.java)
*Abstract eldritch shapes, orbital geometry, cosmic nightmare*
*Theme: Void black, amethyst purple, end stone white, crying obsidian, portal blue*

---

### 72. THE NIGHTMARE STAR — 36 blocks
**Visual**: A 6-pointed star (Star of David form) but 3D and dimensional — clearly a star shape with elongated pointed arms, spinning on multiple axes.
**Materials**: obsidian (main star body), crying_obsidian (glowing edges), amethyst_block (energy charge), end_stone_bricks (contrast accents), tinted_glass (dimensional panels)
**Assembly**:
- 6 star arm spikes: obsidian scale(0.2f, 0.2f, 1.8f) elongated rods pointing in 6 directions (6 blocks)
- 6 arm secondary (shorter): obsidian scale(0.2f, 0.2f, 1.0f) between primary arms (6 blocks)
- 6 edge glows: crying_obsidian scale(0.12f, 0.12f, 1.6f) parallel to each primary arm (6 blocks)
- Central sphere: 4 amethyst_block scale(0.5f, 0.5f, 0.5f) golden-angle distribution (4 blocks)
- 6 flat plane intersections: tinted_glass scale(0.8f, 0.8f, 0.04f) at 60° angles through center (6 blocks)
- 8 end_stone_brick accent: scale(0.3f, 0.3f, 0.3f) floating at arm tips (8 blocks)

**Animation**: Primary rotation on Y (80t). Secondary rotation on X (60t) via separate Marker. These compound rotations create a "tumbling" effect. Arms extend and retract (scale Z 1.8→2.5→1.8 on 30t cycle). Amethyst core pulses. Flat planes shimmer phase.
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` | `ENTITY_ELDER_GUARDIAN_AMBIENT` | `BLOCK_END_PORTAL_FRAME_FILL`
**Particles**: `REVERSE_PORTAL` from arm tips | `PORTAL` from core | `SONIC_BOOM` on arm extension
**Config**: radius=8, damage=34, ticks-between=15, delay=10, duration=450, cooldown=320

---

### 73. THE ELDRITCH MANDALA — 40 blocks
**Visual**: An enormous flat geometric mandala — concentric rings with spoke lines, detailed symmetric pattern. Lies horizontally, spins slowly, clearly mandala-shaped.
**Materials**: polished_blackstone (outer rings), crying_obsidian (ring glow lines), amethyst_block (center gems), end_stone_bricks (white accents), tinted_glass (translucent sections)
**Assembly**:
- Outer ring: 12 polished_blackstone scale(0.7f, 0.08f, 0.7f) in ring (12 blocks)
- Mid ring: 8 crying_obsidian scale(0.5f, 0.07f, 0.5f) in ring (8 blocks)
- Inner ring: 6 amethyst_block scale(0.35f, 0.06f, 0.35f) (6 blocks)
- 8 spoke lines: polished_blackstone scale(0.06f, 0.05f, 1.5f) radiating from center (8 blocks)
- Center: 3 amethyst_block scale(0.4f, 0.1f, 0.4f) overlapping (3 blocks)
- Translucent panels: 4 tinted_glass scale(0.6f, 0.04f, 0.6f) between rings (4 blocks)
- Accent dots: 8 end_stone_bricks scale(0.2f, 0.05f, 0.2f) at spoke-ring intersections (8 blocks)

**Animation**: Outer ring rotates one direction, inner rings counter-rotate. Spokes pulse (scale 0.06→0.12 outward from center sequentially). Center pulses Z-scale. Mandala as a whole lifts and descends (Y-bob). Periodically the entire mandala "activates" — all rings sync speed, center blazes, damage burst.
**Sounds**: `BLOCK_BEACON_AMBIENT` | `BLOCK_AMETHYST_BLOCK_BREAK` activation | `ENTITY_ELDER_GUARDIAN_AMBIENT`
**Particles**: `PORTAL` from ring intersections | `REVERSE_PORTAL` from spokes | `ENCHANT` from center
**Config**: radius=7, damage=30, ticks-between=20, delay=10, duration=480, cooldown=320

---

### 74. THE VOID TENDRIL ARRAY — 40 blocks
**Visual**: 8 massive tentacle/tendril arms radiating from a central void sphere — each tendril curving and writhing, searching. Clearly eldritch horror iconography.
**Materials**: obsidian (tendril sections), blackstone (tendril darker segments), crying_obsidian (tips), tinted_glass (void core), amethyst_block (energy flecks on tendrils)
**Assembly** (8 tendrils × 4 blocks + central core):
- Central void sphere: 4 tinted_glass scale(0.5f, 0.5f, 0.5f) (4 blocks)
- 8 tendrils × 4 segments each:
  - Seg 1 (base): obsidian scale(0.4f, 0.4f, 0.8f)
  - Seg 2: obsidian scale(0.35f, 0.35f, 0.7f) (with slight angle change)
  - Seg 3: blackstone scale(0.25f, 0.25f, 0.6f)
  - Seg 4 (tip): crying_obsidian scale(0.15f, 0.15f, 0.4f)
  = 4 × 8 = 32 blocks
- 4 amethyst energy flecks: scale(0.15f, 0.15f, 0.15f) on tendrils (4 blocks)

**Animation**: Each tendril undulates — all 4 segments in a tendril each get a phase-offset Y-oscillation creating a smooth waving motion. Different tendrils wave at different phases and speeds. One tendril periodically lashes toward the nearest player (rapid extend via teleport, then retract). Core pulses. Tendrils slowly rotate around core (orbit).
**Sounds**: `ENTITY_VEX_AMBIENT` (multiple overlapping) | `ENTITY_ELDER_GUARDIAN_AMBIENT` (0.5f, 0.3f) | `ENTITY_PHANTOM_AMBIENT`
**Particles**: `SCULK_SOUL` from tendril tips | `REVERSE_PORTAL` from core | `SOUL_FIRE_FLAME` along tendril edges
**Config**: radius=6, damage=32, ticks-between=10, delay=5, duration=450, cooldown=300, tracks-player=true

---

### 75. THE ANTI-GRAVITY SPHERE — 38 blocks
**Visual**: A sphere that appears to be "inside out" — blocks radiating outward from center in a sphere but inverted, so the blocks face inward and exterior is void. Deeply unsettling geometry.
**Materials**: crying_obsidian (main sphere blocks), amethyst_block (energy inside sphere), obsidian (void between blocks), polished_blackstone (outer border accents), tinted_glass (transparency panels)
**Assembly**:
- Inner sphere: 18 crying_obsidian using spawnSphere(radius=1.5, 18) scale(0.5f, 0.5f, 0.5f) each (18 blocks)
- Anti-gravity fragments (outside): 8 amethyst_block scale(0.3f, 0.3f, 0.3f) orbiting at radius 3 (8 blocks)
- Tinted glass inner panels: 4 tinted_glass scale(0.9f, 0.9f, 0.04f) flat planes intersecting sphere (4 blocks)
- Obsidian outer ring: 4 obsidian scale(0.4f, 0.15f, 0.4f) in outer ring (4 blocks)
- Polished accent: 4 polished_blackstone scale(0.2f, 0.2f, 0.2f) (4 blocks)

**Animation**: Sphere rotates (three simultaneous axes, different speeds). Anti-gravity fragments orbit but also spiral inward/outward. Inner glass planes rotate at different speed than sphere. Sphere scale pulses 1.5→2.0 periodically. The whole structure "breathes." Pull players inward (velocity toward center, every 20t).
**Sounds**: `ENTITY_ENDERMAN_TELEPORT` | `BLOCK_END_PORTAL_SPAWN` | `BLOCK_AMETHYST_BLOCK_CHIME` hum
**Particles**: `REVERSE_PORTAL` inward spiral | `PORTAL` from fragment orbits | `SONIC_BOOM` on pulse
**Config**: radius=7, damage=30, ticks-between=15, delay=5, duration=450, cooldown=310

---

### 76. THE NIGHTMARE HYPERCUBE — 42 blocks
**Visual**: A tesseract projection — inner cube connected to outer cube with 16 edge beams. The inner cube rotates one direction, outer another, creating a 4D rotation illusion.
**Materials**: obsidian (outer cube corners), amethyst_block (inner cube corners), end_rod (edge beams), crying_obsidian (outer edge beams), tinted_glass (face panels)
**Assembly**:
- Outer cube 8 corners: obsidian scale(0.4f, 0.4f, 0.4f) at ±2 on all axes (8 blocks)
- Inner cube 8 corners: amethyst_block scale(0.35f, 0.35f, 0.35f) at ±1 (8 blocks)
- 12 outer cube edges: end_rod scale(0.05f, 0.05f, 3.5f) spanning corners (12 blocks)
- 8 inner-to-outer edges: crying_obsidian scale(0.05f, 0.05f, 2.0f) connecting inner to outer corners (8 blocks)
- 6 face panels: tinted_glass scale(3.5f, 3.5f, 0.02f) at each face (6 blocks)

**Animation**: Inner cube on separate Marker, rotates Y at speed. Outer cube on another Marker, rotates Z at half speed. Edge beams update dynamically to stretch between the two rotating frames (scale/rotate per-beam via Matrix4f each update). The 4D effect is the key. Tinted glass panels shimmer at phase-shifted rates.
**Sounds**: `BLOCK_BEACON_AMBIENT` | `ENTITY_ENDERMAN_TELEPORT` | `BLOCK_END_PORTAL_FRAME_FILL`
**Particles**: `PORTAL` from inner cube | `REVERSE_PORTAL` from outer cube | `ENCHANT` from edge beams
**Config**: radius=8, damage=32, ticks-between=15, delay=15, duration=500, cooldown=350

---

### 77. THE ELDRITCH SPIRAL — 36 blocks
**Visual**: A double helix that doesn't look like DNA — the nightmare version: dark, asymmetric spirals with thorns/spikes jutting outward, making a sinister rotating double helix.
**Materials**: obsidian (helix strands), crying_obsidian (rungs connecting strands), blackstone_brick_stairs (spike thorns), amethyst_block (energy nodes on rungs), polished_blackstone (strand detail)
**Assembly**:
- Strand A (12 blocks): obsidian scale(0.35f, 0.35f, 0.35f) in helix path (4 revolutions, Y=0 to Y=6) (12 blocks)
- Strand B (12 blocks): obsidian mirror helix offset 180° (12 blocks)
- 5 cross-rungs: crying_obsidian scale(0.15f, 0.15f, 1.5f) connecting A to B at 5 points (5 blocks)
- Spike thorns: 7 blackstone_brick_stairs scale(0.25f, 0.25f, 0.5f) jutting outward at key positions (7 blocks)

**Animation**: Entire helix rotates on Y continuously (80t). The two strands also have independent spin relative to each other. Rungs rotate on their own axis. Spike thorns flash when helix rotates through dangerous zone near player. Energy nodes on rungs pulse brightness.
**Sounds**: `BLOCK_AMETHYST_BLOCK_CHIME` | `ENTITY_ELDER_GUARDIAN_AMBIENT` | `BLOCK_BEACON_AMBIENT`
**Particles**: `REVERSE_PORTAL` from strand paths | `SOUL_FIRE_FLAME` from spikes | `PORTAL` from rung connections
**Config**: radius=5, damage=30, ticks-between=12, delay=10, duration=400, cooldown=300

---

### 78. THE FRACTAL VOID — 40 blocks
**Visual**: A cluster of triangular forms at different scales — clearly a fractal Sierpinski-like tetrahedron arrangement in 3D. Abstract but geometrically striking.
**Materials**: amethyst_block (primary faces), obsidian (edge beams), crying_obsidian (secondary smaller faces), tinted_glass (inner void fill), polished_blackstone (detail accents)
**Assembly**:
- Large tetrahedron face (1/3 scale): 3 amethyst_block scale(0.8f, 0.05f, 0.8f) flat plates at 3 faces (3 blocks, angled to form triangle faces)
- 3 medium tetrahedra: each 3 crying_obsidian scale(0.5f, 0.04f, 0.5f) = 9 blocks
- Small scale fractals: 6 amethyst_block scale(0.25f, 0.03f, 0.25f) (6 blocks)
- Edge beams: 12 obsidian scale(0.06f, 0.06f, 0.7f) spanning tetra edges (12 blocks)
- Void inner fill: 4 tinted_glass scale(0.4f, 0.04f, 0.4f) (4 blocks)
- 6 polished_blackstone accents: scale(0.15f, 0.15f, 0.15f) (6 blocks)

**Animation**: Large tetrahedron rotates Y. Each medium tetrahedron orbits the large one while rotating on its own axis. Small ones orbit the mediums. This creates the fractal orbital cascade. All rotate at different speeds creating never-repeating pattern.
**Sounds**: `BLOCK_AMETHYST_BLOCK_BREAK` | `BLOCK_BEACON_AMBIENT` harmonic | `ENTITY_ELDER_GUARDIAN_AMBIENT`
**Particles**: `PORTAL` from face centers | `REVERSE_PORTAL` from edge beams | `ENCHANT` from vertices
**Config**: radius=7, damage=30, ticks-between=15, delay=15, duration=450, cooldown=310

---

### 79. THE DIMENSIONAL RIFT — 44 blocks
**Visual**: A massive portal-like rift but with visible geometric structure — not a flat circle but a multi-layered dimensional tear with fragments of "dimensions" visible.
**Materials**: end_stone_bricks (rift frame fragments), obsidian (rift edge shards), tinted_glass (void interior), amethyst_block (energy discharge), crying_obsidian (tear edge glow), purpur_block (alien geometry)
**Assembly**:
- Outer rift shards: 10 end_stone_bricks scale(varying 0.3-0.8, 0.05f, varying) jagged points (10 blocks)
- Rift edge glow: 8 crying_obsidian scale(0.15f, 0.06f, 0.5f) along edges (8 blocks)
- Interior panels: 6 tinted_glass scale(0.9f, 0.9f, 0.03f) overlapping at slight angles (6 blocks)
- Obsidian inner shards: 6 obsidian scale(0.2f, 0.2f, 0.8f) pointing inward through interior (6 blocks)
- Amethyst discharge: 6 amethyst_block scale(0.2f, 0.2f, 0.2f) orbiting rift edges (6 blocks)
- Purpur alien fragments: 4 purpur_block scale(0.4f, 0.4f, 0.4f) floating inside (4 blocks)
- 4 energy beam anchors: end_stone_bricks scale(0.08f, 0.08f, 1.5f) to exterior (4 blocks)

**Animation**: Outer shards jitter (rapid small oscillations). Interior panels rotate independently at different rates. Discharge amethysts orbit the rift edge. Rift "breathes" (overall scale 1.0→1.15). Periodically pulls players toward rift (velocity toward rift center every 15t). Purpur fragments orbit inside rift.
**Sounds**: `BLOCK_END_PORTAL_SPAWN` | `ENTITY_ENDERMAN_TELEPORT` | `BLOCK_END_PORTAL_FRAME_FILL` | `BLOCK_AMETHYST_BLOCK_CHIME`
**Particles**: `PORTAL` flooding through rift | `REVERSE_PORTAL` at rift boundary | `SONIC_BOOM` from discharge
**Config**: radius=7, damage=35, ticks-between=15, delay=5, duration=450, cooldown=330, tracks-player=true

---

### 80. THE NIGHTMARE CONSTELLATION — 36 blocks
**Visual**: 12 "star" blocks connected by thin beam lines forming a 3D constellation — recognizable as a connected dot-star pattern but in nightmare form.
**Materials**: shroomlight (star nodes), obsidian (connecting beams/lines), amethyst_block (nebula cloud accents), polished_blackstone (dark star variants), end_stone_bricks (bright accent nodes)
**Assembly**:
- 12 star nodes: shroomlight scale(0.3f, 0.3f, 0.3f) at constellation positions in 3D space (12 blocks)
- 3 bright primary stars: end_stone_bricks scale(0.4f, 0.4f, 0.4f) (included above or extra)
- 14 connecting beams: obsidian scale(0.04f, 0.04f, varying) spanning between nodes (14 blocks)
- 8 amethyst nebula: scale(0.2f, 0.2f, 0.2f) drifting around constellation (8 blocks)
- 2 polished_blackstone dark stars: scale(0.35f, 0.35f, 0.35f) (2 blocks)

**Animation**: Entire constellation rotates slowly (Y-axis, 200t period). Individual stars pulse brightness (scale 0.3→0.5 independently). Connecting beams appear and disappear (scale 0.04→0→0.04, each on own 40-80t cycle with offsets). Nebula wisps drift in gentle arcs. Periodically the constellation "collapses" inward then explodes outward.
**Sounds**: `BLOCK_AMETHYST_BLOCK_STEP` soft | `BLOCK_BEACON_AMBIENT` harmonic | `ENTITY_ENDERMAN_AMBIENT`
**Particles**: `ENCHANT` from star nodes | `REVERSE_PORTAL` drifting from nebula | `PORTAL` constellation edges
**Config**: radius=8, damage=28, ticks-between=20, delay=15, duration=500, cooldown=350

---

### 81. THE VOID MIND — 45 blocks
**Visual**: A massive brain-like structure but abstract/void — floating gray/black mass with protruding "synapse" tendrils, pulsing with dark energy. Brain of the nightmare itself.
**Materials**: obsidian (main mass blocks), polished_blackstone (fold details), tinted_glass (translucent thought panels), amethyst_block (synaptic firing nodes), crying_obsidian (dendrite tendrils), sculk (ambient dark)
**Assembly**:
- Central mass: 10 obsidian scale(varying 0.7-1.0, 0.6-1.0, 0.7-1.0) in irregular sphere (10 blocks)
- Surface folds: 8 polished_blackstone scale(0.5f, 0.2f, 0.6f) flat plates at various surface angles (8 blocks)
- Thought panels: 4 tinted_glass scale(0.8f, 0.8f, 0.03f) orbiting translucent (4 blocks)
- Synapse nodes: 8 amethyst_block scale(0.25f, 0.25f, 0.25f) scattered on surface (8 blocks)
- Dendrite tendrils: 10 crying_obsidian scale(0.1f, 0.1f, 0.8f) thin rods radiating from mass (10 blocks)
- Sculk ground shadow: 5 sculk scale(0.5f, 0.02f, 0.5f) flat shadow (5 blocks)

**Animation**: Mass pulses (scale 1.0→1.15, 25t heartbeat). Surface folds shift position slightly each pulse. Synapse nodes "fire" (scale 0.25→0.5 then back, sequentially propagating across mind like neural firing). Dendrite tendrils writhe. Thought panels orbit and rotate. The mind "thinks" — each neural firing propagates to adjacent nodes.
**Sounds**: `ENTITY_WARDEN_HEARTBEAT` | `ENTITY_ELDER_GUARDIAN_AMBIENT` (mind hum) | `BLOCK_SCULK_SENSOR_CLICKING`
**Particles**: `SCULK_SOUL` from dendrites | `REVERSE_PORTAL` from thought panels | `SOUL_FIRE_FLAME` from synaptic fires
**Config**: radius=8, damage=35, ticks-between=15, delay=10, duration=500, cooldown=350, tracks-player=true

---

*[Attacks 82-105 follow the same format — names below for reference, full descriptions to be added during implementation based on these patterns:]*

**82. The Eldritch Compass** — geometric compass rose rotating in 3D  
**83. The Anti-Matter Ball** — sphere that "un-exists" blocks around it (visual only)  
**84. The Dream Lattice** — infinite-feeling 3D grid structure  
**85. The Quantum Smoke** — indeterminate cloud of phasing blocks  
**86. The Void Clock** — clock that counts down in nightmare numerals  
**87. The Nightmare Knot** — topological torus knot structure  
**88. The Reality Grid** — flat grid of intersecting planes  
**89. The Eldritch Sigil** — giant floor sigil in 3D perspective  
**90. The Collapsing Universe** — sphere imploding inward  
**91. The Nightmare Compass** — compass pointing wrong directions  
**92. The Paradox Gate** — gate that opens to itself  
**93. The Dream Spider Web** — 3D web of fine beams  
**94. The Void Bloom** — flower opening made of dark geometry  
**95. The Nightmare Prism** — triangular prism with impossible faces  
**96. The Abyssal Vortex** — spiraling downward cone  
**97. The Dream Shard** — single massive pointed crystal  
**98. The Eldritch Cross** — ornate nightmare crucifix  
**99. The Void Tendril Boss** — massive single tentacle  
**100. The Nightmare Nova** — exploding star burst structure  
**101. The Abyssal Throne** — elaborate void throne  
**102. The Dream Engine Ultimate** — 50+ block ultimate machine  
**103. The Void Maw** — open mouth of darkness  
**104. The Nightmare Colossus Hand** — reaching void hand  
**105. The Final Nightmare** — ultimate boss-tier 50+ block monstrosity  

---

## Implementation Notes

### Block Type Reference for Non-Cube Geometry
| Use Case | Block Recommendation |
|---|---|
| Thin vertical panels (windows, walls) | `dark_oak_trapdoor`, `iron_trapdoor`, `tinted_glass` at scale Z 0.02-0.05 |
| Wedge/ramp shapes | `*_stairs` with FACING direction set |
| Thin horizontal plates | Any block at scale Y 0.02-0.1 |
| Thin rods/beams | Any block at scale X/Z 0.05-0.15, Y tall |
| Cage bars | `iron_bars`, `chain` |
| Glowing eyes | `shroomlight`, `jack_o_lantern` |
| Flesh/organic | `netherrack`, `nether_wart_block` |
| Crystal/magic | `amethyst_block`, `crying_obsidian` |
| Bone/skeleton | `bone_block`, `polished_blackstone` |
| Void/darkness | `obsidian`, `blackstone`, `coal_block` |

### Animation Pattern Reference
| Pattern | How To Implement |
|---|---|
| Continuous Y-spin | `Matrix4f.rotateY(180°+0.1f)` every N ticks, `interpolation=N` |
| Group movement | Marker anchor + BD passengers, teleport Marker |
| Per-bone independent | Each BD has own `setInterpolationDuration` and `setTransformationMatrix` |
| Caterpillar follow | Per-segment sine wave, `phaseOffset = segmentIndex * offset` |
| Scale breathing | `scale(s→s*1.15)` on repeating timer, interpolation=duration |
| Pendulum swing | `rotateX` ±angle, alternating direction |
| Orbital path | `teleport(arc position)` every N ticks with `setTeleportDuration(N)` |
| Snap/slide attack | `translateZ` interpolation, then `damageOnImpactOnly` |

### Damage Config Standards for Devil's Dream
- Constant area damage: `damage=25-45`, `radius=5-8`, `ticks-between=15-25`
- Impact/falling: `damageOnImpactOnly=true`, `impactDamage=40-60`, `impactRadius=4-8`
- Creature tracking: `tracksPlayer=true`, `damage=30-45`, `ticks-between=10-20`
- Environmental feel: `damage=20-30`, large radius `7-9`, `ticks-between=20-30`
- Boss-tier special: `damage=40-55`, `ticks-between=8-15`, requires repositioning to avoid
