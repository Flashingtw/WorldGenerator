package dev.worldgenerator.terrain;

/** Generates landscape only: macro geography composed with shared surface relief. */
public final class BaseTerrainSampler {
    private final MacroTerrainLayout macroLayout;
    private final Noise2D broadRelief;
    private final Noise2D surfaceDetail;
    private final Noise2D mountainRidges;

    public BaseTerrainSampler(long seed, WorldBounds bounds) {
        macroLayout = new MacroTerrainLayout(seed, bounds);
        broadRelief = new Noise2D(seed ^ 0x6C8E9F1204A7B35DL);
        surfaceDetail = new Noise2D(seed ^ 0x4D91E27AC85306BFL);
        mountainRidges = new Noise2D(seed ^ 0x29B70F163D8CA54EL);
    }

    public TerrainSample sample(int blockX, int blockZ) {
        MacroTerrainLayout.MacroTerrainSample macro = macroLayout.sample(blockX, blockZ);
        double broad = broadRelief.fractal(blockX / 520.0, blockZ / 520.0, 4, 2.0, 0.49);
        double detail = surfaceDetail.fractal(blockX / 175.0, blockZ / 175.0, 3, 2.0, 0.46);
        double oceanRelief = surfaceDetail.fractal(blockX / 310.0, blockZ / 310.0, 3, 2.0, 0.48);
        double oceanFloor = 42.0 + oceanRelief * 4.5;

        double lowlands = 72.0 + broad * 5.2 + detail * 1.8;
        double hillHeight = macro.hillStrength() * (3.0 + Math.max(0.0, broad) * 12.0);
        double ridgeNoise = mountainRidges.fractal(
                blockX / 430.0, blockZ / 430.0, 4, 2.03, 0.52);
        double ridge = Math.pow(1.0 - Math.abs(ridgeNoise), 1.65);
        double mountainHeight = macro.mountainEnvelope() * (25.0 + ridge * 73.0);
        double landHeight = lowlands + hillHeight + mountainHeight;
        int height = (int) Math.round(lerp(oceanFloor, landHeight, macro.landStrength()));
        double mountainStrength = macro.mountainEnvelope() * (0.42 + ridge * 0.58);
        return new TerrainSample(height, macro.continentalness(), mountainStrength);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }
}
