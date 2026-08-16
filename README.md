# WorldGenerator

Paper 26.1.2 / Java 25 custom terrain generator.

## Build

Install a Java 25 JDK, then run `gradle build`. Copy the resulting JAR from
`build/libs/` into the Paper server's `plugins/` directory.

## Create a world

As an operator, run:

```text
/wg create <name> [seed]
/wg tp <name|namespace:name>
/wg lobby
/wg list
```

The world is created under the plugin namespace and the player is teleported to
its spawn. A numeric seed is used directly; text seeds are converted to a stable
number.

The v0.2 generator supplies continents, oceans, rolling plains, coherent mountain
regions, bounded climate regions, terrain-aware vanilla biomes, and deterministic
safe land spawns. Vanilla caves and structures remain disabled; biome decorations
and mob spawning are enabled.
