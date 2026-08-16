package dev.worldgenerator.biome;

import dev.worldgenerator.terrain.TerrainSample;
import dev.worldgenerator.terrain.TerrainSampler;
import dev.worldgenerator.terrain.WorldBounds;

/** Selects a vanilla biome from bounded climate regions and terrain shape. */
public final class BiomeSampler {
    private final TerrainSampler terrain;
    private final ClimateRegionSampler climate;
    private final WorldBounds bounds;

    public BiomeSampler(long seed) {
        this(seed, WorldBounds.UNLIMITED);
    }

    public BiomeSampler(long seed, WorldBounds bounds) {
        terrain = new TerrainSampler(seed, bounds);
        climate = new ClimateRegionSampler(seed);
        this.bounds = bounds;
    }

    public BiomeKind sample(int x, int y, int z) {
        if (!bounds.contains(x, z)) return BiomeKind.VOID;
        TerrainSample landform = terrain.sample(x, z);
        ClimateSample climateSample = climate.sample(x, z);
        double temperature = climateSample.temperature() - Math.max(0, landform.height() - 90) / 95.0;
        double humidity = climateSample.humidity();

        if (landform.height() < TerrainSampler.SEA_LEVEL - 16) {
            return deepOcean(temperature);
        }
        if (landform.height() < TerrainSampler.SEA_LEVEL) {
            return ocean(temperature);
        }
        if (landform.height() <= TerrainSampler.SEA_LEVEL + 2) {
            if (temperature < -0.48) return BiomeKind.SNOWY_BEACH;
            if (landform.mountainStrength() > 0.35) return BiomeKind.STONY_SHORE;
            return BiomeKind.BEACH;
        }
        if (landform.height() >= 132) {
            if (temperature < -0.42) return BiomeKind.FROZEN_PEAKS;
            if (temperature < 0.28) return BiomeKind.JAGGED_PEAKS;
            return BiomeKind.STONY_PEAKS;
        }
        if (landform.height() >= 105) {
            if (temperature < -0.40) return BiomeKind.GROVE;
            if (humidity < -0.15) return BiomeKind.WINDSWEPT_HILLS;
            return BiomeKind.MEADOW;
        }
        if (temperature < -0.58) {
            return BiomeKind.SNOWY_PLAINS;
        }
        if (temperature < -0.28) {
            return humidity > 0.30 ? BiomeKind.MEADOW : BiomeKind.PLAINS;
        }
        if (temperature > 0.58 && humidity < -0.28) {
            return humidity < -0.60 ? BiomeKind.BADLANDS : BiomeKind.DESERT;
        }
        if (temperature > 0.42) {
            if (humidity > 0.55 && landform.height() < 76) return BiomeKind.SWAMP;
            if (humidity < -0.05) return BiomeKind.SAVANNA;
        }
        if (humidity > 0.62 && landform.height() < 76) return BiomeKind.SWAMP;
        if (humidity > 0.35) return BiomeKind.MEADOW;
        if (humidity > 0.08) return BiomeKind.SUNFLOWER_PLAINS;
        return (climateSample.regionId() & 7L) == 0L ? BiomeKind.SUNFLOWER_PLAINS : BiomeKind.PLAINS;
    }

    private static BiomeKind deepOcean(double temperature) {
        if (temperature < -0.50) return BiomeKind.DEEP_FROZEN_OCEAN;
        if (temperature < -0.18) return BiomeKind.DEEP_COLD_OCEAN;
        if (temperature > 0.24) return BiomeKind.DEEP_LUKEWARM_OCEAN;
        return BiomeKind.DEEP_OCEAN;
    }

    private static BiomeKind ocean(double temperature) {
        if (temperature < -0.50) return BiomeKind.FROZEN_OCEAN;
        if (temperature < -0.18) return BiomeKind.COLD_OCEAN;
        if (temperature > 0.58) return BiomeKind.WARM_OCEAN;
        if (temperature > 0.20) return BiomeKind.LUKEWARM_OCEAN;
        return BiomeKind.OCEAN;
    }
}
