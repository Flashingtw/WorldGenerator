package dev.worldgenerator.blueprint;

public record BlueprintPlacement(int anchorX, int anchorY, int anchorZ, int quarterTurns, boolean mirrored) {
    public BlueprintPlacement {
        quarterTurns = Math.floorMod(quarterTurns, 4);
    }
}
