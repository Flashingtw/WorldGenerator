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

## Natural slope relief (v0.7.2)

Mountain flanks and foothills no longer rely almost entirely on the smooth
distance to a mountain-chain curve. Domain-warped medium relief creates spurs
and saddles, narrow Perlin zero-value networks cut shallow drainage gullies,
and a low-amplitude 4-8 block detail layer breaks voxel rounding into shorter
contour steps. Detail strength follows the square root of hill and mountain
transition strength, so foothills receive useful variation without turning
mountain summits into noise or damaging broad buildable lowlands.

The terrain diagnostics now include a one-block-per-pixel shaded slope close-up.
Automated scans across three seeds reject sloped windows where a parallel
one-block contour terrace remains continuous for more than 36 blocks.

## Balanced lowlands (v0.7.3)

The 4-8 block micro-relief layer from v0.7.2 was removed because integer height
rounding turned it into dense one-block bumps and pits across ordinary plains.
Medium-scale spurs and saddles remain, while drainage gullies now activate only
inside established hill and mountain regions. The paired acceptance limits keep
parallel slope terraces at 64 blocks or less and short-scale rough points below
2.1% of ordinary lowland samples. This replaces the overly aggressive 36-block
target with a balance between readable slopes and smooth playable ground.

## Seeded terrain hierarchy (v0.7.4)

Seed mixing owns discrete macro layout while low-frequency noise owns broad
landform continuity. Local fractal relief has no unconditional lowland
amplitude and is restricted to three medium-scale octaves; horizontal domain
warping bends hill and mountain contours without adding vertical micro-pits.
Regression tests now pair a 0.3% cap on small enclosed lowland terrace
fragments with an 80-block cap on straight contour runs. Adjacent seeds are
also sampled across the entire finite map and must differ in both height and
terrain class, rather than merely at one coordinate.

## Terrain-aware road network (v0.8.0)

Finite maps now plan a connected hierarchy of trunk, branch, and POI access
roads. Selected graph edges are routed over a 48-block navigation grid whose
cost penalizes water, steep grades, high elevation, and mountain cores. The
result is simplified, given low-frequency seeded coordinate variation, rounded,
checked against the landscape, and graded to a maximum 10.5% centerline slope.

Every POI owns one deterministic entrance shared by all connected routes. Road
centerlines stop at that entrance and the POI mask prevents external gravel
from entering the site core. Trunks, branches, and access sections use distinct
driving-scale widths and shoulders. Automated acceptance covers three seeds at
both 5000 and 10000 blocks; the rendered road-network preview overlays route
hierarchy and POIs on the generated relief map.

## Planned abandoned modern city

The city is planned as a macro region rather than an oversized POI. A 5000x5000
world may contain a compact 600–900 block urban region, while a 10000x10000
world can support a primary abandoned city roughly 1200–2000 blocks wide, with
about 1500 blocks as the initial target. The city should occupy only 5–10% of
playable land so wilderness, mountains, military sites, and travel remain
meaningful.

Generation will first select a terrain-compatible irregular city boundary, then
lay out external connections, arterial roads, functional districts, local
streets, blocks, open space, building slots, and landmarks. Downtown, housing,
industrial, commercial, public-service, transit, park, and outskirts districts
will use different density and building families. This work follows the planned
hydrology pass, because rivers, lakes, bridge locations, and waterfalls must be
known before the city and its foundations are fixed. See [ROADMAP.md](ROADMAP.md)
and [CONTEXT.md](CONTEXT.md) for the accepted order and terminology.
