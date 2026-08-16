package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerrainNaturalnessTest {
    private static final int WINDOW_RADIUS = 56;
    private static final int MARGIN = 8;

    @Test
    void slopedTerrainDoesNotFormLongParallelContourSteps() {
        TerraceRun worst = new TerraceRun(0, 0, 0, 0L, 0.0, 0.0);
        for (long seed : new long[] {1L, 12_345L, 0x5C00A11L}) {
            BaseTerrainSampler terrain = new BaseTerrainSampler(seed, new WorldBounds(5_000));
            MacroTerrainLayout macro = new MacroTerrainLayout(seed, new WorldBounds(5_000));
            for (int centerX = -2_000; centerX <= 2_000; centerX += 320) {
                for (int centerZ = -2_000; centerZ <= 2_000; centerZ += 320) {
                    TerraceRun candidate = longestTerrace(
                            terrain, macro, centerX, centerZ, seed);
                    if (candidate.length() > worst.length()) worst = candidate;
                }
            }
        }
        assertTrue(worst.length() <= 36,
                "parallel one-block terrace is too long: " + worst);
    }

    private static TerraceRun longestTerrace(
            BaseTerrainSampler terrain, MacroTerrainLayout macro,
            int centerX, int centerZ, long seed) {
        var macroSample = macro.sample(centerX, centerZ);
        int size = WINDOW_RADIUS * 2 + 1;
        int[][] heights = new int[size][size];
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int height = terrain.sample(
                        centerX + x - WINDOW_RADIUS,
                        centerZ + z - WINDOW_RADIUS).height();
                heights[x][z] = height;
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
            }
        }
        if (minimum < TerrainSampler.SEA_LEVEL + 3 || maximum - minimum < 12) {
            return new TerraceRun(0, centerX, centerZ, seed,
                    macroSample.hillStrength(), macroSample.mountainEnvelope());
        }

        int longest = 0;
        for (int[] direction : new int[][] {{1, 0}, {0, 1}, {1, 1}, {1, -1}}) {
            int[][] runs = new int[size][size];
            for (int x = MARGIN; x < size - MARGIN; x++) {
                for (int z = MARGIN; z < size - MARGIN; z++) {
                    int previousX = x - direction[0];
                    int previousZ = z - direction[1];
                    if (previousX < MARGIN || previousX >= size - MARGIN
                            || previousZ < MARGIN || previousZ >= size - MARGIN) continue;
                    int perpendicularX = -direction[1] * 6;
                    int perpendicularZ = direction[0] * 6;
                    int crossSlope = Math.abs(
                            heights[x + perpendicularX][z + perpendicularZ]
                                    - heights[x - perpendicularX][z - perpendicularZ]);
                    if (heights[x][z] == heights[previousX][previousZ] && crossSlope >= 3) {
                        runs[x][z] = runs[previousX][previousZ] + 1;
                        longest = Math.max(longest, runs[x][z]);
                    }
                }
            }
        }
        return new TerraceRun(longest, centerX, centerZ, seed,
                macroSample.hillStrength(), macroSample.mountainEnvelope());
    }

    private record TerraceRun(
            int length, int centerX, int centerZ, long seed,
            double hillStrength, double mountainEnvelope) {
    }
}
