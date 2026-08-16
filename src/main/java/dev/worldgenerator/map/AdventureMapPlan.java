package dev.worldgenerator.map;

import java.util.List;

/** Immutable gameplay layout that flattens POIs and grades roads into terrain. */
public final class AdventureMapPlan {
    private static final AdventureMapPlan EMPTY = new AdventureMapPlan(List.of(), List.of());

    private final List<MapPoi> pointsOfInterest;
    private final List<RoadSegment> roads;

    public AdventureMapPlan(List<MapPoi> pointsOfInterest, List<RoadSegment> roads) {
        this.pointsOfInterest = List.copyOf(pointsOfInterest);
        this.roads = List.copyOf(roads);
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

    public PlannedTerrain shape(int x, int z, int baseHeight) {
        double height = baseHeight;
        double roadStrength = 0.0;
        double poiStrength = 0.0;

        for (RoadSegment road : roads) {
            double distance = road.distanceTo(x, z);
            double shoulder = 1.0 - smoothstep(7.0, 24.0, distance);
            if (shoulder > 0.0) {
                height = lerp(height, road.targetHeight(x, z), shoulder * 0.92);
                roadStrength = Math.max(roadStrength, 1.0 - smoothstep(4.5, 8.0, distance));
            }
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
                case SMALL -> 22.0;
                case MEDIUM -> 27.0;
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
