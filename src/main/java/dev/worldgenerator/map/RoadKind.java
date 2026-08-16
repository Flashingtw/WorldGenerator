package dev.worldgenerator.map;

/** Gameplay road hierarchy; access classification is applied near every POI entrance. */
public enum RoadKind {
    TRUNK(6.5, 27.0),
    BRANCH(5.0, 22.0),
    ACCESS(4.0, 17.0);

    private final double coreRadius;
    private final double shoulderRadius;

    RoadKind(double coreRadius, double shoulderRadius) {
        this.coreRadius = coreRadius;
        this.shoulderRadius = shoulderRadius;
    }

    public double coreRadius() {
        return coreRadius;
    }

    public double shoulderRadius() {
        return shoulderRadius;
    }
}
