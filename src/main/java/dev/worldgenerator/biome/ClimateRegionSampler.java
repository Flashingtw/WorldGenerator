package dev.worldgenerator.biome;

import dev.worldgenerator.terrain.PerlinNoise2D;

/**
 * Jittered climate cells with domain-warped boundaries. Cells keep climate
 * regions coherent while preventing one noise threshold from extending forever.
 */
public final class ClimateRegionSampler {
    public static final int REGION_SIZE = 720;
    private static final double SITE_JITTER = 0.34;

    private final long seed;
    private final PerlinNoise2D boundaryWarp;
    private final PerlinNoise2D temperature;
    private final PerlinNoise2D humidity;

    public ClimateRegionSampler(long seed) {
        this.seed = seed;
        boundaryWarp = new PerlinNoise2D(seed ^ 0x29F6C8A17D4503BEL);
        temperature = new PerlinNoise2D(seed ^ 0x51D2E74A9C3068BFL);
        humidity = new PerlinNoise2D(seed ^ 0x73A09B4C2E1D65F8L);
    }

    public ClimateSample sample(int blockX, int blockZ) {
        double warpedX = blockX + boundaryWarp.fractal(blockX / 1_350.0, blockZ / 1_350.0, 3, 2.0, 0.5) * 145.0;
        double warpedZ = blockZ + boundaryWarp.fractal((blockX + 9_173) / 1_350.0, (blockZ - 4_291) / 1_350.0, 3, 2.0, 0.5) * 145.0;
        int baseCellX = fastFloor(warpedX / REGION_SIZE);
        int baseCellZ = fastFloor(warpedZ / REGION_SIZE);

        int nearestX = baseCellX;
        int nearestZ = baseCellZ;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int cellX = baseCellX - 1; cellX <= baseCellX + 1; cellX++) {
            for (int cellZ = baseCellZ - 1; cellZ <= baseCellZ + 1; cellZ++) {
                double siteX = (cellX + 0.5 + signedHash(cellX, cellZ, 11) * SITE_JITTER) * REGION_SIZE;
                double siteZ = (cellZ + 0.5 + signedHash(cellX, cellZ, 29) * SITE_JITTER) * REGION_SIZE;
                double dx = warpedX - siteX;
                double dz = warpedZ - siteZ;
                double distance = dx * dx + dz * dz;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestX = cellX;
                    nearestZ = cellZ;
                }
            }
        }

        double globalTemperature = temperature.fractal(
                blockX / 2_650.0, blockZ / 2_650.0, 4, 2.0, 0.52) * 1.28;
        double globalHumidity = humidity.fractal(
                blockX / 2_450.0, blockZ / 2_450.0, 4, 2.0, 0.52) * 1.32;
        double localTemperature = temperature.sample(
                (blockX + 8_137) / 610.0, (blockZ - 3_719) / 610.0) * 0.72;
        double localHumidity = humidity.sample(
                (blockX - 5_311) / 590.0, (blockZ + 7_127) / 590.0) * 0.72;
        long regionHash = mix(seed ^ ((long) nearestX * 0x632BE59BD9B4E019L)
                ^ ((long) nearestZ * 0x9E3779B97F4A7C15L));
        int mapColor = Math.floorMod(nearestX, 2) * 2 + Math.floorMod(nearestZ, 2);
        long regionId = (regionHash & ~3L) | mapColor;
        return new ClimateSample(
                clamp(globalTemperature + localTemperature),
                clamp(globalHumidity + localHumidity),
                regionId);
    }

    private double signedHash(int x, int z, int salt) {
        long hash = mix(seed ^ ((long) x * 0xD6E8FEB86659FD93L)
                ^ ((long) z * 0xA5A3564E27F8862FL) ^ salt);
        return ((hash >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double clamp(double value) {
        return Math.max(-1.0, Math.min(1.0, value));
    }
}
