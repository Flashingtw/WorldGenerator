package dev.worldgenerator.terrain;

import dev.worldgenerator.biome.BiomeKind;
import dev.worldgenerator.biome.BiomeSampler;
import dev.worldgenerator.biome.WorldGenBiomeProvider;
import java.util.Random;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public final class WorldGenChunkGenerator extends ChunkGenerator {
    private final TerrainSampler terrain;
    private final BiomeSampler biomes;
    private final long seed;

    public WorldGenChunkGenerator(long seed) {
        this.seed = seed;
        this.terrain = new TerrainSampler(seed);
        this.biomes = new BiomeSampler(seed);
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData) {
        int minY = chunkData.getMinHeight();
        int maxY = chunkData.getMaxHeight();
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                TerrainSample sample = terrain.sample(originX + localX, originZ + localZ);
                int surfaceY = Math.max(minY + 1, Math.min(maxY - 1, sample.height()));
                BiomeKind biome = biomes.sample(originX + localX, surfaceY, originZ + localZ);
                setColumn(chunkData, localX, localZ, minY, surfaceY, biome);
            }
        }
    }

    private static void setColumn(
            ChunkData data, int x, int z, int minY, int surfaceY, BiomeKind biome) {
        data.setBlock(x, minY, z, Material.BEDROCK);
        for (int y = minY + 1; y <= surfaceY; y++) {
            Material material;
            int depth = surfaceY - y;
            if (biome.badlandsSurface()) {
                material = depth < 5 ? Material.TERRACOTTA : Material.STONE;
            } else if (biome.sandySurface() || surfaceY <= TerrainSampler.SEA_LEVEL + 1) {
                material = depth == 0 ? Material.SAND : depth < 4 ? Material.SANDSTONE : Material.STONE;
            } else if (biome.stonySurface()) {
                material = depth < 4 ? Material.STONE : Material.DEEPSLATE;
            } else {
                material = depth == 0 ? Material.GRASS_BLOCK : depth < 4 ? Material.DIRT : Material.STONE;
            }
            data.setBlock(x, y, z, material);
        }
        for (int y = surfaceY + 1; y <= TerrainSampler.SEA_LEVEL && y < data.getMaxHeight(); y++) {
            data.setBlock(x, y, z, Material.WATER);
        }
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new WorldGenBiomeProvider(seed);
    }

    @Override
    public int getBaseHeight(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int x,
            int z,
            @NotNull HeightMap heightMap) {
        return terrain.sample(x, z).height() + 1;
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateDecorations() { return true; }
    @Override public boolean shouldGenerateMobs() { return true; }
}
