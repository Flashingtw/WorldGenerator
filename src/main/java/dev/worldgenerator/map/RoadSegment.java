package dev.worldgenerator.map;

import java.util.List;
import java.util.Objects;

/** A connected route whose curved centerline hides projection and grading details. */
public final class RoadSegment {
    private static final double ACCESS_LENGTH = 96.0;
    private static final double MAX_INFLUENCE = 30.0;

    private final MapPoi from;
    private final MapPoi to;
    private final RoadKind kind;
    private final List<RoadNode> centerline;
    private final double[] cumulativeLength;
    private final double totalLength;
    private final double minimumX;
    private final double maximumX;
    private final double minimumZ;
    private final double maximumZ;

    public RoadSegment(MapPoi from, MapPoi to) {
        this(from, to, RoadKind.BRANCH, List.of(
                new RoadNode(from.x(), from.y(), from.z()),
                new RoadNode(to.x(), to.y(), to.z())));
    }

    public RoadSegment(MapPoi from, MapPoi to, RoadKind kind, List<RoadNode> centerline) {
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.kind = Objects.requireNonNull(kind);
        if (centerline.size() < 2) throw new IllegalArgumentException("road needs two nodes");
        this.centerline = List.copyOf(centerline);
        cumulativeLength = new double[centerline.size()];
        double length = 0.0;
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < centerline.size(); index++) {
            RoadNode node = centerline.get(index);
            if (index > 0) length += centerline.get(index - 1).horizontalDistanceTo(node);
            cumulativeLength[index] = length;
            minX = Math.min(minX, node.x());
            maxX = Math.max(maxX, node.x());
            minZ = Math.min(minZ, node.z());
            maxZ = Math.max(maxZ, node.z());
        }
        totalLength = length;
        minimumX = minX;
        maximumX = maxX;
        minimumZ = minZ;
        maximumZ = maxZ;
    }

    public MapPoi from() {
        return from;
    }

    public MapPoi to() {
        return to;
    }

    public RoadKind kind() {
        return kind;
    }

    public List<RoadNode> centerline() {
        return centerline;
    }

    public double distanceTo(int x, int z) {
        return projection(x, z).distance();
    }

    public double targetHeight(int x, int z) {
        return projection(x, z).targetHeight();
    }

    public RoadKind kindAt(int x, int z) {
        return kindAt(projection(x, z));
    }

    Projection projection(int x, int z) {
        double boxDistanceX = Math.max(0.0, Math.max(minimumX - x, x - maximumX));
        double boxDistanceZ = Math.max(0.0, Math.max(minimumZ - z, z - maximumZ));
        double boxDistance = Math.hypot(boxDistanceX, boxDistanceZ);
        if (boxDistance > MAX_INFLUENCE) {
            return new Projection(boxDistance, centerline.get(0).y(), 0.0);
        }

        double bestDistance = Double.POSITIVE_INFINITY;
        double bestHeight = centerline.get(0).y();
        double bestProgress = 0.0;
        for (int index = 1; index < centerline.size(); index++) {
            RoadNode start = centerline.get(index - 1);
            RoadNode end = centerline.get(index);
            double dx = end.x() - start.x();
            double dz = end.z() - start.z();
            double lengthSquared = dx * dx + dz * dz;
            double t = lengthSquared == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0,
                    ((x - start.x()) * dx + (z - start.z()) * dz) / lengthSquared));
            double nearestX = start.x() + dx * t;
            double nearestZ = start.z() + dz * t;
            double distance = Math.hypot(x - nearestX, z - nearestZ);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestHeight = start.y() + (end.y() - start.y()) * t;
                bestProgress = cumulativeLength[index - 1]
                        + Math.sqrt(lengthSquared) * t;
            }
        }
        return new Projection(bestDistance, bestHeight, bestProgress);
    }

    RoadKind kindAt(Projection projection) {
        if (projection.progress() < ACCESS_LENGTH
                || totalLength - projection.progress() < ACCESS_LENGTH) {
            return RoadKind.ACCESS;
        }
        return kind;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RoadSegment other)) return false;
        return from.equals(other.from) && to.equals(other.to)
                && kind == other.kind && centerline.equals(other.centerline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, kind, centerline);
    }

    @Override
    public String toString() {
        return "RoadSegment[from=" + from + ", to=" + to + ", kind=" + kind
                + ", nodes=" + centerline.size() + "]";
    }

    record Projection(double distance, double targetHeight, double progress) {
    }
}
