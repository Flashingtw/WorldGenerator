# WorldGenerator

Paper 26.1.2 / Java 25 custom terrain generator.

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
