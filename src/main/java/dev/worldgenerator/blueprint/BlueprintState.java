package dev.worldgenerator.blueprint;

import java.util.Locale;

/** Complete, platform-neutral block state used by authored voxel blueprints. */
public record BlueprintState(
        String materialKey,
        BlueprintFacing facing,
        BlueprintPart part,
        int horizontalConnections,
        boolean open) {
    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public BlueprintState {
        if (materialKey == null || materialKey.isBlank()) {
            throw new IllegalArgumentException("material key cannot be blank");
        }
        materialKey = materialKey.toLowerCase(Locale.ROOT);
        facing = facing == null ? BlueprintFacing.NORTH : facing;
        part = part == null ? BlueprintPart.FULL : part;
        horizontalConnections &= NORTH | EAST | SOUTH | WEST;
    }

    BlueprintState transform(int quarterTurns, boolean mirrored) {
        BlueprintFacing transformedFacing = mirrored ? facing.mirrorX() : facing;
        int transformedConnections = mirrored
                ? mirrorConnections(horizontalConnections) : horizontalConnections;
        for (int turn = 0; turn < Math.floorMod(quarterTurns, 4); turn++) {
            transformedConnections = rotateConnections(transformedConnections);
        }
        return new BlueprintState(
                materialKey,
                transformedFacing.rotate(quarterTurns),
                part,
                transformedConnections,
                open);
    }

    private static int mirrorConnections(int connections) {
        int result = connections & (NORTH | SOUTH);
        if ((connections & EAST) != 0) result |= WEST;
        if ((connections & WEST) != 0) result |= EAST;
        return result;
    }

    private static int rotateConnections(int connections) {
        int result = 0;
        if ((connections & NORTH) != 0) result |= EAST;
        if ((connections & EAST) != 0) result |= SOUTH;
        if ((connections & SOUTH) != 0) result |= WEST;
        if ((connections & WEST) != 0) result |= NORTH;
        return result;
    }
}
