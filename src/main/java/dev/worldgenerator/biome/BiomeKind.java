package dev.worldgenerator.biome;

public enum BiomeKind {
    VOID,
    DEEP_FROZEN_OCEAN,
    FROZEN_OCEAN,
    DEEP_COLD_OCEAN,
    COLD_OCEAN,
    DEEP_OCEAN,
    OCEAN,
    DEEP_LUKEWARM_OCEAN,
    LUKEWARM_OCEAN,
    WARM_OCEAN,
    SNOWY_BEACH,
    BEACH,
    STONY_SHORE,
    SNOWY_PLAINS,
    SNOWY_TAIGA,
    TAIGA,
    PLAINS,
    SUNFLOWER_PLAINS,
    FOREST,
    BIRCH_FOREST,
    DARK_FOREST,
    SWAMP,
    SAVANNA,
    JUNGLE,
    DESERT,
    BADLANDS,
    MEADOW,
    GROVE,
    WINDSWEPT_HILLS,
    JAGGED_PEAKS,
    FROZEN_PEAKS,
    STONY_PEAKS;

    public boolean sandySurface() {
        return this == BEACH || this == DESERT;
    }

    public boolean stonySurface() {
        return this == STONY_SHORE || this == STONY_PEAKS || this == JAGGED_PEAKS;
    }

    public boolean badlandsSurface() {
        return this == BADLANDS;
    }
}
