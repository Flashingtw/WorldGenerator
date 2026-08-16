# WorldGenerator

Paper 26.1.2 / Java 25 custom terrain generator.

The accepted production order and visual-quality gates are recorded in
[ROADMAP.md](ROADMAP.md). The v0.6.0 military compound is a rejected technical
experiment and is not the quality baseline for future structures.

## Build

Install a Java 25 JDK, then run `gradle build`. Copy the resulting JAR from
`build/libs/` into the Paper server's `plugins/` directory.

## Create a world

As an operator, run:

```text
/wg create <name> [seed] [size]
/wg tp <name|namespace:name>
/wg delete <name> confirm
/wg lobby
/wg list
```

The world is created under the plugin namespace and the player is teleported to
its spawn. A numeric seed is used directly; text seeds are converted to a stable
number.

For finite sizes, the v0.5 generator creates an adventure island with an irregular
coast, broad lowlands, several long mountain ranges, deterministic flattened POI
pads, and a connected gravel-road network. Roads prefer gentle terrain and avoid
water and high peaks, while unavoidable crossings form graded mountain passes. A
size such as `5000x5000` creates a square world border with void outside it;
omitting size keeps the older unlimited continental terrain.

Forest biomes, natural animals, initial mobs, caves, structures, and vanilla
decorations are disabled. This prevents trees, floating leaves, and tree-top snow,
but also means ores, flowers, buildings, and detailed POI props are not generated
yet. Every planned POI now receives a deterministic war-damaged structure facing
its nearest road: small pads become abandoned gas stations, medium pads become
collapsed warehouses, and large pads become military compounds with hangars,
barracks, walls, gates, and watchtowers. Damage is concentrated into coherent
blast breaches with collapsed roof sections, cracked materials, rust, and rubble;
no loot or other gameplay systems are included.

The v0.5.1 refinement writes explicit neighbor-facing block data for generated
iron bars, replaces isolated damaged bars with rusted posts, clips warehouse and
compound corners, and uses partial aprons, internal lanes, annexes, and helipads
instead of filling every POI with a rectangular paved slab.

The v0.5.2 building pass replaces speckled per-block floors with mostly uniform
8-12 block material patches, uses recessed connected glass panes, and gives each
archetype its own authored massing and interior. Gas stations include a shop,
counter, shelves, cold storage, garage equipment, and exposed pipes; warehouses
use an offset office wing, loading canopy, roof monitor, racks, and work areas;
military compounds add asymmetric barriers, furnished barracks, hangar equipment,
an office plan, bunker, helipad markings, and several targeted blast zones.

The v0.5.3 art-direction pass removes the visible circular POI surface and widens
the hidden terrain blend around foundations. Sites are now 20% intact, 50%
weathered, 25% locally damaged, and only 5% ruined. Interior props use shaped and
oriented world-generation blocks (slab tables and counters, stair chairs, cabinets,
crates, shelving, and facing machinery) instead of solid placeholder rows.

The v0.5.4 road handoff stops external gravel at each site's authored entrance
instead of continuing the road segment through the POI center. Military compound
lanes, forecourts, and loading aprons now fully own their internal surface.

The v0.6.0 large-POI pass replaces the generic compound with a dedicated modern
military installation blueprint. It combines a ridged metal-panel hangar, open
motor pool, offset checkpoint, modular barracks, L-shaped headquarters, hardened
armory, helipad, clipped security perimeter, watchtowers, HESCO barriers, and
purpose-built interiors. The site retains natural ground between authored lanes
and foundations, and the existing seed-driven condition system applies only
localized weathering or damage on top of the coherent intact design.

## Blueprint preview (v0.6.1)

The rejected v0.6.0 military compound is disabled in normal world generation.
Large POI terrain and entrances remain reserved, but no military structure is
placed until an authored replacement passes visual review.

v0.6.1 introduces a platform-neutral voxel-blueprint module and an isolated
Paper preview world. Authored `.vbp` resources retain anchors, sparse layers,
directional facing, upper/lower block parts, open state, and explicit pane or
bar connections through rotation and mirroring. Paper-specific BlockData is
created only by the preview adapter.

As an operator, use:

```text
/wg preview
/wg preview rotation_lab 90
/wg preview rotation_lab 270 mirror
/wg preview rebuild 0
/wg preview clear
```

`rotation_lab` is an acceptance rig for doors, stairs, slabs, trapdoors, panes,
bars, lights, rotation, and mirroring. It is deliberately not a building.

## Macro terrain (v0.7.0)

Natural geography now passes through one macro-terrain module before shared
surface relief is applied. Finite maps receive an irregular connected island,
seeded broad headlands and bays, large lowland reserves, and several curved,
separated mountain chains with tapered and locally warped profiles. The
10000x10000 layout uses four geographic mountain sectors while 5000x5000 uses
three, preventing every range from converging at the map center.

Unlimited terrain uses the same ocean-floor, lowland, hill, foothill, ridge, and
peak composition instead of the former unrelated height formula. Its mountain
regions are sparse uplift zones rather than a world-spanning ridge web.

Climate cells were reduced to a 720-block base scale with tighter boundary warp
and stronger local variation. Automated transects across several seeds cap a
single unchanged biome run at 2800 blocks, avoiding continent-sized frozen or
otherwise uniform biome areas while retaining coherent regions.

Run `gradle renderTerrainPreviews` to produce relief and grayscale height maps in
`build/reports/terrain/` for manual visual QA. These diagnostics cover two
5000x5000 seeds, one 10000x10000 seed, and one unlimited-world window.

## Organic surfaces (v0.7.1)

Climate temperature and humidity now use continuous multi-scale gradient Perlin
noise, while bounded Voronoi cells remain only as biome-region identifiers. Dry
terrain is selected from a separate broad geological field instead of switching
topsoil directly at a discrete biome border. Grass therefore reaches terracotta
through a wide coarse-dirt belt, with secondary red-sand and sand patches rather
than a straight material wall.

Small and medium POI roads now reach their authored forecourt or loading entrance.
Road shoulders receive low-amplitude Perlin variation, but large reserved sites
retain their cutoff so external gravel cannot cross a future military compound.
Custom sparse short grass is generated on suitable grass blocks while roads, POI
pads, snow regions, trees, vanilla decorations, and animal spawning remain clear.
Surface-material PNGs are included in `renderTerrainPreviews` output.
