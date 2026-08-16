package dev.worldgenerator.map;

public enum PoiType {
    SMALL(34),
    MEDIUM(56),
    LARGE(88);

    private final int radius;

    PoiType(int radius) {
        this.radius = radius;
    }

    public int radius() {
        return radius;
    }
}
