package dev.worldgenerator.terrain;

import java.util.List;

/** Immutable finite-world drainage layout applied before roads and POIs. */
public final class HydrologyPlan {
    private static final HydrologyPlan EMPTY = new HydrologyPlan(0L, List.of(), List.of());

    private final List<River> rivers;
    private final List<Lake> lakes;
    private final PerlinNoise2D edgeNoise;

    HydrologyPlan(long seed, List<River> rivers, List<Lake> lakes) {
        this.rivers = List.copyOf(rivers);
        this.lakes = List.copyOf(lakes);
        edgeNoise = new PerlinNoise2D(seed ^ 0x082EFA98EC4E6C89L);
    }

    public static HydrologyPlan empty() {
        return EMPTY;
    }

    public List<River> rivers() {
        return rivers;
    }

    public List<Lake> lakes() {
        return lakes;
    }

    public HydrologySample shape(int x, int z, TerrainSample base) {
        double height = base.height();
        int waterLevel = base.height() < TerrainSampler.SEA_LEVEL
                ? TerrainSampler.SEA_LEVEL : Integer.MIN_VALUE;
        double riverStrength = 0.0;
        double lakeStrength = 0.0;
        double waterfallStrength = 0.0;
        double shoreStrength = 0.0;

        for (Lake lake : lakes) {
            double cosine = Math.cos(lake.angle());
            double sine = Math.sin(lake.angle());
            double dx = x - lake.x();
            double dz = z - lake.z();
            double localX = dx * cosine + dz * sine;
            double localZ = -dx * sine + dz * cosine;
            double irregularity = edgeNoise.fractal(
                    (x + lake.x()) / 92.0, (z - lake.z()) / 92.0,
                    3, 2.0, 0.50) * 0.38;
            double distance = Math.sqrt(
                    square(localX / lake.radiusX()) + square(localZ / lake.radiusZ()))
                    + irregularity;
            double core = 1.0 - smoothstep(0.78, 1.0, distance);
            double shore = 1.0 - smoothstep(1.0, 1.24, distance);
            if (core > 0.0) {
                double bedVariation = edgeNoise.sample((x - 941) / 38.0, (z + 613) / 38.0);
                double targetBed = lake.waterLevel() - lake.depth() + bedVariation * 1.4;
                height = lerp(height, Math.min(height, targetBed), core);
                if (core > 0.42 && height < lake.waterLevel()) {
                    waterLevel = Math.max(waterLevel, lake.waterLevel());
                }
                lakeStrength = Math.max(lakeStrength, core);
            }
            shoreStrength = Math.max(shoreStrength, Math.max(0.0, shore - core));
        }

        for (River river : rivers) {
            Projection projection = river.project(x, z);
            double width = lerp(river.headWidth(), river.mouthWidth(), projection.progress());
            double edgeOffset = edgeNoise.fractal(
                    (x + river.salt()) / 76.0, (z - river.salt()) / 76.0,
                    2, 2.0, 0.48) * 1.8;
            double distance = Math.max(0.0, projection.distance() + edgeOffset);
            double core = 1.0 - smoothstep(width - 1.0, width + 1.5, distance);
            double bank = 1.0 - smoothstep(width + 1.5, width + 7.0, distance);
            if (core > 0.0) {
                double depth = lerp(river.headDepth(), river.mouthDepth(), projection.progress());
                double targetBed = projection.waterLevel() - depth;
                height = lerp(height, Math.min(height, targetBed), core);
                if (core > 0.35 && height < projection.waterLevel()) {
                    waterLevel = Math.max(waterLevel, (int) Math.floor(projection.waterLevel()));
                }
                riverStrength = Math.max(riverStrength, core);
            }
            shoreStrength = Math.max(shoreStrength, Math.max(0.0, bank - core));
            if (projection.drop() >= 5.0
                    && projection.progress() > 0.08 && projection.progress() < 0.90) {
                double fall = (1.0 - smoothstep(0.0, width + 6.0, distance))
                        * smoothstep(5.0, 13.0, projection.drop());
                waterfallStrength = Math.max(waterfallStrength, fall);
            }
        }

        return new HydrologySample((int) Math.round(height), waterLevel,
                riverStrength, lakeStrength, waterfallStrength, shoreStrength);
    }

    private static double square(double value) {
        return value * value;
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private static double smoothstep(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }

    public record WaterNode(double x, double z, double waterLevel) {
    }

    public record Lake(
            double x, double z, double radiusX, double radiusZ,
            int waterLevel, double depth, double angle) {
    }

    public static final class River {
        private final List<WaterNode> centerline;
        private final double headWidth;
        private final double mouthWidth;
        private final double headDepth;
        private final double mouthDepth;
        private final int salt;
        private final double minX;
        private final double maxX;
        private final double minZ;
        private final double maxZ;

        River(
                List<WaterNode> centerline, double headWidth, double mouthWidth,
                double headDepth, double mouthDepth, int salt) {
            this.centerline = List.copyOf(centerline);
            this.headWidth = headWidth;
            this.mouthWidth = mouthWidth;
            this.headDepth = headDepth;
            this.mouthDepth = mouthDepth;
            this.salt = salt;
            double smallestX = Double.POSITIVE_INFINITY;
            double largestX = Double.NEGATIVE_INFINITY;
            double smallestZ = Double.POSITIVE_INFINITY;
            double largestZ = Double.NEGATIVE_INFINITY;
            for (WaterNode node : centerline) {
                smallestX = Math.min(smallestX, node.x());
                largestX = Math.max(largestX, node.x());
                smallestZ = Math.min(smallestZ, node.z());
                largestZ = Math.max(largestZ, node.z());
            }
            double margin = mouthWidth + 12.0;
            minX = smallestX - margin;
            maxX = largestX + margin;
            minZ = smallestZ - margin;
            maxZ = largestZ + margin;
        }

        public List<WaterNode> centerline() {
            return centerline;
        }

        public double headWidth() {
            return headWidth;
        }

        public double mouthWidth() {
            return mouthWidth;
        }

        public double headDepth() {
            return headDepth;
        }

        public double mouthDepth() {
            return mouthDepth;
        }

        int salt() {
            return salt;
        }

        Projection project(double x, double z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                return new Projection(Double.POSITIVE_INFINITY,
                        centerline.get(0).waterLevel(), 0.0, 0.0);
            }
            double bestDistance = Double.POSITIVE_INFINITY;
            double bestWater = centerline.get(0).waterLevel();
            double bestProgress = 0.0;
            double bestDrop = 0.0;
            for (int index = 1; index < centerline.size(); index++) {
                WaterNode start = centerline.get(index - 1);
                WaterNode end = centerline.get(index);
                double dx = end.x() - start.x();
                double dz = end.z() - start.z();
                double lengthSquared = dx * dx + dz * dz;
                double amount = lengthSquared == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0,
                        ((x - start.x()) * dx + (z - start.z()) * dz) / lengthSquared));
                double projectedX = start.x() + dx * amount;
                double projectedZ = start.z() + dz * amount;
                double distance = Math.hypot(x - projectedX, z - projectedZ);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestWater = lerp(start.waterLevel(), end.waterLevel(), amount);
                    bestProgress = (index - 1 + amount) / (centerline.size() - 1.0);
                    bestDrop = start.waterLevel() - end.waterLevel();
                }
            }
            return new Projection(bestDistance, bestWater, bestProgress, bestDrop);
        }
    }

    private record Projection(
            double distance, double waterLevel, double progress, double drop) {
    }
}
