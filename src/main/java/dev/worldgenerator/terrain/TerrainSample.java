package dev.worldgenerator.terrain;

public record TerrainSample(
        int height,
        double continentalness,
        double mountainStrength,
        double roadStrength,
        double poiStrength) {
    public TerrainSample(int height, double continentalness, double mountainStrength) {
        this(height, continentalness, mountainStrength, 0.0, 0.0);
    }
}
