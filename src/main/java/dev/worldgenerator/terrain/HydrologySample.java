package dev.worldgenerator.terrain;

/** Terrain and water result after deterministic river and lake shaping. */
public record HydrologySample(
        int height,
        int waterLevel,
        double riverStrength,
        double lakeStrength,
        double waterfallStrength,
        double shoreStrength) {
}
