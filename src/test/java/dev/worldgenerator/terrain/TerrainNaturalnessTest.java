package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
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
        assertTrue(worst.length() <= 80,
                "parallel one-block terrace is too long: " + worst);
    }

    @Test
    void ordinaryLowlandsDoNotContainDenseShortScaleBumpsAndPits() {
        int samples = 0;
        int rough = 0;
        for (long seed : new long[] {1L, 12_345L, 0x5C00A11L}) {
            BaseTerrainSampler terrain = new BaseTerrainSampler(seed, new WorldBounds(5_000));
            for (int x = -2_100; x <= 2_100; x += 8) {
                for (int z = -2_100; z <= 2_100; z += 8) {
                    TerrainSample center = terrain.sample(x, z);
                    if (center.height() < TerrainSampler.SEA_LEVEL + 4
                            || center.height() > 90
                            || center.mountainStrength() >= 0.30) continue;
                    int north = terrain.sample(x, z - 4).height();
                    int east = terrain.sample(x + 4, z).height();
                    int south = terrain.sample(x, z + 4).height();
                    int west = terrain.sample(x - 4, z).height();
                    int laplacian = Math.abs(center.height() * 4 - north - east - south - west);
                    if (laplacian >= 3) rough++;
                    samples++;
                }
            }
        }
        double ratio = rough / (double) samples;
        assertTrue(ratio <= 0.021,
                "ordinary lowland short-scale roughness=" + ratio
                        + " (" + rough + "/" + samples + ")");
    }

    @Test
    void ordinaryLowlandsDoNotBreakIntoSmallOneBlockTerraceFragments() {
        int eligible = 0;
        int fragmented = 0;
        for (long seed : new long[] {1L, 12_345L, 0x5C00A11L}) {
            BaseTerrainSampler terrain = new BaseTerrainSampler(seed, new WorldBounds(5_000));
            for (int centerX = -1_920; centerX <= 1_920; centerX += 320) {
                for (int centerZ = -1_920; centerZ <= 1_920; centerZ += 320) {
                    Fragmentation sample = fragmentation(terrain, centerX, centerZ);
                    eligible += sample.eligible();
                    fragmented += sample.fragmented();
                }
            }
        }
        double ratio = fragmented / (double) eligible;
        assertTrue(ratio <= 0.003,
                "ordinary lowland small terrace fragmentation=" + ratio
                        + " (" + fragmented + "/" + eligible + ")");
    }

    private static Fragmentation fragmentation(
            BaseTerrainSampler terrain, int centerX, int centerZ) {
        int size = 128;
        int[][] heights = new int[size][size];
        boolean[][] eligible = new boolean[size][size];
        boolean[][] visited = new boolean[size][size];
        int eligibleCount = 0;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                TerrainSample sample = terrain.sample(
                        centerX + x - size / 2, centerZ + z - size / 2);
                heights[x][z] = sample.height();
                eligible[x][z] = sample.height() >= TerrainSampler.SEA_LEVEL + 4
                        && sample.height() <= 90
                        && sample.mountainStrength() < 0.30;
                if (eligible[x][z]) eligibleCount++;
            }
        }

        int fragmentedCount = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int startX = 1; startX < size - 1; startX++) {
            for (int startZ = 1; startZ < size - 1; startZ++) {
                if (!eligible[startX][startZ] || visited[startX][startZ]) continue;
                int height = heights[startX][startZ];
                int componentSize = 0;
                boolean enclosed = true;
                var queue = new ArrayDeque<int[]>();
                queue.add(new int[] {startX, startZ});
                visited[startX][startZ] = true;
                while (!queue.isEmpty()) {
                    int[] point = queue.removeFirst();
                    componentSize++;
                    for (int[] direction : directions) {
                        int x = point[0] + direction[0];
                        int z = point[1] + direction[1];
                        if (x <= 0 || x >= size - 1 || z <= 0 || z >= size - 1
                                || !eligible[x][z]) {
                            enclosed = false;
                            continue;
                        }
                        if (heights[x][z] == height && !visited[x][z]) {
                            visited[x][z] = true;
                            queue.addLast(new int[] {x, z});
                        }
                    }
                }
                if (enclosed && componentSize <= 96) fragmentedCount += componentSize;
            }
        }
        return new Fragmentation(eligibleCount, fragmentedCount);
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

    private record Fragmentation(int eligible, int fragmented) {
    }
}
