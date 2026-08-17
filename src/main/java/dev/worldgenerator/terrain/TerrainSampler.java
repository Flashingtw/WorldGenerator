package dev.worldgenerator.terrain;

import dev.worldgenerator.map.AdventureMapPlan;
import dev.worldgenerator.map.AdventureMapPlanner;

/** Deep terrain interface: base geography plus the gameplay layout carved into it. */
public final class TerrainSampler {
    public static final int SEA_LEVEL = 63;

    private final BaseTerrainSampler base;
    private final HydrologyPlan hydrology;
    private final AdventureMapPlan plan;
    private final SatelliteIslandPlan satelliteIslands;

    public TerrainSampler(long seed) {
        this(seed, WorldBounds.UNLIMITED);
    }

    public TerrainSampler(long seed, WorldBounds bounds) {
        base = new BaseTerrainSampler(seed, bounds);
        satelliteIslands = new SatelliteIslandPlan(seed, bounds);
        hydrology = bounds.isLimited()
                ? HydrologyPlanner.create(seed, bounds, base)
                : HydrologyPlan.empty();
        TerrainSource hydrated = this::sampleHydrology;
        plan = bounds.isLimited()
                ? AdventureMapPlanner.create(seed, bounds, hydrated, satelliteIslands)
                : AdventureMapPlan.empty();
    }

    public TerrainSample sample(int blockX, int blockZ) {
        TerrainSample landscape = sampleHydrology(blockX, blockZ);
        var shaped = plan.shape(blockX, blockZ, landscape.height());
        int waterLevel = shaped.height() >= landscape.waterLevel()
                ? Integer.MIN_VALUE : landscape.waterLevel();
        return new TerrainSample(
                shaped.height(),
                landscape.continentalness(),
                landscape.mountainStrength(),
                shaped.roadStrength(),
                shaped.poiStrength(),
                waterLevel,
                landscape.riverStrength(),
                landscape.lakeStrength(),
                landscape.waterfallStrength(),
                landscape.shoreStrength());
    }

    private TerrainSample sampleHydrology(int blockX, int blockZ) {
        TerrainSample landscape = base.sample(blockX, blockZ);
        HydrologySample water = hydrology.shape(blockX, blockZ, landscape);
        return new TerrainSample(
                water.height(), landscape.continentalness(), landscape.mountainStrength(),
                0.0, 0.0, water.waterLevel(), water.riverStrength(), water.lakeStrength(),
                water.waterfallStrength(), water.shoreStrength());
    }

    public AdventureMapPlan plan() {
        return plan;
    }

    public HydrologyPlan hydrology() {
        return hydrology;
    }
}
