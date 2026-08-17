package dev.worldgenerator.map;

import dev.worldgenerator.terrain.PerlinNoise2D;
import java.util.List;

/** Immutable gameplay layout that flattens POIs and grades roads into terrain. */
public final class AdventureMapPlan {
    private static final AdventureMapPlan EMPTY = new AdventureMapPlan(0L, List.of(), List.of());

    private final List<MapPoi> pointsOfInterest;
    private final List<RoadSegment> roads;
    private final List<RoadSegment> bridgeApproaches;
    private final List<RoadSegment> shapingRoads;
    private final PerlinNoise2D roadEdgeNoise;

    public AdventureMapPlan(List<MapPoi> pointsOfInterest, List<RoadSegment> roads) {
        this(0L, pointsOfInterest, roads);
    }

    public AdventureMapPlan(long seed, List<MapPoi> pointsOfInterest, List<RoadSegment> roads) {
        this(seed, pointsOfInterest, roads, List.of());
    }

    public AdventureMapPlan(
            long seed, List<MapPoi> pointsOfInterest, List<RoadSegment> roads,
            List<RoadSegment> bridgeApproaches) {
        this.pointsOfInterest = List.copyOf(pointsOfInterest);
        this.roads = List.copyOf(roads);
        this.bridgeApproaches = List.copyOf(bridgeApproaches);
        var combined = new java.util.ArrayList<RoadSegment>(
                roads.size() + bridgeApproaches.size());
        combined.addAll(roads);
        combined.addAll(bridgeApproaches);
        shapingRoads = List.copyOf(combined);
        roadEdgeNoise = new PerlinNoise2D(seed ^ 0xA54FF53A5F1D36F1L);
    }

    public static AdventureMapPlan empty() {
        return EMPTY;
    }

    public List<MapPoi> pointsOfInterest() {
        return pointsOfInterest;
    }

    public List<RoadSegment> roads() {
        return roads;
    }

    public List<RoadSegment> bridgeApproaches() {
        return bridgeApproaches;
    }

    public List<RoadSegment> allRoads() {
        return shapingRoads;
    }

    public PlannedTerrain shape(int x, int z, int baseHeight) {
        double height = baseHeight;
        double roadStrength = 0.0;
        double poiStrength = 0.0;

        double strongestShoulder = 0.0;
        double roadTargetHeight = baseHeight;

        for (RoadSegment road : shapingRoads) {
            RoadSegment.Projection projection = road.projection(x, z);
            RoadKind kind = road.kindAt(projection);
            double edgeScale = kind == RoadKind.TRUNK ? 1.4 : kind == RoadKind.BRANCH ? 1.8 : 1.2;
            double edgeOffset = roadEdgeNoise.fractal(
                    x / 108.0, z / 108.0, 3, 2.0, 0.50) * edgeScale;
            double distance = Math.max(0.0, projection.distance() + edgeOffset);
            double shoulder = 1.0 - smoothstep(
                    kind.coreRadius() + 2.0, kind.shoulderRadius(), distance);
            if (shoulder > strongestShoulder) {
                strongestShoulder = shoulder;
                roadTargetHeight = projection.targetHeight();
            }
            roadStrength = Math.max(roadStrength, 1.0 - smoothstep(
                    kind.coreRadius() - 1.0, kind.coreRadius() + 1.5, distance));
        }
        if (strongestShoulder > 0.0) {
            height = lerp(height, roadTargetHeight,
                    Math.max(strongestShoulder * 0.90, roadStrength));
        }

        for (MapPoi poi : pointsOfInterest) {
            double distance = Math.hypot(x - poi.x(), z - poi.z());
            double flatRadius = switch (poi.type()) {
                case SMALL -> 29.0;
                case MEDIUM -> 40.0;
                case LARGE -> 77.0;
            };
            double outerRadius = flatRadius + 72.0;
            double flatten = 1.0 - smoothstep(flatRadius, outerRadius, distance);
            if (flatten > 0.0) {
                height = lerp(height, poi.y(), flatten);
                poiStrength = Math.max(poiStrength,
                        1.0 - smoothstep(flatRadius - 10.0, flatRadius, distance));
            }
            double roadCutoff = switch (poi.type()) {
                case SMALL -> 14.0;
                case MEDIUM -> 22.0;
                case LARGE -> 58.0;
            };
            roadStrength *= smoothstep(roadCutoff - 4.0, roadCutoff + 3.0, distance);
        }
        return new PlannedTerrain((int) Math.round(height), roadStrength, poiStrength);
    }

    private static double smoothstep(double lower, double upper, double value) {
        double t = Math.max(0.0, Math.min(1.0, (value - lower) / (upper - lower)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }
}
