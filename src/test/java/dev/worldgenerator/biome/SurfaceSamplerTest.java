package dev.worldgenerator.biome;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.worldgenerator.terrain.TerrainSample;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SurfaceSamplerTest {
    @Test
    void dryLandUsesACoarseTransitionBandInsteadOfGrassTouchingClay() {
        SurfaceSampler sampler = new SurfaceSampler(12_345L);
        TerrainSample terrain = new TerrainSample(74, 0.7, 0.0);
        Set<SurfaceKind> seen = EnumSet.noneOf(SurfaceKind.class);
        for (int z = -4_000; z <= 4_000; z += 320) {
            SurfaceKind previous = sampler.sample(-4_000, z, BiomeKind.BADLANDS, terrain).kind();
            seen.add(previous);
            for (int x = -3_996; x <= 4_000; x += 4) {
                SurfaceKind current = sampler.sample(x, z, BiomeKind.BADLANDS, terrain).kind();
                assertFalse(isGrassClayPair(previous, current),
                        "grass touches terracotta at " + x + "," + z);
                seen.add(current);
                previous = current;
            }
        }
        assertTrue(seen.contains(SurfaceKind.GRASS), "missing grass in " + seen);
        assertTrue(seen.contains(SurfaceKind.COARSE_DIRT), "missing transition soil in " + seen);
        assertTrue(seen.contains(SurfaceKind.TERRACOTTA), "missing terracotta in " + seen);
    }

    @Test
    void shortGrassIsSparseAndNeverOccupiesRoadsOrPoiPads() {
        SurfaceSampler sampler = new SurfaceSampler(987L);
        int covered = 0;
        int samples = 0;
        for (int x = -500; x <= 500; x += 3) {
            for (int z = -500; z <= 500; z += 3) {
                SurfaceSample sample = sampler.sample(
                        x, z, BiomeKind.PLAINS, new TerrainSample(74, 0.7, 0.0));
                if (sample.shortGrass()) covered++;
                samples++;
            }
        }
        double density = covered / (double) samples;
        assertTrue(density > 0.08 && density < 0.42, "short-grass density=" + density);
        assertFalse(sampler.sample(0, 0, BiomeKind.PLAINS,
                new TerrainSample(74, 0.7, 0.0, 1.0, 0.0)).shortGrass());
        assertFalse(sampler.sample(0, 0, BiomeKind.PLAINS,
                new TerrainSample(74, 0.7, 0.0, 0.0, 1.0)).shortGrass());
    }

    private static boolean isGrassClayPair(SurfaceKind first, SurfaceKind second) {
        return first == SurfaceKind.GRASS && second == SurfaceKind.TERRACOTTA
                || first == SurfaceKind.TERRACOTTA && second == SurfaceKind.GRASS;
    }
}
