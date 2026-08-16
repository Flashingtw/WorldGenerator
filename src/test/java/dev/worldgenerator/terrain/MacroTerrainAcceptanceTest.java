package dev.worldgenerator.terrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import dev.worldgenerator.map.PoiType;
import org.junit.jupiter.api.Test;

class MacroTerrainAcceptanceTest {
    private static final long[] SEEDS = {1L, 12345L, 0x5C00A11L};

    @Test
    void finiteMapsBalanceCoastLowlandsAndCoherentMountainChains() {
        for (int size : new int[] {5_000, 10_000}) {
            for (long seed : SEEDS) {
                BaseTerrainSampler terrain = new BaseTerrainSampler(seed, new WorldBounds(size));
                Metrics metrics = metrics(terrain, size, 100);
                String context = "size=" + size + " seed=" + seed + " " + metrics;
                assertTrue(metrics.landRatio() >= 0.38 && metrics.landRatio() <= 0.66, context);
                assertTrue(metrics.lowlandShareOfLand() >= 0.48, context);
                assertTrue(metrics.mountainShareOfLand() >= 0.025
                        && metrics.mountainShareOfLand() <= 0.24, context);
                assertTrue(metrics.highest() >= 124, context);
                assertTrue(metrics.largestMountainComponent() >= 7, context);
                assertTrue(coastRadiusRange(terrain, size) >= size * 0.09,
                        "coast lacks bays and peninsulas: " + context);
            }
        }
    }

    @Test
    void unlimitedTerrainUsesTheSameOceanLowlandAndMountainLanguage() {
        for (long seed : SEEDS) {
            BaseTerrainSampler terrain = new BaseTerrainSampler(seed, WorldBounds.UNLIMITED);
            int ocean = 0;
            int lowland = 0;
            int mountain = 0;
            int highest = Integer.MIN_VALUE;
            for (int x = -12_000; x <= 12_000; x += 300) {
                for (int z = -12_000; z <= 12_000; z += 300) {
                    TerrainSample sample = terrain.sample(x, z);
                    if (sample.height() < TerrainSampler.SEA_LEVEL) ocean++;
                    if (sample.height() >= TerrainSampler.SEA_LEVEL + 3
                            && sample.height() <= 90) lowland++;
                    if (sample.mountainStrength() >= 0.42) mountain++;
                    highest = Math.max(highest, sample.height());
                }
            }
            String context = "seed=" + seed + " ocean=" + ocean + " lowland=" + lowland
                    + " mountain=" + mountain + " highest=" + highest;
            assertTrue(ocean >= 700, context);
            assertTrue(lowland >= 700, context);
            assertTrue(mountain >= 80, context);
            assertTrue(highest >= 124, context);
        }
    }

    @Test
    void finiteMapsReserveEnoughBuildableLowlandForTheGameplayPlan() {
        for (int size : new int[] {5_000, 10_000}) {
            for (long seed : SEEDS) {
                TerrainSampler terrain = new TerrainSampler(seed, new WorldBounds(size));
                int minimumPois = size == 5_000 ? 14 : 24;
                String context = "size=" + size + " seed=" + seed;
                assertTrue(terrain.plan().pointsOfInterest().size() >= minimumPois,
                        "not enough buildable lowland for " + context);
                assertTrue(terrain.plan().pointsOfInterest().stream()
                                .anyMatch(poi -> poi.type() == PoiType.LARGE),
                        "missing a reserved large site for " + context);
                assertTrue(terrain.plan().roads().size()
                                >= terrain.plan().pointsOfInterest().size() - 1,
                        "gameplay layout is disconnected for " + context);
            }
        }
    }

    private static Metrics metrics(BaseTerrainSampler terrain, int size, int step) {
        int half = size / 2;
        int columns = size / step;
        boolean[][] mountains = new boolean[columns][columns];
        int land = 0;
        int lowland = 0;
        int mountain = 0;
        int highest = Integer.MIN_VALUE;
        for (int gridX = 0; gridX < columns; gridX++) {
            int x = -half + step / 2 + gridX * step;
            for (int gridZ = 0; gridZ < columns; gridZ++) {
                int z = -half + step / 2 + gridZ * step;
                TerrainSample sample = terrain.sample(x, z);
                boolean isLand = sample.height() >= TerrainSampler.SEA_LEVEL + 3;
                boolean isMountain = isLand && sample.mountainStrength() >= 0.42;
                if (isLand) land++;
                if (isLand && sample.height() <= 90 && sample.mountainStrength() < 0.30) lowland++;
                if (isMountain) mountain++;
                mountains[gridX][gridZ] = isMountain;
                highest = Math.max(highest, sample.height());
            }
        }
        return new Metrics(
                land / (double) (columns * columns),
                lowland / (double) land,
                mountain / (double) land,
                highest,
                largestComponent(mountains));
    }

    private static int coastRadiusRange(BaseTerrainSampler terrain, int size) {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int index = 0; index < 72; index++) {
            double angle = index * Math.PI * 2.0 / 72.0;
            int outermostLand = 0;
            for (int radius = 0; radius < size / 2; radius += 25) {
                int x = (int) Math.round(Math.cos(angle) * radius);
                int z = (int) Math.round(Math.sin(angle) * radius);
                if (terrain.sample(x, z).height() >= TerrainSampler.SEA_LEVEL + 3) {
                    outermostLand = radius;
                }
            }
            minimum = Math.min(minimum, outermostLand);
            maximum = Math.max(maximum, outermostLand);
        }
        return maximum - minimum;
    }

    private static int largestComponent(boolean[][] cells) {
        boolean[][] visited = new boolean[cells.length][cells[0].length];
        int largest = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int x = 0; x < cells.length; x++) {
            for (int z = 0; z < cells[x].length; z++) {
                if (!cells[x][z] || visited[x][z]) continue;
                int count = 0;
                ArrayDeque<int[]> pending = new ArrayDeque<>();
                pending.add(new int[] {x, z});
                visited[x][z] = true;
                while (!pending.isEmpty()) {
                    int[] cell = pending.removeFirst();
                    count++;
                    for (int[] direction : directions) {
                        int nextX = cell[0] + direction[0];
                        int nextZ = cell[1] + direction[1];
                        if (nextX < 0 || nextX >= cells.length || nextZ < 0
                                || nextZ >= cells[nextX].length
                                || visited[nextX][nextZ] || !cells[nextX][nextZ]) continue;
                        visited[nextX][nextZ] = true;
                        pending.add(new int[] {nextX, nextZ});
                    }
                }
                largest = Math.max(largest, count);
            }
        }
        return largest;
    }

    private record Metrics(
            double landRatio,
            double lowlandShareOfLand,
            double mountainShareOfLand,
            int highest,
            int largestMountainComponent) {
    }
}
