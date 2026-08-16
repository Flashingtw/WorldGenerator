package dev.worldgenerator.structure;

public record StructureBlock(
        int x, int y, int z, StructureMaterial material, int horizontalConnections, int facing) {
    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public StructureBlock(int x, int y, int z, StructureMaterial material) {
        this(x, y, z, material, 0, 0);
    }

    public StructureBlock(int x, int y, int z, StructureMaterial material, int horizontalConnections) {
        this(x, y, z, material, horizontalConnections, 0);
    }
}
