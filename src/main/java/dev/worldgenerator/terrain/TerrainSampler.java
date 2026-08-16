package dev.worldgenerator.terrain;

/** Produces coherent macro terrain independently of Paper APIs. */
public final class TerrainSampler {
    public static final int SEA_LEVEL = 63;

    private final Noise2D continents;
    private final Noise2D warp;
    private final Noise2D mountainRegions;
    private final Noise2D mountainRidges;
    private final Noise2D detail;

    public TerrainSampler(long seed) {
        continents = new Noise2D(seed ^ 0x1D2C3B4A59687766L);
        warp = new Noise2D(seed ^ 0x50A7F019D34C268BL);
        mountainRegions = new Noise2D(seed ^ 0x7E13A9B84F260D5CL);
        mountainRidges = new Noise2D(seed ^ 0x3BD471E2A6509F8CL);
        detail = new Noise2D(seed ^ 0x6C8E9F1204A7B35DL);
    }

    public TerrainSample sample(int blockX, int blockZ) {
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
        double ridge = 1.0 - Math.abs(ridgeNoise);
        ridge = Math.pow(clamp01((ridge - 0.30) / 0.70), 2.2);
        double mountainHeight = mountainMask * ridge * 105.0;

        int height = (int) Math.round(lerp(oceanFloor, plains, land) + mountainHeight);
        return new TerrainSample(height, continentalness, mountainMask * ridge);
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
}
