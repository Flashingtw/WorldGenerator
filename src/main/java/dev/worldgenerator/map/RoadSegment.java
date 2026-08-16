package dev.worldgenerator.map;

public record RoadSegment(MapPoi from, MapPoi to) {
    public double distanceTo(int x, int z) {
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0.0) return Math.hypot(x - from.x(), z - from.z());
        double t = Math.max(0.0, Math.min(1.0,
                ((x - from.x()) * dx + (z - from.z()) * dz) / lengthSquared));
        double nearestX = from.x() + dx * t;
        double nearestZ = from.z() + dz * t;
        return Math.hypot(x - nearestX, z - nearestZ);
    }

    public double targetHeight(int x, int z) {
        double dx = to.x() - from.x();
        double dz = to.z() - from.z();
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0.0) return from.y();
        double t = Math.max(0.0, Math.min(1.0,
                ((x - from.x()) * dx + (z - from.z()) * dz) / lengthSquared));
        return from.y() + (to.y() - from.y()) * t;
    }
}
