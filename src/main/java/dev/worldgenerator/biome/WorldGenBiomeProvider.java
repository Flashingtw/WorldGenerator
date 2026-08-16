package dev.worldgenerator.biome;

import java.util.Arrays;
import java.util.List;
import dev.worldgenerator.terrain.WorldBounds;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public final class WorldGenBiomeProvider extends BiomeProvider {
    private static final List<Biome> BIOMES = Arrays.stream(BiomeKind.values())
            .map(WorldGenBiomeProvider::toBukkitBiome)
            .distinct()
            .toList();

    private final BiomeSampler sampler;

    public WorldGenBiomeProvider(long seed) {
        this(seed, WorldBounds.UNLIMITED);
    }

    public WorldGenBiomeProvider(long seed, WorldBounds bounds) {
        sampler = new BiomeSampler(seed, bounds);
    }

    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return toBukkitBiome(sampler.sample(x, y, z));
    }

    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return BIOMES;
    }

    static Biome toBukkitBiome(BiomeKind kind) {
        return switch (kind) {
            case VOID -> Biome.THE_VOID;
            case DEEP_FROZEN_OCEAN -> Biome.DEEP_FROZEN_OCEAN;
            case FROZEN_OCEAN -> Biome.FROZEN_OCEAN;
            case DEEP_COLD_OCEAN -> Biome.DEEP_COLD_OCEAN;
            case COLD_OCEAN -> Biome.COLD_OCEAN;
            case DEEP_OCEAN -> Biome.DEEP_OCEAN;
            case OCEAN -> Biome.OCEAN;
            case DEEP_LUKEWARM_OCEAN -> Biome.DEEP_LUKEWARM_OCEAN;
            case LUKEWARM_OCEAN -> Biome.LUKEWARM_OCEAN;
            case WARM_OCEAN -> Biome.WARM_OCEAN;
            case SNOWY_BEACH -> Biome.SNOWY_BEACH;
            case BEACH -> Biome.BEACH;
            case STONY_SHORE -> Biome.STONY_SHORE;
            case SNOWY_PLAINS -> Biome.SNOWY_PLAINS;
            case SNOWY_TAIGA -> Biome.SNOWY_TAIGA;
            case TAIGA -> Biome.TAIGA;
            case PLAINS -> Biome.PLAINS;
            case SUNFLOWER_PLAINS -> Biome.SUNFLOWER_PLAINS;
            case FOREST -> Biome.FOREST;
            case BIRCH_FOREST -> Biome.BIRCH_FOREST;
            case DARK_FOREST -> Biome.DARK_FOREST;
            case SWAMP -> Biome.SWAMP;
            case SAVANNA -> Biome.SAVANNA;
            case JUNGLE -> Biome.JUNGLE;
            case DESERT -> Biome.DESERT;
            case BADLANDS -> Biome.BADLANDS;
            case MEADOW -> Biome.MEADOW;
            case GROVE -> Biome.GROVE;
            case WINDSWEPT_HILLS -> Biome.WINDSWEPT_HILLS;
            case JAGGED_PEAKS -> Biome.JAGGED_PEAKS;
            case FROZEN_PEAKS -> Biome.FROZEN_PEAKS;
            case STONY_PEAKS -> Biome.STONY_PEAKS;
        };
    }
}
