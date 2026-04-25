# DevilsDream ModelEngine — Master UV & Structure Strategy

This is the AUTHORITATIVE specification for how every .bbmodel in
`models/devilsdream/` must apply UV mapping. Follow it exactly. The previous
attempts failed because every face of every cube sampled the same UV region,
producing a uniform flat-color render. This document fixes that.

---

## 1. Texture Layout (every model uses one 64×64 PNG)

The 64×64 texture is divided into FOUR HARD-EDGED COLOR ZONES.
Color in each zone must be DISTINCTLY DIFFERENT from the others.

```
+---------------------+---------------------+
|                     |                     |
|   ZONE A            |   ZONE B            |
|   PRIMARY-LIT       |   PRIMARY-SHADOW    |
|   rows  0..31       |   rows  0..31       |
|   cols  0..31       |   cols 32..63       |
|                     |                     |
|   Use for:          |   Use for:          |
|   north + south     |   east + west       |
|   side faces        |   side faces        |
|   (faces visible    |   (faces facing     |
|   facing camera)    |   away from light)  |
|                     |                     |
+---------------------+---------------------+
|                     |                     |
|   ZONE C            |   ZONE D            |
|   TOP / HIGHLIGHT   |   EMISSIVE / GLOW   |
|   rows 32..63       |   rows 32..63       |
|   cols  0..31       |   cols 32..63       |
|                     |                     |
|   Use for:          |   Use for:          |
|   up faces          |   ALL faces of      |
|   (brightest, top)  |   emissive elements |
|   AND down faces    |   (glow rings,      |
|   (use bottom rows  |   energy nodes,     |
|   of zone C if you  |   inner fire, etc.) |
|   need a darker     |                     |
|   shadow)           |   Also: down faces  |
|                     |   of structural     |
|                     |   elements that     |
|                     |   have under-lighting|
+---------------------+---------------------+
```

### Color requirements per zone

* **Zone A — primary-lit (top-left)**: the main visible material color.
  Apply a vertical gradient lighter→darker top-to-bottom so that UV-Y near
  row 0 reads brightest and UV-Y near row 31 reads dimmer.
* **Zone B — primary-shadow (top-right)**: same material as Zone A but
  ~40% darker overall. Use a darker base color, less highlight.
* **Zone C — top/highlight (bottom-left)**: brightest version of the
  material — what direct overhead light hits. Bright at top of zone,
  fading to medium at bottom.
* **Zone D — emissive (bottom-right)**: a radial glow centered in the
  zone. Bright emissive color in the middle (e.g. blood-red, purple,
  yellow, cyan — depends on the attack), fading to near-black at zone
  edges. This zone reads as "energy" when faces sample it.

Each model's palette is per-attack (see spec). But the LAYOUT is identical.

---

## 2. Per-Face UV Assignment — STRUCTURAL ELEMENTS

For ordinary cubes (spike segments, bone pieces, frame parts, vertebrae,
slabs that represent solid structure):

```json
"faces": {
  "north": {"uv": [ 2,  2, 30, 30], "texture": 0},
  "south": {"uv": [ 2,  2, 30, 30], "texture": 0},
  "east":  {"uv": [34,  2, 62, 30], "texture": 0},
  "west":  {"uv": [34,  2, 62, 30], "texture": 0},
  "up":    {"uv": [ 2, 34, 30, 62], "texture": 0},
  "down":  {"uv": [16, 50, 30, 62], "texture": 0}
}
```

**This is the default mapping. Apply it to ~80% of all cubes.**

If the cube is small (< 1 unit on a side), you can use sub-regions of
each zone to vary the look across many small cubes:

* small cube variant 1: shift each UV rect by +4 in x, +4 in y
* small cube variant 2: shift each UV rect by -4 in x, +0 in y
  (still inside the 32×32 zone bounds)

Variation across small cubes prevents repetition.

---

## 3. Per-Face UV Assignment — EMISSIVE ELEMENTS

For cubes that represent glowing/energy/fire/halo material (inner fire
ring, halo glow, comet core, eye iris, stained glass, mirror surface,
star points, disc glow between vertebrae, etc.):

```json
"faces": {
  "north": {"uv": [34, 34, 62, 62], "texture": 0},
  "south": {"uv": [34, 34, 62, 62], "texture": 0},
  "east":  {"uv": [34, 34, 62, 62], "texture": 0},
  "west":  {"uv": [34, 34, 62, 62], "texture": 0},
  "up":    {"uv": [34, 34, 62, 62], "texture": 0},
  "down":  {"uv": [34, 34, 62, 62], "texture": 0}
}
```

All six faces sample Zone D (radial glow). The cube reads as a
luminous object. Use this for ~10–15% of cubes per model.

---

## 4. Per-Face UV Assignment — ACCENT / SECONDARY ELEMENTS

For decorative accents (band ornaments, micro-shards, surface flickers,
crack details, page corners, orbital shards, etc.):

```json
"faces": {
  "north": {"uv": [34,  2, 62, 30], "texture": 0},
  "south": {"uv": [34,  2, 62, 30], "texture": 0},
  "east":  {"uv": [ 2,  2, 30, 30], "texture": 0},
  "west":  {"uv": [ 2,  2, 30, 30], "texture": 0},
  "up":    {"uv": [16, 34, 30, 50], "texture": 0},
  "down":  {"uv": [34, 34, 62, 62], "texture": 0}
}
```

Zones swapped from primary so accents read differently from main structure.
Down face uses emissive zone — accents glow slightly underneath. Use this
for 5–10% of cubes per model.

---

## 5. STRUCTURAL UNIQUENESS — DO NOT default to "spikes from ground"

The temptation is to make every model a cluster of spikes erupting from
Y=0. This is the previous-attempt failure mode. EACH attack has a
distinct geometry:

| # | Name | Footprint | Hero shape |
|---|------|-----------|-----------|
| 2 | nightmare_root_surge | star spread, ground level | 5 winding angular root chains |
| 3 | devils_spine_array | curved line along Z | 7 anatomical vertebrae in S-curve |
| 4 | fallen_halo_burst | overhead Y≈8 | flat horizontal ring (not vertical) |
| 5 | dream_collapse_ring | concentric vertical | 2 nested rings + sphere center |
| 6 | devils_sermon_nova | spherical 3D | 20 rays radiating in all directions |
| 7 | nightmare_static_field | scattered random | 24 floating slabs at random heights |
| 8 | infernal_crown_burst | head-height ring | 9 spikes at Y≈10 (circle around head) |
| 9 | fallen_feather_lance | long projectile | single feather with 5 rachis segments |
| 10 | nightmare_shard_volley | tight cluster | 9 shards in formation, flying |
| 11 | devils_tongue_beam | wide horizontal | 6 stacked flat layers, Z-aligned |
| 12 | silver_wing_blade | long projectile | feather blade, vane panels each side |
| 13 | nightmare_eye_projectile | floating sphere | eyeball + iris + tendrils trailing |
| 14 | blood_comet | dense ball+tail | 8 overlapping core cubes + comet tail |
| 15 | fallen_angel_wings_summon | wide horizontal | two giant wings flanking caster |
| 16 | nightmare_cathedral | tall arch | gothic 2-pillar arch + stained glass |
| 17 | devils_halo_array | tall vertical orbit | 7 halos at heights Y=3..14, varied tilt |
| 18 | bone_throne_summon | seated structure | seat + back columns + armrests + legs |
| 19 | silver_mirror_portal | tall thin frame | rectangular frame + reflection inside |
| 20 | devils_constellation | wide irregular | 8 stars in irregular pattern + lines |
| 21 | nightmare_clock | flat vertical disc | clock face + hands + pendulum |
| 22 | fallen_seraph_skeleton | tall organic | ribcage + spine + skull fragments |
| 23 | nightmare_planetarium | nested orbits | central star + 4 orbiting planets |
| 24 | infernal_scripture_array | scattered orbit | 10 floating book pages around lectern |
| 25 | the_dream_itself | massive composite | sphere + arches + wings + halos all |

**No two models should look like minor variants of each other when viewed
from above.** Verify by mentally rendering each model from a top-down
camera before committing.

---

## 6. Density targets

* 50–80 elements (cubes) per model
* 25–35 bones in the outliner hierarchy
* 3 animations (spawn=once, idle=loop, dissipate=once)
* 5–8 keyframes per channel (position/rotation/scale) per animated bone per
  animation
* Total keyframes ~700–1000 per model
* File size ~200–280 KB

If you cannot reach this density without exceeding response token
limits, prioritize: (1) correct structure first, (2) correct UVs second,
(3) animations third. A 150 KB file with right structure beats a 280 KB
file with everything sampling the same UV.

---

## 7. Workflow

For each model:

1. Read its full spec from `D:/CC/ATTACK IDEAS/devilsdreammodelengine.txt`
2. Write a Python texture script to
   `D:/CC/ChaosCraft/tools/gen_textures/<name>_textures.py` that produces
   the 4-zone 64×64 PNG with the per-attack palette. Save PNG to
   `D:/CC/ChaosCraft/src/main/resources/models/devilsdream/<name>_tex.png`
   and base64 to
   `D:/CC/ChaosCraft/tools/gen_textures/<name>_b64.txt`.
3. Run the Python script with the Bash tool.
4. Read the b64 file with the Read tool.
5. **Hand-write** the .bbmodel JSON via the Write tool to
   `D:/CC/ChaosCraft/src/main/resources/models/devilsdream/<name>.bbmodel`.
   Embed the b64 in `textures[0].source` as
   `"data:image/png;base64,<b64-content>"`.
6. Apply the per-face UV mapping rules above to every element.

DO NOT use Python to generate the .bbmodel JSON. Python is for the
texture PNG only.

---

## 8. JSON Schema (matches Blockbench 4.5 free format)

```json
{
  "meta": {"format_version": "4.5", "model_format": "free", "box_uv": false},
  "name": "<model_name>",
  "resolution": {"width": 64, "height": 64},
  "elements": [
    {
      "uuid": "<unique-uuid>",
      "name": "<descriptive_name>",
      "from": [x1, y1, z1],
      "to":   [x2, y2, z2],
      "rotation": [rx, ry, rz],
      "origin":   [ox, oy, oz],
      "faces": {
        "north": {"uv": [u1,v1,u2,v2], "texture": 0},
        "south": {"uv": [u1,v1,u2,v2], "texture": 0},
        "east":  {"uv": [u1,v1,u2,v2], "texture": 0},
        "west":  {"uv": [u1,v1,u2,v2], "texture": 0},
        "up":    {"uv": [u1,v1,u2,v2], "texture": 0},
        "down":  {"uv": [u1,v1,u2,v2], "texture": 0}
      }
    }
  ],
  "outliner": [
    {
      "uuid": "<bone-uuid>",
      "name": "<bone_name>",
      "origin":   [x, y, z],
      "rotation": [rx, ry, rz],
      "children": ["<element-uuid>", "<nested-bone-uuid-or-bone-object>"]
    }
  ],
  "textures": [
    {
      "name": "<name>_tex.png",
      "source": "data:image/png;base64,<b64>",
      "width": 64, "height": 64,
      "id": "0",
      "render_mode": "default",
      "render_sides": "front"
    }
  ],
  "animations": [
    {
      "uuid": "<anim-uuid>",
      "name": "spawn", "loop": "once", "length": 1.2,
      "animators": {
        "<bone-uuid>": {
          "name": "<bone_name>",
          "keyframes": [
            {"uuid":"<kf-uuid>","time":0.0,"color":-1,"interpolation":"catmullrom","data_points":[{"x":"0","y":"0","z":"0"}],"channel":"position"},
            {"uuid":"<kf-uuid>","time":0.5,"color":-1,"interpolation":"catmullrom","data_points":[{"x":"0","y":"3","z":"0"}],"channel":"position"}
          ]
        }
      }
    }
  ]
}
```

UUIDs must be unique within the file. Format is fine to use the
namespace pattern `<modelprefix>NNNN-NNNN-NNNN-NNNN-NNNNNNNNNNNN`
where modelprefix is unique per model (e.g., 02 for model #2, 23 for
model #23).
