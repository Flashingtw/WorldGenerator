package dev.worldgenerator.blueprint;

public enum BlueprintFacing {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    BlueprintFacing rotate(int quarterTurns) {
        return values()[Math.floorMod(ordinal() + quarterTurns, 4)];
    }

    BlueprintFacing mirrorX() {
        return switch (this) {
            case EAST -> WEST;
            case WEST -> EAST;
            default -> this;
        };
    }
}
