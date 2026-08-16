package dev.worldgenerator.terrain;

import java.util.ArrayList;
import java.util.List;

/** Generates landscape only: island/coast, lowlands, hills, and directed ranges. */
public final class BaseTerrainSampler {
    private final long seed;
    private final WorldBounds bounds;
    private final Noise2D continents;
    private final Noise2D warp;
    private final Noise2D mountainRegions;
    private final Noise2D mountainRidges;
    private final Noise2D detail;
    private final List<MountainRange> ranges;

    public BaseTerrainSampler(long seed, WorldBounds bounds) {
        this.seed = seed;
        this.bounds = bounds;
        continents = new Noise2D(seed ^ 0x1D2C3B4A59687766L);
        warp = new Noise2D(seed ^ 0x50A7F019D34C268BL);
        mountainRegions = new Noise2D(seed ^ 0x7E13A9B84F260D5CL);
        mountainRidges = new Noise2D(seed ^ 0x3BD471E2A6509F8CL);
        detail = new Noise2D(seed ^ 0x6C8E9F1204A7B35DL);
        ranges = bounds.isLimited() ? createRanges(bounds.size()) : List.of();
    }

    public TerrainSample sample(int blockX, int blockZ) {
        return bounds.isLimited() ? sampleIsland(blockX, blockZ) : sampleUnlimited(blockX, blockZ);
    }

    private TerrainSample sampleIsland(int blockX, int blockZ) {
        double half = bounds.size() / 2.0;
        double warpScale = Math.max(700.0, bounds.size() / 5.5);
        double warpAmount = bounds.size() * 0.045;
        double warpedX = blockX + warp.fractal(blockX / warpScale, blockZ / warpScale, 3, 2.0, 0.5) * warpAmount;
        double warpedZ = blockZ + warp.fractal((blockX + 18_731) / warpScale,
                (blockZ - 7_913) / warpScale, 3, 2.0, 0.5) * warpAmount;

        double radial = Math.hypot(warpedX / (half * 0.93), warpedZ / half);
        double coastDetail = detail.fractal(warpedX / 620.0, warpedZ / 620.0, 3, 2.0, 0.5) * 0.055;
        double island = 1.0 - smoothstep(0.72 + coastDetail, 0.94 + coastDetail, radial);
        double continentalness = island * 2.0 - 1.0;

        double oceanFloor = 41.0 + detail.fractal(warpedX / 300.0, warpedZ / 300.0, 3, 2.0, 0.48) * 4.0;
        double rolling = detail.fractal(warpedX / 410.0, warpedZ / 410.0, 4, 2.0, 0.48);
        double lowlands = 71.0 + rolling * 7.0;
        double inland = smoothstep(0.18, 0.72, island);

        double mountainStrength = 0.0;
        for (MountainRange range : ranges) {
            double distance = range.distanceTo(blockX, blockZ);
            double rangeMask = 1.0 - smoothstep(range.width() * 0.45, range.width(), distance);
            mountainStrength = Math.max(mountainStrength, rangeMask * inland);
        }
        double ridgeNoise = mountainRidges.fractal(warpedX / 360.0, warpedZ / 360.0, 4, 2.05, 0.52);
        double ridges = Math.pow(1.0 - Math.abs(ridgeNoise), 1.7);
        double mountainHeight = mountainStrength * (28.0 + ridges * 54.0);

        int height = (int) Math.round(lerp(oceanFloor, lowlands + mountainHeight, island));
        return new TerrainSample(height, continentalness, mountainStrength * (0.45 + ridges * 0.55));
    }

    private TerrainSample sampleUnlimited(int blockX, int blockZ) {
        double warpX = warp.fractal(blockX / 900.0, blockZ / 900.0, 3, 2.0, 0.5) * 260.0;
        double warpZ = warp.fractal((blockX + 18_731) / 900.0, (blockZ - 7_913) / 900.0, 3, 2.0, 0.5) * 260.0;
        double x = blockX + warpX;
        double z = blockZ + warpZ;
        double continentalness = continents.fractal(x / 2_300.0, z / 2_300.0, 4, 2.05, 0.52);
        double land = smoothstep(-0.16, 0.10, continentalness);
        double oceanFloor = 42.0 + detail.fractal(x / 260.0, z / 260.0, 3, 2.0, 0.48) * 5.0;
        double plains = 67.0 + detail.fractal(x / 330.0, z / 330.0, 4, 2.0, 0.5) * 10.0;
        double region = mountainRegions.fractal(x / 1_150.0, z / 1_150.0, 3, 2.0, 0.5);
        double mountainMask = smoothstep(0.02, 0.42, region) * land;
        double ridgeNoise = mountainRidges.fractal(x / 510.0, z / 510.0, 4, 2.05, 0.52);
        double ridge = Math.pow(clamp01((1.0 - Math.abs(ridgeNoise) - 0.30) / 0.70), 2.2);
        int height = (int) Math.round(lerp(oceanFloor, plains, land) + mountainMask * ridge * 105.0);
        return new TerrainSample(height, continentalness, mountainMask * ridge);
    }

    private List<MountainRange> createRanges(int size) {
        List<MountainRange> result = new ArrayList<>();
        int count = size >= 8_000 ? 4 : 3;
        for (int index = 0; index < count; index++) {
            double centerX = signedHash(index, 17) * size * 0.22;
            double centerZ = signedHash(index, 31) * size * 0.22;
            double angle = (signedHash(index, 47) + 1.0) * Math.PI;
            double length = size * (0.28 + unitHash(index, 61) * 0.17);
            double halfDx = Math.cos(angle) * length * 0.5;
            double halfDz = Math.sin(angle) * length * 0.5;
            double width = size * (0.055 + unitHash(index, 79) * 0.035);
            result.add(new MountainRange(centerX - halfDx, centerZ - halfDz,
                    centerX + halfDx, centerZ + halfDz, width));
        }
        return List.copyOf(result);
    }

    private double signedHash(int index, int salt) {
        return unitHash(index, salt) * 2.0 - 1.0;
    }

    private double unitHash(int index, int salt) {
        long value = seed ^ ((long) index * 0x9E3779B97F4A7C15L) ^ salt;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double smoothstep(double lower, double upper, double value) {
        double t = clamp01((value - lower) / (upper - lower));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private record MountainRange(double startX, double startZ, double endX, double endZ, double width) {
        double distanceTo(double x, double z) {
            double dx = endX - startX;
            double dz = endZ - startZ;
            double lengthSquared = dx * dx + dz * dz;
            double t = Math.max(0.0, Math.min(1.0,
                    ((x - startX) * dx + (z - startZ) * dz) / lengthSquared));
            return Math.hypot(x - (startX + dx * t), z - (startZ + dz * t));
        }
    }
}
