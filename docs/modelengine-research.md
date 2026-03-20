# ModelEngine 4 — Complete Limits & Research

Comprehensive reference for creating ChaosCraft VFX models in Blockbench. Covers every hard limit, workaround, art direction, and the full attack model list for Freezing Ice (25 models) and Corrupted Corruption (30 models).

---

## Table of Contents

1. [Geometry Limits](#geometry-limits)
2. [Bone System](#bone-system)
3. [Animation Limits](#animation-limits)
4. [Performance & Networking](#performance--networking)
5. [What ME4 Cannot Do](#what-me4-cannot-do)
6. [ChaosCraft-Specific Gotchas](#chaoscraft-specific-gotchas)
7. [BBMODEL Creation Rules](#bbmodel-creation-rules)
8. [Ice Element Art Direction](#ice-element-art-direction)
9. [Corruption Element Art Direction](#corruption-element-art-direction)
10. [Attack List — Freezing Ice (25 Models)](#attack-list--freezing-ice-25-models)
11. [Attack List — Corrupted Corruption (30 Models)](#attack-list--corrupted-corruption-30-models)

---

## Geometry Limits

### Cube Rotation
- **Allowed angles:** 0, +22.5, -22.5, +45, -45 degrees only.
- **Only ONE axis at a time per cube.** You cannot rotate a single cube on both X and Z simultaneously.

### Faking Multi-Axis Rotation
- **Compound angles via nested bones:** Place a parent bone rotated on the X axis, then give it a child bone rotated on the Z axis. The child's cubes will appear to have multi-axis tilt.
- **Faking 90 degrees:** Chain two 45-degree rotations across two nested bones (parent at 45, child at 45 = visual 90).

### Size Limits
- **Cube maximum size:** 112 x 112 x 112 pixels (equivalent to 7 x 7 x 7 blocks).
- **Bone maximum size:** 112 x 112 x 112 pixels. If your elements are more than 7 blocks apart, you must split them into separate child bones.
- **Hitbox X and Z must be identical** (no rectangular hitboxes). Maximum hitbox: 64 x 64 x 64 blocks.

---

## Bone System

### Display Entity Mapping
- Every bone that contains cubes becomes **one display entity** at runtime. This is the primary performance cost.
- **Virtual bones** (bones with no cubes, used purely for organization or animation pivots) are **free** — you can have unlimited organizational bones with zero performance cost.

### Special Bone Prefixes

| Prefix | Purpose |
|--------|---------|
| `b_`   | Sub-hitbox bone. Defines a collision region within the model. |
| `ob_`  | Oriented bounding box. Directional collision detection. |
| `tag_` | Nametag anchor. Controls where nametags render above the model. |
| `l_`   | Leash point. Where lead ropes attach. |
| `h_`   | Head bone. Follows player head rotation (for wearable models). |
| `p_`   | Passenger seat. Where a riding entity sits. |

### Rotation Warnings
- **Avoid setting resting rotations to exactly 90, -90, 180, or -180 degrees on bones.** These values can cause gimbal-lock-like visual glitches. Use 89.5 or 90.5 if you need something close to 90.

### Performance Budget
- **Practical limit: 15-20 animated bones per VFX model.** This keeps the display entity count manageable during combat with multiple simultaneous attacks. For static decorative bones that only move during spawn/despawn, you can go higher.

---

## Animation Limits

### Interpolation Types
- **Linear:** Constant speed between keyframes. Good for mechanical motion, sharp impacts, snapping rotations.
- **Catmullrom (smooth):** Cardinal spline interpolation. Good for organic motion, breathing, gentle swaying, fluid arcs.
- **Bezier:** Listed as "under development" — do NOT rely on it. Treat it as unavailable.

### Scriptable Keyframes
- Keyframes can trigger **MythicMobs skills** at specific animation frames. This is how attacks deal damage, spawn particles, or play sounds at precise moments in the animation.

### State Machine
- ME4 has a **state machine animation system** (recommended over the legacy animation controller). Use states like `spawn`, `idle`, `attack`, `dissipate` with transitions between them.

### Hard Restrictions
- **No MoLang support.** Every keyframe value must be a static number. No expressions, no variables, no runtime math.
- **All animation names must be lowercase.** `idle` not `Idle`, `spawn_in` not `Spawn_In`.

---

## Performance & Networking

### Display Entities vs Armor Stands
- ME4 uses **display entities** (introduced in 1.19.4). These are significantly more performant than ME3's armor stand approach:
  - Fewer packets per entity
  - Better client-side interpolation
  - Native transformation support (scale, rotation, translation)

### Built-in Culling
- ME4 has a built-in culling system based on:
  - **Vertical distance threshold** — models beyond a configurable Y-distance are hidden
  - **Angle threshold** — models outside the player's view angle are hidden
- **Culling interval is configurable** in the ME4 config. Lower intervals = more responsive culling but slightly higher server cost.

### ChaosCraft Budget Rules
- Maximum **200 entities** active per attack instance
- Maximum **150 block displays** per attack instance
- These limits exist to protect budget PC players on the server

---

## What ME4 Cannot Do

| Limitation | Workaround |
|------------|------------|
| No true sphere geometry | Approximate spheres by overlapping multiple rotated slab/plane cubes at different angles. 8-12 intersecting planes reads as "round" at gameplay distance. |
| No transparency gradients | Use semi-transparent PNG textures. Set pixel alpha values in the texture file itself. ME4 respects per-pixel alpha. |
| No procedural animation | All keyframes must be hardcoded. No runtime randomization of bone positions. For variety, create 2-3 animation variants and randomly select one via MythicMobs. |
| Single-axis cube rotation only | Use nested bone hierarchies for compound angles (see Geometry Limits). |
| No vertex deformation / mesh morphing | Fake organic deformation by scaling/rotating groups of cubes on different bones. |
| No dynamic texture swapping | Bake all texture states into one PNG using UV region offsets, or use separate models. |
| No runtime color tinting | Pre-bake color variants into the texture. For glowing effects, use emissive texture pixels. |

---

## ChaosCraft-Specific Gotchas

### Orientation
- **Models face North in Blockbench** (negative Z direction). When the model spawns in-game, "forward" is North. The plugin rotates models to face the target player at spawn time — design all models assuming they face -Z.

### Ground Level
- **Feet / ground contact must sit on the 16x16 grid at Y=0.** The bottom of your lowest ground-touching element should be at Y position 0 in Blockbench. This ensures the model sits flush on the block surface in-game.

### Invisible Face Bug
- Blockbench occasionally creates faces with 0-area UVs that are invisible in-game but look fine in the editor. If a face disappears in-game, check its UV mapping — it likely has a collapsed UV rect. Delete the face and recreate it.

### Hot Reloading
- Use `/meg reload` to reload models in-game. No server restart required. This reloads all .bbmodel files from the ModelEngine models folder.

### State Machine Naming
- **All state names must be lowercase.** The state machine is case-sensitive and will silently fail to match states with uppercase characters.

---

## BBMODEL Creation Rules

These rules apply to every .bbmodel file created for ChaosCraft VFX attacks. Follow them exactly.

### Geometry Rules

#### Tapered / Sharp Shapes
- To create tapered geometry (spikes, fangs, pointed tips), **stack 3-4 cubes of descending width**. Example for an icicle:
  - Bottom cube: 6x8x6
  - Middle cube: 4x8x4
  - Upper cube: 2x8x2
  - Tip cube: 1x6x1
- Never rely on a single long thin cube — it reads as a stick, not a spike.

#### Ground Contact
- Every model must have at least one element at **Y=0** that establishes a visual ground plane (frost ring, shadow disc, corruption puddle, base platform).

#### Floating / Orbiting Elements
- Every model must have **at least one orbiting or floating element on its own dedicated bone**. This element should loop independently during idle animation. It sells the "magical VFX" feel.

#### Scale Hierarchy (Readability at Distance)
- **Hero shape:** The largest, most recognizable silhouette element. Must be clearly identifiable at **20 blocks distance**. This is the model's identity.
- **Supporting shapes:** Secondary elements that add complexity. Readable at **10 blocks distance**.
- **Accent shapes:** Fine detail elements (small floating particles, thin lines, tiny orbiting bits). Only visible **up close**.

### Texture Rules (32x32 RGBA)

All ChaosCraft VFX models use a single **32x32 pixel RGBA PNG** texture atlas.

#### Tonal Range (4 values)
Every surface must use exactly 4 tonal values:
1. **Deep shadow** — The darkest tone. Used on undersides, deep recesses, and ground-facing surfaces.
2. **Base mid-tone** — The primary color of the surface. Covers the largest area.
3. **Highlight** — Lighter tone on upward-facing surfaces and edges catching light.
4. **Specular** — Near-white bright spot. Used very sparingly (see below).

#### Directional Lighting (Baked)
- Light comes from **top-left** (northwest). Bake this into the texture:
  - Top faces: brightest
  - Left-facing sides: second brightest
  - Right-facing sides: mid-tone
  - Bottom faces: darkest

#### Edge Highlights
- Every hard edge should have a **1-2 pixel bright line** along the top/left edge. This sells the "hard surface" look and improves readability at distance.

#### Specular Clusters
- Place **3-5 near-white pixels** clustered together on surfaces that would catch direct light. Not scattered randomly — grouped in a tight cluster on the brightest face.

#### Emissive Accents
- **2-4 vivid, fully saturated color pixels** per model section. These represent glowing magical energy. They should be the most chromatically intense pixels in the texture. Place them where magical energy would concentrate (cracks, veins, eye sockets, rune centers).

#### No Flat Zones
- **No flat same-color zone larger than 4x4 pixels.** Break up any large same-color area with subtle tonal variation (even 1-2 brightness steps). Flat zones look like untextured placeholder art.

#### Face-Specific UV Mapping
- **Top faces:** Map to highlight region of the texture (lighter tones).
- **Side faces:** Map to mid-tone center region.
- **Bottom faces:** Map to a dark strip (deep shadow or near-black).

### Animation Rules

#### No Static Bones
- **No bone may ever be completely static during idle animation.** Every bone must have at least subtle motion: +/-0.5 units of translation or +/-1 degree of rotation. Perfectly still bones look dead and break the VFX illusion.

#### Staggered Timing
- **Stagger the keyframe timing of similar bones by 0.05-0.15 seconds.** If you have 4 orbiting crystals, bone 1 starts at 0.0s, bone 2 at 0.07s, bone 3 at 0.12s, bone 4 at 0.05s. Identical timing looks mechanical.

#### Non-Uniform Loop Periods
- **Give different bones different loop lengths.** Core rotation might loop every 4.0s while orbiting elements loop every 5.3s. This creates organic-feeling motion that never exactly repeats.

#### Secondary Motion Lag
- **Child bones should peak 0.2-0.4 seconds after their parent bone.** When a parent bone swings right, the child should follow with a slight delay. This creates follow-through / drag that reads as physical.

#### Interpolation Selection
- **Catmullrom** for: organic motion, breathing, swaying, gentle arcs, idle loops.
- **Linear** for: sharp impacts, snapping into position, mechanical motion, damage frames.

#### Spawn Animation (~0.8-1.0s)
- Structure: anticipation frame (slight pull-back or compression) at ~0.1s, rapid expansion to full size, **overshoot to 1.10-1.15x scale** at the peak, then settle back to 1.0x.
- The overshoot-and-settle is critical — it makes the spawn feel punchy and physical.

#### Idle Animation (4.0-6.0s loop)
- Must include:
  - **Core rotation** (slow continuous spin or sway, 2-5 degrees per second)
  - **Breathing pulse** (gentle scale oscillation, 0.98x to 1.02x)
  - **Orbital lag** (floating elements trailing behind core motion)
  - **Seamless loop** (first and last keyframes must match exactly, with matching tangents)

#### Dissipate Animation (~0.5-0.7s)
- Structure:
  - **Follow-through** — initial momentum continues briefly before collapse begins
  - **Staged collapse** — small accent elements disappear first (0.0-0.2s), medium elements next (0.1-0.4s), hero shape last (0.3-0.7s)
  - **Expansion before collapse** — elements briefly scale UP by 1.1-1.2x before shrinking to 0. This "puff out then vanish" reads much better than a simple shrink.

### Bone Naming Conventions

- **All lowercase, underscores for spaces, no spaces.** `ice_shard_01` not `Ice Shard 01`.
- **Root bone** must be at origin `[0, 0, 0]` and named `root` or `base`.
- Use numbered suffixes for repeated elements: `orbit_crystal_01`, `orbit_crystal_02`, etc.
- Use descriptive intermediate bones: `ring_inner`, `ring_outer`, `spike_cluster_left`.

#### Required Special Bones

| Bone | Purpose | Required? |
|------|---------|-----------|
| `root` | Origin bone at [0,0,0], parent of everything | Yes |
| `hitbox` | Collision detection volume | Yes (for damageable models) |
| `tag_name` | Nametag position | Optional |
| `h_head` | Head tracking bone | No (VFX don't need this) |
| `b_*` | Sub-hitbox regions | Optional |
| `ob_*` | Oriented bounding boxes | Optional |
| `p_seat` | Passenger mount point | No (VFX don't need this) |
| `l_leash` | Leash attach point | No (VFX don't need this) |

---

## Ice Element Art Direction

### Visual Identity
- **Faceted and geometric**, not spiky or organic. Think cut gemstones, crystal lattices, fractured planes — not tree-branch icicles or smooth ice cream scoops.
- **Colors lean cyan, not blue.** The ice palette is cold and bright, not deep-ocean blue. Cyan dominance with white highlights.
- **Frost ground = spreading crack pattern, not glow ring.** Ground effects should look like fracturing ice/permafrost radiating outward, not a generic magic circle.
- **Shadow color stays cold.** Even shadows should have a cool tone. Absolutely no warm tones anywhere in the ice palette.

### Color Palette

#### Primary Surfaces
| Role | Hex | Description |
|------|-----|-------------|
| Deep shadow | `#0a1a3d` | Dark navy. Undersides and deep recesses. |
| Base mid-tone | `#3a7ea8` | Ice blue. Primary surface color. |
| Highlight | `#7de8ff` | Cyan. Top faces and edges. |
| Specular | `#ffffff` | Pure white. Specular clusters only. |

#### Ground / Base Elements
| Role | Hex | Description |
|------|-----|-------------|
| Ground deep | `#060e1a` | Near-black navy. Deepest cracks. |
| Ground mid | `#1a3a52` | Dark slate. Frost-covered ground plane. |

### Texture Notes
- Emissive pixels should be **bright cyan** (`#7de8ff` to `#b0f4ff`) placed along crystal edges and fracture lines.
- Interior glow effect: place 2-3 bright cyan pixels behind a semi-transparent darker face to simulate light inside ice.
- Frost texture: irregular 1px dots of white/cyan scattered on surfaces to simulate frost crystals.

---

## Corruption Element Art Direction

### Visual Identity
- **Near-black base with deep purple veins.** The corruption palette is overwhelmingly dark. Color comes from glowing magical corruption, not from surface paint.
- **Emissive: sharp magenta or sickly green.** Two emissive accent colors — magenta (`#ff00aa`) for active corruption energy, sickly green (`#44ff22`) for toxic/decay variants.
- **Heavy and angular.** Wide, flat planes pressing downward. Corruption feels like weight and oppression, not reaching upward.
- **NOT spikes reaching up.** Corruption models should emphasize **horizontal mass** and **downward pressure**. Flat slabs, crushing plates, heavy overhangs. If something extends upward, it should look like it's being pulled up against its will (bent, straining).

### Color Palette

#### Primary Surfaces
| Role | Hex | Description |
|------|-----|-------------|
| Deep shadow / void | `#0a0012` | Near-black with faint purple. The abyss. |
| Base mid-tone | `#2a0845` | Deep purple. Primary surface color. |
| Emissive (corruption) | `#ff00aa` | Sharp magenta. Glowing veins and cracks. |
| Emissive (toxic) | `#44ff22` | Sickly green. Alternate emissive for decay variants. |

#### Ground / Base Elements
| Role | Hex | Description |
|------|-----|-------------|
| Ground deep | `#050008` | Near-black void. Deepest areas. |
| Ground mid | `#1a0030` | Dark violet. Corrupted ground plane. |

### Texture Notes
- Emissive veins should be **1px wide lines** of magenta running through dark surfaces. They should branch and fork like cracks, not run in straight lines.
- Corruption "pulse" effect: two slightly different emissive shades alternating along a vein line to create a visual rhythm.
- The overall texture should be ~80% dark tones and ~20% emissive. Corruption is darkness with cracks of light, not a glowing object.

---

## Attack List -- Freezing Ice (25 Models)

These 25 ModelEngine VFX models are registered by `IceModelEngine.registerAll()` in Freezing Ice mode. Each model needs a `.bbmodel` file in the ModelEngine models folder.

| # | Model ID | Description |
|---|----------|-------------|
| 1 | `ice_glacier_slam` | Massive faceted glacier block slams down from above onto player position. Hero shape: wide angular ice slab. Spawn: falls from 15 blocks up. Idle: frost particles crack outward from impact point. Dissipate: fractures into angular shards. |
| 2 | `ice_crystal_cage` | Six tall crystal pillars rise around the player forming a hexagonal prison. Each pillar is a separate bone for staggered animation. Spawn: pillars erupt from ground sequentially. Idle: inner frost mist swirls. Dissipate: pillars shatter outward. |
| 3 | `ice_frost_wyrm` | Serpentine dragon-like creature made of faceted ice segments. 8-10 body segment bones with follow-through lag. Spawn: bursts from ground in an arc. Idle: undulates in a figure-8 pattern. Dissipate: segments crack and fall. |
| 4 | `ice_avalanche_wall` | Wide wall of tumbling ice boulders that sweeps across the arena. 6+ rolling boulder bones at different speeds. Spawn: cascades from one side. Idle: continuous rolling motion. Dissipate: boulders crumble into powder. |
| 5 | `ice_frozen_hand` | Giant skeletal hand made of ice reaches up from the ground to grab players. 5 finger bones with independent curl animation. Spawn: fingers burst through ice crust. Idle: fingers slowly close. Dissipate: shatters from fingertips inward. |
| 6 | `ice_blizzard_vortex` | Spinning funnel of ice shards and frost. Core rotation bone with 3 orbital rings of shards. Spawn: builds from ground up. Idle: continuous spin with accelerating inner ring. Dissipate: flies apart centrifugally. |
| 7 | `ice_permafrost_spire` | Tall spiraling tower of ice that erupts from the ground and radiates cold. Geometric helix of faceted planes. Spawn: drills upward from ground. Idle: slow rotation, frost drip particles. Dissipate: crumbles top-down. |
| 8 | `ice_shatter_mine` | Crystalline orb embedded in the ground that detonates when approached. Faceted sphere approximation with internal glow. Spawn: pushes up through frost crust. Idle: pulses with cyan light (scale breathing). Dissipate: explosive outward burst. |
| 9 | `ice_frozen_sentinel` | Humanoid ice golem torso and head rising from the ground. Blocky faceted anatomy. Spawn: assembles from scattered ice chunks. Idle: slow head tracking rotation, breathing pulse. Dissipate: crumbles into rubble. |
| 10 | `ice_icicle_rain` | Cluster of large icicles suspended overhead that drop one by one. 8-12 individual icicle bones. Spawn: icicles materialize in the air. Idle: gentle sway, drip particles. Dissipate: remaining icicles shatter in place. |
| 11 | `ice_frost_rune_circle` | Flat geometric rune pattern on the ground made of ice. Multiple concentric ring bones with counter-rotation. Spawn: rings expand outward from center. Idle: slow rotation, rune symbols pulse. Dissipate: rings contract and shatter. |
| 12 | `ice_glacial_hammer` | Enormous ice war hammer hovering overhead, slams down periodically. Faceted head on a crystalline shaft. Spawn: materializes above player. Idle: slow menacing bob. Dissipate: head shatters on impact, shaft crumbles. |
| 13 | `ice_cryo_beam_tower` | Vertical ice pillar that charges and fires a horizontal beam. Pillar with rotating focus rings. Spawn: pillar rises with rings assembling around it. Idle: rings spin, charging glow. Dissipate: pillar topples sideways and shatters. |
| 14 | `ice_frozen_tree` | Dead tree encased in ice with spreading root frost. Trunk, 4-5 branch bones, root spread bones. Spawn: tree freezes from roots up (sequential bone activation). Idle: branches creak and sway. Dissipate: ice shell cracks off, tree crumbles. |
| 15 | `ice_hailstone_barrage` | Cluster of massive hailstones orbiting a central point before launching at players. 6 large faceted spheres on orbital bones. Spawn: hailstones coalesce from ice dust. Idle: orbital spin with varying radii. Dissipate: launch outward simultaneously. |
| 16 | `ice_frost_serpent_coil` | Ice snake coiled in a defensive spiral pattern. 12+ segment bones forming a coil. Spawn: uncoils from center. Idle: slow breathing coil expansion/contraction. Dissipate: segments separate and shatter individually. |
| 17 | `ice_frozen_crown` | Floating crown of ice spikes rotating overhead. Ring of 8 geometric spike bones. Spawn: spikes converge from all directions to form the ring. Idle: majestic slow rotation, frost trail. Dissipate: spikes launch outward like projectiles. |
| 18 | `ice_cryo_geyser` | Vertical eruption of ice chunks and frost from a ground crack. Central column bone with scattered debris bones. Spawn: ground crack opens, column blasts upward. Idle: continuous upward particle flow. Dissipate: column collapses back into crack. |
| 19 | `ice_crystal_chandelier` | Ornate hanging ice structure with multiple dangling crystal elements. Central hub with 6-8 chain/crystal pendulum bones. Spawn: descends from above with crystals assembling. Idle: gentle pendulum swinging with staggered timing. Dissipate: crystals fall individually then hub shatters. |
| 20 | `ice_glacial_shield` | Large hexagonal ice shield that blocks player movement. Faceted hexagonal plate with reinforcing geometric struts. Spawn: crystallizes from thin air. Idle: frost crawls across surface (UV scroll effect baked into keyframes). Dissipate: cracks radiate from center then collapses. |
| 21 | `ice_frozen_wave` | Curving wall of ice shaped like a frozen ocean wave. Multi-segment curved surface using nested rotated cubes. Spawn: rises from ground in a sweeping arc. Idle: subtle internal shimmer (scale oscillation). Dissipate: topples forward and shatters. |
| 22 | `ice_frost_anchor` | Heavy angular ice mass that pins itself to the ground and radiates freezing cold. Geometric anvil-like shape with radiating ground frost bones. Spawn: drops from above with heavy impact. Idle: frost ground expansion/contraction pulse. Dissipate: frost retracts then anchor cracks. |
| 23 | `ice_cryo_orb_cluster` | Group of 5 floating faceted ice orbs that orbit a central empty point. Each orb is an independent bone with unique orbital path. Spawn: orbs drift in from different directions. Idle: complex multi-speed orbital pattern. Dissipate: orbs accelerate outward and shatter. |
| 24 | `ice_frozen_altar` | Geometric stepped pyramid of ice with a glowing core at the apex. 3-4 tier bones (stepped platform), apex crystal bone. Spawn: tiers stack upward sequentially. Idle: apex crystal rotates and pulses. Dissipate: tiers collapse downward in reverse order. |
| 25 | `ice_blizzard_titan_arm` | Colossal arm made of ice reaching across the arena. Upper arm, forearm, hand, and finger bones with massive scale. Spawn: emerges from ground at the horizon. Idle: slow deliberate reaching motion toward players. Dissipate: fractures starting from fingertips, chain reaction to shoulder. |

---

## Attack List -- Corrupted Corruption (30 Models)

These 30 ModelEngine VFX models are registered by `CorruptionModelEngine.registerAll()` in Corrupted Corruption mode. Each model needs a `.bbmodel` file in the ModelEngine models folder.

| # | Model ID | Description |
|---|----------|-------------|
| 1 | `corruption_void_maw` | Massive mouth-like structure opens in the ground — flat angular jaws with magenta vein interiors. Upper and lower jaw bones. Spawn: ground splits open. Idle: jaws slowly close. Dissipate: jaws slam shut and sink back. |
| 2 | `corruption_tendril_cluster` | 6-8 thick tendrils erupting from a central corruption pool. Each tendril is a multi-segment bone chain. Spawn: tendrils burst upward sequentially. Idle: slow writhing with secondary motion lag. Dissipate: tendrils retract into the pool. |
| 3 | `corruption_crushing_slab` | Wide, flat corrupted slab hovering overhead that slowly descends. Massive horizontal plane with glowing vein cracks. Spawn: materializes above with menacing weight. Idle: slow inevitable descent. Dissipate: cracks widen, slab breaks into falling chunks. |
| 4 | `corruption_decay_pillar` | Corrupted column that rises and emits waves of decay. Thick angular pillar with pulsing vein rings. Spawn: punches through ground. Idle: vein rings pulse outward (scale animation). Dissipate: crumbles from top, veins go dark. |
| 5 | `corruption_glitch_cube` | Rapidly flickering corrupted cube that teleports between positions. Single cube bone with aggressive scale/rotation keyframes. Spawn: glitches into existence (rapid on/off). Idle: chaotic rotation, position jitter. Dissipate: stretches into a line and vanishes. |
| 6 | `corruption_shadow_cage` | Heavy angular cage structure that encases the player. 6 flat wall panel bones pressing inward. Spawn: panels slam in from all directions. Idle: panels slowly compress, veins brighten. Dissipate: panels shatter outward explosively. |
| 7 | `corruption_abyssal_eye` | Giant floating eye made of corruption, watching the player. Outer ring bone, iris bone, pupil bone with tracking rotation. Spawn: assembles from void particles. Idle: iris dilates, tracks player direction. Dissipate: eye collapses into itself. |
| 8 | `corruption_vein_network` | Spreading network of corruption veins across the ground. 12+ branching line bones from a central point. Spawn: veins spread outward rapidly from center. Idle: pulsing emissive traveling along veins. Dissipate: veins retract to center and sink. |
| 9 | `corruption_gravity_well` | Swirling vortex of corrupted debris pulling inward. Central dark core with 8+ orbiting debris bones spiraling inward. Spawn: debris pulls in from edges. Idle: accelerating inward spiral. Dissipate: core detonates, debris launches outward. |
| 10 | `corruption_flesh_wall` | Organic-looking wall of corrupted mass blocking a path. Wide flat structure with magenta vein surface detail. Spawn: grows outward from a single point. Idle: surface undulates like breathing. Dissipate: melts downward into ground. |
| 11 | `corruption_void_anchor` | Heavy angular anchor shape that pins reality in place, creating a distortion zone. Massive downward-pointing geometric shape. Spawn: crashes down from above. Idle: area around it visually distorts (floating debris bones orbit erratically). Dissipate: sinks into ground leaving a crater crack. |
| 12 | `corruption_dark_obelisk` | Tall narrow corrupted monolith with inscription veins. Angular four-sided pillar with glowing rune bones. Spawn: rises from ground with tremor effect. Idle: rune bones pulse in sequence (wave pattern). Dissipate: topples and shatters. |
| 13 | `corruption_decay_bloom` | Flower-like corruption structure that opens to release spores. 5-6 petal bones around a central stamen. Spawn: bud shape pushes through ground. Idle: petals slowly open, spore particles. Dissipate: petals wilt and fall, center collapses. |
| 14 | `corruption_shadow_hand` | Massive hand made of pure darkness reaching from below. 5 finger bones, palm bone, wrist bone. All dark with magenta vein highlights. Spawn: fingers break through ground one by one. Idle: grasping motion. Dissipate: fingers dissolve to shadow wisps. |
| 15 | `corruption_reality_crack` | Floating crack in space with visible void behind it. Jagged frame bones around a dark center plane. Spawn: starts as a point, tears open. Idle: edges flicker and shift. Dissipate: edges collapse inward, sealing shut. |
| 16 | `corruption_bile_geyser` | Eruption of toxic corrupted liquid from the ground. Central column with splash debris bones. Sickly green emissive variant. Spawn: ground bubbles then column erupts. Idle: continuous upward flow with arcing splash. Dissipate: column sputters and dies. |
| 17 | `corruption_parasite_swarm` | Cloud of small corruption fragments orbiting as a swarm. 15-20 tiny cube bones with chaotic independent orbits. Spawn: fragments coalesce from all directions. Idle: buzzing chaotic swarm pattern. Dissipate: fragments scatter in all directions. |
| 18 | `corruption_oppression_plate` | Wide flat plate pressing down from above with immense visual weight. Single massive horizontal bone with ground-shadow bone beneath it. Spawn: fades in overhead. Idle: slow descent with increasing ground shadow. Dissipate: cracks and falls in fragments. |
| 19 | `corruption_wither_tree` | Dead tree shape made of corruption, dripping with decay. Trunk, 5 branch bones, hanging drip element bones. Spawn: grows from ground rapidly. Idle: branches sway heavily, drips fall. Dissipate: bark peels away, trunk collapses. |
| 20 | `corruption_void_portal` | Circular ring structure framing absolute darkness. Ring built from angular segments, center is empty/dark. Spawn: segments assemble into ring. Idle: ring rotates, inner darkness pulses. Dissipate: segments fly apart, darkness implodes. |
| 21 | `corruption_chain_drag` | Heavy corrupted chains erupting from ground, dragging across terrain. 4-6 chain strand bones with segment sub-bones. Spawn: chains burst from ground cracks. Idle: chains sweep and drag across ground. Dissipate: chains retract violently underground. |
| 22 | `corruption_miasma_cloud` | Low-hanging cloud of corruption fog with occasional lightning. Flat wide cloud bones layered at different heights, lightning bolt bone. Spawn: cloud rolls in from one direction. Idle: slow drift, lightning bolt flashes (scale 0 to 1 rapidly). Dissipate: cloud dissipates from edges inward. |
| 23 | `corruption_spine_ridge` | Row of angular spine-like protrusions erupting along a line. 8-10 spine bones in a row with staggered heights. Spawn: spines erupt sequentially along the line. Idle: spines pulse with corruption energy. Dissipate: spines retract into ground in reverse order. |
| 24 | `corruption_entropic_sphere` | Large angular approximation of a sphere that radiates decay. 8-12 intersecting plane bones forming a pseudo-sphere. Spawn: planes snap into alignment. Idle: slow rotation on multiple axes via nested bones. Dissipate: planes separate and drift apart. |
| 25 | `corruption_dread_monolith` | Impossibly tall narrow structure that leans toward the player. Segmented tower bones that can bend. Spawn: grows from ground rapidly. Idle: slow lean toward nearest player, segments adjusting. Dissipate: snaps and falls like a cut tree. |
| 26 | `corruption_heart_pulse` | Beating heart-like core of corruption exposed in the ground. Organic angular shape with rhythmic scale animation. Spawn: ground opens to reveal it. Idle: rhythmic beating (scale pulse 0.95-1.05 at ~1Hz). Dissipate: final massive beat then collapse. |
| 27 | `corruption_null_zone` | Cubic distortion zone where geometry appears broken. Outer frame cube with inner scrambled fragment bones. Spawn: frame assembles, inner fragments randomize. Idle: inner fragments continuously rearrange positions. Dissipate: frame collapses, fragments scatter. |
| 28 | `corruption_stalker_shade` | Humanoid silhouette made of corruption that tracks the player. Simple geometric humanoid (torso, head, 2 arms as slabs). Spawn: rises from player's shadow. Idle: slow menacing drift toward player, head tracking. Dissipate: melts into ground as a puddle. |
| 29 | `corruption_tectonic_rift` | Long ground crack with corruption energy pouring out. Linear crack bone with branching sub-cracks and vertical energy plume bones. Spawn: crack races across ground from one end. Idle: energy plumes pulse along the crack length. Dissipate: crack seals from ends toward center. |
| 30 | `corruption_eclipse_ring` | Massive overhead ring that blocks out light. Large ring bone with inner corona glow bones. Spawn: ring expands from a point overhead. Idle: corona flickers, ring rotates slowly. Dissipate: ring contracts to a point and vanishes with a flash. |

---

## Quick Reference Card

### File Checklist per Model
- [ ] `.bbmodel` file with correct bone naming
- [ ] 32x32 RGBA `.png` texture in the model
- [ ] `spawn` animation (0.8-1.0s)
- [ ] `idle` animation (4.0-6.0s, seamless loop)
- [ ] `dissipate` animation (0.5-0.7s)
- [ ] Root bone at [0,0,0]
- [ ] Ground contact element at Y=0
- [ ] At least one orbiting/floating element on its own bone
- [ ] Hero shape readable at 20 blocks
- [ ] Max 15-20 animated bones
- [ ] All bone names lowercase with underscores
- [ ] All animation names lowercase
- [ ] No resting rotations at exactly 90/-90/180/-180

### Naming Convention
```
<element>_<descriptor>
```
- Ice models: `ice_<name>`
- Corruption models: `corruption_<name>`
- Bone naming: `<purpose>_<detail>_<number>` (e.g., `orbit_crystal_01`, `jaw_upper`, `spine_segment_04`)
