package dev.worldgenerator.terrain;

import java.util.ArrayList;
import java.util.List;

/** Deterministic offshore islands and the ruined causeways that once served them. */
public final class SatelliteIslandPlan {
    private final Noise2D outlineNoise;
    private final List<Island> islands;
    private final List<BrokenBridge> bridges;

    public SatelliteIslandPlan(long seed, WorldBounds bounds) {
        outlineNoise = new Noise2D(seed ^ 0x51A7E1115A4D5EEDL);
        if (!bounds.isLimited()) {
            islands = List.of();
            bridges = List.of();
            return;
        }

        double countRoll = unitHash(seed, 0, 211);
        int maximum = bounds.size() >= 8_000 ? 3 : 2;
        int count = countRoll < 0.08 ? 0
                : 1 + Math.min(maximum - 1, (int) (unitHash(seed, 0, 223) * maximum));
        double phase = unitHash(seed, 0, 227) * Math.PI * 2.0;
        List<Island> generatedIslands = new ArrayList<>();
        List<BrokenBridge> generatedBridges = new ArrayList<>();
        double half = bounds.size() / 2.0;
        for (int index = 0; index < count; index++) {
            double angle = phase + index * Math.PI * 2.0 / count
                    + signedHash(seed, index, 229) * 0.28;
            double radialLength = bounds.size()
                    * (0.032 + unitHash(seed, index, 233) * 0.008);
            double tangentialWidth = bounds.size()
                    * (0.036 + unitHash(seed, index, 239) * 0.009);
            double centerRadius = half
                    * (0.890 + unitHash(seed, index, 241) * 0.008);
            double centerX = Math.cos(angle) * centerRadius;
            double centerZ = Math.sin(angle) * centerRadius;
            Island island = new Island(index, centerX, centerZ, angle,
                    radialLength, tangentialWidth,
                    unitHash(seed, index, 251) * Math.PI * 2.0);
            generatedIslands.add(island);

            double startRadius = centerRadius - radialLength * 1.72;
            double endRadius = centerRadius + radialLength * 0.10;
            int deckY = TerrainSampler.SEA_LEVEL + 9
                    + (int) Math.round(unitHash(seed, index, 257) * 2.0);
            generatedBridges.add(new BrokenBridge(
                    Math.cos(angle) * startRadius,
                    Math.sin(angle) * startRadius,
                    Math.cos(angle) * endRadius,
                    Math.sin(angle) * endRadius,
                    9.0 + unitHash(seed, index, 263) * 3.0,
                    0.285 + unitHash(seed, index, 269) * 0.020,
                    0.435 + unitHash(seed, index, 271) * 0.025,
                    deckY,
                    mix(seed ^ ((long) index * 0x9E3779B97F4A7C15L))));
        }
        islands = List.copyOf(generatedIslands);
        bridges = List.copyOf(generatedBridges);
    }

    double shapeMainIsland(double mainLand, double x, double z) {
        double carved = mainLand;
        for (Island island : islands) {
            carved *= 1.0 - island.channelStrength(x, z) * 0.985;
        }
        double result = carved;
        for (Island island : islands) {
            result = Math.max(result, island.strength(x, z, outlineNoise));
        }
        return clamp01(result);
    }

    double islandStrength(double x, double z) {
        double result = 0.0;
        for (Island island : islands) {
            result = Math.max(result, island.strength(x, z, outlineNoise));
        }
        return result;
    }

    public List<Island> islands() {
        return islands;
    }

    public List<BrokenBridge> bridges() {
        return bridges;
    }

    public record Island(
            int index,
            double centerX,
            double centerZ,
            double angle,
            double radialLength,
            double tangentialWidth,
            double phase) {
        double strength(double x, double z, Noise2D noise) {
            Local local = local(x, z);
            double texture = noise.fractal(
                    (x + index * 1_913.0) / 165.0,
                    (z - index * 2_137.0) / 165.0, 3, 2.03, 0.51);
            double outlineAngle = Math.atan2(local.tangential(), local.radial());
            double lobes = Math.sin(outlineAngle * 3.0 + phase) * 0.12
                    + Math.sin(outlineAngle * 5.0 - phase * 0.73) * 0.065;
            double distance = Math.hypot(
                    local.radial() / radialLength,
                    local.tangential() / tangentialWidth);
            double outline = 1.0
                    - smoothstep(0.57, 1.0 + texture * 0.24 + lobes, distance);
            double inwardTaper = smoothstep(
                    -radialLength * 1.04, -radialLength * 0.80, local.radial());
            return outline * inwardTaper;
        }

        double channelStrength(double x, double z) {
            Local local = local(x, z);
            double islandDistance = Math.hypot(
                    local.radial() / radialLength,
                    local.tangential() / tangentialWidth);
            double ring = smoothstep(0.91, 1.04, islandDistance)
                    * (1.0 - smoothstep(1.28, 1.52, islandDistance));
            double inward = -radialLength * 1.22;
            double outward = -radialLength * 0.52;
            double radialMask = smoothstep(inward - radialLength * 0.25, inward, local.radial())
                    * (1.0 - smoothstep(outward, outward + radialLength * 0.24,
                    local.radial()));
            double sideMask = 1.0 - smoothstep(
                    tangentialWidth * 0.78, tangentialWidth * 1.34,
                    Math.abs(local.tangential()));
            return Math.max(ring, radialMask * sideMask);
        }

        private Local local(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return new Local(
                    dx * Math.cos(angle) + dz * Math.sin(angle),
                    -dx * Math.sin(angle) + dz * Math.cos(angle));
        }
    }

    public record BrokenBridge(
            double startX,
            double startZ,
            double endX,
            double endZ,
            double width,
            double firstBreak,
            double secondBreak,
            int deckY,
            long seed) {
        public BridgeColumn sample(int x, int z) {
            double dx = endX - startX;
            double dz = endZ - startZ;
            double length = Math.hypot(dx, dz);
            double along = ((x - startX) * dx + (z - startZ) * dz) / length;
            double t = along / length;
            double lateral = (-(x - startX) * dz + (z - startZ) * dx) / length;
            double raggedFirst = firstBreak
                    + signedHash(seed, (int) Math.round(lateral * 3.0), 277) * 0.018;
            double raggedSecond = secondBreak
                    + signedHash(seed, (int) Math.round(lateral * 3.0), 281) * 0.018;
            boolean onSpan = t >= 0.0 && t <= 1.0
                    && (t <= raggedFirst || t >= raggedSecond);
            boolean onDeck = onSpan && Math.abs(lateral) <= width * 0.5;
            boolean edge = onDeck && Math.abs(lateral) >= width * 0.5 - 1.15;
            double pierPhase = Math.abs(Math.IEEEremainder(along, 38.0));
            boolean pier = onDeck && pierPhase <= 2.45 && Math.abs(lateral) <= 2.85;
            boolean pierCap = onDeck && pierPhase <= 3.35
                    && Math.abs(lateral) <= Math.min(width * 0.5 - 0.65, 4.35);
            boolean damaged = unitHash(seed, x * 31 + z, 283) < 0.13
                    || Math.abs(t - raggedFirst) < 0.025
                    || Math.abs(t - raggedSecond) < 0.025;
            return new BridgeColumn(onDeck, edge, pier, pierCap, damaged);
        }
    }

    public record BridgeColumn(
            boolean deck, boolean edge, boolean pier, boolean pierCap, boolean damaged) {
    }

    private record Local(double radial, double tangential) {
    }

    private static double signedHash(long seed, int index, int salt) {
        return unitHash(seed, index, salt) * 2.0 - 1.0;
    }

    private static double unitHash(long seed, int index, int salt) {
        long value = seed ^ ((long) index * 0x9E3779B97F4A7C15L) ^ salt;
        value = mix(value);
        return (value >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double smoothstep(double lower, double upper, double value) {
        if (upper <= lower) return value >= upper ? 1.0 : 0.0;
        double t = clamp01((value - lower) / (upper - lower));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
