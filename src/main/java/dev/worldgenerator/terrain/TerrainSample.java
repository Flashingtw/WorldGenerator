package dev.worldgenerator.terrain;

public record TerrainSample(
        int height,
        double continentalness,
        double mountainStrength,
        double roadStrength,
        double poiStrength,
        int waterLevel,
        double riverStrength,
        double lakeStrength,
        double waterfallStrength,
        double shoreStrength) {
    public TerrainSample(int height, double continentalness, double mountainStrength) {
        this(height, continentalness, mountainStrength, 0.0, 0.0,
                height < TerrainSampler.SEA_LEVEL ? TerrainSampler.SEA_LEVEL : Integer.MIN_VALUE,
                0.0, 0.0, 0.0, 0.0);
    }

    public TerrainSample(
            int height, double continentalness, double mountainStrength,
            double roadStrength, double poiStrength) {
        this(height, continentalness, mountainStrength, roadStrength, poiStrength,
                height < TerrainSampler.SEA_LEVEL ? TerrainSampler.SEA_LEVEL : Integer.MIN_VALUE,
                0.0, 0.0, 0.0, 0.0);
    }

    public boolean underwater() {
        return waterLevel > height;
    }

    public boolean inlandWater() {
        return underwater() && waterLevel > TerrainSampler.SEA_LEVEL;
    }
}
