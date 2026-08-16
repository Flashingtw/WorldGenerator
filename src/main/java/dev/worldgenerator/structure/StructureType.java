package dev.worldgenerator.structure;

public enum StructureType {
    GAS_STATION(47, 39, 16),
    WAREHOUSE(65, 51, 20),
    MILITARY_COMPOUND(145, 121, 22);

    private final int width;
    private final int depth;
    private final int height;

    StructureType(int width, int depth, int height) {
        this.width = width;
        this.depth = depth;
        this.height = height;
    }

    public int width() { return width; }
    public int depth() { return depth; }
    public int height() { return height; }
}
