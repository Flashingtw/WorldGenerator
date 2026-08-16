package dev.worldgenerator.biome;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import dev.worldgenerator.terrain.WorldBounds;
import org.junit.jupiter.api.Test;

class BiomeSamplerTest {
    @Test
    void finiteWorldUsesVoidOutsideRequestedSize() {
        BiomeSampler sampler = new BiomeSampler(123L, new WorldBounds(5_000));
        assertTrue(sampler.sample(2_499, 70, 0) != BiomeKind.VOID);
        assertTrue(sampler.sample(2_500, 70, 0) == BiomeKind.VOID);
    }

    @Test
    void seed123ContainsVariedBiomes() {
        BiomeSampler sampler = new BiomeSampler(123L);
        Set<BiomeKind> biomes = EnumSet.noneOf(BiomeKind.class);
        for (int x = -8_000; x <= 8_000; x += 400) {
            for (int z = -8_000; z <= 8_000; z += 400) {
                biomes.add(sampler.sample(x, 70, z));
            }
        }
        assertTrue(biomes.size() >= 12, "expected biome variety, got " + biomes);
    }

    @Test
    void noSingleBiomeOccupiesMostOfLongTransects() {
        for (long seed : new long[] {1L, 123L, 12_345L}) {
            BiomeSampler sampler = new BiomeSampler(seed);
            for (int fixed : new int[] {-9_000, 0, 9_000}) {
                assertTrue(longestRun(sampler, fixed, true) <= 2_800,
                        "single x-axis biome run exceeded 2800 blocks for seed " + seed);
                assertTrue(longestRun(sampler, fixed, false) <= 2_800,
                        "single z-axis biome run exceeded 2800 blocks for seed " + seed);
            }
        }
    }

    private static int longestRun(BiomeSampler sampler, int fixed, boolean alongX) {
        BiomeKind previous = null;
        int run = 0;
        int longestRun = 0;
        for (int changing = -12_000; changing <= 12_000; changing += 100) {
            BiomeKind current = alongX
                    ? sampler.sample(changing, 70, fixed)
                    : sampler.sample(fixed, 70, changing);
            run = current == previous ? run + 1 : 1;
            longestRun = Math.max(longestRun, run);
            previous = current;
        }
        return longestRun * 100;
    }

    @Test
    void noForestBiomeIsSelected() {
        BiomeSampler sampler = new BiomeSampler(123L);
        Set<BiomeKind> forbidden = EnumSet.of(
                BiomeKind.FOREST,
                BiomeKind.BIRCH_FOREST,
                BiomeKind.DARK_FOREST,
                BiomeKind.TAIGA,
                BiomeKind.SNOWY_TAIGA,
                BiomeKind.JUNGLE);
        for (int x = -12_000; x <= 12_000; x += 300) {
            for (int z = -12_000; z <= 12_000; z += 300) {
                BiomeKind biome = sampler.sample(x, 70, z);
                assertTrue(!forbidden.contains(biome), "forest biome selected at " + x + ", " + z + ": " + biome);
            }
        }
    }
}
