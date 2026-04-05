# Block Display Attack Requirements — ChaosCraft Doom Mode

## Structure
- Minimum 10 block displays per attack structure
- Each attack must look like what it's described as — players should identify it
- Use varied shapes: triangles, spheres, helixes, pyramids, crescents, rings, columns
- Different sizes within a structure (large base → tapered tip, thick core → thin edges)

## Animations & Movement
- Smooth interpolated animations — rotations, scale changes, translations
- Multi-phase lifecycle: spawn animation → active/idle loop → dissipate/cleanup
- Moving attacks should have real motion (sine waves, orbits, falling physics)
- Some attacks track/move toward players, others are stationary area denial
- Always spawn straight — zero yaw/pitch regardless of player look direction

## Visuals
- Glow effects with mode-appropriate color palette
- Particles that fit the attack theme
- Sound effects on spawn, impact, and ambient loops
- Block type variety within a structure
- No potion effects — damage only

## Damage System
Two damage modes:
1. Constant radius — persistent damage zone, configurable delay + interval
2. Impact-only — damage on specific events (meteor hitting ground, etc.)

Default damage radiuses should be somewhat hard to dodge.

## Configuration (per attack, in YAML)
- damage, damage-radius, ticks-between-damage, damage-delay-ticks
- cooldown-ticks, duration-ticks, chance, enabled
- damage-on-impact-only, impact-damage, impact-radius
- tracks-player, modelengine-scale
