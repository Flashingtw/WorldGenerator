package dev.worldgenerator.terrain;

/** Finds a deterministic, moderate-height land spawn without loading chunks. */
public final class SafeSpawnLocator {
    private static final int STEP = 64;
    private static final int MAX_RADIUS = 4_096;

    private final TerrainSampler terrain;

    public SafeSpawnLocator(long seed) {
        terrain = new TerrainSampler(seed);
    }

    public SpawnPoint locate() {
        for (int radius = 0; radius <= MAX_RADIUS; radius += STEP) {
            SpawnPoint best = null;
            double bestScore = Double.POSITIVE_INFINITY;
            int min = -radius;
            int max = radius;
            for (int x = min; x <= max; x += STEP) {
                SpawnPoint north = candidate(x, min);
                SpawnPoint south = candidate(x, max);
                if (score(north) < bestScore) { best = north; bestScore = score(north); }
                if (score(south) < bestScore) { best = south; bestScore = score(south); }
            }
            for (int z = min + STEP; z < max; z += STEP) {
                SpawnPoint west = candidate(min, z);
                SpawnPoint east = candidate(max, z);
                if (score(west) < bestScore) { best = west; bestScore = score(west); }
                if (score(east) < bestScore) { best = east; bestScore = score(east); }
            }
            if (best != null) return best;
        }
        TerrainSample fallback = terrain.sample(0, 0);
        return new SpawnPoint(0, fallback.height() + 1, 0);
    }

    private SpawnPoint candidate(int x, int z) {
        TerrainSample center = terrain.sample(x, z);
        if (center.height() < TerrainSampler.SEA_LEVEL + 4 || center.height() > 108) return null;
        int maxSlope = 0;
        for (int dx : new int[] {-8, 8}) {
            for (int dz : new int[] {-8, 8}) {
                maxSlope = Math.max(maxSlope, Math.abs(center.height() - terrain.sample(x + dx, z + dz).height()));
            }
        }
        if (maxSlope > 6 || center.mountainStrength() > 0.50) return null;
        return new SpawnPoint(x, center.height() + 1, z);
    }

    private static double score(SpawnPoint point) {
        if (point == null) return Double.POSITIVE_INFINITY;
        return Math.abs(point.y() - 72) * 8.0 + Math.hypot(point.x(), point.z()) / 64.0;
    }
}
