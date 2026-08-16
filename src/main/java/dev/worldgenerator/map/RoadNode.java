package dev.worldgenerator.map;

/** One point on a terrain-aware road centerline. */
public record RoadNode(double x, double y, double z) {
    public double horizontalDistanceTo(RoadNode other) {
        return Math.hypot(other.x - x, other.z - z);
    }
}
