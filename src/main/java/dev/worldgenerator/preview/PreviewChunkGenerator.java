package dev.worldgenerator.preview;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/** Empty, deterministic review world with a single neutral floor. */
final class PreviewChunkGenerator extends ChunkGenerator {
    static final int FLOOR_Y = 63;

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData) {
        if (FLOOR_Y < chunkData.getMinHeight() || FLOOR_Y >= chunkData.getMaxHeight()) return;
        chunkData.setRegion(0, FLOOR_Y, 0, 16, FLOOR_Y + 1, 16, Material.GRAY_CONCRETE);
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
}
