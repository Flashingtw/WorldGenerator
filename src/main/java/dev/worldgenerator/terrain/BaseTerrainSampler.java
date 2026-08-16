package dev.worldgenerator.terrain;

/** Generates landscape only: macro geography composed with shared surface relief. */
public final class BaseTerrainSampler {
    private final MacroTerrainLayout macroLayout;
    private final Noise2D broadRelief;
    private final Noise2D surfaceDetail;
    private final Noise2D mountainRidges;
    private final PerlinNoise2D detailWarp;
    private final PerlinNoise2D slopeRelief;
    private final PerlinNoise2D erosionChannels;
    private final PerlinNoise2D microRelief;

    public BaseTerrainSampler(long seed, WorldBounds bounds) {
        macroLayout = new MacroTerrainLayout(seed, bounds);
        broadRelief = new Noise2D(seed ^ 0x6C8E9F1204A7B35DL);
        surfaceDetail = new Noise2D(seed ^ 0x4D91E27AC85306BFL);
        mountainRidges = new Noise2D(seed ^ 0x29B70F163D8CA54EL);
        detailWarp = new PerlinNoise2D(seed ^ 0x243F6A8885A308D3L);
        slopeRelief = new PerlinNoise2D(seed ^ 0x13198A2E03707344L);
        erosionChannels = new PerlinNoise2D(seed ^ 0xA4093822299F31D0L);
        microRelief = new PerlinNoise2D(seed ^ 0x082EFA98EC4E6C89L);
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
        double warpedX = blockX + detailWarp.fractal(
                blockX / 310.0, blockZ / 310.0, 3, 2.0, 0.50) * 38.0;
        double warpedZ = blockZ + detailWarp.fractal(
                (blockX + 7_913) / 310.0, (blockZ - 5_219) / 310.0,
                3, 2.0, 0.50) * 38.0;
        double localShape = slopeRelief.fractal(
                warpedX / 76.0, warpedZ / 76.0, 4, 2.0, 0.62);
        double hillTransition = Math.sqrt(macro.hillStrength());
        double mountainTransition = Math.sqrt(macro.mountainEnvelope());
        double localAmplitude = 1.0
                + hillTransition * 5.0
                + mountainTransition * 28.0;
        double localRelief = localShape * localAmplitude;
        double channelField = Math.abs(erosionChannels.fractal(
                warpedX / 155.0, warpedZ / 155.0, 3, 2.0, 0.54));
        double channel = 1.0 - smoothstep(0.025, 0.115, channelField);
        double channelDepth = channel * channel
                * (hillTransition * 2.5 + mountainTransition * 7.0);
        double microShape = microRelief.fractal(
                warpedX / 8.0, warpedZ / 8.0, 2, 2.0, 0.55);
        double microAmplitude = 0.6
                + hillTransition * 1.5
                + mountainTransition * 4.0;
        double landHeight = lowlands + hillHeight + mountainHeight
                + localRelief + microShape * microAmplitude - channelDepth;
        int height = (int) Math.round(lerp(oceanFloor, landHeight, macro.landStrength()));
        double mountainStrength = macro.mountainEnvelope() * (0.42 + ridge * 0.58);
        return new TerrainSample(height, macro.continentalness(), mountainStrength);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private static double smoothstep(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }
}
