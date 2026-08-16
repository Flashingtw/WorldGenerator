package dev.worldgenerator.structure;

public record StructurePlacement(
        int centerX,
        int baseY,
        int centerZ,
        StructureType type,
        int rotation,
        long damageSeed,
        StructureCondition condition) {

    public StructurePlacement {
        rotation = Math.floorMod(rotation, 4);
    }

    int localX(int worldX, int worldZ) {
        int dx = worldX - centerX;
        int dz = worldZ - centerZ;
        return switch (rotation) {
            case 1 -> dz;
            case 2 -> -dx;
            case 3 -> -dz;
            default -> dx;
        };
    }

    int localZ(int worldX, int worldZ) {
        int dx = worldX - centerX;
        int dz = worldZ - centerZ;
        return switch (rotation) {
            case 1 -> -dx;
            case 2 -> -dz;
            case 3 -> dx;
            default -> dz;
        };
    }

    int horizontalReach() {
        return Math.max(type.width(), type.depth()) / 2 + 2;
    }
}
