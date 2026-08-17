package dev.worldgenerator.terrain;

import java.util.ArrayList;
import java.util.List;

/** Owns continent shape, coast features, broad hill regions, and mountain chains. */
final class MacroTerrainLayout {
    private final long seed;
    private final WorldBounds bounds;
    private final Noise2D continents;
    private final Noise2D boundaryWarp;
    private final Noise2D coastTexture;
    private final Noise2D hillRegions;
    private final Noise2D upliftRegions;
    private final List<CoastFeature> peninsulas;
    private final List<CoastFeature> bays;
    private final List<MountainChain> mountainChains;
    private final SatelliteIslandPlan satelliteIslands;
    private final double coastPhaseA;
    private final double coastPhaseB;

    MacroTerrainLayout(long seed, WorldBounds bounds) {
        this.seed = seed;
        this.bounds = bounds;
        continents = new Noise2D(seed ^ 0x1D2C3B4A59687766L);
        boundaryWarp = new Noise2D(seed ^ 0x50A7F019D34C268BL);
        coastTexture = new Noise2D(seed ^ 0x0C0A57A91B4D228FL);
        hillRegions = new Noise2D(seed ^ 0x7E13A9B84F260D5CL);
        upliftRegions = new Noise2D(seed ^ 0x3BD471E2A6509F8CL);
        coastPhaseA = unitHash(0, 101) * Math.PI * 2.0;
        coastPhaseB = unitHash(0, 103) * Math.PI * 2.0;
        peninsulas = bounds.isLimited() ? createCoastFeatures(false) : List.of();
        bays = bounds.isLimited() ? createCoastFeatures(true) : List.of();
        mountainChains = bounds.isLimited() ? createMountainChains() : List.of();
        satelliteIslands = new SatelliteIslandPlan(seed, bounds);
    }

    MacroTerrainSample sample(int blockX, int blockZ) {
        double scale = bounds.isLimited() ? Math.max(760.0, bounds.size() / 5.8) : 1_050.0;
        double amount = bounds.isLimited() ? bounds.size() * 0.035 : 240.0;
        double warpedX = blockX
                + boundaryWarp.fractal(blockX / scale, blockZ / scale, 3, 2.0, 0.5) * amount;
        double warpedZ = blockZ
                + boundaryWarp.fractal((blockX + 18_731) / scale,
                        (blockZ - 7_913) / scale, 3, 2.0, 0.5) * amount;
        return bounds.isLimited()
                ? finiteSample(warpedX, warpedZ, blockX, blockZ)
                : unlimitedSample(warpedX, warpedZ);
    }

    private MacroTerrainSample finiteSample(
            double x, double z, double featureX, double featureZ) {
        double half = bounds.size() / 2.0;
        double angle = Math.atan2(z, x);
        double radial = Math.hypot(x / (half * 0.91), z / (half * 0.94));
        double coastNoise = coastTexture.fractal(x / 680.0, z / 680.0, 3, 2.0, 0.5);
        double coastRadius = 0.86
                + Math.sin(angle * 3.0 + coastPhaseA) * 0.055
                + Math.sin(angle * 5.0 + coastPhaseB) * 0.032
                + coastNoise * 0.055;
        double land = smoothstep(-0.035, 0.055, coastRadius - radial);

        for (CoastFeature peninsula : peninsulas) {
            land = Math.max(land, peninsula.strength(x, z));
        }
        for (CoastFeature bay : bays) {
            land *= 1.0 - bay.strength(x, z) * 0.96;
        }
        // Authored offshore features stay aligned with their bridge geometry while
        // the main coastline remains domain-warped.
        land = satelliteIslands.shapeMainIsland(land, featureX, featureZ);
        land = clamp01(land);

        double hills = smoothstep(-0.30, 0.52,
                hillRegions.fractal(x / 1_050.0, z / 1_050.0, 3, 2.0, 0.52));
        double satelliteStrength = satelliteIslands.islandStrength(featureX, featureZ);
        double rangeWarp = bounds.size() * 0.018;
        double rangeX = x + upliftRegions.fractal(x / 760.0, z / 760.0, 3, 2.0, 0.5) * rangeWarp;
        double rangeZ = z + hillRegions.fractal(
                (x + 5_219) / 760.0, (z - 8_117) / 760.0, 3, 2.0, 0.5) * rangeWarp;
        double mountainEnvelope = 0.0;
        for (MountainChain chain : mountainChains) {
            mountainEnvelope = Math.max(mountainEnvelope, chain.strength(rangeX, rangeZ));
        }
        double mountainTexture = 0.82 + upliftRegions.fractal(
                x / 620.0, z / 620.0, 3, 2.0, 0.5) * 0.30;
        mountainEnvelope = clamp01(mountainEnvelope * mountainTexture);
        mountainEnvelope *= smoothstep(0.48, 0.90, land);
        mountainEnvelope *= 1.0 - satelliteStrength * 0.96;
        hills *= land * (1.0 - mountainEnvelope * 0.62);
        hills *= 1.0 - satelliteStrength * 0.88;
        return new MacroTerrainSample(land, land * 2.0 - 1.0, hills, mountainEnvelope);
    }

    private MacroTerrainSample unlimitedSample(double x, double z) {
        double continentalness = continents.fractal(x / 2_550.0, z / 2_550.0, 4, 2.02, 0.52);
        double land = smoothstep(-0.17, 0.10, continentalness);
        double hills = smoothstep(-0.34, 0.50,
                hillRegions.fractal(x / 1_100.0, z / 1_100.0, 3, 2.0, 0.52));
        double uplift = upliftRegions.fractal(x / 1_650.0, z / 1_650.0, 3, 2.0, 0.52);
        double mountainEnvelope = smoothstep(0.20, 0.48, uplift)
                * smoothstep(0.46, 0.88, land);
        hills *= land * (1.0 - mountainEnvelope * 0.58);
        return new MacroTerrainSample(land, continentalness, hills, mountainEnvelope);
    }

    private List<CoastFeature> createCoastFeatures(boolean bay) {
        int count = bounds.size() >= 8_000 ? 3 : 2;
        double half = bounds.size() / 2.0;
        List<CoastFeature> result = new ArrayList<>();
        double featurePhase = unitHash(0, bay ? 149 : 129) * Math.PI * 2.0;
        for (int index = 0; index < count; index++) {
            double angle = featurePhase + index * 2.399963229728653
                    + signedHash(index, bay ? 151 : 131) * 0.16;
            double centerRadius = half * (bay ? 0.77 : 0.78);
            double centerX = Math.cos(angle) * centerRadius;
            double centerZ = Math.sin(angle) * centerRadius;
            double radialLength = half * (bay ? 0.20 : 0.21);
            double tangentialWidth = bounds.size() * (bay ? 0.075 : 0.065)
                    * (0.86 + unitHash(index, bay ? 157 : 137) * 0.28);
            result.add(new CoastFeature(
                    centerX, centerZ, angle, radialLength, tangentialWidth));
        }
        return List.copyOf(result);
    }

    private List<MountainChain> createMountainChains() {
        int count = bounds.size() >= 8_000 ? 4 : 3;
        int size = bounds.size();
        List<MountainChain> result = new ArrayList<>();
        double layoutPhase = unitHash(0, 89) * Math.PI * 2.0;
        for (int index = 0; index < count; index++) {
            double sectorAngle = layoutPhase + index * Math.PI * 2.0 / count
                    + signedHash(index, 17) * 0.24;
            double centerRadius = size * (0.155 + unitHash(index, 31) * 0.080);
            double centerX = Math.cos(sectorAngle) * centerRadius;
            double centerZ = Math.sin(sectorAngle) * centerRadius;
            double angle = sectorAngle + Math.PI * 0.5 + signedHash(index, 47) * 0.68;
            double length = size * (0.20 + unitHash(index, 61) * 0.11);
            double halfDx = Math.cos(angle) * length * 0.5;
            double halfDz = Math.sin(angle) * length * 0.5;
            double curve = signedHash(index, 73) * size * 0.075;
            double normalX = -Math.sin(angle);
            double normalZ = Math.cos(angle);
            double width = size * (0.032 + unitHash(index, 79) * 0.020);
            result.add(new MountainChain(
                    centerX - halfDx,
                    centerZ - halfDz,
                    centerX + normalX * curve,
                    centerZ + normalZ * curve,
                    centerX + halfDx,
                    centerZ + halfDz,
                    width,
                    unitHash(index, 83) * Math.PI * 2.0));
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

    record MacroTerrainSample(
            double landStrength,
            double continentalness,
            double hillStrength,
            double mountainEnvelope) {
    }

    private record CoastFeature(
            double centerX,
            double centerZ,
            double angle,
            double radialLength,
            double tangentialWidth) {
        double strength(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            double radial = dx * Math.cos(angle) + dz * Math.sin(angle);
            double tangential = -dx * Math.sin(angle) + dz * Math.cos(angle);
            double distance = Math.hypot(radial / radialLength, tangential / tangentialWidth);
            return 1.0 - smoothstep(0.48, 1.0, distance);
        }
    }

    private record MountainChain(
            double startX,
            double startZ,
            double controlX,
            double controlZ,
            double endX,
            double endZ,
            double width,
            double phase) {
        double strength(double x, double z) {
            double distance = Double.POSITIVE_INFINITY;
            double nearestT = 0.5;
            double previousX = startX;
            double previousZ = startZ;
            for (int step = 1; step <= 18; step++) {
                double t = step / 18.0;
                double inverse = 1.0 - t;
                double nextX = inverse * inverse * startX
                        + 2.0 * inverse * t * controlX + t * t * endX;
                double nextZ = inverse * inverse * startZ
                        + 2.0 * inverse * t * controlZ + t * t * endZ;
                SegmentDistance candidate = distanceToSegment(
                        x, z, previousX, previousZ, nextX, nextZ);
                if (candidate.distance() < distance) {
                    distance = candidate.distance();
                    nearestT = (step - 1 + candidate.segmentT()) / 18.0;
                }
                previousX = nextX;
                previousZ = nextZ;
            }
            double widthVariation = 0.70 + Math.sin(nearestT * Math.PI) * 0.23
                    + Math.sin(nearestT * Math.PI * 6.0 + phase) * 0.09;
            double localWidth = width * widthVariation;
            double endTaper = smoothstep(0.02, 0.14, nearestT)
                    * (1.0 - smoothstep(0.86, 0.98, nearestT));
            return (1.0 - smoothstep(localWidth * 0.30, localWidth, distance)) * endTaper;
        }

        private static SegmentDistance distanceToSegment(
                double x, double z, double startX, double startZ, double endX, double endZ) {
            double dx = endX - startX;
            double dz = endZ - startZ;
            double lengthSquared = dx * dx + dz * dz;
            if (lengthSquared == 0.0) {
                return new SegmentDistance(Math.hypot(x - startX, z - startZ), 0.0);
            }
            double t = Math.max(0.0, Math.min(1.0,
                    ((x - startX) * dx + (z - startZ) * dz) / lengthSquared));
            return new SegmentDistance(
                    Math.hypot(x - (startX + dx * t), z - (startZ + dz * t)), t);
        }

        private record SegmentDistance(double distance, double segmentT) {
        }
    }
}
