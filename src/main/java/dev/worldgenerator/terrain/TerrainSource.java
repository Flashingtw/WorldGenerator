package dev.worldgenerator.terrain;

/** Minimal terrain query used by planners without coupling them to one generation stage. */
@FunctionalInterface
public interface TerrainSource {
    TerrainSample sample(int blockX, int blockZ);
}
