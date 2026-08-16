package dev.worldgenerator.terrain;

import dev.worldgenerator.map.AdventureMapPlan;
import dev.worldgenerator.map.AdventureMapPlanner;

/** Deep terrain interface: base geography plus the gameplay layout carved into it. */
public final class TerrainSampler {
    public static final int SEA_LEVEL = 63;

    private final BaseTerrainSampler base;
    private final AdventureMapPlan plan;

    public TerrainSampler(long seed) {
        this(seed, WorldBounds.UNLIMITED);
    }

    public TerrainSampler(long seed, WorldBounds bounds) {
        base = new BaseTerrainSampler(seed, bounds);
        plan = bounds.isLimited()
                ? AdventureMapPlanner.create(seed, bounds, base)
                : AdventureMapPlan.empty();
    }

    public TerrainSample sample(int blockX, int blockZ) {
        TerrainSample landscape = base.sample(blockX, blockZ);
        var shaped = plan.shape(blockX, blockZ, landscape.height());
        return new TerrainSample(
                shaped.height(),
                landscape.continentalness(),
                landscape.mountainStrength(),
                shaped.roadStrength(),
                shaped.poiStrength());
    }

    public AdventureMapPlan plan() {
        return plan;
    }
}
